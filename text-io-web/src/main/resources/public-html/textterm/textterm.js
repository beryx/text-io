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
 *
 *
 * -----------------------------------------------------------------------------
 *
 * Adapted from: https://github.com/AVGP/terminal.js (https://github.com/AVGP/terminal.js/blob/gh-pages/terminal.js)
 * See the original license info below.
 *
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Martin N.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
(function (root, factory) {
    'use strict';
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define([], factory);
    } else if (typeof module === 'object' && module.exports) {
        // Node.
        module.exports.createTextTerm = factory();
    } else {
        // Browser globals (root is window)
        root.createTextTerm = factory();
    }
}(this, function () {
    var createTextTerm = function TextTerm(ttElem) {
        var self = {};
        self.terminated = false;
        self.textTerminalInitPath = "/textTerminalInit";
        self.textTerminalDataPath = "/textTerminalData";
        self.textTerminalInputPath = "/textTerminalInput";

        var textTermElem;
        var inputElem;
        var promptElem;

        var action;

        var bookmarkOffsets = new Map();

        var LEVEL = {
            OFF: 0,
            ERROR: 1,
            WARN: 2,
            INFO: 3,
            DEBUG: 4,
            TRACE: 5
        };
        var logLevel = LEVEL.INFO;

        var rawLog = function(level, message) {
            if(level <= logLevel) {
                console.log(message);
            }
        };
        var logError = function(message) {rawLog(LEVEL.ERROR, message);};
        var logWarn = function(message) {rawLog(LEVEL.WARN, message);};
        var logInfo = function(message) {rawLog(LEVEL.INFO, message);};
        var logDebug = function(message) {rawLog(LEVEL.DEBUG, message);};
        var logTrace = function(message) {rawLog(LEVEL.TRACE, message);};

        logDebug("Creating new terminal.");

        var generateUUID = function() {
            var d = new Date().getTime();
            var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = (d + Math.random()*16)%16 | 0;
                d = Math.floor(d/16);
                return (c=='x' ? r : (r&0x3|0x8)).toString(16);
            });
            return uuid;
        };

        self.uuid = generateUUID();

        var history = [];
        try {
            if(localStorage.getItem("history")) {
                history = localStorage.getItem("history").split(",");
                logDebug("history retrieved from localStorage.");
            } else {
                logInfo("history not available.");
            }
        } catch(e) {
            logWarn("Cannot use localStorage. Error: " + e);
        }

        var historyIndex = history.length;

        var updateHistory = function(cmd) {
            history.push(cmd);
            try{
                localStorage.setItem("history", history);
            } catch(e) {
                logWarn("Cannot update localStorage. " * e);
            }
            historyIndex = history.length;
        };

        var KEY_UP   = 38,
            KEY_DOWN = 40;

        var browseHistory = function(target, direction) {
            var changedInput = false;
            if(direction == KEY_UP && historyIndex > 0) {
                if(action == 'READ') {
                    inputElem.textContent = history[--historyIndex];
                }
                changedInput = true;
            } else if(direction == KEY_DOWN) {
                if(action == 'READ') {
                    if(historyIndex < history.length) ++historyIndex;
                    if(historyIndex < history.length) inputElem.textContent = history[historyIndex];
                    else inputElem.textContent = "";
                }
                changedInput = true;
            }
            if(changedInput) {
                moveCaretToEnd();
            }
        };

        var moveCaretToEnd = function() {
            var range = document.createRange();
            var sel = window.getSelection();
            var childCount = inputElem.childNodes.length;
            if(childCount > 0) {
                range.setEnd(inputElem.childNodes[0], inputElem.textContent.length);
            } else {
                range.setEnd(inputElem, inputElem.textContent.length);
            }
            range.collapse(false);
            sel.removeAllRanges();
            sel.addRange(range);
        };


        var displayMessageGroups = function(messageGroups, specialPromptStyleClass) {
            var groupCount = messageGroups.length;
            logTrace("groupCount: " + groupCount);
            for(var k = 0; k < groupCount; k++) {
                var settingsCount = applySettings(messageGroups[k].settings);
                var msgCount = messageGroups[k].messages.length;
                logTrace("msgCount: " + msgCount);
                if (msgCount > 0) {
                    var newPrompt = "";
                    for (var i = 0; i < msgCount; i++) {
                        newPrompt += messageGroups[k].messages[i];
                    }
                    if(specialPromptStyleClass || settingsCount > 0) {
                        createNewTextTermPair("", specialPromptStyleClass, true);
                    }
                    promptElem.innerHTML += newPrompt;
                    textTermElem.scrollTop = textTermElem.scrollHeight;
                    inputElem.focus();
                }
            }
            if(specialPromptStyleClass) {
                createNewTextTermPair("", null, true);
                inputElem.focus();
            }
        };

        var rawHandleXhrError = function(xhr) {
            if(self.terminated) return;
            switch (xhr.status) {
                case 403:
                    self.onSessionExpired();
                    break;
                case 500:
                    self.onServerError();
                    break;
                default:
                    var level = (xhr.status >= 400) ? LEVEL.WARN : (xhr.status >= 300) ? LEVEL.INFO : LEVEL.DEBUG;
                    rawLog(level, "xhr: readyState = " + xhr.readyState + ", status = " + xhr.status);
                    if(xhr.status > 200) {
                        setTimeout(requestData, 2000);
                    }
                    break;
            }
        };

        var handleXhrError = function(xhr) {
            return (function() {
                rawHandleXhrError(xhr);
            });
        };

        var handleXhrStateChange = function(xhr) {
            return (function() {
                if((xhr.readyState == XMLHttpRequest.DONE) && (xhr.status == 200)) {
                    var data = JSON.parse(xhr.responseText);
                    self.onDataReceived(data);
                    if (data.resetRequired) {
                        self.resetTextTerm();
                    }
                    if (data.lineResetRequired) {
                        self.lineReset();
                    }
                    if (data.bookmark) {
                        self.setBookmark(data.bookmark);
                    }
                    if (data.resetToBookmark) {
                        self.resetToBookmark(data.resetToBookmark);
                    }
                    displayMessageGroups(data.messageGroups, null);
                    logTrace("data.action: " + data.action);
                    if (data.action != 'NONE') {
                        action = data.action;
                    }
                    if(action == 'FLUSH') {
                        createNewTextTermPair("", null, true);
                        inputElem.focus();
                    }
                    var textSecurity = (action == 'READ_MASKED') ? "disc" : "none";
                    inputElem.style["-webkit-text-security"] = textSecurity;
                    inputElem.style["text-security"] = textSecurity;

                    if (action == 'DISPOSE') {
                        inputElem.setAttribute("contenteditable", false);
                        self.onDispose(data.actionData);
                    } else if (action == 'ABORT') {
                        inputElem.setAttribute("contenteditable", false);
                        logTrace("Calling onAbort()...");
                        self.onAbort();
                    } else {
                        requestData();
                    }
                } else {
                    rawHandleXhrError(xhr);
                }
            });
        };

        var requestData = function() {
            if(self.terminated) return;
            var xhr = new XMLHttpRequest();
            xhr.onreadystatechange = handleXhrStateChange(xhr);
            var rnd = generateUUID();
            xhr.open("GET", self.textTerminalDataPath + "?rnd=" + rnd, true);
            xhr.setRequestHeader("uuid", self.uuid);
            xhr.send(null);
        };

        var currentInitData = null;

        var postInitData = function(initData) {
            action = undefined;
            currentInitData = initData;
            var xhr = new XMLHttpRequest();
            xhr.onreadystatechange = handleXhrStateChange(xhr);
            xhr.open("POST", self.textTerminalInitPath, true);
            xhr.setRequestHeader("Content-type", "application/json");
            xhr.setRequestHeader("uuid", self.uuid);
            xhr.send(JSON.stringify(initData));
        };

        var postAsInput = function(text, userInterrupt) {
            var xhr = new XMLHttpRequest();
            xhr.onreadystatechange = handleXhrError(xhr);
            xhr.open("POST", self.textTerminalInputPath, true);
            xhr.setRequestHeader("Content-type", "text/plain");
            xhr.setRequestHeader("uuid", self.uuid);

            if(userInterrupt) {
                logInfo("User interrupt!");
                xhr.setRequestHeader("textio-user-interrupt", "true");
            } else {
                createNewTextTermPair("<br/>", null, true);
            }
            inputElem.focus();
            xhr.send(text);
        };

        var postInput = function(userInterrupt) {
            postAsInput(inputElem.textContent, userInterrupt);
        };

        var getColor = function(colorName) {
            var color = colorName || null;
            if(color == 'default' || color == 'null' || color == 'none') {
                color = null;
            }
            return color;
        };

        var createNewTextTermPair = function(initialInnerHTML, specialPromptStyleClass, appendToTextTermElem) {
            var newParentElem = inputElem.parentNode.cloneNode(true);
            if(inputElem.textContent) {
                inputElem.setAttribute("contenteditable", false);
            } else {
                inputElem.parentNode.removeChild(inputElem);
            }

            inputElem = newParentElem.querySelector(".textterm-input");
            inputElem.style.color = getColor(self.settings.inputColor);
            inputElem.style.backgroundColor = getColor(self.settings.inputBackgroundColor);
            inputElem.style.fontWeight = (self.settings.inputBold) ? 'bold' : null;
            inputElem.style.fontStyle = (self.settings.inputItalic) ? 'italic' : null;
            inputElem.style.textDecoration = (self.settings.inputUnderline) ? 'underline' : null;
            inputElem.className = "textterm-input";
            if(self.settings.inputStyleClass) {
                inputElem.classList.add(self.settings.inputStyleClass);
            }
            inputElem.textContent = "";

            promptElem = newParentElem.querySelector(".textterm-prompt");
            promptElem.className = "textterm-prompt";
            if(specialPromptStyleClass) {
                promptElem.style = null;
                promptElem.classList.add(specialPromptStyleClass);
            } else {
                promptElem.classList.add('textterm-normal-prompt');
                promptElem.style.color = getColor(self.settings.promptColor);
                promptElem.style.backgroundColor = getColor(self.settings.promptBackgroundColor);
                promptElem.style.fontWeight = (self.settings.promptBold) ? 'bold' : null;
                promptElem.style.fontStyle = (self.settings.promptItalic) ? 'italic' : null;
                promptElem.style.textDecoration = (self.settings.promptUnderline) ? 'underline' : null;
                if(self.settings.promptStyleClass) {
                    promptElem.classList.add(self.settings.promptStyleClass);
                }
            }
            promptElem.textContent = "";
            if(initialInnerHTML) {
                promptElem.innerHTML = initialInnerHTML;
            }
            if(self.settings.paneBackgroundColor) {
                textTermElem.style.backgroundColor = self.settings.paneBackgroundColor;
            }
            if(self.settings.paneStyleClass) {
                textTermElem.className = "textterm-pane";
                textTermElem.classList.add(self.settings.paneStyleClass);
            }
            if(appendToTextTermElem) {
                textTermElem.appendChild(newParentElem);
            }
            return newParentElem;
        };

        var isUserInterruptKey = function(event) {
            var key = event.which || event.keyCode || 0;
            if(key != self.settings.userInterruptKeyCode) return false;
            if(event.ctrlKey != self.settings.userInterruptKeyCtrl) return false;
            if(event.shiftKey != self.settings.userInterruptKeyShift) return false;
            if(event.altKey != self.settings.userInterruptKeyAlt) return false;

            return true;
        };

        var initSettings = function() {
            self.settings = {};

            self.settings.userInterruptKeyCode = 'Q'.charCodeAt(0);
            self.settings.userInterruptKeyCtrl = true;
            self.settings.userInterruptKeyShift = false;
            self.settings.userInterruptKeyAlt = false;

            self.settings.promptStyleClass = "";
            self.settings.promptColor = "";
            self.settings.promptBackgroundColor = "";
            self.settings.promptBold = false;
            self.settings.promptItalic = false;
            self.settings.promptUnderline = false;

            self.settings.inputStyleClass = "";
            self.settings.inputColor = "";
            self.settings.inputBackgroundColor = "";
            self.settings.inputBold = false;
            self.settings.inputItalic = false;
            self.settings.inputUnderline = false;

            self.settings.paneStyleClass = "";
            self.settings.paneBackgroundColor = "";
        };

        var applySettings = function(settings) {
            var count = settings.length;
            for (var i = 0; i < count; i++) {
                var key = settings[i].key;
                var value = settings[i].value;
                self.settings[key] = value;
                logTrace("settings: " + key + " = " + value);
            }
            return count;
        };

        var create = function(ttElem) {
            textTermElem = ttElem;
            inputElem = ttElem.querySelector(".textterm-input");
            promptElem = ttElem.querySelector(".textterm-prompt");

            initSettings();

            self.specialKeyPressHandler = null;

            self.setLogLevelOff = function() {logLevel = LEVEL.OFF;};
            self.setLogLevelError = function() {logLevel = LEVEL.ERROR;};
            self.setLogLevelWarn = function() {logLevel = LEVEL.WARN;};
            self.setLogLevelInfo = function() {logLevel = LEVEL.INFO;};
            self.setLogLevelDebug = function() {logLevel = LEVEL.DEBUG;};
            self.setLogLevelTrace = function() {logLevel = LEVEL.TRACE;};

            self.displayMessage = function(message, specialPromptStyleClass) {
                var messageGroup = {
                    messages: [message],
                    settings: []
                };
                displayMessageGroups([messageGroup], specialPromptStyleClass);
            };

            self.displayError = function(message) {
                self.displayMessage(message, 'textterm-error-prompt');
            };

            self.onDataReceived = function(data) {
                logTrace("onDataReceived: data = " + JSON.stringify(data));
            };

            self.onDispose = function(resultData) {
                logDebug("onDispose: resultData = " + resultData);
                textTerm.terminate();
            };

            self.onAbort = function() {
                logDebug("onAbort: default implementation");
                textTerm.terminate();
            };

            var waitForEnterToRestart = function(event) {
                var key = event.which || event.keyCode || 0;
                event.preventDefault();
                if(key != 13) return;
                self.specialKeyPressHandler = null;
                self.restart();
            }

            self.onSessionExpired = function() {
                logInfo("onSessionExpired() called.");
                self.resetTextTerm();
                self.displayError("<h2>Session expired.</h2><br/>Press enter to restart.");
                self.specialKeyPressHandler = waitForEnterToRestart;
            };

            self.onServerError = function() {
                logError("onServerError() called.");
                self.resetTextTerm();
                self.displayError("<h2>Server error.</h2><br/>Press enter to restart.");
                self.specialKeyPressHandler = waitForEnterToRestart;
            };


            self.sendUserInterrupt = function() {
                postAsInput("", true);
            };

            self.resetTextTerm = function() {
                logInfo("Resetting terminal.");
                var pairs = textTermElem.querySelectorAll(".textterm-pair");
                for (var i = 0; i < pairs.length - 1; i++) {
                    textTermElem.removeChild(pairs[i]);
                }
                promptElem.textContent = "";
                inputElem.textContent = "";
                inputElem.setAttribute("contenteditable", true);
            };

            self.lineReset = function() {
                logDebug("Resetting line.");
                promptElem.textContent = "";
                inputElem.textContent = "";
            };

            self.setBookmark = function(bookmark) {
                logDebug("Setting bookmark " + bookmark);
                var pairs = textTermElem.querySelectorAll(".textterm-pair");
                if(pairs.length > 0) {
                    bookmarkOffsets.set(bookmark, pairs[pairs.length - 1]);
                }
            };

            self.resetToBookmark = function(bookmark) {
                logDebug("Resetting to bookmark " + bookmark);
                var bookmarkedPair = bookmarkOffsets.get(bookmark);
                if(bookmarkedPair) {
                    var pairs = textTermElem.querySelectorAll(".textterm-pair");
                    var bookmarkIdx = -1;
                    for (var i = 0; i < pairs.length; i++) {
                        if(pairs[i] === bookmarkedPair) {
                            bookmarkIdx = i;
                            break;
                        }
                    }
                    if(bookmarkIdx >= 0) {
                        var newParentElem = createNewTextTermPair("", null, false);
                        for (var i = bookmarkIdx; i < pairs.length; i++) {
                            textTermElem.removeChild(pairs[i]);
                        }
                        textTermElem.appendChild(newParentElem);
                    }
                }
            };

            self.execute = postInitData;

            self.restart = function() {
                postInitData(currentInitData);
            };

            ttElem.onkeyup = function(event) {
                if(historyIndex < 0) return;
                browseHistory(event.target, event.keyCode);
            };

            ttElem.onkeydown = function(event) {
                if(isUserInterruptKey(event)) {
                    postInput(true);
                    event.preventDefault();
                }
            };

            ttElem.onmouseup = function(event) {
                var inputHasFocus = (document.activeElement == inputElem);
                var sel = window.getSelection();
                var selRange = sel.getRangeAt(Math.max(sel.rangeCount, 1) - 1);
                var count = selRange.endOffset - selRange.startOffset;
                if(!count && !inputHasFocus) {
                    inputElem.focus();
                    moveCaretToEnd();
                }
            };

            var keyPressHandler = function(event) {
                if(self.specialKeyPressHandler) {
                    self.specialKeyPressHandler(event);
                } else {
                    var key = event.which || event.keyCode || 0;
                    if(key != 13) return;
                    if(action != "READ_MASKED") {
                        updateHistory(inputElem.textContent);
                    }
                    postInput(false);
                    event.preventDefault();
                }
            };
            ttElem.addEventListener("keypress", keyPressHandler);

            self.terminate = function() {
                ttElem.removeEventListener("keypress", keyPressHandler);
                self.terminated = true;
            };

            return self;
        };

        return create(ttElem);
    };

    return createTextTerm;
}));
