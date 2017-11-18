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
package org.beryx.textio.demo;

import org.beryx.textio.web.TextIoApp;

import java.awt.*;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Allows executing code in a {@link org.beryx.textio.web.WebTextTerminal}
 * by configuring and initializing a {@link TextIoApp}.
 */
public class WebTextIoExecutor {
    private Integer port;

    public WebTextIoExecutor withPort(int port) {
        this.port = port;
        return this;
    }

    public void execute(TextIoApp<?> app) {
        Consumer<String> stopServer = sessionId -> Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            System.exit(0);
        }, 2, TimeUnit.SECONDS);

        app.withOnDispose(stopServer)
            .withOnAbort(stopServer)
            .withPort(port)
            .withMaxInactiveSeconds(600)
            .withStaticFilesLocation("public-html")
            .init();

        String url = "http://localhost:" + app.getPort() + "/web-demo.html";
        boolean browserStarted = false;
        if(Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
                browserStarted = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(!browserStarted) {
            System.out.println("Please open the following link in your browser: " + url);
        }
    }
}
