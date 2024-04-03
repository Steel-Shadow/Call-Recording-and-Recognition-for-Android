package org.tfri.data;

public class ChatContent {
    public enum Type {
        RECEIVE,
        SEND
    }

    private final Type type;
    private final String content;

    public ChatContent(String content, Type type) {
        this.content = content;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
}
