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
package org.beryx.textio.jline;

import java.awt.Color;
import jline.console.ConsoleReader;
import jline.console.CursorBuffer;
import jline.console.UserInterruptException;
import jline.internal.Configuration;
import org.beryx.awt.color.ColorFactory;
import org.beryx.textio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.beryx.textio.PropertiesConstants.*;
import static org.beryx.textio.ReadInterruptionStrategy.Action.*;

/**
 * A JLine-based {@link TextTerminal}.
 */
@PropertiesPrefixes({"jline"})
public class JLineTextTerminal extends AbstractTextTerminal<JLineTextTerminal> {
    private static final Logger logger =  LoggerFactory.getLogger(JLineTextTerminal.class);

    private static final Consumer<JLineTextTerminal> DEFAULT_USER_INTERRUPT_HANDLER = textTerm -> System.exit(-1);

    private static String ANSI_RESET = "\u001B[0m";
    private static String ANSI_BOLD = "\u001B[1m";
    private static String ANSI_ITALIC = "\u001B[3m";
    private static String ANSI_UNDERLINE = "\u001B[4m";

    private static Map<String, Integer> ANSI_COLOR_MAP = new LinkedHashMap<>();
    static {
        ANSI_COLOR_MAP.put("default", -1);
        ANSI_COLOR_MAP.put("black", 0);
        ANSI_COLOR_MAP.put("red", 1);
        ANSI_COLOR_MAP.put("green", 2);
        ANSI_COLOR_MAP.put("yellow", 3);
        ANSI_COLOR_MAP.put("blue", 4);
        ANSI_COLOR_MAP.put("magenta", 5);
        ANSI_COLOR_MAP.put("cyan", 6);
        ANSI_COLOR_MAP.put("white", 7);
    }

    private static Color[] STANDARD_COLORS = {
            Color.BLACK,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.BLUE,
            Color.MAGENTA,
            Color.CYAN,
            Color.WHITE
    };

    private final ConsoleReader reader;
    private Consumer<JLineTextTerminal>userInterruptHandler = DEFAULT_USER_INTERRUPT_HANDLER;
    private boolean abortRead = true;

    private AnsiColorMode ansiColorMode = AnsiColorMode.STANDARD;

    private StyleData inputStyleData = new StyleData();
    private StyleData promptStyleData = new StyleData();

    private boolean moveToLineStartRequired = false;
    private String initialReadBuffer;

    private static class StyleData {
        String ansiColor = "";
        String ansiBackgroundColor = "";
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
    }

    private enum AnsiColorMode {
        STANDARD(JLineTextTerminal::getStandardColorCode),
        INDEXED(JLineTextTerminal::getIndexedColorCode),
        RGB(JLineTextTerminal::getRGBColorCode);

        private final Function<Color, String> colorCodeProvider;

        AnsiColorMode(Function<Color, String> colorCodeProvider) {
            this.colorCodeProvider = colorCodeProvider;
        }

        String getAnsiColorCode(Color color) {
            return colorCodeProvider.apply(color);
        }
    }

    private static String getStandardColorCode(Color color) {
        double bestDist = Double.MAX_VALUE;
        int bestIndex = -1;
        for(int i = 0; i < STANDARD_COLORS.length; i++) {
            double dist =  getColorDistance(color, STANDARD_COLORS[i]);
            if(dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }
        return "" + bestIndex;
    }

    private static double getColorDistance(Color col1, Color col2) {
        double r1 = col1.getRed() / 255.0;
        double g1 = col1.getGreen() / 255.0;
        double b1 = col1.getBlue() / 255.0;
        double r2 = col2.getRed() / 255.0;
        double g2 = col2.getGreen() / 255.0;
        double b2 = col2.getBlue() / 255.0;

        double rmean = (r1 + r2) / 2;
        double dr = r1 - r2;
        double dg = g1 - g2;
        double db = b1 - b2;

        return Math.sqrt((2 + rmean) * dr * dr + 4 * dg * dg + (3 - rmean) * db * db);
    }

    private static String getIndexedColorCode(Color color) {
        double r = color.getRed();
        double g = color.getGreen();
        double b = color.getBlue();
        int val = 16 + 36 * mapTo6(r) + 6 * mapTo6(g) + mapTo6(b);
        return "8;5;" + val;
    }

    private static String getRGBColorCode(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return "8;2;" + r + ";" + g + ";" + b;
    }

    public static int getStandardColorCode(String colorName) {
        return ANSI_COLOR_MAP.getOrDefault(colorName.toLowerCase(), -1);
    }

    public Optional<String> getColorCode(String colorName) {
        if(colorName == null || colorName.isEmpty()) return Optional.empty();
        try {
            int code = getStandardColorCode(colorName);
            if(code >= 0) {
                return Optional.of("" + code);
            }
            Color color = ColorFactory.web(colorName);
            return Optional.of(ansiColorMode.getAnsiColorCode(color));
        } catch (Exception e) {
            // the error will be logged below
        }
        logger.warn("Invalid color: {}", colorName);
        return Optional.empty();
    }

    private static int mapTo6(double val) {
        if(val < 0) val = 0;
        if(val > 255) val = 255;
        return (int)(val * 6.0 / 256.0);
    }

    private String getAnsiColorWithPrefix(int prefix, String colorName) {
        String ansiCode = getColorCode(colorName).map(col -> "\u001B[1;" + prefix + col + "m").orElse("");
        logger.debug("ansiColor({}, {}) = {}", prefix , colorName, ansiCode);
        return ansiCode;
    }

    public String getAnsiColor(String colorName) {
        return getAnsiColorWithPrefix(3, colorName);
    }

    public String getAnsiBackgroundColor(String colorName) {
        return getAnsiColorWithPrefix(4, colorName);
    }

    public static ConsoleReader createReader() {
        try {
            if(System.console() == null) throw new IllegalArgumentException("Console not available.");
            ConsoleReader consoleReader = new ConsoleReader();
            boolean expandEvents = Configuration.getBoolean(ConsoleReader.JLINE_EXPAND_EVENTS, false);
            consoleReader.setExpandEvents(expandEvents);
            return consoleReader;
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot create a JLine ConsoleReader.", e);
        }
    }

    public JLineTextTerminal() {
        this(createReader());
    }

    public JLineTextTerminal(ConsoleReader reader) {
        if(reader == null) throw new IllegalArgumentException("reader is null");
        reader.setHandleUserInterrupt(true);
        this.reader = reader;

        TerminalProperties<JLineTextTerminal> props = getProperties();
        props.addStringListener(PROP_PROMPT_COLOR, null, (term, newVal) -> setPromptColor(newVal));
        props.addStringListener(PROP_PROMPT_BGCOLOR, null, (term, newVal) -> setPromptBackgroundColor(newVal));
        props.addBooleanListener(PROP_PROMPT_BOLD, false, (term, newVal) -> setPromptBold(newVal));
        props.addBooleanListener(PROP_PROMPT_ITALIC, false, (term, newVal) -> setPromptItalic(newVal));
        props.addBooleanListener(PROP_PROMPT_UNDERLINE, false, (term, newVal) -> setPromptUnderline(newVal));
        props.addStringListener(PROP_INPUT_COLOR, null, (term, newVal) -> setInputColor(newVal));
        props.addStringListener(PROP_INPUT_BGCOLOR, null, (term, newVal) -> setInputBackgroundColor(newVal));
        props.addBooleanListener(PROP_INPUT_BOLD, false, (term, newVal) -> setInputBold(newVal));
        props.addBooleanListener(PROP_INPUT_ITALIC, false, (term, newVal) -> setInputItalic(newVal));
        props.addBooleanListener(PROP_INPUT_UNDERLINE, false, (term, newVal) -> setInputUnderline(newVal));

        props.addStringListener(PROP_ANSI_COLOR_MODE, AnsiColorMode.STANDARD.toString(), (term, newVal) -> setAnsiColorMode(newVal));
    }

    @Override
    public String read(boolean masking) {
        printAnsi(getAnsiPrefix(inputStyleData));
        try {
            String prefix = "";
            Character mask = masking ? '*' : null;
            while(true) {
                try {
                    String buffer = initialReadBuffer;
                    initialReadBuffer = null;
                    return prefix + reader.readLine(null, mask, buffer);
                } catch(UserInterruptException e) {
                    userInterruptHandler.accept(this);
                    prefix = prefix + e.getPartialLine();
                    if(abortRead) return prefix;
                } catch (ReadInterruptionException e) {
                    throw e;
                } catch (IOException e) {
                    logger.error("read error.", e);
                    return "";
                } catch (Exception e) {
                    logger.error("read error.", e);
                }
            }
        } finally {
            printAnsi(ANSI_RESET);
        }
    }

    @Override
    public void rawPrint(String message) {
        String msgPrefix = "";
        if(moveToLineStartRequired) {
            moveToLineStartRequired = false;
            msgPrefix = "\r";
        }
        printAnsi(getAnsiPrefix(promptStyleData) + msgPrefix + message + ANSI_RESET);
    }

    public void printAnsi(String message) {
        try {
            reader.setPrompt(message);
            reader.drawLine();
            reader.flush();
        } catch (IOException e) {
            logger.error("print error.", e);
        } finally {
            reader.setPrompt(null);
        }
    }

    public String getAnsiPrefix(StyleData styleData) {
        return styleData.ansiColor +
                styleData.ansiBackgroundColor +
                (styleData.bold ? ANSI_BOLD : "") +
                (styleData.italic ? ANSI_ITALIC : "") +
                (styleData.underline ? ANSI_UNDERLINE : "");
    }

    @Override
    public void println() {
        try {
            reader.println();
            reader.flush();
        } catch (IOException e) {
            logger.error("println error.", e);
        }
    }

    @Override
    public boolean resetLine() {
        try {
            reader.resetPromptLine("", "", 0);
            return true;
        } catch (IOException e) {
            logger.error("resetLine error.", e);
            return false;
        }
    }

    @Override
    public boolean moveToLineStart() {
        moveToLineStartRequired = true;
        return true;
    }

    @Override
    public boolean registerUserInterruptHandler(Consumer<JLineTextTerminal> handler, boolean abortRead) {
        this.userInterruptHandler = (handler != null) ? handler : DEFAULT_USER_INTERRUPT_HANDLER;
        this.abortRead = abortRead;
        return true;
    }

    private static class UserHandler implements ActionListener {
        private final JLineTextTerminal textTerminal;
        private final Function<JLineTextTerminal, ReadHandlerData> handler;

        private UserHandler(JLineTextTerminal textTerminal, Function<JLineTextTerminal, ReadHandlerData> handler) {
            this.textTerminal = textTerminal;
            this.handler = handler;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CursorBuffer buf = textTerminal.reader.getCursorBuffer();
            String partialInput = buf.buffer.toString();
            buf.clear();

            ReadHandlerData handlerData = handler.apply(textTerminal);
            ReadInterruptionStrategy.Action action = handlerData.getAction();
            if(action == CONTINUE) {
                buf.write(partialInput);
            } else {
                if(action == RESTART) {
                    textTerminal.initialReadBuffer = partialInput;
                }
                ReadInterruptionData interruptData = ReadInterruptionData.from(handlerData, partialInput);
                throw new ReadInterruptionException(interruptData, partialInput);
            }
        }
    }

    @Override
    public boolean registerHandler(String keyStroke, Function<JLineTextTerminal, ReadHandlerData> handler) {
        String keySeq = getKeySequence(keyStroke);
        if(keySeq == null) return false;
        reader.getKeys().bind(keySeq, new UserHandler(this, handler));
        return true;
    }

    @Override
    public void dispose(String resultData) {
        printAnsi(ANSI_RESET);
        reader.close();
    }

    @Override
    public void abort() {
        printAnsi(ANSI_RESET);
        reader.close();
    }

    public ConsoleReader getReader() {
        return reader;
    }

    public void setPromptColor(String colorName) {
        promptStyleData.ansiColor = getAnsiColor(colorName);
    }

    public void setPromptBackgroundColor(String colorName) {
        promptStyleData.ansiBackgroundColor = getAnsiBackgroundColor(colorName);
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

    public void setInputColor(String colorName) {
        inputStyleData.ansiColor = getAnsiColor(colorName);
    }

    public void setInputBackgroundColor(String colorName) {
        inputStyleData.ansiBackgroundColor = getAnsiBackgroundColor(colorName);
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

    public void setAnsiColorMode(String mode) {
        if(mode == null || mode.isEmpty()) {
            ansiColorMode = AnsiColorMode.STANDARD;
            return;
        }
        try {
            ansiColorMode = AnsiColorMode.valueOf(mode.toUpperCase());
            logger.debug("ansiColorMed set to: {}", ansiColorMode);
        } catch (Exception e) {
            logger.warn("Invalid value for ansiColorMode: {}", mode);
        }
    }

    public static String getKeySequence(String keyStroke) {
        KeyCombination kc = KeyCombination.of(keyStroke);
        if(kc == null) return null;
        if(kc.isTyped()) return String.valueOf(kc.getChar());
        int code = kc.getCode();
        if(code < 'A' || code > 'Z') return null;
        if(kc.isCtrlDown()) {
            if(kc.isAltDown()) return null;
            return String.valueOf((char)(code + 1 -'A'));
        } else if(kc.isAltDown()) {
            if(!kc.isShiftDown()) {
                code += 32;
            }
            return String.format("%c%c", (char)27, (char)code);
        }
        return null;
    }
}
