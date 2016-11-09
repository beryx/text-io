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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class InputReader<T, B extends InputReader<T, B>> {
    @FunctionalInterface
    public static interface ErrorMessageProvider {
        List<String> getErrorMessage(String sVal, String propertyName);
    }
    public static class ParseResult<T> {
        private final T value;
        private final List<String> errorMessages;

        public ParseResult(T value) {
            this.value = value;
            this.errorMessages = null;
        }

        public ParseResult(T value, List<String> errorMessages) {
            this.value = value;
            this.errorMessages = (errorMessages != null && errorMessages.isEmpty()) ? null : errorMessages;
        }

        public ParseResult(T value, String... errorMessages) {
            this.value = value;
            this.errorMessages = (errorMessages.length == 0) ? null : Arrays.asList(errorMessages);
        }

        public T getValue() {
            return value;
        }

        public List<String> getErrorMessages() {
            return errorMessages;
        }
    }

    protected final Supplier<TextTerminal> textTerminalSupplier;

    protected T defaultValue;
    protected List<T> possibleValues;
    protected boolean numberedPossibleValues = false;
    protected ErrorMessageProvider errorMessageProvider;
    protected String propertyName;
    protected boolean inputMasking = false;
    protected boolean inputTrimming = true;

    protected Function<T, String> valueFormatter = val -> String.valueOf(val);

    public abstract ParseResult<T> parse(String s);

    public InputReader(Supplier<TextTerminal> textTerminalSupplier) {
        this.textTerminalSupplier = textTerminalSupplier;
    }

    public B withDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return (B)this;
    }

    public B withPossibleValues(T... possibleValues) {
        this.possibleValues = null;
        if(possibleValues.length > 0) {
            this.possibleValues = new ArrayList<>();
            for(T val : possibleValues) {
                this.possibleValues.add(val);
            }
        }
        return (B)this;
    }

    public B withPossibleValues(List<T> possibleValues) {
        this.possibleValues = (possibleValues != null && possibleValues.isEmpty()) ? null : possibleValues;
        return (B)this;
    }

    public B withNumberedPossibleValues(boolean numbered) {
        this.numberedPossibleValues = numbered;
        return (B)this;
    }

    public B withInputMasking(boolean inputMasking) {
        this.inputMasking = inputMasking;
        return (B)this;
    }

    public B withInputTrimming(boolean inputTrimming) {
        this.inputTrimming = inputTrimming;
        return (B)this;
    }

    public B withPropertyName(String propertyName) {
        this.propertyName = "".equals(propertyName) ? null : propertyName;
        return (B)this;
    }

    public B withValueFormatter(Function<T, String> valueFormatter) {
        this.valueFormatter = valueFormatter;
        return (B)this;
    }

    public B withErrorMessageProvider(ErrorMessageProvider errorMessageProvider) {
        this.errorMessageProvider = errorMessageProvider;
        return (B)this;
    }

    protected String getDefaultErrorMessage() {
        StringBuilder errBuilder = new StringBuilder("Invalid value");
        if(propertyName != null) errBuilder.append(" for '" + propertyName + "'");
        errBuilder.append('.');
        return errBuilder.toString();
    }

    protected List<String> getDefaultErrorMessage(String s) {
        return new ArrayList<>(Collections.singleton(getDefaultErrorMessage()));
    }

    public final List<String> getErrorMessage(String s) {
        ErrorMessageProvider provider = (this.errorMessageProvider != null) ? this.errorMessageProvider : (sVal, pVal) -> getDefaultErrorMessage(sVal);
        return provider.getErrorMessage(s, propertyName);
    }

    public T read(String... prompt) {
        return read(Arrays.asList(prompt));
    }

    public void checkConfiguration() throws java.lang.IllegalArgumentException {
        if(possibleValues != null && defaultValue != null) {
            if(!possibleValues.contains(defaultValue)) {
                throw new IllegalArgumentException("Invalid default value: " + defaultValue + ". Allowed values: " + possibleValues);
            }
        }
    }

    public T read(List<String> prompt) {
        checkConfiguration();
        TextTerminal textTerminal = textTerminalSupplier.get();
        while(true) {
            printPrompt(prompt, textTerminal, possibleValues);
            String sVal = textTerminal.read(inputMasking);
            if(sVal != null && inputTrimming) sVal = sVal.trim();
            if(sVal == null || sVal.isEmpty()) {
                if(defaultValue != null) return defaultValue;
            }
            if(possibleValues == null || !numberedPossibleValues) {
                ParseResult<T> result = parse(sVal);
                List<String> errMessages = result.getErrorMessages();
                if(errMessages == null) {
                    if(possibleValues == null || possibleValues.contains(result.getValue())) {
                        return result.getValue();
                    }
                    textTerminal.print(getDefaultErrorMessage());
                    textTerminal.println(" You must enter one of the displayed values.");
                    textTerminal.println( );
                } else {
                    textTerminal.println(errMessages);
                    textTerminal.println();
                }
            } else {
                try {
                    int optIndex = Integer.parseInt(sVal);
                    if(optIndex > 0 && optIndex <= possibleValues.size()) {
                        return possibleValues.get(optIndex - 1);
                    }
                } catch (NumberFormatException e) {
                    // Continue the execution. The next statement will print the error message.
                }
                textTerminal.print(getDefaultErrorMessage());
                textTerminal.println(" Enter a value between 1 and " + possibleValues.size() + ".");
                textTerminal.println();
            }
        }
    }

    protected void printPrompt(List<String> prompt, TextTerminal textTerminal, List<T> options) {
        textTerminal.print(prompt);
        boolean useColon = false;
        if(prompt != null && !prompt.isEmpty()) {
            String lastLine = prompt.get(prompt.size() - 1);
            useColon = !lastLine.isEmpty() && Character.isJavaIdentifierPart(lastLine.charAt(lastLine.length() - 1));
        }
        if(options == null) {
            if(defaultValue != null) textTerminal.print(" [" + defaultValue + "]");
            textTerminal.print(useColon ? ": " : " ");
        } else {
            textTerminal.println(useColon ? ":" : "");
            for(int i = 0; i < options.size(); i++) {
                T option = options.get(i);
                boolean isDefault = (defaultValue != null) && defaultValue.equals(option);
                textTerminal.println((isDefault ? "* ": "  ")
                        + (numberedPossibleValues ? ((i + 1) + ": ") : "")
                        + valueFormatter.apply(option));
            }
            textTerminal.print("Enter your choice: ");
        }
    }
}
