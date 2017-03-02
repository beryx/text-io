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
import jline.console.UserInterruptException;
import org.beryx.textio.AbstractTextTerminal;
import org.beryx.textio.TextTerminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * A JLine-based {@link TextTerminal}.
 */
public class JLineTextTerminal extends AbstractTextTerminal<JLineTextTerminal> {
    private static final Logger logger =  LoggerFactory.getLogger(JLineTextTerminal.class);
    private static final Consumer<JLineTextTerminal> DEFAULT_USER_INTERRUPT_HANDLER = textTerm -> System.exit(-1);

    private final ConsoleReader reader;
    private Consumer<JLineTextTerminal>userInterruptHandler = DEFAULT_USER_INTERRUPT_HANDLER;
    private boolean abortRead = true;

    public JLineTextTerminal(ConsoleReader reader) {
        if(reader == null) throw new IllegalArgumentException("reader is null");
        reader.setHandleUserInterrupt(true);
        this.reader = reader;
    }

    @Override
    public String read(boolean masking) {
        String prefix = "";
        Character mask = masking ? '*' : null;
        while(true) {
            try {
                return prefix + reader.readLine(mask);
            } catch(UserInterruptException e) {
                userInterruptHandler.accept(this);
                prefix = prefix + e.getPartialLine();
                if(abortRead) return prefix;
            } catch (IOException e) {
                logger.error("read error.", e);
                return "";
            }
        }
    }

    @Override
    public void rawPrint(String message) {
        try {
            reader.setPrompt(message);
            reader.drawLine();
            reader.flush();
        } catch (IOException e) {
            logger.error("print error.", e);
        } finally {
            reader.setPrompt(null);
        }
    }

    @Override
    public void println() {
        try {
            reader.println();
            reader.flush();
        } catch (IOException e) {
            logger.error("println error.", e);
        }
    }

    @Override
    public boolean registerUserInterruptHandler(Consumer<JLineTextTerminal> handler, boolean abortRead) {
        this.userInterruptHandler = (handler != null) ? handler : DEFAULT_USER_INTERRUPT_HANDLER;
        this.abortRead = abortRead;
        return true;
    }

    public ConsoleReader getReader() {
        return reader;
    }
}
