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
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class StringInputReader extends InputReader<String, StringInputReader> {
    private Pattern pattern;
    private boolean allowEmpty = false;

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

    public StringInputReader withAllowEmpty(boolean allowEmpty) {
        this.allowEmpty = allowEmpty;
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
    public ParseResult<String> parse(String s) {
        if(getValidationError(s) == null) return new ParseResult<>(s);
        return new ParseResult<>(s, getErrorMessage(s));
    }

    private String getValidationError(String s) {
        if(possibleValues == null) {
            if((s == null || s.isEmpty()) && !allowEmpty && (defaultValue == null)) return "Empty strings are not allowed.";
            if((pattern != null) && !pattern.matcher(s).matches()) return "Expected format: " + pattern.pattern();
        }
        return null;
    }
}
