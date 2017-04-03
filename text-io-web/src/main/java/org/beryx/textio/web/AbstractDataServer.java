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

/**
 * Typically used as base class for {@link DataServer} implementations.
 */
public abstract class AbstractDataServer implements DataServer {
    private static final Logger logger =  LoggerFactory.getLogger(WebTextTerminal.class);
    public static final String DEFAULT_PATH_FOR_GET_DATA = "textTerminalData";
    public static final String DEFAULT_PATH_FOR_POST_INPUT  = "textTerminalInput";

    private String pathForGetData = DEFAULT_PATH_FOR_GET_DATA;
    private String pathForPostInput = DEFAULT_PATH_FOR_POST_INPUT;

    private final Gson gson = new Gson();

    public AbstractDataServer withPathForGetData(String pathForGetData) {
        this.pathForGetData = pathForGetData;
        return this;
    }
    public String getPathForGetData() {
        return pathForGetData;
    }

    public AbstractDataServer withPathForPostInput(String pathForPostInput) {
        this.pathForPostInput = pathForPostInput;
        return this;
    }
    public String getPathForPostInput() {
        return pathForPostInput;
    }

    protected String handleGetData(DataApi dataApi) {
        logger.trace("Retrieving terminal data...");
        TextTerminalData data = dataApi.getTextTerminalData();
        logger.trace("Retrieved terminal data: {}", data);
        return gson.toJson(data);
    }

    protected String handlePostInput(DataApi dataApi, String input, boolean userInterrupt) {
        if(userInterrupt) {
            logger.trace("Posting user interrupted input...");
            dataApi.postUserInterrupt(input);
        } else {
            logger.trace("Posting input...");
            dataApi.postUserInput(input);
        }
        return "OK";
    }

}
