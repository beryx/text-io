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

import org.beryx.textio.web.SparkDataServer;
import org.beryx.textio.web.SparkTextIoApp;

import java.awt.*;
import java.net.URI;
import java.time.Month;

import static spark.Spark.staticFiles;
import static spark.Spark.stop;

public class WebTextIoDemo {
    public static void main(String[] args) {
        SparkTextIoApp app = new SparkTextIoApp(textIO -> {
            String user = textIO.newStringInputReader()
                    .withDefaultValue("admin")
                    .read("Username");

            String password = textIO.newStringInputReader()
                    .withMinLength(6)
                    .withInputMasking(true)
                    .read("Password");

            int age = textIO.newIntInputReader()
                    .withMinVal(13)
                    .read("Age");

            Month month = textIO.newEnumInputReader(Month.class)
                    .read("What month were you born in?");

            textIO.getTextTerminal().printf("\nUser %s is %d years old, was born in %s and has the password %s.\n", user, age, month, password);

            textIO.newStringInputReader().withMinLength(0).read("\nPress enter to terminate...");
            textIO.dispose();
        });

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

        if(args.length > 0) {
            try {
                if(args.length > 1) throw new IllegalArgumentException("Too many arguments.");
                int port = Integer.parseInt(args[0]);
                server.withPort(port);
            } catch (Exception e) {
                System.err.println("This program accepts an optional int argument denoting the port");
            }
        }

        staticFiles.location("/public-html");
        server.init();

        if(Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:" + server.getPort() + "/web-demo.html"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
