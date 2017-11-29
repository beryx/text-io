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

import org.beryx.textio.*;
import org.beryx.textio.web.RunnerData;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Illustrates some features introduced in version 3 of Text-IO: line reset, bookmarking etc.
 */
public class ShoppingList implements BiConsumer<TextIO, RunnerData> {
    private boolean useCelsius;
    private boolean useKmh;
    private boolean useMbar;

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

        boolean registeredReboot = terminal.bindHandler(keyStrokeReboot, t -> {
            String color = terminal.getProperties().getString(PropertiesConstants.PROP_PROMPT_COLOR);
            terminal.getProperties().setPromptColor("red");
            terminal.println("\nSystem reboot in 5 minutes!");
            terminal.getProperties().setPromptColor(color);
            return new ReadHandlerData(ReadInterruptionStrategy.Action.RESTART).withRedrawRequired(true);
        });

        boolean registeredAutoValue = terminal.bindHandler(keyStrokeAutoValue, t -> {
            terminal.println();
            return new ReadHandlerData(ReadInterruptionStrategy.Action.RETURN)
                    .withReturnValueProvider(partialInput -> partialInput.isEmpty() ? "nothing" : "big " + partialInput);
        });

        boolean registeredHelp = terminal.bindHandler(keyStrokeHelp, t -> {
            JOptionPane optionPane = new JOptionPane("Type the name of a product to be included in your shopping list",
                    JOptionPane.INFORMATION_MESSAGE);
            JDialog dialog = optionPane.createDialog("Help");
            dialog.setModal(true);
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
            dialog.dispose();
            return new ReadHandlerData(ReadInterruptionStrategy.Action.CONTINUE);
        });

        boolean registeredAbort = terminal.bindHandler(keyStrokeAbort,
                t -> new ReadHandlerData(ReadInterruptionStrategy.Action.ABORT)
                        .withPayload(System.getProperty("user.name", "nobody")));

        boolean hasHandlers = registeredReboot || registeredAutoValue || registeredHelp || registeredAbort;
        if(!hasHandlers) {
            terminal.println("No handlers can been registered.");
        } else {
            terminal.println("--------------------------------------------------------------------------------");
            if(registeredReboot) {
                terminal.println("Press " + keyStrokeReboot + " to print a 'reboot' message");
            }
            if(registeredAutoValue) {
                terminal.println("Press " + keyStrokeAutoValue + " to provide a product name based on the current input text");
            }
            if(registeredHelp) {
                terminal.println("Press " + keyStrokeHelp + " to display a help message box");
            }
            if(registeredAbort) {
                terminal.println("Press " + keyStrokeAbort + " to abort the program");
            }
            terminal.println("You can use these key combinations at any moment during your data entry session.");
            terminal.println("--------------------------------------------------------------------------------");

            List<String> products = new ArrayList<>();
            while(true) {
                String product = null;
                try {
                    product = textIO.newStringInputReader().read("product");
                } catch (ReadAbortedException e) {
                    terminal.println("\nRead aborted by user " + e.getPayload());
                    break;
                }
                products.add(product);
                terminal.println("Your shopping list contains: " + products.stream().collect(Collectors.joining(", ")));
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
