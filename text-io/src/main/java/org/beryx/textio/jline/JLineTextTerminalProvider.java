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
package org.beryx.textio.jline;

import jline.console.ConsoleReader;
import jline.internal.Configuration;
import org.beryx.textio.TextTerminalProvider;

/**
 * If {@link System#console()} is not null and a ConsoleReader can be created, it provides a {@link JLineTextTerminal}.
 */
public class JLineTextTerminalProvider implements TextTerminalProvider {
    public JLineTextTerminal getTextTerminal() {
        if(System.console() == null) return null;
        try {
            ConsoleReader reader = new ConsoleReader();
            boolean expandEvents = Configuration.getBoolean(ConsoleReader.JLINE_EXPAND_EVENTS, false);
            reader.setExpandEvents(expandEvents);
            return new JLineTextTerminal(reader);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "JLine terminal";
    }
}
