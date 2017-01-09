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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnsiTextTerminal extends JLineTextTerminal {
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

    private String ansiColor = getAnsiColor("green");
    private String ansiBackgroundColor = "";
    private boolean bold = false;

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

    private static ConsoleReader createReader() {
        try {
            if(System.console() == null) throw new IllegalArgumentException("Console not available.");
            return new ConsoleReader();
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot create a JLine ConsoleReader.", e);
        }
    }

    public AnsiTextTerminal() {
        super(createReader());
    }

    public AnsiTextTerminal withColor(String colorName) {
        this.ansiColor = getAnsiColor(colorName);
        return this;
    }

    public AnsiTextTerminal withBackgroundColor(String colorName) {
        this.ansiBackgroundColor = getAnsiBackgroundColor(colorName);
        return this;
    }

    public AnsiTextTerminal withBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    @Override
    public void rawPrint(String message) {
        super.rawPrint(ansiColor + ansiBackgroundColor + (bold ? ANSI_BOLD : "") + message + ANSI_RESET);
    }
}
