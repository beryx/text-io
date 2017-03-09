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
public class Shopping implements Consumer<TextIO> {
    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.getTextIO();
        new Shopping().accept(textIO);
    }

    @Override
    public void accept(TextIO textIO) {
        TextTerminal terminal = textIO.getTextTerminal();
        TerminalProperties props = terminal.getProperties();

        props.put(PROP_PROMPT_BOLD, true);
        props.put(PROP_PROMPT_UNDERLINE, true);
        props.put(PROP_PROMPT_COLOR, "cyan");
        terminal.println("Order details");

        props.put(PROP_PROMPT_UNDERLINE, false);
        props.put(PROP_PROMPT_BOLD, false);
        props.put(PROP_INPUT_COLOR, "blue");
        props.put(PROP_INPUT_ITALIC, true);
        String product = textIO.newStringInputReader().read("Product name");

        int quantity = textIO.newIntInputReader()
                .withMinVal(1)
                .withMaxVal(10)
                .read("Quantity");

        props.put(PROP_PROMPT_BOLD, true);
        props.put(PROP_PROMPT_UNDERLINE, true);
        props.put(PROP_PROMPT_COLOR, "green");
        terminal.println("\nShipping Information");

        props.put(PROP_PROMPT_BOLD, false);
        props.put(PROP_PROMPT_UNDERLINE, false);
        props.put(PROP_INPUT_COLOR, "yellow");
        String city = textIO.newStringInputReader().read("City");
        String street = textIO.newStringInputReader().read("Street Address");
        String shippingOptions = textIO.newStringInputReader()
                .withNumberedPossibleValues("Standard Shipping", "Two-Day Shipping", "One-Day Shipping")
                .read("Shipping Options");


        props.put(PROP_PROMPT_BOLD, true);
        props.put(PROP_PROMPT_UNDERLINE, true);
        props.put(PROP_PROMPT_COLOR, "white");
        terminal.println("\nPayment Details");

        props.put(PROP_PROMPT_BOLD, false);
        props.put(PROP_PROMPT_UNDERLINE, false);
        props.put(PROP_INPUT_COLOR, "magenta");
        String paymentType = textIO.newStringInputReader()
                .withNumberedPossibleValues("PayPal", "MasterCard", "VISA")
                .read("Payment Type");
        String owner = textIO.newStringInputReader().read("Account Owner");


        props.put(PROP_PROMPT_BOLD, true);
        props.put(PROP_PROMPT_UNDERLINE, true);
        props.put(PROP_PROMPT_COLOR, "red");
        terminal.println("\nOrder Overview");

        props.put(PROP_PROMPT_BOLD, false);
        props.put(PROP_PROMPT_UNDERLINE, false);
        props.put(PROP_PROMPT_COLOR, "yellow");
        terminal.printf("Product: %s\nQuantity: %d\n", product, quantity);

        terminal.printf("\n%s to %s, %s\n", shippingOptions, street, city);

        terminal.printf("%s is paying with %s.\n", owner, paymentType);

        props.put(PROP_PROMPT_COLOR, "green");
        textIO.newStringInputReader().withMinLength(0).read("\nPress enter to terminate...");
        textIO.dispose();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": an application for placing an online order";
    }
}
