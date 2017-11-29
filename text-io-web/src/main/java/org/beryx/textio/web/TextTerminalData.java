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

import org.apache.commons.lang3.StringEscapeUtils;
import org.beryx.textio.KeyCombination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The data sent by the server to a polling web component.
 * Includes:<ul>
 *     <li>an action to be executed by the web component (NONE, VIRTUAL, FLUSH, READ, READ_MASKED, CONTINUE_READ, CLEAR_OLD_INPUT, DISPOSE or ABORT).</li>
 *     <li>a boolean value indicating whether the terminal should reset its settings before performing the specified action.</li>
 *     <li>a list of {@link MessageGroup}s, each one consisting of a list of settings (represented as {@link KeyValue}s) and a list of prompt messages.</li>
 * </ul>
 */
public class TextTerminalData {
    public enum Action {NONE, VIRTUAL, FLUSH, READ, READ_MASKED, CONTINUE_READ, CLEAR_OLD_INPUT, DISPOSE, ABORT}

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

    public static class Key {
        public final String id;
        public final String key;
        public final int keyCode;
        public final boolean ctrlKey;
        public final boolean shiftKey;
        public final boolean altKey;

        public Key(String id, String key, int keyCode, boolean ctrlKey, boolean shiftKey, boolean altKey) {
            this.id = id;
            this.key = key;
            this.keyCode = keyCode;
            this.ctrlKey = ctrlKey;
            this.shiftKey = shiftKey;
            this.altKey = altKey;
        }

        public static Key of(String s) {
            KeyCombination kc = KeyCombination.of(s);
            if(kc == null) return null;
            String id = s.replaceAll("\\s", "-");
            String key = String.valueOf((char)kc.getCharOrCode()).toLowerCase();
            return new Key(id, key, kc.getCode(), kc.isCtrlDown(), kc.isShiftDown(), kc.isAltDown());
        }
    }

    private final List<MessageGroup> messageGroups = new ArrayList<>();
    private Action action = Action.NONE;
    private String actionData = null;
    private boolean resetRequired = true;
    private boolean lineResetRequired = false;
    private boolean moveToLineStartRequired = false;
    private String bookmark = null;
    private String resetToBookmark = null;
    private final List<Key> handlerKeys = new ArrayList<>();

    public TextTerminalData getCopy() {
        TextTerminalData data = new TextTerminalData();

        messageGroups.forEach(group -> {
            MessageGroup copyGroup = new MessageGroup();
            copyGroup.settings.addAll(group.settings);
            copyGroup.messages.addAll(group.messages);
            data.messageGroups.add(copyGroup);
        });
        data.action = action;
        data.actionData = actionData;
        data.resetRequired = resetRequired;
        data.lineResetRequired = lineResetRequired;
        data.moveToLineStartRequired = moveToLineStartRequired;
        data.bookmark = bookmark;
        data.resetToBookmark = resetToBookmark;
        data.handlerKeys.addAll(handlerKeys);
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

    public void addRawMessage(String message) {
        MessageGroup group = (messageGroups.isEmpty()) ? newMessageGroup() : messageGroups.get(messageGroups.size() - 1);
        group.messages.add(message);
    }

    public String addMessage(String message) {
        String escapedMessage = StringEscapeUtils.escapeHtml4(message);
        escapedMessage = Arrays.stream(escapedMessage.split("\\R", -1))
                .map(line -> line.replaceAll("\t", "    "))
                .map(line -> {
                    int count = 0;
                    while(count < line.length() && line.charAt(count) == ' ') count++;
                    if(count == 0) return line;
                    StringBuilder sb = new StringBuilder(line.length() + 5 * count);
                    for(int i = 0; i < count; i++) {
                        sb.append("&nbsp;");
                    }
                    sb.append(line.substring(count));
                    return sb.toString();
                })
                .collect(Collectors.joining("<br>"));
        addRawMessage(escapedMessage);
        return escapedMessage;
    }

    public Action getAction() {
        return action;
    }
    public void setAction(Action action) {
        this.action = action;
    }

    public String getActionData() {
        return actionData;
    }
    public void setActionData(String actionData) {
        this.actionData = actionData;
    }

    public boolean isResetRequired() {
        return resetRequired;
    }
    public void setResetRequired(boolean resetRequired) {
        this.resetRequired = resetRequired;
    }

    public boolean isLineResetRequired() {
        return lineResetRequired;
    }
    public void setLineResetRequired(boolean lineResetRequired) {
        this.lineResetRequired = lineResetRequired;
    }

    public boolean isMoveToLineStartRequired() {
        return moveToLineStartRequired;
    }

    public void setMoveToLineStartRequired(boolean moveToLineStartRequired) {
        this.moveToLineStartRequired = moveToLineStartRequired;
    }

    public String getBookmark() {
        return bookmark;
    }
    public void setBookmark(String bookmark) {
        this.bookmark = bookmark;
    }

    public String getResetToBookmark() {
        return resetToBookmark;
    }

    public void setResetToBookmark(String resetToBookmark) {
        this.resetToBookmark = resetToBookmark;
    }

    public boolean isEmpty() {
        return messageGroups.isEmpty() && (action == Action.NONE);
    }

    public boolean hasAction() {
        return (action != Action.NONE);
    }

    public List<Key> getHandlerKeys() {
        return handlerKeys;
    }

    public void addKey(Key key) {
        for(int i=0; i<handlerKeys.size(); i++) {
            Key k = handlerKeys.get(i);
            if(k.id.equals(key.id)) {
                handlerKeys.set(i, key);
                return;
            }
        }
        handlerKeys.add(key);
    }

    public String addKey(String keyStroke) {
        Key key = Key.of(keyStroke);
        if(key == null) return null;
        addKey(key);
        return key.id;
    }

    public void clear() {
        messageGroups.clear();
        action = Action.NONE;
        actionData = null;
        resetRequired = false;
        lineResetRequired = false;
        moveToLineStartRequired = false;
        bookmark = null;
        resetToBookmark = null;
        handlerKeys.clear();
    }

    @Override
    public String toString() {
        return "resetRequired: " + resetRequired +
                ", lineResetRequired: " + lineResetRequired +
                ", moveToLineStartRequired: " + moveToLineStartRequired +
                ", bookmark: " + bookmark +
                ", resetToBookmark: " + resetToBookmark +
                ", handlerKeys: " + handlerKeys +
                ", action: " + action +
                ", actionData: " + actionData +
                ", messageGroups: " + messageGroups;
    }
}
