package com.example.chatbot;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("create")
    Call<Void> createIndex(@Query("index_name") String indexName);

    @POST("upload")
    Call<Void> uploadIndex(@Query("index_name") String indexName, @Query("title") String title, @Query("content") String content);

    @POST("chat")
    Call<ChatResponse> chat(@Query("index_name") String indexName, @Query("text") String text);
}

class ChatResponse {
    private String result;

    public String getResult() {
        return result;
    }
}
