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
package org.beryx.textio.system;

import org.beryx.textio.AbstractTextTerminal;
import org.beryx.textio.PropertiesPrefixes;
import org.beryx.textio.TextTerminal;

import java.io.PrintStream;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * A {@link TextTerminal} implemented using {@link System#out}, {@link System#in} and {@link Scanner}.
 * It is not capable to mask input strings, therefore not recommended when reading sensitive data.
 */
@PropertiesPrefixes({"system"})
public class SystemTextTerminal extends AbstractTextTerminal<SystemTextTerminal> {
    private final Scanner scanner = new Scanner(System.in);
    private final PrintStream out = System.out;

    @Override
    public String read(boolean masking) {
        return scanner.nextLine();
    }

    @Override
    public void rawPrint(String message) {
        out.print(message);
        out.flush();
    }

    @Override
    public void println() {
        out.println();
        out.flush();
    }
}
