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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * A reader for values of y type that implements {@link Comparable}.
 * Allows configuring the minimum and maximum permitted values.
 */
public abstract class ComparableInputReader<T extends Comparable<T>, B extends ComparableInputReader<T, B>> extends InputReader<T, B> {
    protected T minVal;
    protected T maxVal;

    protected abstract String typeNameWithIndefiniteArticle();

    public ComparableInputReader(Supplier<TextTerminal<?>> textTerminalSupplier) {
        super(textTerminalSupplier);
        parseErrorMessagesProvider = (val, propName) -> Arrays.asList(getDefaultErrorMessage(val), getStandardMinMaxErrorMessage());
        valueCheckers.add((val, propName) -> getMinMaxErrorMessage(val));
    }

    /** Configures the minimum allowed value */
    @SuppressWarnings("unchecked")
    public B withMinVal(T minVal) {
        this.minVal = minVal;
        return (B)this;
    }

    /** Configures the maximum allowed value */
    @SuppressWarnings("unchecked")
    public B withMaxVal(T maxVal) {
        this.maxVal = maxVal;
        return (B)this;
    }

    protected List<String> getMinMaxErrorMessage(T val) {
        if(isInRange(val)) return null;
        return Collections.singletonList(getStandardMinMaxErrorMessage());
    }

    private String getStandardMinMaxErrorMessage() {
        if(minVal != null && maxVal != null) return "Expected " + typeNameWithIndefiniteArticle() + " value between " + minVal + " and " + maxVal + ".";
        if(minVal != null) return "Expected " + typeNameWithIndefiniteArticle() + " value greater than or equal to " + minVal + ".";
        if(maxVal != null) return "Expected " + typeNameWithIndefiniteArticle() + " value less than or equal to " + maxVal + ".";
        return "Expected " + typeNameWithIndefiniteArticle() + " value.";
    }

    /** In addition to the checks performed by {@link InputReader#checkConfiguration()}, it checks if minVal &lt;= maxVal */
    @Override
    public void checkConfiguration() throws IllegalArgumentException {
        super.checkConfiguration();
        if(minVal != null && maxVal != null && minVal.compareTo(maxVal) > 0) throw new IllegalArgumentException("minVal = " + minVal + ", maxVal = " + maxVal);
    }

    /** Returns true if minVal &lt;= val &lt;= maxVal */
    public boolean isInRange(T val) {
        return (minVal == null || minVal.compareTo(val) <= 0) && (maxVal == null || maxVal.compareTo(val) >= 0);
    }
}
