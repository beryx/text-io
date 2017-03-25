/*
 * Copyright 2017 the original author or authors.
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
package org.beryx.textio.web;

import org.beryx.textio.TextIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Session;

import java.util.function.Consumer;

public class SparkTextIoApp {
    private static final Logger logger =  LoggerFactory.getLogger(SparkTextIoApp.class);

    private final WebTextTerminal termTemplate;

    private final Consumer<TextIO> textIoRunner;
    private final SparkDataServer server;
    private Integer maxInactiveSeconds = null;

    private Consumer<String> onDispose;
    private Consumer<String> onAbort;

    public SparkTextIoApp(Consumer<TextIO> textIoRunner, WebTextTerminal termTemplate) {
        this.textIoRunner = textIoRunner;
        this.termTemplate = termTemplate;
        this.server = new SparkDataServer(this::getDataApi);
    }

    public SparkDataServer getServer() {
        return server;
    }

    public void setOnDispose(Consumer<String> onDispose) {
        this.onDispose = onDispose;
    }

    public void setOnAbort(Consumer<String> onAbort) {
        this.onAbort = onAbort;
    }

    public void setMaxInactiveSeconds(Integer maxInactiveSeconds) {
        this.maxInactiveSeconds = maxInactiveSeconds;
    }

    private WebTextTerminal getDataApi(String sessionId, Session session) {
        WebTextTerminal terminal = session.attribute(getSessionIdAttribute(sessionId));
        if(terminal == null) {
            logger.debug("Creating terminal for sessionId: " + sessionId);
            terminal = termTemplate.createCopy();
            terminal.setOnDispose(() -> {
                session.removeAttribute(getSessionIdAttribute(sessionId));
                if(onDispose != null) {
                    onDispose.accept(sessionId);
                }
            });
            terminal.setOnAbort(() -> {
                session.removeAttribute(getSessionIdAttribute(sessionId));
                if(onAbort != null) {
                    onAbort.accept(sessionId);
                }
            });
            session.attribute(getSessionIdAttribute(sessionId), terminal);

            if(maxInactiveSeconds != null) {
                session.maxInactiveInterval(maxInactiveSeconds);
            }

            TextIO textIO = new TextIO(terminal);

            Thread thread = new Thread(() -> textIoRunner.accept(textIO));
            thread.setDaemon(true);
            thread.start();
        } else {
            logger.trace("Terminal found for sessionId: " + sessionId + " on session with attributes " + session.attributes());
        }
        return terminal;
    }

    private String getSessionIdAttribute(String sessionId) {
        return "web-text-terminal-" + sessionId;
    }
}
