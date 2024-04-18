package com.example.chatbot;
public class UidSingleton {
    private static UidSingleton instance;
    private String uid;

    private UidSingleton() {
        // private 생성자로 외부에서 인스턴스화를 막음
    }

    public static synchronized UidSingleton getInstance() {
        if (instance == null) {
            instance = new UidSingleton();
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
