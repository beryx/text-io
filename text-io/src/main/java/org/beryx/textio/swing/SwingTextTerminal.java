/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.beryx.textio.swing;

import org.beryx.awt.color.ColorFactory;
import org.beryx.textio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.beryx.textio.PropertiesConstants.*;
import static org.beryx.textio.ReadInterruptionStrategy.Action.*;

/**
 * A {@link TextTerminal} implemented using a {@link JTextPane} inside a {@link JFrame}.
 */
@PropertiesPrefixes({"swing"})
public class SwingTextTerminal extends AbstractTextTerminal<SwingTextTerminal> {
    private static final Logger logger =  LoggerFactory.getLogger(SwingTextTerminal.class);

    private static final String ZERO_WIDTH_SPACE = "\u200B";

    public static final int DEFAULT_FONT_SIZE = 15;
    public static final Color DEFAULT_PANE_BACKGROUND = Color.black;
    public static final Color DEFAULT_PROMPT_COLOR = Color.green;
    public static final Color DEFAULT_INPUT_COLOR = Color.green;

    private final JFrame frame;
    private final JTextPane textPane;
    private final JScrollPane scrollPane;

    private String unmaskedInput = "";
    private String savedUnmaskedInput = "";
    private int startReadLen;
    private int startLineOffset;
    private int overwriteOffset = -1;

    private final Map<String, Integer> bookmarkOffsets = new HashMap<>();

    private final Object editLock = new Object();
    private volatile boolean readMode = false;
    private volatile boolean fakeReadMode = false;
    private volatile boolean inputMasking = false;
    private volatile String input;

    private Consumer<SwingTextTerminal> userInterruptHandler = textTerm -> System.exit(-1);
    private Function<SwingTextTerminal, ReadHandlerData> activatedHandler;

    private final Action userInterruptAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            if(userInterruptHandler != null) {
                userInterruptHandler.accept(SwingTextTerminal.this);
            }
        }
    };

    private boolean initialized = false;

    private final StyledDocument document;
    private final StyleData promptStyleData = new StyleData();
    private final StyleData inputStyleData = new StyleData();

    private static class StyleData {
        Color color;
        Color bgColor;
        boolean bold;
        boolean italic;
        boolean underline;
        boolean strikeThrough;
        boolean subscript;
        boolean superscript;
        String fontFamily = "Courier New";
        int fontSize = DEFAULT_FONT_SIZE;

        public String getStyleName() {
            return Stream.of(id(color), id(bgColor), id(bold), id(italic), id(underline),
                    id(strikeThrough), id(subscript), id(superscript), fontFamily, "" + fontSize)
                    .collect(Collectors.joining("|"));
        }
        private static String id(Color color) {
            return (color == null) ? "*" : ("" + color.getRGB());
        }
        private static String id(boolean b) {
            return b ? "1" : "0";
        }
    }


    @FunctionalInterface
    private interface TextChanger {
        void changeText(String text) throws BadLocationException;
    }

    private static class OffsetAttrs {
        final int offset;
        final AttributeSet attrs;

        OffsetAttrs(int offset, AttributeSet attrs) {
            this.offset = offset;
            this.attrs = attrs;
        }
    }
    private class TerminalDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException {
            OffsetAttrs fixed = fixCaretPosition(offset, attrs);
            changeText(fb, attrs, fixed.offset, text, t -> super.insertString(fb, fixed.offset, t, fixed.attrs));
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            OffsetAttrs fixed = fixCaretPosition(offset, attrs);
            if(fixed.offset == offset) {
                changeText(fb, attrs, fixed.offset, text, t -> super.replace(fb, fixed.offset, length, t, fixed.attrs));
            } else {
                changeText(fb, attrs, fixed.offset, text, t -> super.insertString(fb, fixed.offset, t, fixed.attrs));
            }
        }

        @Override
        public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
            OffsetAttrs fixed = fixCaretPosition(offset, null);
            if(offset == fixed.offset) {
                changeText(fb, null, offset, null, t -> super.remove(fb, offset, length));
            }
        }

        private OffsetAttrs fixCaretPosition(int offset, AttributeSet attrs) {
            if (!isEditAllowedAt(offset) && (readMode || fakeReadMode)) {
                textPane.setCaretPosition(document.getLength());
                return new OffsetAttrs(document.getLength(), textPane.getInputAttributes().copyAttributes());
            }
            return new OffsetAttrs(offset, attrs);
        }

        private void changeText(DocumentFilter.FilterBypass fb, AttributeSet attrs, int offset, String text, TextChanger textChanger) throws BadLocationException {
            synchronized (editLock) {
                if (isEditAllowedAt(offset)) {
                    Document doc = fb.getDocument();
                    int pos = -1;
                    if(text != null && readMode) {
                        pos = text.indexOf("\n");
                        if(pos >= 0) text = text.substring(0, pos);
                    }
                    try {
                        textChanger.changeText(text);
                        int newUnmaskedInputLen = doc.getLength() - startReadLen;
                        if((readMode || fakeReadMode) && inputMasking) {
                            int caretPosition = textPane.getCaretPosition();
                            fb.replace(startReadLen, newUnmaskedInputLen, unmaskedInput, attrs);
                            if(!fakeReadMode) {
                                textChanger.changeText(text);
                            }
                            unmaskedInput = doc.getText(startReadLen, newUnmaskedInputLen);
                            maskContent(fb, attrs);
                            textPane.setCaretPosition(caretPosition);
                        } else {
                            unmaskedInput = doc.getText(startReadLen, newUnmaskedInputLen);
                        }
                    } catch (Exception e) {
                        logger.error("changeText failed", e);
                        if(e instanceof BadLocationException) throw (BadLocationException)e;
                        else throw new BadLocationException(e.toString(), offset);
                    }
                    if(pos >= 0) {
                        input = unmaskedInput;
                        editLock.notifyAll();
                    }
                }
            }
        }

        private void maskContent(FilterBypass fb, AttributeSet attrs) throws BadLocationException {
            String maskedInput = getMaskedInput();
            fb.replace(startReadLen, maskedInput.length(), maskedInput, attrs);
        }

        private boolean isEditAllowedAt(int offset) {
            return offset >= startReadLen;
        }
    }

    public SwingTextTerminal() {
        TerminalProperties<SwingTextTerminal> props = getProperties();

        props.addStringListener(PROP_USER_INTERRUPT_KEY, null, (term, newVal) -> setUserInterruptKey(newVal));

        props.addStringListener(PROP_PANE_WIDTH, null, (term, newVal) -> updateScrollPaneSize(true));
        props.addStringListener(PROP_PANE_HEIGHT, null, (term, newVal) -> updateScrollPaneSize(true));
        props.addStringListener(PROP_PANE_BGCOLOR, null, (term, newVal) -> setPaneBackgroundColor(newVal));
        props.addStringListener(PROP_PANE_TITLE, null, (term, newVal) -> setPaneTitle(newVal));
        props.addStringListener(PROP_PANE_ICON_URL, null, (term, newVal) -> setPaneIconUrl(newVal));
        props.addStringListener(PROP_PANE_ICON_FILE, null, (term, newVal) -> setPaneIconFile(newVal));
        props.addStringListener(PROP_PANE_ICON_RESOURCE, null, (term, newVal) -> setPaneIconResource(newVal));

        props.addStringListener(PROP_PROMPT_COLOR, null, (term, newVal) -> setPromptColor(newVal));
        props.addStringListener(PROP_PROMPT_BGCOLOR, null, (term, newVal) -> setPromptBackgroundColor(newVal));
        props.addStringListener(PROP_PROMPT_FONT_FAMILY, null, (term, newVal) -> setPromptFontFamily(newVal));
        props.addIntListener(PROP_PROMPT_FONT_SIZE, DEFAULT_FONT_SIZE, (term, newVal) -> setPromptFontSize(newVal));
        props.addBooleanListener(PROP_PROMPT_BOLD, false, (term, newVal) -> setPromptBold(newVal));
        props.addBooleanListener(PROP_PROMPT_ITALIC, false, (term, newVal) -> setPromptItalic(newVal));
        props.addBooleanListener(PROP_PROMPT_UNDERLINE, false, (term, newVal) -> setPromptUnderline(newVal));
        props.addBooleanListener(PROP_PROMPT_SUBSCRIPT, false, (term, newVal) -> setPromptSubscript(newVal));
        props.addBooleanListener(PROP_PROMPT_SUPERSCRIPT, false, (term, newVal) -> setPromptSuperscript(newVal));

        props.addStringListener(PROP_INPUT_COLOR, null, (term, newVal) -> setInputColor(newVal));
        props.addStringListener(PROP_INPUT_BGCOLOR, null, (term, newVal) -> setInputBackgroundColor(newVal));
        props.addStringListener(PROP_INPUT_FONT_FAMILY, null, (term, newVal) -> setInputFontFamily(newVal));
        props.addIntListener(PROP_INPUT_FONT_SIZE, DEFAULT_FONT_SIZE, (term, newVal) -> setInputFontSize(newVal));
        props.addBooleanListener(PROP_INPUT_BOLD, false, (term, newVal) -> setInputBold(newVal));
        props.addBooleanListener(PROP_INPUT_ITALIC, false, (term, newVal) -> setInputItalic(newVal));
        props.addBooleanListener(PROP_INPUT_UNDERLINE, false, (term, newVal) -> setInputUnderline(newVal));
        props.addBooleanListener(PROP_INPUT_SUBSCRIPT, false, (term, newVal) -> setInputSubscript(newVal));
        props.addBooleanListener(PROP_INPUT_SUPERSCRIPT, false, (term, newVal) -> setInputSuperscript(newVal));

        frame = new JFrame("Text Terminal");
        textPane = new JTextPane();

        textPane.setBackground(DEFAULT_PANE_BACKGROUND);
        promptStyleData.color = DEFAULT_PROMPT_COLOR;
        inputStyleData.color = DEFAULT_INPUT_COLOR;
        textPane.setCaretColor(inputStyleData.color);

        document = textPane.getStyledDocument();
        ((AbstractDocument) document).setDocumentFilter(new TerminalDocumentFilter());

        scrollPane = new JScrollPane (textPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        updateScrollPaneSize(false);
        scrollPane.setMinimumSize(new Dimension(40, 40));

        frame.add(scrollPane);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        WindowListener exitListener = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(userInterruptHandler != null) {
                    userInterruptHandler.accept(SwingTextTerminal.this);
                }
            }
        };
        frame.addWindowListener(exitListener);

        frame.add(scrollPane);
        frame.pack();
    }

    public JFrame getFrame() {
        return frame;
    }

    public JTextPane getTextPane() {
        return textPane;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public StyledDocument getDocument() {
        return document;
    }

    public void appendToInput(String message, boolean preserveCaretPosition) {
        try {
            document.insertString(document.getLength(), message, textPane.getInputAttributes().copyAttributes());
        } catch (BadLocationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Cannot insert input text", e);
        }
        if(!preserveCaretPosition) {
            textPane.setCaretPosition(document.getLength());
        }
    }

    public void replaceInput(String message, boolean preserveCaretPosition) {
        int oldCaretPosition = textPane.getCaretPosition();
        try {
            document.remove(startReadLen, document.getLength() - startReadLen);
            document.insertString(document.getLength(), message, textPane.getInputAttributes().copyAttributes());
        } catch (BadLocationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Cannot insert input text", e);
        }
        int newCaretPosition = (preserveCaretPosition && oldCaretPosition <= document.getLength()) ? oldCaretPosition : document.getLength();
        textPane.setCaretPosition(newCaretPosition);
    }

    public String getPartialInput() {
        try {
            return document.getText(startReadLen, document.getLength() - startReadLen);
        } catch (BadLocationException e) {
            logger.error("Failed to retrieve partial input text", e);
            return "";
        }
    }

    @Override
    public String read(boolean masking) {
        rawPrint(ZERO_WIDTH_SPACE, inputStyleData);
        display();
        try {
            synchronized (editLock) {
                startReadLen = document.getLength();
                unmaskedInput = savedUnmaskedInput;
                savedUnmaskedInput = "";
                if(!unmaskedInput.isEmpty()) {
                    inputMasking = masking;
                    fakeReadMode = true;
                    plainOverwriteCurrentLine(unmaskedInput, inputStyleData);
                    fakeReadMode = false;
                }
                inputMasking = masking;
                readMode = true;
                input = null;
                while(input == null) {
                    editLock.wait();
                    if(activatedHandler != null) {
                        String currentUnmaskedInput = unmaskedInput;
                        unmaskedInput = "";
                        readMode = false;
                        Function<SwingTextTerminal, ReadHandlerData> handler = activatedHandler;
                        activatedHandler = null;
                        ReadHandlerData data = handler.apply(this);
                        readMode = true;
                        unmaskedInput = currentUnmaskedInput;
                        if(data.getAction() != CONTINUE) {
                            if(data.getAction() == RESTART) {
                                readMode = false;
                                savedUnmaskedInput = currentUnmaskedInput;
                            }
                            ReadInterruptionData readInterruptionData = ReadInterruptionData.from(data, unmaskedInput);
                            throw new ReadInterruptionException(readInterruptionData, unmaskedInput);
                        }
                    }
                }
                return input;
            }
        } catch(InterruptedException e) {
            throw new RuntimeException("read interrupted", e);
        } finally {
            synchronized (editLock) {
                inputMasking = false;
                readMode = false;
                fakeReadMode = false;
            }
            if(savedUnmaskedInput.isEmpty()) {
                rawPrint("\n", inputStyleData);
            }
        }
    }

    @Override
    public void println() {
        overwriteOffset = -1;
        rawPrint("\n");
        startLineOffset = document.getLength();
    }

    @Override
    public void rawPrint(String message) {
        display();
        synchronized (editLock) {
            plainOverwriteCurrentLine(message, promptStyleData);
        }
    }

    private void rawPrint(String message, StyleData styleData) {
        display();
        synchronized (editLock) {
            overwriteOffset = -1;
            plainOverwriteCurrentLine(message, styleData);
        }
    }

    private String getMaskedInput() {
        StringBuilder maskedSb = new StringBuilder();
        int maskedLen = unmaskedInput.length();
        for(int i=0; i<maskedLen; i++) maskedSb.append('*');
        return maskedSb.toString();
    }

    private boolean plainOverwriteCurrentLine(String message, StyleData styleData) {
        boolean result;
        if(document instanceof AbstractDocument) {
            result = plainReplaceText(message, styleData);
        } else {
            boolean resultRemove = plainRemoveFromOffset(overwriteOffset, message.length());
            if(overwriteOffset >= document.getLength()) {
                overwriteOffset = -1;
            }
            boolean resultInsert = plainInsertMessage(message, styleData);
            result = resultRemove && resultInsert;
        }
        textPane.setCaretPosition(document.getLength());
        return result;
    }

    private boolean plainReplaceText(String message, StyleData styleData) {
        if(overwriteOffset >= document.getLength()) {
            overwriteOffset = -1;
        }
        if(overwriteOffset < 0) return plainInsertMessage(message, styleData);
        String styleName = getStyle(styleData);
        int oldStartReadLen = startReadLen;
        if(startReadLen > overwriteOffset) {
            startReadLen = overwriteOffset;
        }
        boolean result = true;
        try {
            int len = Math.min(document.getLength() - overwriteOffset, message.length());
            ((AbstractDocument) document).replace(overwriteOffset, len, message, document.getStyle(styleName));
            overwriteOffset += message.length();
        } catch (BadLocationException e) {
            logger.error("Cannot replace text", e);
            startReadLen = oldStartReadLen;
            result = false;
        }
        return result;
    }

    private boolean plainRemoveFromOffset(int offset, int maxLen) {
        int len = (offset < 0) ? -1 : (document.getLength() - offset);
        if(len > maxLen) len = maxLen;
        if(len > 0) {
            int oldStartReadLen = startReadLen;
            if(startReadLen > offset) {
                startReadLen = offset;
            }
            try {
                document.remove(offset, len);
            } catch (BadLocationException e) {
                logger.error("Cannot remove from offset " + offset, e);
                startReadLen = oldStartReadLen;
                return false;
            }
        }
        return true;
    }
    private boolean plainInsertMessage(String message, StyleData styleData) {
        String styleName = getStyle(styleData);
        try {
            if(overwriteOffset >= document.getLength()) {
                overwriteOffset = -1;
            }
            int insertOffset = (overwriteOffset < 0) ? document.getLength() : overwriteOffset;
            document.insertString(insertOffset, message, document.getStyle(styleName));
            if(overwriteOffset >= 0) {
                overwriteOffset += message.length();
            }
        } catch (BadLocationException e) {
            logger.error("Cannot insert string", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean resetLine() {
        return resetToOffset(startLineOffset);
    }

    @Override
    public boolean moveToLineStart() {
        overwriteOffset = startLineOffset;
        return true;
    }

    @Override
    public boolean setBookmark(String bookmark) {
        bookmarkOffsets.put(bookmark, document.getLength());
        return true;
    }

    @Override
    public boolean resetToBookmark(String bookmark) {
        int offset = getBookmarkOffset(bookmark);
        return resetToOffset(offset);
    }

    public int getBookmarkOffset(String bookmark) {
        return bookmarkOffsets.getOrDefault(bookmark, -1);
    }

    public boolean resetToOffset(int offset) {
        if(offset < 0) return false;
        display();
        synchronized (editLock) {
            boolean result = true;
            int len = document.getLength() - offset;
            if(len > 0) {
                int oldStartReadLen = startReadLen;
                if(startReadLen > offset) {
                    startReadLen = offset;
                }
                try {
                    document.remove(offset, len);
                } catch (BadLocationException e) {
                    logger.error("Cannot reset to offset " + offset, e);
                    startReadLen = oldStartReadLen;
                    result = false;
                }
                textPane.setCaretPosition(document.getLength());
            }
            return result;
        }
    }

    public void display() {
        if(!initialized) {
            initialized = true;
            frame.pack();
        }
        if(!frame.isVisible()) {
            frame.setVisible(true);
        }
    }

    @Override
    public void dispose(String resultData) {
        frame.dispose();
        if(resultData != null && !resultData.isEmpty()) {
            logger.info("Disposed with resultData: {}.", resultData);
        }
    }

    @Override
    public void abort() {
        frame.dispose();
    }

    @Override
    public boolean registerUserInterruptHandler(Consumer<SwingTextTerminal> handler, boolean abortRead) {
        this.userInterruptHandler = handler;
        return true;
    }

    public void setUserInterruptKey(KeyStroke keyStroke) {
        String userInterruptActionKey = "SwingTextTerminal.userInterrupt";
        textPane.getInputMap().put(keyStroke, userInterruptActionKey);
        textPane.getActionMap().put(userInterruptActionKey, userInterruptAction);
    }

    public void setUserInterruptKey(String keyStroke) {
        setUserInterruptKey(KeyStroke.getKeyStroke(keyStroke));
    }


    private static class HandlerAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        private final SwingTextTerminal textTerminal;
        private final Function<SwingTextTerminal, ReadHandlerData> handler;

        private HandlerAction(SwingTextTerminal textTerminal, Function<SwingTextTerminal, ReadHandlerData> handler) {
            this.textTerminal = textTerminal;
            this.handler = handler;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            synchronized (textTerminal.editLock) {
                textTerminal.activatedHandler = handler;
                textTerminal.editLock.notifyAll();
            }
        }
    };


    @Override
    public boolean registerHandler(String keyStroke, Function<SwingTextTerminal, ReadHandlerData> handler) {
        KeyStroke ks = KeyStroke.getKeyStroke(keyStroke);
        if(ks == null) {
            logger.warn("Invalid keyStroke: " + keyStroke);
            return false;
        }
        String actionKey = "SwingTextTerminal.handler." + keyStroke.replaceAll("\\s", "-");
        textPane.getInputMap().put(ks, actionKey);
        textPane.getActionMap().put(actionKey, new HandlerAction(this, handler));
        return true;
    }

    public String getStyle(StyleData styleData) {
        String styleName = styleData.getStyleName();
        Style style = document.getStyle(styleName);
        if (style == null) {
            Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
            style = document.addStyle(styleName, defaultStyle);
        }

        if(styleData.fontFamily != null) {
            StyleConstants.setFontFamily(style, styleData.fontFamily);
        }
        if(styleData.fontSize > 0) {
            StyleConstants.setFontSize(style, styleData.fontSize);
        }
        if(styleData.color != null) {
            StyleConstants.setForeground(style, styleData.color);
        }
        if(styleData.bgColor != null) {
            StyleConstants.setBackground(style, styleData.bgColor);
        }
        StyleConstants.setBold(style, styleData.bold);
        StyleConstants.setItalic(style, styleData.italic);
        StyleConstants.setUnderline(style, styleData.underline);
        StyleConstants.setStrikeThrough(style, styleData.strikeThrough);
        StyleConstants.setSubscript(style, styleData.subscript);
        StyleConstants.setSuperscript(style, styleData.superscript);

        return styleName;
    }

    public void setPromptColor(String colorName) {
        getColor(colorName).ifPresent(col -> promptStyleData.color = col);
    }

    public void setPromptBackgroundColor(String colorName) {
        getColor(colorName).ifPresent(col -> promptStyleData.bgColor = col);
    }

    public void setPromptFontFamily(String fontFamily) {
        promptStyleData.fontFamily = fontFamily;
    }

    public void setPromptFontSize(int fontSize) {
        promptStyleData.fontSize = fontSize;
    }

    public void setPromptBold(boolean bold) {
        promptStyleData.bold = bold;
    }

    public void setPromptItalic(boolean italic) {
        promptStyleData.italic = italic;
    }

    public void setPromptUnderline(boolean underline) {
        promptStyleData.underline = underline;
    }

    public void setPromptSubscript(boolean subscript) {
        promptStyleData.subscript = subscript;
    }

    public void setPromptSuperscript(boolean superscript) {
        promptStyleData.superscript = superscript;
    }

    public void setInputColor(String colorName) {
        getColor(colorName).ifPresent(col -> {
            inputStyleData.color = col;
            textPane.setCaretColor(inputStyleData.color);
        });
    }

    public void setInputBackgroundColor(String colorName) {
        getColor(colorName).ifPresent(col -> inputStyleData.bgColor = col);
    }

    public void setInputFontFamily(String fontFamily) {
        inputStyleData.fontFamily = fontFamily;
    }

    public void setInputFontSize(int fontSize) {
        inputStyleData.fontSize = fontSize;
    }

    public void setInputBold(boolean bold) {
        inputStyleData.bold = bold;
    }

    public void setInputItalic(boolean italic) {
        inputStyleData.italic = italic;
    }

    public void setInputUnderline(boolean underline) {
        inputStyleData.underline = underline;
    }

    public void setInputSubscript(boolean subscript) {
        inputStyleData.subscript = subscript;
    }

    public void setInputSuperscript(boolean superscript) {
        inputStyleData.superscript = superscript;
    }

    public void setPaneBackgroundColor(String colorName) {
        getColor(colorName).ifPresent(textPane::setBackground);
    }

    public void setPaneTitle(String newTitle) {
        frame.setTitle(newTitle);
    }

    public void setPaneIconUrl(String url) {
        try {
            frame.setIconImage(ImageIO.read(new URL(url)));
        } catch (IOException e) {
            logger.warn("Cannot set icon from URL " + url, e);
        }
    }

    public void setPaneIconFile(String filePath) {
        try {
            frame.setIconImage(ImageIO.read(new File(filePath)));
        } catch (IOException e) {
            logger.warn("Cannot set icon from file " + filePath, e);
        }
    }

    public void setPaneIconResource(String res) {
        InputStream istream = getClass().getResourceAsStream(res);
        if(istream == null) {
            logger.warn("Cannot find icon resource " + res);
        } else {
            try {
                frame.setIconImage(ImageIO.read(istream));
            } catch (IOException e) {
                logger.warn("Cannot set icon from resource " + res, e);
            }
        }
    }

    protected void updateScrollPaneSize(boolean pack) {
        TerminalProperties<SwingTextTerminal> props = getProperties();
        int w = props.getInt(PROP_PANE_WIDTH, 640);
        int h = props.getInt(PROP_PANE_HEIGHT, 480);
        scrollPane.setPreferredSize(new Dimension(w, h));
        if(pack) frame.pack();
    }

    public static Optional<Color> getColor(String colorName) {
        if(colorName == null || colorName.isEmpty()) return Optional.empty();
        try {
            return Optional.of(ColorFactory.web(colorName));
        } catch (Exception e) {
            logger.warn("Invalid color: {}", colorName);
            return Optional.empty();
        }
    }
}
