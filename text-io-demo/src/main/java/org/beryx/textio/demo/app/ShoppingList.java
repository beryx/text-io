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

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Illustrates how to use read handlers.
 */
public class ShoppingList implements BiConsumer<TextIO, RunnerData> {

    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.getTextIO();
        new ShoppingList().accept(textIO, null);
    }

    @Override
    public void accept(TextIO textIO, RunnerData runnerData) {
        TextTerminal<?> terminal = textIO.getTextTerminal();
        String initData = (runnerData == null) ? null : runnerData.getInitData();
        AppUtil.printGsonMessage(terminal, initData);

        String keyStrokeReboot = "ctrl R";
        String keyStrokeAutoValue = "ctrl S";
        String keyStrokeHelp = "ctrl U";
        String keyStrokeAbort = "alt Z";

        boolean registeredReboot = terminal.registerHandler(keyStrokeReboot, t -> {
            JOptionPane optionPane = new JOptionPane("System reboot in 5 minutes!", JOptionPane.WARNING_MESSAGE);
            JDialog dialog = optionPane.createDialog("REBOOT");
            dialog.setModal(true);
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
            dialog.dispose();
            return new ReadHandlerData(ReadInterruptionStrategy.Action.CONTINUE);
        });

        boolean registeredAutoValue = terminal.registerHandler(keyStrokeAutoValue, t -> {
            terminal.println();
            return new ReadHandlerData(ReadInterruptionStrategy.Action.RETURN)
                    .withReturnValueProvider(partialInput -> partialInput.isEmpty() ? "nothing" : "high-quality-" + partialInput);
        });

        boolean registeredHelp = terminal.registerHandler(keyStrokeHelp, t -> {
            terminal.executeWithPropertiesPrefix("help",
                    tt -> tt.print("\n\nType the name of a product to be included in your shopping list."));
            return new ReadHandlerData(ReadInterruptionStrategy.Action.RESTART).withRedrawRequired(true);
        });

        boolean registeredAbort = terminal.registerHandler(keyStrokeAbort,
                t -> new ReadHandlerData(ReadInterruptionStrategy.Action.ABORT)
                        .withPayload(System.getProperty("user.name", "nobody")));

        boolean hasHandlers = registeredReboot || registeredAutoValue || registeredHelp || registeredAbort;
        if(!hasHandlers) {
            terminal.println("No handlers can be registered.");
        } else {
            terminal.println("--------------------------------------------------------------------------------");
            if(registeredReboot) {
                terminal.println("Press " + keyStrokeReboot + " to display a 'reboot' message box");
            }
            if(registeredAutoValue) {
                terminal.println("Press " + keyStrokeAutoValue + " to provide a product name based on the current input text");
            }
            if(registeredHelp) {
                terminal.println("Press " + keyStrokeHelp + " to print a help message");
            }
            if(registeredAbort) {
                terminal.println("Press " + keyStrokeAbort + " to abort the program");
            }
            terminal.println("You can use these key combinations at any moment during your data entry session.");
            terminal.println("--------------------------------------------------------------------------------");

            List<String> products = new ArrayList<>();
            while(true) {
                String product;
                try {
                    product = textIO.newStringInputReader().withPropertiesPrefix("product").read("product");
                } catch (ReadAbortedException e) {
                    terminal.executeWithPropertiesPrefix("abort",
                            t -> t.println("\nRead aborted by user " + e.getPayload()));
                    break;
                }
                products.add(product);
                String content = products.stream().collect(Collectors.joining(", "));
                terminal.executeWithPropertiesPrefix("content", t ->t.println("Your shopping list contains: " + content));
                terminal.println();
            }
        }

        textIO.newStringInputReader().withMinLength(0).read("\nPress enter to terminate...");
        textIO.dispose();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": creating a shopping list.\n" +
                "(Illustrates how to use read handlers.)";
    }
}
