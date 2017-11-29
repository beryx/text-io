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

/**
 * API for the data exchanged between browser and server.
 */
public interface DataApi {
    /** This method is called by the web component while polling for data */
    TextTerminalData getTextTerminalData();

    /** This method is called by the web component to post the user input */
    void postUserInput(String input);

    /**
     * This method is called by the web component in response to a user interrupt (typically triggered by typing Ctrl+Q).
     * @param partialInput the partially entered input when the user interrupt occurred.
     */
    void postUserInterrupt(String partialInput);

    /**
     * This method is called by the web component in response to a handler call (triggered by typing its associated key combination).
     * @param handlerId the id of the handler to be called.
     * @param partialInput the partially entered input when the hander call occurred.
     */
    void postHandlerCall(String handlerId, String partialInput);
}
