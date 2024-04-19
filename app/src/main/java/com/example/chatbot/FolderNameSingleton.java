package com.example.chatbot;

public class FolderNameSingleton {
    private static FolderNameSingleton instance;
    private String uid;

    private FolderNameSingleton() {
        // private 생성자로 외부에서 인스턴스화를 막음
    }

    public static synchronized FolderNameSingleton getInstance() {
        if (instance == null) {
            instance = new FolderNameSingleton();
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
