package com.messages.smart.sms.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.messages.smart.sms.R;
import com.messages.smart.sms.adapters.IntroSwipePagerAdapter;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.ScreenFlowNavigator;
import com.messages.smart.sms.common.Utils;

public class IntroSwipeActivity extends AppCompatActivity {
    private ViewPager2 vpIntro;
    private IntroSwipePagerAdapter adapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Utils.wrapContext(newBase));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.applyStoredLocale(this);
        setContentView(R.layout.activity_intro_swipe);

        initViews();
    }

    private void initViews() {
        vpIntro = findViewById(R.id.vpIntro);
        adapter = new IntroSwipePagerAdapter(this, this::handleNextClick);
        vpIntro.setSaveEnabled(false);
        vpIntro.setAdapter(adapter);
        vpIntro.setOffscreenPageLimit(1);
    }

    private void handleNextClick(int position) {
        if (position < adapter.getItemCount() - 1) {
            vpIntro.setCurrentItem(position + 1, true);
            return;
        }

        Utils.setIntroCompleted(getApplicationContext(), true);
        ScreenFlowNavigator.continueAfter(this, AdPlacement.SCREEN_INTRO);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}