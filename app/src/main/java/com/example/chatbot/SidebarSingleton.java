package com.example.chatbot;

public class SidebarSingleton {
    private static SidebarSingleton instance;

    private String selectedGroupName;
    private String selectedFolderName;

    private SidebarSingleton() {
        // Private constructor to enforce singleton pattern
    }

    public static SidebarSingleton getInstance() {
        if (instance == null) {
            instance = new SidebarSingleton();
        }
        return instance;
    }

    public String getSelectedGroupName() {
        return selectedGroupName;
    }

    public void setSelectedGroupName(String selectedGroupName) {
        this.selectedGroupName = selectedGroupName;
    }

    public String getSelectedFolderName() {
        return selectedFolderName;
    }

    public void setSelectedFolderName(String selectedFolderName) {
        this.selectedFolderName = selectedFolderName;
    }
}
