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

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Session;

import java.util.function.BiFunction;

import static spark.Spark.*;

/**
 * A SparkJava-based web server that allows clients to access the {@link DataApi}.
 */
public class SparkDataServer {
    private static final Logger logger =  LoggerFactory.getLogger(WebTextTerminal.class);
    public static final String DEFAULT_PATH_FOR_GET_DATA = "/textTerminalData";
    public static final String DEFAULT_PATH_FOR_POST_INPUT  = "/textTerminalInput";

    static {
        exception(Exception.class, (exception, request, response) -> {
            logger.error("Spark failure", exception);
        });
    }

    private final BiFunction<String, Session, DataApi> dataApiProvider;

    private String pathForGetData = DEFAULT_PATH_FOR_GET_DATA;
    private String pathForPostInput = DEFAULT_PATH_FOR_POST_INPUT;

    private final Gson gson = new Gson();

    public SparkDataServer(BiFunction<String, Session, DataApi> dataApiProvider) {
        this.dataApiProvider = dataApiProvider;
    }

    public SparkDataServer withPathForGetData(String pathForGetData) {
        this.pathForGetData = pathForGetData;
        return this;
    }

    public SparkDataServer withPathForPostInput(String pathForPostInput) {
        this.pathForPostInput = pathForPostInput;
        return this;
    }

    public SparkDataServer withPort(int portNumber) {
        port(portNumber);
        return this;
    }
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

    protected DataApi getDataApi(Request request) {
        String id = getId(request);
        return dataApiProvider.apply(id, request.session());
    }

    public void init() {
        get(pathForGetData, "application/json", (request, response) -> {
            logger.trace("Received GET");
            DataApi dataApi = getDataApi(request);
            logger.trace("Retrieving terminal data...");
            TextTerminalData data = dataApi.getTextTerminalData();
            logger.trace("Retrieved terminal data: {}", data);
            return gson.toJson(data);
        });

        post(pathForPostInput, (request, response) -> {
            logger.trace("Received POST");
            DataApi dataApi = getDataApi(request);
            boolean userInterrupt = Boolean.parseBoolean(request.headers("textio-user-interrupt"));
            String input = new String(request.body().getBytes(), "UTF-8");
            if(userInterrupt) {
                logger.trace("Posting user interrupted input...");
                dataApi.postUserInterrupt(input);
            } else {
                logger.trace("Posting input...");
                dataApi.postUserInput(input);
            }
            return "OK";
        });
    }
}
