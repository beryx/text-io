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

import org.beryx.textio.console.ConsoleTextTerminalProvider;
import org.beryx.textio.jline.JLineTextTerminalProvider;
import org.beryx.textio.swing.SwingTextTerminalProvider;
import org.beryx.textio.system.SystemTextTerminalProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Provides {@link TextTerminal} and {@link TextIO} implementations.
 * <br>The concrete {@link TextTerminal} implementation is obtained as follows:
 * <ol>
 *     <li>If the system property {@value #TEXT_TERMINAL_CLASS_PROPERTY} is defined, then it is
 *         taken to be the fully-qualified name of a concrete {@link TextTerminal} class.
 *         The class is loaded and instantiated. If this process fails, then the next step is executed.</li>
 *     <li>a {@link ServiceLoader} loads the configured {@link TextTerminalProvider}s and searches for the
 *         first one capable to provide a {@link TextTerminal} instance.
 *         If none is found, then the next step is executed.</li>
 *     <li>A default implementation is provided as follows:
 *          <ul>
 *              <li>if {@link System#console()} is not null, and a JLine ConsoleReader can be created, then a {@link org.beryx.textio.jline.JLineTextTerminal} is provided.</li>
 *              <li>else, if {@link System#console()} is not null, then a {@link org.beryx.textio.console.ConsoleTextTerminal} is provided.</li>
 *              <li>else, if the system is not headless, then a {@link org.beryx.textio.swing.SwingTextTerminal} is provided.</li>
 *              <li>else, a {@link org.beryx.textio.system.SystemTextTerminal} is provided</li>
 *          </ul>
 *     </li>
 * </ol>
 */
public class TextIoFactory {
    private static final Logger logger =  LoggerFactory.getLogger(TextIoFactory.class);

    public static final String TEXT_TERMINAL_CLASS_PROPERTY = "org.beryx.textio.TextTerminal";

    private static class Holder {
        static Holder INSTANCE = new Holder();

        final TextTerminal<?> terminal;
        final TextIO textIO;

        private Holder() {
            TextTerminal<?> t = getTerminalFromProperty();
            if(t == null) {
                t = getTerminalFromService();
            }
            if(t == null) {
                t = getDefaultTerminal();
            }
            t.init();
            this.terminal = t;
            this.textIO = new TextIO(t);
        }

        private TextTerminal<?> getTerminalFromProperty() {
            String clsName = System.getProperty(TEXT_TERMINAL_CLASS_PROPERTY, "").trim();
            if(clsName.isEmpty()) return null;
            try {
                Class<?> cls = Class.forName(clsName);
                return (TextTerminal<?>) cls.newInstance();
            } catch(Exception e) {
                logger.warn("Unable to create a TextTerminal of type {}", clsName);
                return null;
            }
        }

        private TextTerminal<?> getTerminalFromService() {
            ServiceLoader<TextTerminalProvider> svcLoader = ServiceLoader.load(TextTerminalProvider.class);
            Iterator<TextTerminalProvider> it = svcLoader.iterator();
            while(it.hasNext()) {
                TextTerminal<?> t = it.next().getTextTerminal();
                if(t != null) return t;
            }
            return null;
        }

        private TextTerminal<?> getDefaultTerminal() {
            TextTerminal<?> terminal = new JLineTextTerminalProvider().getTextTerminal();
            if(terminal != null) return terminal;
            terminal = new ConsoleTextTerminalProvider().getTextTerminal();
            if(terminal != null) return terminal;
            terminal = new SwingTextTerminalProvider().getTextTerminal();
            if(terminal != null) return terminal;
            terminal = new SystemTextTerminalProvider().getTextTerminal();
            return terminal;
        }
    }

    public static TextTerminal<?> getTextTerminal() {
        return Holder.INSTANCE.terminal;
    }

    public static TextIO getTextIO() {
        return Holder.INSTANCE.textIO;
    }
}
