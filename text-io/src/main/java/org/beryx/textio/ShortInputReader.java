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
 * A reader for short values.
 */
public class ShortInputReader extends ComparableInputReader<Short, ShortInputReader> {
    public ShortInputReader(Supplier<TextTerminal<?>> textTerminalSupplier) {
        super(textTerminalSupplier);
    }

    @Override
    protected String typeNameWithIndefiniteArticle() {
        return "a short";
    }

    @Override
    protected ParseResult<Short> parse(String s) {
        try {
            return new ParseResult<>(Short.parseShort(s));
        } catch (NumberFormatException e) {
            return new ParseResult<>(null, getErrorMessages(s));
        }
    }
}
