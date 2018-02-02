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

import java.util.*;
import java.util.function.Supplier;

/**
 * A reader for values in a collection.
 *
 * @author Zygimantus
 */
@SuppressWarnings("unchecked")
public class CollectionInputReader<T> extends InputReader<T, CollectionInputReader<T>> {

    private final Collection<T> collection;

    public CollectionInputReader(Supplier<TextTerminal<?>> textTerminalSupplier, Collection<T> collection) {
        super(textTerminalSupplier);
        this.collection = collection;
        this.possibleValues = new ArrayList<>(collection);
        this.numberedPossibleValues = true;
    }

    public CollectionInputReader<T> withAllValues() {
        return withPossibleValues(new ArrayList<>(collection));
    }

    public CollectionInputReader<T> withAllValuesNumbered() {
        return withNumberedPossibleValues(new ArrayList<>(collection));
    }

    public CollectionInputReader<T> withAllValuesInline() {
        return withInlinePossibleValues(new ArrayList<>(collection));
    }

    @Override
    protected ParseResult<T> parse(String s) {
        int index = Integer.parseInt(s);
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        T value = null;
        if (collection instanceof List) {
            value = ((List<T>) collection).get(index);
        } else {
            Iterator it = collection.iterator();
            while (it.hasNext()) {
                index--;
                if (index == -1) {
                    value = (T) it.next();
                } else {
                    it.next();
                }
            }

        }

        if (value != null) {
            return new ParseResult<>(value);
        }
        return new ParseResult<>(null, getErrorMessages(s));
    }

}
