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

import org.beryx.textio.TextIO;
import org.beryx.textio.web.SparkDataServer;
import org.beryx.textio.web.SparkTextIoApp;

import java.awt.*;
import java.net.URI;
import java.util.function.Consumer;

import static spark.Spark.staticFiles;
import static spark.Spark.stop;

/**
 * Allows executing code in a {@link org.beryx.textio.web.WebTextTerminal}.
 * by configuring and initializing a {@link SparkDataServer}.
 */
public class WebTextIoExecutor {
    private int port = -1;

    public WebTextIoExecutor withPort(int port) {
        this.port = port;
        return this;
    }

    public void execute(Consumer<TextIO> textIoRunner) {
        SparkTextIoApp app = new SparkTextIoApp(textIoRunner);
        app.setOnDispose(sessionId -> {
            new Thread(() -> {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stop();
            }).start();
        });

        SparkDataServer server = app.getServer();
        if(port > 0) {
            server.withPort(port);
        }

        staticFiles.location("/public-html");
        server.init();

        String url = "http://localhost:" + server.getPort() + "/web-demo.html";
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
