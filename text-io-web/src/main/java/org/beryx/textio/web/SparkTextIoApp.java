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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SparkTextIoApp {
    private final Map<String, WebTextTerminal> dataApiMap = new HashMap<>();

    private final Consumer<TextIO> textIoRunner;
    private final SparkDataServer server;

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

    private final WebTextTerminal getDataApi(String sessionId) {
        synchronized (dataApiMap) {
            WebTextTerminal terminal = dataApiMap.get(sessionId);
            if(terminal == null) {
                terminal = new WebTextTerminal();
                if(onDispose != null) {
                    terminal.setOnDispose(() -> onDispose.accept(sessionId));
                }
                dataApiMap.put(sessionId, terminal);

                TextIO textIO = new TextIO(terminal);

                Thread thread = new Thread(() -> textIoRunner.accept(textIO));
                thread.setDaemon(true);
                thread.start();
            }
            return terminal;
        }
    }
}
