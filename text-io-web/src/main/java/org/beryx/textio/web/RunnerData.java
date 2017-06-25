package org.beryx.textio.web;

import java.util.HashMap;
import java.util.Map;

public class RunnerData {
    private final String initData;
    private final Map<String, String> sessionData = new HashMap<>();

    public RunnerData(String initData) {
        this.initData = initData;
    }

    public String getInitData() {
        return initData;
    }

    public Map<String, String> getSessionData() {
        return sessionData;
    }
}
