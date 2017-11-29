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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.func.Action;
import ratpack.guice.BindingsSpec;
import ratpack.guice.Guice;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.http.Request;
import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfigBuilder;
import ratpack.session.Session;
import ratpack.session.SessionModule;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A Ratpack-based web server that allows clients to access the {@link DataApi}.
 */
public class RatpackDataServer extends AbstractDataServer<Context> {
    private static final Logger logger =  LoggerFactory.getLogger(RatpackDataServer.class);

    private int port;
    private String baseDir;

    private final BiFunction<ContextHolder, String, DataApi> dataApiCreator;
    private final Function<ContextHolder, DataApi> dataApiGetter;

    private final DataApiProvider<Context> dataApiProvider = new DataApiProvider<Context>() {
        @Override
        public DataApi create(Context context, String initData) {
            return dataApiCreator.apply(getContextHolder(context), initData);
        }

        @Override
        public DataApi get(Context context) {
            return dataApiGetter.apply(getContextHolder(context));
        }
    };

    public static class ContextHolder {
        public final String contextId;
        public final Context context;

        public ContextHolder(String contextId, Context context) {
            this.contextId = contextId;
            this.context = context;
        }
    }

    private static ContextHolder getContextHolder(Context ctx) {
        return new ContextHolder(getId(ctx), ctx);
    }

    @Override
    public DataApiProvider<Context> getDataApiProvider() {
        return dataApiProvider;
    }

    protected final Action<Chain> handlerPostInit = chain ->
            chain.post(getPathForPostInit(), ctx -> {
                logger.trace("Received INIT");
                Request request = ctx.getRequest();
                request.getBody().then(req -> {
                    String initData = req.getText(StandardCharsets.UTF_8);
                    sendResponseData(ctx, handleInit(ctx, initData));
                });
            });
    protected final Action<Chain> handlerGetData =  chain ->
            chain.get(getPathForGetData(), ctx -> {
                logger.trace("Received GET");
                sendResponseData(ctx, handleGetData(ctx));
            });

    protected final Action<Chain> handlerPostInput =  chain ->
            chain.post(getPathForPostInput(), ctx -> {
                logger.trace("Received POST");
                Request request = ctx.getRequest();
                boolean userInterrupt = Boolean.parseBoolean(request.getHeaders().get("textio-user-interrupt"));
                String handlerId = request.getHeaders().get("textio-handler-id");
                request.getBody().then(req -> {
                    String text = req.getText(StandardCharsets.UTF_8);
                    sendResponseData(ctx, handlePostInput(ctx, text, userInterrupt, handlerId));
                });
            });

    protected final Action<Chain> handlerTexttermAssets =  chain ->
            chain.get("textterm/:name", ctx -> {
                String resName = "/public-html/textterm/" + ctx.getPathTokens().get("name");
                String content = getResourceContent(resName).orElse(null);
                if(content == null) {
                    ctx.next();
                } else {
                    String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(resName);
                    if(contentType != null) {
                        ctx.getResponse().contentType(contentType);
                    }
                    ctx.getResponse().send(content);
                }
            });

    protected final Action<Chain> handlerStaticAssets =  chain ->
            chain.files(files -> {
                if(baseDir != null) {
                    files.dir(baseDir);
                }
            });

    private final List<Action<Chain>> handlers = new ArrayList<>(Arrays.asList(
            handlerPostInit,
            handlerGetData,
            handlerPostInput,
            handlerTexttermAssets,
            handlerStaticAssets
    ));

    private final List<Action<BindingsSpec>> bindings = new ArrayList<>(Collections.singletonList(
            b -> b.module(SessionModule.class)
    ));


    protected final Action<ServerConfigBuilder> portConfigurator = c -> {
        if (port > 0) {
            c.port(port);
        }
    };

    protected final Action<ServerConfigBuilder> baseDirConfigurator = c -> {
        if(baseDir != null) {
            c.baseDir(BaseDir.find(baseDir));
        }
    };

    private final List<Action<ServerConfigBuilder>> configurators = new ArrayList<>(Arrays.asList(
            portConfigurator,
            baseDirConfigurator
    ));

    public RatpackDataServer(BiFunction<ContextHolder, String, DataApi> dataApiCreator, Function<ContextHolder, DataApi> dataApiGetter) {
        this.dataApiCreator = dataApiCreator;
        this.dataApiGetter = dataApiGetter;
    }

    public List<Action<Chain>> getHandlers() {
        return handlers;
    }

    public List<Action<BindingsSpec>> getBindings() {
        return bindings;
    }

    public List<Action<ServerConfigBuilder>> getConfigurators() {
        return configurators;
    }

    public RatpackDataServer withBaseDir(String baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    @Override
    public RatpackDataServer withPort(int portNumber) {
        this.port = portNumber;
        return this;
    }
    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void init() {
        try {
            RatpackServer.start(server -> {
                server.serverConfig(c -> {
                    for(Action<ServerConfigBuilder> cfg : getConfigurators()) {
                        c = cfg.with(c);
                    }
                });
                server.registry(Guice.registry(b -> {
                    for(Action<BindingsSpec> binding : getBindings()) {
                        b = binding.with(b);
                    }
                }));
                server.handlers(chain -> {
                    for(Action<Chain> handler : getHandlers()) {
//                        chain = chain.insert(handler);
                        chain = handler.with(chain);
                    }
                });
            });
        } catch (Exception e) {
            logger.error("Ratpack failure", e);
        }
    }

    protected void sendResponseData(Context ctx, ResponseData r) {
        ctx.getResponse()
                .status(r.status)
                .contentType(r.contentType)
                .send(r.text);
    }

    protected Optional<String> getResourceContent(String resourceName) {
        URL url = getClass().getResource(resourceName);
        if(url == null) return Optional.empty();
        return getUrlContent(url);
    }

    protected Optional<String> getUrlContent(URL url) {
        try(Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8.name())) {
            return Optional.of(scanner.useDelimiter("\\A").next());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    protected static String getId(Context ctx) {
        String id = ctx.get(Session.class).getId();
        String uuid = ctx.getRequest().getHeaders().get("uuid");
        if(uuid != null) {
            id += "-" + uuid;
        }
        logger.trace("id: {}", id);
        return id;
    }
}
