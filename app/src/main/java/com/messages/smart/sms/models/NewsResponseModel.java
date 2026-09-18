package com.messages.smart.sms.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NewsResponseModel {
    @SerializedName("status")
    private String status;

    @SerializedName("news")
    private List<NewsModel> news;

    @SerializedName("page")
    private int page;

    public String getStatus() {
        return status;
    }

    public List<NewsModel> getNews() {
        return news;
    }

    public int getPage() {
        return page;
    }
}