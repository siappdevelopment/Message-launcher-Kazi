package com.messages.smartsms.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NewsModel {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("url")
    private String url;

    @SerializedName("author")
    private String author;

    @SerializedName("image")
    private String image;

    @SerializedName("language")
    private String language;

    @SerializedName("category")
    private List<String> category;

    @SerializedName("published")
    private String published;

    private boolean isAdShow;
    private int adSlot;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public String getUrl() {
        return url;
    }

    public boolean isAdShow() {
        return isAdShow;
    }

    public void setAdShow(boolean adShow) {
        isAdShow = adShow;
    }

    public int getAdSlot() {
        return adSlot;
    }

    public void setAdSlot(int adSlot) {
        this.adSlot = adSlot;
    }
}