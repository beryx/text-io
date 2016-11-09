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

public interface TextTerminal {
    String read(boolean masking);
    void rawPrint(String message);
    void println();
    default void dispose() {}

    default void rawPrint(List<String> messages) {
        if(messages != null && !messages.isEmpty()) {
            rawPrint(messages.get(0));
            messages.subList(1, messages.size()).forEach(msg -> {
                println();
                print(msg);
            });
        }
    }

    default void print(String message) {
        List<String> messages = Arrays.asList(message.split("\\R", -1));
        rawPrint(messages);
    }

    default void println(String message) {
        print(message);
        println();
    }

    default void print(List<String> messages) {
        if(messages == null) return;
        List<String> rawMessages = messages.stream().flatMap(msg -> Arrays.stream(msg.split("\\R", -1))).collect(Collectors.toList());
        rawPrint(rawMessages);
    }

    default void println(List<String> messages) {
        print(messages);
        println();
    }
}
