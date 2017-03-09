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

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;

import java.time.Month;
import java.util.function.Consumer;

/**
 * A simple application illustrating the use of TextIO.
 */
public class UserDataCollector implements Consumer<TextIO> {
    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.getTextIO();
        new UserDataCollector().accept(textIO);
    }

    @Override
    public void accept(TextIO textIO) {
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

        textIO.getTextTerminal().printf("\nUser %s is %d years old, was born in %s and has the password %s.\n", user, age, month, password);

        textIO.newStringInputReader().withMinLength(0).read("\nPress enter to terminate...");
        textIO.dispose();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": an application for reading personal data";
    }
}
