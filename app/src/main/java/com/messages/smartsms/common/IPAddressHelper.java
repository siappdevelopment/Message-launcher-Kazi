package com.messages.smartsms.common;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IPAddressHelper {
    private static final String API_URL = "https://pro.ip-api.com/json/?key=RrtTUs6AdrbN4q0&fields=status,countryCode,city,query,proxy,hosting,regionName,country";

    public static final class LocationInfo {
        public final String city;
        public final String state;
        public final String country;

        public LocationInfo(String city, String state, String country) {
            this.city = city == null ? "" : city.trim();
            this.state = state == null ? "" : state.trim();
            this.country = country == null ? "" : country.trim();
        }
    }

    public interface IPCallback {
        void onResponse(String countryName);

        void onFailure(Exception e);
    }

    public interface LocationCallback {
        void onResponse(@NonNull LocationInfo locationInfo);

        void onFailure(Exception e);
    }

    public static void getLocationInfo(@Nullable String apiUrl, @NonNull LocationCallback callback) {
        String requestUrl = apiUrl == null || apiUrl.trim().isEmpty() ? API_URL : apiUrl.trim();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) new URL(requestUrl).openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(10000);

                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP error code: " + responseCode);
                }

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = bufferedReader.readLine()) != null) {
                    response.append(inputLine);
                }
                bufferedReader.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                if (!"success".equalsIgnoreCase(jsonObject.optString("status"))) {
                    throw new Exception("Location lookup failed");
                }

                LocationInfo locationInfo = new LocationInfo(jsonObject.optString("city", ""), jsonObject.optString("regionName", ""), jsonObject.optString("country", ""));
                handler.post(() -> callback.onResponse(locationInfo));
            } catch (Exception e) {
                handler.post(() -> callback.onFailure(e));
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    public static void getCountryName(IPCallback callback) {
        getLocationInfo(API_URL, new LocationCallback() {
            @Override
            public void onResponse(@NonNull LocationInfo locationInfo) {
                if (locationInfo.country.isEmpty()) {
                    callback.onFailure(new Exception("Country data not found in response"));
                    return;
                }
                callback.onResponse(locationInfo.country);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
}