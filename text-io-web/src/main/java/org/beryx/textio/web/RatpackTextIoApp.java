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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import org.beryx.textio.TextIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RatpackTextIoApp implements TextIoApp<RatpackTextIoApp> {
    private static final Logger logger =  LoggerFactory.getLogger(RatpackTextIoApp.class);

    private final WebTextTerminal termTemplate;

    private final Consumer<TextIO> textIoRunner;
    private final RatpackDataServer server;

    private Consumer<String> onDispose;
    private Consumer<String> onAbort;

    private Cache<String, WebTextTerminal> webTextTerminalCache;
    private int maxInactiveSeconds = 600;

    public RatpackTextIoApp(Consumer<TextIO> textIoRunner, WebTextTerminal termTemplate) {
        this.textIoRunner = textIoRunner;
        this.termTemplate = termTemplate;
        this.server = new RatpackDataServer(this::getDataApi);
    }

    public RatpackDataServer getServer() {
        return server;
    }

    @Override
    public void init() {
        webTextTerminalCache  =
                CacheBuilder.newBuilder()
                .expireAfterAccess(maxInactiveSeconds, TimeUnit.SECONDS)
                .removalListener((RemovalListener<String, WebTextTerminal>) notification ->
                        logger.debug("removed from cache: " + notification.getKey() + ". Remaining entries: " + webTextTerminalCache.size()))
                .build();
        server.init();
    }

    public RatpackTextIoApp withOnDispose(Consumer<String> onDispose) {
        this.onDispose = onDispose;
        return this;
    }

    public RatpackTextIoApp withOnAbort(Consumer<String> onAbort) {
        this.onAbort = onAbort;
        return this;
    }

    public RatpackTextIoApp withMaxInactiveSeconds(Integer maxInactiveSeconds) {
        if(maxInactiveSeconds != null) {
            this.maxInactiveSeconds = maxInactiveSeconds;
        }
        return this;
    }

    @Override
    public RatpackTextIoApp withStaticFilesLocation(String location) {
        server.withBaseDir(location);
        return this;
    }

    @Override
    public RatpackTextIoApp withPort(Integer portNumber) {
        if(portNumber != null) {
            server.withPort(portNumber);
        }
        return this;
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    private WebTextTerminal getDataApi(String textTermSessionId) {
        String mapKey = getSessionIdMapKey(textTermSessionId);
        WebTextTerminal terminal = webTextTerminalCache.getIfPresent(mapKey);
        if(terminal == null) {
            logger.debug("Creating terminal for textTermSessionId: " + textTermSessionId);
            terminal = termTemplate.createCopy();
            terminal.setOnDispose(() -> {
                webTextTerminalCache.invalidate(mapKey);
                if(onDispose != null) {
                    onDispose.accept(textTermSessionId);
                }
            });
            terminal.setOnAbort(() -> {
                webTextTerminalCache.invalidate(mapKey);
                if(onAbort != null) {
                    onAbort.accept(textTermSessionId);
                }
            });
            webTextTerminalCache.put(mapKey, terminal);
            TextIO textIO = new TextIO(terminal);

            Thread thread = new Thread(() -> textIoRunner.accept(textIO));
            thread.setDaemon(true);
            thread.start();
        } else {
            logger.trace("Terminal found for mapKey: " + mapKey);
        }
        webTextTerminalCache.cleanUp();
        return terminal;
    }

    private String getSessionIdMapKey(String textTermSessionId) {
        return "web-text-terminal-" + textTermSessionId;
    }
}
