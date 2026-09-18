package com.messages.smart.sms.activities;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.messages.smart.sms.R;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.fragments.MainContainerFragment;

public class MainActivity extends AppCompatActivity {
    private static final String STATE_CURRENT_TAB = "state_current_tab";
    private static final String TAG_MAIN_CONTAINER = "MAIN_CONTAINER";

    public static String currentTab;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Utils.wrapContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(getColor(R.color.white));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getString(STATE_CURRENT_TAB, "MessagesFragment");
        } else if (Utils.isFromContacts) {
            currentTab = "ContactsFragment";
        }

        if (currentTab == null || currentTab.isEmpty()) {
            currentTab = "MessagesFragment";
        }

        Fragment existing = getSupportFragmentManager().findFragmentByTag(TAG_MAIN_CONTAINER);
        if (existing == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.flMainContainer, new MainContainerFragment(), TAG_MAIN_CONTAINER).commitNow();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Utils.isLocaleChangedPending()) {
            Utils.applySavedLocaleToActivity(this);
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_MAIN_CONTAINER);
            if (fragment instanceof MainContainerFragment && fragment.isAdded()) {
                ((MainContainerFragment) fragment).reapplyLocalizedTexts();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CURRENT_TAB, currentTab != null ? currentTab : "MessagesFragment");
    }

    public void switchToContactsTab() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_MAIN_CONTAINER);
        if (fragment instanceof MainContainerFragment) {
            ((MainContainerFragment) fragment).switchToContactsTab();
        } else {
            currentTab = "ContactsFragment";
        }
    }
}