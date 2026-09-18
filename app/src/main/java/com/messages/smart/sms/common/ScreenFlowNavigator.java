package com.messages.smart.sms.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.messages.smart.sms.activities.CollectionActivity;
import com.messages.smart.sms.activities.DefaultAppActivity;
import com.messages.smart.sms.activities.DefaultSMSActivity;
import com.messages.smart.sms.activities.IntroSwipeActivity;
import com.messages.smart.sms.activities.AppLanguageActivity;
import com.messages.smart.sms.activities.LauncherHomeActivity;

import java.util.List;

public final class ScreenFlowNavigator {
    private static final String TAG = "ScreenFlowNavigator";
    public static final String EXTRA_LANGUAGE_FLOW_STARTING = "extra_language_flow_starting";
    private static final long FINAL_NAVIGATION_DEBOUNCE_MS = 1000L;
    private static long lastFinalNavigationElapsedMs;

    private ScreenFlowNavigator() {
    }

    public static void openNext(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (AdPlacement.getShowScreenFlow().isEmpty()) {
            AdPlacement.restoreShowScreenFlow(activity);
        }
        openFromIndex(activity, 0);
    }

    public static void continueAfter(Activity activity, String completedScreen) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        markScreenCompleted(activity, completedScreen);

        List<String> flow = AdPlacement.getShowScreenFlow();
        int completedIndex = indexOfScreen(flow, completedScreen);
        int startIndex = completedIndex >= 0 ? completedIndex + 1 : 0;
        openFromIndex(activity, startIndex);
    }

    public static void continueAfter(Context context, String completedScreen) {
        if (context instanceof Activity) {
            continueAfter((Activity) context, completedScreen);
            return;
        }
        if (context == null) {
            return;
        }
        markScreenCompleted(context, completedScreen);
        List<String> flow = AdPlacement.getShowScreenFlow();
        int completedIndex = indexOfScreen(flow, completedScreen);
        int startIndex = completedIndex >= 0 ? completedIndex + 1 : 0;
        String next = findNextIncomplete(context, flow, startIndex);
        if (next == null) {
            openFinalDestination(context);
            return;
        }
        launchScreen(context, next);
    }

    private static void openFromIndex(Activity activity, int startIndex) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        List<String> flow = AdPlacement.getShowScreenFlow();
        String next = findNextIncomplete(activity, flow, startIndex);
        if (next == null) {
            openMain(activity);
            return;
        }
        openScreen(activity, next);
    }

    private static String findNextIncomplete(Context context, List<String> flow, int startIndex) {
        if (flow == null || flow.isEmpty()) {
            return null;
        }
        int start = Math.max(0, startIndex);
        for (int i = start; i < flow.size(); i++) {
            String screen = flow.get(i);
            if (!isSupportedScreen(screen)) {
                continue;
            }
            if (!isScreenCompleted(context, screen)) {
                return screen;
            }
        }
        return null;
    }

    public static boolean isScreenCompleted(Context context, String screen) {
        if (context == null || screen == null) {
            return true;
        }
        if (AdPlacement.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
            return Utils.getLanguageFlowCompleted(context);
        }
        if (AdPlacement.SCREEN_COLLECTION.equalsIgnoreCase(screen)) {
            return Utils.getCollectionCompleted(context);
        }
        if (AdPlacement.SCREEN_DEFAULT_SMS.equalsIgnoreCase(screen)) {
            return Utils.getDefaultSmsFlowCompleted(context) || Utils.isDefaultSmsApp(context);
        }
        if (AdPlacement.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen)) {
            return Utils.getDefaultHomeFlowCompleted(context) || Utils.isDefaultHomeApp(context);
        }
        if (AdPlacement.SCREEN_INTRO.equalsIgnoreCase(screen)) {
            return Utils.getIntroCompleted(context);
        }
        return true;
    }

    private static void markScreenCompleted(Context context, String screen) {
        if (context == null || screen == null) {
            return;
        }
        if (AdPlacement.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
            Utils.setAppLanguageSelected(context, true);
            Utils.setLanguageFlowCompleted(context, true);
        } else if (AdPlacement.SCREEN_COLLECTION.equalsIgnoreCase(screen)) {
            Utils.setCollectionCompleted(context, true);
        } else if (AdPlacement.SCREEN_DEFAULT_SMS.equalsIgnoreCase(screen)) {
            Utils.setDefaultSmsFlowCompleted(context, true);
        } else if (AdPlacement.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen)) {
            Utils.setDefaultHomeFlowCompleted(context, true);
        } else if (AdPlacement.SCREEN_INTRO.equalsIgnoreCase(screen)) {
            Utils.setIntroCompleted(context, true);
        }
    }

    private static boolean isSupportedScreen(String screen) {
        return AdPlacement.SCREEN_LANGUAGE.equalsIgnoreCase(screen) || AdPlacement.SCREEN_COLLECTION.equalsIgnoreCase(screen) || AdPlacement.SCREEN_DEFAULT_SMS.equalsIgnoreCase(screen) || AdPlacement.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen) || AdPlacement.SCREEN_INTRO.equalsIgnoreCase(screen);
    }

    private static int indexOfScreen(List<String> flow, String screen) {
        if (flow == null || screen == null) {
            return -1;
        }
        for (int i = 0; i < flow.size(); i++) {
            if (screen.equalsIgnoreCase(flow.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static void openScreen(Activity activity, String screen) {
        if (AdPlacement.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
            Utils.isAppLanguageStarting = true;
        } else if (AdPlacement.SCREEN_INTRO.equalsIgnoreCase(screen)) {
            if (!"swipe".equalsIgnoreCase(AdPlacement.getIntroType())) {
                IntroNavigation.openIntroButtonFlow(activity);
                return;
            }
        }

        Intent intent = buildScreenIntent(activity, screen);
        if (intent == null) {
            openMain(activity);
            return;
        }
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.finish();
                activity.overridePendingTransition(0, 0);
            }
        });
    }

    private static void launchScreen(Context context, String screen) {
        if (AdPlacement.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
            Utils.isAppLanguageStarting = true;
        }
        Intent intent = buildScreenIntent(context, screen);
        if (intent == null) {
            openFinalDestination(context);
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        Utils.clearActivityTransition(context);
    }

    private static Intent buildScreenIntent(Context context, String screen) {
        if (AdPlacement.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
            Intent intent = new Intent(context, AppLanguageActivity.class);
            intent.putExtra(EXTRA_LANGUAGE_FLOW_STARTING, true);
            return intent;
        }
        if (AdPlacement.SCREEN_COLLECTION.equalsIgnoreCase(screen)) {
            return new Intent(context, CollectionActivity.class);
        }
        if (AdPlacement.SCREEN_DEFAULT_SMS.equalsIgnoreCase(screen)) {
            return new Intent(context, DefaultSMSActivity.class);
        }
        if (AdPlacement.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen)) {
            return new Intent(context, DefaultAppActivity.class);
        }
        if (AdPlacement.SCREEN_INTRO.equalsIgnoreCase(screen)) {
            return new Intent(context, IntroSwipeActivity.class);
        }
        return null;
    }

    private static void openMain(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (!openFinalDestination(activity)) {
            return;
        }
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }

    private static boolean openFinalDestination(Context context) {
        if (context == null) {
            return false;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastFinalNavigationElapsedMs < FINAL_NAVIGATION_DEBOUNCE_MS) {
            return false;
        }
        lastFinalNavigationElapsedMs = now;

        Utils.isFromContacts = false;
        Intent intent = buildFinalDestinationIntent(context);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        Utils.clearActivityTransition(context);
        return true;
    }

    private static Intent buildFinalDestinationIntent(Context context) {
        Class<?> destination = LauncherHomeActivity.class;
        return new Intent(context, destination);
    }
}