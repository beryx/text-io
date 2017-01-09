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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.beryx.textio.web.TextTerminalData.Action.*;

/**
 * A TextTerminal that allows accessing the application via a browser.
 * It works only in conjunction with a web server supporting the {@link DataApi} (such as {@link SparkDataServer})
 * and a web component that accesses this API (typically via textterm.js).
 */
public class WebTextTerminal implements TextTerminal, DataApi {
    private static final Logger logger =  LoggerFactory.getLogger(WebTextTerminal.class);

    public static final long DEFAULT_TIMEOUT_NOT_EMPTY = 5000L;
    public static final long DEFAULT_TIMEOUT_HAS_ACTION = 250L;

    private final TextTerminalData data = new TextTerminalData();
    private final Lock dataLock = new ReentrantLock();
    private final Condition dataNotEmpty = dataLock.newCondition();
    private final Condition dataHasAction = dataLock.newCondition();

    private String input;
    private final Lock inputLock = new ReentrantLock();
    private final Condition inputAvailable = inputLock.newCondition();

    private Runnable onDispose;

    private long timeoutNotEmpty = DEFAULT_TIMEOUT_NOT_EMPTY;
    private long timeoutHasAction = DEFAULT_TIMEOUT_HAS_ACTION;

    public void setTimeoutNotEmpty(long timeoutNotEmpty) {
        this.timeoutNotEmpty = timeoutNotEmpty;
    }

    public void setTimeoutHasAction(long timeoutHasAction) {
        this.timeoutHasAction = timeoutHasAction;
    }

    @Override
    public void dispose() {
        setAction(DISPOSE);
        if(onDispose != null) onDispose.run();
    }

    public void setOnDispose(Runnable onDispose) {
        this.onDispose = onDispose;
    }

    @Override
    public String read(boolean masking) {
        setAction(masking ? READ_MASKED : READ);
        inputLock.lock();
        try {
            while(true) {
                try {
                    inputAvailable.await();
                    if(input != null) {
                        String result = input;
                        input = null;
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
        if(action == NONE || action == null) throw new IllegalArgumentException("Not a proper action: " + action);
        dataLock.lock();
        try {
            if(data.getAction() != NONE) throw new IllegalStateException("data.getAction() is not NONE");
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
            String escapedMessage = StringEscapeUtils.escapeHtml4(message).replaceAll("\n", "<br>");
            data.getMessages().add(escapedMessage);
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
            return result;
        } finally {
            dataLock.unlock();
        }
    }

    @Override
    public void postUserInput(String newInput) {
        inputLock.lock();
        try {
            if(newInput == null) throw new IllegalArgumentException("newInput is null");
            if(input != null) throw new IllegalStateException("input is not null");
            input = newInput;
            inputAvailable.signalAll();
        } finally {
            inputLock.unlock();
        }
    }
}
