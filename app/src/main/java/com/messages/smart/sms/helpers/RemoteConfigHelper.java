package com.messages.smart.sms.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.messages.smart.sms.MyApplication;
import com.messages.smart.sms.R;
import com.messages.smart.sms.common.AdPlacement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RemoteConfigHelper {
    private static final AtomicBoolean IS_FETCHING = new AtomicBoolean(false);

    public interface FetchCallback {
        void onComplete(boolean success);
    }

    private RemoteConfigHelper() {
    }

    public static int getMainAdAutoSecond() {
        return AdPlacement.getMainAdAutoSecond();
    }

    public static void fetchRemoteConfig(Activity activity, @Nullable FetchCallback onComplete) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (!IS_FETCHING.compareAndSet(false, true)) {
            return;
        }

        if (FirebaseApp.getApps(activity).isEmpty()) {
            FirebaseApp.initializeApp(activity);
        }

        int version = getAppVersion(activity);
        String key = "Messages_" + version;

        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build();
        firebaseRemoteConfig.setConfigSettingsAsync(settings);
        firebaseRemoteConfig.setDefaultsAsync(R.xml.default_config);
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(activity, task -> {
            IS_FETCHING.set(false);
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            boolean fetchSucceeded = false;
            if (task.isSuccessful()) {
                try {
                    String remoteJson = firebaseRemoteConfig.getString(key);
                    if (remoteJson == null || remoteJson.trim().isEmpty()) {
                        AdPlacement.restoreShowScreenFlow(activity);
                        notifyComplete(activity, onComplete, true);
                        return;
                    }

                    String trimmedJson = remoteJson.trim();
                    JSONObject jsonObject;
                    if (trimmedJson.startsWith("[")) {
                        JSONArray jsonArray = new JSONArray(trimmedJson);
                        if (jsonArray.length() == 0) {
                            AdPlacement.restoreShowScreenFlow(activity);
                            notifyComplete(activity, onComplete, true);
                            return;
                        }
                        jsonObject = jsonArray.getJSONObject(0);
                    } else {
                        jsonObject = new JSONObject(trimmedJson);
                    }

                    String marketingUrl = manageMarketingConverter(AdPlacement.getReferrerUrl(activity));
                    String facebookUser = facebookMarketingConverter(AdPlacement.getReferrerUrl(activity));
                    String newString = stringMarketingConverter(AdPlacement.getReferrerUrl(activity));

                    if (newString.equals(activity.getString(R.string.markrting_converter1)) || facebookUser.equals(activity.getString(R.string.utm_source_apps_facebook_com)) || facebookUser.equals(activity.getString(R.string.utm_source_apps_instagram_com)) || marketingUrl.equals(activity.getString(R.string.utm_source_marketing))) {
                        jsonObject = resolveUserConfig(jsonObject, "paid_user");
                    } else {
                        jsonObject = resolveUserConfig(jsonObject, "normal_user");
                    }

                    applyAdConfig(activity, jsonObject, firebaseRemoteConfig.getString("Show_Screen_Flow"));
                    fetchSucceeded = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    AdPlacement.restoreShowScreenFlow(activity);
                }
            } else {
                AdPlacement.restoreShowScreenFlow(activity);
            }

            notifyComplete(activity, onComplete, fetchSucceeded);
        });
    }

    private static void notifyComplete(Activity activity, @Nullable FetchCallback onComplete, boolean success) {
        if (onComplete == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        onComplete.onComplete(success);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    private static JSONObject resolveUserConfig(JSONObject root, String profileKey) throws Exception {
        if (root == null) {
            throw new Exception("Remote Config root JSON is null");
        }
        if (!root.has(profileKey)) {
            return root;
        }
        Object profileValue = root.get(profileKey);
        if (profileValue instanceof JSONObject) {
            return (JSONObject) profileValue;
        }
        if (profileValue instanceof String) {
            String nestedKey = ((String) profileValue).trim();
            if (nestedKey.isEmpty()) {
                throw new Exception("Remote Config profile key '" + profileKey + "' is empty");
            }
            if (nestedKey.startsWith("{")) {
                return new JSONObject(nestedKey);
            }
            return root.getJSONObject(nestedKey);
        }
        throw new Exception("Unsupported Remote Config profile type for '" + profileKey + "': " + profileValue);
    }

    private static void applyAdConfig(Context context, JSONObject jsonObject, String topLevelFlowJson) throws Exception {
        AdPlacement.applyShowScreenFlowFromConfig(jsonObject, topLevelFlowJson);
        AdPlacement.cacheShowScreenFlow(context);

        String privacyPolicy = jsonObject.getString("Privacy_Policy");
        String termsConditions = jsonObject.getString("Terms_Conditions");
        boolean appOpenAdShow = jsonObject.getBoolean("App_Open_Ad_Show");
        boolean appOpenDialogShow = jsonObject.getBoolean("App_Open_Dialog_Show");
        String appOpenId = jsonObject.getString("App_Open_Id");
        int appOpenShowPerDay = jsonObject.getInt("App_Open_Show_Per_Day");
        int interstitialClick = jsonObject.getInt("Interstitial_Click");
        boolean rightSwipeInterstitialAdShow = jsonObject.getBoolean("Right_Swipe_Interstitial_Ad_Show");
        int rightSwipeInterstitial = jsonObject.getInt("Right_Swipe_Interstitial");
        boolean interstitialAdShowSwitchFragment = jsonObject.getBoolean("Interstitial_Ad_Show_Switch_Fragment");
        int interstitialAdShowSwitchClick = jsonObject.getInt("Interstitial_Ad_Show_Switch_Click");
        String nativeAdLabelColor = jsonObject.optString("Native_Ad_Label_Color", "");
        String nativeAdButtonColor = jsonObject.optString("Native_Ad_Button_Color", "");
        boolean clEndScreenShow = jsonObject.optBoolean("Cl_End_Screen_Show", false);
        String adPriority = jsonObject.optString("Ad_Priority", "");
        boolean googleAdFailedShowQuiz = jsonObject.optBoolean("Google_Ad_Failed_Show_Quiz", false);
        boolean newsScreenShow = jsonObject.optBoolean("News_Screen_Show", false);
        boolean defaultAppPopupShow = jsonObject.optBoolean("Default_App_Popup_Show", false);
        int defaultAppPopupCount = jsonObject.optInt("Default_App_Popup_Count", 0);

        JSONObject screen = jsonObject.getJSONObject("Screen");
        JSONObject splashScreen = screen.getJSONObject("SplashScreen");
        int splashDuration = splashScreen.getInt("Splash_Duration");
        boolean splashAdShow = splashScreen.getBoolean("Splash_Ad_Show");
        String splashAdType = splashScreen.getString("Splash_Ad_Type");
        String splashBannerId = splashScreen.getString("Splash_Banner_Id");
        String splashNativeId = splashScreen.getString("Splash_Native_Id");
        boolean afterSplashAdShow = splashScreen.getBoolean("After_Splash_Ad_Show");
        String afterSplashAdType = splashScreen.getString("After_Splash_Ad_Type");
        String afterSplashInterstitialId = splashScreen.getString("After_Splash_Interstitial_Id");

        JSONObject introScreen = screen.getJSONObject("IntroScreen");
        String introType = introScreen.getString("Intro_Type");
        boolean introSwipeNativeAdShow = introScreen.getBoolean("Intro_Swipe_Native_Ad_Show");
        String introSwipeNativeId1 = introScreen.getString("Intro_Swipe_Native_Id_1");
        String introSwipeNativeId2 = introScreen.getString("Intro_Swipe_Native_Id_2");
        String introSwipeNativeId3 = introScreen.getString("Intro_Swipe_Native_Id_3");
        int introButtonScreen = introScreen.getInt("Intro_Button_Screen");
        boolean introButtonAdShow = introScreen.getBoolean("Intro_Button_Ad_Show");
        String introButtonAdType = introScreen.getString("Intro_Button_Ad_Type");
        String introButtonBannerId1 = introScreen.getString("Intro_Button_Banner_Id_1");
        String introButtonBannerId2 = introScreen.getString("Intro_Button_Banner_Id_2");
        String introButtonBannerId3 = introScreen.getString("Intro_Button_Banner_Id_3");
        String introButtonBannerId4 = introScreen.getString("Intro_Button_Banner_Id_4");
        String introButtonNativeId1 = introScreen.getString("Intro_Button_Native_Id_1");
        String introButtonNativeId2 = introScreen.getString("Intro_Button_Native_Id_2");
        String introButtonNativeId3 = introScreen.getString("Intro_Button_Native_Id_3");
        String introButtonNativeId4 = introScreen.getString("Intro_Button_Native_Id_4");

        JSONObject collectionScreen = screen.getJSONObject("CollectionScreen");
        boolean collectionAdShow = collectionScreen.getBoolean("Collection_Ad_Show");
        String collectionAdType = collectionScreen.getString("Collection_Ad_Type");
        String collectionBannerId = collectionScreen.getString("Collection_Banner_Id");
        String collectionNativeId = collectionScreen.getString("Collection_Native_Id");

        JSONObject defaultScreen = screen.getJSONObject("DefaultScreen");
        boolean defaultAdShow = defaultScreen.getBoolean("Default_Ad_Show");
        String defaultAdType = defaultScreen.getString("Default_Ad_Type");
        String defaultBannerId = defaultScreen.getString("Default_Banner_Id");
        String defaultNativeId = defaultScreen.getString("Default_Native_Id");
        AdPlacement.applyAfterDefaultAdConfigFromScreen(defaultScreen);

        JSONObject languageScreen = screen.getJSONObject("LanguageScreen");
        boolean languageAdShow = languageScreen.getBoolean("Language_Ad_Show");
        String languageAdType = languageScreen.getString("Language_Ad_Type");
        String languageBannerId = languageScreen.getString("Language_Banner_Id");
        String languageNativeId = languageScreen.getString("Language_Native_Id");
        boolean languageInterstitialAdShow = languageScreen.getBoolean("Language_Interstitial_Ad_Show");

        JSONObject mainScreen = screen.getJSONObject("MainScreen");
        boolean mainAdShow = mainScreen.optBoolean("Main_Ad_Show");
        String mainAdType = mainScreen.optString("Main_Ad_Type", "banner");
        String mainBannerId = mainScreen.optString("Main_Banner_Id", "");
        String mainNativeId = mainScreen.optString("Main_Native_Id", "");
        boolean mainAdAutoRefresh = mainScreen.optBoolean("Main_Ad_Auto_Refresh");
        int mainAdAutoSecond = mainScreen.optInt("Main_Ad_Auto_Second");

        JSONObject messageListScreen = screen.getJSONObject("MessageListScreen");
        boolean messageListAdShow = messageListScreen.optBoolean("MessageList_Ad_Show");
        String messageListAdType = messageListScreen.optString("MessageList_Ad_Type", "banner");
        String messageListBannerId = messageListScreen.optString("MessageList_Banner_Id", "");
        String messageListNativeId = messageListScreen.optString("MessageList_Native_Id", "");
        boolean messageListInterstitialAdShow = messageListScreen.getBoolean("MessageList_Interstitial_Ad_Show");
        int messageListInterstitialClick = messageListScreen.getInt("MessageList_Interstitial_Click");
        String messageListInterstitialId = messageListScreen.getString("MessageList_Interstitial_Id");

        JSONObject messageContentScreen = screen.getJSONObject("MessageContentScreen");
        boolean messageContentBannerAdShow = messageContentScreen.getBoolean("MessageContent_Banner_Ad_Show");
        String messageContentBannerId = messageContentScreen.getString("MessageContent_Banner_Id");
        boolean messageContentBackInterstitialAdShow = messageContentScreen.getBoolean("MessageContent_Back_Interstitial_Ad_Show");
        int messageContentBackInterstitialClick = messageContentScreen.getInt("MessageContent_Back_Interstitial_Click");
        String messageContentBackInterstitialId = messageContentScreen.getString("MessageContent_Back_Interstitial_Id");

        JSONObject launcherSettingScreen = screen.getJSONObject("LauncherSettingScreen");
        boolean launcherSettingAdShow = launcherSettingScreen.getBoolean("LauncherSetting_Ad_Show");
        String launcherSettingAdType = launcherSettingScreen.getString("LauncherSetting_Ad_Type");
        String launcherSettingBannerId = launcherSettingScreen.getString("LauncherSetting_Banner_Id");
        String launcherSettingNativeId = launcherSettingScreen.getString("LauncherSetting_Native_Id");

        JSONObject otherScreen = screen.getJSONObject("OtherScreen");
        boolean otherAdShow = otherScreen.getBoolean("Other_Ad_Show");
        String otherAdType = otherScreen.getString("Other_Ad_Type");
        String otherBannerId = otherScreen.getString("Other_Banner_Id");
        String otherNativeId = otherScreen.getString("Other_Native_Id");
        boolean otherInterstitialAdShow = otherScreen.getBoolean("Other_Interstitial_Ad_Show");
        String otherInterstitialId = otherScreen.getString("Other_Interstitial_Id");

        JSONObject launcherAppScreen = screen.getJSONObject("LauncherAppScreen");
        boolean launcherAppNativeAdShow = launcherAppScreen.getBoolean("LauncherApp_Native_Ad_Show");
        int launcherAppNativeAdShowPerDay = launcherAppScreen.getInt("LauncherApp_Native_Ad_Show_Per_Day");
        String launcherAppNativeId = launcherAppScreen.getString("LauncherApp_Native_Id");
        boolean launcherAppBannerAdShow = launcherAppScreen.getBoolean("LauncherApp_Banner_Ad_Show");
        int launcherAppBannerAdShowPerDay = launcherAppScreen.getInt("LauncherApp_Banner_Ad_Show_Per_Day");
        String launcherAppBannerId = launcherAppScreen.getString("LauncherApp_Banner_Id");
        boolean launcherAppClickAdShow = launcherAppScreen.optBoolean("LauncherApp_Click_Ad_Show", false);
        int launcherAppCount = launcherAppScreen.optInt("LauncherApp_Count", 0);
        String launcherAppAdType = launcherAppScreen.optString("LauncherApp_Ad_Type", "");
        String launcherAppInterstitialId = launcherAppScreen.optString("LauncherApp_Interstitial_Id", "");

        JSONObject newsScreen = screen.getJSONObject("NewsScreen");
        boolean newsNativeAdShow = newsScreen.getBoolean("News_Native_Ad_Show");
        String newsNativeId1 = newsScreen.getString("News_Native_Id_1");
        String newsNativeId2 = newsScreen.getString("News_Native_Id_2");

        JSONObject messageScreen = screen.getJSONObject("MessageScreen");
        boolean messageAdShow = messageScreen.getBoolean("Message_Ad_Show");
        String messageAdType = messageScreen.getString("Message_Ad_Type");
        String messageBannerId = messageScreen.getString("Message_Banner_Id");
        String messageNativeId = messageScreen.getString("Message_Native_Id");

        JSONObject clEndScreen = screen.getJSONObject("ClEndScreen");
        boolean clEndAdShow = clEndScreen.getBoolean("ClEnd_Ad_Show");
        String clEndAdType = clEndScreen.getString("ClEnd_Ad_Type");
        String clEndBannerId = clEndScreen.getString("ClEnd_Banner_Id");
        String clEndNativeId = clEndScreen.getString("ClEnd_Native_Id");
        boolean clEndBackAdShow = clEndScreen.getBoolean("ClEnd_Back_Ad_Show");
        String clEndBackAdType = clEndScreen.getString("ClEnd_Back_Ad_Type");
        int clEndBackAdShowAfterDay = clEndScreen.getInt("ClEnd_Back_Ad_Show_After_Day");
        int clEndBackAdShowPerDay = clEndScreen.getInt("ClEnd_Back_Ad_Show_Per_Day");
        String clEndBackAdInterstitialId = clEndScreen.getString("ClEnd_Back_Ad_Interstitial_Id");
        boolean clEndBackAdCountryIP = clEndScreen.getBoolean("ClEnd_Back_Ad_Country_IP");
        JSONArray clEndBackAdShowCountryArray = clEndScreen.optJSONArray("ClEnd_Back_Ad_Show_Country");
        ArrayList<String> clEndBackAdShowCountryList = parseCountryArray(clEndBackAdShowCountryArray);

        int notificationInstallDays = clEndScreen.optInt("Notification_Install_Days", 0);
        int notificationCallInstallDays = clEndScreen.optInt("Notification_Call_Install_Days", 0);
        int notificationCallOverlayInstallDays = clEndScreen.optInt("Notification_Call_Overlay_Install_Days", 0);
        boolean allAllowPermissionShowNotification = clEndScreen.optBoolean("All_Allow_Permission_Show_Notification", false);
        boolean notificationBackAdShow = clEndScreen.optBoolean("Notification_Back_Ad_Show", false);
        boolean notificationCloseButtonShow = clEndScreen.optBoolean("Notification_Close_Button_Show", false);
        ArrayList<String> notificationCountryList = parseCountryArray(clEndScreen.optJSONArray("Notification_Country"));
        ArrayList<String> notificationCallCountryList = parseCountryArray(clEndScreen.optJSONArray("Notification_Call_Country"));
        ArrayList<String> notificationCallOverlayCountryList = parseCountryArray(clEndScreen.optJSONArray("Notification_Call_Overlay_Country"));

        AdPlacement.setPrivacyPolicy(privacyPolicy);
        AdPlacement.setTermsConditions(termsConditions);
        AdPlacement.setAppOpenAdShow(appOpenAdShow);
        AdPlacement.setAppOpenDialogShow(appOpenDialogShow);
        AdPlacement.setAppOpenId(appOpenId);
        if (context.getApplicationContext() instanceof MyApplication) {
            ((MyApplication) context.getApplicationContext()).fetchAd();
        }
        AdPlacement.setAppOpenShowPerDay(appOpenShowPerDay);
        AdPlacement.setInterstitialClick(interstitialClick);
        MyApplication.interstitialCount = AdPlacement.getInterstitialClick();
        AdPlacement.setRightSwipeInterstitialAdShow(rightSwipeInterstitialAdShow);
        AdPlacement.setRightSwipeInterstitial(rightSwipeInterstitial);
        AdPlacement.setInterstitialAdShowSwitchFragment(interstitialAdShowSwitchFragment);
        AdPlacement.setInterstitialAdShowSwitchClick(interstitialAdShowSwitchClick);
        AdPlacement.setNativeAdLabelColor(nativeAdLabelColor);
        AdPlacement.setNativeAdButtonColor(nativeAdButtonColor);
        AdPlacement.setClEndScreenShow(clEndScreenShow);
        AdPlacement.setAdPriority(adPriority);
        AdPlacement.setGoogleAdFailedShowQuiz(googleAdFailedShowQuiz);

        JSONObject quizAdsDesign = screen.optJSONObject("QuizAdsDesign");
        if (quizAdsDesign != null) {
            AdPlacement.applyQuizAdsConfig(quizAdsDesign);
        }
        JSONObject appProxyStructure = screen.optJSONObject("AppProxyStructure");
        if (appProxyStructure == null) {
            appProxyStructure = jsonObject.optJSONObject("AppProxyStructure");
        }
        if (appProxyStructure != null) {
            AdPlacement.applyAppProxyConfig(appProxyStructure);
        }

        AdPlacement.setNewsScreenShow(newsScreenShow);
        AdPlacement.setDefaultAppPopupShow(defaultAppPopupShow);
        AdPlacement.setDefaultAppPopupCount(defaultAppPopupCount);

        AdPlacement.setSplashDuration(splashDuration);
        AdPlacement.setSplashAdShow(splashAdShow);
        AdPlacement.setSplashAdType(splashAdType);
        AdPlacement.setSplashBannerId(splashBannerId);
        AdPlacement.setSplashNativeId(splashNativeId);
        AdPlacement.setAfterSplashAdShow(afterSplashAdShow);
        AdPlacement.setAfterSplashAdType(afterSplashAdType);
        AdPlacement.setAfterSplashInterstitialId(afterSplashInterstitialId);

        AdPlacement.setIntroType(introType);
        AdPlacement.setIntroSwipeNativeAdShow(introSwipeNativeAdShow);
        AdPlacement.setIntroSwipeNativeId1(introSwipeNativeId1);
        AdPlacement.setIntroSwipeNativeId2(introSwipeNativeId2);
        AdPlacement.setIntroSwipeNativeId3(introSwipeNativeId3);
        AdPlacement.setIntroButtonScreen(introButtonScreen);
        AdPlacement.setIntroButtonAdShow(introButtonAdShow);
        AdPlacement.setIntroButtonAdType(introButtonAdType);
        AdPlacement.setIntroButtonBannerId1(introButtonBannerId1);
        AdPlacement.setIntroButtonBannerId2(introButtonBannerId2);
        AdPlacement.setIntroButtonBannerId3(introButtonBannerId3);
        AdPlacement.setIntroButtonBannerId4(introButtonBannerId4);
        AdPlacement.setIntroButtonNativeId1(introButtonNativeId1);
        AdPlacement.setIntroButtonNativeId2(introButtonNativeId2);
        AdPlacement.setIntroButtonNativeId3(introButtonNativeId3);
        AdPlacement.setIntroButtonNativeId4(introButtonNativeId4);

        AdPlacement.setCollectionAdShow(collectionAdShow);
        AdPlacement.setCollectionAdType(collectionAdType);
        AdPlacement.setCollectionBannerId(collectionBannerId);
        AdPlacement.setCollectionNativeId(collectionNativeId);

        AdPlacement.setDefaultAdShow(defaultAdShow);
        AdPlacement.setDefaultAdType(defaultAdType);
        AdPlacement.setDefaultBannerId(defaultBannerId);
        AdPlacement.setDefaultNativeId(defaultNativeId);

        AdPlacement.setLanguageAdShow(languageAdShow);
        AdPlacement.setLanguageAdType(languageAdType);
        AdPlacement.setLanguageBannerId(languageBannerId);
        AdPlacement.setLanguageNativeId(languageNativeId);
        AdPlacement.setLanguageInterstitialAdShow(languageInterstitialAdShow);

        AdPlacement.setMainAdShow(mainAdShow);
        AdPlacement.setMainAdType(mainAdType);
        AdPlacement.setMainBannerId(mainBannerId);
        AdPlacement.setMainNativeId(mainNativeId);
        AdPlacement.setMainAdAutoRefresh(mainAdAutoRefresh);
        AdPlacement.setMainAdAutoSecond(mainAdAutoSecond);

        AdPlacement.setMessageListAdShow(messageListAdShow);
        AdPlacement.setMessageListAdType(messageListAdType);
        AdPlacement.setMessageListBannerId(messageListBannerId);
        AdPlacement.setMessageListNativeId(messageListNativeId);
        AdPlacement.setMessageListInterstitialAdShow(messageListInterstitialAdShow);
        AdPlacement.setMessageListInterstitialClick(messageListInterstitialClick);
        AdPlacement.setMessageListInterstitialId(messageListInterstitialId);

        AdPlacement.setMessageContentBannerAdShow(messageContentBannerAdShow);
        AdPlacement.setMessageContentBannerId(messageContentBannerId);
        AdPlacement.setMessageContentBackInterstitialAdShow(messageContentBackInterstitialAdShow);
        AdPlacement.setMessageContentBackInterstitialClick(messageContentBackInterstitialClick);
        AdPlacement.setMessageContentBackInterstitialId(messageContentBackInterstitialId);

        AdPlacement.setLauncherSettingAdShow(launcherSettingAdShow);
        AdPlacement.setLauncherSettingAdType(launcherSettingAdType);
        AdPlacement.setLauncherSettingBannerId(launcherSettingBannerId);
        AdPlacement.setLauncherSettingNativeId(launcherSettingNativeId);

        AdPlacement.setOtherAdShow(otherAdShow);
        AdPlacement.setOtherAdType(otherAdType);
        AdPlacement.setOtherBannerId(otherBannerId);
        AdPlacement.setOtherNativeId(otherNativeId);
        AdPlacement.setOtherInterstitialAdShow(otherInterstitialAdShow);
        AdPlacement.setOtherInterstitialId(otherInterstitialId);

        AdPlacement.setLauncherAppNativeAdShow(launcherAppNativeAdShow);
        AdPlacement.setLauncherAppNativeAdShowPerDay(launcherAppNativeAdShowPerDay);
        AdPlacement.setLauncherAppNativeId(launcherAppNativeId);
        AdPlacement.setLauncherAppBannerAdShow(launcherAppBannerAdShow);
        AdPlacement.setLauncherAppBannerAdShowPerDay(launcherAppBannerAdShowPerDay);
        AdPlacement.setLauncherAppBannerId(launcherAppBannerId);
        AdPlacement.setLauncherAppClickAdShow(launcherAppClickAdShow);
        AdPlacement.setLauncherAppCount(launcherAppCount);
        AdPlacement.setLauncherAppAdType(launcherAppAdType);
        AdPlacement.setLauncherAppInterstitialId(launcherAppInterstitialId);

        AdPlacement.setNewsNativeAdShow(newsNativeAdShow);
        AdPlacement.setNewsNativeId1(newsNativeId1);
        AdPlacement.setNewsNativeId2(newsNativeId2);

        AdPlacement.setMessageAdShow(messageAdShow);
        AdPlacement.setMessageAdType(messageAdType);
        AdPlacement.setMessageBannerId(messageBannerId);
        AdPlacement.setMessageNativeId(messageNativeId);
        AdPlacement.setRemoteConfigApplied(true);
        AdPlacement.cacheMessageAdConfig(context);

        AdPlacement.setClEndAdShow(clEndAdShow);
        AdPlacement.setClEndAdType(clEndAdType);
        AdPlacement.setClEndBannerId(clEndBannerId);
        AdPlacement.setClEndNativeId(clEndNativeId);
        AdPlacement.setClEndBackAdShow(clEndBackAdShow);
        AdPlacement.setClEndBackAdType(clEndBackAdType);
        AdPlacement.setClEndBackAdShowAfterDay(clEndBackAdShowAfterDay);
        AdPlacement.setClEndBackAdShowPerDay(clEndBackAdShowPerDay);
        AdPlacement.setClEndBackAdInterstitialId(clEndBackAdInterstitialId);
        AdPlacement.setClEndBackAdCountryIP(clEndBackAdCountryIP);
        AdPlacement.setClEndBackAdShowCountryList(clEndBackAdShowCountryList);
        AdPlacement.setNotificationInstallDays(notificationInstallDays);
        AdPlacement.setNotificationCallInstallDays(notificationCallInstallDays);
        AdPlacement.setNotificationCallOverlayInstallDays(notificationCallOverlayInstallDays);
        AdPlacement.setAllAllowPermissionShowNotification(allAllowPermissionShowNotification);
        AdPlacement.setNotificationBackAdShow(notificationBackAdShow);
        AdPlacement.setNotificationCloseButtonShow(notificationCloseButtonShow);
        AdPlacement.setNotificationCountryList(notificationCountryList);
        AdPlacement.setNotificationCallCountryList(notificationCallCountryList);
        AdPlacement.setNotificationCallOverlayCountryList(notificationCallOverlayCountryList);
        AdPlacement.cacheClEndConfig(context);
    }

    private static ArrayList<String> parseCountryArray(@Nullable JSONArray countryArray) {
        ArrayList<String> countryList = new ArrayList<>();
        if (countryArray != null) {
            for (int i = 0; i < countryArray.length(); i++) {
                countryList.add(countryArray.optString(i, "").toLowerCase());
            }
        }
        return countryList;
    }

    private static String stringMarketingConverter(String str) {
        if (str == null) {
            return "";
        }
        String[] arr = str.split("=", 2);
        return arr.length > 0 ? arr[0] : "";
    }

    private static String manageMarketingConverter(String str) {
        if (str == null) {
            return "";
        }
        String[] arr = str.split("=", 2);
        if (arr.length > 1) {
            return arr[1];
        }
        return "";
    }

    private static String facebookMarketingConverter(String input) {
        if (input == null) {
            return "";
        }
        int index = input.indexOf('&');
        if (index != -1) {
            return input.substring(0, index);
        }
        return "";
    }
}