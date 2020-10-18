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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.beryx.textio.TerminalProperties.ExtendedChangeListener;

/**
 * Interface for text-based terminals capable of reading (optionally masking the input) and writing text.
 */
public interface TextTerminal<T extends TextTerminal<T>> {
    /**
     * Reads a line of text
     * @param masking true, if the input should be masked (for example to enter a password)
     * @return the entered text
     */
    String read(boolean masking);

    /**
     * Prints the message in its raw form.
     * This method expects a single line of text.
     * The behavior is undefined if the string contains line separators.
     */
    void rawPrint(String message);

    /**
     * Terminates the current line by writing the line separator string.
     */
    void println();

    /**
     * @return the {@link TerminalProperties} of this text terminal.
     */
    TerminalProperties<T> getProperties();

    /**
     * Registers a handler that will be called in response to a user interrupt.
     * The event that triggers a user interrupt is usually Ctrl+C, but in general it is terminal specific.
     * For example, a Swing based terminal may send a user interrupt when the X close button of its window is hit.
     * Since not all terminals support this feature, the default implementation just returns false.
     * @param handler the action to be performed in response to a user interrupt.
     * @param abortRead true, if the current read operation should be aborted on user interrupt.
     * @return true, if the terminal supports this feature and the handler has been registered; false, otherwise.
     */
    default boolean registerUserInterruptHandler(Consumer<T> handler, boolean abortRead) {
        return false;
    }

    /**
     * Associates a key combination to a handler.
     * Since not all terminals support this feature, the default implementation just returns false.
     * @param keyStroke the key combination associated with the handler.
     *                  It should have the same format as the argument of {@link javax.swing.KeyStroke#getKeyStroke(String)}.
     * @param handler the action to be performed when the {@code keyStroke} is detected during a read operation.
     * @return true, if the terminal supports this feature and the handler has been associated with the given key combination; false, otherwise.
     */
    default boolean registerHandler(String keyStroke, Function<T, ReadHandlerData> handler) {
        return false;
    }

    /**
     * This method is typically called after the terminal has been created.
     * The default implementation does nothing.
     */
    default void init() {}

    /**
     * This method is typically called at the end of a text-based input/output session in order to allow the terminal to release its screen resources.
     * The terminal should be able to rebuild the released resources when a print or read method is subsequently called.
     * The default implementation does nothing.
     *
     * @param resultData stringified information about the outcome of the input/output session; may be null,
     */
    default void dispose(String resultData) {}

    /**
     * Convenience method for disposing the terminal without providing information about the outcome of the input/output session.
     * The default implementation calls {@link #dispose(String)} with a null argument.
     */
    default void dispose() {
        dispose(null);
    }

    /**
     * This method is typically called when a text-based input/output session has been aborted by the user or when a severe error occurred.
     * The default implementation does nothing.
     */
    default void abort() {}

    /**
     * Clears the current line of text.
     * Since not all terminals support this feature, the default implementation calls {@link #println()} and returns false.
     * @return true, if the terminal supports this feature and the current line has been successfully cleared.
     */
    default boolean resetLine() {
        println();
        return false;
    }

    /**
     * Moves the cursor to the start of the current line of text in order to allow overwriting the current line.
     * Since not all terminals support this feature, the default implementation calls {@link #println()} and returns false.
     * @return true, if the terminal supports this feature and the cursor has been moved to the start of the current line.
     */
    default boolean moveToLineStart() {
        println();
        return false;
    }

    /**
     * Sets a bookmark with the given name at the current position.
     * The bookmark name can be subsequently used in a call to {@link #resetToBookmark(String)} in order to clear the text after this bookmark.
     * If a bookmark with this name already exists, it will be overwritten.
     * Since not all terminals support this feature, the default implementation does nothing and returns false.
     * @return true, if the terminal supports bookmarking and the bookmark has been successfully set.
     */
    default boolean setBookmark(String bookmark) {
        return false;
    }

    /**
     * Clears the text after the given bookmark.
     * The bookmark name can be subsequently used in a call to {@link #resetToBookmark(String)} in order to clear the text after this bookmark.
     * Since not all terminals support this feature, the default implementation calls {@link #println()} and returns false.
     * @return true, if the terminal supports bookmarking, the given bookmark exists and the text after it has been successfully cleared.
     */
    default boolean resetToBookmark(String bookmark) {
        println();
        return false;
    }

    /**
     * Prints each message in the list in its raw form, inserting the line separator string between messages.
     * No separator string is printed after the last message.
     * The behavior is undefined if one or more strings in the list contain line separators.
     */
    default void rawPrint(List<String> messages) {
        if(messages != null && !messages.isEmpty()) {
            rawPrint(messages.get(0));
            messages.subList(1, messages.size()).forEach(msg -> {
                println();
                print(msg);
            });
        }
    }

    /**
     * Prints a message that possibly contains line separators.
     */
    default void print(String message) {
        List<String> messages = Arrays.asList(message.split("\\R", -1));
        rawPrint(messages);
    }

    /**
     * Prints a message that possibly contains line separators and subsequently prints a line separator.
     */
    default void println(String message) {
        print(message);
        println();
    }

    /**
     * Prints a decorated line separator.
     * @param decorator the string used to decorate the line.
     *                  It can consist of a single character (e.g. "*") or a sequence of characters (e.g. "+--").
     * @param length the number of times the decorator string should be repeated.
     */
    default void separateLineWithDecorator(String decorator, int length) {
        String separator = "";
        for (int i = 0; i < length; i++){
            separator = separator + decorator;
        }
        print(separator);
        println();
    }

    /**
     * Prints each message in the list, inserting the line separator string between messages.
     * No separator string is printed after the last message.
     * The messages in the list may contain line separators.
     */
    default void print(List<String> messages) {
        if(messages == null) return;
        List<String> rawMessages = messages.stream().flatMap(msg -> Arrays.stream(msg.split("\\R", -1))).collect(Collectors.toList());
        rawPrint(rawMessages);
    }

    /**
     * Prints each message in the list, inserting the line separator string between messages.
     * A separator string is also printed after the last message.
     * The messages in the list may contain line separators.
     */
    default void println(List<String> messages) {
        print(messages);
        println();
    }

    /**
     * Prints a formatted string using the default locale and the specified format string and arguments.
     * @param  format A format string as described in {@link java.util.Formatter}.
     * @param  args Arguments referenced by the format specifiers in the format string.
     */
    default void printf(String format, Object... args) {
        print(String.format(format, args));
    }

    /**
     * Prints a formatted string using the specified locale, format string and arguments.
     * @param l The {@linkplain java.util.Locale locale} to apply during formatting. If {@code l} is {@code null} then no localization is applied.
     * @param  format A format string as described in {@link java.util.Formatter}.
     * @param  args Arguments referenced by the format specifiers in the format string.
     */
    default void printf(Locale l, String format, Object... args) {
        print(String.format(l, format, args));
    }

    /**
     * Executes an action that returns a result, while temporarily modifying the terminal properties.
     * @param propertiesConfigurator the task that is in charge of modifying the TerminalProperties.
     * The configurator will be applied to the terminal properties before starting the action and
     * the properties will be reverted to their previous values at the end of the action.
     * @param action the action to be performed on the TextTerminal in order to obtain a result.
     *               Usually, the result is the return value of a {@link #read(boolean)} operation.
     */
    default <R> R applyWithPropertiesConfigurator(Consumer<TerminalProperties<?>> propertiesConfigurator,
                                                  Function<TextTerminal<T>, R> action) {
        LinkedList<String[]> toRestore = new LinkedList<>();
        ExtendedChangeListener listener = (term, key, oldVal, newVal) -> {
            boolean exists = toRestore.stream().filter(e -> e[0].equals(key)).findAny().isPresent();
            if (!exists) {
                toRestore.add(new String[]{key, oldVal});
            }
        };
        TerminalProperties<?> props = getProperties();
        if(propertiesConfigurator != null) {
            props.addListener(listener);
            propertiesConfigurator.accept(props);
        }
        try {
            return action.apply(this);
        } finally {
            if(propertiesConfigurator != null) {
                props.removeListener(listener);
                toRestore.forEach(pair -> props.put(pair[0], pair[1]));
            }
        }
    }

    /**
     * Executes an action while temporarily modifying the terminal properties.
     * @param propertiesConfigurator the task that is in charge of modifying the TerminalProperties.
     * The configurator will be applied to the terminal properties before starting the action and
     * the properties will be reverted to their previous values at the end of the action.
     * @param action the action to be executed, usually consisting of a series of TextTerminal-related operations.
     */
    default void executeWithPropertiesConfigurator(Consumer<TerminalProperties<?>> propertiesConfigurator,
                                                    Consumer<TextTerminal<T>> action) {
        applyWithPropertiesConfigurator(propertiesConfigurator, t -> {action.accept(t); return null;});
    }

    /**
     * A convenience method that calls {@link #executeWithPropertiesConfigurator(Consumer, Consumer)}
     * with a configurator that takes all terminal properties with the given prefix
     * and applies them after stripping the prefix from their keys.
     * <br>For example, if {@code textio.properties contains}:
     * <pre>
     *   textio.prompt.color = green
     *   textio.error.prompt.color = red
     * </pre>
     * then the following statement:
     * <pre>
     *   textTerminal.executeWithPropertiesPrefix("error", t -> t.println("Connection failed."));
     * </pre>
     * will display the message in red.
     * @param prefix the prefix of the terminal properties whose values will be temporarily used.
     * @param action the action to be performed on the TextTerminal.
     */
    default void executeWithPropertiesPrefix(String prefix, Consumer<TextTerminal<T>> action) {
        executeWithPropertiesConfigurator(t -> {
            Set<String> keys = t.getMatchingKeys(key -> key.startsWith(prefix + "."));
            int len = prefix.length() + 1;
            keys.forEach(key -> {
                String baseKey = key.substring(len);
                t.put(baseKey, t.getString(key));
            });
        }, action);
    }
}
