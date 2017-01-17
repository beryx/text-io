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

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SparkTextIoApp {
    private static final Logger logger =  LoggerFactory.getLogger(SparkTextIoApp.class);

    private final Map<String, WebTextTerminal> dataApiMap = new HashMap<>();

    private final Consumer<TextIO> textIoRunner;
    private final SparkDataServer server;
    private Integer maxInactiveSeconds = null;

    private Consumer<String> onDispose;

    public SparkTextIoApp(Consumer<TextIO> textIoRunner) {
        this.textIoRunner = textIoRunner;
        this.server = new SparkDataServer(this::getDataApi);
    }

    public SparkDataServer getServer() {
        return server;
    }

    public void setOnDispose(Consumer<String> onDispose) {
        this.onDispose = onDispose;
    }

    public void setMaxInactiveSeconds(Integer maxInactiveSeconds) {
        this.maxInactiveSeconds = maxInactiveSeconds;
    }

    private final WebTextTerminal getDataApi(String sessionId, Session session) {
        synchronized (dataApiMap) {
            WebTextTerminal terminal = dataApiMap.get(sessionId);
            if(terminal == null) {
                terminal = new WebTextTerminal();
                if(onDispose != null) {
                    terminal.setOnDispose(() -> onDispose.accept(sessionId));
                }
                dataApiMap.put(sessionId, terminal);

                if(maxInactiveSeconds != null) {
                    session.maxInactiveInterval(maxInactiveSeconds);
                }
                session.attribute("web-text-io-session-id", new SessionIdListener(sessionId));

                TextIO textIO = new TextIO(terminal);

                Thread thread = new Thread(() -> textIoRunner.accept(textIO));
                thread.setDaemon(true);
                thread.start();
            }
            return terminal;
        }
    }

    private class SessionIdListener implements HttpSessionBindingListener {
        private final String sessionId;

        SessionIdListener(String sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public void valueBound(HttpSessionBindingEvent event) {
            // Nothing to do
        }

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            WebTextTerminal removed;
            synchronized (dataApiMap) {
                removed = dataApiMap.remove(sessionId);
            }
            if(removed == null) {
                logger.warn("Unbinding sessionId {}: not found in dataApiMap", sessionId);
            } else {
                logger.trace("Unbinding sessionId {}: removed from dataApiMap", sessionId);
            }
        }
    }
}
