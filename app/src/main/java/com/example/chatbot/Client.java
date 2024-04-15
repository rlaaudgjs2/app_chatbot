package com.example.chatbot;

import java.util.Scanner;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Client {
    private static final String BASE_URL = "http://127.0.0.1:8001/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Enter command: ");
            String input = scanner.nextLine().trim();
            String[] cmd = input.split(" ");

            if (cmd[0].equals("create")) {
                createIndex(cmd[1]);
            } else if (cmd[0].equals("upload")) {
                String indexName = cmd[1];
                String title = cmd[2];
                String content = getContent(cmd, 3);
                uploadIndex(indexName, title, content);
            } else {
                String indexName = cmd[1];
                String text = getContent(cmd, 2);
                chat(indexName, text);
            }
        }
    }

    private static String getContent(String[] cmd, int startIndex) {
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = startIndex; i < cmd.length; i++) {
            contentBuilder.append(cmd[i]).append(" ");
        }
        return contentBuilder.toString().trim();
    }

    private static void createIndex(String indexName) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(BASE_URL + "create?index_name=" + indexName)
                .post(RequestBody.create("", JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("Index created successfully");
            } else {
                System.out.println("Failed to create index: " + response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void uploadIndex(String indexName, String title, String content) {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = RequestBody.create(JSON, "{\"index_name\":\"" + indexName + "\",\"title\":\"" + title + "\",\"content\":\"" + content + "\"}");

        Request request = new Request.Builder()
                .url(BASE_URL + "upload")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("Index uploaded successfully");
            } else {
                System.out.println("Failed to upload index: " + response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void chat(String indexName, String text) {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = RequestBody.create(JSON, "{\"index_name\":\"" + indexName + "\",\"text\":\"" + text + "\"}");

        Request request = new Request.Builder()
                .url(BASE_URL + "chat")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("\nYou: " + text);
                System.out.println("\nCat-GPT: " + response.body().string() + "\n");
            } else {
                System.out.println("Failed to chat: " + response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
