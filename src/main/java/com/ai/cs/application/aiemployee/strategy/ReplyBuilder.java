package com.ai.cs.application.aiemployee.strategy;

public class ReplyBuilder {
    private final StringBuilder reply = new StringBuilder();
    private boolean hasContent = false;

    public ReplyBuilder append(String text) {
        if (text != null && !text.isEmpty()) {
            if (hasContent) reply.append("\n\n");
            reply.append(text);
            hasContent = true;
        }
        return this;
    }

    public String build() {
        return reply.toString();
    }

    public boolean hasContent() {
        return hasContent;
    }
}
