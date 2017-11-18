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
package org.beryx.textio;

import java.util.function.Supplier;

/**
 * A reader for byte values.
 */
public class ByteInputReader extends ComparableInputReader<Byte, ByteInputReader> {
    public ByteInputReader(Supplier<TextTerminal<?>> textTerminalSupplier) {
        super(textTerminalSupplier);
    }

    @Override
    protected String typeNameWithIndefiniteArticle() {
        return "a byte";
    }

    @Override
    protected ParseResult<Byte> parse(String s) {
        try {
            return new ParseResult<>(Byte.parseByte(s));
        } catch (NumberFormatException e) {
            return new ParseResult<>(null, getErrorMessages(s));
        }
    }
}
