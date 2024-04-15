package com.example.chatbot;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatbotManager {
    private static final String BASE_URL = "http://127.0.0.1:8001/";
    private ApiService apiService;

    public ChatbotManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public void createIndex(String indexName) {
        Call<Void> call = apiService.createIndex(indexName);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    System.out.println("Index created successfully");
                } else {
                    System.out.println("Failed to create index");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                System.out.println("Failed to create index: " + t.getMessage());
            }
        });
    }

    public void uploadIndex(String indexName, String title, String content) {
        Call<Void> call = apiService.uploadIndex(indexName, title, content);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    System.out.println("Index uploaded successfully");
                } else {
                    System.out.println("Failed to upload index");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                System.out.println("Failed to upload index: " + t.getMessage());
            }
        });
    }

    public void chat(String indexName, String text) {
        Call<ChatResponse> call = apiService.chat(indexName, text);
        call.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                if (response.isSuccessful()) {
                    ChatResponse chatResponse = response.body();
                    System.out.println("Chatbot Response: " + chatResponse.getResult());
                } else {
                    System.out.println("Failed to get chatbot response");
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                System.out.println("Failed to get chatbot response: " + t.getMessage());
            }
        });
    }
}
