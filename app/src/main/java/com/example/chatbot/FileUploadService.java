package com.example.chatbot;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface FileUploadService {
    @POST("upload")
    Call<Void> uploadFiles(@Body MultipartBody body);
}