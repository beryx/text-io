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
import org.beryx.textio.TextTerminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.beryx.textio.web.TextTerminalData.Action.*;

/**
 * A TextTerminal that allows accessing the application via a browser.
 * It works only in conjunction with a web server supporting the {@link DataApi} (such as {@link SparkDataServer})
 * and a web component that accesses this API (typically via textterm.js).
 */
public class WebTextTerminal implements TextTerminal<WebTextTerminal>, DataApi {
    private static final Logger logger =  LoggerFactory.getLogger(WebTextTerminal.class);

    public static final long DEFAULT_TIMEOUT_NOT_EMPTY = 5000L;
    public static final long DEFAULT_TIMEOUT_HAS_ACTION = 250L;

    private final TextTerminalData data = new TextTerminalData();
    private final Lock dataLock = new ReentrantLock();
    private final Condition dataNotEmpty = dataLock.newCondition();
    private final Condition dataHasAction = dataLock.newCondition();

    private String input;
    private boolean userInterruptedInput;
    private final Lock inputLock = new ReentrantLock();
    private final Condition inputAvailable = inputLock.newCondition();

    private Runnable onDispose;
    private Runnable onAbort;

    private long timeoutNotEmpty = DEFAULT_TIMEOUT_NOT_EMPTY;
    private long timeoutHasAction = DEFAULT_TIMEOUT_HAS_ACTION;

    private int userInterruptKeyCode = 'Q';
    private boolean userInterruptKeyCtrl = true;
    private boolean userInterruptKeyShift = false;
    private boolean userInterruptKeyAlt = false;

    public void setTimeoutNotEmpty(long timeoutNotEmpty) {
        this.timeoutNotEmpty = timeoutNotEmpty;
    }

    public void setTimeoutHasAction(long timeoutHasAction) {
        this.timeoutHasAction = timeoutHasAction;
    }

    private Consumer<WebTextTerminal> userInterruptHandler = textTerm -> {
        textTerm.abort();
        Executors.newSingleThreadScheduledExecutor().schedule(() -> System.exit(-1), 2, TimeUnit.SECONDS);
    };
    private boolean abortRead = true;

    public WebTextTerminal createCopy() {
        WebTextTerminal copy = new WebTextTerminal();
        copy.setOnDispose(this.onDispose);
        copy.setOnAbort(this.onAbort);
        copy.setTimeoutNotEmpty(this.timeoutNotEmpty);
        copy.setTimeoutHasAction(this.timeoutHasAction);
        copy.setUserInterruptKey(this.userInterruptKeyCode, this.userInterruptKeyCtrl, this.userInterruptKeyShift, this.userInterruptKeyAlt);
        copy.registerUserInterruptHandler(this.userInterruptHandler, abortRead);
        return copy;
    }

    @Override
    public void dispose() {
        setAction(DISPOSE);
        if(onDispose != null) onDispose.run();
    }

    @Override
    public void abort() {
        setAction(ABORT);
        if(onAbort != null) onAbort.run();
    }

    public void setOnDispose(Runnable onDispose) {
        this.onDispose = onDispose;
    }

    public void setOnAbort(Runnable onAbort) {
        this.onAbort = onAbort;
    }

    @Override
    public String read(boolean masking) {
        inputLock.lock();
        try {
            if(data.getAction() != ABORT) {
                setAction(masking ? READ_MASKED : READ);
            }
            while(true) {
                try {
                    logger.trace("read(): waiting for input...");
                    inputAvailable.await();
                    if(input != null) {
                        if(userInterruptedInput && (userInterruptHandler != null)) {
                            logger.debug("Calling userInterruptHandler");
                             userInterruptHandler.accept(this);
                             if(!abortRead) continue;
                        }
                        String result = input;
                        input = null;
                        userInterruptedInput = false;
                        if(logger.isTraceEnabled()) {
                            logger.trace("read: " + (masking ? result.replaceAll(".", "*") : result));
                        }
                        return result;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("read() interrupted", e);
                    return null;
                }
            }
        } finally {
            inputLock.unlock();
        }
    }

    protected void setAction(TextTerminalData.Action action) {
        if(action == NONE || action == null) {
            logger.error("Not a proper action: " + action);
            return;
        }
        dataLock.lock();
        try {
            if(data.getAction() != NONE) {
                logger.warn("data.getAction() is not NONE");
            }
            data.setAction(action);
            dataNotEmpty.signalAll();
            dataHasAction.signalAll();
        } finally {
            dataLock.unlock();
        }
    }

    @Override
    public void rawPrint(String message) {
        dataLock.lock();
        try {
            if(data.getAction() != ABORT) {
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
                data.getMessages().add(escapedMessage);
                logger.trace("rawPrint(): signaling data: {}", escapedMessage);
            }
            dataNotEmpty.signalAll();
            if(data.hasAction()) {
                dataHasAction.signalAll();
            }
        } finally {
            dataLock.unlock();
        }
    }

    @Override
    public void println() {
        rawPrint("\n");
    }

    @Override
    public boolean registerUserInterruptHandler(Consumer<WebTextTerminal> handler, boolean abortRead) {
        this.userInterruptHandler = handler;
        this.abortRead = abortRead;
        return true;
    }

    @Override
    public TextTerminalData getTextTerminalData() {
        dataLock.lock();
        try {
            try {
                dataNotEmpty.await(timeoutNotEmpty, TimeUnit.MILLISECONDS);
                dataHasAction.await(timeoutHasAction, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            TextTerminalData result = data.getCopy();
            data.clear();
            logger.trace("returning terminalData: {}", result);
            return result;
        } finally {
            dataLock.unlock();
        }
    }

    public void postUserInput(String newInput, boolean userInterrupt) {
        inputLock.lock();
        try {
            this.userInterruptedInput = userInterrupt;
            if(newInput == null) {
                if(userInterrupt) {
                    newInput = "";
                } else {
                    logger.error("newInput is null");
                    return;
                }
            }
            if(input != null) {
                logger.warn("old input is not null");
            }
            input = newInput;
            inputAvailable.signalAll();
        } finally {
            inputLock.unlock();
        }
    }

    @Override
    public void postUserInput(String newInput) {
        postUserInput(newInput, false);
    }

    @Override
    public void postUserInterrupt(String partialInput) {
        postUserInput(partialInput, true);
    }

    public void setUserInterruptKey(int code, boolean ctrl, boolean shift, boolean alt) {
        dataLock.lock();
        try {
            this.userInterruptKeyCode = code;
            this.userInterruptKeyCtrl = ctrl;
            this.userInterruptKeyShift = shift;
            this.userInterruptKeyAlt = alt;
            data.addSetting("userInterruptKeyCode", code);
            data.addSetting("userInterruptKeyCtrl", ctrl);
            data.addSetting("userInterruptKeyShift", shift);
            data.addSetting("userInterruptKeyAlt", alt);
            dataNotEmpty.signalAll();
            if(data.hasAction()) {
                dataHasAction.signalAll();
            }
        } finally {
            dataLock.unlock();
        }
    }
}
