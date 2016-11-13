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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface for text-based terminals capable of reading (optionally masking the input) and writing text.
 */
public interface TextTerminal {
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
     * This method is typically called at the end of a text-based input/output session in order to allow the terminal to release its screen resources.
     * The terminal should be able to rebuild the released resources when a print or read method is subsequently called.
     * The default implementation does nothing.
     */
    default void dispose() {}

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
}
