/*
 * Copyright 2017 the original author or authors.
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

import org.beryx.textio.*;
import org.beryx.textio.web.RunnerData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.beryx.textio.ReadInterruptionStrategy.Action.ABORT;

/**
 * Illustrates how to use read handlers to allow going back to a previous field.
 */
public class ContactInfo implements BiConsumer<TextIO, RunnerData> {
    private static class Contact {
        String firstName;
        String lastName;
        String streetAddress;
        String city;
        String zipCode;
        String state;
        String country;
        String phone;

        @Override
        public String toString() {
            return "\n\tfirstName: " + firstName +
                    "\n\tlastName: " + lastName +
                    "\n\tstreetAddress: " + streetAddress +
                    "\n\tcity: " + city +
                    "\n\tzipCode: " + zipCode +
                    "\n\tstate: " + state +
                    "\n\tcountry: " + country +
                    "\n\tphone: " + phone;
        }
    }

    private final Contact contact = new Contact();
    private final List<Runnable> operations = new ArrayList<>();

    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.getTextIO();
        new ContactInfo().accept(textIO, null);
    }

    @Override
    public void accept(TextIO textIO, RunnerData runnerData) {
        TextTerminal<?> terminal = textIO.getTextTerminal();
        String initData = (runnerData == null) ? null : runnerData.getInitData();
        AppUtil.printGsonMessage(terminal, initData);

        addTask(textIO, "First name", () -> contact.firstName, s -> contact.firstName = s);
        addTask(textIO, "Last name", () -> contact.lastName, s -> contact.lastName = s);
        addTask(textIO, "Street address", () -> contact.streetAddress, s -> contact.streetAddress = s);
        addTask(textIO, "City", () -> contact.city, s -> contact.city = s);
        addTask(textIO, "Zip code", () -> contact.zipCode, s -> contact.zipCode = s);
        addTask(textIO, "State", () -> contact.state, s -> contact.state = s);
        addTask(textIO, "Country", () -> contact.country, s -> contact.country = s);
        addTask(textIO, "Phone number", () -> contact.phone, s -> contact.phone = s);


        String backKeyStroke = "ctrl U";
        boolean registered = terminal.registerHandler(backKeyStroke, t -> new ReadHandlerData(ABORT));
        if(registered) {
            terminal.println("During data entry you can press '" + backKeyStroke + "' to go back to the previous field.\n");
        }
        int step = 0;
        while(step < operations.size()) {
            terminal.setBookmark("bookmark_" + step);
            try {
                operations.get(step).run();
            } catch (ReadAbortedException e) {
                if(step > 0) step--;
                terminal.resetToBookmark("bookmark_" + step);
                continue;
            }
            step++;
        }

        terminal.println("\nContact info: " + contact);

        textIO.newStringInputReader().withMinLength(0).read("\nPress enter to terminate...");
        textIO.dispose();
    }

    private void addTask(TextIO textIO, String prompt, Supplier<String> defaultValueSupplier, Consumer<String> valueSetter) {
        operations.add(() -> valueSetter.accept(textIO.newStringInputReader()
                .withDefaultValue(defaultValueSupplier.get())
                .read(prompt)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": reading contact info.\n" +
                "(Illustrates how to use read handlers to allow going back to a previous field.)";
    }
}
