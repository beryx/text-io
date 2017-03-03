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

import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import org.beryx.textio.AbstractTextTerminal;
import org.beryx.textio.PropertiesPrefixes;
import org.beryx.textio.TextTerminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A JLine-based {@link TextTerminal}.
 */
@PropertiesPrefixes({"jline"})
public class JLineTextTerminal extends AbstractTextTerminal<JLineTextTerminal> {
    private static final Logger logger =  LoggerFactory.getLogger(JLineTextTerminal.class);

    public static final String PROP_PROMPT_COLOR = "prompt.color";
    public static final String PROP_PROMPT_BGCOLOR = "prompt.bgcolor";
    public static final String PROP_PROMPT_BOLD = "prompt.promptBold";
    public static final String PROP_INPUT_COLOR = "input.color";
    public static final String PROP_INPUT_BGCOLOR = "input.bgcolor";
    public static final String PROP_INPUT_BOLD = "input.promptBold";

    private static final Consumer<JLineTextTerminal> DEFAULT_USER_INTERRUPT_HANDLER = textTerm -> System.exit(-1);

    private static String ANSI_RESET = "\u001B[0m";
    private static String ANSI_BOLD = "\u001B[1m";

    public static Map<String, Integer> ANSI_COLOR_MAP = new LinkedHashMap<>();
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

    private final ConsoleReader reader;
    private Consumer<JLineTextTerminal>userInterruptHandler = DEFAULT_USER_INTERRUPT_HANDLER;
    private boolean abortRead = true;

    private String ansiPromptColor = "";
    private String ansiPromptBackgroundColor = "";
    private boolean promptBold = false;
    private String ansiInputColor = "";
    private String ansiInputBackgroundColor = "";
    private boolean inputBold = false;

    public static int getColorCode(String colorName) {
        return ANSI_COLOR_MAP.getOrDefault(colorName.toLowerCase(), -1);
    }

    public static String getAnsiColor(String colorName) {
        int color = getColorCode(colorName);
        return (color < 0) ? "" : ("\u001B[1;3" + color + "m");
    }

    public static String getAnsiBackgroundColor(String colorName) {
        int color = getColorCode(colorName);
        return (color < 0) ? "" : ("\u001B[1;4" + color + "m");
    }

    public static ConsoleReader createReader() {
        try {
            if(System.console() == null) throw new IllegalArgumentException("Console not available.");
            return new ConsoleReader();
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

        addPropertyChangeListener(PROP_PROMPT_COLOR, (oldKey, newKey) -> setPromptColor(newKey));
        addPropertyChangeListener(PROP_PROMPT_BGCOLOR, (oldKey, newKey) -> setPromptBackgroundColor(newKey));
        addPropertyChangeListener(PROP_PROMPT_BOLD, (oldKey, newKey) -> setPromptBold(Boolean.valueOf(newKey)));
        addPropertyChangeListener(PROP_INPUT_COLOR, (oldKey, newKey) -> setInputColor(newKey));
        addPropertyChangeListener(PROP_INPUT_BGCOLOR, (oldKey, newKey) -> setInputBackgroundColor(newKey));
        addPropertyChangeListener(PROP_INPUT_BOLD, (oldKey, newKey) -> setInputBold(Boolean.valueOf(newKey)));
    }

    @Override
    public String read(boolean masking) {
        printAnsi(ansiInputColor + ansiInputBackgroundColor + (inputBold ? ANSI_BOLD : ""));
        try {
            String prefix = "";
            Character mask = masking ? '*' : null;
            while(true) {
                try {
                    return prefix + reader.readLine(mask);
                } catch(UserInterruptException e) {
                    userInterruptHandler.accept(this);
                    prefix = prefix + e.getPartialLine();
                    if(abortRead) return prefix;
                } catch (IOException e) {
                    logger.error("read error.", e);
                    return "";
                }
            }
        } finally {
            printAnsi(ANSI_RESET);
        }
    }

    @Override
    public void rawPrint(String message) {
        printAnsi(ansiPromptColor + ansiPromptBackgroundColor + (promptBold ? ANSI_BOLD : "") + message + ANSI_RESET);
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
    public boolean registerUserInterruptHandler(Consumer<JLineTextTerminal> handler, boolean abortRead) {
        this.userInterruptHandler = (handler != null) ? handler : DEFAULT_USER_INTERRUPT_HANDLER;
        this.abortRead = abortRead;
        return true;
    }

    public ConsoleReader getReader() {
        return reader;
    }

    public void setPromptColor(String colorName) {
        this.ansiPromptColor = getAnsiColor(colorName);
    }

    public void setPromptBackgroundColor(String colorName) {
        this.ansiPromptBackgroundColor = getAnsiBackgroundColor(colorName);
    }

    public void setPromptBold(boolean promptBold) {
        this.promptBold = promptBold;
    }

    public void setInputColor(String colorName) {
        this.ansiInputColor = getAnsiColor(colorName);
    }

    public void setInputBackgroundColor(String colorName) {
        this.ansiInputBackgroundColor = getAnsiBackgroundColor(colorName);
    }

    public void setInputBold(boolean promptBold) {
        this.inputBold = promptBold;
    }
}
