package com.messages.smart.sms.activities;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.messages.smart.sms.R;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.Utils;

public class AboutMessagesActivity extends AppCompatActivity {
    private AppCompatImageView ivBack;
    private AppCompatTextView tvTitle, tvAppName, tvVersion, tvPrivacyPolicy, tvTermsConditions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_messages);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        initViews();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvAppName = findViewById(R.id.tvAppName);
        tvVersion = findViewById(R.id.tvVersion);
        tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);
        tvTermsConditions = findViewById(R.id.tvTermsConditions);

        clickEvents();
    }

    private void clickEvents() {
        ivBack.setOnClickListener(view -> {
            AboutMessagesActivity.this.finish();
            overridePendingTransition(0, 0);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                AboutMessagesActivity.this.finish();
                overridePendingTransition(0, 0);
            }
        });

        tvPrivacyPolicy.setOnClickListener(view -> {
            String url = AdPlacement.getPrivacyPolicy();
            if (url != null && !url.isEmpty()) {
                if (Utils.openHttpUrl(this, url)) {
                    overridePendingTransition(0, 0);
                } else {
                    Toast.makeText(this, "Privacy Policy URL Not Found !", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Privacy Policy URL Not Found !", Toast.LENGTH_SHORT).show();
            }
        });

        tvTermsConditions.setOnClickListener(view -> {
            String url = AdPlacement.getTermsConditions();
            if (url != null && !url.isEmpty()) {
                if (Utils.openHttpUrl(this, url)) {
                    overridePendingTransition(0, 0);
                } else {
                    Toast.makeText(this, "Terms & Conditions URL Not Found !", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Terms & Conditions URL Not Found !", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        if (tvTitle != null) {
            tvTitle.setText(getString(R.string.about_messages));
            tvAppName.setText(getString(R.string.app_name));
            try {
                PackageManager packageManager = getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
                int versionCode = packageInfo.versionCode;
                tvVersion.setText(getString(R.string.version) + " " + versionCode);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            tvPrivacyPolicy.setText(getString(R.string.privacy_policy));
            tvTermsConditions.setText(getString(R.string.terms_conditions));
        }
    }
}