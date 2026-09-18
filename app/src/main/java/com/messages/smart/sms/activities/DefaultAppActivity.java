package com.messages.smart.sms.activities;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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

import com.facebook.shimmer.ShimmerFrameLayout;
import com.messages.smart.sms.R;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.Utils;

public class DefaultAppActivity extends AppCompatActivity {
    private AppCompatTextView tvTitle, tvDescription, tvSetAsDefault;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout btnSetDefault, slBannerShimmer, slNativeShimmer;
    public LinearLayout llBannerAd;
    private FrameLayout flNativeAd;

    private boolean navigatedAway;
    private boolean handlingAfterDefaultFlow;
    private boolean continuePosted;

    private final ActivityResultLauncher<Intent> homeRoleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> scheduleContinueAfterDefaultRole());

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Utils.wrapContext(newBase));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.applyStoredLocale(this);
        setContentView(R.layout.activity_default_app);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        initViews();
        AdPlacement.preloadAfterDefaultAd(this);
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
            if (handlingAfterDefaultFlow || navigatedAway) {
                return;
            }
            handlingAfterDefaultFlow = false;
            Utils.setCompletingDefaultAppSetup(this, true);

            if (Utils.isDefaultHomeApp(this)) {
                Utils.setAwaitingDefaultRoleResult(this, false);
                scheduleContinueAfterDefaultRole();
            } else {
                requestDefaultHomeApp();
            }
        });
    }

    private void scheduleContinueAfterDefaultRole() {
        if (navigatedAway || isFinishing() || isDestroyed() || continuePosted) {
            return;
        }
        if (!Utils.isCompletingDefaultAppSetup(this)) {
            return;
        }
        continuePosted = true;
        View decor = getWindow() != null ? getWindow().getDecorView() : null;
        Runnable action = () -> {
            continuePosted = false;
            runAfterDefaultFlowIfNeeded();
        };
        if (decor != null) {
            decor.post(action);
        } else {
            action.run();
        }
    }

    private void runAfterDefaultFlowIfNeeded() {
        if (navigatedAway || isFinishing() || isDestroyed() || handlingAfterDefaultFlow) {
            return;
        }
        if (!Utils.isCompletingDefaultAppSetup(this)) {
            return;
        }
        if (Utils.isDefaultHomeApp(this) && !hasWindowFocus()) {
            return;
        }
        if (!Utils.tryClaimAfterDefaultFlow(this)) {
            if (Utils.isDefaultHomeApp(this)) {
                return;
            }
            Utils.releaseAfterDefaultFlowClaim();
            if (!Utils.tryClaimAfterDefaultFlow(this)) {
                return;
            }
        }

        Utils.setAwaitingDefaultRoleResult(this, false);
        handlingAfterDefaultFlow = true;
        showAfterDefaultAdThen(() -> {
            handlingAfterDefaultFlow = false;
            goNextAfterDefault();
        });
    }

    private void showAfterDefaultAdThen(Runnable onComplete) {
        if (!AdPlacement.getAfterDefaultAdShow()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        AdPlacement.loadAfterDefaultAd(this, () -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void goNextAfterDefault() {
        if (navigatedAway) {
            return;
        }
        navigatedAway = true;
        handlingAfterDefaultFlow = false;
        Utils.navigateAfterDefaultAppSetup(this);
    }

    private void requestDefaultHomeApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        scheduleContinueAfterDefaultRole();
                        return;
                    }
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        Utils.setAwaitingDefaultRoleResult(this, true);
                        homeRoleLauncher.launch(intent);
                        return;
                    }
                }
            }
            openHomeAppChooser();
        } catch (Exception e) {
            e.printStackTrace();
            openHomeSettingsFallback();
        }
    }

    private void openHomeAppChooser() {
        try {
            Intent selector = new Intent(Intent.ACTION_MAIN);
            selector.addCategory(Intent.CATEGORY_HOME);
            if (selector.resolveActivity(getPackageManager()) != null) {
                Utils.setAwaitingDefaultRoleResult(this, true);
                startActivity(selector);
                overridePendingTransition(0, 0);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Utils.setAwaitingDefaultRoleResult(this, false);
        scheduleContinueAfterDefaultRole();
    }

    private void openHomeSettingsFallback() {
        try {
            Utils.setAwaitingDefaultRoleResult(this, true);
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            overridePendingTransition(0, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Utils.setAwaitingDefaultRoleResult(this, false);
            scheduleContinueAfterDefaultRole();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTitle != null) {
            tvTitle.setText(getString(R.string.set_default_app));
            tvDescription.setText(getString(R.string.set_default_app_desc));
            tvSetAsDefault.setText(getString(R.string.set_as_default_app));
        }

        if (Utils.isCompletingDefaultAppSetup(this) && Utils.isAwaitingDefaultRoleResult(this)) {
            scheduleContinueAfterDefaultRole();
        }
    }

    @Override
    protected void onDestroy() {
        if (handlingAfterDefaultFlow && !navigatedAway && Utils.isCompletingDefaultAppSetup(this)) {
            Utils.releaseAfterDefaultFlowClaim();
            handlingAfterDefaultFlow = false;
        }
        super.onDestroy();
    }
}