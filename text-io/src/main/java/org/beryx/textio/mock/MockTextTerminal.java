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
package org.beryx.textio.mock;

import org.beryx.textio.AbstractTextTerminal;
import org.beryx.textio.PropertiesPrefixes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A mock terminal for test purposes.
 */
@PropertiesPrefixes({"mock"})
public class MockTextTerminal extends AbstractTextTerminal<MockTextTerminal> {
    public static final int DEFAULT_MAX_READS = 100;

    private int maxReads = DEFAULT_MAX_READS;
    private final List<String> inputs = new ArrayList<>();
    private int inputIndex = -1;
    private final StringBuilder outputBuilder = new StringBuilder();

    @Override
    public String read(boolean masking) {
        if(inputs.isEmpty()) throw new IllegalStateException("No entries available in the 'inputs' list");
        inputIndex++;
        if(inputIndex >= maxReads) throw new RuntimeException("Too many read calls");
        String val = inputs.get((inputIndex < inputs.size()) ? inputIndex : -1);
        outputBuilder.append(val).append('\n');
        return val;
    }

    @Override
    public void rawPrint(String message) {
        outputBuilder.append(message);
    }

    @Override
    public void println() {
        outputBuilder.append('\n');
    }

    public List<String> getInputs() {
        return inputs;
    }

    public String getOutput() {
        return stripAll(outputBuilder.toString());
    }

    public int getReadCalls() {
        return inputIndex + 1;
    }

    public int getMaxReads() {
        return maxReads;
    }

    public void setMaxReads(int maxReads) {
        this.maxReads = maxReads;
    }

    public static String stripAll(String text) {
        if(text == null) return null;
        return Arrays.stream(text.split("\\R"))
                .map(s -> s.replaceAll("\\t", ""))
                .map(s -> s.replaceAll("^\\s+|\\s+$", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public boolean registerUserInterruptHandler(Consumer<MockTextTerminal> handler, boolean abortRead) {
        return false;
    }
}
