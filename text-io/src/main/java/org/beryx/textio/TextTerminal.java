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

import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
     * @return an observable map of properties of this text terminal.
     */
    ObservableMap<String, String> getProperties();

    /**
     * Registers a handler that will be called in response to a user interrupt.
     * The event that triggers a user interrupt is usually Ctrl+C, but in general it is terminal specific.
     * For example, a Swing based terminal may send a user interrupt when the X close button of its window is hit.
     * If a terminal is not able to register user interrupt handlers, it should return false.
     * @param handler the action to be performed in response to a user interrupt.
     * @param abortRead true, if the current read operation should be aborted on user interrupt.
     * @return true, if the handler has been registered; false, otherwise.
     */
    boolean registerUserInterruptHandler(Consumer<T> handler, boolean abortRead);

    /**
     * This method is typically called after the terminal has been created.
     * The default implementation does nothing.
     */
    default void init() {}

    /**
     * This method is typically called at the end of a text-based input/output session in order to allow the terminal to release its screen resources.
     * The terminal should be able to rebuild the released resources when a print or read method is subsequently called.
     * The default implementation does nothing.
     */
    default void dispose() {}

    /**
     * This method is typically called when a text-based input/output session has been aborted by the user or when a severe error occurred.
     * The default implementation does nothing.
     */
    default void abort() {}

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
     * Convenience method that provides the String value associated with a specified property key or a default value.
     * @param key the key of the property to be retrieved.
     * @return the corresponding value or a {@code defaultValue} if no property is associated with the specified {@code key}.
     */
    default String getStringProperty(String key, String defaultValue) {
        String val = getProperties().get(key);
        if(val == null) return defaultValue;
        val = val.trim();
        if(val.isEmpty()) return defaultValue;
        return val;
    }

    /**
     * Convenience method that provides the int value associated with a specified property key or a default value.
     * @param key the key of the property to be retrieved.
     * @return the corresponding value or a {@code defaultValue} if no property is associated with the specified {@code key}.
     */
    default int getIntProperty(String key, int defaultValue) {
        String sVal = getStringProperty(key, null);
        if(sVal == null) return defaultValue;
        try {
            return Integer.parseInt(sVal);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Convenience method that provides the long value associated with a specified property key or a default value.
     * @param key the key of the property to be retrieved.
     * @return the corresponding value or a {@code defaultValue} if no property is associated with the specified {@code key}.
     */
    default long getLongProperty(String key, long defaultValue) {
        String sVal = getStringProperty(key, null);
        if(sVal == null) return defaultValue;
        try {
            return Long.parseLong(sVal);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Convenience method that provides the double value associated with a specified property key or a default value.
     * @param key the key of the property to be retrieved.
     * @return the corresponding value or a {@code defaultValue} if no property is associated with the specified {@code key}.
     */
    default double getDoubleProperty(String key, double defaultValue) {
        String sVal = getStringProperty(key, null);
        if(sVal == null) return defaultValue;
        try {
            return Double.parseDouble(sVal);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Convenience method that provides the boolean value associated with a specified property key or a default value.
     * @param key the key of the property to be retrieved.
     * @return the corresponding value or a {@code defaultValue} if no property is associated with the specified {@code key}.
     */
    default boolean getBooleanProperty(String key, boolean defaultValue) {
        String sVal = getStringProperty(key, null);
        if(sVal == null) return defaultValue;
        return Boolean.parseBoolean(sVal);
    }

    /**
     * Convenience method for setting the value of a property with the specified key.
     * @param key the key of the property to be set.
     * @param value the value to be associated with the specified {@code key}.
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
     */
    default String setProperty(String key, String value) {
        return getProperties().put(key, value);
    }

    /**
     * Convenience method that adds a listener for the property with the specified key.
     * @param key the key of the property for which the listener is added.
     * @param listener the listener to be added, as a {@link BiConsumer} that accepts the arguments ({@code oldValue}, {@code newValue}).
     */
    default void addPropertyChangeListener(String key, BiConsumer<String, String> listener) {
        if(key == null) return;
        getProperties().addListener((MapChangeListener<String,String>) (change -> {
            if(key.equals(change.getKey())) {
                listener.accept(change.getValueRemoved(), change.getValueAdded());
            }
        }));
    }
}
