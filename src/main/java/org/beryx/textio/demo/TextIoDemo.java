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
package org.beryx.textio.demo;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;

import java.time.Month;

/**
 * Demo application illustrating the use of TextIO.
 * <br>If an argument is provided, it will be used to set the value of the <tt>{@value TextIoFactory#TEXT_TERMINAL_CLASS_PROPERTY}</tt> system property.
 * This means that the program will interpret the argument as the fully-qualified name of a concrete {@link TextTerminal} class and will try to create and use an instance of this class.
 * <br>Example: run the program with the argument <tt>org.beryx.textio.demo.ColorTextTerminal</tt>.
 */
public class TextIoDemo {
    public static void main(String[] args) {
        if(args.length > 0) {
            System.setProperty(TextIoFactory.TEXT_TERMINAL_CLASS_PROPERTY, args[0]);
        }
        TextIO textIO = TextIoFactory.getTextIO();

        String user = textIO.newStringInputReader()
                .withDefaultValue("admin")
                .read("Username");

        String password = textIO.newStringInputReader()
                .withMinLength(6)
                .withInputMasking(true)
                .read("Password");

        int age = textIO.newIntInputReader()
                .withMinVal(13)
                .read("Age");

        Month month = textIO.newEnumInputReader(Month.class)
                .read("What month were you born in?");

        TextTerminal terminal = textIO.getTextTerminal();
        terminal.println("\nUser " + user + " is " + age + " years old, was born in " + month +
                " and has the password " + password + ".");

        textIO.newStringInputReader().withMinLength(0).read("\nPress enter to terminate...");
        textIO.dispose();
    }
}
