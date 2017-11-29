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

public class ReadInterruptionStrategy<T extends ReadInterruptionStrategy<T>> {
    public enum Action {CONTINUE, RESTART, RETURN, ABORT}

    private final Action action;
    private boolean redrawRequired;
    private String payload;

    public ReadInterruptionStrategy(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public T withRedrawRequired(boolean redrawRequired) {
        this.redrawRequired = redrawRequired;
        return (T)this;
    }

    public boolean isRedrawRequired() {
        return redrawRequired;
    }

    public T withPayload(String payload) {
        this.payload = payload;
        return (T)this;
    }

    public String getPayload() {
        return payload;

    }
    @Override
    public String toString() {
        return "action: " + action
                + ", redrawRequired: " + redrawRequired
                + ", payload: " + payload;
    }
}
