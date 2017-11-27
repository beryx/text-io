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
package org.beryx.textio.console;

import org.beryx.textio.AbstractTextTerminal;
import org.beryx.textio.PropertiesPrefixes;
import org.beryx.textio.TextTerminal;

import java.io.Console;
import java.util.function.Consumer;

/**
 * A {@link TextTerminal} backed by a {@link Console}.
 */
@PropertiesPrefixes({"console"})
public class ConsoleTextTerminal extends AbstractTextTerminal<ConsoleTextTerminal> {
    private final Console console;

    public ConsoleTextTerminal() {
        this(System.console());
    }

    public ConsoleTextTerminal(Console console) {
        if(console == null) throw new IllegalArgumentException("console is null");
        this.console = console;
    }

    @Override
    public String read(boolean masking) {
        if(masking) {
            char[] chars = console.readPassword();
            return (chars == null) ? null : new String(chars);
        } else {
            return console.readLine();
        }
    }

    @Override
    public void rawPrint(String message) {
        console.printf(message);
        console.flush();
    }

    @Override
    public void println() {
        console.printf("\n");
        console.flush();
    }
}
