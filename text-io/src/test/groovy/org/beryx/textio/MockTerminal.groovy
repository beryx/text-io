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
package org.beryx.textio

class MockTerminal implements TextTerminal {
    static final int MAX_READS = 100
    final List<String> inputs = []
    private int inputIndex = -1
    private final StringBuilder outputBuilder = new StringBuilder()

    @Override
    String read(boolean masking) {
        assert !inputs.empty
        inputIndex++
        if(inputIndex >= MAX_READS) throw new RuntimeException("Too many read calls")
        def val = inputs[(inputIndex < inputs.size()) ? inputIndex : -1]
        outputBuilder << "$val\n"
        val
    }

    @Override
    void rawPrint(String message) {
        outputBuilder << message
    }

    @Override
    void println() {
        outputBuilder << '\n'
    }

    String getOutput() {
        TextIoSpec.stripAll(outputBuilder.toString())
    }

    int getReadCalls() {
        inputIndex + 1
    }
}
