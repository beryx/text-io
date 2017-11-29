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
import spark.Request;
import spark.Response;
import spark.Session;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import java.util.function.Function;

import static spark.Spark.*;

/**
 * A SparkJava-based web server that allows clients to access the {@link DataApi}.
 */
public class SparkDataServer extends AbstractDataServer<Request> {
    private static final Logger logger =  LoggerFactory.getLogger(SparkDataServer.class);
    static {
        exception(Exception.class, (exception, request, response) -> logger.error("Spark failure", exception));
    }

    private final BiFunction<SessionHolder, String, DataApi> dataApiCreator;
    private final Function<SessionHolder, DataApi> dataApiGetter;

    private final DataApiProvider<Request> dataApiProvider = new DataApiProvider<Request>() {
        @Override
        public DataApi create(Request request, String initData) {
            return dataApiCreator.apply(getSessionHolder(request), initData);
        }

        @Override
        public DataApi get(Request request) {
            return dataApiGetter.apply(getSessionHolder(request));
        }
    };


    public static class SessionHolder {
        public final String sessionId;
        public final Session session;

        public SessionHolder(String sessionId, Session session) {
            this.sessionId = sessionId;
            this.session = session;
        }
    }

    private static SessionHolder getSessionHolder(Request r) {
        return new SessionHolder(getId(r), r.session());
    }

    @Override
    public DataApiProvider<Request> getDataApiProvider() {
        return dataApiProvider;
    }

    public SparkDataServer(BiFunction<SessionHolder, String, DataApi> dataApiCreator, Function<SessionHolder, DataApi> dataApiGetter) {
        this.dataApiCreator = dataApiCreator;
        this.dataApiGetter = dataApiGetter;
    }

    @Override
    public SparkDataServer withPort(int portNumber) {
        port(portNumber);
        return this;
    }
    @Override
    public int getPort() {
        return port();
    }

    protected static String getId(Request request) {
        Session session = request.session();
        String id = session.id();
        String uuid = request.headers("uuid");
        if(uuid != null) {
            id += "-" + uuid;
        }
        logger.trace("id: {}", id);
        return id;
    }

    protected String configureResponseData(Response response, ResponseData r) {
        response.status(r.status);
        response.type(r.contentType);
        response.body(r.text);
        return r.text;
    }

    @Override
    public void init() {
        post("/" + getPathForPostInit(), (request, response) -> {
            logger.trace("Received INIT");
            String initData = new String(request.bodyAsBytes(), StandardCharsets.UTF_8);
            return configureResponseData(response, handleInit(request, initData));
        });

        get("/" + getPathForGetData(), "application/json", (request, response) -> {
            logger.trace("Received GET");
            return configureResponseData(response, handleGetData(request));
        });

        post("/" + getPathForPostInput(), (request, response) -> {
            logger.trace("Received POST");
            boolean userInterrupt = Boolean.parseBoolean(request.headers("textio-user-interrupt"));
            String handlerId = request.headers("textio-handler-id");
            String input = new String(request.body().getBytes(), StandardCharsets.UTF_8);
            return configureResponseData(response, handlePostInput(request, input, userInterrupt, handlerId));
        });
    }
}
