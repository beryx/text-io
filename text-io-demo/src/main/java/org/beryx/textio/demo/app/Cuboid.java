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
package org.beryx.textio.demo.app;

import org.beryx.textio.TerminalProperties;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;

import java.util.function.Consumer;

import static org.beryx.textio.PropertiesConstants.*;

/**
 * A simple application illustrating the use of TextIO.
 */
public class Cuboid implements Consumer<TextIO> {
    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.getTextIO();
        new Cuboid().accept(textIO);
    }

    private static class TextProps {
        private final String prefix;
        private final TerminalProperties props;

        public final String color;
        public final String bgcolor;
        public final boolean bold;
        public final boolean italic;
        public final boolean underline;

        public TextProps(TerminalProperties props, String prefix) {
            this.props = props;
            this.prefix = prefix;
            color = props.getString(propName("color"));
            bgcolor = props.getString(propName("bgcolor"));
            bold = props.getBoolean(propName("bold"), false);
            italic = props.getBoolean(propName("italic"), false);
            underline = props.getBoolean(propName("underline"), false);
        }

        private String propName(String name) {
            return "custom." + prefix + "." + name;
        }

        public void configurePrompt() {
            if(color != null) {
                props.setPromptColor(color);
            }
            if(bgcolor != null) {
                props.setPromptBackgroundColor(bgcolor);
            }
            props.setPromptBold(bold);
            props.setPromptItalic(italic);
            props.setPromptUnderline(underline);
        }

        public void configureInput() {
            if(color != null) {
                props.setInputColor(color);
            }
            if(bgcolor != null) {
                props.setInputBackgroundColor(bgcolor);
            }
            props.setInputBold(bold);
            props.setInputItalic(italic);
            props.setInputUnderline(underline);
        }
    }

    @Override
    public void accept(TextIO textIO) {
        TextTerminal terminal = textIO.getTextTerminal();
        TerminalProperties props = terminal.getProperties();

        new TextProps(props, "title").configurePrompt();
        terminal.println("Cuboid dimensions");

        new TextProps(props, "length.prompt").configurePrompt();
        new TextProps(props, "length.input").configureInput();
        double length = textIO.newDoubleInputReader()
                .withMinVal(0.0)
                .read("Length");

        new TextProps(props, "width.prompt").configurePrompt();
        new TextProps(props, "width.input").configureInput();
        double width = textIO.newDoubleInputReader()
                .withMinVal(0.0)
                .read("Width");

        new TextProps(props, "height.prompt").configurePrompt();
        new TextProps(props, "height.input").configureInput();
        double height = textIO.newDoubleInputReader()
                .withMinVal(0.0)
                .read("Height");


        new TextProps(props, "title").configurePrompt();
        terminal.println("The volume of your cuboid is: " + length * width * height);

        new TextProps(props, "default").configurePrompt();
        textIO.newStringInputReader().withMinLength(0).read("\nPress enter to terminate...");
        textIO.dispose();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": computing the volume of a cuboid.";
    }
}
