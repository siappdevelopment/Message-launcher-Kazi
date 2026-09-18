package com.messages.smart.sms.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.List;

public class WeatherUiModel {
    @NonNull
    public final String location;
    @NonNull
    public final String temperature;
    @NonNull
    public final String condition;
    @NonNull
    public final String highTemperature;
    @NonNull
    public final String lowTemperature;
    @DrawableRes
    public final int iconRes;
    @NonNull
    public final List<WeatherHourlyUiModel> hourlyForecast;

    public WeatherUiModel(@NonNull String location, @NonNull String temperature, @NonNull String condition, @NonNull String highTemperature, @NonNull String lowTemperature, @DrawableRes int iconRes, @NonNull List<WeatherHourlyUiModel> hourlyForecast) {
        this.location = location;
        this.temperature = temperature;
        this.condition = condition;
        this.highTemperature = highTemperature;
        this.lowTemperature = lowTemperature;
        this.iconRes = iconRes;
        this.hourlyForecast = List.copyOf(hourlyForecast);
    }
}