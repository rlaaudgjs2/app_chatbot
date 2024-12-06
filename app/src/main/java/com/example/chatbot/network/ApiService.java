package com.example.chatbot.network;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("/api/upload")
    Call<okhttp3.ResponseBody> uploadFile(
            @Part MultipartBody.Part file,
            @Part("groupName") RequestBody groupName,
            @Part("folderName") RequestBody folderName
    );
}