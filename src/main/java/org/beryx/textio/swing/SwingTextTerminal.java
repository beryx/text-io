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

import org.beryx.textio.TextTerminal;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * A {@link TextTerminal} implemented using a {@link JTextArea} inside a {@link JFrame}.
 */
public class SwingTextTerminal implements TextTerminal {

    private final JFrame frame;
    private final JTextArea textArea;

    private String extendedPrompt = "";
    private String unmaskedContent = "";

    private final Object editLock = new Object();
    private volatile boolean readMode = false;
    private volatile boolean writeMode = false;
    private volatile boolean inputMasking = false;
    private volatile String input;

    private boolean initialized = false;

    @FunctionalInterface
    private interface TextChanger {
        void changeText(String text) throws BadLocationException;
    }

    private class TerminalDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException {
            changeText(fb, attrs, offset, text, t -> super.insertString(fb, offset, t, attrs));
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            changeText(fb, attrs, offset, text, t -> super.replace(fb, offset, length, t, attrs));
        }

        @Override
        public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
            changeText(fb, null, offset, null, t -> super.remove(fb, offset, length));
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
                        if(readMode && inputMasking) {
                            textChanger.changeText(text);
                            int caretPosition = textArea.getCaretPosition();

                            fb.remove(0, doc.getLength());
                            fb.insertString(0, unmaskedContent, attrs);
                            textChanger.changeText(text);
                            unmaskedContent = doc.getText(0, doc.getLength());

                            maskContent(fb, attrs);
                            textArea.setCaretPosition(caretPosition);
                        } else {
                            textChanger.changeText(text);
                            unmaskedContent = doc.getText(0, doc.getLength());
                        }
                    } catch (Exception e) {
                        if(e instanceof BadLocationException) throw (BadLocationException)e;
                        else throw new BadLocationException(e.toString(), offset);
                    }
                    if(pos >= 0) {
                        input = unmaskedContent.substring(extendedPrompt.length());
                        unmaskedContent = doc.getText(0, doc.getLength());
                        editLock.notifyAll();
                    }
                }
            }
        }

        private void maskContent(FilterBypass fb, AttributeSet attrs) throws BadLocationException {
            StringBuilder maskedSb = new StringBuilder(extendedPrompt);
            int maskedLen = unmaskedContent.length() - extendedPrompt.length();
            for(int i=0; i<maskedLen; i++) maskedSb.append('*');
            fb.remove(0, fb.getDocument().getLength());
            fb.insertString(0, maskedSb.toString(), attrs);
        }

        private boolean isEditAllowedAt(int offset) {
            return (readMode || writeMode)&& textArea.getCaretPosition() >= extendedPrompt.length() && offset >= extendedPrompt.length();
        }
    }

    public SwingTextTerminal() {
        frame = new JFrame("Text Terminal");
        textArea = new JTextArea(30, 80);
        textArea.setLineWrap(true);

        textArea.setBackground(Color.black);
        textArea.setForeground(Color.green);
        textArea.setCaretColor(Color.green);
        textArea.setFont(new Font("Courier New", Font.PLAIN, 15));

        ((AbstractDocument) textArea.getDocument()).setDocumentFilter(new TerminalDocumentFilter());

        JScrollPane scroll = new JScrollPane (textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        frame.add(scroll);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        WindowListener exitListener = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                synchronized (editLock) {
                    if(readMode || writeMode) return;
                }
                frame.dispose();
            }
        };
        frame.addWindowListener(exitListener);

        frame.add(scroll);
        frame.pack();
    }

    public JFrame getFrame() {
        return frame;
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    @Override
    public String read(boolean masking) {
        display();
        try {
            synchronized (editLock) {
                input = null;
                inputMasking = masking;
                readMode = true;
                while(input == null) {
                    editLock.wait();
                }
                return input;
            }
        } catch(InterruptedException e) {
            throw new RuntimeException("read interrupted", e);
        } finally {
            synchronized (editLock) {
                inputMasking = false;
                readMode = false;
            }
            println();
        }
    }

    @Override
    public void rawPrint(String message) {
        display();
        try {
            synchronized (editLock) {
                writeMode = true;
                textArea.append(message);
                extendedPrompt = textArea.getText();
                textArea.setCaretPosition(extendedPrompt.length());
            }
        } finally {
            synchronized (editLock) {
                writeMode = false;
            }
        }
    }

    @Override
    public void println() {
        rawPrint("\n");
    }

    public void display() {
        if(!initialized) {
            initialized = true;
            frame.pack();
        }
        frame.setVisible(true);
    }

    @Override
    public void dispose() {
        frame.dispose();
    }
}
