package com.messages.smart.sms.common;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.messages.smart.sms.MyApplication;
import com.messages.smart.sms.R;
import com.messages.smart.sms.interfaces.OnInterstitialAdListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AdPlacement {
    public static String privacyPolicy = "";
    public static String termsConditions = "";
    public static boolean appOpenAdShow = false;
    public static boolean appOpenDialogShow = false;
    public static String appOpenId = "";
    public static int appOpenShowPerDay;
    public static int interstitialClick;
    public static boolean rightSwipeInterstitialAdShow = false;
    public static int rightSwipeInterstitial;
    public static boolean interstitialAdShowSwitchFragment = false;
    public static int interstitialAdShowSwitchClick;
    public static String nativeAdLabelColor = "";
    public static String nativeAdButtonColor = "";
    public static boolean clEndScreenShow = false;
    public static String adPriority = "";
    public static boolean googleAdFailedShowQuiz = false;

    private static final List<String> quizBannerTitleList = new ArrayList<>();
    private static final List<String> quizBannerDescriptionList = new ArrayList<>();
    private static final List<String> quizNativeTitleList = new ArrayList<>();
    private static final List<String> quizNativeDescriptionList = new ArrayList<>();
    private static final List<String> quizInterstitialTitleList = new ArrayList<>();
    private static final List<String> quizInterstitialDescriptionList = new ArrayList<>();
    private static final List<String> quizInterstitialRateList = new ArrayList<>();
    private static final List<String> quizAppIconList = new ArrayList<>();
    private static final List<String> quizNativeMediaList = new ArrayList<>();
    private static final List<String> quizInterstitialMediaList = new ArrayList<>();
    private static final List<String> quizAppOpenMediaList = new ArrayList<>();
    private static String quizButtonText = "";
    private static final int QUIZ_IMAGE_FALLBACK = R.mipmap.ic_launcher;
    private static final float QUIZ_IMAGE_REVEAL_START_SCALE = 0.93f;
    private static final long QUIZ_IMAGE_REVEAL_DURATION_MS = 200L;
    private static final ExecutorService QUIZ_IMAGE_EXECUTOR = Executors.newCachedThreadPool();
    private static final AtomicInteger bannerLoadToken = new AtomicInteger(0);
    private static final AtomicInteger nativeLoadToken = new AtomicInteger(0);
    private static final AtomicInteger interstitialLoadToken = new AtomicInteger(0);
    private static final AtomicInteger appOpenLoadToken = new AtomicInteger(0);
    private static final String[] QUIZ_INTERSTITIAL_RATES = {"4.1", "4.2", "4.3", "4.4", "4.5", "4.6", "4.7", "4.8", "4.9"};
    private static final String[] QUIZ_INTERSTITIAL_USERS = {"50K+ Users", "100K+ Users", "500K+ Users", "1M+ Users"};
    private static final long QUIZ_INTERSTITIAL_CLOSE_DELAY_MS = 5000L;

    private static final List<String> quizLinkList = new ArrayList<>();
    private static final List<String> quizLinkExcludeList = new ArrayList<>();
    private static final List<String> activeQuizLinkList = new ArrayList<>();
    private static final List<String> appProxyListCity = new ArrayList<>();
    private static final List<String> appProxyListState = new ArrayList<>();
    private static final List<String> appProxyListCountry = new ArrayList<>();
    private static boolean appProxyCheckIp = false;
    private static String appProxyIpCheckerUrl = "";
    private static final AtomicInteger appProxyLookupToken = new AtomicInteger(0);

    public static boolean newsScreenShow = false;
    public static boolean defaultAppPopupShow = false;
    public static int defaultAppPopupCount;
    public static final String SCREEN_LANGUAGE = "Language";
    public static final String SCREEN_COLLECTION = "Collection";
    public static final String SCREEN_DEFAULT_SMS = "DefaultSMS";
    public static final String SCREEN_DEFAULT_HOME = "DefaultHome";
    public static final String SCREEN_INTRO = "Intro";
    private static final String FLOW_PREFS = "show_screen_flow_cache";
    private static List<String> showScreenFlow = new ArrayList<>();

    public static int splashDuration;
    public static boolean splashAdShow = false;
    public static String splashAdType = "";
    public static String splashBannerId = "";
    public static String splashNativeId = "";
    public static boolean afterSplashAdShow = false;
    public static String afterSplashAdType = "";
    public static String afterSplashInterstitialId = "";

    public static String introType = "";
    public static boolean introSwipeNativeAdShow = false;
    public static String introSwipeNativeId1 = "";
    public static String introSwipeNativeId2 = "";
    public static String introSwipeNativeId3 = "";
    public static int introButtonScreen;
    public static boolean introButtonAdShow = false;
    public static String introButtonAdType = "";
    public static String introButtonBannerId1 = "";
    public static String introButtonBannerId2 = "";
    public static String introButtonBannerId3 = "";
    public static String introButtonBannerId4 = "";
    public static String introButtonNativeId1 = "";
    public static String introButtonNativeId2 = "";
    public static String introButtonNativeId3 = "";
    public static String introButtonNativeId4 = "";

    public static boolean collectionAdShow = false;
    public static String collectionAdType = "";
    public static String collectionBannerId = "";
    public static String collectionNativeId = "";

    public static boolean defaultAdShow = false;
    public static String defaultAdType = "";
    public static String defaultBannerId = "";
    public static String defaultNativeId = "";
    public static boolean afterDefaultAdShow = false;
    public static String afterDefaultAdType = "";

    public static boolean languageAdShow = false;
    public static String languageAdType = "";
    public static String languageBannerId = "";
    public static String languageNativeId = "";
    public static boolean languageInterstitialAdShow = false;

    public static boolean mainAdShow = false;
    public static String mainAdType = "";
    public static String mainBannerId = "";
    public static String mainNativeId = "";
    public static boolean mainAdAutoRefresh = false;
    public static int mainAdAutoSecond;
    private static long lastMainContainerAdLoadElapsedMs;
    private static long lastMessageListAdLoadElapsedMs;

    public static boolean messageListAdShow = false;
    public static String messageListAdType = "";
    public static String messageListBannerId = "";
    public static String messageListNativeId = "";
    public static boolean messageListInterstitialAdShow = false;
    public static int messageListInterstitialClick;
    public static String messageListInterstitialId = "";

    public static boolean messageContentBannerAdShow = false;
    public static String messageContentBannerId = "";
    public static boolean messageContentBackInterstitialAdShow = false;
    public static int messageContentBackInterstitialClick;
    public static String messageContentBackInterstitialId = "";

    public static boolean launcherSettingAdShow = false;
    public static String launcherSettingAdType = "";
    public static String launcherSettingBannerId = "";
    public static String launcherSettingNativeId = "";

    public static boolean otherAdShow = false;
    public static String otherAdType = "";
    public static String otherBannerId = "";
    public static String otherNativeId = "";
    public static boolean otherInterstitialAdShow = false;
    public static String otherInterstitialId = "";

    public static boolean launcherAppBannerAdShow = false;
    public static int launcherAppBannerAdShowPerDay;
    public static String launcherAppBannerId = "";
    public static boolean launcherAppNativeAdShow = false;
    public static int launcherAppNativeAdShowPerDay;
    public static String launcherAppNativeId = "";
    public static boolean launcherAppClickAdShow = false;
    public static int launcherAppCount;
    public static String launcherAppAdType = "";
    public static String launcherAppInterstitialId = "";
    private static int launcherAppClickCount = 1;
    private static final AtomicInteger launcherAppClickAdToken = new AtomicInteger(0);
    private static final String LAUNCHER_APP_AD_TYPE_GOOGLE_INTER = "google_inter";
    private static final String LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN = "google_app_open";
    private static final String LAUNCHER_APP_AD_TYPE_GOOGLE_NATIVE = "google_native";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_INTER = "quiz_inter";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_APP_OPEN = "quiz_app_open";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_NATIVE = "quiz_native";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_BROWSER = "quiz_browser";
    private static final long INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS = 10_000L;

    public static boolean newsNativeAdShow = false;
    public static String newsNativeId1 = "";
    public static String newsNativeId2 = "";

    public static boolean messageAdShow = false;
    public static String messageAdType = "";
    public static String messageBannerId = "";
    public static String messageNativeId = "";
    private static boolean remoteConfigApplied = false;
    private static final String MESSAGE_AD_PREFS = "message_ad_config_cache";
    private static final String CL_END_PREFS = "cl_end_config_cache";

    public static boolean clEndAdShow = false;
    public static String clEndAdType = "";
    public static String clEndBannerId = "";
    public static String clEndNativeId = "";
    private static boolean clEndBackAdShow = false;
    private static String clEndBackAdType = "";
    private static int clEndBackAdShowAfterDay;
    private static int clEndBackAdShowPerDay;
    private static String clEndBackAdInterstitialId = "";
    private static boolean clEndBackAdCountryIP = false;
    private static ArrayList<String> clEndBackAdShowCountryList = new ArrayList<>();

    private static int notificationInstallDays;
    private static int notificationCallInstallDays;
    private static int notificationCallOverlayInstallDays;
    private static boolean allAllowPermissionShowNotification = false;
    private static boolean notificationBackAdShow = false;
    private static boolean notificationCloseButtonShow = false;
    private static ArrayList<String> notificationCountryList = new ArrayList<>();
    private static ArrayList<String> notificationCallCountryList = new ArrayList<>();
    private static ArrayList<String> notificationCallOverlayCountryList = new ArrayList<>();
    private static boolean clEndConfigLoadedFromPrefs = false;

    public static String ipCountryName;

    private static final String REFERRER_PREFS = "referrer_preferences";
    public static final String SetsReferrerUrl = "SetsReferrerUrl";

    public static String getReferrerUrl(Context context) {
        if (context == null) {
            return "";
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(REFERRER_PREFS, MODE_PRIVATE);
        return prefs.getString(SetsReferrerUrl, "");
    }

    public static void setReferrerUrl(Context context, String value) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(REFERRER_PREFS, MODE_PRIVATE).edit().putString(SetsReferrerUrl, value == null ? "" : value).apply();
    }

    public static String getPrivacyPolicy() {
        return privacyPolicy;
    }

    public static void setPrivacyPolicy(String privacyPolicy) {
        AdPlacement.privacyPolicy = privacyPolicy;
    }

    public static String getTermsConditions() {
        return termsConditions;
    }

    public static void setTermsConditions(String termsConditions) {
        AdPlacement.termsConditions = termsConditions;
    }

    public static boolean getAppOpenAdShow() {
        return appOpenAdShow;
    }

    public static void setAppOpenAdShow(boolean appOpenAdShow) {
        AdPlacement.appOpenAdShow = appOpenAdShow;
    }

    public static boolean getAppOpenDialogShow() {
        return appOpenDialogShow;
    }

    public static void setAppOpenDialogShow(boolean appOpenDialogShow) {
        AdPlacement.appOpenDialogShow = appOpenDialogShow;
    }

    public static String getAppOpenId() {
        return appOpenId;
    }

    public static void setAppOpenId(String appOpenId) {
        AdPlacement.appOpenId = appOpenId;
    }

    public static int getAppOpenShowPerDay() {
        return appOpenShowPerDay;
    }

    public static void setAppOpenShowPerDay(int appOpenShowPerDay) {
        AdPlacement.appOpenShowPerDay = appOpenShowPerDay;
    }

    public static int getInterstitialClick() {
        return interstitialClick;
    }

    public static void setInterstitialClick(int interstitialClick) {
        AdPlacement.interstitialClick = interstitialClick;
    }

    public static boolean getRightSwipeInterstitialAdShow() {
        return rightSwipeInterstitialAdShow;
    }

    public static void setRightSwipeInterstitialAdShow(boolean rightSwipeInterstitialAdShow) {
        AdPlacement.rightSwipeInterstitialAdShow = rightSwipeInterstitialAdShow;
    }

    public static int getRightSwipeInterstitial() {
        return rightSwipeInterstitial;
    }

    public static void setRightSwipeInterstitial(int rightSwipeInterstitial) {
        AdPlacement.rightSwipeInterstitial = rightSwipeInterstitial;
    }

    public static boolean getInterstitialAdShowSwitchFragment() {
        return interstitialAdShowSwitchFragment;
    }

    public static void setInterstitialAdShowSwitchFragment(boolean interstitialAdShowSwitchFragment) {
        AdPlacement.interstitialAdShowSwitchFragment = interstitialAdShowSwitchFragment;
    }

    public static int getInterstitialAdShowSwitchClick() {
        return interstitialAdShowSwitchClick;
    }

    public static void setInterstitialAdShowSwitchClick(int interstitialAdShowSwitchClick) {
        AdPlacement.interstitialAdShowSwitchClick = interstitialAdShowSwitchClick;
    }

    public static String getNativeAdLabelColor() {
        return nativeAdLabelColor;
    }

    public static void setNativeAdLabelColor(String nativeAdLabelColor) {
        AdPlacement.nativeAdLabelColor = nativeAdLabelColor;
    }

    public static String getNativeAdButtonColor() {
        return nativeAdButtonColor;
    }

    public static void setNativeAdButtonColor(String nativeAdButtonColor) {
        AdPlacement.nativeAdButtonColor = nativeAdButtonColor;
    }

    public static List<String> getShowScreenFlow() {
        if (showScreenFlow == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(showScreenFlow);
    }

    public static void setShowScreenFlow(List<String> flow) {
        showScreenFlow = flow == null ? new ArrayList<>() : new ArrayList<>(flow);
    }

    public static List<String> parseShowScreenFlow(Object rawFlow) {
        JSONArray array = toJsonArray(rawFlow);
        if (array == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String screen = normalizeScreenName(array.optString(i, ""));
            if (!screen.isEmpty() && !result.contains(screen)) {
                result.add(screen);
            }
        }
        return result;
    }

    public static void applyShowScreenFlowFromConfig(JSONObject jsonObject, String topLevelFlowJson) {
        List<String> nestedFlow = new ArrayList<>();
        if (jsonObject != null && jsonObject.has("Show_Screen_Flow")) {
            nestedFlow = parseShowScreenFlow(jsonObject.opt("Show_Screen_Flow"));
        }
        List<String> topLevelFlow = parseShowScreenFlow(topLevelFlowJson);

        if (!topLevelFlow.isEmpty() && topLevelFlow.contains(SCREEN_LANGUAGE) && (nestedFlow.isEmpty() || !nestedFlow.contains(SCREEN_LANGUAGE))) {
            setShowScreenFlow(topLevelFlow);
            return;
        }
        if (!nestedFlow.isEmpty()) {
            setShowScreenFlow(nestedFlow);
            return;
        }
        setShowScreenFlow(topLevelFlow);
    }

    public static void cacheShowScreenFlow(Context context) {
        if (context == null) {
            return;
        }
        JSONArray array = new JSONArray();
        for (String screen : getShowScreenFlow()) {
            array.put(screen);
        }
        context.getApplicationContext().getSharedPreferences(FLOW_PREFS, MODE_PRIVATE).edit().putBoolean("cached", true).putString("flow", array.toString()).apply();
    }

    public static void restoreShowScreenFlow(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(FLOW_PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean("cached", false)) {
            setShowScreenFlow(new ArrayList<>());
            return;
        }
        setShowScreenFlow(parseShowScreenFlow(prefs.getString("flow", "[]")));
    }

    private static JSONArray toJsonArray(Object rawFlow) {
        if (rawFlow == null) {
            return null;
        }
        try {
            if (rawFlow instanceof JSONArray) {
                return (JSONArray) rawFlow;
            }
            String value = String.valueOf(rawFlow).trim();
            if (value.startsWith("\uFEFF")) {
                value = value.substring(1).trim();
            }
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1).replace("\\\"", "\"").trim();
            }
            if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
                return null;
            }
            if (value.startsWith("[")) {
                return new JSONArray(value);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String normalizeScreenName(String screen) {
        if (screen == null) {
            return "";
        }
        String value = screen.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (value.equalsIgnoreCase(SCREEN_LANGUAGE)) {
            return SCREEN_LANGUAGE;
        }
        if (value.equalsIgnoreCase(SCREEN_COLLECTION)) {
            return SCREEN_COLLECTION;
        }
        if (value.equalsIgnoreCase(SCREEN_DEFAULT_SMS) || value.equalsIgnoreCase("Default_SMS") || value.equalsIgnoreCase("DefaultSms")) {
            return SCREEN_DEFAULT_SMS;
        }
        if (value.equalsIgnoreCase(SCREEN_DEFAULT_HOME) || value.equalsIgnoreCase("Default") || value.equalsIgnoreCase("DefaultApp") || value.equalsIgnoreCase("Default_Home")) {
            return SCREEN_DEFAULT_HOME;
        }
        if (value.equalsIgnoreCase(SCREEN_INTRO)) {
            return SCREEN_INTRO;
        }
        return value;
    }

    public static boolean getClEndScreenShow() {
        return clEndScreenShow;
    }

    public static void setClEndScreenShow(boolean clEndScreenShow) {
        AdPlacement.clEndScreenShow = clEndScreenShow;
    }

    public static String getAdPriority() {
        return adPriority;
    }

    public static void setAdPriority(String adPriority) {
        AdPlacement.adPriority = adPriority == null ? "" : adPriority;
    }

    public static boolean shouldUseQuizPriority() {
        return "QUIZ".equalsIgnoreCase(getAdPriority());
    }

    public static boolean bindQuizNativeAdSlot(@Nullable Activity activity, @Nullable RelativeLayout rlNativeAdView, @Nullable ShimmerFrameLayout slNativeShimmer, @Nullable FrameLayout flNativeAd, @Nullable String type) {
        if (activity == null || (!getGoogleAdFailedShowQuiz() && !shouldUseQuizPriority())) {
            return false;
        }
        return showQuizNativeAd(activity, rlNativeAdView, slNativeShimmer, flNativeAd, type);
    }

    public static boolean getGoogleAdFailedShowQuiz() {
        return googleAdFailedShowQuiz;
    }

    public static void setGoogleAdFailedShowQuiz(boolean googleAdFailedShowQuiz) {
        AdPlacement.googleAdFailedShowQuiz = googleAdFailedShowQuiz;
    }

    public static List<String> getQuizBannerTitleList() {
        return quizBannerTitleList;
    }

    public static List<String> getQuizBannerDescriptionList() {
        return quizBannerDescriptionList;
    }

    public static List<String> getQuizNativeTitleList() {
        return quizNativeTitleList;
    }

    public static List<String> getQuizNativeDescriptionList() {
        return quizNativeDescriptionList;
    }

    public static void applyQuizAdsConfig(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }
        if (jsonObject.has("Ad_Priority")) {
            setAdPriority(jsonObject.optString("Ad_Priority", ""));
        }
        if (jsonObject.has("Google_Ad_Failed_Show_Quiz")) {
            setGoogleAdFailedShowQuiz(jsonObject.optBoolean("Google_Ad_Failed_Show_Quiz", false));
        }

        List<String> appIcons = parseQuizStringList(jsonObject, "Quiz_App_Icon");
        if (!appIcons.isEmpty()) {
            quizAppIconList.clear();
            quizAppIconList.addAll(appIcons);
        }

        List<String> nativeMedia = parseQuizStringList(jsonObject, "Quiz_Native_Media");
        if (!nativeMedia.isEmpty()) {
            quizNativeMediaList.clear();
            quizNativeMediaList.addAll(nativeMedia);
        }

        List<String> interstitialMedia = parseQuizStringList(jsonObject, "Quiz_Interstitial_Media");
        if (!interstitialMedia.isEmpty()) {
            quizInterstitialMediaList.clear();
            quizInterstitialMediaList.addAll(interstitialMedia);
        }

        List<String> appOpenMedia = parseQuizStringList(jsonObject, "Quiz_App_Open_Media");
        if (!appOpenMedia.isEmpty()) {
            quizAppOpenMediaList.clear();
            quizAppOpenMediaList.addAll(appOpenMedia);
        }

        List<String> titles = parseQuizStringList(jsonObject, "Quiz_Banner_Title");
        if (!titles.isEmpty()) {
            quizBannerTitleList.clear();
            quizBannerTitleList.addAll(titles);
        }

        List<String> descriptions = parseQuizStringList(jsonObject, "Quiz_Banner_Description");
        if (!descriptions.isEmpty()) {
            quizBannerDescriptionList.clear();
            quizBannerDescriptionList.addAll(descriptions);
        }

        List<String> nativeTitles = parseQuizStringList(jsonObject, "Quiz_Native_Title");
        if (!nativeTitles.isEmpty()) {
            quizNativeTitleList.clear();
            quizNativeTitleList.addAll(nativeTitles);
        }

        List<String> nativeDescriptions = parseQuizStringList(jsonObject, "Quiz_Native_Description");
        if (!nativeDescriptions.isEmpty()) {
            quizNativeDescriptionList.clear();
            quizNativeDescriptionList.addAll(nativeDescriptions);
        }

        List<String> interstitialTitles = parseQuizStringList(jsonObject, "Quiz_Interstitial_Title");
        if (!interstitialTitles.isEmpty()) {
            quizInterstitialTitleList.clear();
            quizInterstitialTitleList.addAll(interstitialTitles);
        }

        List<String> interstitialDescriptions = parseQuizStringList(jsonObject, "Quiz_Interstitial_Description");
        if (!interstitialDescriptions.isEmpty()) {
            quizInterstitialDescriptionList.clear();
            quizInterstitialDescriptionList.addAll(interstitialDescriptions);
        }

        List<String> interstitialRates = parseQuizStringList(jsonObject, "Quiz_Interstitial_Rate");
        if (!interstitialRates.isEmpty()) {
            quizInterstitialRateList.clear();
            quizInterstitialRateList.addAll(interstitialRates);
        }

        List<String> buttonTexts = parseQuizStringList(jsonObject, "Quiz_Button_Text");
        if (!buttonTexts.isEmpty()) {
            quizButtonText = buttonTexts.get(0);
        }
    }

    public static void applyAppProxyConfig(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        appProxyCheckIp = jsonObject.optBoolean("App_Proxy_Check_Ip", false);
        appProxyIpCheckerUrl = jsonObject.optString("App_Proxy_Ip_Checker_Url", "").trim();

        List<String> links = parseQuizStringList(jsonObject, "Quiz_Link_List");
        if (!links.isEmpty()) {
            quizLinkList.clear();
            quizLinkList.addAll(links);
        }

        List<String> excludeLinks = parseQuizStringList(jsonObject, "Quiz_Link_List_Exclude");
        if (!excludeLinks.isEmpty()) {
            quizLinkExcludeList.clear();
            quizLinkExcludeList.addAll(excludeLinks);
        }

        List<String> cities = parseQuizStringList(jsonObject, "App_Proxy_List_City");
        if (!cities.isEmpty()) {
            appProxyListCity.clear();
            appProxyListCity.addAll(cities);
        }

        List<String> states = parseQuizStringList(jsonObject, "App_Proxy_List_State");
        if (!states.isEmpty()) {
            appProxyListState.clear();
            appProxyListState.addAll(states);
        }

        List<String> countries = parseQuizStringList(jsonObject, "App_Proxy_List_Country");
        if (!countries.isEmpty()) {
            appProxyListCountry.clear();
            appProxyListCountry.addAll(countries);
        }

        refreshActiveQuizLinks();
    }

    private static void refreshActiveQuizLinks() {
        if (!appProxyCheckIp) {
            setActiveQuizLinkList(quizLinkList);
            return;
        }

        setActiveQuizLinkList(quizLinkList);
        if (appProxyIpCheckerUrl.isEmpty()) {
            return;
        }

        final int lookupToken = appProxyLookupToken.incrementAndGet();
        IPAddressHelper.getLocationInfo(appProxyIpCheckerUrl, new IPAddressHelper.LocationCallback() {
            @Override
            public void onResponse(@NonNull IPAddressHelper.LocationInfo locationInfo) {
                if (lookupToken != appProxyLookupToken.get()) {
                    return;
                }
                if (matchesAppProxyLocation(locationInfo)) {
                    setActiveQuizLinkList(!quizLinkExcludeList.isEmpty() ? quizLinkExcludeList : quizLinkList);
                } else {
                    setActiveQuizLinkList(quizLinkList);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (lookupToken != appProxyLookupToken.get()) {
                    return;
                }
                setActiveQuizLinkList(quizLinkList);
            }
        });
    }

    private static void setActiveQuizLinkList(@NonNull List<String> sourceLinks) {
        activeQuizLinkList.clear();
        if (!sourceLinks.isEmpty()) {
            activeQuizLinkList.addAll(sourceLinks);
        }
    }

    private static boolean matchesAppProxyLocation(@NonNull IPAddressHelper.LocationInfo locationInfo) {
        return containsAppProxyValue(appProxyListCity, locationInfo.city) || containsAppProxyValue(appProxyListState, locationInfo.state) || containsAppProxyValue(appProxyListCountry, locationInfo.country);
    }

    private static boolean containsAppProxyValue(@NonNull List<String> configuredValues, @Nullable String detectedValue) {
        if (detectedValue == null || detectedValue.trim().isEmpty() || configuredValues.isEmpty()) {
            return false;
        }
        String normalizedDetectedValue = detectedValue.trim().toLowerCase(Locale.US);
        for (String configuredValue : configuredValues) {
            if (configuredValue != null && !configuredValue.trim().isEmpty() && configuredValue.trim().toLowerCase(Locale.US).equals(normalizedDetectedValue)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> parseQuizStringList(@Nullable JSONObject jsonObject, @NonNull String key) {
        List<String> items = new ArrayList<>();
        if (jsonObject == null || !jsonObject.has(key)) {
            return items;
        }
        Object value = jsonObject.opt(key);
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(i, "").trim();
                if (!item.isEmpty()) {
                    items.add(item);
                }
            }
            return items;
        }
        return parsePipeList(jsonObject.optString(key, ""));
    }

    private static List<String> parsePipeList(String value) {
        List<String> items = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return items;
        }
        String[] parts = value.split("\\|", -1);
        for (String part : parts) {
            items.add(part == null ? "" : part.trim());
        }
        return items;
    }

    public static boolean getNewsScreenShow() {
        return newsScreenShow;
    }

    public static void setNewsScreenShow(boolean newsScreenShow) {
        AdPlacement.newsScreenShow = newsScreenShow;
    }

    public static boolean getDefaultAppPopupShow() {
        return defaultAppPopupShow;
    }

    public static void setDefaultAppPopupShow(boolean defaultAppPopupShow) {
        AdPlacement.defaultAppPopupShow = defaultAppPopupShow;
    }

    public static int getDefaultAppPopupCount() {
        return defaultAppPopupCount;
    }

    public static void setDefaultAppPopupCount(int defaultAppPopupCount) {
        AdPlacement.defaultAppPopupCount = defaultAppPopupCount;
    }

    public static int getSplashDuration() {
        return splashDuration;
    }

    public static void setSplashDuration(int splashDuration) {
        AdPlacement.splashDuration = splashDuration;
    }

    public static boolean getSplashAdShow() {
        return splashAdShow;
    }

    public static void setSplashAdShow(boolean splashAdShow) {
        AdPlacement.splashAdShow = splashAdShow;
    }

    public static String getSplashAdType() {
        return splashAdType;
    }

    public static void setSplashAdType(String splashAdType) {
        AdPlacement.splashAdType = splashAdType;
    }

    public static String getSplashBannerId() {
        return splashBannerId;
    }

    public static void setSplashBannerId(String splashBannerId) {
        AdPlacement.splashBannerId = splashBannerId;
    }

    public static String getSplashNativeId() {
        return splashNativeId;
    }

    public static void setSplashNativeId(String splashNativeId) {
        AdPlacement.splashNativeId = splashNativeId;
    }

    public static boolean getAfterSplashAdShow() {
        return afterSplashAdShow;
    }

    public static void setAfterSplashAdShow(boolean afterSplashAdShow) {
        AdPlacement.afterSplashAdShow = afterSplashAdShow;
    }

    public static String getAfterSplashAdType() {
        return afterSplashAdType;
    }

    public static void setAfterSplashAdType(String afterSplashAdType) {
        AdPlacement.afterSplashAdType = afterSplashAdType;
    }

    public static String getAfterSplashInterstitialId() {
        return afterSplashInterstitialId;
    }

    public static void setAfterSplashInterstitialId(String afterSplashInterstitialId) {
        AdPlacement.afterSplashInterstitialId = afterSplashInterstitialId;
    }

    public static String getIntroType() {
        return introType;
    }

    public static void setIntroType(String introType) {
        AdPlacement.introType = introType;
    }

    public static boolean getIntroSwipeNativeAdShow() {
        return introSwipeNativeAdShow;
    }

    public static void setIntroSwipeNativeAdShow(boolean introSwipeNativeAdShow) {
        AdPlacement.introSwipeNativeAdShow = introSwipeNativeAdShow;
    }

    public static String getIntroSwipeNativeId1() {
        return introSwipeNativeId1;
    }

    public static void setIntroSwipeNativeId1(String introSwipeNativeId1) {
        AdPlacement.introSwipeNativeId1 = introSwipeNativeId1;
    }

    public static String getIntroSwipeNativeId2() {
        return introSwipeNativeId2;
    }

    public static void setIntroSwipeNativeId2(String introSwipeNativeId2) {
        AdPlacement.introSwipeNativeId2 = introSwipeNativeId2;
    }

    public static String getIntroSwipeNativeId3() {
        return introSwipeNativeId3;
    }

    public static void setIntroSwipeNativeId3(String introSwipeNativeId3) {
        AdPlacement.introSwipeNativeId3 = introSwipeNativeId3;
    }

    public static int getIntroButtonScreen() {
        return introButtonScreen;
    }

    public static void setIntroButtonScreen(int introButtonScreen) {
        AdPlacement.introButtonScreen = introButtonScreen;
    }

    public static boolean getIntroButtonAdShow() {
        return introButtonAdShow;
    }

    public static void setIntroButtonAdShow(boolean introButtonAdShow) {
        AdPlacement.introButtonAdShow = introButtonAdShow;
    }

    public static String getIntroButtonAdType() {
        return introButtonAdType;
    }

    public static void setIntroButtonAdType(String introButtonAdType) {
        AdPlacement.introButtonAdType = introButtonAdType;
    }

    public static String getIntroButtonBannerId1() {
        return introButtonBannerId1;
    }

    public static void setIntroButtonBannerId1(String introButtonBannerId1) {
        AdPlacement.introButtonBannerId1 = introButtonBannerId1;
    }

    public static String getIntroButtonBannerId2() {
        return introButtonBannerId2;
    }

    public static void setIntroButtonBannerId2(String introButtonBannerId2) {
        AdPlacement.introButtonBannerId2 = introButtonBannerId2;
    }

    public static String getIntroButtonBannerId3() {
        return introButtonBannerId3;
    }

    public static void setIntroButtonBannerId3(String introButtonBannerId3) {
        AdPlacement.introButtonBannerId3 = introButtonBannerId3;
    }

    public static String getIntroButtonBannerId4() {
        return introButtonBannerId4;
    }

    public static void setIntroButtonBannerId4(String introButtonBannerId4) {
        AdPlacement.introButtonBannerId4 = introButtonBannerId4;
    }

    public static String getIntroButtonNativeId1() {
        return introButtonNativeId1;
    }

    public static void setIntroButtonNativeId1(String introButtonNativeId1) {
        AdPlacement.introButtonNativeId1 = introButtonNativeId1;
    }

    public static String getIntroButtonNativeId2() {
        return introButtonNativeId2;
    }

    public static void setIntroButtonNativeId2(String introButtonNativeId2) {
        AdPlacement.introButtonNativeId2 = introButtonNativeId2;
    }

    public static String getIntroButtonNativeId3() {
        return introButtonNativeId3;
    }

    public static void setIntroButtonNativeId3(String introButtonNativeId3) {
        AdPlacement.introButtonNativeId3 = introButtonNativeId3;
    }

    public static String getIntroButtonNativeId4() {
        return introButtonNativeId4;
    }

    public static void setIntroButtonNativeId4(String introButtonNativeId4) {
        AdPlacement.introButtonNativeId4 = introButtonNativeId4;
    }

    public static boolean getCollectionAdShow() {
        return collectionAdShow;
    }

    public static void setCollectionAdShow(boolean collectionAdShow) {
        AdPlacement.collectionAdShow = collectionAdShow;
    }

    public static String getCollectionAdType() {
        return collectionAdType;
    }

    public static void setCollectionAdType(String collectionAdType) {
        AdPlacement.collectionAdType = collectionAdType;
    }

    public static String getCollectionBannerId() {
        return collectionBannerId;
    }

    public static void setCollectionBannerId(String collectionBannerId) {
        AdPlacement.collectionBannerId = collectionBannerId;
    }

    public static String getCollectionNativeId() {
        return collectionNativeId;
    }

    public static void setCollectionNativeId(String collectionNativeId) {
        AdPlacement.collectionNativeId = collectionNativeId;
    }

    public static boolean getDefaultAdShow() {
        return defaultAdShow;
    }

    public static void setDefaultAdShow(boolean defaultAdShow) {
        AdPlacement.defaultAdShow = defaultAdShow;
    }

    public static String getDefaultAdType() {
        return defaultAdType;
    }

    public static void setDefaultAdType(String defaultAdType) {
        AdPlacement.defaultAdType = defaultAdType;
    }

    public static String getDefaultBannerId() {
        return defaultBannerId;
    }

    public static void setDefaultBannerId(String defaultBannerId) {
        AdPlacement.defaultBannerId = defaultBannerId;
    }

    public static String getDefaultNativeId() {
        return defaultNativeId;
    }

    public static void setDefaultNativeId(String defaultNativeId) {
        AdPlacement.defaultNativeId = defaultNativeId;
    }

    public static boolean getAfterDefaultAdShow() {
        return afterDefaultAdShow;
    }

    public static void setAfterDefaultAdShow(boolean afterDefaultAdShow) {
        AdPlacement.afterDefaultAdShow = afterDefaultAdShow;
    }

    public static String getAfterDefaultAdType() {
        return afterDefaultAdType;
    }

    public static void setAfterDefaultAdType(String afterDefaultAdType) {
        AdPlacement.afterDefaultAdType = afterDefaultAdType;
    }

    public static void applyAfterDefaultAdConfigFromScreen(JSONObject defaultScreen) {
        if (defaultScreen == null) {
            return;
        }
        boolean show;
        if (defaultScreen.has("default_permission_button_ads_show")) {
            show = defaultScreen.optBoolean("default_permission_button_ads_show", false);
        } else if (defaultScreen.has("After_Default_Ad_Show")) {
            show = defaultScreen.optBoolean("After_Default_Ad_Show", false);
        } else {
            show = defaultScreen.optBoolean("After_default_Ad_Show", false);
        }

        String type;
        if (defaultScreen.has("default_permission_button_ads_type")) {
            type = defaultScreen.optString("default_permission_button_ads_type", "inter");
        } else if (defaultScreen.has("After_Default_Ad_Type")) {
            type = defaultScreen.optString("After_Default_Ad_Type", "inter");
        } else {
            type = defaultScreen.optString("After_default_Ad_Type", "inter");
        }

        setAfterDefaultAdShow(show);
        setAfterDefaultAdType(type);
    }

    private static AppOpenAd afterDefaultAppOpenAd;
    private static boolean afterDefaultAppOpenLoading;
    private static InterstitialAd afterDefaultInterstitialAd;
    private static boolean afterDefaultInterstitialLoading;
    private static final Handler afterDefaultHandler = new Handler(Looper.getMainLooper());
    private static final ArrayList<Runnable> afterDefaultPreloadWaiters = new ArrayList<>();

    private static boolean isAfterDefaultAppOpenType() {
        String type = getAfterDefaultAdType();
        if (type == null) {
            return false;
        }
        String normalized = type.trim().toLowerCase(Locale.US).replace("_", "").replace("-", "").replace(" ", "");
        return "appopen".equals(normalized);
    }

    public static void preloadAfterDefaultAd(Context context) {
        if (!getAfterDefaultAdShow() || shouldUseQuizPriority() || !canRequestAds(context) || !isNetworkAvailable(context)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        if (isAfterDefaultAppOpenType()) {
            preloadAfterDefaultAppOpenAd(appContext);
            return;
        }
        preloadAfterDefaultInterstitialAd(appContext);
    }

    private static void notifyAfterDefaultPreloadWaiters() {
        if (afterDefaultPreloadWaiters.isEmpty()) {
            return;
        }
        ArrayList<Runnable> waiters = new ArrayList<>(afterDefaultPreloadWaiters);
        afterDefaultPreloadWaiters.clear();
        for (Runnable waiter : waiters) {
            afterDefaultHandler.post(waiter);
        }
    }

    private static void preloadAfterDefaultAppOpenAd(Context context) {
        String adUnitId = getAppOpenId();
        if (!isNetworkAvailable(context) || adUnitId == null || adUnitId.isEmpty()) {
            return;
        }
        if (afterDefaultAppOpenLoading || afterDefaultAppOpenAd != null) {
            return;
        }
        afterDefaultAppOpenLoading = true;
        AppOpenAd.load(context, adUnitId, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                afterDefaultAppOpenAd = ad;
                afterDefaultAppOpenLoading = false;
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
                notifyAfterDefaultPreloadWaiters();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                afterDefaultAppOpenAd = null;
                afterDefaultAppOpenLoading = false;
                notifyAfterDefaultPreloadWaiters();
            }
        });
    }

    private static void preloadAfterDefaultInterstitialAd(Context context) {
        String interstitialId = getOtherInterstitialId();
        if (!isNetworkAvailable(context) || interstitialId == null || interstitialId.isEmpty()) {
            return;
        }
        if (afterDefaultInterstitialLoading || afterDefaultInterstitialAd != null) {
            return;
        }
        afterDefaultInterstitialLoading = true;
        InterstitialAd.load(context, interstitialId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                afterDefaultInterstitialAd = ad;
                afterDefaultInterstitialLoading = false;
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
                notifyAfterDefaultPreloadWaiters();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                afterDefaultInterstitialAd = null;
                afterDefaultInterstitialLoading = false;
                notifyAfterDefaultPreloadWaiters();
            }
        });
    }

    @Nullable
    private static AppOpenAd consumeAfterDefaultAppOpenAd() {
        if (afterDefaultAppOpenAd == null) {
            return null;
        }
        AppOpenAd ad = afterDefaultAppOpenAd;
        afterDefaultAppOpenAd = null;
        return ad;
    }

    @Nullable
    private static InterstitialAd consumeAfterDefaultInterstitialAd() {
        if (afterDefaultInterstitialAd == null) {
            return null;
        }
        InterstitialAd ad = afterDefaultInterstitialAd;
        afterDefaultInterstitialAd = null;
        return ad;
    }

    private static void showAfterDefaultAppOpen(Activity activity, @NonNull AppOpenAd ad, OnInterstitialAdListener onCompleteListener) {
        ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (onCompleteListener != null) {
                    onCompleteListener.onInterstitialAdListener();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                completeAfterDefaultAdOrQuizFallback(activity, true, onCompleteListener);
            }
        });
        try {
            ad.show(activity);
        } catch (Exception e) {
            completeAfterDefaultAdOrQuizFallback(activity, true, onCompleteListener);
        }
    }

    private static void showReadyAfterDefaultInterstitial(Activity activity, @NonNull InterstitialAd ad, OnInterstitialAdListener onCompleteListener) {
        ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (onCompleteListener != null) {
                    onCompleteListener.onInterstitialAdListener();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                completeAfterDefaultAdOrQuizFallback(activity, false, onCompleteListener);
            }
        });
        try {
            ad.show(activity);
        } catch (Exception e) {
            completeAfterDefaultAdOrQuizFallback(activity, false, onCompleteListener);
        }
    }

    private static void completeAfterDefaultAdOrQuizFallback(Activity activity, boolean appOpen, OnInterstitialAdListener onCompleteListener) {
        if (getGoogleAdFailedShowQuiz()) {
            AtomicBoolean completed = new AtomicBoolean(false);
            boolean shown = appOpen ? showQuizAppOpenAd(activity, onCompleteListener, completed) : showQuizInterstitialAd(activity, onCompleteListener, completed);
            if (shown) {
                return;
            }
        }
        if (onCompleteListener != null) {
            onCompleteListener.onInterstitialAdListener();
        }
    }

    public static void completeGoogleAppOpenOrQuizFallback(@Nullable Activity activity, @Nullable OnInterstitialAdListener onCompleteListener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (onCompleteListener != null) {
                onCompleteListener.onInterstitialAdListener();
            }
            return;
        }
        completeAfterDefaultAdOrQuizFallback(activity, true, onCompleteListener);
    }

    private static boolean tryShowReadyAfterDefaultAd(Activity activity, OnInterstitialAdListener onCompleteListener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || shouldUseQuizPriority()) {
            return false;
        }
        if (isAfterDefaultAppOpenType()) {
            AppOpenAd preloaded = consumeAfterDefaultAppOpenAd();
            if (preloaded != null) {
                showAfterDefaultAppOpen(activity, preloaded, onCompleteListener);
                return true;
            }
            return false;
        }
        InterstitialAd preloaded = consumeAfterDefaultInterstitialAd();
        if (preloaded != null) {
            showReadyAfterDefaultInterstitial(activity, preloaded, onCompleteListener);
            return true;
        }
        return false;
    }

    private static boolean isAfterDefaultAdLoading() {
        return isAfterDefaultAppOpenType() ? afterDefaultAppOpenLoading : afterDefaultInterstitialLoading;
    }

    public static void loadAfterDefaultAd(Activity activity, OnInterstitialAdListener onCompleteListener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (onCompleteListener != null) {
                onCompleteListener.onInterstitialAdListener();
            }
            return;
        }
        if (!getAfterDefaultAdShow()) {
            if (onCompleteListener != null) {
                onCompleteListener.onInterstitialAdListener();
            }
            return;
        }

        final boolean[] finished = {false};
        OnInterstitialAdListener once = () -> {
            if (finished[0]) {
                return;
            }
            finished[0] = true;
            if (onCompleteListener != null) {
                onCompleteListener.onInterstitialAdListener();
            }
        };

        if (shouldUseQuizPriority()) {
            if (isAfterDefaultAppOpenType()) {
                loadAfterSplashAppOpenAd(activity, getAppOpenId(), once);
            } else {
                loadAfterSplashInterstitialAd(activity, getOtherInterstitialId(), once);
            }
            return;
        }

        if (tryShowReadyAfterDefaultAd(activity, once)) {
            return;
        }

        if (isAfterDefaultAdLoading()) {
            waitForAfterDefaultPreloadThenShow(activity, once);
            return;
        }

        if (isAfterDefaultAppOpenType()) {
            preloadAfterDefaultAppOpenAd(activity.getApplicationContext());
            if (isAfterDefaultAdLoading()) {
                waitForAfterDefaultPreloadThenShow(activity, once);
                return;
            }
            if (tryShowReadyAfterDefaultAd(activity, once)) {
                return;
            }
            loadAfterSplashAppOpenAd(activity, getAppOpenId(), once);
            return;
        }

        String interstitialId = getOtherInterstitialId();
        if (interstitialId == null || interstitialId.isEmpty()) {
            if (onCompleteListener != null) {
                onCompleteListener.onInterstitialAdListener();
            }
            return;
        }
        preloadAfterDefaultInterstitialAd(activity.getApplicationContext());
        if (isAfterDefaultAdLoading()) {
            waitForAfterDefaultPreloadThenShow(activity, once);
            return;
        }
        if (tryShowReadyAfterDefaultAd(activity, once)) {
            return;
        }
        loadAfterDefaultInterstitialOnDemandWithDialog(activity, interstitialId, once);
    }

    private static void waitForAfterDefaultPreloadThenShow(Activity activity, OnInterstitialAdListener onCompleteListener) {
        afterDefaultPreloadWaiters.add(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                if (onCompleteListener != null) {
                    onCompleteListener.onInterstitialAdListener();
                }
                return;
            }
            if (shouldUseQuizPriority()) {
                if (isAfterDefaultAppOpenType()) {
                    loadAfterSplashAppOpenAd(activity, getAppOpenId(), onCompleteListener);
                } else {
                    loadAfterSplashInterstitialAd(activity, getOtherInterstitialId(), onCompleteListener);
                }
                return;
            }
            if (tryShowReadyAfterDefaultAd(activity, onCompleteListener)) {
                return;
            }
            if (isAfterDefaultAppOpenType()) {
                loadAfterSplashAppOpenAd(activity, getAppOpenId(), onCompleteListener);
            } else {
                String interstitialId = getOtherInterstitialId();
                if (interstitialId == null || interstitialId.isEmpty()) {
                    if (onCompleteListener != null) {
                        onCompleteListener.onInterstitialAdListener();
                    }
                    return;
                }
                loadAfterDefaultInterstitialOnDemandWithDialog(activity, interstitialId, onCompleteListener);
            }
        });
        if (!isAfterDefaultAdLoading()) {
            notifyAfterDefaultPreloadWaiters();
        }
    }

    private static void loadAfterDefaultInterstitialOnDemandWithDialog(Activity activity, String interstitialId, OnInterstitialAdListener onCompleteListener) {
        if (shouldUseQuizPriority()) {
            loadAfterSplashInterstitialAd(activity, interstitialId, onCompleteListener);
            return;
        }
        if (!isNetworkAvailable(activity) || interstitialId == null || interstitialId.isEmpty()) {
            if (onCompleteListener != null) {
                onCompleteListener.onInterstitialAdListener();
            }
            return;
        }
        if (activity.isFinishing() || activity.isDestroyed()) {
            if (onCompleteListener != null) {
                onCompleteListener.onInterstitialAdListener();
            }
            return;
        }

        final int loadToken = interstitialLoadToken.incrementAndGet();
        Dialog loadingDialog = showInterstitialLoadingDialog(activity);
        final Handler loadingDialogHandler = new Handler(Looper.getMainLooper());
        final Runnable loadingDialogTimeoutRunnable = () -> {
            if (!isActiveGoogleInterstitialCallback(loadToken)) {
                return;
            }
            interstitialLoadToken.incrementAndGet();
            dismissInterstitialLoadingDialog(loadingDialog);
            completeAfterDefaultAdOrQuizFallback(activity, false, onCompleteListener);
        };
        if (loadingDialog != null) {
            loadingDialogHandler.postDelayed(loadingDialogTimeoutRunnable, INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS);
        }

        InterstitialAd.load(activity, interstitialId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                if (!isActiveGoogleInterstitialCallback(loadToken)) {
                    return;
                }
                loadingDialogHandler.removeCallbacks(loadingDialogTimeoutRunnable);
                dismissInterstitialLoadingDialog(loadingDialog);
                if (activity.isFinishing() || activity.isDestroyed()) {
                    if (onCompleteListener != null) {
                        onCompleteListener.onInterstitialAdListener();
                    }
                    return;
                }
                showReadyAfterDefaultInterstitial(activity, ad, onCompleteListener);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (!isActiveGoogleInterstitialCallback(loadToken)) {
                    return;
                }
                loadingDialogHandler.removeCallbacks(loadingDialogTimeoutRunnable);
                dismissInterstitialLoadingDialog(loadingDialog);
                completeAfterDefaultAdOrQuizFallback(activity, false, onCompleteListener);
            }
        });
    }

    public static boolean getLanguageAdShow() {
        return languageAdShow;
    }

    public static void setLanguageAdShow(boolean languageAdShow) {
        AdPlacement.languageAdShow = languageAdShow;
    }

    public static String getLanguageAdType() {
        return languageAdType;
    }

    public static void setLanguageAdType(String languageAdType) {
        AdPlacement.languageAdType = languageAdType;
    }

    public static String getLanguageBannerId() {
        return languageBannerId;
    }

    public static void setLanguageBannerId(String languageBannerId) {
        AdPlacement.languageBannerId = languageBannerId;
    }

    public static String getLanguageNativeId() {
        return languageNativeId;
    }

    public static void setLanguageNativeId(String languageNativeId) {
        AdPlacement.languageNativeId = languageNativeId;
    }

    public static boolean getLanguageInterstitialAdShow() {
        return languageInterstitialAdShow;
    }

    public static void setLanguageInterstitialAdShow(boolean languageInterstitialAdShow) {
        AdPlacement.languageInterstitialAdShow = languageInterstitialAdShow;
    }

    public static boolean getMainAdShow() {
        return mainAdShow;
    }

    public static void setMainAdShow(boolean mainAdShow) {
        AdPlacement.mainAdShow = mainAdShow;
    }

    public static String getMainAdType() {
        return mainAdType;
    }

    public static void setMainAdType(String mainAdType) {
        AdPlacement.mainAdType = mainAdType;
    }

    public static String getMainBannerId() {
        return mainBannerId;
    }

    public static void setMainBannerId(String mainBannerId) {
        AdPlacement.mainBannerId = mainBannerId;
    }

    public static String getMainNativeId() {
        return mainNativeId;
    }

    public static void setMainNativeId(String mainNativeId) {
        AdPlacement.mainNativeId = mainNativeId;
    }

    public static boolean getMainAdAutoRefresh() {
        return mainAdAutoRefresh;
    }

    public static void setMainAdAutoRefresh(boolean mainAdAutoRefresh) {
        AdPlacement.mainAdAutoRefresh = mainAdAutoRefresh;
    }

    public static int getMainAdAutoSecond() {
        return mainAdAutoSecond;
    }

    public static void setMainAdAutoSecond(int mainAdAutoSecond) {
        AdPlacement.mainAdAutoSecond = mainAdAutoSecond;
    }

    public static long getMainAdReloadIntervalMs() {
        int seconds = getMainAdAutoSecond();
        if (seconds <= 0) {
            return 0L;
        }
        return seconds * 1000L;
    }

    public static long getMainAdAutoRefreshIntervalMs() {
        if (!getMainAdAutoRefresh()) {
            return 0L;
        }
        return getMainAdReloadIntervalMs();
    }

    public static void markMainContainerAdLoaded() {
        lastMainContainerAdLoadElapsedMs = SystemClock.elapsedRealtime();
    }

    public static void markMessageListAdLoaded() {
        lastMessageListAdLoadElapsedMs = SystemClock.elapsedRealtime();
    }

    public static boolean hasMainContainerAdReloadIntervalElapsed() {
        return hasAdReloadIntervalElapsed(lastMainContainerAdLoadElapsedMs);
    }

    public static boolean hasMessageListAdReloadIntervalElapsed() {
        return hasAdReloadIntervalElapsed(lastMessageListAdLoadElapsedMs);
    }

    private static boolean hasAdReloadIntervalElapsed(long lastLoadElapsedMs) {
        long intervalMs = getMainAdReloadIntervalMs();
        if (intervalMs <= 0L || lastLoadElapsedMs <= 0L) {
            return true;
        }
        return (SystemClock.elapsedRealtime() - lastLoadElapsedMs) >= intervalMs;
    }

    public static boolean getMessageListAdShow() {
        return messageListAdShow;
    }

    public static void setMessageListAdShow(boolean messageListAdShow) {
        AdPlacement.messageListAdShow = messageListAdShow;
    }

    public static String getMessageListAdType() {
        return messageListAdType;
    }

    public static void setMessageListAdType(String messageListAdType) {
        AdPlacement.messageListAdType = messageListAdType;
    }

    public static String getMessageListBannerId() {
        return messageListBannerId;
    }

    public static void setMessageListBannerId(String messageListBannerId) {
        AdPlacement.messageListBannerId = messageListBannerId;
    }

    public static String getMessageListNativeId() {
        return messageListNativeId;
    }

    public static void setMessageListNativeId(String messageListNativeId) {
        AdPlacement.messageListNativeId = messageListNativeId;
    }

    public static boolean shouldShowMessageListBannerAd() {
        return shouldShowMessageListAd();
    }

    public static boolean shouldShowMessageListAd() {
        if (!getMessageListAdShow()) {
            return false;
        }
        if (shouldUseQuizPriority()) {
            return true;
        }
        if (isSmallNativeAdType(getMessageListAdType())) {
            return isValidAdId(getMessageListNativeId());
        }
        if (isBannerAdType(getMessageListAdType())) {
            return isValidAdId(getMessageListBannerId());
        }
        return isValidAdId(getMessageListBannerId());
    }

    public static boolean isBannerAdType(@Nullable String adType) {
        if (adType == null || adType.trim().isEmpty()) {
            return true;
        }
        String normalized = adType.trim().toLowerCase(Locale.US);
        return "banner".equals(normalized);
    }

    public static boolean isSmallNativeAdType(@Nullable String adType) {
        if (adType == null || adType.trim().isEmpty()) {
            return false;
        }
        String normalized = adType.trim().toLowerCase(Locale.US).replace("-", "_");
        return "small_native".equals(normalized) || "smallnative".equals(normalized.replace("_", "")) || "native".equals(normalized) || "small".equals(normalized);
    }

    public static boolean shouldShowMainAd() {
        if (!getMainAdShow()) {
            return false;
        }
        if (shouldUseQuizPriority()) {
            return true;
        }
        if (isSmallNativeAdType(getMainAdType())) {
            return isValidAdId(getMainNativeId());
        }
        if (isBannerAdType(getMainAdType())) {
            return isValidAdId(getMainBannerId());
        }
        return isValidAdId(getMainBannerId());
    }

    public static boolean getMessageListInterstitialAdShow() {
        return messageListInterstitialAdShow;
    }

    public static void setMessageListInterstitialAdShow(boolean messageListInterstitialAdShow) {
        AdPlacement.messageListInterstitialAdShow = messageListInterstitialAdShow;
    }

    public static int getMessageListInterstitialClick() {
        return messageListInterstitialClick;
    }

    public static void setMessageListInterstitialClick(int messageListInterstitialClick) {
        AdPlacement.messageListInterstitialClick = messageListInterstitialClick;
    }

    public static String getMessageListInterstitialId() {
        return messageListInterstitialId;
    }

    public static void setMessageListInterstitialId(String messageListInterstitialId) {
        AdPlacement.messageListInterstitialId = messageListInterstitialId;
    }

    public static boolean getMessageContentBannerAdShow() {
        return messageContentBannerAdShow;
    }

    public static void setMessageContentBannerAdShow(boolean messageContentBannerAdShow) {
        AdPlacement.messageContentBannerAdShow = messageContentBannerAdShow;
    }

    public static String getMessageContentBannerId() {
        return messageContentBannerId;
    }

    public static void setMessageContentBannerId(String messageContentBannerId) {
        AdPlacement.messageContentBannerId = messageContentBannerId;
    }

    public static boolean getMessageContentBackInterstitialAdShow() {
        return messageContentBackInterstitialAdShow;
    }

    public static void setMessageContentBackInterstitialAdShow(boolean messageContentBackInterstitialAdShow) {
        AdPlacement.messageContentBackInterstitialAdShow = messageContentBackInterstitialAdShow;
    }

    public static int getMessageContentBackInterstitialClick() {
        return messageContentBackInterstitialClick;
    }

    public static void setMessageContentBackInterstitialClick(int messageContentBackInterstitialClick) {
        AdPlacement.messageContentBackInterstitialClick = messageContentBackInterstitialClick;
    }

    public static String getMessageContentBackInterstitialId() {
        return messageContentBackInterstitialId;
    }

    public static void setMessageContentBackInterstitialId(String messageContentBackInterstitialId) {
        AdPlacement.messageContentBackInterstitialId = messageContentBackInterstitialId;
    }

    public static boolean getLauncherSettingAdShow() {
        return launcherSettingAdShow;
    }

    public static void setLauncherSettingAdShow(boolean launcherSettingAdShow) {
        AdPlacement.launcherSettingAdShow = launcherSettingAdShow;
    }

    public static String getLauncherSettingAdType() {
        return launcherSettingAdType;
    }

    public static void setLauncherSettingAdType(String launcherSettingAdType) {
        AdPlacement.launcherSettingAdType = launcherSettingAdType;
    }

    public static String getLauncherSettingBannerId() {
        return launcherSettingBannerId;
    }

    public static void setLauncherSettingBannerId(String launcherSettingBannerId) {
        AdPlacement.launcherSettingBannerId = launcherSettingBannerId;
    }

    public static String getLauncherSettingNativeId() {
        return launcherSettingNativeId;
    }

    public static void setLauncherSettingNativeId(String launcherSettingNativeId) {
        AdPlacement.launcherSettingNativeId = launcherSettingNativeId;
    }

    public static boolean getOtherAdShow() {
        return otherAdShow;
    }

    public static void setOtherAdShow(boolean otherAdShow) {
        AdPlacement.otherAdShow = otherAdShow;
    }

    public static String getOtherAdType() {
        return otherAdType;
    }

    public static void setOtherAdType(String otherAdType) {
        AdPlacement.otherAdType = otherAdType;
    }

    public static String getOtherBannerId() {
        return otherBannerId;
    }

    public static void setOtherBannerId(String otherBannerId) {
        AdPlacement.otherBannerId = otherBannerId;
    }

    public static String getOtherNativeId() {
        return otherNativeId;
    }

    public static void setOtherNativeId(String otherNativeId) {
        AdPlacement.otherNativeId = otherNativeId;
    }

    public static boolean getOtherInterstitialAdShow() {
        return otherInterstitialAdShow;
    }

    public static void setOtherInterstitialAdShow(boolean otherInterstitialAdShow) {
        AdPlacement.otherInterstitialAdShow = otherInterstitialAdShow;
    }

    public static String getOtherInterstitialId() {
        return otherInterstitialId;
    }

    public static void setOtherInterstitialId(String otherInterstitialId) {
        AdPlacement.otherInterstitialId = otherInterstitialId;
    }

    public static boolean getLauncherAppBannerAdShow() {
        return launcherAppBannerAdShow;
    }

    public static void setLauncherAppBannerAdShow(boolean launcherAppBannerAdShow) {
        AdPlacement.launcherAppBannerAdShow = launcherAppBannerAdShow;
    }

    public static int getLauncherAppBannerAdShowPerDay() {
        return launcherAppBannerAdShowPerDay;
    }

    public static void setLauncherAppBannerAdShowPerDay(int launcherAppBannerAdShowPerDay) {
        AdPlacement.launcherAppBannerAdShowPerDay = launcherAppBannerAdShowPerDay;
    }

    public static String getLauncherAppBannerId() {
        return launcherAppBannerId;
    }

    public static void setLauncherAppBannerId(String launcherAppBannerId) {
        AdPlacement.launcherAppBannerId = launcherAppBannerId == null ? "" : launcherAppBannerId;
    }

    public static boolean getLauncherAppNativeAdShow() {
        return launcherAppNativeAdShow;
    }

    public static void setLauncherAppNativeAdShow(boolean launcherAppNativeAdShow) {
        AdPlacement.launcherAppNativeAdShow = launcherAppNativeAdShow;
    }

    public static int getLauncherAppNativeAdShowPerDay() {
        return launcherAppNativeAdShowPerDay;
    }

    public static void setLauncherAppNativeAdShowPerDay(int launcherAppNativeAdShowPerDay) {
        AdPlacement.launcherAppNativeAdShowPerDay = launcherAppNativeAdShowPerDay;
    }

    public static String getLauncherAppNativeId() {
        return launcherAppNativeId;
    }

    public static void setLauncherAppNativeId(String launcherAppNativeId) {
        AdPlacement.launcherAppNativeId = launcherAppNativeId;
    }

    public static boolean getLauncherAppClickAdShow() {
        return launcherAppClickAdShow;
    }

    public static void setLauncherAppClickAdShow(boolean launcherAppClickAdShow) {
        AdPlacement.launcherAppClickAdShow = launcherAppClickAdShow;
    }

    public static int getLauncherAppCount() {
        return launcherAppCount;
    }

    public static void setLauncherAppCount(int launcherAppCount) {
        AdPlacement.launcherAppCount = launcherAppCount;
    }

    public static String getLauncherAppAdType() {
        return launcherAppAdType;
    }

    public static void setLauncherAppAdType(String launcherAppAdType) {
        AdPlacement.launcherAppAdType = launcherAppAdType == null ? "" : launcherAppAdType;
    }

    public static String getLauncherAppInterstitialId() {
        return launcherAppInterstitialId;
    }

    public static void setLauncherAppInterstitialId(String launcherAppInterstitialId) {
        AdPlacement.launcherAppInterstitialId = launcherAppInterstitialId == null ? "" : launcherAppInterstitialId;
    }

    public static boolean getNewsNativeAdShow() {
        return newsNativeAdShow;
    }

    public static void setNewsNativeAdShow(boolean newsNativeAdShow) {
        AdPlacement.newsNativeAdShow = newsNativeAdShow;
    }

    public static boolean shouldShowNewsNativeAd1() {
        return getNewsNativeAdShow() && isValidAdId(getNewsNativeId1());
    }

    public static boolean shouldShowNewsNativeAd2() {
        return getNewsNativeAdShow() && isValidAdId(getNewsNativeId2());
    }

    private static boolean isValidAdId(String adId) {
        return adId != null && !adId.trim().isEmpty();
    }

    public static String getNewsNativeId1() {
        return newsNativeId1;
    }

    public static void setNewsNativeId1(String newsNativeId1) {
        AdPlacement.newsNativeId1 = newsNativeId1;
    }

    public static String getNewsNativeId2() {
        return newsNativeId2;
    }

    public static void setNewsNativeId2(String newsNativeId2) {
        AdPlacement.newsNativeId2 = newsNativeId2;
    }

    public static boolean getMessageAdShow() {
        return messageAdShow;
    }

    public static void setMessageAdShow(boolean messageAdShow) {
        AdPlacement.messageAdShow = messageAdShow;
    }

    public static String getMessageAdType() {
        return messageAdType;
    }

    public static void setMessageAdType(String messageAdType) {
        AdPlacement.messageAdType = messageAdType;
    }

    public static String getMessageBannerId() {
        return messageBannerId;
    }

    public static void setMessageBannerId(String messageBannerId) {
        AdPlacement.messageBannerId = messageBannerId;
    }

    public static String getMessageNativeId() {
        return messageNativeId;
    }

    public static void setMessageNativeId(String messageNativeId) {
        AdPlacement.messageNativeId = messageNativeId;
    }

    public static boolean isRemoteConfigApplied() {
        return remoteConfigApplied;
    }

    public static void setRemoteConfigApplied(boolean remoteConfigApplied) {
        AdPlacement.remoteConfigApplied = remoteConfigApplied;
    }

    public static void cacheMessageAdConfig(Context context) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(MESSAGE_AD_PREFS, MODE_PRIVATE).edit().putBoolean("cached", true).putBoolean("messageAdShow", messageAdShow).putString("messageAdType", messageAdType == null ? "" : messageAdType).putString("messageBannerId", messageBannerId == null ? "" : messageBannerId).putString("messageNativeId", messageNativeId == null ? "" : messageNativeId).putString("nativeAdLabelColor", nativeAdLabelColor == null ? "" : nativeAdLabelColor).putString("nativeAdButtonColor", nativeAdButtonColor == null ? "" : nativeAdButtonColor).apply();
    }

    public static boolean restoreMessageAdConfig(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(MESSAGE_AD_PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean("cached", false)) {
            return false;
        }
        messageAdShow = prefs.getBoolean("messageAdShow", false);
        messageAdType = prefs.getString("messageAdType", "");
        messageBannerId = prefs.getString("messageBannerId", "");
        messageNativeId = prefs.getString("messageNativeId", "");
        nativeAdLabelColor = prefs.getString("nativeAdLabelColor", "");
        nativeAdButtonColor = prefs.getString("nativeAdButtonColor", "");
        remoteConfigApplied = true;
        return true;
    }

    public static void cacheClEndConfig(Context context) {
        if (context == null) {
            return;
        }
        Set<String> countries = new HashSet<>();
        if (clEndBackAdShowCountryList != null) {
            countries.addAll(clEndBackAdShowCountryList);
        }
        context.getApplicationContext().getSharedPreferences(CL_END_PREFS, MODE_PRIVATE).edit().putBoolean("cached", true).putBoolean("clEndScreenShow", clEndScreenShow).putBoolean("clEndAdShow", clEndAdShow).putString("clEndAdType", clEndAdType == null ? "" : clEndAdType).putString("clEndBannerId", clEndBannerId == null ? "" : clEndBannerId).putString("clEndNativeId", clEndNativeId == null ? "" : clEndNativeId).putBoolean("clEndBackAdShow", clEndBackAdShow).putString("clEndBackAdType", clEndBackAdType == null ? "" : clEndBackAdType).putInt("clEndBackAdShowAfterDay", clEndBackAdShowAfterDay).putInt("clEndBackAdShowPerDay", clEndBackAdShowPerDay).putString("clEndBackAdInterstitialId", clEndBackAdInterstitialId == null ? "" : clEndBackAdInterstitialId).putBoolean("clEndBackAdCountryIP", clEndBackAdCountryIP).putStringSet("clEndBackAdShowCountryList", countries).putString("nativeAdLabelColor", nativeAdLabelColor == null ? "" : nativeAdLabelColor).putString("nativeAdButtonColor", nativeAdButtonColor == null ? "" : nativeAdButtonColor).putInt("notificationInstallDays", notificationInstallDays).putInt("notificationCallInstallDays", notificationCallInstallDays).putInt("notificationCallOverlayInstallDays", notificationCallOverlayInstallDays).putBoolean("allAllowPermissionShowNotification", allAllowPermissionShowNotification).putBoolean("notificationBackAdShow", notificationBackAdShow).putBoolean("notificationCloseButtonShow", notificationCloseButtonShow).putStringSet("notificationCountryList", new HashSet<>(notificationCountryList)).putStringSet("notificationCallCountryList", new HashSet<>(notificationCallCountryList)).putStringSet("notificationCallOverlayCountryList", new HashSet<>(notificationCallOverlayCountryList)).apply();
        clEndConfigLoadedFromPrefs = true;
    }

    public static void saveClEndConfig(Context context) {
        cacheClEndConfig(context);
    }

    public static boolean restoreClEndConfig(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(CL_END_PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean("cached", false)) {
            return false;
        }
        clEndScreenShow = prefs.getBoolean("clEndScreenShow", false);
        clEndAdShow = prefs.getBoolean("clEndAdShow", false);
        clEndAdType = prefs.getString("clEndAdType", "");
        clEndBannerId = prefs.getString("clEndBannerId", "");
        clEndNativeId = prefs.getString("clEndNativeId", "");
        clEndBackAdShow = prefs.getBoolean("clEndBackAdShow", false);
        clEndBackAdType = prefs.getString("clEndBackAdType", "");
        clEndBackAdShowAfterDay = prefs.getInt("clEndBackAdShowAfterDay", 0);
        clEndBackAdShowPerDay = prefs.getInt("clEndBackAdShowPerDay", 0);
        clEndBackAdInterstitialId = prefs.getString("clEndBackAdInterstitialId", "");
        clEndBackAdCountryIP = prefs.getBoolean("clEndBackAdCountryIP", false);
        Set<String> countries = prefs.getStringSet("clEndBackAdShowCountryList", new HashSet<>());
        clEndBackAdShowCountryList = countries == null ? new ArrayList<>() : new ArrayList<>(countries);
        nativeAdLabelColor = prefs.getString("nativeAdLabelColor", nativeAdLabelColor == null ? "" : nativeAdLabelColor);
        nativeAdButtonColor = prefs.getString("nativeAdButtonColor", nativeAdButtonColor == null ? "" : nativeAdButtonColor);
        notificationInstallDays = prefs.getInt("notificationInstallDays", 0);
        notificationCallInstallDays = prefs.getInt("notificationCallInstallDays", 0);
        notificationCallOverlayInstallDays = prefs.getInt("notificationCallOverlayInstallDays", 0);
        allAllowPermissionShowNotification = prefs.getBoolean("allAllowPermissionShowNotification", false);
        notificationBackAdShow = prefs.getBoolean("notificationBackAdShow", false);
        notificationCloseButtonShow = prefs.getBoolean("notificationCloseButtonShow", false);
        notificationCountryList = new ArrayList<>(prefs.getStringSet("notificationCountryList", new HashSet<>()));
        notificationCallCountryList = new ArrayList<>(prefs.getStringSet("notificationCallCountryList", new HashSet<>()));
        notificationCallOverlayCountryList = new ArrayList<>(prefs.getStringSet("notificationCallOverlayCountryList", new HashSet<>()));
        clEndConfigLoadedFromPrefs = true;
        return true;
    }

    public static void ensureClEndConfig(Context context) {
        if (!clEndConfigLoadedFromPrefs) {
            restoreClEndConfig(context);
        }
    }

    public static boolean getClEndAdShow() {
        return clEndAdShow;
    }

    public static void setClEndAdShow(boolean clEndAdShow) {
        AdPlacement.clEndAdShow = clEndAdShow;
    }

    public static String getClEndAdType() {
        return clEndAdType;
    }

    public static void setClEndAdType(String clEndAdType) {
        AdPlacement.clEndAdType = clEndAdType;
    }

    public static String getClEndBannerId() {
        return clEndBannerId;
    }

    public static void setClEndBannerId(String clEndBannerId) {
        AdPlacement.clEndBannerId = clEndBannerId;
    }

    public static String getClEndNativeId() {
        return clEndNativeId;
    }

    public static void setClEndNativeId(String clEndNativeId) {
        AdPlacement.clEndNativeId = clEndNativeId;
    }

    public static boolean getClEndBackAdShow() {
        return clEndBackAdShow;
    }

    public static void setClEndBackAdShow(boolean clEndBackAdShow) {
        AdPlacement.clEndBackAdShow = clEndBackAdShow;
    }

    public static String getClEndBackAdType() {
        return clEndBackAdType;
    }

    public static void setClEndBackAdType(String clEndBackAdType) {
        AdPlacement.clEndBackAdType = clEndBackAdType;
    }

    public static int getClEndBackAdShowAfterDay() {
        return clEndBackAdShowAfterDay;
    }

    public static void setClEndBackAdShowAfterDay(int clEndBackAdShowAfterDay) {
        AdPlacement.clEndBackAdShowAfterDay = clEndBackAdShowAfterDay;
    }

    public static int getClEndBackAdShowPerDay() {
        return clEndBackAdShowPerDay;
    }

    public static void setClEndBackAdShowPerDay(int clEndBackAdShowPerDay) {
        AdPlacement.clEndBackAdShowPerDay = clEndBackAdShowPerDay;
    }

    public static String getClEndBackAdInterstitialId() {
        return clEndBackAdInterstitialId;
    }

    public static void setClEndBackAdInterstitialId(String clEndBackAdInterstitialId) {
        AdPlacement.clEndBackAdInterstitialId = clEndBackAdInterstitialId;
    }

    public static boolean getClEndBackAdCountryIP() {
        return clEndBackAdCountryIP;
    }

    public static void setClEndBackAdCountryIP(boolean clEndBackAdCountryIP) {
        AdPlacement.clEndBackAdCountryIP = clEndBackAdCountryIP;
    }

    public static ArrayList<String> getClEndBackAdShowCountryList() {
        return clEndBackAdShowCountryList;
    }

    public static void setClEndBackAdShowCountryList(ArrayList<String> countryList) {
        clEndBackAdShowCountryList = countryList;
    }

    public static int getNotificationInstallDays() {
        return notificationInstallDays;
    }

    public static void setNotificationInstallDays(int notificationInstallDays) {
        AdPlacement.notificationInstallDays = notificationInstallDays;
    }

    public static int getNotificationCallInstallDays() {
        return notificationCallInstallDays;
    }

    public static void setNotificationCallInstallDays(int notificationCallInstallDays) {
        AdPlacement.notificationCallInstallDays = notificationCallInstallDays;
    }

    public static int getNotificationCallOverlayInstallDays() {
        return notificationCallOverlayInstallDays;
    }

    public static void setNotificationCallOverlayInstallDays(int notificationCallOverlayInstallDays) {
        AdPlacement.notificationCallOverlayInstallDays = notificationCallOverlayInstallDays;
    }

    public static boolean getAllAllowPermissionShowNotification() {
        return allAllowPermissionShowNotification;
    }

    public static void setAllAllowPermissionShowNotification(boolean allAllowPermissionShowNotification) {
        AdPlacement.allAllowPermissionShowNotification = allAllowPermissionShowNotification;
    }

    public static boolean getNotificationBackAdShow() {
        return notificationBackAdShow;
    }

    public static void setNotificationBackAdShow(boolean notificationBackAdShow) {
        AdPlacement.notificationBackAdShow = notificationBackAdShow;
    }

    public static boolean getNotificationCloseButtonShow() {
        return notificationCloseButtonShow;
    }

    public static void setNotificationCloseButtonShow(boolean notificationCloseButtonShow) {
        AdPlacement.notificationCloseButtonShow = notificationCloseButtonShow;
    }

    public static ArrayList<String> getNotificationCountryList() {
        return notificationCountryList;
    }

    public static void setNotificationCountryList(ArrayList<String> countryList) {
        notificationCountryList = countryList == null ? new ArrayList<>() : countryList;
    }

    public static ArrayList<String> getNotificationCallCountryList() {
        return notificationCallCountryList;
    }

    public static void setNotificationCallCountryList(ArrayList<String> countryList) {
        notificationCallCountryList = countryList == null ? new ArrayList<>() : countryList;
    }

    public static ArrayList<String> getNotificationCallOverlayCountryList() {
        return notificationCallOverlayCountryList;
    }

    public static void setNotificationCallOverlayCountryList(ArrayList<String> countryList) {
        notificationCallOverlayCountryList = countryList == null ? new ArrayList<>() : countryList;
    }

    public static void loadBannerAd(Activity activity, String bannerId, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd) {
        loadBannerAd(activity, bannerId, rlBannerAdView, slBannerShimmer, llBannerAd, null, null);
    }

    public static boolean canRequestAds(Context context) {
        return context != null && ConsentManager.getInstance(context).canRequestAds();
    }

    public static void gatherConsent(@Nullable Activity activity, @Nullable ConsentManager.OnConsentGatheringCompleteListener onComplete) {
        ConsentManager.getInstance(activity).gatherConsent(activity, onComplete);
    }

    public static void loadBannerAd(Context context, String bannerId, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, Runnable onAdFailed, Consumer<AdView> onAdLoaded) {
        final int loadToken = bannerLoadToken.incrementAndGet();
        Activity activity = resolveActivity(context);

        if (shouldUseQuizPriority() && activity != null) {
            clearBannerContainer(slBannerShimmer, llBannerAd);
            if (showQuizBannerAd(activity, rlBannerAdView, slBannerShimmer, llBannerAd)) {
                if (onAdLoaded != null) {
                    onAdLoaded.accept(null);
                }
            } else {
                hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
                if (onAdFailed != null) {
                    onAdFailed.run();
                }
            }
            return;
        }

        if (!canRequestAds(context) || !isNetworkAvailable(context) || bannerId == null || bannerId.isEmpty()) {
            hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
            if (onAdFailed != null) {
                onAdFailed.run();
            }
            return;
        }

        AdView adView = new AdView(context);
        AdSize adSize = activity != null ? getAdSize(activity) : AdSize.BANNER;
        adView.setAdSize(adSize);
        adView.setAdUnitId(bannerId);

        if (llBannerAd != null) {
            llBannerAd.removeAllViews();
            blockAdFocus(llBannerAd);
            llBannerAd.addView(adView);
            llBannerAd.setVisibility(View.GONE);
        }
        blockAdFocus(rlBannerAdView);
        blockAdFocus(adView);
        if (slBannerShimmer != null) {
            slBannerShimmer.setVisibility(View.VISIBLE);
        }
        if (rlBannerAdView != null) {
            rlBannerAdView.setVisibility(View.VISIBLE);
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                if (!isActiveGoogleBannerCallback(loadToken, adView, llBannerAd)) {
                    adView.destroy();
                    return;
                }
                adView.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));

                if (slBannerShimmer != null) {
                    slBannerShimmer.setVisibility(View.GONE);
                }
                if (llBannerAd != null) {
                    llBannerAd.setVisibility(View.VISIBLE);
                }
                if (rlBannerAdView != null) {
                    rlBannerAdView.setVisibility(View.VISIBLE);
                }
                if (onAdLoaded != null) {
                    onAdLoaded.accept(adView);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                super.onAdFailedToLoad(adError);
                if (!isActiveGoogleBannerCallback(loadToken, adView, llBannerAd)) {
                    adView.destroy();
                    return;
                }
                if (llBannerAd != null) {
                    llBannerAd.removeAllViews();
                }
                adView.destroy();
                if (activity != null) {
                    handleGoogleBannerFailure(activity, rlBannerAdView, slBannerShimmer, llBannerAd, onAdLoaded, onAdFailed);
                } else {
                    hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
                    if (onAdFailed != null) {
                        onAdFailed.run();
                    }
                }
            }
        });
    }

    public static AdSize getAdSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    private static Activity resolveActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    private static Activity resolveActivity(Context context, @Nullable View view) {
        Activity activity = resolveActivity(context);
        if (activity != null || view == null) {
            return activity;
        }
        return resolveActivity(view.getContext());
    }

    public static void loadAdaptiveBannerAd(Activity activity, String bannerId, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd) {
        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || bannerId == null || bannerId.isEmpty()) {
            if (rlBannerAdView != null) {
                rlBannerAdView.setVisibility(View.GONE);
            }
            return;
        }

        AdView adView = new AdView(activity);
        AdSize adSize = getAdaptiveAdSize(activity);
        adView.setAdSize(adSize);
        adView.setAdUnitId(bannerId);
        llBannerAd.removeAllViews();
        blockAdFocus(llBannerAd);
        blockAdFocus(rlBannerAdView);
        blockAdFocus(adView);
        llBannerAd.addView(adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                adView.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));

                slBannerShimmer.setVisibility(View.GONE);
                llBannerAd.setVisibility(View.VISIBLE);
            }

            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                rlBannerAdView.setVisibility(View.GONE);
            }
        });
    }

    public static AdSize getAdaptiveAdSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = outMetrics.density <= 0f ? 1f : outMetrics.density;
        int adWidth = (int) (outMetrics.widthPixels / density);
        if (adWidth <= 0) {
            adWidth = 320;
        }
        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, adWidth);
    }

    @SuppressLint("InflateParams")
    public static void loadNativeAd(Context context, String nativeId, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, String type) {
        loadNativeAd(context, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, type, null, null);
    }

    @SuppressLint("InflateParams")
    public static void loadNativeAd(Context context, String nativeId, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, String type, @Nullable Consumer<NativeAd> onAdLoaded, @Nullable Runnable onAdFailed) {
        final int loadToken = nativeLoadToken.incrementAndGet();
        Activity activity = resolveActivity(context, rlNativeAdView != null ? rlNativeAdView : flNativeAd);

        if (shouldUseQuizPriority() && activity != null) {
            clearNativeContainer(slNativeShimmer, flNativeAd);
            if (showQuizNativeAd(activity, rlNativeAdView, slNativeShimmer, flNativeAd, type)) {
                if (onAdLoaded != null) {
                    onAdLoaded.accept(null);
                }
            } else {
                hideNativeContainer(rlNativeAdView, slNativeShimmer, flNativeAd);
                if (onAdFailed != null) {
                    onAdFailed.run();
                }
            }
            return;
        }

        if (!canRequestAds(context) || !isNetworkAvailable(context) || nativeId == null || nativeId.isEmpty()) {
            hideNativeContainer(rlNativeAdView, slNativeShimmer, flNativeAd);
            if (onAdFailed != null) {
                onAdFailed.run();
            }
            return;
        }

        clearNativeContainer(slNativeShimmer, flNativeAd);
        if (flNativeAd != null) {
            flNativeAd.setTag(loadToken);
        }
        if (slNativeShimmer != null) {
            slNativeShimmer.setVisibility(View.VISIBLE);
        }

        disableViewHierarchySaveState(flNativeAd);
        AdLoader adLoader = new AdLoader.Builder(context, nativeId).forNativeAd(nativeAd -> {
            if (!isActiveGoogleNativeCallback(loadToken, flNativeAd)) {
                nativeAd.destroy();
                return;
            }
            if (context instanceof Activity) {
                Activity nativeActivity = (Activity) context;
                if (nativeActivity.isFinishing() || nativeActivity.isDestroyed()) {
                    nativeAd.destroy();
                    return;
                }
            }

            nativeAd.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));

            LayoutInflater inflater = LayoutInflater.from(context);
            NativeAdView adView = null;
            if (Objects.equals(type, "large")) {
                adView = (NativeAdView) inflater.inflate(R.layout.native_large_ad_layout, null);
            } else if (Objects.equals(type, "full")) {
                adView = (NativeAdView) inflater.inflate(R.layout.native_fullscreen_ad_layout, null);
            } else if (Objects.equals(type, "medium")) {
                adView = (NativeAdView) inflater.inflate(R.layout.native_medium_ad_layout, null);
            } else if (Objects.equals(type, "small")) {
                adView = (NativeAdView) inflater.inflate(R.layout.native_small_ad_layout, null);
            }
            if (adView == null) {
                nativeAd.destroy();
                if (rlNativeAdView != null) {
                    rlNativeAdView.setVisibility(View.GONE);
                }
                if (onAdFailed != null) {
                    onAdFailed.run();
                }
                return;
            }

            populateNativeAdView(nativeAd, adView, type);
            disableViewHierarchySaveState(adView);
            blockAdFocus(flNativeAd);
            blockAdFocus(adView);
            flNativeAd.removeAllViews();
            flNativeAd.addView(adView);

            if (slNativeShimmer != null) {
                slNativeShimmer.setVisibility(View.GONE);
            }
            flNativeAd.setVisibility(View.VISIBLE);
            if (rlNativeAdView != null) {
                rlNativeAdView.setVisibility(View.VISIBLE);
            }
            if (onAdLoaded != null) {
                onAdLoaded.accept(nativeAd);
            }
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                super.onAdFailedToLoad(adError);
                if (!isActiveGoogleNativeCallback(loadToken, flNativeAd)) {
                    return;
                }
                Activity failureActivity = resolveActivity(context, rlNativeAdView != null ? rlNativeAdView : flNativeAd);
                if (failureActivity != null) {
                    handleGoogleNativeFailure(failureActivity, rlNativeAdView, slNativeShimmer, flNativeAd, type, onAdLoaded, onAdFailed);
                } else {
                    hideNativeContainer(rlNativeAdView, slNativeShimmer, flNativeAd);
                    if (onAdFailed != null) {
                        onAdFailed.run();
                    }
                }
            }
        }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private static void disableViewHierarchySaveState(View view) {
        if (view == null) {
            return;
        }
        view.setSaveEnabled(false);
        view.setSaveFromParentEnabled(false);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                disableViewHierarchySaveState(group.getChildAt(i));
            }
        }
    }

    private static void blockAdFocus(@Nullable View view) {
        if (view == null) {
            return;
        }
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
        if (view instanceof ViewGroup) {
            ((ViewGroup) view).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
    }

    public static void populateNativeAdView(NativeAd nativeAd, NativeAdView adView, String type) {
        boolean hasShowView = "large".equalsIgnoreCase(type) || "full".equalsIgnoreCase(type) || "medium".equalsIgnoreCase(type);

        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        if (hasShowView) {
            adView.setMediaView(adView.findViewById(R.id.ad_media));
        }
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));

        NativeAd.Image icon = nativeAd.getIcon();
        if (icon != null) {
            ((AppCompatImageView) Objects.requireNonNull(adView.getIconView())).setImageDrawable(icon.getDrawable());
        }

        ((AppCompatTextView) Objects.requireNonNull(adView.getHeadlineView())).setText(nativeAd.getHeadline());

        if (hasShowView) {
            MediaView mediaView = adView.getMediaView();
            if (mediaView != null) {
                mediaView.setMediaContent(nativeAd.getMediaContent());
            }
        }

        ((AppCompatTextView) Objects.requireNonNull(adView.getBodyView())).setText(nativeAd.getBody());

        AppCompatButton callToActionView = (AppCompatButton) Objects.requireNonNull(adView.getCallToActionView());
        callToActionView.setText(nativeAd.getCallToAction());
        applyBackgroundColor(callToActionView, getNativeAdButtonColor(), R.color.primary);

        View adAttribution = adView.findViewById(R.id.ad_attribution);
        if (adAttribution != null) {
            applyBackgroundColor(adAttribution, getNativeAdLabelColor(), 0);
        }

        adView.setNativeAd(nativeAd);
    }

    private static void applyBackgroundColor(View view, String colorHex, int fallbackColorRes) {
        if (view == null) {
            return;
        }
        Integer color = parseAdColor(colorHex);
        if (color == null && fallbackColorRes != 0) {
            color = ContextCompat.getColor(view.getContext(), fallbackColorRes);
        }
        if (color == null) {
            return;
        }
        if (view.getBackground() != null) {
            DrawableCompat.setTint(DrawableCompat.wrap(view.getBackground().mutate()), color);
        } else {
            view.setBackgroundColor(color);
        }
    }

    private static Integer parseAdColor(String colorHex) {
        if (colorHex == null || colorHex.trim().isEmpty()) {
            return null;
        }
        try {
            String value = colorHex.trim();
            if (!value.startsWith("#")) {
                value = "#" + value;
            }
            return Color.parseColor(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void dismissLoadingDialogSafely(Activity activity, Dialog dialog) {
        try {
            if (dialog != null && dialog.isShowing() && activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                dialog.dismiss();
            }
        } catch (Exception ignored) {
        }
    }

    private static void loadInterstitialAdInternal(Activity activity, String interstitialId, @Nullable OnInterstitialAdListener onInterstitialAdListener, boolean showLoadingDialog) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (onInterstitialAdListener != null) {
                onInterstitialAdListener.onInterstitialAdListener();
            }
            return;
        }

        final int loadToken = interstitialLoadToken.incrementAndGet();
        final AtomicBoolean completed = new AtomicBoolean(false);

        if (shouldUseQuizPriority()) {
            if (!showQuizInterstitialAd(activity, onInterstitialAdListener, completed)) {
                notifyInterstitialComplete(onInterstitialAdListener, completed);
            }
            return;
        }

        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || interstitialId == null || interstitialId.isEmpty()) {
            notifyInterstitialComplete(onInterstitialAdListener, completed);
            return;
        }

        Dialog loadingDialog = showLoadingDialog ? showInterstitialLoadingDialog(activity) : null;
        final Handler loadingDialogHandler = new Handler(Looper.getMainLooper());
        final Runnable loadingDialogTimeoutRunnable = () -> {
            if (!isActiveGoogleInterstitialCallback(loadToken)) {
                return;
            }
            interstitialLoadToken.incrementAndGet();
            dismissInterstitialLoadingDialog(loadingDialog);
            handleGoogleInterstitialFailure(activity, onInterstitialAdListener, completed, null);
        };
        if (loadingDialog != null) {
            loadingDialogHandler.postDelayed(loadingDialogTimeoutRunnable, INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS);
        }

        InterstitialAd.load(activity, interstitialId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                if (!isActiveGoogleInterstitialCallback(loadToken)) {
                    return;
                }
                loadingDialogHandler.removeCallbacks(loadingDialogTimeoutRunnable);
                dismissInterstitialLoadingDialog(loadingDialog);
                if (activity.isFinishing() || activity.isDestroyed()) {
                    notifyInterstitialComplete(onInterstitialAdListener, completed);
                    return;
                }
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));
                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        notifyInterstitialComplete(onInterstitialAdListener, completed);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        handleGoogleInterstitialFailure(activity, onInterstitialAdListener, completed, null);
                    }
                });
                try {
                    ad.show(activity);
                } catch (Exception e) {
                    handleGoogleInterstitialFailure(activity, onInterstitialAdListener, completed, null);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (!isActiveGoogleInterstitialCallback(loadToken)) {
                    return;
                }
                loadingDialogHandler.removeCallbacks(loadingDialogTimeoutRunnable);
                handleGoogleInterstitialFailure(activity, onInterstitialAdListener, completed, loadingDialog);
            }
        });
    }

    private static boolean isActiveGoogleInterstitialCallback(int loadToken) {
        return loadToken == interstitialLoadToken.get() && !shouldUseQuizPriority();
    }

    private static void handleGoogleInterstitialFailure(Activity activity, @Nullable OnInterstitialAdListener onInterstitialAdListener, AtomicBoolean completed, @Nullable Dialog loadingDialog) {
        dismissInterstitialLoadingDialog(loadingDialog);
        if (getGoogleAdFailedShowQuiz() && showQuizInterstitialAd(activity, onInterstitialAdListener, completed)) {
            return;
        }
        notifyInterstitialComplete(onInterstitialAdListener, completed);
    }

    @Nullable
    private static Dialog showInterstitialLoadingDialog(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_loading_ads);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);
        try {
            dialog.show();
            return dialog;
        } catch (Exception e) {
            return null;
        }
    }

    private static void dismissInterstitialLoadingDialog(@Nullable Dialog dialog) {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }
        try {
            dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    public static void loadInterstitialAd(Activity activity, String interstitialId, OnInterstitialAdListener onInterstitialAdListener) {
        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || interstitialId == null || interstitialId.isEmpty()) {
            if (onInterstitialAdListener != null) {
                onInterstitialAdListener.onInterstitialAdListener();
            }
            return;
        }

        int clickThreshold = AdPlacement.getInterstitialClick();
        if (clickThreshold > 0) {
            if (clickThreshold == MyApplication.interstitialCount) {
                MyApplication.interstitialCount = 1;
            } else {
                MyApplication.interstitialCount++;
                if (onInterstitialAdListener != null) {
                    onInterstitialAdListener.onInterstitialAdListener();
                }
                return;
            }
        }

        loadInterstitialAdInternal(activity, interstitialId, onInterstitialAdListener, true);
    }

    public static void loadRightSwipeInterstitialAd(Activity activity, String interstitialId, OnInterstitialAdListener onInterstitialAdListener) {
        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || interstitialId == null || interstitialId.isEmpty()) {
            if (onInterstitialAdListener != null) {
                onInterstitialAdListener.onInterstitialAdListener();
            }
            return;
        }

        int swipeThreshold = AdPlacement.getRightSwipeInterstitial();
        if (swipeThreshold > 0) {
            if (swipeThreshold == MyApplication.rightSwipeInterstitialCount) {
                MyApplication.rightSwipeInterstitialCount = 1;
            } else {
                MyApplication.rightSwipeInterstitialCount++;
                if (onInterstitialAdListener != null) {
                    onInterstitialAdListener.onInterstitialAdListener();
                }
                return;
            }
        }

        loadInterstitialAdInternal(activity, interstitialId, onInterstitialAdListener, true);
    }

    public static void loadInterstitialAdFragmentSwitch(Activity activity, String interstitialId, OnInterstitialAdListener onInterstitialAdListener) {
        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || interstitialId == null || interstitialId.isEmpty()) {
            if (onInterstitialAdListener != null) {
                onInterstitialAdListener.onInterstitialAdListener();
            }
            return;
        }

        int clickThreshold = AdPlacement.getInterstitialAdShowSwitchClick();
        if (clickThreshold > 0) {
            if (clickThreshold == MyApplication.interstitialSwitchCount) {
                MyApplication.interstitialSwitchCount = 1;
            } else {
                MyApplication.interstitialSwitchCount++;
                if (onInterstitialAdListener != null) {
                    onInterstitialAdListener.onInterstitialAdListener();
                }
                return;
            }
        }

        loadInterstitialAdInternal(activity, interstitialId, onInterstitialAdListener, true);
    }

    public static void loadInterstitialAdMessageList(Activity activity, String interstitialId, OnInterstitialAdListener onInterstitialAdListener) {
        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || interstitialId == null || interstitialId.isEmpty()) {
            if (onInterstitialAdListener != null) {
                onInterstitialAdListener.onInterstitialAdListener();
            }
            return;
        }

        int clickThreshold = AdPlacement.getMessageListInterstitialClick();
        if (clickThreshold > 0) {
            if (clickThreshold == MyApplication.interstitialMessageListCount) {
                MyApplication.interstitialMessageListCount = 1;
            } else {
                MyApplication.interstitialMessageListCount++;
                if (onInterstitialAdListener != null) {
                    onInterstitialAdListener.onInterstitialAdListener();
                }
                return;
            }
        }

        loadInterstitialAdInternal(activity, interstitialId, onInterstitialAdListener, true);
    }

    public static void loadInterstitialAdMessageContentBack(Activity activity, String interstitialId, OnInterstitialAdListener onInterstitialAdListener) {
        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || interstitialId == null || interstitialId.isEmpty()) {
            if (onInterstitialAdListener != null) {
                onInterstitialAdListener.onInterstitialAdListener();
            }
            return;
        }

        int clickThreshold = AdPlacement.getMessageContentBackInterstitialClick();
        if (clickThreshold > 0) {
            if (clickThreshold == MyApplication.interstitialMessageContentBackCount) {
                MyApplication.interstitialMessageContentBackCount = 1;
            } else {
                MyApplication.interstitialMessageContentBackCount++;
                if (onInterstitialAdListener != null) {
                    onInterstitialAdListener.onInterstitialAdListener();
                }
                return;
            }
        }

        loadInterstitialAdInternal(activity, interstitialId, onInterstitialAdListener, true);
    }

    public static void loadAfterSplashInterstitialAd(Activity activity, String interstitialId, OnInterstitialAdListener onInterstitialAdListener) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        if (shouldUseQuizPriority()) {
            if (!showQuizInterstitialAd(activity, onInterstitialAdListener, completed)) {
                notifyInterstitialComplete(onInterstitialAdListener, completed);
            }
            return;
        }

        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || interstitialId == null || interstitialId.isEmpty()) {
            if (onInterstitialAdListener != null) {
                onInterstitialAdListener.onInterstitialAdListener();
            }
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, interstitialId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));

                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        onInterstitialAdListener.onInterstitialAdListener();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        completeAfterDefaultAdOrQuizFallback(activity, false, onInterstitialAdListener);
                    }
                });
                ad.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                completeAfterDefaultAdOrQuizFallback(activity, false, onInterstitialAdListener);
            }
        });
    }

    public static void loadAfterSplashAppOpenAd(Activity activity, String appOpenId, OnInterstitialAdListener onCompleteListener) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        if (shouldUseQuizPriority()) {
            if (!showQuizAppOpenAd(activity, onCompleteListener, completed)) {
                notifyInterstitialComplete(onCompleteListener, completed);
            }
            return;
        }

        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || appOpenId == null || appOpenId.isEmpty()) {
            if (onCompleteListener != null) {
                onCompleteListener.onInterstitialAdListener();
            }
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        AppOpenAd.load(activity, appOpenId, adRequest, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));

                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        onCompleteListener.onInterstitialAdListener();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        completeAfterDefaultAdOrQuizFallback(activity, true, onCompleteListener);
                    }
                });
                try {
                    ad.show(activity);
                } catch (Exception e) {
                    completeAfterDefaultAdOrQuizFallback(activity, true, onCompleteListener);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                completeAfterDefaultAdOrQuizFallback(activity, true, onCompleteListener);
            }
        });
    }

    public static long getLauncherAppLastShowTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("launcher_app_native_ad_preferences", MODE_PRIVATE);
        return sharedPreferences.getLong("last_show_time", 0);
    }

    public static void setLauncherAppLastShowTime(Context context, long time) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("launcher_app_native_ad_preferences", MODE_PRIVATE);
        sharedPreferences.edit().putLong("last_show_time", time).apply();
    }

    public static boolean canShowLauncherNativeAd(Context context) {
        int countPerDay = getLauncherAppNativeAdShowPerDay();
        if (countPerDay <= 0) {
            return true;
        }

        long intervalMillis = (24L * 60L * 60L * 1000L) / countPerDay;
        long lastShowTime = getLauncherAppLastShowTime(context);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastShowTime) >= intervalMillis;
    }

    public static boolean canShowLauncherAppBannerAd(Context context) {
        int countPerDay = getLauncherAppBannerAdShowPerDay();
        if (countPerDay <= 0) {
            return true;
        }

        long intervalMillis = (24L * 60L * 60L * 1000L) / countPerDay;
        long lastShowTime = getLauncherAppBannerLastShowTime(context);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastShowTime) >= intervalMillis;
    }

    public static long getLauncherAppBannerLastShowTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("launcher_app_banner_ad_preferences", MODE_PRIVATE);
        return sharedPreferences.getLong("last_show_time", 0);
    }

    public static void setLauncherAppBannerLastShowTime(Context context, long time) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("launcher_app_banner_ad_preferences", MODE_PRIVATE);
        sharedPreferences.edit().putLong("last_show_time", time).apply();
    }

    public static long getAppOpenLastShowTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("app_open_ad_preferences", MODE_PRIVATE);
        return sharedPreferences.getLong("last_show_time", 0);
    }

    public static void setAppOpenLastShowTime(Context context, long time) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("app_open_ad_preferences", MODE_PRIVATE);
        sharedPreferences.edit().putLong("last_show_time", time).apply();
    }

    public static boolean canShowAppOpenAd(Context context) {
        int countPerDay = AdPlacement.getAppOpenShowPerDay();
        if (countPerDay <= 0) {
            return true;
        }

        long intervalMillis = (24L * 60L * 60L * 1000L) / countPerDay;
        long lastShowTime = getAppOpenLastShowTime(context);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastShowTime) >= intervalMillis;
    }

    public static boolean showClEndBackAd(Context context) {
        if (!getClEndBackAdShow()) {
            return false;
        }

        int dayCount = getClEndBackAdShowAfterDay();
        int activeShowCount = getClEndBackAdShowPerDay();

        if (dayCount <= 0 || activeShowCount <= 0) {
            return false;
        }

        if (getDaysSinceInstall(context) < dayCount) {
            return false;
        }

        if (!isCountryAllowedForCallEnd(context)) {
            return false;
        }

        long intervalMillis = (24L * 60L * 60L * 1000L) / activeShowCount;
        long lastShowTime = getClEndLastShowTime(context);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastShowTime) >= intervalMillis;
    }

    public static long getDaysSinceInstall(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appInstallDate", MODE_PRIVATE);
        String installDate = sharedPreferences.getString("install_date", "");
        if (installDate.isEmpty()) {
            return 1;
        }

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date1 = simpleDateFormat.parse(installDate);
            Date date2 = new Date();
            long diff = date2.getTime() - date1.getTime();
            return (diff / (24 * 60 * 60 * 1000)) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    public static boolean isCountryAllowedForCallEnd(Context context) {
        return isCountryAllowed(getDeviceCountry(context), getClEndBackAdShowCountryList());
    }

    private static boolean isCountryAllowed(String currentCountry, List<String> allowedCountries) {
        if (allowedCountries == null || allowedCountries.isEmpty()) {
            return true;
        }
        if (currentCountry == null || currentCountry.isEmpty()) {
            return false;
        }
        for (String country : allowedCountries) {
            if (country != null && country.equalsIgnoreCase(currentCountry)) {
                return true;
            }
        }
        return false;
    }

    public static long getClEndLastShowTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cl_end_ad_preferences", MODE_PRIVATE);
        return sharedPreferences.getLong("last_show_time", 0);
    }

    public static void setClEndLastShowTime(Context context, long time) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cl_end_ad_preferences", MODE_PRIVATE);
        sharedPreferences.edit().putLong("last_show_time", time).apply();
    }

    public static String getDeviceCountry(Context context) {
        if (getClEndBackAdCountryIP() && !getIpCountryName(context).isEmpty()) {
            return getIpCountryName(context).toUpperCase();
        }
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String countryCode = telephonyManager.getNetworkCountryIso();
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = telephonyManager.getSimCountryIso();
            }
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = context.getResources().getConfiguration().locale.getCountry();
            }
            if (!countryCode.isEmpty()) {
                Locale locale = new Locale("", countryCode);
                return locale.getDisplayCountry(Locale.ENGLISH).toUpperCase();
            }
            return "";
        } catch (Exception e1) {
            try {
                Locale locale = context.getResources().getConfiguration().locale;
                return locale.getDisplayCountry(Locale.ENGLISH).toUpperCase();
            } catch (Exception e2) {
                return "";
            }
        }
    }

    public static String getIpCountryName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("ipCountryName", MODE_PRIVATE);
        return sharedPreferences.getString(ipCountryName, "");
    }

    public static void setIpCountryName(Context context, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("ipCountryName", MODE_PRIVATE);
        sharedPreferences.edit().putString(ipCountryName, value).apply();
    }

    private static int getQuizSyncedItemCount() {
        return getQuizItemCount(quizAppIconList, quizBannerTitleList, quizBannerDescriptionList, quizNativeTitleList, quizNativeDescriptionList, quizInterstitialTitleList, quizInterstitialDescriptionList);
    }

    private static int pickQuizSyncedIndex(int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }
        return new Random().nextInt(itemCount);
    }

    private static String pickRandomQuizLink() {
        List<String> links = !activeQuizLinkList.isEmpty() ? activeQuizLinkList : quizLinkList;
        if (links.isEmpty()) {
            return "";
        }
        return links.get(new Random().nextInt(links.size()));
    }

    private static int getQuizBannerItemCount() {
        return getQuizSyncedItemCount();
    }

    public static int getQuizNativeItemCount() {
        return getQuizSyncedItemCount();
    }

    public static boolean isQuizNativeAvailable() {
        return getQuizNativeItemCount() > 0;
    }

    private static int getQuizInterstitialItemCount() {
        return getQuizSyncedItemCount();
    }

    private static int getQuizAppOpenItemCount() {
        return getQuizSyncedItemCount();
    }

    private static String getQuizButtonText() {
        return quizButtonText == null ? "" : quizButtonText.trim();
    }

    private static void applyQuizButtonText(@Nullable AppCompatTextView button) {
        if (button == null) {
            return;
        }
        String buttonText = getQuizButtonText();
        if (!buttonText.isEmpty()) {
            button.setText(buttonText);
        }
    }

    private static void applyQuizAdColors(@Nullable View view) {
        if (view == null) {
            return;
        }
        View labelView = view.findViewById(R.id.tvQZLabel);
        if (labelView != null) {
            applyBackgroundColor(labelView, getNativeAdLabelColor(), 0);
        }
        View buttonView = view.findViewById(R.id.btnQZClick);
        if (buttonView != null) {
            applyBackgroundColor(buttonView, getNativeAdButtonColor(), R.color.primary);
        }
    }

    @SafeVarargs
    private static int getQuizItemCount(List<String>... requiredLists) {
        int count = Integer.MAX_VALUE;
        for (List<String> list : requiredLists) {
            if (list == null || list.isEmpty()) {
                return 0;
            }
            count = Math.min(count, list.size());
        }
        return count == Integer.MAX_VALUE ? 0 : Math.min(3, count);
    }

    private static String getQuizListItem(@Nullable List<String> list, int index) {
        if (list == null || list.isEmpty() || index < 0 || index >= list.size()) {
            return "";
        }
        return list.get(index);
    }

    private static void loadQuizIconImage(@Nullable View root, @Nullable AppCompatImageView imageView, int index) {
        loadQuizShimmerImage(root, imageView, quizAppIconList, index, R.id.qzShimmerIcon);
    }

    private static void loadQuizMediaImage(@Nullable View root, @Nullable AppCompatImageView imageView, @Nullable List<String> urls, int index) {
        loadQuizShimmerImage(root, imageView, urls, index, R.id.qzShimmer);
    }

    private static void loadQuizShimmerImage(@Nullable View root, @Nullable AppCompatImageView imageView, @Nullable List<String> urls, int index, int shimmerId) {
        if (imageView == null) {
            return;
        }
        ShimmerFrameLayout shimmer = root == null ? null : root.findViewById(shimmerId);
        String imageUrl = getQuizListItem(urls, index).trim();
        if (imageUrl.isEmpty()) {
            stopQuizShimmer(shimmer);
            imageView.setImageResource(QUIZ_IMAGE_FALLBACK);
            imageView.setVisibility(View.VISIBLE);
            return;
        }
        startQuizShimmer(shimmer, imageView);
        imageView.setTag(imageUrl);
        QUIZ_IMAGE_EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadQuizBitmap(imageUrl);
            new Handler(Looper.getMainLooper()).post(() -> {
                Object tag = imageView.getTag();
                if (!(tag instanceof String) || !imageUrl.equals(tag)) {
                    return;
                }
                if (bitmap != null) {
                    finishQuizShimmerLoadSuccess(shimmer, imageView, bitmap);
                } else {
                    finishQuizShimmerLoadFailure(shimmer, imageView);
                }
            });
        });
    }

    private static void startQuizShimmer(@Nullable ShimmerFrameLayout shimmer, @Nullable AppCompatImageView imageView) {
        if (imageView != null) {
            imageView.animate().cancel();
            imageView.setAlpha(1f);
            imageView.setScaleX(1f);
            imageView.setScaleY(1f);
            imageView.setVisibility(View.GONE);
        }
        if (shimmer == null) {
            return;
        }
        shimmer.setVisibility(View.VISIBLE);
        shimmer.startShimmer();
    }

    private static void stopQuizShimmer(@Nullable ShimmerFrameLayout shimmer) {
        if (shimmer == null) {
            return;
        }
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
    }

    private static void revealQuizLoadedImage(@NonNull AppCompatImageView imageView, @NonNull Runnable applyImage) {
        imageView.animate().cancel();
        applyImage.run();
        imageView.setAlpha(0f);
        imageView.setScaleX(QUIZ_IMAGE_REVEAL_START_SCALE);
        imageView.setScaleY(QUIZ_IMAGE_REVEAL_START_SCALE);
        imageView.setVisibility(View.VISIBLE);
        imageView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(QUIZ_IMAGE_REVEAL_DURATION_MS).start();
    }

    private static void finishQuizShimmerLoadSuccess(@Nullable ShimmerFrameLayout shimmer, @NonNull AppCompatImageView imageView, @NonNull Bitmap bitmap) {
        stopQuizShimmer(shimmer);
        revealQuizLoadedImage(imageView, () -> imageView.setImageBitmap(bitmap));
    }

    private static void finishQuizShimmerLoadFailure(@Nullable ShimmerFrameLayout shimmer, @NonNull AppCompatImageView imageView) {
        stopQuizShimmer(shimmer);
        imageView.setImageResource(QUIZ_IMAGE_FALLBACK);
        imageView.setVisibility(View.VISIBLE);
    }

    @Nullable
    private static Bitmap downloadQuizBitmap(@NonNull String imageUrl) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setDoInput(true);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (InputStream inputStream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static float resolveQuizInterstitialRating(int index) {
        if (index >= 0 && index < quizInterstitialRateList.size()) {
            return clampQuizRating(parseQuizRatingValue(quizInterstitialRateList.get(index)));
        }
        if (index >= 0 && index < QUIZ_INTERSTITIAL_RATES.length) {
            return clampQuizRating(parseQuizRatingValue(QUIZ_INTERSTITIAL_RATES[index]));
        }
        return 4.5f;
    }

    private static float parseQuizRatingValue(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return 4.5f;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (Exception ignored) {
            return 4.5f;
        }
    }

    private static float clampQuizRating(float rating) {
        return Math.max(0f, Math.min(5f, rating));
    }

    private static String formatQuizInterstitialRating(float rating) {
        float clampedRating = clampQuizRating(rating);
        if (Math.abs(clampedRating - Math.round(clampedRating)) < 0.05f) {
            return String.format(Locale.US, "%.1f", (float) Math.round(clampedRating));
        }
        return String.format(Locale.US, "%.1f", clampedRating);
    }

    private static void setQuizInterstitialCloseButtonState(AppCompatTextView btnQZClose, boolean enabled) {
        if (btnQZClose == null) {
            return;
        }
        Context context = btnQZClose.getContext();
        if (enabled) {
            btnQZClose.setEnabled(true);
            btnQZClose.setClickable(true);
            applyQuizCloseButtonBorderColor(btnQZClose, getNativeAdButtonColor(), R.color.primary);
            Integer buttonColor = parseAdColor(getNativeAdButtonColor());
            if (buttonColor == null) {
                buttonColor = ContextCompat.getColor(context, R.color.primary);
            }
            btnQZClose.setTextColor(buttonColor);
            btnQZClose.animate().alpha(1f).setDuration(200L).start();
            return;
        }
        btnQZClose.setEnabled(false);
        btnQZClose.setClickable(false);
        btnQZClose.setAlpha(1f);
        applyQuizCloseButtonBorderColor(btnQZClose, "", R.color.gray);
        btnQZClose.setTextColor(ContextCompat.getColor(context, R.color.gray));
    }

    private static void applyQuizCloseButtonBorderColor(AppCompatTextView button, String colorHex, int fallbackColorRes) {
        if (button == null) {
            return;
        }
        Context context = button.getContext();
        Integer color = parseAdColor(colorHex);
        if (color == null && fallbackColorRes != 0) {
            color = ContextCompat.getColor(context, fallbackColorRes);
        }
        if (color == null) {
            return;
        }
        Drawable background = ContextCompat.getDrawable(context, R.drawable.custom_button_round_border);
        if (background == null) {
            return;
        }
        background = background.mutate();
        ViewCompat.setBackgroundTintList(button, null);
        if (background instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) background;
            int strokeWidth = context.getResources().getDimensionPixelSize(R.dimen.quiz_color_border_stroke);
            drawable.setStroke(strokeWidth, color);
            drawable.setColor(Color.TRANSPARENT);
            button.setBackground(drawable);
            return;
        }
        button.setBackground(background);
    }

    private static void bindQuizInterstitialRating(AppCompatTextView tvQZRate, AppCompatRatingBar rbQZRating, int index) {
        float rating = resolveQuizInterstitialRating(index);
        tvQZRate.setText(formatQuizInterstitialRating(rating));
        rbQZRating.setMax(5);
        rbQZRating.setNumStars(5);
        rbQZRating.setStepSize(0.1f);
        rbQZRating.setIsIndicator(true);
        Drawable progressDrawable = rbQZRating.getProgressDrawable();
        if (progressDrawable != null) {
            progressDrawable = progressDrawable.mutate();
            DrawableCompat.setTintList(progressDrawable, null);
            rbQZRating.setProgressDrawable(progressDrawable);
        }
        rbQZRating.setRating(rating);
    }

    private static void openQuizLink(Activity activity, String link) {
        if (activity == null || link == null || link.trim().isEmpty()) {
            return;
        }
        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(activity.getColor(R.color.primary));
            CustomTabsIntent tabsIntent = builder.build();
            tabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            tabsIntent.launchUrl(activity, Uri.parse(link.trim()));
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.trim()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

    private static void clearBannerContainer(@Nullable ShimmerFrameLayout slBannerShimmer, @Nullable LinearLayout llBannerAd) {
        if (slBannerShimmer != null) {
            slBannerShimmer.setVisibility(View.GONE);
        }
        if (llBannerAd != null) {
            llBannerAd.removeAllViews();
            llBannerAd.setVisibility(View.GONE);
        }
    }

    private static void hideBannerContainer(@Nullable RelativeLayout rlBannerAdView, @Nullable ShimmerFrameLayout slBannerShimmer, @Nullable LinearLayout llBannerAd) {
        clearBannerContainer(slBannerShimmer, llBannerAd);
        if (rlBannerAdView != null) {
            rlBannerAdView.setVisibility(View.GONE);
        }
    }

    private static boolean isActiveGoogleBannerCallback(int loadToken, @Nullable AdView adView, @Nullable LinearLayout llBannerAd) {
        if (shouldUseQuizPriority()) {
            return false;
        }
        return adView != null && llBannerAd != null && adView.getParent() == llBannerAd;
    }

    private static void handleGoogleBannerFailure(Activity activity, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, @Nullable Consumer<AdView> onAdLoaded, @Nullable Runnable onAdFailed) {
        if (getGoogleAdFailedShowQuiz() && showQuizBannerAd(activity, rlBannerAdView, slBannerShimmer, llBannerAd)) {
            if (onAdLoaded != null) {
                onAdLoaded.accept(null);
            }
            return;
        }
        hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
        if (onAdFailed != null) {
            onAdFailed.run();
        }
    }

    private static boolean showQuizBannerAd(Activity activity, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd) {
        try {
            int itemCount = getQuizBannerItemCount();
            if (itemCount <= 0 || activity == null || rlBannerAdView == null || llBannerAd == null) {
                return false;
            }

            if (slBannerShimmer != null) {
                slBannerShimmer.setVisibility(View.GONE);
            }
            llBannerAd.removeAllViews();

            View view = LayoutInflater.from(activity).inflate(R.layout.qz_banner_ad, llBannerAd, false);

            AppCompatImageView ivQZAppIcon = view.findViewById(R.id.ivQZAppIcon);
            AppCompatTextView tvQZAppTitle = view.findViewById(R.id.tvQZAppTitle);
            AppCompatTextView tvQZAppDescription = view.findViewById(R.id.tvQZAppDescription);
            AppCompatTextView btnQZClick = view.findViewById(R.id.btnQZClick);

            int index = pickQuizSyncedIndex(itemCount);
            List<String> titles = getQuizBannerTitleList();
            List<String> descriptions = getQuizBannerDescriptionList();
            loadQuizIconImage(view, ivQZAppIcon, index);
            tvQZAppTitle.setText(titles.get(index));
            tvQZAppDescription.setText(descriptions.get(index));
            applyQuizButtonText(btnQZClick);
            applyQuizAdColors(view);

            view.setOnClickListener(v -> openQuizLink(activity, pickRandomQuizLink()));
            btnQZClick.setOnClickListener(v -> openQuizLink(activity, pickRandomQuizLink()));

            llBannerAd.addView(view);
            llBannerAd.setVisibility(View.VISIBLE);
            rlBannerAdView.setVisibility(View.VISIBLE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean showQuizAdaptiveBannerAd(Activity activity, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd) {
        try {
            int itemCount = getQuizNativeItemCount();
            if (itemCount <= 0 || activity == null || rlBannerAdView == null || llBannerAd == null) {
                return false;
            }

            if (slBannerShimmer != null) {
                slBannerShimmer.setVisibility(View.GONE);
            }
            llBannerAd.removeAllViews();

            View view = LayoutInflater.from(activity).inflate(resolveQuizNativeLayout("large"), llBannerAd, false);
            int index = pickQuizSyncedIndex(itemCount);
            if (!bindQuizNativeContent(activity, view, index, "large")) {
                return false;
            }

            llBannerAd.addView(view);
            llBannerAd.setVisibility(View.VISIBLE);
            rlBannerAdView.setVisibility(View.VISIBLE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void clearNativeContainer(@Nullable ShimmerFrameLayout slNativeShimmer, @Nullable FrameLayout flNativeAd) {
        if (slNativeShimmer != null) {
            slNativeShimmer.setVisibility(View.GONE);
        }
        if (flNativeAd != null) {
            flNativeAd.removeAllViews();
            flNativeAd.setVisibility(View.GONE);
        }
    }

    private static void hideNativeContainer(@Nullable RelativeLayout rlNativeAdView, @Nullable ShimmerFrameLayout slNativeShimmer, @Nullable FrameLayout flNativeAd) {
        clearNativeContainer(slNativeShimmer, flNativeAd);
        if (rlNativeAdView != null) {
            rlNativeAdView.setVisibility(View.GONE);
        }
    }

    private static boolean isActiveGoogleNativeCallback(int loadToken, @Nullable FrameLayout flNativeAd) {
        if (shouldUseQuizPriority()) {
            return false;
        }
        if (flNativeAd == null) {
            return loadToken == nativeLoadToken.get();
        }
        Object tag = flNativeAd.getTag();
        return tag instanceof Integer && (Integer) tag == loadToken;
    }

    private static boolean showQuizNativeAd(Activity activity, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, @Nullable String type) {
        try {
            int itemCount = getQuizNativeItemCount();
            if (itemCount <= 0 || activity == null || rlNativeAdView == null || flNativeAd == null) {
                return false;
            }

            if (slNativeShimmer != null) {
                slNativeShimmer.setVisibility(View.GONE);
            }
            flNativeAd.removeAllViews();

            View view = LayoutInflater.from(activity).inflate(resolveQuizNativeLayout(type), flNativeAd, false);
            int index = pickQuizSyncedIndex(itemCount);
            if (!bindQuizNativeContent(activity, view, index, type)) {
                return false;
            }

            flNativeAd.addView(view);
            flNativeAd.setVisibility(View.VISIBLE);
            rlNativeAdView.setVisibility(View.VISIBLE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static int resolveQuizNativeLayout(@Nullable String type) {
        if ("large".equalsIgnoreCase(type)) {
            return R.layout.qz_native_large_ad;
        }
        if ("medium".equalsIgnoreCase(type)) {
            return R.layout.qz_native_medium_ad;
        }
        return R.layout.qz_native_small_ad;
    }

    private static boolean bindQuizNativeContent(Activity activity, View view, int index, @Nullable String type) {
        AppCompatImageView ivQZAppIcon = view.findViewById(R.id.ivQZAppIcon);
        AppCompatTextView tvQZAppTitle = view.findViewById(R.id.tvQZAppTitle);
        AppCompatTextView tvQZAppDescription = view.findViewById(R.id.tvQZAppDescription);
        AppCompatTextView btnQZClick = view.findViewById(R.id.btnQZClick);
        if (ivQZAppIcon == null || tvQZAppTitle == null || tvQZAppDescription == null || btnQZClick == null) {
            return false;
        }

        List<String> titles = getQuizNativeTitleList();
        List<String> descriptions = getQuizNativeDescriptionList();
        if (index < 0 || index >= getQuizSyncedItemCount()) {
            return false;
        }

        loadQuizIconImage(view, ivQZAppIcon, index);
        tvQZAppTitle.setText(titles.get(index));
        tvQZAppDescription.setText(descriptions.get(index));
        applyQuizButtonText(btnQZClick);

        view.setOnClickListener(v -> openQuizLink(activity, pickRandomQuizLink()));
        btnQZClick.setOnClickListener(v -> openQuizLink(activity, pickRandomQuizLink()));

        View mediaView = view.findViewById(R.id.ivQZAppMedia);
        if (mediaView instanceof AppCompatImageView) {
            if ("large".equalsIgnoreCase(type) || "medium".equalsIgnoreCase(type) || "full".equalsIgnoreCase(type)) {
                AppCompatImageView ivQZAppMedia = (AppCompatImageView) mediaView;
                loadQuizMediaImage(view, ivQZAppMedia, quizNativeMediaList, index);
            } else {
                mediaView.setVisibility(View.GONE);
            }
        }

        applyQuizAdColors(view);
        return true;
    }

    private static void handleGoogleNativeFailure(Activity activity, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, @Nullable String type, @Nullable Consumer<NativeAd> onAdLoaded, @Nullable Runnable onAdFailed) {
        if (getGoogleAdFailedShowQuiz() && showQuizNativeAd(activity, rlNativeAdView, slNativeShimmer, flNativeAd, type)) {
            if (onAdLoaded != null) {
                onAdLoaded.accept(null);
            }
            return;
        }
        hideNativeContainer(rlNativeAdView, slNativeShimmer, flNativeAd);
        if (onAdFailed != null) {
            onAdFailed.run();
        }
    }

    private static void notifyInterstitialComplete(@Nullable OnInterstitialAdListener onInterstitialAdListener, AtomicBoolean completed) {
        if (onInterstitialAdListener == null || !completed.compareAndSet(false, true)) {
            return;
        }
        onInterstitialAdListener.onInterstitialAdListener();
    }

    @SuppressLint("InflateParams")
    private static boolean showQuizInterstitialAd(Activity activity, @Nullable OnInterstitialAdListener onInterstitialAdListener, AtomicBoolean completed) {
        try {
            int itemCount = getQuizInterstitialItemCount();
            if (itemCount <= 0 || activity == null || activity.isFinishing()) {
                return false;
            }

            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            View contentView = LayoutInflater.from(activity).inflate(R.layout.qz_interstitial_ad, null);
            dialog.setContentView(contentView);
            dialog.setCancelable(false);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            AppCompatTextView tvQZTimer = contentView.findViewById(R.id.tvQZTimer);
            AppCompatImageView ivQZClose = contentView.findViewById(R.id.ivQZClose);
            AppCompatImageView ivQZAppMedia = contentView.findViewById(R.id.ivQZAppMedia);
            AppCompatImageView ivQZAppIcon = contentView.findViewById(R.id.ivQZAppIcon);
            AppCompatTextView tvQZAppTitle = contentView.findViewById(R.id.tvQZAppTitle);
            AppCompatTextView tvQZAppDescription = contentView.findViewById(R.id.tvQZAppDescription);
            AppCompatTextView tvQZRate = contentView.findViewById(R.id.tvQZRate);
            AppCompatRatingBar rbQZRating = contentView.findViewById(R.id.rbQZRating);
            AppCompatTextView tvQZUser = contentView.findViewById(R.id.tvQZUser);
            AppCompatTextView btnQZClose = contentView.findViewById(R.id.btnQZClose);
            AppCompatTextView btnQZClick = contentView.findViewById(R.id.btnQZClick);
            if (ivQZAppMedia == null || ivQZAppIcon == null || tvQZAppTitle == null || tvQZAppDescription == null || tvQZRate == null || rbQZRating == null || tvQZUser == null || tvQZTimer == null || ivQZClose == null || btnQZClose == null || btnQZClick == null) {
                return false;
            }

            int index = pickQuizSyncedIndex(itemCount);
            loadQuizMediaImage(contentView, ivQZAppMedia, quizInterstitialMediaList, index);
            loadQuizIconImage(contentView, ivQZAppIcon, index);
            tvQZAppTitle.setText(quizInterstitialTitleList.get(index));
            tvQZAppDescription.setText(quizInterstitialDescriptionList.get(index));
            bindQuizInterstitialRating(tvQZRate, rbQZRating, index);
            tvQZUser.setText(QUIZ_INTERSTITIAL_USERS[index % QUIZ_INTERSTITIAL_USERS.length]);
            applyQuizButtonText(btnQZClick);
            applyQuizAdColors(contentView);

            ivQZClose.setVisibility(View.GONE);
            setQuizInterstitialCloseButtonState(btnQZClose, false);

            Runnable dismissAndComplete = () -> {
                if (dialog.isShowing()) {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {
                    }
                }
                notifyInterstitialComplete(onInterstitialAdListener, completed);
            };

            ivQZClose.setOnClickListener(v -> dismissAndComplete.run());
            contentView.setOnClickListener(v -> openQuizLink(activity, pickRandomQuizLink()));
            btnQZClose.setOnClickListener(v -> {
                if (!btnQZClose.isEnabled()) {
                    return;
                }
                dismissAndComplete.run();
            });
            btnQZClick.setOnClickListener(v -> openQuizLink(activity, pickRandomQuizLink()));

            new CountDownTimer(QUIZ_INTERSTITIAL_CLOSE_DELAY_MS, 1000L) {
                @Override
                public void onTick(long millisUntilFinished) {
                    tvQZTimer.setText(String.valueOf((millisUntilFinished / 1000L) + 1L));
                }

                @Override
                public void onFinish() {
                    tvQZTimer.setVisibility(View.GONE);
                    ivQZClose.setVisibility(View.VISIBLE);
                    setQuizInterstitialCloseButtonState(btnQZClose, true);
                }
            }.start();

            dialog.show();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("InflateParams")
    private static boolean showQuizAppOpenAd(Activity activity, @Nullable OnInterstitialAdListener onCompleteListener, AtomicBoolean completed) {
        try {
            int itemCount = getQuizAppOpenItemCount();
            if (itemCount <= 0 || activity == null || activity.isFinishing()) {
                return false;
            }

            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            View contentView = LayoutInflater.from(activity).inflate(R.layout.qz_app_open_ad, null);
            dialog.setContentView(contentView);
            dialog.setCancelable(false);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            View llQZClose = contentView.findViewById(R.id.llQZClose);
            AppCompatImageView ivQZAppIcon = contentView.findViewById(R.id.ivQZAppIcon);
            AppCompatImageView ivQZAppMedia = contentView.findViewById(R.id.ivQZAppMedia);
            AppCompatTextView btnQZClick = contentView.findViewById(R.id.btnQZClick);
            if (llQZClose == null || ivQZAppIcon == null || ivQZAppMedia == null || btnQZClick == null) {
                return false;
            }

            int index = pickQuizSyncedIndex(itemCount);
            loadQuizIconImage(contentView, ivQZAppIcon, index);
            loadQuizMediaImage(contentView, ivQZAppMedia, quizAppOpenMediaList, index);
            applyQuizButtonText(btnQZClick);
            applyQuizAdColors(contentView);

            Runnable dismissAndComplete = () -> {
                if (dialog.isShowing()) {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {
                    }
                }
                notifyInterstitialComplete(onCompleteListener, completed);
            };

            llQZClose.setOnClickListener(v -> dismissAndComplete.run());
            ivQZAppMedia.setOnClickListener(v -> openQuizLink(activity, pickRandomQuizLink()));
            btnQZClick.setOnClickListener(v -> openQuizLink(activity, pickRandomQuizLink()));

            dialog.show();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void handleLauncherAppClickAd(@Nullable Activity activity, @Nullable Runnable openSelectedApp) {
        if (openSelectedApp == null) {
            return;
        }
        if (activity == null || activity.isFinishing()) {
            runLauncherAppClickCompleteOnce(openSelectedApp, new AtomicBoolean(false));
            return;
        }
        if (!getLauncherAppClickAdShow() || getLauncherAppCount() <= 0) {
            runLauncherAppClickCompleteOnce(openSelectedApp, new AtomicBoolean(false));
            return;
        }
        if (shouldShowLauncherAppAd()) {
            resetLauncherAppClickCount();
            executeLauncherAppClickAd(activity, openSelectedApp);
            return;
        }
        trackLauncherAppClickCount();
        runLauncherAppClickCompleteOnce(openSelectedApp, new AtomicBoolean(false));
    }

    public static boolean shouldShowLauncherAppAd() {
        int threshold = getLauncherAppCount();
        return threshold > 0 && threshold == launcherAppClickCount;
    }

    public static void resetLauncherAppClickCount() {
        launcherAppClickCount = 1;
    }

    private static void trackLauncherAppClickCount() {
        launcherAppClickCount++;
    }

    private static void executeLauncherAppClickAd(Activity activity, Runnable openSelectedApp) {
        switch (normalizeLauncherAppAdType(getLauncherAppAdType())) {
            case LAUNCHER_APP_AD_TYPE_GOOGLE_INTER:
                showLauncherAppGoogleInterstitial(activity, openSelectedApp);
                break;
            case LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN:
                showLauncherAppGoogleAppOpen(activity, openSelectedApp);
                break;
            case LAUNCHER_APP_AD_TYPE_GOOGLE_NATIVE:
                showLauncherAppGoogleNative(activity, openSelectedApp);
                break;
            case LAUNCHER_APP_AD_TYPE_QUIZ_INTER:
                showLauncherAppQuizInterstitial(activity, openSelectedApp);
                break;
            case LAUNCHER_APP_AD_TYPE_QUIZ_APP_OPEN:
                showLauncherAppQuizAppOpen(activity, openSelectedApp);
                break;
            case LAUNCHER_APP_AD_TYPE_QUIZ_NATIVE:
                showLauncherAppQuizNative(activity, openSelectedApp);
                break;
            case LAUNCHER_APP_AD_TYPE_QUIZ_BROWSER:
                openLauncherAppQuizBrowser(activity, openSelectedApp);
                break;
            default:
                runLauncherAppClickCompleteOnce(openSelectedApp, new AtomicBoolean(false));
                break;
        }
    }

    private static String normalizeLauncherAppAdType(@Nullable String adType) {
        if (adType == null) {
            return "";
        }
        String value = adType.trim();
        if (value.isEmpty()) {
            return "";
        }
        if ("Google_Inter".equalsIgnoreCase(value) || "google_inter".equalsIgnoreCase(value) || "inter".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_GOOGLE_INTER;
        }
        if ("Google_App_Open".equalsIgnoreCase(value) || "google_app_open".equalsIgnoreCase(value) || "app_open".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN;
        }
        if ("Google_Native".equalsIgnoreCase(value) || "google_native".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_GOOGLE_NATIVE;
        }
        if ("Quiz_Inter".equalsIgnoreCase(value) || "quiz_inter".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_INTER;
        }
        if ("Quiz_App_Open".equalsIgnoreCase(value) || "quiz_app_open".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_APP_OPEN;
        }
        if ("Quiz_Native".equalsIgnoreCase(value) || "quiz_native".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_NATIVE;
        }
        if ("Quiz_Browser".equalsIgnoreCase(value) || "quiz_browser".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_BROWSER;
        }
        return value.toLowerCase(Locale.US);
    }

    private static void runLauncherAppClickCompleteOnce(@Nullable Runnable openSelectedApp, AtomicBoolean completed) {
        if (openSelectedApp == null || !completed.compareAndSet(false, true)) {
            return;
        }
        openSelectedApp.run();
    }

    private static boolean isValidLauncherAdActivity(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        return !activity.isDestroyed();
    }

    private static void showLauncherAppGoogleInterstitial(Activity activity, Runnable openSelectedApp) {
        final int loadToken = launcherAppClickAdToken.incrementAndGet();
        String interstitialId = getLauncherAppInterstitialId();
        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || interstitialId == null || interstitialId.isEmpty()) {
            runLauncherAppClickCompleteOnce(openSelectedApp, new AtomicBoolean(false));
            return;
        }

        final LauncherGoogleAdLoadState loadState = new LauncherGoogleAdLoadState(loadToken, openSelectedApp, showInterstitialLoadingDialog(activity));
        loadState.scheduleTimeout();

        InterstitialAd.load(activity, interstitialId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                if (!loadState.settleLoadAttempt()) {
                    return;
                }
                loadState.cancelTimeout();
                dismissInterstitialLoadingDialog(loadState.loadingDialog);
                if (loadToken != launcherAppClickAdToken.get()) {
                    loadState.completeOnce().run();
                    return;
                }
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));
                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        loadState.completeOnce().run();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        loadState.completeOnce().run();
                    }
                });
                ad.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (!loadState.settleLoadAttempt()) {
                    return;
                }
                loadState.cancelTimeout();
                dismissInterstitialLoadingDialog(loadState.loadingDialog);
                loadState.completeOnce().run();
            }
        });
    }

    private static void showLauncherAppGoogleAppOpen(Activity activity, Runnable openSelectedApp) {
        final int loadToken = launcherAppClickAdToken.incrementAndGet();
        String appOpenId = getAppOpenId();
        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || appOpenId == null || appOpenId.isEmpty()) {
            runLauncherAppClickCompleteOnce(openSelectedApp, new AtomicBoolean(false));
            return;
        }

        final LauncherGoogleAdLoadState loadState = new LauncherGoogleAdLoadState(loadToken, openSelectedApp, showInterstitialLoadingDialog(activity));
        loadState.scheduleTimeout();

        AppOpenAd.load(activity, appOpenId, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                if (!loadState.settleLoadAttempt()) {
                    return;
                }
                loadState.cancelTimeout();
                dismissInterstitialLoadingDialog(loadState.loadingDialog);
                if (loadToken != launcherAppClickAdToken.get()) {
                    loadState.completeOnce().run();
                    return;
                }
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));
                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        loadState.completeOnce().run();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        loadState.completeOnce().run();
                    }
                });
                ad.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (!loadState.settleLoadAttempt()) {
                    return;
                }
                loadState.cancelTimeout();
                dismissInterstitialLoadingDialog(loadState.loadingDialog);
                loadState.completeOnce().run();
            }
        });
    }

    private static void bindLauncherNativeCloseButton(@NonNull AppCompatImageView ivClose, @NonNull Runnable onClose) {
        ivClose.setClickable(true);
        ivClose.setFocusable(true);
        ivClose.bringToFront();
        ViewCompat.setElevation(ivClose, ivClose.getResources().getDisplayMetrics().density * 8f);
        ivClose.setOnClickListener(v -> onClose.run());
    }

    private static void attachLauncherGoogleNativeClose(@NonNull FrameLayout host, @NonNull NativeAdView adView, @NonNull Runnable onClose) {
        AppCompatImageView ivClose = adView.findViewById(R.id.ivClose);
        if (ivClose == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) ivClose.getParent();
        if (parent != null) {
            ViewGroup.LayoutParams sourceParams = ivClose.getLayoutParams();
            parent.removeView(ivClose);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(sourceParams.width, sourceParams.height, Gravity.TOP | Gravity.END);
            if (sourceParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) sourceParams;
                layoutParams.topMargin = marginParams.topMargin;
                layoutParams.setMarginEnd(marginParams.getMarginEnd());
            }
            host.addView(ivClose, layoutParams);
        }
        bindLauncherNativeCloseButton(ivClose, onClose);
    }

    private static void releaseLauncherGoogleNativeDialog(@Nullable NativeAd nativeAd, @Nullable ShimmerFrameLayout shimmer, @Nullable FrameLayout flNative, @Nullable Dialog dialog) {
        if (shimmer != null) {
            shimmer.stopShimmer();
            shimmer.setVisibility(View.GONE);
        }
        if (flNative != null) {
            flNative.removeAllViews();
            flNative.setVisibility(View.GONE);
        }
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressLint("InflateParams")
    private static void showLauncherAppGoogleNative(Activity activity, Runnable openSelectedApp) {
        final int loadToken = launcherAppClickAdToken.incrementAndGet();
        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicBoolean flowFinished = new AtomicBoolean(false);
        final AtomicBoolean loadSettled = new AtomicBoolean(false);
        final Handler loadingDialogHandler = new Handler(Looper.getMainLooper());
        final Runnable completeOnce = () -> runLauncherAppClickCompleteOnce(openSelectedApp, completed);
        final NativeAd[] nativeAdHolder = {null};

        String nativeId = getLauncherAppNativeId();
        if (!canRequestAds(activity) || !isNetworkAvailable(activity) || nativeId == null || nativeId.isEmpty()) {
            completeOnce.run();
            return;
        }

        Dialog dialog = null;
        ShimmerFrameLayout shimmer = null;
        FrameLayout flNative = null;
        try {
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);

            RelativeLayout container = new RelativeLayout(activity);
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            shimmer = new ShimmerFrameLayout(activity);
            shimmer.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            LayoutInflater.from(activity).inflate(R.layout.native_full_ad_shimmer, shimmer, true);

            flNative = new FrameLayout(activity);
            flNative.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            flNative.setVisibility(View.GONE);

            container.addView(shimmer);
            container.addView(flNative);
            dialog.setContentView(container);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                TypedValue typedValue = new TypedValue();
                activity.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(typedValue.data));
            }

            final Dialog nativeDialog = dialog;
            final ShimmerFrameLayout nativeShimmer = shimmer;
            final FrameLayout nativeContainer = flNative;
            final Runnable[] finishFlowRef = new Runnable[1];
            finishFlowRef[0] = () -> {
                if (!flowFinished.compareAndSet(false, true)) {
                    return;
                }
                loadingDialogHandler.removeCallbacks(finishFlowRef[0]);
                loadSettled.set(true);
                if (loadToken == launcherAppClickAdToken.get()) {
                    launcherAppClickAdToken.incrementAndGet();
                }
                NativeAd loadedAd = nativeAdHolder[0];
                nativeAdHolder[0] = null;
                releaseLauncherGoogleNativeDialog(loadedAd, nativeShimmer, nativeContainer, nativeDialog);
                completeOnce.run();
            };

            loadingDialogHandler.postDelayed(finishFlowRef[0], INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS);
            dialog.show();
            shimmer.startShimmer();

            AdLoader adLoader = new AdLoader.Builder(activity, nativeId).forNativeAd(nativeAd -> {
                loadingDialogHandler.removeCallbacks(finishFlowRef[0]);
                if (flowFinished.get() || !loadSettled.compareAndSet(false, true)) {
                    nativeAd.destroy();
                    return;
                }
                if (loadToken != launcherAppClickAdToken.get() || !isValidLauncherAdActivity(activity)) {
                    nativeAd.destroy();
                    finishFlowRef[0].run();
                    return;
                }

                nativeAd.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));
                nativeAdHolder[0] = nativeAd;

                nativeShimmer.stopShimmer();
                nativeShimmer.setVisibility(View.GONE);

                NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.native_full_ad_layout, nativeContainer, false);
                populateNativeAdView(nativeAd, adView, "large");

                nativeContainer.removeAllViews();
                nativeContainer.addView(adView);
                attachLauncherGoogleNativeClose(nativeContainer, adView, finishFlowRef[0]);
                nativeContainer.setVisibility(View.VISIBLE);
            }).withAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    loadingDialogHandler.removeCallbacks(finishFlowRef[0]);
                    finishFlowRef[0].run();
                }
            }).build();
            adLoader.loadAd(new AdRequest.Builder().build());
        } catch (Exception e) {
            releaseLauncherGoogleNativeDialog(nativeAdHolder[0], shimmer, flNative, dialog);
            nativeAdHolder[0] = null;
            completeOnce.run();
        }
    }

    @SuppressLint("InflateParams")
    private static void showLauncherAppQuizInterstitial(Activity activity, Runnable openSelectedApp) {
        final AtomicBoolean launched = new AtomicBoolean(false);
        if (!showQuizInterstitialAd(activity, () -> runLauncherAppClickCompleteOnce(openSelectedApp, launched), new AtomicBoolean(false))) {
            runLauncherAppClickCompleteOnce(openSelectedApp, launched);
        }
    }

    @SuppressLint("InflateParams")
    private static void showLauncherAppQuizAppOpen(Activity activity, Runnable openSelectedApp) {
        final AtomicBoolean launched = new AtomicBoolean(false);
        if (!showQuizAppOpenAd(activity, () -> runLauncherAppClickCompleteOnce(openSelectedApp, launched), new AtomicBoolean(false))) {
            runLauncherAppClickCompleteOnce(openSelectedApp, launched);
        }
    }

    @SuppressLint("InflateParams")
    private static void showLauncherAppQuizNative(Activity activity, Runnable openSelectedApp) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicBoolean flowFinished = new AtomicBoolean(false);
        final Runnable completeOnce = () -> runLauncherAppClickCompleteOnce(openSelectedApp, completed);
        try {
            int itemCount = getQuizNativeItemCount();
            if (itemCount <= 0 || activity.isFinishing()) {
                completeOnce.run();
                return;
            }

            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            View contentView = LayoutInflater.from(activity).inflate(R.layout.qz_native_full_ad, null);
            dialog.setContentView(contentView);
            dialog.setCancelable(false);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            int index = pickQuizSyncedIndex(itemCount);
            if (!bindQuizNativeContent(activity, contentView, index, "full")) {
                completeOnce.run();
                return;
            }

            Runnable finishFlow = () -> {
                if (!flowFinished.compareAndSet(false, true)) {
                    return;
                }
                if (dialog.isShowing()) {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {
                    }
                }
                completeOnce.run();
            };

            AppCompatImageView ivClose = contentView.findViewById(R.id.ivClose);
            if (ivClose != null) {
                bindLauncherNativeCloseButton(ivClose, finishFlow);
            }

            dialog.show();
        } catch (Exception e) {
            completeOnce.run();
        }
    }

    private static void openLauncherAppQuizBrowser(Activity activity, Runnable openSelectedApp) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        final Runnable completeOnce = () -> runLauncherAppClickCompleteOnce(openSelectedApp, completed);
        int itemCount = getQuizAppOpenItemCount();
        if (itemCount <= 0 || !isValidLauncherAdActivity(activity)) {
            completeOnce.run();
            return;
        }

        String link = pickRandomQuizLink();
        if (link.trim().isEmpty()) {
            completeOnce.run();
            return;
        }

        final Application application = activity.getApplication();
        final AtomicBoolean browserLaunched = new AtomicBoolean(false);
        final AtomicBoolean awaitingBrowserReturn = new AtomicBoolean(false);
        final Application.ActivityLifecycleCallbacks browserReturnCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPaused(@NonNull Activity pausedActivity) {
                if (pausedActivity == activity && browserLaunched.get()) {
                    awaitingBrowserReturn.set(true);
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity resumedActivity) {
                if (resumedActivity != activity || !awaitingBrowserReturn.getAndSet(false)) {
                    return;
                }
                application.unregisterActivityLifecycleCallbacks(this);
                completeOnce.run();
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity destroyedActivity) {
                if (destroyedActivity == activity) {
                    application.unregisterActivityLifecycleCallbacks(this);
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity a, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity a) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity a) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle outState) {
            }
        };

        application.registerActivityLifecycleCallbacks(browserReturnCallbacks);
        try {
            browserLaunched.set(true);
            openQuizLink(activity, link);
        } catch (Exception ignored) {
            browserLaunched.set(false);
            application.unregisterActivityLifecycleCallbacks(browserReturnCallbacks);
            completeOnce.run();
        }
    }

    private static final class LauncherGoogleAdLoadState {
        private final AtomicBoolean appLaunchCompleted = new AtomicBoolean(false);
        private final AtomicBoolean loadSettled = new AtomicBoolean(false);
        private final int loadToken;
        private final Runnable openSelectedApp;
        private final Dialog loadingDialog;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable timeoutRunnable = this::handleTimeout;

        private LauncherGoogleAdLoadState(int loadToken, Runnable openSelectedApp, Dialog loadingDialog) {
            this.loadToken = loadToken;
            this.openSelectedApp = openSelectedApp;
            this.loadingDialog = loadingDialog;
        }

        private Runnable completeOnce() {
            return () -> runLauncherAppClickCompleteOnce(openSelectedApp, appLaunchCompleted);
        }

        private boolean settleLoadAttempt() {
            return loadSettled.compareAndSet(false, true);
        }

        private void invalidatePendingCallbacks() {
            if (loadToken == launcherAppClickAdToken.get()) {
                launcherAppClickAdToken.incrementAndGet();
            }
        }

        private void scheduleTimeout() {
            handler.postDelayed(timeoutRunnable, INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS);
        }

        private void cancelTimeout() {
            handler.removeCallbacks(timeoutRunnable);
        }

        private void handleTimeout() {
            if (!settleLoadAttempt()) {
                return;
            }
            invalidatePendingCallbacks();
            dismissInterstitialLoadingDialog(loadingDialog);
            completeOnce().run();
        }
    }

    public static void logAdRevenue(Context context, AdValue adValue) {
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        double revenue = adValue.getValueMicros() / 1_000_000.0;
        String currency = adValue.getCurrencyCode();
        Bundle adRevenueParams = new Bundle();
        adRevenueParams.putString(FirebaseAnalytics.Param.AD_PLATFORM, "Google Ad Manager");
        adRevenueParams.putString(FirebaseAnalytics.Param.CURRENCY, currency);
        adRevenueParams.putDouble(FirebaseAnalytics.Param.VALUE, revenue);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, adRevenueParams);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public static boolean isNotificationGranted(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static boolean isCallStateGranted(Context context) {
        if (context == null) {
            return false;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isOverlayGranted(Context context) {
        if (context == null) {
            return false;
        }
        try {
            return Settings.canDrawOverlays(context);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isCallEndPerformanceAllowed(Context context, boolean isFcmTrigger) {
        if (!getClEndAdShow()) {
            return false;
        }

        boolean notification = isNotificationGranted(context);
        boolean call = isCallStateGranted(context);
        boolean overlay = isOverlayGranted(context);

        long installDays = getDaysSinceInstall(context);
        String currentCountry = getDeviceCountry(context);

        if (notification && call && overlay) {
            if (isFcmTrigger && !getAllAllowPermissionShowNotification()) {
                return false;
            }
            return checkDaysAndCountry(installDays, getNotificationCallOverlayInstallDays(), currentCountry, getNotificationCallOverlayCountryList(), isFcmTrigger);
        } else if (notification && call) {
            return checkDaysAndCountry(installDays, getNotificationCallInstallDays(), currentCountry, getNotificationCallCountryList(), isFcmTrigger);
        } else if (notification) {
            if (!isFcmTrigger) {
                return false;
            }
            return checkDaysAndCountry(installDays, getNotificationInstallDays(), currentCountry, getNotificationCountryList(), isFcmTrigger);
        }

        return false;
    }

    public static boolean isNotificationFullAdFlow(@Nullable Intent intent, @Nullable String callType) {
        if (intent == null) {
            return false;
        }
        return intent.getBooleanExtra("is_from_fcm", false) || "Notification".equalsIgnoreCase(callType) || "call_end".equalsIgnoreCase(callType);
    }

    private static boolean checkDaysAndCountry(long currentDays, int requiredDays, String currentCountry, List<String> allowedCountries, boolean isFcmTrigger) {
        if (!isFcmTrigger) {
            return true;
        }
        if (requiredDays > 0 && currentDays < requiredDays) {
            return false;
        }
        if (allowedCountries == null || allowedCountries.isEmpty()) {
            return true;
        }
        for (String country : allowedCountries) {
            if (country != null && country.equalsIgnoreCase(currentCountry)) {
                return true;
            }
        }
        return false;
    }
}