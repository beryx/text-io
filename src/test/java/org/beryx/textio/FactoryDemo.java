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
package org.beryx.textio;

import java.time.Month;

public class FactoryDemo {
    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.get();

        String firstName = textIO.newStringInputReader()
                .withPropertyName("firstName")
                .read("First name");

        String lastName = textIO.newStringInputReader()
                .withAllowEmpty(true)
                .withPropertyName("lastName")
                .read("Last name");

        int age = textIO.newIntInputReader()
                .withPropertyName("age")
                .withMinVal(13)
                .read("Age");

        Month month = textIO.newEnumInputReader(Month.class)
                .withPropertyName("month")
                .read("What month were you born in?");

        TextTerminal terminal = textIO.getTextTerminal();
        terminal.println("\nHello " + firstName + (lastName.isEmpty() ? "" : (" " +  lastName)) + ".");
        if(lastName.isEmpty()) terminal.println("You have only one name. Just like Madonna.");
        terminal.println("You are " + age + " years old and you were born in " + month + ".");

        textIO.newStringInputReader().withAllowEmpty(true).read("\nPress enter terminate...");
        textIO.dispose();
    }
}
