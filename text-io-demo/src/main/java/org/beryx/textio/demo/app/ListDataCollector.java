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

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.beryx.textio.web.RunnerData;

/**
 * A simple application illustrating the use of TextIO.
 */
public class ListDataCollector implements BiConsumer<TextIO, RunnerData> {
    
    private static final List<String> LIST = Arrays.asList("Hello", "World!", "How", "Are", "You");    
    
    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.getTextIO();
        new ListDataCollector().accept(textIO, null);
    }

    @Override
    public void accept(TextIO textIO, RunnerData runnerData) {
        TextTerminal<?> terminal = textIO.getTextTerminal();

        String test = (String) textIO.newCollectionInputReader(LIST)
                .read("What element from list you want to choose?");

        terminal.printf("\nYou chose %s", test);
        textIO.dispose();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": reading personal data.\n" +
                "(Properties are initialized at start-up.\n" +
                "Properties file: " + getClass().getSimpleName() + ".properties.)";
    }
}
