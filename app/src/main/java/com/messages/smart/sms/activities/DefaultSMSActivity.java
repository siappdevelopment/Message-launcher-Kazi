package com.messages.smart.sms.activities;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.messages.smart.sms.R;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.ScreenFlowNavigator;
import com.messages.smart.sms.common.Utils;

import java.util.ArrayList;
import java.util.Map;

public class DefaultSMSActivity extends AppCompatActivity {
    private AppCompatTextView tvTitle, tvDescription, tvSetAsDefault;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout btnSetDefault, slBannerShimmer, slNativeShimmer;
    public LinearLayout llBannerAd;
    private FrameLayout flNativeAd;
    private boolean navigatedAway;
    private boolean waitingForDefaultResult;
    private boolean handlingDefaultResult;

    private final ActivityResultLauncher<Intent> smsRoleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        waitingForDefaultResult = false;
        handleAfterDefaultSmsPrompt();
    });

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        trackGrantedPermissions(result);
        goNextAfterDefaultSms();
    });

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Utils.wrapContext(newBase));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.applyStoredLocale(this);
        setContentView(R.layout.activity_default_sms);

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
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        btnSetDefault = findViewById(R.id.btnSetDefault);
        tvSetAsDefault = findViewById(R.id.tvSetAsDefault);
        rlAdView = findViewById(R.id.rlAdView);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);

        clickEvents();
    }

    private void clickEvents() {
        if (!AdPlacement.getDefaultAdShow()) {
            rlAdView.setVisibility(View.GONE);
        } else {
            rlAdView.setVisibility(View.VISIBLE);

            if ("banner".equalsIgnoreCase(AdPlacement.getDefaultAdType())) {
                rlBannerAdView.setVisibility(View.VISIBLE);
                rlNativeAdView.setVisibility(View.GONE);
                AdPlacement.loadBannerAd(this, AdPlacement.getDefaultBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd);
            } else {
                rlBannerAdView.setVisibility(View.GONE);
                rlNativeAdView.setVisibility(View.VISIBLE);
                AdPlacement.loadNativeAd(this, AdPlacement.getDefaultNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "small");
            }
        }

        btnSetDefault.setOnClickListener(view -> {
            if (Utils.isDefaultSmsApp(this)) {
                goNextAfterDefaultSms();
            } else {
                waitingForDefaultResult = true;
                requestDefaultSmsApp();
            }
        });
    }

    private void handleAfterDefaultSmsPrompt() {
        if (navigatedAway || isFinishing() || handlingDefaultResult) {
            return;
        }
        handlingDefaultResult = true;
        waitingForDefaultResult = false;

        if (Utils.isDefaultSmsApp(this)) {
            goNextAfterDefaultSms();
            return;
        }

        requestMissingPermissionsThenContinue();
    }

    private void requestMissingPermissionsThenContinue() {
        ArrayList<String> missingPermissions = buildMissingPermissions();
        if (missingPermissions.isEmpty()) {
            goNextAfterDefaultSms();
            return;
        }
        permissionLauncher.launch(missingPermissions.toArray(new String[0]));
    }

    private ArrayList<String> buildMissingPermissions() {
        ArrayList<String> permissionList = new ArrayList<>();
        addIfNotGranted(permissionList, Manifest.permission.READ_SMS);
        addIfNotGranted(permissionList, Manifest.permission.RECEIVE_SMS);
        addIfNotGranted(permissionList, Manifest.permission.READ_CONTACTS);
        addIfNotGranted(permissionList, Manifest.permission.READ_PHONE_STATE);
        addIfNotGranted(permissionList, Manifest.permission.CALL_PHONE);
        return permissionList;
    }

    private void addIfNotGranted(ArrayList<String> permissionList, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(permission);
        }
    }

    private void trackGrantedPermissions(Map<String, Boolean> result) {
        if (result == null || result.isEmpty()) {
            return;
        }
        if (Boolean.TRUE.equals(result.get(Manifest.permission.READ_SMS))) {
            Utils.trackScreenOnce(this, "READ_SMS");
        }
        if (Boolean.TRUE.equals(result.get(Manifest.permission.RECEIVE_SMS))) {
            Utils.trackScreenOnce(this, "RECEIVE_SMS");
        }
        if (Boolean.TRUE.equals(result.get(Manifest.permission.READ_CONTACTS))) {
            Utils.trackScreenOnce(this, "READ_CONTACTS");
        }
        if (Boolean.TRUE.equals(result.get(Manifest.permission.READ_PHONE_STATE))) {
            Utils.trackScreenOnce(this, "READ_PHONE_STATE");
        }
        if (Boolean.TRUE.equals(result.get(Manifest.permission.CALL_PHONE))) {
            Utils.trackScreenOnce(this, "CALL_PHONE");
        }
    }

    private void goNextAfterDefaultSms() {
        if (navigatedAway || isFinishing()) {
            return;
        }
        navigatedAway = true;
        waitingForDefaultResult = false;
        if (Utils.isDefaultSmsApp(this)) {
            Utils.trackScreenOnce(this, "DEFAULT_SMS_APP_SET");
        }
        ScreenFlowNavigator.continueAfter(this, AdPlacement.SCREEN_DEFAULT_SMS);
    }

    private void requestDefaultSmsApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                        goNextAfterDefaultSms();
                        return;
                    }
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        smsRoleLauncher.launch(intent);
                        return;
                    }
                }
            }
            openChangeDefaultSmsFallback();
        } catch (Exception e) {
            e.printStackTrace();
            waitingForDefaultResult = false;
            goNextAfterDefaultSms();
        }
    }

    private void openChangeDefaultSmsFallback() {
        try {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            if (intent.resolveActivity(getPackageManager()) != null) {
                waitingForDefaultResult = true;
                smsRoleLauncher.launch(intent);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        waitingForDefaultResult = false;
        goNextAfterDefaultSms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTitle != null) {
            tvTitle.setText(getString(R.string.set_default_sms));
            tvDescription.setText(getString(R.string.set_default_sms_desc));
            tvSetAsDefault.setText(getString(R.string.set_as_default_sms));
        }

        if (waitingForDefaultResult) {
            waitingForDefaultResult = false;
            handleAfterDefaultSmsPrompt();
        }
    }
}