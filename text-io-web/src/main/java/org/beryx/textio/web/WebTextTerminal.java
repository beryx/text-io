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

import org.apache.commons.lang3.StringUtils;
import org.beryx.textio.*;
import org.beryx.textio.web.TextTerminalData.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.beryx.textio.PropertiesConstants.*;
import static org.beryx.textio.web.TextTerminalData.Action.*;
import static org.beryx.textio.web.TextTerminalData.Action.ABORT;

/**
 * A {@link TextTerminal} that allows accessing the application via a browser.
 * It works only in conjunction with a web server supporting the {@link DataApi} (such as {@link SparkDataServer})
 * and a web component that accesses this API (typically via textterm.js).
 */
@PropertiesPrefixes({"web"})
public class WebTextTerminal extends AbstractTextTerminal<WebTextTerminal> implements DataApi {
    private static final Logger logger =  LoggerFactory.getLogger(WebTextTerminal.class);

    public static final long DEFAULT_TIMEOUT_NOT_EMPTY = 5000L;
    public static final long DEFAULT_TIMEOUT_HAS_ACTION = 250L;
    public static final long DEFAULT_TIMEOUT_DATA_CLEARED = 1000L;

    private final TextTerminalData data = new TextTerminalData();
    private final Lock dataLock = new ReentrantLock();
    private final Condition dataNotEmpty = dataLock.newCondition();
    private final Condition dataHasAction = dataLock.newCondition();
    private final Condition dataCleared = dataLock.newCondition();

    private String input;
    private boolean userInterruptedInput;
    private String handlerIdInput;
    private final Lock inputLock = new ReentrantLock();
    private final Condition inputAvailable = inputLock.newCondition();

    private Runnable onDispose;
    private Runnable onAbort;

    private long timeoutNotEmpty = DEFAULT_TIMEOUT_NOT_EMPTY;
    private long timeoutHasAction = DEFAULT_TIMEOUT_HAS_ACTION;
    private long timeoutDataCleared = DEFAULT_TIMEOUT_DATA_CLEARED;

    private int userInterruptKeyCode = 'Q';
    private boolean userInterruptKeyCtrl = true;
    private boolean userInterruptKeyShift = false;
    private boolean userInterruptKeyAlt = false;

    private final Map<String, Function<WebTextTerminal, ReadHandlerData>> registeredHandlers = new HashMap<>();

    public void setTimeoutNotEmpty(long timeoutNotEmpty) {
        this.timeoutNotEmpty = timeoutNotEmpty;
    }

    public void setTimeoutHasAction(long timeoutHasAction) {
        this.timeoutHasAction = timeoutHasAction;
    }

    public void setTimeoutDataCleared(long timeoutDataCleared) {
        this.timeoutDataCleared = timeoutDataCleared;
    }

    private Consumer<WebTextTerminal> userInterruptHandler = textTerm -> {
        textTerm.abort();
        Executors.newSingleThreadScheduledExecutor().schedule(() -> System.exit(-1), 2, TimeUnit.SECONDS);
    };
    private boolean abortRead = true;

    public WebTextTerminal() {
        TerminalProperties<WebTextTerminal> props = getProperties();
        props.addStringListener(PROP_USER_INTERRUPT_KEY, null, (term, newVal) -> setUserInterruptKey(newVal));

        props.addStringListener(PROP_PROMPT_STYLE_CLASS, null, (term, newVal) -> addSetting("promptStyleClass", newVal));
        props.addStringListener(PROP_PROMPT_COLOR, null, (term, newVal) -> addSetting("promptColor", newVal));
        props.addStringListener(PROP_PROMPT_BGCOLOR, null, (term, newVal) -> addSetting("promptBackgroundColor", newVal));
        props.addBooleanListener(PROP_PROMPT_BOLD, false, (term, newVal) -> addSetting("promptBold", newVal));
        props.addBooleanListener(PROP_PROMPT_ITALIC, false, (term, newVal) -> addSetting("promptItalic", newVal));
        props.addBooleanListener(PROP_PROMPT_UNDERLINE, false, (term, newVal) -> addSetting("promptUnderline", newVal));

        props.addStringListener(PROP_INPUT_STYLE_CLASS, null, (term, newVal) -> addSetting("inputStyleClass", newVal));
        props.addStringListener(PROP_INPUT_COLOR, null, (term, newVal) -> addSetting("inputColor", newVal));
        props.addStringListener(PROP_INPUT_BGCOLOR, null, (term, newVal) -> addSetting("inputBackgroundColor", newVal));
        props.addBooleanListener(PROP_INPUT_BOLD, false, (term, newVal) -> addSetting("inputBold", newVal));
        props.addBooleanListener(PROP_INPUT_ITALIC, false, (term, newVal) -> addSetting("inputItalic", newVal));
        props.addBooleanListener(PROP_INPUT_UNDERLINE, false, (term, newVal) -> addSetting("inputUnderline", newVal));

        props.addStringListener(PROP_PANE_BGCOLOR, null, (term, newVal) -> addSetting("paneBackgroundColor", newVal));
        props.addStringListener(PROP_PANE_STYLE_CLASS, null, (term, newVal) -> addSetting("paneStyleClass", newVal));
    }

    public WebTextTerminal createCopy() {
        WebTextTerminal copy = new WebTextTerminal();
        copy.setOnDispose(this.onDispose);
        copy.setOnAbort(this.onAbort);

        TerminalProperties<WebTextTerminal> props = copy.getProperties();
        List<TerminalProperties.ExtendedChangeListener<WebTextTerminal>> listeners = getProperties().getListeners();
        listeners.forEach(props::addListener);

        copy.setTimeoutNotEmpty(this.timeoutNotEmpty);
        copy.setTimeoutHasAction(this.timeoutHasAction);
        copy.setUserInterruptKey(this.userInterruptKeyCode, this.userInterruptKeyCtrl, this.userInterruptKeyShift, this.userInterruptKeyAlt);
        copy.registerUserInterruptHandler(this.userInterruptHandler, abortRead);

        copy.init();
        return copy;
    }

    @Override
    public void dispose(String resultData) {
        if(onDispose != null) onDispose.run();
        setAction(DISPOSE, resultData);
    }

    @Override
    public void abort() {
        if(onAbort != null) onAbort.run();
        setAction(ABORT);
    }

    @Override
    public boolean resetLine() {
        setAction(VIRTUAL);
        waitForDataCleared();
        data.setLineResetRequired(true);
        setAction(VIRTUAL);
        return true;
    }

    @Override
    public boolean moveToLineStart() {
        setAction(VIRTUAL);
        waitForDataCleared();
        data.setMoveToLineStartRequired(true);
        return true;
    }

    @Override
    public boolean setBookmark(String bookmark) {
        setAction(VIRTUAL);
        waitForDataCleared();
        data.setBookmark(bookmark);
        return true;
    }

    @Override
    public boolean resetToBookmark(String bookmark) {
        setAction(VIRTUAL);
        waitForDataCleared();
        data.setResetToBookmark(bookmark);
        return true;
    }

    private void waitForDataCleared() {
        dataLock.lock();
        try {
            try {
                if(data.hasAction()) {
                    boolean ok = dataCleared.await(timeoutDataCleared, TimeUnit.MILLISECONDS);
                    if(!ok) {
                        logger.warn("dataCleared timeout.");
                    }
                }
            } finally {
                dataLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
                    String result = input;
                    if(input != null) {
                        if(userInterruptedInput && (userInterruptHandler != null)) {
                            logger.debug("Calling userInterruptHandler");
                             userInterruptHandler.accept(this);
                             if(!abortRead) continue;
                        } else if(StringUtils.isNotEmpty(handlerIdInput)) {
                            logger.debug("Calling handler: {}", handlerIdInput);
                            String hInput = handlerIdInput;
                            handlerIdInput = null;
                            Function<WebTextTerminal, ReadHandlerData> handler = registeredHandlers.get(hInput);
                            if(handler == null) {
                                logger.error("Unknown handler: {}", hInput);
                                continue;
                            }
                            ReadHandlerData handlerData = handler.apply(this);
                            logger.debug("handlerData: {}", handlerData);
                            ReadInterruptionStrategy.Action action = handlerData.getAction();
                            switch (action) {
                                case CONTINUE:
                                    logger.debug("Setting action: CONTINUE_READ");
                                    setAction(CONTINUE_READ);
                                    continue;
                                case RESTART:
                                    ReadInterruptionData readInterruptionData = ReadInterruptionData.from(handlerData, input);
                                    throw new ReadInterruptionException(readInterruptionData, input);
                                case RETURN:
                                    setAction(FLUSH);
                                    waitForDataCleared();
                                    println();
                                    waitForDataCleared();
                                    Function<String, String> valueProvider = handlerData.getReturnValueProvider();
                                    result = (valueProvider == null) ? null : valueProvider.apply(input);
                                    setAction(CLEAR_OLD_INPUT);
                                    waitForDataCleared();
                                    break;
                                case ABORT:
                                    println();
                                    waitForDataCleared();
                                    setAction(CLEAR_OLD_INPUT);
                                    waitForDataCleared();
                                    throw new ReadAbortedException(handlerData.getPayload(), input);
                            }
                        }
                        input = null;
                        userInterruptedInput = false;
                        if(logger.isTraceEnabled()) {
                            logger.trace("read: {}", masking ? result.replaceAll(".", "*") : result);
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
        setAction(action, null);
    }

    protected void setAction(TextTerminalData.Action action, String actionData) {
        if(action == NONE || action == null) {
            logger.error("Not a proper action: {}", action);
            return;
        }
        dataLock.lock();
        try {
            TextTerminalData.Action currAction = data.getAction();
            if(currAction != NONE && currAction != FLUSH) {
                logger.warn("data.getAction() is {} in setAction({})", currAction, action);
            }
            data.setAction(action);
            data.setActionData(actionData);
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
                String escapedMessage = data.addMessage(message);
                logger.trace("rawPrint(): signalling data: {}", escapedMessage);
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
        setAction(FLUSH);
        rawPrint("\n");
    }

    @Override
    public boolean registerUserInterruptHandler(Consumer<WebTextTerminal> handler, boolean abortRead) {
        this.userInterruptHandler = handler;
        this.abortRead = abortRead;
        return true;
    }

    @Override
    public boolean registerHandler(String keyStroke, Function<WebTextTerminal, ReadHandlerData> handler) {
        KeyCombination kc = KeyCombination.of(keyStroke);
        if(kc == null) return false;

        dataLock.lock();
        try {
            String key = data.addKey(keyStroke);
            if(key != null) {
                registeredHandlers.put(key, handler);
                dataNotEmpty.signalAll();
                if(data.hasAction()) {
                    dataHasAction.signalAll();
                }
            }
        } finally {
            dataLock.unlock();
        }
        return true;
    }

    @Override
    public TextTerminalData getTextTerminalData() {
        dataLock.lock();
        try {
            try {
                if(data.isEmpty()) {
                    dataNotEmpty.await(timeoutNotEmpty, TimeUnit.MILLISECONDS);
                }
                if(!data.hasAction()) {
                    dataHasAction.await(timeoutHasAction, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            TextTerminalData result = data.getCopy();
            data.clear();
            dataCleared.signalAll();
            logger.debug("returning terminalData: {}", result);
            return result;
        } finally {
            dataLock.unlock();
        }
    }

    public void postUserInput(String newInput, boolean userInterrupt, String handlerId) {
        inputLock.lock();
        try {
            this.userInterruptedInput = userInterrupt;
            this.handlerIdInput = handlerId;
            if(newInput == null) {
                if(userInterrupt) {
                    newInput = "";
                } else if(StringUtils.isEmpty(handlerId)){
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
        postUserInput(newInput, false, null);
    }

    @Override
    public void postUserInterrupt(String partialInput) {
        postUserInput(partialInput, true, null);
    }

    @Override
    public void postHandlerCall(String handlerId, String partialInput) {
        postUserInput(partialInput, false, handlerId);
    }

    public void setUserInterruptKey(String keyStroke) {
        KeyCombination kc = KeyCombination.of(keyStroke);
        if(kc == null) {
            logger.warn("Invalid keyStroke: {}", keyStroke);
        } else {
            setUserInterruptKey(kc.getCharOrCode(), kc.isCtrlDown(), kc.isShiftDown(), kc.isAltDown());
        }
    }

    public void addSetting(String key, Object value) {
        addSettings(new KeyValue(key, value));
    }

    public void addSettings(KeyValue... keyValues) {
        logger.debug("Adding settings: {}", Arrays.asList(keyValues));
        dataLock.lock();
        try {
            for(KeyValue keyVal : keyValues) {
                data.addSetting(keyVal);
            }
            dataNotEmpty.signalAll();
            if(data.hasAction()) {
                dataHasAction.signalAll();
            }
        } finally {
            dataLock.unlock();
        }
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
