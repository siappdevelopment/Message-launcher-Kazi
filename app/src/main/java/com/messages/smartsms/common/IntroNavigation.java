package com.messages.smartsms.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.messages.smartsms.R;
import com.messages.smartsms.activities.Intro1Activity;
import com.messages.smartsms.activities.Intro2Activity;
import com.messages.smartsms.activities.Intro3Activity;
import com.messages.smartsms.activities.Intro4Activity;

public class IntroNavigation {
    private static final int MAX_INTRO_BUTTON_SCREENS = 4;

    public static int getEffectiveIntroButtonScreenCount() {
        int count = AdPlacement.getIntroButtonScreen();
        if (count <= 0) {
            return 0;
        }
        return Math.min(count, MAX_INTRO_BUTTON_SCREENS);
    }

    public static void setupIntroButtonIndicators(Context context, LinearLayout llIndicator, int activeScreen) {
        int pageCount = getEffectiveIntroButtonScreenCount();
        llIndicator.removeAllViews();

        if (pageCount <= 0) {
            llIndicator.setVisibility(View.GONE);
            return;
        }

        llIndicator.setVisibility(View.VISIBLE);
        int activeWidth = Utils.dpToPx(context, 18);
        int inactiveWidth = Utils.dpToPx(context, 6);
        int dotHeight = Utils.dpToPx(context, 6);
        int margin = Utils.dpToPx(context, 2);

        for (int i = 1; i <= pageCount; i++) {
            AppCompatImageView dot = new AppCompatImageView(context);
            boolean isActive = i == activeScreen;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(isActive ? activeWidth : inactiveWidth, dotHeight);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setAdjustViewBounds(true);
            dot.setImageResource(isActive ? R.drawable.custom_button_round : R.drawable.custom_circle);
            dot.setColorFilter(ContextCompat.getColor(context, isActive ? R.color.primary : R.color.gray));
            llIndicator.addView(dot);
        }
    }

    public static void openIntroButtonFlow(Activity activity) {
        if (getEffectiveIntroButtonScreenCount() == 0) {
            completeIntroAndOpenDefaultApp(activity);
            return;
        }
        activity.startActivity(new Intent(activity, Intro1Activity.class));
        activity.overridePendingTransition(0, 0);
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }

    public static void goToNextIntroButtonScreen(Activity activity, int currentScreen) {
        if (currentScreen >= getEffectiveIntroButtonScreenCount()) {
            completeIntroAndOpenDefaultApp(activity);
            return;
        }

        Class<?> nextClass;
        switch (currentScreen + 1) {
            case 2:
                nextClass = Intro2Activity.class;
                break;
            case 3:
                nextClass = Intro3Activity.class;
                break;
            case 4:
                nextClass = Intro4Activity.class;
                break;
            default:
                completeIntroAndOpenDefaultApp(activity);
                return;
        }

        activity.startActivity(new Intent(activity, nextClass));
        activity.overridePendingTransition(0, 0);
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }

    public static void completeIntroAndOpenDefaultApp(Activity activity) {
        Utils.setIntroCompleted(activity.getApplicationContext(), true);
        ScreenFlowNavigator.continueAfter(activity, AdPlacement.SCREEN_INTRO);
    }
}