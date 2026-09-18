package com.messages.smartsms.common;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.messages.smartsms.R;
import com.messages.smartsms.helpers.BlockHelper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Utils {
    public static final int ONBOARDING_FLOW_LANGUAGE_FIRST = 1;
    public static final int ONBOARDING_FLOW_DEFAULT_FIRST = 2;

    private static final String PREF_KEY_APP_LANGUAGE_SELECTED = "appLanguageSelected";
    private static final String PREF_KEY_LANGUAGE_FLOW_COMPLETED = "languageFlowCompleted";
    private static final String PREF_KEY_INTRO_COMPLETED = "introCompleted";
    private static final String PREF_KEY_COLLECTION_COMPLETED = "collectionCompleted";
    private static final String PREF_KEY_DEFAULT_HOME_FLOW_COMPLETED = "defaultHomeFlowCompleted";
    private static final String PREF_KEY_DEFAULT_SMS_FLOW_COMPLETED = "defaultSmsFlowCompleted";
    private static final String PREF_KEY_APP_LANGUAGE_NEW = "appLanguageNew";
    private static final String PREF_KEY_APP_LANGUAGE_NAME_NEW = "appLanguageNameNew";
    private static final String PREF_KEY_DELAY_SENDING = "delaySending";
    private static final String PREF_KEY_SIGNATURES_NAME = "signaturesName";
    private static final String PREF_KEY_CATEGORY_BAR = "categoryBar";
    private static final String PREF_KEY_LABEL_VISIBILITY = "labelVisibility";
    private static final String PREF_KEY_APP_ICON_SIZE = "appIconSize";
    private static final String PREF_KEY_APP_LABEL_SIZE = "appLabelSize";
    private static final String PREF_KEY_APP_SERIALIZE = "appSerialize";
    private static final String PREF_KEY_BLOCK_THREADS = "blockThreads";

    public static boolean isAppLanguageStarting;
    public static String appLanguage;
    public static String appLanguageName;
    public static boolean isPermissionAllow;
    public static boolean isDefaultApp;
    public static boolean isFromContacts;
    private static boolean localeChangedPending;
    private static String pendingLanguageCode;

    private static Boolean wallpaperDarkCached;
    private static long wallpaperDarkCachedAtMs;
    private static final long WALLPAPER_DARK_CACHE_TTL_MS = 60_000L;

    public static void markLocaleChanged(String languageCode) {
        localeChangedPending = true;
        pendingLanguageCode = normalizeLanguageTag(languageCode);
    }

    public static boolean consumeLocaleChanged() {
        if (localeChangedPending) {
            localeChangedPending = false;
            return true;
        }
        return false;
    }

    public static boolean isLocaleChangedPending() {
        return localeChangedPending;
    }

    public static String getPendingLanguageCode(Context context) {
        if (pendingLanguageCode != null && !pendingLanguageCode.isEmpty()) {
            String code = pendingLanguageCode;
            pendingLanguageCode = null;
            return code;
        }
        return getAppLanguageNew(context);
    }

    public static String normalizeLanguageTag(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return "en";
        }
        return languageCode.trim().replace('_', '-');
    }

    @NonNull
    private static Context prefsContext(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        return appContext != null ? appContext : context;
    }

    public static void applyStoredLocale(Context context) {
        if (context == null) {
            return;
        }
        applyLocaleWithoutRecreate(context, getAppLanguageNew(context));
    }

    public static void setLocale(Context context, String languageCode) {
        applyLocaleWithoutRecreate(context, languageCode);
    }

    public static Context createLocaleContext(Context context, String languageCode) {
        if (context == null) {
            return null;
        }
        String languageTag = normalizeLanguageTag(languageCode);
        Locale locale = Locale.forLanguageTag(languageTag);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
        } else {
            configuration.setLocale(locale);
        }
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    public static void applyLocaleWithoutRecreate(Context context, String languageCode) {
        if (context == null) {
            return;
        }
        String languageTag = normalizeLanguageTag(languageCode);
        Locale locale = Locale.forLanguageTag(languageTag);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
        } else {
            configuration.setLocale(locale);
        }
        configuration.setLayoutDirection(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public static Context wrapContext(Context context) {
        if (context == null) {
            return null;
        }
        return createLocaleContext(context, getAppLanguageNew(context));
    }

    @Nullable
    public static Context localeResourcesContext(Context context) {
        if (context == null) {
            return null;
        }
        Context localized = createLocaleContext(context, getAppLanguageNew(context));
        return localized != null ? localized : context;
    }

    public static void saveAndApplyAppLanguage(Context context, String languageCode, @Nullable String languageName) {
        if (context == null) {
            return;
        }
        Context appContext = prefsContext(context);
        String languageTag = normalizeLanguageTag(languageCode);

        appContext.getSharedPreferences("appLanguageNew", MODE_PRIVATE).edit().putString(PREF_KEY_APP_LANGUAGE_NEW, languageTag).commit();
        if (languageName != null) {
            appContext.getSharedPreferences("appLanguageNameNew", MODE_PRIVATE).edit().putString(PREF_KEY_APP_LANGUAGE_NAME_NEW, languageName).commit();
        }

        appLanguage = languageTag;
        if (languageName != null) {
            appLanguageName = languageName;
        }

        setAppLanguageSelected(appContext, true);
        applyLocaleWithoutRecreate(appContext, languageTag);
        applyLocaleWithoutRecreate(context, languageTag);
        markLocaleChanged(languageTag);
    }

    public static void applySavedLocaleToActivity(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        String languageTag = getAppLanguageNew(activity);
        applyLocaleWithoutRecreate(prefsContext(activity), languageTag);
        applyLocaleWithoutRecreate(activity, languageTag);
    }

    public static void setAppLanguageSelected(Context context, boolean value) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = prefsContext(context).getSharedPreferences("appLanguageSelected", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(PREF_KEY_APP_LANGUAGE_SELECTED, value).apply();
    }

    public static boolean getAppLanguageSelected(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = prefsContext(context).getSharedPreferences("appLanguageSelected", MODE_PRIVATE);
        return sharedPreferences.getBoolean(PREF_KEY_APP_LANGUAGE_SELECTED, false);
    }

    public static void setLanguageFlowCompleted(Context context, boolean value) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = prefsContext(context).getSharedPreferences("languageFlowCompleted", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(PREF_KEY_LANGUAGE_FLOW_COMPLETED, value).apply();
    }

    public static boolean getLanguageFlowCompleted(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences prefs = prefsContext(context).getSharedPreferences("languageFlowCompleted", MODE_PRIVATE);
        if (prefs.contains(PREF_KEY_LANGUAGE_FLOW_COMPLETED)) {
            return prefs.getBoolean(PREF_KEY_LANGUAGE_FLOW_COMPLETED, false);
        }
        return getAppLanguageSelected(context) && getCollectionCompleted(context);
    }

    public static void setIntroCompleted(Context context, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("introCompleted", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(PREF_KEY_INTRO_COMPLETED, value).apply();
    }

    public static boolean getIntroCompleted(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("introCompleted", MODE_PRIVATE);
        return sharedPreferences.getBoolean(PREF_KEY_INTRO_COMPLETED, false);
    }

    public static void setCollectionCompleted(Context context, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("collectionCompleted", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(PREF_KEY_COLLECTION_COMPLETED, value).apply();
    }

    public static boolean getCollectionCompleted(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("collectionCompleted", MODE_PRIVATE);
        return sharedPreferences.getBoolean(PREF_KEY_COLLECTION_COMPLETED, false);
    }

    public static void setDefaultHomeFlowCompleted(Context context, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("defaultHomeFlowCompleted", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(PREF_KEY_DEFAULT_HOME_FLOW_COMPLETED, value).apply();
    }

    public static boolean getDefaultHomeFlowCompleted(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("defaultHomeFlowCompleted", MODE_PRIVATE);
        return sharedPreferences.getBoolean(PREF_KEY_DEFAULT_HOME_FLOW_COMPLETED, false);
    }

    public static void setDefaultSmsFlowCompleted(Context context, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("defaultSmsFlowCompleted", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(PREF_KEY_DEFAULT_SMS_FLOW_COMPLETED, value).apply();
    }

    public static boolean getDefaultSmsFlowCompleted(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("defaultSmsFlowCompleted", MODE_PRIVATE);
        return sharedPreferences.getBoolean(PREF_KEY_DEFAULT_SMS_FLOW_COMPLETED, false);
    }

    public static void setAppLanguageNew(Context context, String value) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = prefsContext(context).getSharedPreferences("appLanguageNew", MODE_PRIVATE);
        sharedPreferences.edit().putString(PREF_KEY_APP_LANGUAGE_NEW, normalizeLanguageTag(value)).commit();
    }

    public static String getAppLanguageNew(Context context) {
        if (context == null) {
            return "en";
        }
        SharedPreferences sharedPreferences = prefsContext(context).getSharedPreferences("appLanguageNew", MODE_PRIVATE);
        return normalizeLanguageTag(sharedPreferences.getString(PREF_KEY_APP_LANGUAGE_NEW, "en"));
    }

    public static void setAppLanguageNameNew(Context context, String value) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = prefsContext(context).getSharedPreferences("appLanguageNameNew", MODE_PRIVATE);
        sharedPreferences.edit().putString(PREF_KEY_APP_LANGUAGE_NAME_NEW, value).commit();
    }

    public static String getAppLanguageNameNew(Context context) {
        if (context == null) {
            return "System Default (English)";
        }
        SharedPreferences sharedPreferences = prefsContext(context).getSharedPreferences("appLanguageNameNew", MODE_PRIVATE);
        return sharedPreferences.getString(PREF_KEY_APP_LANGUAGE_NAME_NEW, "System Default (English)");
    }

    public static void setDelaySending(Context context, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("delaySending", MODE_PRIVATE);
        sharedPreferences.edit().putString(PREF_KEY_DELAY_SENDING, value).apply();
    }

    public static String getDelaySending(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("delaySending", MODE_PRIVATE);
        return sharedPreferences.getString(PREF_KEY_DELAY_SENDING, context.getResources().getString(R.string.no_delay));
    }

    public static void setSignaturesName(Context context, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("signaturesName", MODE_PRIVATE);
        sharedPreferences.edit().putString(PREF_KEY_SIGNATURES_NAME, value).apply();
    }

    public static String getSignaturesName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("signaturesName", MODE_PRIVATE);
        return sharedPreferences.getString(PREF_KEY_SIGNATURES_NAME, "");
    }

    public static void setCategoryBar(Context context, Boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("categoryBar", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(PREF_KEY_CATEGORY_BAR, value).apply();
    }

    public static Boolean getCategoryBar(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("categoryBar", MODE_PRIVATE);
        return sharedPreferences.getBoolean(PREF_KEY_CATEGORY_BAR, true);
    }

    public static void setLabelVisibility(Context context, Boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("labelVisibility", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(PREF_KEY_LABEL_VISIBILITY, value).apply();
    }

    public static Boolean getLabelVisibility(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("labelVisibility", MODE_PRIVATE);
        return sharedPreferences.getBoolean(PREF_KEY_LABEL_VISIBILITY, true);
    }

    public static void setAppIconSize(Context context, int value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appIconSize", MODE_PRIVATE);
        sharedPreferences.edit().putInt(PREF_KEY_APP_ICON_SIZE, value).apply();
    }

    public static int getAppIconSize(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appIconSize", MODE_PRIVATE);
        return sharedPreferences.getInt(PREF_KEY_APP_ICON_SIZE, 60);
    }

    public static int getLauncherAppIconCornerRadiusPx(Context context) {
        return dpToPx(context, 16);
    }

    public static void setAppLabelSize(Context context, int value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appLabelSize", MODE_PRIVATE);
        sharedPreferences.edit().putInt(PREF_KEY_APP_LABEL_SIZE, value).apply();
    }

    public static int getAppLabelSize(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appLabelSize", MODE_PRIVATE);
        return sharedPreferences.getInt(PREF_KEY_APP_LABEL_SIZE, 12);
    }

    public static void setAppSerialize(Context context, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appSerialize", MODE_PRIVATE);
        sharedPreferences.edit().putString(PREF_KEY_APP_SERIALIZE, value).apply();
    }

    public static String getAppSerialize(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appSerialize", MODE_PRIVATE);
        return sharedPreferences.getString(PREF_KEY_APP_SERIALIZE, "a");
    }

    @Nullable
    public static List<ResolveInfo> queryLauncherActivities(PackageManager packageManager) {
        if (packageManager == null) {
            return null;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        try {
            return packageManager.queryIntentActivities(intent, 0);
        } catch (Exception first) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            try {
                return packageManager.queryIntentActivities(intent, 0);
            } catch (Exception retry) {
                retry.printStackTrace();
                return null;
            }
        }
    }

    public static boolean isAddressBlocked(Context context, String address) {
        return BlockHelper.isBlockedByAddress(context, address);
    }

    public static Set<String> getBlock(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("blockThreads", Context.MODE_PRIVATE);
        return new HashSet<>(sharedPreferences.getStringSet(PREF_KEY_BLOCK_THREADS, new HashSet<>()));
    }

    public static void setDraft(Context context, String address, String draft) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("drafts", Context.MODE_PRIVATE);
        if (draft == null || draft.trim().isEmpty()) {
            sharedPreferences.edit().remove(address).apply();
        } else {
            sharedPreferences.edit().putString(address, draft.trim()).apply();
        }
    }

    public static String getDraft(Context context, String address) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("drafts", Context.MODE_PRIVATE);
        return sharedPreferences.getString(address, "");
    }

    public static int getNotificationId(String number) {
        if (number == null || number.isEmpty()) {
            return 0;
        }
        String clean = number.replaceAll("[^a-zA-Z0-9]", "");
        if (clean.length() > 10 && clean.substring(clean.length() - 10).matches("\\d{10}")) {
            clean = clean.substring(clean.length() - 10);
        }
        return clean.hashCode();
    }

    public static SmsManager getSmsManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SmsManager smsManager = context.getSystemService(SmsManager.class);
            if (smsManager != null) {
                return smsManager;
            }
        }
        return SmsManager.getDefault();
    }

    public static void sendSms(Context context, String phoneNumber, String message) {
        try {
            SmsManager smsManager = getSmsManager(context);
            if (smsManager == null) {
                return;
            }
            ArrayList<String> parts = smsManager.divideMessage(message);
            if (parts != null && parts.size() > 1) {
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveryIntents = new ArrayList<>();
                for (int i = 0; i < parts.size(); i++) {
                    Intent sentIntent = new Intent("SMS_SENT");
                    sentIntents.add(PendingIntent.getBroadcast(context, (int) System.currentTimeMillis() + i, sentIntent, PendingIntent.FLAG_IMMUTABLE));
                    Intent deliveryIntent = new Intent("SMS_DELIVERED");
                    deliveryIntents.add(PendingIntent.getBroadcast(context, (int) System.currentTimeMillis() + i + 1000, deliveryIntent, PendingIntent.FLAG_IMMUTABLE));
                }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void clearActivityTransition(@Nullable Context context) {
        if (!(context instanceof Activity)) {
            return;
        }
        try {
            ((Activity) context).overridePendingTransition(0, 0);
        } catch (Exception ignored) {
        }
    }

    public static void openDialer(Context context, String phoneNumber) {
        if (context == null) {
            return;
        }

        String number = phoneNumber == null ? "" : phoneNumber.trim();
        Uri uri = number.isEmpty() ? Uri.parse("tel:") : Uri.parse("tel:" + Uri.encode(number));
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, uri);
        if (!(context instanceof Activity)) {
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null && !resolveInfo.activityInfo.exported) {
                openDialerChooser(context, dialIntent);
                return;
            }
            context.startActivity(dialIntent);
            clearActivityTransition(context);
        } catch (SecurityException | ActivityNotFoundException exception) {
            openDialerChooser(context, dialIntent);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void openDialerChooser(Context context, Intent dialIntent) {
        try {
            Intent chooser = Intent.createChooser(dialIntent, null);
            if (!(context instanceof Activity)) {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(chooser);
            clearActivityTransition(context);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static void trackScreen(Context context, String screenName) {
        try {
            Bundle bundle = new Bundle();
            bundle.putBoolean(screenName, true);
            FirebaseAnalytics.getInstance(context).logEvent(screenName, bundle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void trackScreenOnce(Context context, String screenName) {
        if (context == null || screenName == null || screenName.isEmpty()) {
            return;
        }
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences("analytics_events", MODE_PRIVATE);
        if (sharedPreferences.getBoolean(screenName, false)) {
            return;
        }
        sharedPreferences.edit().putBoolean(screenName, true).apply();
        trackScreen(context, screenName);
    }

    private static final String PREFS_DEFAULT_APP_FLOW = "defaultAppFlow";
    private static final String KEY_COMPLETING_DEFAULT_APP_SETUP = "completingDefaultAppSetup";
    private static final String KEY_AWAITING_DEFAULT_ROLE_RESULT = "awaitingDefaultRoleResult";
    private static volatile boolean afterDefaultFlowClaimed;

    public static void setCompletingDefaultAppSetup(Context context, boolean value) {
        if (context == null) {
            return;
        }
        afterDefaultFlowClaimed = false;
        SharedPreferences.Editor editor = context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).edit().putBoolean(KEY_COMPLETING_DEFAULT_APP_SETUP, value);
        if (!value) {
            editor.putBoolean(KEY_AWAITING_DEFAULT_ROLE_RESULT, false);
        }
        editor.apply();
    }

    public static void setAwaitingDefaultRoleResult(Context context, boolean value) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).edit().putBoolean(KEY_AWAITING_DEFAULT_ROLE_RESULT, value).apply();
    }

    public static boolean isAwaitingDefaultRoleResult(Context context) {
        if (context == null) {
            return false;
        }
        return context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).getBoolean(KEY_AWAITING_DEFAULT_ROLE_RESULT, false);
    }

    public static boolean isCompletingDefaultAppSetup(Context context) {
        if (context == null) {
            return false;
        }
        return context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).getBoolean(KEY_COMPLETING_DEFAULT_APP_SETUP, false);
    }

    public static synchronized boolean tryClaimAfterDefaultFlow(Context context) {
        if (!isCompletingDefaultAppSetup(context)) {
            return false;
        }
        if (afterDefaultFlowClaimed) {
            return false;
        }
        afterDefaultFlowClaimed = true;
        return true;
    }

    public static synchronized void releaseAfterDefaultFlowClaim() {
        afterDefaultFlowClaimed = false;
    }

    public static boolean isAfterDefaultFlowClaimed(Context context) {
        return afterDefaultFlowClaimed;
    }

    public static void navigateAfterDefaultAppSetup(Context context) {
        if (context == null) {
            return;
        }
        setCompletingDefaultAppSetup(context, false);
        if (isDefaultHomeApp(context)) {
            trackScreenOnce(context, "DEFAULT_HOME_APP_SET");
        } else {
            trackScreenOnce(context, "DEFAULT_HOME_APP_CANCEL");
        }
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                ScreenFlowNavigator.continueAfter(activity, AdPlacement.SCREEN_DEFAULT_HOME);
                return;
            }
        }
        ScreenFlowNavigator.continueAfter(context.getApplicationContext(), AdPlacement.SCREEN_DEFAULT_HOME);
    }

    public static boolean isWallpaperDark(Context context) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (wallpaperDarkCached != null && (now - wallpaperDarkCachedAtMs) < WALLPAPER_DARK_CACHE_TTL_MS) {
            return wallpaperDarkCached;
        }

        boolean isDark;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                WallpaperColors colors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                if (colors != null) {
                    int hints = colors.getColorHints();
                    isDark = (hints & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) == 0;
                    wallpaperDarkCached = isDark;
                    wallpaperDarkCachedAtMs = now;
                    return isDark;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        isDark = isWallpaperDarkFromBitmap(context);
        wallpaperDarkCached = isDark;
        wallpaperDarkCachedAtMs = now;
        return isDark;
    }

    public static WallpaperUiColors getWallpaperUiColors(Context context) {
        boolean isDark = isWallpaperDark(context);
        int bgColor = Color.parseColor(isDark ? "#101010" : "#F0F0F0");
        int textColor = Color.parseColor(isDark ? "#FFFFFF" : "#151515");
        int secondaryTextColor = Color.parseColor(isDark ? "#AEAEAE" : "#3B3B3B");
        return new WallpaperUiColors(isDark, bgColor, textColor, secondaryTextColor);
    }

    public static final class WallpaperUiColors {
        public final boolean isDark;
        public final int bgColor;
        public final int textColor;
        public final int secondaryTextColor;

        public WallpaperUiColors(boolean isDark, int bgColor, int textColor, int secondaryTextColor) {
            this.isDark = isDark;
            this.bgColor = bgColor;
            this.textColor = textColor;
            this.secondaryTextColor = secondaryTextColor;
        }
    }

    @Nullable
    public static Boolean getCachedWallpaperDark() {
        if (wallpaperDarkCached == null) {
            return null;
        }
        long now = android.os.SystemClock.elapsedRealtime();
        if ((now - wallpaperDarkCachedAtMs) >= WALLPAPER_DARK_CACHE_TTL_MS) {
            return null;
        }
        return wallpaperDarkCached;
    }

    private static boolean isWallpaperDarkFromBitmap(Context context) {
        try {
            Drawable drawable = WallpaperManager.getInstance(context).getDrawable();
            if (drawable == null) {
                return true;
            }

            Bitmap bitmap = drawableToBitmap(drawable);
            int sampleWidth = Math.min(bitmap.getWidth(), 64);
            int sampleHeight = Math.min(bitmap.getHeight(), 64);
            Bitmap sample = Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true);

            long totalLuminance = 0;
            int pixelCount = sampleWidth * sampleHeight;
            for (int x = 0; x < sampleWidth; x++) {
                for (int y = 0; y < sampleHeight; y++) {
                    int pixel = sample.getPixel(x, y);
                    totalLuminance += (long) (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel));
                }
            }

            if (sample != bitmap) {
                sample.recycle();
            }
            return ((double) totalLuminance / pixelCount) < 128;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        int targetWidth = 64;
        int targetHeight = 64;
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Nullable
    public static Bitmap decodeScaledBitmap(@Nullable InputStream input, int maxDimension) {
        if (input == null) {
            return null;
        }
        try {
            return scaleToMaxDimension(BitmapFactory.decodeStream(input), maxDimension);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public static Bitmap scaleToMaxDimension(@Nullable Bitmap bitmap, int maxDimension) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) {
            return bitmap;
        }

        int maxSide = Math.max(width, height);
        float scale = 1f;
        if (maxDimension > 0 && maxSide > maxDimension) {
            scale = (float) maxDimension / (float) maxSide;
        }

        long byteCount = (long) width * (long) height * 4L;
        long maxBytes = 32L * 1024L * 1024L;
        if (byteCount * scale * scale > maxBytes) {
            scale = Math.min(scale, (float) Math.sqrt(maxBytes / (double) byteCount));
        }
        if (scale >= 1f) {
            return bitmap;
        }

        int newWidth = Math.max(1, Math.round(width * scale));
        int newHeight = Math.max(1, Math.round(height * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        if (scaled != bitmap && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return scaled;
    }

    public static boolean isDefaultHomeApp(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = context.getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME);
            }
        }

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && resolveInfo.activityInfo != null && context.getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    public static boolean isRightSwipeTutorialShown(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("launcherFirstTutorial", MODE_PRIVATE);
        return prefs.getBoolean("swipe_right_done", false) || prefs.getBoolean("isRightSwipeTutorialCompleted", false);
    }

    public static void setRightSwipeTutorialShown(Context context, boolean shown) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences("launcherFirstTutorial", MODE_PRIVATE).edit().putBoolean("swipe_right_done", shown).putBoolean("isRightSwipeTutorialCompleted", shown).apply();
    }

    public static void clearWindowFocusSafely(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            View focused = activity.getCurrentFocus();
            if (focused != null) {
                focused.clearFocus();
            }
            clearWindowFocusSafely(activity.getWindow());
        } catch (Exception ignored) {
        }
    }

    public static void clearWindowFocusSafely(@Nullable Window window) {
        if (window == null) {
            return;
        }
        try {
            View decor = window.getDecorView();
            View focused = decor.findFocus();
            if (focused != null) {
                focused.clearFocus();
            }
            decor.clearFocus();
        } catch (Exception ignored) {
        }
    }

    @Nullable
    public static Intent createDefaultHomeRoleRequestIntent(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_HOME) || roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
            return null;
        }
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
    }

    public static boolean openDefaultHomeChooser(@NonNull Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean openHttpUrl(@NonNull Activity activity, @Nullable String url) {
        if (activity.isFinishing() || activity.isDestroyed() || url == null || url.trim().isEmpty()) {
            return false;
        }
        Uri uri = Uri.parse(url.trim());
        Intent viewIntent = new Intent(Intent.ACTION_VIEW, uri);
        viewIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        try {
            activity.startActivity(viewIntent);
            return true;
        } catch (Exception ignored) {
        }
        try {
            activity.startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, uri), null));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isDefaultSmsApp(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = context.getSystemService(RoleManager.class);
                return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_SMS);
            }
            return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void applyNavigationBarPadding(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 0, 0, navInsets.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}