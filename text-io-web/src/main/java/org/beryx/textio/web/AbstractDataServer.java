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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Typically used as base class for {@link DataServer} implementations.
 */
public abstract class AbstractDataServer<CTX> implements DataServer {
    private static final Logger logger =  LoggerFactory.getLogger(WebTextTerminal.class);

    public static final String DEFAULT_PATH_FOR_INIT_DATA = "textTerminalInit";
    public static final String DEFAULT_PATH_FOR_GET_DATA = "textTerminalData";
    public static final String DEFAULT_PATH_FOR_POST_INPUT  = "textTerminalInput";

    private String pathForInitData = DEFAULT_PATH_FOR_INIT_DATA;
    private String pathForGetData = DEFAULT_PATH_FOR_GET_DATA;
    private String pathForPostInput = DEFAULT_PATH_FOR_POST_INPUT;

    private final Gson gson = new Gson();

    public abstract DataApiProvider<CTX> getDataApiProvider();

    public static class ResponseData {
        public final int status;
        public final String contentType;
        public final String text;

        public ResponseData(int status, String contentType, String text) {
            this.status = status;
            this.contentType = contentType;
            this.text = text;
        }
    }

    @Override
    public AbstractDataServer<CTX> withPathForInitData(String pathForInitData) {
        this.pathForInitData = pathForInitData;
        return this;
    }
    public String getPathForPostInit() {
        return pathForInitData;
    }

    @Override
    public AbstractDataServer<CTX> withPathForGetData(String pathForGetData) {
        this.pathForGetData = pathForGetData;
        return this;
    }
    public String getPathForGetData() {
        return pathForGetData;
    }

    @Override
    public AbstractDataServer<CTX> withPathForPostInput(String pathForPostInput) {
        this.pathForPostInput = pathForPostInput;
        return this;
    }
    public String getPathForPostInput() {
        return pathForPostInput;
    }

    protected ResponseData handle(Supplier<String> textSupplier) {
        try {
            return new ResponseData(200, "application/json", textSupplier.get());
        } catch (DataApiProviderException e) {
            logger.warn("Session expired", e);
            return new ResponseData(403, "text/plain", e.getMessage());
        } catch (Exception e) {
            logger.warn("Failed to handle getData", e);
            return new ResponseData(500, "text/plain", "An error occurred");
        }

    }

    protected ResponseData handleInit(CTX ctx, String initData) {
        logger.trace("Initializing terminal...");
        return handle(() -> {
            DataApi dataApi = getDataApiProvider().create(ctx, initData);
            TextTerminalData data = dataApi.getTextTerminalData();
            logger.trace("Retrieved terminal data: {}", data);
            return gson.toJson(data);
        });
    }

    protected ResponseData handleGetData(CTX ctx) {
        logger.trace("Retrieving terminal data...");
        return handle(() -> {
            DataApi dataApi = getDataApiProvider().get(ctx);
            TextTerminalData data = dataApi.getTextTerminalData();
            logger.trace("Retrieved terminal data: {}", data);
            return gson.toJson(data);
        });
    }

    protected ResponseData handlePostInput(CTX ctx, String input, boolean userInterrupt, String handlerId) {
        return handle(() -> {
            DataApi dataApi = getDataApiProvider().get(ctx);
            if(userInterrupt) {
                logger.trace("Posting user interrupted input...");
                dataApi.postUserInterrupt(input);
            } else if(StringUtils.isNotEmpty(handlerId)) {
                logger.trace("Posting handler call(" + handlerId + ")");
                dataApi.postHandlerCall(handlerId, input);
            } else {
                logger.trace("Posting input...");
                dataApi.postUserInput(input);
            }
            return gson.toJson("OK");
        });
    }
}
