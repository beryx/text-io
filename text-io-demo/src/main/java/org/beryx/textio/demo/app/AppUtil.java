package org.beryx.textio.demo.app;

import com.google.gson.Gson;
import org.beryx.textio.TextTerminal;

public class AppUtil {
    public static void printGsonMessage(TextTerminal terminal, String initData) {
        if(initData != null && !initData.isEmpty()) {
            String message = new Gson().fromJson(initData, String.class);
            if(message != null && !message.isEmpty()) {
                terminal.println(message);
            }
        }
    }
}
