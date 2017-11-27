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
package org.beryx.textio;

import java.util.function.Function;

public class ReadHandlerData extends ReadInterruptionStrategy<ReadHandlerData> {
    private Function<String, String> returnValueProvider;

    public ReadHandlerData(Action action) {
        super(action);
    }

    public ReadHandlerData withReturnValueProvider(Function<String, String> returnValueProvider) {
        this.returnValueProvider = returnValueProvider;
        return this;
    }
    public Function<String, String> getReturnValueProvider() {
        return returnValueProvider;
    }
}
