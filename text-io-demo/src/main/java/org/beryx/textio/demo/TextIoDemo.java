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
import org.beryx.textio.TextTerminalProvider;
import org.beryx.textio.console.ConsoleTextTerminalProvider;
import org.beryx.textio.jline.JLineTextTerminalProvider;
import org.beryx.textio.swing.SwingTextTerminalProvider;
import org.beryx.textio.system.SystemTextTerminal;
import org.beryx.textio.system.SystemTextTerminalProvider;
import org.beryx.textio.web.WebTextTerminal;
import spark.Service;

import java.util.function.Supplier;

/**
 * Demo application showing various TextTerminals.
 */
public class TextIoDemo {
    private static int webServerPort = -1;

    private static class NamedProvider implements TextTerminalProvider {
        final String name;
        final Supplier<TextTerminal> supplier;

        NamedProvider(String name, Supplier<TextTerminal> supplier) {
            this.name = name;
            this.supplier = supplier;
        }

        @Override
        public TextTerminal getTextTerminal() {
            return supplier.get();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static void main(String[] args) {
        // Uncomment the line below to trigger a user interrupt in the Swing terminal by typing Ctrl+C (instead of the default Ctrl+Q).
//        System.setProperty(SwingTextTerminal.PROP_USER_INTERRUPT_KEY, "ctrl C");

        TextIO textIO = chooseTextIO();

        // Uncomment the line below to ignore user interrupts.
//        textIO.getTextTerminal().registerUserInterruptHandler(term -> System.out.println("\n\t### User interrupt ignored."), false);

        if(textIO.getTextTerminal() instanceof WebTextTerminal) {
            WebTextTerminal webTextTerm = (WebTextTerminal)textIO.getTextTerminal();

            // Uncomment the line below to trigger a user interrupt in the web terminal by typing Ctrl+C (instead of the default Ctrl+Q).
//            webTextTerm.setUserInterruptKey('C', true, false, false);

            WebTextIoExecutor webTextIoExecutor = new WebTextIoExecutor(webTextTerm).withPort(webServerPort);
            webTextIoExecutor.execute(SimpleApp::execute);
        } else {
            SimpleApp.execute(textIO);
        }
    }

    private static TextIO chooseTextIO() {
        TextTerminal terminal = new SystemTextTerminal();
        TextIO textIO = new TextIO(terminal);
        while(true) {
            TextTerminalProvider terminalProvider = textIO.<TextTerminalProvider>newGenericInputReader(null)
                    .withNumberedPossibleValues(
                            new NamedProvider("Default terminal (provided by TextIoFactory)", TextIoFactory::getTextTerminal),
                            new SystemTextTerminalProvider(),
                            new ConsoleTextTerminalProvider(),
                            new JLineTextTerminalProvider(),
                            new SwingTextTerminalProvider(),
                            new NamedProvider("Web terminal", () -> createWebTextTerminal(textIO))
                    )
                    .read("Choose the terminal to be used for running the demo");

            TextTerminal chosenTerminal = null;
            String errMsg = null;
            try {
                chosenTerminal = terminalProvider.getTextTerminal();
            } catch (Exception e) {
                errMsg = e.getMessage();
            }
            if(chosenTerminal == null) {
                terminal.printf("\nCannot create a %s%s\n\n", terminalProvider, ((errMsg != null) ? (": " + errMsg) : "."));
                continue;
            }
            chosenTerminal.init();
            return new TextIO(chosenTerminal);
        }
    }

    private static WebTextTerminal createWebTextTerminal(TextIO textIO) {
        webServerPort = textIO.newIntInputReader()
                .withDefaultValue(Service.SPARK_DEFAULT_PORT)
                .read("Server port number");

        // The returned WebTextTerminal is used as a template by the WebTextIoExecutor, which instantiates a new WebTextTerminal each time a client starts a new session.
        return new WebTextTerminal();
    }
}
