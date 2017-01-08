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

import org.beryx.textio.TextTerminal;
import org.beryx.textio.TextTerminalProvider;

/**
 * It provides a {@link WebTextTerminal} connected to a {@link SparkDataServer}.
 */
public class SparkWebTextTerminalProvider implements TextTerminalProvider {
    public static final String WTT_TIMEOUT_NOT_EMPTY = "wttTimeoutNotEmpty";
    public static final String WTT_TIMEOUT_HAS_ACTION = "wttTimeoutHasAction";
    public static final String WTT_PORT = "wttPort";
    public static final String WTT_PATH_FOR_GET_DATA = "wttPathForGetData";
    public static final String WTT_PATH_FOR_POST_INPUT = "wttPathForPostInput";

    public TextTerminal getTextTerminal() {

        // TODO - FIXME
        return null;


//        WebTextTerminal terminal = new WebTextTerminal();
//
//        String propWttTimeoutNotEmpty = System.getProperty(WTT_TIMEOUT_NOT_EMPTY);
//        if(propWttTimeoutNotEmpty != null) {
//            terminal.setTimeoutNotEmpty(Long.parseLong(propWttTimeoutNotEmpty));
//        }
//
//        String propWttTimeoutHasAction = System.getProperty(WTT_TIMEOUT_HAS_ACTION);
//        if(propWttTimeoutHasAction != null) {
//            terminal.setTimeoutHasAction(Long.parseLong(propWttTimeoutHasAction));
//        }
//
//        SparkDataServer server = new SparkDataServer(terminal);
//
//        String propWttPort = System.getProperty(WTT_PORT);
//        if(propWttPort != null) {
//            server.withPort(Integer.parseInt(propWttPort));
//        }
//
//        String propWttPathForGetData = System.getProperty(WTT_PATH_FOR_GET_DATA);
//        if(propWttPathForGetData != null) {
//            server.withPathForGetData(propWttPathForGetData);
//        }
//
//        String propWttPathForPostInput = System.getProperty(WTT_PATH_FOR_POST_INPUT);
//        if(propWttPathForPostInput != null) {
//            server.withPathForPostInput(propWttPathForPostInput);
//        }
//
//        server.init();
//        return terminal;
    }
}
