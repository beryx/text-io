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

import javax.swing.*;
import java.awt.event.KeyEvent;

public class KeyCombination {
    private final KeyStroke keyStroke;

    private KeyCombination(KeyStroke keyStroke) {
        this.keyStroke = keyStroke;
    }

    public static KeyCombination of(String s) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(s);
        if(keyStroke == null) return null;
        return new KeyCombination(keyStroke);
    }

    public int getCode() {
        return keyStroke.getKeyCode();
    }

    public char getChar() {
        return keyStroke.getKeyChar();
    }

    public boolean isTyped() {
        return (keyStroke.getKeyEventType() == KeyEvent.KEY_TYPED);
    }

    public int getCharOrCode() {
        return isTyped() ? getChar() : getCode();
    }

    public boolean isShiftDown() {
        return (keyStroke.getModifiers() & (KeyEvent.SHIFT_DOWN_MASK | KeyEvent.SHIFT_MASK)) != 0;
    }

    public boolean isCtrlDown() {
        return (keyStroke.getModifiers() & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.CTRL_MASK)) != 0;
    }

    public boolean isAltDown() {
        return (keyStroke.getModifiers() & (KeyEvent.ALT_DOWN_MASK | KeyEvent.ALT_MASK)) != 0;
    }
}
