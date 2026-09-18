package com.messages.smart.sms.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class WeatherHourlyUiModel {
    @NonNull
    public final String timeLabel;
    @DrawableRes
    public final int iconRes;
    @NonNull
    public final String temperature;

    public WeatherHourlyUiModel(@NonNull String timeLabel, @DrawableRes int iconRes, @NonNull String temperature) {
        this.timeLabel = timeLabel;
        this.iconRes = iconRes;
        this.temperature = temperature;
    }
}