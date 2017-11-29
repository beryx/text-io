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

public class ReadInterruptionData extends ReadInterruptionStrategy<ReadInterruptionData> {
    private String returnValue;

    public ReadInterruptionData(Action action) {
        super(action);
        if(action == Action.CONTINUE) {
            throw new IllegalArgumentException("Action CONTINUE not allowed for ReadInterruptionData.");
        }
    }

    public static ReadInterruptionData from(ReadHandlerData handlerData, String partialInput) {
        Function<String, String> valueProvider = handlerData.getReturnValueProvider();
        String retVal = (valueProvider == null) ? null : valueProvider.apply(partialInput);
        return new ReadInterruptionData(handlerData.getAction())
                .withRedrawRequired(handlerData.isRedrawRequired())
                .withPayload(handlerData.getPayload())
                .withReturnValue(retVal);
    }

    public ReadInterruptionData withReturnValue(String returnValue) {
        this.returnValue = returnValue;
        return this;
    }
    public String getReturnValue() {
        return returnValue;
    }

    @Override
    public String toString() {
        return super.toString() + ", returnValue: " + returnValue;
    }
}
