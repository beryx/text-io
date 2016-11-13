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

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * A reader for string values.
 * Allows configuring the minimum and maximum length, as well as a regex pattern.
 * By default, it uses a numbered list for displaying the possible values.
 */
public class StringInputReader extends InputReader<String, StringInputReader> {
    private Pattern pattern;
    private int minLength = 1;
    private int maxLength = -1;

    public StringInputReader(Supplier<TextTerminal> textTerminalSupplier) {
        super(textTerminalSupplier);
        this.numberedPossibleValues = true;
    }

    public StringInputReader withPattern(String regex) {
        this.pattern = (regex == null) ? null : Pattern.compile(regex);
        return this;
    }

    public StringInputReader withPattern(String regex, int flags) {
        this.pattern = (regex == null) ? null : Pattern.compile(regex, flags);
        return this;
    }

    public StringInputReader withMinLength(int minLength) {
        this.minLength = minLength;
        return this;
    }

    public StringInputReader withMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    @Override
    protected List<String> getDefaultErrorMessage(String s) {
        List<String> errList = super.getDefaultErrorMessage(s);
        String validationErr = getValidationError(s);
        if(validationErr != null) errList.add(validationErr);
        return errList;
    }

    @Override
    public void checkConfiguration() throws IllegalArgumentException {
        super.checkConfiguration();
        if(minLength > 0 && maxLength > 0 && minLength > maxLength) throw new IllegalArgumentException("minLength = " + minLength + ", maxLength = " + maxLength);
        if(defaultValue != null) {
            if(minLength > 0 && minLength > defaultValue.length()) throw new IllegalArgumentException("minLength = " + minLength + ", defaultValue = " + defaultValue);
            if(maxLength > 0 && maxLength < defaultValue.length()) throw new IllegalArgumentException("maxLength = " + maxLength + ", defaultValue = " + defaultValue);
        }
    }

    @Override
    public ParseResult<String> parse(String s) {
        if(getValidationError(s) == null) return new ParseResult<>(s);
        return new ParseResult<>(s, getErrorMessage(s));
    }

    private String getValidationError(String s) {
        if(possibleValues == null) {
            int len = (s == null) ? 0 : s.length();
            IntFunction<String> chr = l -> l + " character" + ((l > 1) ? "s." : ".");
            if(minLength > 0 && minLength > len) return "Expected a string with at least " + chr.apply(minLength);
            if(maxLength > 0 && maxLength < len) return "Expected a string with at most " + chr.apply(maxLength);
            if((pattern != null) && !pattern.matcher(s).matches()) return "Expected format: " + pattern.pattern();
        }
        return null;
    }
}
