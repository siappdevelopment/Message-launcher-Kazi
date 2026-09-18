package com.messages.smart.sms.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.messages.smart.sms.R;
import com.messages.smart.sms.adapters.SerializeDropdownAdapter;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.Utils;

import java.util.ArrayList;
import java.util.List;

public class LauncherSettingsActivity extends AppCompatActivity {
    private AppCompatImageView ivBack, ivArrow, ivAppIcon1, ivAppIcon2, ivAppIcon3, ivAppIcon4;
    private LinearLayout llDropdown;
    private AppCompatTextView tvTitle, tvSerialize, tvSelectedSerialize, tvPreview, tvLabelVisibility, tvHideAppName, tvAppIconSize, tvAppLabelSize, tvAppName1, tvAppName2, tvAppName3, tvAppName4, tvAppIconSizePercentage, tvAppLabelSizePercentage;
    private SwitchCompat scLabelVisibility;
    private AppCompatSeekBar sbAppIconSize, sbAppLabelSize;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer, slNativeShimmer;
    public LinearLayout llBannerAd;
    private FrameLayout flNativeAd;

    private final List<String> arraySerialize = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_settings);

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
        llDropdown = findViewById(R.id.llDropdown);
        tvSerialize = findViewById(R.id.tvSerialize);
        tvSelectedSerialize = findViewById(R.id.tvSelectedSerialize);
        tvPreview = findViewById(R.id.tvPreview);
        tvLabelVisibility = findViewById(R.id.tvLabelVisibility);
        tvHideAppName = findViewById(R.id.tvHideAppName);
        tvAppIconSize = findViewById(R.id.tvAppIconSize);
        tvAppLabelSize = findViewById(R.id.tvAppLabelSize);
        ivArrow = findViewById(R.id.ivArrow);

        ivAppIcon1 = findViewById(R.id.ivAppIcon1);
        ivAppIcon2 = findViewById(R.id.ivAppIcon2);
        ivAppIcon3 = findViewById(R.id.ivAppIcon3);
        ivAppIcon4 = findViewById(R.id.ivAppIcon4);

        tvAppName1 = findViewById(R.id.tvAppName1);
        tvAppName2 = findViewById(R.id.tvAppName2);
        tvAppName3 = findViewById(R.id.tvAppName3);
        tvAppName4 = findViewById(R.id.tvAppName4);

        scLabelVisibility = findViewById(R.id.scLabelVisibility);

        sbAppIconSize = findViewById(R.id.sbAppIconSize);
        tvAppIconSizePercentage = findViewById(R.id.tvAppIconSizePercentage);

        sbAppLabelSize = findViewById(R.id.sbAppLabelSize);
        tvAppLabelSizePercentage = findViewById(R.id.tvAppLabelSizePercentage);

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
        if (!AdPlacement.getLauncherSettingAdShow()) {
            rlAdView.setVisibility(View.GONE);
        } else {
            rlAdView.setVisibility(View.VISIBLE);

            if ("banner".equalsIgnoreCase(AdPlacement.getLauncherSettingAdType())) {
                rlBannerAdView.setVisibility(View.VISIBLE);
                rlNativeAdView.setVisibility(View.GONE);
                AdPlacement.loadBannerAd(this, AdPlacement.getLauncherSettingBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd);
            } else {
                rlBannerAdView.setVisibility(View.GONE);
                rlNativeAdView.setVisibility(View.VISIBLE);
                AdPlacement.loadNativeAd(this, AdPlacement.getLauncherSettingNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "small");
            }
        }

        updateSerializeList();
        updateSelectedSerializeText();

        setApp();
        updateAppIconText();
        updateSeekbarProgress();

        ivBack.setOnClickListener(view -> {
            LauncherSettingsActivity.this.finish();
            overridePendingTransition(0, 0);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                LauncherSettingsActivity.this.finish();
                overridePendingTransition(0, 0);
            }
        });

        llDropdown.setOnClickListener(this::setupSerializeDropdownPopup);

        scLabelVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.setLabelVisibility(this, isChecked);

            updateLabelVisibility(Utils.getLabelVisibility(this));
        });

        sbAppIconSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Utils.setAppIconSize(getApplicationContext(), progress);

                int sizePx = Utils.dpToPx(getApplicationContext(), Utils.getAppIconSize(getApplicationContext()));
                AppCompatImageView[] imageViews = {ivAppIcon1, ivAppIcon2, ivAppIcon3, ivAppIcon4};
                for (AppCompatImageView imageView : imageViews) {
                    ViewGroup.LayoutParams params = imageView.getLayoutParams();
                    params.width = sizePx;
                    params.height = sizePx;
                    imageView.setLayoutParams(params);
                }

                tvAppIconSizePercentage.setText(String.valueOf(Utils.getAppIconSize(getApplicationContext())));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        sbAppLabelSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Utils.setAppLabelSize(getApplicationContext(), progress);

                updateTextSize();

                tvAppLabelSizePercentage.setText(String.valueOf(Utils.getAppLabelSize(getApplicationContext())));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @SuppressLint("InflateParams")
    private void setupSerializeDropdownPopup(View view) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.serialize_dropdown_popup, null);

        ListView lvSerialize = popupView.findViewById(R.id.lvSerialize);

        SerializeDropdownAdapter serializeDropdownAdapter = new SerializeDropdownAdapter(this, arraySerialize);
        lvSerialize.setAdapter(serializeDropdownAdapter);

        PopupWindow popupWindow = new PopupWindow(popupView, llDropdown.getWidth(), WindowManager.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setElevation(10f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        ivArrow.animate().rotation(270f).setDuration(180).start();

        popupWindow.setOnDismissListener(() -> ivArrow.animate().rotation(90f).setDuration(180).start());

        int y = (int) (10 * getResources().getDisplayMetrics().density);
        popupWindow.showAsDropDown(llDropdown, 0, y);

        lvSerialize.setOnItemClickListener((parent, viewInside, position, id) -> {
            if (position == 0) {
                Utils.setAppSerialize(getApplicationContext(), "a");
            } else if (position == 1) {
                Utils.setAppSerialize(getApplicationContext(), "d");
            } else if (position == 2) {
                Utils.setAppSerialize(getApplicationContext(), "i");
            } else {
                Utils.setAppSerialize(getApplicationContext(), "a");
            }

            updateSelectedSerializeText();

            popupWindow.dismiss();
        });
    }

    private void updateSerializeList() {
        arraySerialize.clear();
        arraySerialize.add(getString(R.string.by_name_asc_a_to_z));
        arraySerialize.add(getString(R.string.by_name_desc_z_to_a));
        arraySerialize.add(getString(R.string.by_installed_date));
    }

    private void updateSelectedSerializeText() {
        String serialize = Utils.getAppSerialize(getApplicationContext());
        if ("a".equals(serialize)) {
            tvSelectedSerialize.setText(getString(R.string.by_name_asc_a_to_z));
        } else if ("d".equals(serialize)) {
            tvSelectedSerialize.setText(getString(R.string.by_name_desc_z_to_a));
        } else if ("i".equals(serialize)) {
            tvSelectedSerialize.setText(getString(R.string.by_installed_date));
        } else {
            tvSelectedSerialize.setText(getString(R.string.by_name_asc_a_to_z));
        }
    }

    private void setApp() {
        PackageManager packageManager = getPackageManager();

        TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
        if (telecomManager != null) {
            String stringCall = telecomManager.getDefaultDialerPackage();
            if (stringCall != null) {
                ApplicationInfo applicationInfoCall;
                try {
                    applicationInfoCall = packageManager.getApplicationInfo(stringCall, 0);
                    Drawable appIcon1 = packageManager.getApplicationIcon(stringCall);
                    String appName1 = packageManager.getApplicationLabel(applicationInfoCall).toString();
                    ivAppIcon1.setImageDrawable(appIcon1);
                    tvAppName1.setText(appName1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            String packageNameMessage = getPackageName();
            Drawable appIcon2 = packageManager.getApplicationIcon(packageNameMessage);
            ivAppIcon2.setImageDrawable(appIcon2);
            tvAppName2.setText(R.string.app_name);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intentCamera = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        ResolveInfo resolveInfo = packageManager.resolveActivity(intentCamera, 0);
        if (resolveInfo != null) {
            Drawable appIcon3 = resolveInfo.loadIcon(packageManager);
            String appName3 = (String) resolveInfo.loadLabel(packageManager);
            ivAppIcon3.setImageDrawable(appIcon3);
            tvAppName3.setText(appName3);
        }

        try {
            String packageNameSettings = "com.android.settings";
            ApplicationInfo applicationInfoSettings = packageManager.getApplicationInfo(packageNameSettings, 0);
            Drawable appIcon4 = packageManager.getApplicationIcon(packageNameSettings);
            String appName4 = packageManager.getApplicationLabel(applicationInfoSettings).toString();
            ivAppIcon4.setImageDrawable(appIcon4);
            tvAppName4.setText(appName4);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAppIconText() {
        int sizePx = Utils.dpToPx(this, Utils.getAppIconSize(this));

        AppCompatImageView[] imageViews = {ivAppIcon1, ivAppIcon2, ivAppIcon3, ivAppIcon4};
        for (AppCompatImageView imageView : imageViews) {
            ViewGroup.LayoutParams params = imageView.getLayoutParams();
            params.width = sizePx;
            params.height = sizePx;
            imageView.setLayoutParams(params);
        }

        updateLabelVisibility(Utils.getLabelVisibility(this));

        updateTextSize();
    }

    private void updateLabelVisibility(boolean isVisible) {
        int visibility = isVisible ? VISIBLE : GONE;
        tvAppName1.setVisibility(visibility);
        tvAppName2.setVisibility(visibility);
        tvAppName3.setVisibility(visibility);
        tvAppName4.setVisibility(visibility);
    }

    private void updateTextSize() {
        AppCompatTextView[] textViews = {tvAppName1, tvAppName2, tvAppName3, tvAppName4};
        for (AppCompatTextView textView : textViews) {
            textView.setTextSize(Utils.getAppLabelSize(this));
        }
    }

    private void updateSeekbarProgress() {
        scLabelVisibility.setChecked(Utils.getLabelVisibility(this));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sbAppIconSize.setMin(40);
        }
        sbAppIconSize.setMax(80);
        sbAppIconSize.setProgress(Utils.getAppIconSize(this));
        tvAppIconSizePercentage.setText(String.valueOf(Utils.getAppIconSize(getApplicationContext())));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sbAppLabelSize.setMin(10);
        }
        sbAppLabelSize.setMax(16);
        sbAppLabelSize.setProgress(Utils.getAppLabelSize(this));
        tvAppLabelSizePercentage.setText(String.valueOf(Utils.getAppLabelSize(getApplicationContext())));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTitle != null) {
            tvTitle.setText(getString(R.string.launcher_settings));
            tvSerialize.setText(getString(R.string.serialize));
            tvPreview.setText(getString(R.string.preview));
            tvLabelVisibility.setText(getString(R.string.label_visibility));
            tvHideAppName.setText(getString(R.string.hide_app_name));
            tvAppIconSize.setText(getString(R.string.app_icon_size));
            tvAppLabelSize.setText(getString(R.string.app_label_size));
            updateSerializeList();
            updateSelectedSerializeText();
            setApp();
            updateSeekbarProgress();
        }
    }
}