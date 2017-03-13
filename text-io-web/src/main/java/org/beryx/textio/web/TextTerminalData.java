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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The data sent by the server to a polling web component.
 * Includes:<ul>
 *     <li>an action to be executed by the web component (NONE, READ, READ_MASKED, DISPOSE or ABORT).</li>
 *     <li>a boolean value indicating whether the terminal should reset its settings before performing the specified action.</li>
 *     <li>a list of {@link MessageGroup}s, each one consisting of a list of settings (represented as {@link KeyValue}s) and a list of prompt messages.</li>
 * </ul>
 */
public class TextTerminalData {
    private static final Logger logger =  LoggerFactory.getLogger(TextTerminalData.class);

    public enum Action {NONE, READ, READ_MASKED, DISPOSE, ABORT}

    /** A key-value pair */
    public static class KeyValue {
        /** The key of this pair */
        public final String key;

        /** The value of this pair */
        public final Object value;

        public KeyValue(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + ": " + value;
        }
    }

    /**
     * A group of messages sharing the same settings
     */
    public static class MessageGroup {
        /** The settings of the messages in this group, as a list of {@link KeyValue} pairs */
        public final List<KeyValue> settings = new ArrayList<>() ;

        /** The list of messages in this group */
        public final List<String> messages = new ArrayList<>();

        @Override
        public String toString() {
            return "settings: " + settings + ", messages: " + messages;
        }
    }

    private final List<MessageGroup> messageGroups = new ArrayList<>();
    private Action action = Action.NONE;
    private boolean resetRequired = true;

    public TextTerminalData getCopy() {
        TextTerminalData data = new TextTerminalData();

        messageGroups.forEach(group -> {
            MessageGroup copyGroup = new MessageGroup();
            copyGroup.settings.addAll(group.settings);
            copyGroup.messages.addAll(group.messages);
            data.messageGroups.add(copyGroup);
        });
        data.action = action;
        data.resetRequired = resetRequired;
        return data;
    }

    public List<MessageGroup> getMessageGroups() {
        return messageGroups;
    }

    public MessageGroup newMessageGroup() {
        MessageGroup group = new MessageGroup();
        messageGroups.add(group);
        return group;
    }

    public boolean isNewGroupRequiredForSetting() {
        if(messageGroups.isEmpty()) return true;
        return !messageGroups.get(messageGroups.size() - 1).messages.isEmpty();
    }

    public void addSetting(String key, Object value) {
        addSetting(new KeyValue(key, value));
    }

    public void addSetting(KeyValue keyVal) {
        MessageGroup group = (isNewGroupRequiredForSetting()) ? newMessageGroup() : messageGroups.get(messageGroups.size() - 1);
        int size = group.settings.size();
        for(int i = 0; i < size; i++) {
            if(group.settings.get(i).key.equals(keyVal.key)) {
                group.settings.set(i, keyVal);
                return;
            }
        }
        group.settings.add(keyVal);
    }

    public void addMessage(String message) {
        MessageGroup group = (messageGroups.isEmpty()) ? newMessageGroup() : messageGroups.get(messageGroups.size() - 1);
        group.messages.add(message);
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
        return messageGroups.isEmpty() && (action == Action.NONE);
    }

    public boolean hasAction() {
        return (action != Action.NONE);
    }

    public void clear() {
        messageGroups.clear();
        action = Action.NONE;
        resetRequired = false;
    }

    @Override
    public String toString() {
        return "resetRequired: " + resetRequired +
                ", action: " + action +
                ", messageGroups: " + messageGroups;
    }
}
