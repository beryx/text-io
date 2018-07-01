/*
 * Copyright 2016 the original author or authors.
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
package org.beryx.textio.swing;

import org.beryx.textio.TextTerminalProvider;

import java.awt.*;

/**
 * If the system is not headless, it provides a {@link SwingTextTerminal}.
 */
public class SwingTextTerminalProvider implements TextTerminalProvider {
    public SwingTextTerminal getTextTerminal() {
        if (isHeadless()) return null;
        try {
            return new SwingTextTerminal();
        } catch(Exception e) {
            return null;
        }
    }

    private static boolean isHeadless() {
        if (GraphicsEnvironment.isHeadless()) return true;
        try {
            GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            return screenDevices == null || screenDevices.length == 0;
        } catch (HeadlessException e) {
            return true;
        }
    }

    @Override
    public String toString() {
        return "Swing terminal";
    }
}
