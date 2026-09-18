package com.messages.smart.sms.fragments;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.nativead.NativeAd;
import com.messages.smart.sms.R;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.common.WeatherForecastGenerator;
import com.messages.smart.sms.models.AppsModel;
import com.messages.smart.sms.models.WeatherHourlyUiModel;
import com.messages.smart.sms.models.WeatherUiModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubContainerFragment extends Fragment {
    private AppCompatTextView tvGreeting, tvMostUsedApps;
    private CardView cvSuggestedFolder;
    private CardView cvWeather;
    private AppCompatImageView ivWeatherIcon;
    private AppCompatTextView tvWeatherTemp, tvWeatherCondition;
    private LinearLayout llWeatherHourly;
    private GridLayout gvSuggestedApps;
    private CardView cvNativeAdView;
    private RelativeLayout rlNativeAdView;
    private ShimmerFrameLayout slNativeShimmer;
    private FrameLayout flNativeAd;

    private static final String LAUNCHER_NATIVE_AD_TYPE = "medium";
    private static NativeAd cachedLauncherNativeAd;
    private static boolean nativeAdLoadInProgress;

    private static final int SUGGESTED_COLUMNS = 5;
    private static final String[][] SUGGESTED_APP_PACKAGES = {{"com.whatsapp"}, {"com.instagram.android"}, {"com.facebook.katana", "com.facebook.lite"}, {"com.snapchat.android"}, {"com.google.android.apps.nbu.paisa.user"}, {"com.phonepe.app"}, {"net.one97.paytm"}, {"com.google.android.youtube"}, {"com.google.android.calculator", "com.sec.android.app.popupcalculator", "com.miui.calculator", "com.huawei.calculator", "com.oneplus.calculator", "com.android.calculator2"}, {"com.google.android.documentsui", "com.sec.android.app.myfiles", "com.mi.android.globalFileexplorer", "com.coloros.filemanager", "com.android.fileexplorer", "com.mediatek.filemanager"}, {"com.google.android.apps.photos", "com.sec.android.gallery3d", "com.miui.gallery", "com.oneplus.gallery", "com.android.gallery3d"}, {"com.android.settings"}};
    private static final List<AppsModel> cachedSuggestedApps = new ArrayList<>();
    private static String cachedSuggestedKey = "";

    private final ArrayList<AppsModel> suggestedApps = new ArrayList<>();
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private int loadRequestId;

    private boolean isFragmentReady() {
        return isAdded() && getActivity() != null;
    }

    private boolean hasContext() {
        return isAdded() && getContext() != null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        updateGreetingAndDate();
        refreshWeatherCard();
        if (!cachedSuggestedApps.isEmpty()) {
            suggestedApps.clear();
            suggestedApps.addAll(cachedSuggestedApps);
            bindSuggestedApps();
        }
        loadSuggestedApps(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreetingAndDate();
        refreshWeatherCard();
        loadSuggestedApps(true);
        showAd();
    }

    @Override
    public void onDestroyView() {
        if (flNativeAd != null) {
            flNativeAd.removeAllViews();
        }
        destroyCachedNativeAd();
        clearViewReferences();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        clearCachedNativeAd();
        bgExecutor.shutdownNow();
        super.onDestroy();
    }

    private void bindViews(View view) {
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvMostUsedApps = view.findViewById(R.id.tvMostUsedApps);
        cvSuggestedFolder = view.findViewById(R.id.cvSuggestedFolder);
        gvSuggestedApps = view.findViewById(R.id.gvSuggestedApps);

        cvWeather = view.findViewById(R.id.weatherCard);
        ivWeatherIcon = view.findViewById(R.id.ivWeatherIcon);
        tvWeatherTemp = view.findViewById(R.id.tvWeatherTemp);
        tvWeatherCondition = view.findViewById(R.id.tvWeatherCondition);
        llWeatherHourly = view.findViewById(R.id.llWeatherHourly);

        cvNativeAdView = view.findViewById(R.id.cvNativeAdView);
        rlNativeAdView = view.findViewById(R.id.rlNativeAdView);
        slNativeShimmer = view.findViewById(R.id.slNativeShimmer);
        flNativeAd = view.findViewById(R.id.flNativeAd);
    }

    private void showAd() {
        if (cvNativeAdView == null || rlNativeAdView == null || slNativeShimmer == null || flNativeAd == null || !isFragmentReady()) {
            return;
        }

        if (!shouldShowNativeAd()) {
            hideNativeAdContainer();
            return;
        }

        destroyCachedNativeAd();
        nativeAdLoadInProgress = true;
        cvNativeAdView.setVisibility(VISIBLE);
        rlNativeAdView.setVisibility(VISIBLE);
        slNativeShimmer.setVisibility(VISIBLE);
        flNativeAd.setVisibility(GONE);

        AdPlacement.loadNativeAd(requireActivity(), AdPlacement.getLauncherAppNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, LAUNCHER_NATIVE_AD_TYPE, nativeAd -> {
            nativeAdLoadInProgress = false;
            cachedLauncherNativeAd = nativeAd;
            if (!hasContext()) {
                return;
            }
            AdPlacement.setLauncherAppLastShowTime(getContext(), System.currentTimeMillis());
        }, () -> {
            nativeAdLoadInProgress = false;
            hideNativeAdContainer();
        });
    }

    private boolean shouldShowNativeAd() {
        return AdPlacement.isNetworkAvailable(requireActivity()) && AdPlacement.getLauncherAppNativeAdShow() && AdPlacement.canShowLauncherNativeAd(requireContext()) && !nativeAdLoadInProgress;
    }

    private void hideNativeAdContainer() {
        if (cvNativeAdView != null) {
            cvNativeAdView.setVisibility(GONE);
        }
    }

    private static void destroyCachedNativeAd() {
        if (cachedLauncherNativeAd != null) {
            cachedLauncherNativeAd.destroy();
            cachedLauncherNativeAd = null;
        }
    }

    public static void clearCachedNativeAd() {
        destroyCachedNativeAd();
        nativeAdLoadInProgress = false;
    }

    private void clearViewReferences() {
        tvGreeting = null;
        tvMostUsedApps = null;
        cvSuggestedFolder = null;
        cvWeather = null;
        ivWeatherIcon = null;
        tvWeatherTemp = null;
        tvWeatherCondition = null;
        llWeatherHourly = null;
        gvSuggestedApps = null;
        cvNativeAdView = null;
        rlNativeAdView = null;
        slNativeShimmer = null;
        flNativeAd = null;
    }

    private void bindWeatherCard(@Nullable WeatherUiModel weather) {
        if (cvWeather == null || weather == null) {
            if (cvWeather != null) {
                cvWeather.setVisibility(GONE);
            }
            return;
        }

        cvWeather.setVisibility(VISIBLE);
        if (tvWeatherTemp != null) {
            tvWeatherTemp.setText(weather.temperature);
        }
        if (tvWeatherCondition != null) {
            tvWeatherCondition.setText(weather.condition);
        }
        if (ivWeatherIcon != null) {
            ivWeatherIcon.setImageResource(weather.iconRes);
        }
        bindHourlyForecast(weather.hourlyForecast);
    }

    private void refreshWeatherCard() {
        bindWeatherCard(WeatherForecastGenerator.generate());
    }

    private void bindHourlyForecast(@NonNull List<WeatherHourlyUiModel> hourlyForecast) {
        if (llWeatherHourly == null || getContext() == null) {
            return;
        }
        llWeatherHourly.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (WeatherHourlyUiModel hour : hourlyForecast) {
            View itemView = inflater.inflate(R.layout.item_weather_hourly, llWeatherHourly, false);
            AppCompatTextView tvHourlyTime = itemView.findViewById(R.id.tvHourlyTime);
            AppCompatImageView ivHourlyIcon = itemView.findViewById(R.id.ivHourlyIcon);
            AppCompatTextView tvHourlyTemp = itemView.findViewById(R.id.tvHourlyTemp);
            if (tvHourlyTime != null) {
                tvHourlyTime.setText(hour.timeLabel);
            }
            if (ivHourlyIcon != null) {
                ivHourlyIcon.setImageResource(hour.iconRes);
            }
            if (tvHourlyTemp != null) {
                tvHourlyTemp.setText(hour.temperature);
            }
            llWeatherHourly.addView(itemView);
        }
    }

    private void updateGreetingAndDate() {
        if (tvGreeting == null || tvMostUsedApps == null) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else if (hour < 21) {
            greeting = "Good Evening";
        } else {
            greeting = "Good Night";
        }
        tvGreeting.setText(greeting);
    }

    private void loadSuggestedApps(boolean forceRefresh) {
        if (!isAdded()) {
            return;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (!forceRefresh && !cachedSuggestedApps.isEmpty()) {
            return;
        }

        final int requestId = ++loadRequestId;
        final Context appContext = context.getApplicationContext();
        bgExecutor.execute(() -> {
            ArrayList<AppsModel> loaded = resolveSuggestedApps(appContext);
            String key = buildPackagesKey(loaded);

            if (key.equals(cachedSuggestedKey) && !cachedSuggestedApps.isEmpty()) {
                loaded = new ArrayList<>(cachedSuggestedApps);
            } else {
                cachedSuggestedApps.clear();
                cachedSuggestedApps.addAll(loaded);
                cachedSuggestedKey = key;
            }

            ArrayList<AppsModel> result = loaded;
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (requestId != loadRequestId || !isAdded() || gvSuggestedApps == null) {
                    return;
                }
                suggestedApps.clear();
                suggestedApps.addAll(result);
                bindSuggestedApps();
            });
        });
    }

    @NonNull
    private ArrayList<AppsModel> resolveSuggestedApps(Context context) {
        ArrayList<AppsModel> result = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();

        for (String[] candidates : SUGGESTED_APP_PACKAGES) {
            AppsModel app = resolveFirstInstalledApp(packageManager, candidates);
            if (app != null) {
                result.add(app);
            }
        }
        return result;
    }

    @Nullable
    private AppsModel resolveFirstInstalledApp(PackageManager packageManager, String[] candidates) {
        for (String packageName : candidates) {
            try {
                packageManager.getPackageInfo(packageName, 0);
                Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
                if (launchIntent == null) {
                    continue;
                }
                String appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString();
                Drawable appIcon = packageManager.getApplicationIcon(packageName);
                long installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime;
                return new AppsModel(appName, packageName, appIcon, installTime);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String buildPackagesKey(List<AppsModel> apps) {
        StringBuilder builder = new StringBuilder();
        for (AppsModel app : apps) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(app.getPackageName());
        }
        return builder.toString();
    }

    private void bindSuggestedApps() {
        if (gvSuggestedApps == null || cvSuggestedFolder == null || getContext() == null) {
            return;
        }

        gvSuggestedApps.removeAllViews();

        if (suggestedApps.isEmpty()) {
            cvSuggestedFolder.setVisibility(GONE);
            return;
        }
        cvSuggestedFolder.setVisibility(VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int rowCount = (suggestedApps.size() + SUGGESTED_COLUMNS - 1) / SUGGESTED_COLUMNS;
        gvSuggestedApps.setColumnCount(SUGGESTED_COLUMNS);
        gvSuggestedApps.setRowCount(Math.max(rowCount, 1));

        for (int i = 0; i < suggestedApps.size(); i++) {
            AppsModel app = suggestedApps.get(i);
            View itemView = inflater.inflate(R.layout.item_sub_suggested_app, gvSuggestedApps, false);

            AppCompatImageView ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            AppCompatTextView tvAppName = itemView.findViewById(R.id.tvAppName);
            if (ivAppIcon != null) {
                ivAppIcon.setImageDrawable(app.getAppIcon());
            }
            if (tvAppName != null) {
                tvAppName.setText(app.getAppName());
            }

            final String packageName = app.getPackageName();
            itemView.setOnClickListener(v -> launchApp(packageName));

            int row = i / SUGGESTED_COLUMNS;
            int column = i % SUGGESTED_COLUMNS;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(row, 1f), GridLayout.spec(column, 1f));
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            itemView.setLayoutParams(params);
            gvSuggestedApps.addView(itemView);
        }
    }

    private void launchApp(String packageName) {
        if (!isAdded() || packageName == null || packageName.isEmpty()) {
            return;
        }
        try {
            Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null) {
                Toast.makeText(requireContext(), "Unable to open app", Toast.LENGTH_SHORT).show();
                return;
            }
            trackRecentApp(packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Utils.clearActivityTransition(getActivity());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open app", Toast.LENGTH_SHORT).show();
        }
    }

    private void trackRecentApp(String packageName) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("recentThread", MODE_PRIVATE);
        String oldData = sharedPreferences.getString("packages", "");

        ArrayList<String> list = new ArrayList<>();
        if (!oldData.isEmpty()) {
            list.addAll(Arrays.asList(oldData.split(",")));
        }
        list.remove(packageName);
        list.add(0, packageName);

        while (list.size() > 4) {
            list.remove(list.size() - 1);
        }

        sharedPreferences.edit().putString("packages", TextUtils.join(",", list)).apply();
    }
}