package com.messages.smartsms.common;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.messages.smartsms.R;
import com.messages.smartsms.models.WeatherHourlyUiModel;
import com.messages.smartsms.models.WeatherUiModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class WeatherForecastGenerator {
    private static final int HOURLY_COUNT = 6;

    private WeatherForecastGenerator() {
    }

    @NonNull
    public static WeatherUiModel generate() {
        return generate(Calendar.getInstance());
    }

    @NonNull
    public static WeatherUiModel generate(@NonNull Calendar calendar) {
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        DayProfile day = DayProfile.forCalendar(calendar);

        List<WeatherHourlyUiModel> hourly = new ArrayList<>(HOURLY_COUNT);
        int[] temps = new int[HOURLY_COUNT];
        for (int i = 0; i < HOURLY_COUNT; i++) {
            int hour = (currentHour + i) % 24;
            temps[i] = temperatureForHour(hour, day);
            if (i > 0) {
                int delta = temps[i] - temps[i - 1];
                if (delta > 2) {
                    temps[i] = temps[i - 1] + 2;
                } else if (delta < -2) {
                    temps[i] = temps[i - 1] - 2;
                }
            }
        }

        for (int i = 0; i < HOURLY_COUNT; i++) {
            int hour = (currentHour + i) % 24;
            String condition = conditionFor(hour, temps[i], day);
            hourly.add(new WeatherHourlyUiModel(i == 0 ? "Now" : String.format(Locale.US, "%02d:00", hour), iconFor(condition, isDaytime(hour)), temps[i] + "°"));
        }

        String nowCondition = conditionFor(currentHour, temps[0], day);
        String location = Locale.getDefault().getDisplayCountry();
        if (location == null || location.trim().isEmpty()) {
            location = "Local";
        }

        return new WeatherUiModel(location, temps[0] + "°", nowCondition, day.high + "°", day.low + "°", iconFor(nowCondition, isDaytime(currentHour)), hourly);
    }

    private static int temperatureForHour(int hour, @NonNull DayProfile day) {
        double factor = (Math.cos((hour - 14) * Math.PI / 12.0) + 1.0) * 0.5;
        return (int) Math.round(day.low + (day.high - day.low) * factor);
    }

    @NonNull
    private static String conditionFor(int hour, int temp, @NonNull DayProfile day) {
        boolean dayTime = isDaytime(hour);
        int span = Math.max(1, day.high - day.low);
        float warmth = (temp - day.low) / (float) span;

        if (dayTime) {
            if (warmth >= 0.78f) {
                return "Sunny";
            }
            if (warmth >= 0.45f) {
                return "Partly Cloudy";
            }
            return "Cloudy";
        }

        if (warmth >= 0.55f) {
            return "Clear";
        }
        if (warmth >= 0.30f) {
            return "Partly Cloudy";
        }
        return "Cloudy";
    }

    @DrawableRes
    private static int iconFor(@NonNull String condition, boolean dayTime) {
        switch (condition) {
            case "Sunny":
                return R.drawable.ic_weather_sunny;
            case "Clear":
                return dayTime ? R.drawable.ic_weather_sunny : R.drawable.ic_weather_clear_night;
            case "Cloudy":
                return R.drawable.ic_weather_cloudy;
            case "Partly Cloudy":
            default:
                return dayTime ? R.drawable.ic_weather_partly_cloudy : R.drawable.ic_weather_partly_cloudy_night;
        }
    }

    private static boolean isDaytime(int hour) {
        return hour >= 6 && hour < 19;
    }

    private static final class DayProfile {
        final int high;
        final int low;

        private DayProfile(int high, int low) {
            this.high = high;
            this.low = low;
        }

        @NonNull
        static DayProfile forCalendar(@NonNull Calendar calendar) {
            int seed = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR);
            Random random = new Random(seed);

            int month = calendar.get(Calendar.MONTH);
            int seasonalBaseHigh;
            if (month <= 1 || month == 11) {
                seasonalBaseHigh = 24;
            } else if (month <= 4) {
                seasonalBaseHigh = 32;
            } else if (month <= 8) {
                seasonalBaseHigh = 35;
            } else {
                seasonalBaseHigh = 30;
            }

            int high = seasonalBaseHigh + random.nextInt(5) - 1;
            int spread = 7 + random.nextInt(5);
            int low = high - spread;
            return new DayProfile(high, low);
        }
    }
}