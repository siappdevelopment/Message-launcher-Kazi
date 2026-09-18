package com.messages.smartsms.services;

import com.messages.smartsms.models.NewsResponseModel;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface APIInterface {
    @GET("v1/latest-news")
    Call<NewsResponseModel> getLatestNews(@Query("apiKey") String apiKey, @Query("language") String language);
}