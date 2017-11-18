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
package org.beryx.textio.demo.app;

import com.google.gson.Gson;
import org.beryx.textio.TextTerminal;

public class AppUtil {
    public static void printGsonMessage(TextTerminal<?> terminal, String initData) {
        if(initData != null && !initData.isEmpty()) {
            String message = new Gson().fromJson(initData, String.class);
            if(message != null && !message.isEmpty()) {
                terminal.println(message);
            }
        }
    }
}
