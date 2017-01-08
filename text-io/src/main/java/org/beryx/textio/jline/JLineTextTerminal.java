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
import org.beryx.textio.TextTerminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A JLine-based {@link TextTerminal}.
 */
public class JLineTextTerminal implements TextTerminal {
    private static final Logger logger =  LoggerFactory.getLogger(JLineTextTerminal.class);

    private final ConsoleReader reader;

    public JLineTextTerminal(ConsoleReader reader) {
        if(reader == null) throw new IllegalArgumentException("reader is null");
        this.reader = reader;
    }

    @Override
    public String read(boolean masking) {
        Character mask = masking ? '*' : null;
        try {
            return reader.readLine(mask);
        } catch (IOException e) {
            logger.error("read error.", e);
            return "";
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

    public ConsoleReader getReader() {
        return reader;
    }
}
