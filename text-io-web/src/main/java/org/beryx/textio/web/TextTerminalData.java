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

import java.util.ArrayList;
import java.util.List;

/**
 * The data sent by the server to a polling web component.
 * Includes a list of prompt messages and an action to be executed by the web component (NONE, READ, READ_MASKED, DISPOSE).
 */
public class TextTerminalData {
    public enum Action {NONE, READ, READ_MASKED, DISPOSE}

    private final List<String> messages = new ArrayList<>();
    private Action action = Action.NONE;
    private boolean resetRequired = true;

    public TextTerminalData getCopy() {
        TextTerminalData data = new TextTerminalData();
        data.messages.addAll(messages);
        data.action = action;
        data.resetRequired = resetRequired;
        return data;
    }

    public List<String> getMessages() {
        return messages;
    }

    public Action getAction() {
        return action;
    }
    public void setAction(Action action) {
        this.action = action;
    }

    public boolean isResetRequired() {
        return resetRequired;
    }

    public void setResetRequired(boolean resetRequired) {
        this.resetRequired = resetRequired;
    }

    public boolean isEmpty() {
        return messages.isEmpty() && (action == Action.NONE);
    }

    public boolean hasAction() {
        return (action != Action.NONE);
    }

    public void clear() {
        messages.clear();
        action = Action.NONE;
        resetRequired = false;
    }

    @Override
    public String toString() {
        return "resetRequired: " + resetRequired +
                ", messages: " + messages +
                ", action: " + action;
    }
}
