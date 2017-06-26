package org.beryx.textio.web;

import java.util.Collections;
import java.util.Map;

public class RunnerData {
    private final String initData;
    private Map<String, String> sessionData = Collections.emptyMap();

    public RunnerData(String initData) {
        this.initData = initData;
    }

    public String getInitData() {
        return initData;
    }

    public Map<String, String> getSessionData() {
        return sessionData;
    }
    public void setSessionData(Map<String, String> sessionData) {
        this.sessionData = sessionData;
    }
}
