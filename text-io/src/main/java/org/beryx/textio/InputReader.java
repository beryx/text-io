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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A reader for values of type T.
 * It offers a fluent interface for configuring various settings such as input masking, possible values, default value, error messages etc.
 * @param <T> the type of the values that can be read by this InputReader
 * @param <B> the type of this InputReader
 */
public abstract class InputReader<T, B extends InputReader<T, B>> {
    private static final Logger logger =  LoggerFactory.getLogger(InputReader.class);

    public static final String PROPS_PREFIX_ERROR_MESSAGE = "error";

    /** Functional interface for providing error messages */
    @FunctionalInterface
    public interface ErrorMessagesProvider {
        /**
         * Returns the list of error messages for the given string representation of the value
         * @param sVal the string representation of the value
         * @param itemName the name of the item corresponding to this value. May be null.
         * @return - the list of error messages or null if no error has been detected.
         */
        List<String> getErrorMessages(String sVal, String itemName);
    }

    /** Functional interface for providing error messages for invalid index */
    @FunctionalInterface
    public interface InvalidIndexErrorMessagesProvider {
        /**
         * Returns the list of error messages for the given string representation of the value
         * @param sVal the string representation of the index entered by the user
         * @param itemName the name of the item corresponding to this value. May be null.
         * @param minIndex the minimum value allowed for the index
         * @param maxIndex the maximum value allowed for the index
         * @return - the list of error messages or null if no error has been detected.
         */
        List<String> getErrorMessages(String sVal, String itemName, int minIndex, int maxIndex);
    }

    /** Functional interface for checking value constraints */
    @FunctionalInterface
    public interface ValueChecker<T> {
        /**
         * Returns the list of error messages due to constraint violations caused by <code>val</code>
         * @param val the value for which constraint violations are checked
         * @param itemName the name of the item corresponding to this value. May be null.
         * @return - the list of error messages or null if no error has been detected.
         */
        List<String> getErrorMessages(T val, String itemName);
    }

    /**
     * A holder object returned by the {@link #parse(String)} method, containing the parsed value and/or the error messages.
     * @param <T>
     */
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

    /** Supplier of {@link TextTerminal}s */
    protected final Supplier<TextTerminal<?>> textTerminalSupplier;

    /** null, if there is no default value */
    protected T defaultValue;

    /** Non-null and non-empty, if the value to be read must be chosen from a list of allowed values. */
    protected List<T> possibleValues;

    /** If true, the list of possible values will be numbered and the desired value will be selected by choosing the corresponding number. */
    protected boolean numberedPossibleValues = false;

    /** If true, the list of possible values will be displayed on a single line. */
    protected boolean inlinePossibleValues = false;

    /** The provider of parse error messages. If null, the {@link #getDefaultErrorMessages(String)} will be used. */
    protected ErrorMessagesProvider parseErrorMessagesProvider;

    /** The provider of invalid index error messages. If null, a default message will be used. */
    protected InvalidIndexErrorMessagesProvider invalidIndexErrorMessagesProvider;

    /** The name of the item corresponding to the value to be read. May be null. */
    protected String itemName;

    /** If true, the input will be masked (useful for example when reading passwords) */
    protected boolean inputMasking = false;

    /** If true, the input will be trimmed. Default: true */
    protected boolean inputTrimming = true;

    /**
     * If true, the input reader is allowed to tweak the prompter, for example by including the default value or appending a colon at the end.
     * Default: true
     */
    protected boolean promptAdjustments = true;

    /** The list of value checkers used to detect constraint violations */
    protected final List<ValueChecker<T>> valueCheckers = new ArrayList<>();

    /** The list of value checkers used to detect constraint violations for a list of values */
    protected final List<ValueChecker<List<T>>> valueListCheckers = new ArrayList<>();

    /** The formatter used when displaying values of type T. Default: use {@link String#valueOf(Object)} */
    protected Function<T, String> valueFormatter = String::valueOf;

    /** The function used to check whether two values are equal. Default: {@link Objects#equals(Object, Object)} */
    protected BiFunction<T, T, Boolean> equalsFunc = Objects::equals;

    protected Consumer<TerminalProperties<?>> propertiesConfigurator = null;

    private boolean valueListMode = false;

    /**
     * Parses the input string
     * @param s the input string
     * @return a {@link ParseResult} that holds the parsed value and/or the error messages, if errors occurred.
     */
    protected abstract ParseResult<T> parse(String s);

    public InputReader(Supplier<TextTerminal<?>> textTerminalSupplier) {
        this.textTerminalSupplier = textTerminalSupplier;
    }

    @SuppressWarnings("unchecked")
    public B withDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withPossibleValues(T... possibleValues) {
        this.possibleValues = null;
        if(possibleValues.length > 0) {
            this.possibleValues = Arrays.asList(possibleValues);
        }
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withPossibleValues(List<T> possibleValues) {
        this.possibleValues = (possibleValues != null && possibleValues.isEmpty()) ? null : possibleValues;
        this.numberedPossibleValues = false;
        this.inlinePossibleValues = false;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withNumberedPossibleValues(T... possibleValues) {
        withPossibleValues(possibleValues);
        this.numberedPossibleValues = true;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withNumberedPossibleValues(List<T> possibleValues) {
        withPossibleValues(possibleValues);
        this.numberedPossibleValues = true;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withInlinePossibleValues(T... possibleValues) {
        withPossibleValues(possibleValues);
        this.inlinePossibleValues = true;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withInlinePossibleValues(List<T> possibleValues) {
        withPossibleValues(possibleValues);
        this.inlinePossibleValues = true;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withInputMasking(boolean inputMasking) {
        this.inputMasking = inputMasking;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withInputTrimming(boolean inputTrimming) {
        this.inputTrimming = inputTrimming;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withPromptAdjustments(boolean promptAdjustment) {
        this.promptAdjustments = promptAdjustment;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withItemName(String itemName) {
        this.itemName = "".equals(itemName) ? null : itemName;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withValueFormatter(Function<T, String> valueFormatter) {
        this.valueFormatter = valueFormatter;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withEqualsFunc(BiFunction<T, T, Boolean> equalsFunc) {
        this.equalsFunc = equalsFunc;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withParseErrorMessagesProvider(ErrorMessagesProvider parseErrorMessagesProvider) {
        this.parseErrorMessagesProvider = parseErrorMessagesProvider;
        return (B)this;
    }

    @SuppressWarnings("unchecked")
    public B withInvalidIndexErrorMessagesProvider(InvalidIndexErrorMessagesProvider invalidIndexErrorMessagesProvider) {
        this.invalidIndexErrorMessagesProvider = invalidIndexErrorMessagesProvider;
        return (B)this;
    }

    /** Adds the valueChecker passed as argument. May be called multiple times. */
    @SuppressWarnings("unchecked")
    public B withValueChecker(ValueChecker<T> valueChecker) {
        this.valueCheckers.add(valueChecker);
        return (B)this;
    }

    /** Adds the valueListChecker passed as argument. May be called multiple times. */
    @SuppressWarnings("unchecked")
    public B withValueListChecker(ValueChecker<List<T>> valueListChecker) {
        this.valueListCheckers.add(valueListChecker);
        return (B)this;
    }

    /** Sets a configurator for InputReader-specific properties.
     * The configurator will be applied to the terminal properties at the start of a read operation and
     * the properties will be reverted to their previous values at the end of the read operation.
     **/
    @SuppressWarnings("unchecked")
    public B withPropertiesConfigurator(Consumer<TerminalProperties<?>> propertiesConfigurator) {
        this.propertiesConfigurator = propertiesConfigurator;
        return (B)this;
    }

    /** Convenience method that calls {@link #withPropertiesConfigurator(Consumer)}
     * with a configurator that takes all terminal properties with the given prefix
     * and applies them after stripping the prefix from their keys.
     * <br>For example, if {@code textio.properties} contains:
     * <pre>
     *   textio.prompt.color = green
     *   textio.input.color = yellow
     *   textio.warn.prompt.color = red
     *   textio.warn.input.color = orange
     * </pre>
     * then the following statement:
     * <pre>
     *   textIO.newBooleanInputReader()
     *      .withPropertiesPrefix("warn")
     *      .read("Erase all data?");
     * </pre>
     * will display the question in red and the user input in orange.
     **/
    @SuppressWarnings("unchecked")
    public B withPropertiesPrefix(String prefix) {
        return withPropertiesConfigurator(t -> {
            Set<String> keys = t.getMatchingKeys(key -> key.startsWith(prefix + "."));
            int len = prefix.length() + 1;
            keys.forEach(key -> {
                String baseKey = key.substring(len);
                t.put(baseKey, t.getString(key));
            });
        });
    }

    /**
     * @return true, if currently reading a list of values via {@link #readList(List)}
     */
    public boolean isValueListMode() {
        return valueListMode;
    }

    /** Returns a generic error message. */
    protected String getDefaultErrorMessage(String sVal) {
        StringBuilder errBuilder = new StringBuilder("Invalid value");
        if(valueListMode) {
            errBuilder.append(" in the comma-separated list");
            if(itemName != null) errBuilder.append(" of '").append(itemName).append("'");
            if(sVal != null && !sVal.isEmpty()) errBuilder.append(": ").append(sVal);
        } else {
            if(itemName != null) errBuilder.append(" for '").append(itemName).append("'");
        }
        errBuilder.append('.');
        return errBuilder.toString();
    }

    /**
     * If no {@link #parseErrorMessagesProvider} exists, this method is used to provide the list of error messages for the input string <code>s</code>.
     * It should return a non-empty list of messages.
     */
    protected List<String> getDefaultErrorMessages(String s) {
        return new ArrayList<>(Collections.singleton(getDefaultErrorMessage(s)));
    }

    /**
     * Provides the list of error messages for the input string <code>s</code>.
     * If a {@link #parseErrorMessagesProvider} exists, it will be used. Otherwise, {@link #getDefaultErrorMessages(String)} will be called.
     */
    protected final List<String> getErrorMessages(String s) {
        if(parseErrorMessagesProvider != null) return parseErrorMessagesProvider.getErrorMessages(s, itemName);
        return getDefaultErrorMessages(s);
    }

    /**
     * Parses the input string and runs all value checkers in order to find constraint violations.
     * @param s the input string
     * @return a {@link ParseResult} that holds the parsed value and/or the error messages, if errors occurred.
     */
    protected ParseResult<T> parseAndCheck(String s) {
        ParseResult<T> res = parse(s);
        if(res.errorMessages == null) {
            List<String> allErrors = new ArrayList<>();
            for(ValueChecker<T> checker : valueCheckers) {
                List<String> errors = checker.getErrorMessages(res.value, itemName);
                if(errors != null) allErrors.addAll(errors);
            }
            if(!allErrors.isEmpty()) {
                allErrors.add(0, getDefaultErrorMessage(s));
                res = new ParseResult<>(res.value, allErrors);
            }
        }
        return res;
    }

    /**
     * Reads a value of type T.
     * It repeatedly prompts the users to enter the value, until they provide a valid input string.
     * @param prompt the messages to be displayed for prompting the user to enter the value
     * @return the value of type T parsed from the input string
     */
    public T read(String... prompt) {
        return read(Arrays.asList(prompt));
    }

    /**
     * Reads a value of type T.
     * It repeatedly prompts the user to enter a value, until a valid input string is provided.
     * @param prompt the list of messages to be displayed for prompting the user to enter the value
     * @return the value of type T parsed from the input string
     */
    public T read(List<String> prompt) {
        valueListMode = false;
        checkConfiguration();
        return executeWithTerminal(textTerminal -> {
            while(true) {
                String sVal = readWithPrompt(textTerminal, prompt);
                if(sVal != null && inputTrimming) sVal = sVal.trim();
                if(sVal == null || sVal.isEmpty()) {
                    if(defaultValue != null) return defaultValue;
                }
                T value = getValueFromStringOrIndex(sVal, textTerminal);
                if (value != null) return value;
            }
        });
    }

    public List<T> readList(String... prompt) {
        return readList(Arrays.asList(prompt));
    }

    public List<T> readList(List<String> prompt) {
        valueListMode = true;
        checkConfiguration();
        return executeWithTerminal(textTerminal -> {
            mainLoop:
            while(true) {
                String sInput = readWithPrompt(textTerminal, prompt);
                String[] sValues = (sInput == null) ? new String[0] : sInput.split(",");
                if(inputTrimming) {
                    for(int i=0; i<sValues.length; i++) sValues[i] = sValues[i].trim();
                }
                if(sValues.length == 1 && sValues[0].isEmpty()) sValues = new String[0];
                if(sValues.length == 0 && defaultValue != null) return Collections.singletonList(defaultValue);
                List<T> values = new ArrayList<>();
                for(String sVal : sValues) {
                    T value = getValueFromStringOrIndex(sVal, textTerminal);
                    if(value == null) continue mainLoop;
                    values.add(value);
                }
                List<String> allErrors = new ArrayList<>();
                for(ValueChecker<List<T>> checker : valueListCheckers) {
                    List<String> errors = checker.getErrorMessages(values, itemName);
                    if(errors != null) allErrors.addAll(errors);
                }
                if(!allErrors.isEmpty()) {
                    allErrors.add(0, getDefaultErrorMessage(null));
                    textTerminal.executeWithPropertiesPrefix(PROPS_PREFIX_ERROR_MESSAGE, t ->t.println(allErrors));
                    textTerminal.println();
                    continue;
                }
                return values;
            }
        });
    }

    protected String readWithPrompt(TextTerminal<?> textTerminal, List<String> prompt) {
        printPrompt(prompt, textTerminal);
        while(true) {
            String sInput = null;
            try {
                sInput = textTerminal.read(inputMasking);
            } catch (ReadInterruptionException e) {
                ReadInterruptionData data = e.getReadInterruptionData();
                logger.debug("ReadInterruptionException with data: " + data);
                switch (data.getAction()) {
                    case CONTINUE: logger.error("ReadInterruptionException with action CONTINUE."); // no break here: handle it as RESTART
                    case RESTART:
                        if(data.isRedrawRequired()) {
                            logger.trace("Re-printing prompt before read restart");
                            textTerminal.println();
                            printPrompt(prompt, textTerminal);
                            logger.trace("Prompt re-printed.");
                        }
                        continue;
                    case RETURN: return data.getReturnValue();
                    case ABORT: throw new ReadAbortedException(data.getPayload(), e.getPartialInput());
                }
            }
            return sInput;
        }
    }

    protected <V> V executeWithTerminal(Function<TextTerminal<?>, V> action) {
        TextTerminal textTerminal = textTerminalSupplier.get();
        return (V)textTerminal.applyWithPropertiesConfigurator(propertiesConfigurator, action);
    }

    private T getValueFromStringOrIndex(String sVal, TextTerminal<?> textTerminal) {
        if(possibleValues == null || !numberedPossibleValues) return getValueFromString(sVal, textTerminal);
        else return getValueFromIndex(sVal, textTerminal);
    }

    private T getValueFromString(String sVal, TextTerminal<?> textTerminal) {
        ParseResult<T> result = parseAndCheck(sVal);
        List<String> errMessages = result.getErrorMessages();
        if(errMessages == null) {
            Optional<T> value = getPossibleValue(result.getValue());
            if(value.isPresent()) return value.get();
            textTerminal.executeWithPropertiesPrefix(PROPS_PREFIX_ERROR_MESSAGE, t -> {
                t.print(getDefaultErrorMessage(sVal));
                if(inlinePossibleValues) {
                    String options = possibleValues.stream()
                            .map(val -> "'" + valueFormatter.apply(val) + "'")
                            .collect(Collectors.joining(", "));
                    t.println(" Please enter one of: " + options + ".");
                } else {
                    t.println(" Please enter one of the displayed values.");
                }
            });
            textTerminal.println();
        } else {
            textTerminal.executeWithPropertiesPrefix(PROPS_PREFIX_ERROR_MESSAGE, t -> t.println(errMessages));
            textTerminal.println();
        }
        return null;
    }

    private T getValueFromIndex(String sVal, TextTerminal<?> textTerminal) {
        try {
            int optIndex = Integer.parseInt(sVal);
            if(optIndex > 0 && optIndex <= possibleValues.size()) {
                return possibleValues.get(optIndex - 1);
            }
        } catch (NumberFormatException e) {
            // Continue the execution. The next statement will print the error message.
        }
        textTerminal.executeWithPropertiesPrefix(PROPS_PREFIX_ERROR_MESSAGE, t -> {
            if(invalidIndexErrorMessagesProvider != null) {
                textTerminal.println(invalidIndexErrorMessagesProvider.getErrorMessages(sVal, itemName, 1, possibleValues.size()));
            } else {
                textTerminal.print(getDefaultErrorMessage(sVal));
                textTerminal.println(" Enter a value between 1 and " + possibleValues.size() + ".");
            }
        });
        textTerminal.println();
        return null;
    }

    protected boolean isPossibleValue(T val) {
        if(possibleValues == null) return true;
        for(T pVal : possibleValues) {
            if(equalsFunc.apply(pVal, val)) return true;
        }
        return false;
    }

    protected Optional<T> getPossibleValue(T val) {
        if(possibleValues == null) return Optional.of(val);
        for(T pVal : possibleValues) {
            if(equalsFunc.apply(pVal, val)) return Optional.of(pVal);
        }
        return Optional.empty();
    }

    /**
     * Checks if the reader is correctly configured.
     * This default implementation checks if the defaultValue is among the possibleValues.
     * @throws java.lang.IllegalArgumentException
     */
    protected void checkConfiguration() throws java.lang.IllegalArgumentException {
        if(defaultValue != null && !isPossibleValue(defaultValue)) {
            throw new IllegalArgumentException("Invalid default value: " + valueFormatter.apply(defaultValue) + ". Allowed values: " + possibleValues);
        }
        for(ValueChecker<T> checker : valueCheckers) {
            List<String> errors;
            if(defaultValue != null) {
                errors = checker.getErrorMessages(defaultValue, itemName);
                if(errors != null) throw new IllegalArgumentException("Invalid default value: " + valueFormatter.apply(defaultValue) + ".\n" + errors);
            }
            if(possibleValues != null) {
                for(T val : possibleValues) {
                    errors = checker.getErrorMessages(val, itemName);
                    if(errors != null) throw new IllegalArgumentException("Invalid entry in the list of possible values: " + valueFormatter.apply(val) + ".\n" + errors);
                }
            }
        }
    }

    /**
     * Displays a prompt inviting the user to enter a value.
     * @param prompt the list of prompt messages. May be null.
     * @param textTerminal the text terminal to which the messages are sent.
     */
    protected void printPrompt(List<String> prompt, TextTerminal<?> textTerminal) {
        textTerminal.print(prompt);
        boolean useColon = false;
        if(promptAdjustments && prompt != null && !prompt.isEmpty()) {
            String lastLine = prompt.get(prompt.size() - 1);
            useColon = shouldappendColon(lastLine);
        }
        if(possibleValues == null) {
            if(promptAdjustments && defaultValue != null) textTerminal.print(" [" + valueFormatter.apply(defaultValue) + "]: ");
            else textTerminal.print(useColon ? ": " : " ");
        } else if(promptAdjustments) {
            int optionCount = possibleValues.size();
            if(inlinePossibleValues) {
                String sValues = IntStream.range(0, optionCount)
                        .mapToObj(i -> possibleValues.get(i))
                        .map(option -> {
                            boolean isDefault = (defaultValue != null) && equalsFunc.apply(defaultValue, option);
                            return (isDefault ? "*": "") + valueFormatter.apply(option);
                        })
                        .collect(Collectors.joining(", ", " (", "): "));
                textTerminal.print(sValues);
            } else {
                textTerminal.println(useColon ? ":" : "");
                for(int i = 0; i < optionCount; i++) {
                    T option = possibleValues.get(i);
                    boolean isDefault = (defaultValue != null) && equalsFunc.apply(defaultValue, option);
                    String optionId = "";
                    String optionText = valueFormatter.apply(option);
                    if(numberedPossibleValues) {
                        int digits = ("" + optionCount).length();
                        optionId = String.format("%"+ digits + "d: ", i + 1);
                        String[] textLines = optionText.split("\\R", -1);
                        if(textLines.length > 1) {
                            String delimiter = String.format("\n%" + (digits + 4) + "s", "");
                            optionText = Arrays.stream(textLines).collect(Collectors.joining(delimiter));
                        }
                    }
                    textTerminal.println((isDefault ? "* ": "  ") + optionId + optionText);
                }
                textTerminal.print(valueListMode ? "Enter your choices as comma-separated values: " : "Enter your choice: ");
            }
        }
    }

    private static boolean shouldappendColon(String s) {
        if(s == null || s.isEmpty()) return false;
        char lastChar = s.charAt(s.length() - 1);
        return "()[]{}".indexOf(lastChar) > 0 || Character.isJavaIdentifierPart(lastChar);
    }

    public static <T> ValueChecker<List<T>> nonEmptyListChecker() {
        return (list, propName) -> {
            if(list == null || list.isEmpty()) return Collections.singletonList("Expected at least one element.");
            else return null;
        };
    }

    public static <T> ValueChecker<List<T>> noDuplicatesChecker() {
        return (list, propName) -> {
            if(list == null || list.size() < 2) return null;
            Set<T> valueSet = new HashSet<>(list);
            if(valueSet.size() < list.size()) return Collections.singletonList("Duplicate values are not allowed.");
            return null;
        };
    }
}
