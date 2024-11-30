package com.example.chatbot;
public class ChatData {
    private String chatName;
    private String lastMessage;
    private String messageTime;

    // Constructor
    public ChatData(String chatName, String lastMessage, String messageTime) {
        this.chatName = chatName;
        this.lastMessage = lastMessage;
        this.messageTime = messageTime;
    }

    // Getters
    public String getChatName() {
        return chatName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getMessageTime() {
        return messageTime;
    }
}
