package com.example.chatbot;

public class GroupSingleton {
    private static GroupSingleton instance;
    private String uid;

    private GroupSingleton() {
        // private 생성자로 외부에서 인스턴스화를 막음
    }

    public static synchronized GroupSingleton getInstance() {
        if (instance == null) {
            instance = new GroupSingleton();
        }
        return instance;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}