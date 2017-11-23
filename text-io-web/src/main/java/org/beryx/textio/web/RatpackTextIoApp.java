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
import org.beryx.textio.web.RatpackDataServer.ContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.session.Session;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class RatpackTextIoApp implements TextIoApp<RatpackTextIoApp> {
    private static final Logger logger =  LoggerFactory.getLogger(RatpackTextIoApp.class);

    private final WebTextTerminal termTemplate;

    private final BiConsumer<TextIO, RunnerData> textIoRunner;
    private final RatpackDataServer server;

    private Consumer<String> onDispose;
    private Consumer<String> onAbort;

    private Cache<String, WebTextTerminal> webTextTerminalCache;
    private int maxInactiveSeconds = 600;

    private Function<Session, Map<String,String>> sessionDataProvider = session -> Collections.emptyMap();

    public RatpackTextIoApp(BiConsumer<TextIO, RunnerData> textIoRunner, WebTextTerminal termTemplate) {
        this.textIoRunner = textIoRunner;
        this.termTemplate = termTemplate;
        this.server = new RatpackDataServer(this::create, this::get);
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
                        logger.debug("removed from cache: {}. Remaining entries: {}", notification.getKey(), webTextTerminalCache.size()))
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

    public RatpackTextIoApp withSessionDataProvider(Function<Session, Map<String,String>> provider) {
        this.sessionDataProvider = provider;
        return this;
    }

    protected DataApi create(RatpackDataServer.ContextHolder ctxHolder, String initData) {
        String textTermSessionId = ctxHolder.contextId;

        logger.debug("Creating terminal for textTermSessionId: {}", textTermSessionId);
        WebTextTerminal terminal = termTemplate.createCopy();
        String mapKey = getSessionIdMapKey(textTermSessionId);
        terminal.setOnDispose(() -> {
            if(onDispose != null) {
                onDispose.accept(textTermSessionId);
            }
            Executors.newSingleThreadScheduledExecutor().schedule(() -> webTextTerminalCache.invalidate(mapKey), 5, TimeUnit.SECONDS);
        });
        terminal.setOnAbort(() -> {
            if(onAbort != null) {
                onAbort.accept(textTermSessionId);
            }
            Executors.newSingleThreadScheduledExecutor().schedule(() -> webTextTerminalCache.invalidate(mapKey), 5, TimeUnit.SECONDS);
        });
        webTextTerminalCache.put(mapKey, terminal);
        TextIO textIO = new TextIO(terminal);

        RunnerData runnerData = createRunnerData(initData, ctxHolder);
        Thread thread = new Thread(() -> textIoRunner.accept(textIO, runnerData));
        thread.setDaemon(true);
        thread.start();
        webTextTerminalCache.cleanUp();
        return terminal;
    }

    private RunnerData createRunnerData(String initData, ContextHolder ctxHolder) {
        RunnerData runnerData = new RunnerData(initData);
        Session session = ctxHolder.context.get(Session.class);
        Map<String, String> sessionData = sessionDataProvider.apply(session);
        runnerData.setSessionData(sessionData);
        return runnerData;
    }

    protected DataApi get(ContextHolder ctxHolder) {
        String textTermSessionId = ctxHolder.contextId;
        String mapKey = getSessionIdMapKey(textTermSessionId);
        WebTextTerminal terminal = webTextTerminalCache.getIfPresent(mapKey);
        if(terminal == null) {
            throw new DataApiProviderException("Unknown session: " + textTermSessionId);
        }
        webTextTerminalCache.cleanUp();
        return terminal;
    }

    private static String getSessionIdMapKey(String textTermSessionId) {
        return "web-text-terminal-" + textTermSessionId;
    }
}
