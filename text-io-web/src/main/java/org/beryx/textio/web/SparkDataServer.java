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

import java.util.function.Function;

import static spark.Spark.*;

public class SparkDataServer {
    private static final Logger logger =  LoggerFactory.getLogger(WebTextTerminal.class);
    public final String DEFAULT_PATH_FOR_GET_DATA = "/textTerminalData";
    public final String DEFAULT_PATH_FOR_POST_INPUT  = "/textTerminalInput";

    private final Function<String, DataApi> dataApiProvider;

    private String pathForGetData = DEFAULT_PATH_FOR_GET_DATA;
    private String pathForPostInput = DEFAULT_PATH_FOR_POST_INPUT;

    private final Gson gson = new Gson();

    public SparkDataServer(Function<String, DataApi> dataApiProvider) {
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

    public void init() {
        get(pathForGetData, "application/json", (request, response) -> {
            String id = getId(request);
            DataApi dataApi = dataApiProvider.apply(id);
            return gson.toJson(dataApi.getTextTerminalData());
        });

        post(pathForPostInput, (request, response) -> {
            String id = getId(request);
            DataApi dataApi = dataApiProvider.apply(id);
            dataApi.postUserInput(request.body());
            return "OK";
        });
    }
}
