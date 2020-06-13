package com.gmail.gimbimloki.gw.spring.mt.util;

public class StackTraces {
    private StackTraces() {
        // Do Nothing
    }

    public static String getStackTrancesString() {
        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (elements == null || elements.length < 1) {
            return "No Stack Traces";
        }

        final StringBuilder stackTraces = new StringBuilder();
        for (StackTraceElement element : elements) {
            stackTraces
                    .append(element.toString())
                    .append("\n");
        }

        return stackTraces.toString();
    }
}
