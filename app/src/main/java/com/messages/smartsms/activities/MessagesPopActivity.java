package com.messages.smartsms.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.messages.smartsms.R;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;

public class MessagesPopActivity extends AppCompatActivity {
    private AppCompatImageView ivImage;
    private AppCompatTextView tvName, tvUserName, tvUserNumber, tvUserMessage;
    private LinearLayout llMessageSend;
    private AppCompatEditText etMessage;
    private RelativeLayout rlSend;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer, slNativeShimmer;
    public LinearLayout llBannerAd;
    private FrameLayout flNativeAd;
    private boolean adsLoaded;
    private ActivityResultLauncher<Intent> defaultSmsLauncher;
    private boolean pendingSendMessage = false;
    private String currentNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        defaultSmsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (pendingSendMessage && isDefaultSmsApp()) {
                pendingSendMessage = false;
                sendMessage();
            } else {
                pendingSendMessage = false;
            }
        });

        setContentView(R.layout.activity_messages_pop);

        initViews();
        ensureAdsReady();
    }

    private void initViews() {
        ivImage = findViewById(R.id.ivImage);
        tvName = findViewById(R.id.tvName);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserNumber = findViewById(R.id.tvUserNumber);
        AppCompatImageView ivPopClose = findViewById(R.id.ivPopClose);
        tvUserMessage = findViewById(R.id.tvUserMessage);
        llMessageSend = findViewById(R.id.llMessageSend);
        etMessage = findViewById(R.id.etMessage);
        rlSend = findViewById(R.id.rlSend);

        rlAdView = findViewById(R.id.rlAdView);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);

        ivPopClose.setOnClickListener(view -> {
            finish();
            overridePendingTransition(0, 0);
        });
        processIntent(getIntent());
    }

    private void ensureAdsReady() {
        AdPlacement.gatherConsent(this, error -> {
            if (!AdPlacement.canRequestAds(this)) {
                return;
            }

            if (AdPlacement.isRemoteConfigApplied()) {
                loadMessageAds();
                return;
            }

            if (AdPlacement.restoreMessageAdConfig(this)) {
                loadMessageAds();
                return;
            }

            fetchMessageAdConfig();
        });
    }

    private void fetchMessageAdConfig() {
        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception ignored) {
        }

        int version = getAppVersion(this);
        String key = "Messages_" + version;

        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build();
        firebaseRemoteConfig.setConfigSettingsAsync(settings);
        firebaseRemoteConfig.setDefaultsAsync(R.xml.default_config);
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                try {
                    JSONArray jsonArray = new JSONArray(firebaseRemoteConfig.getString(key));
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    String nativeAdLabelColor = jsonObject.getString("Native_Ad_Label_Color");
                    String nativeAdButtonColor = jsonObject.getString("Native_Ad_Button_Color");

                    JSONObject screen = jsonObject.getJSONObject("Screen");
                    JSONObject messageScreen = screen.getJSONObject("MessageScreen");
                    boolean messageAdShow = messageScreen.getBoolean("Message_Ad_Show");
                    String messageAdType = messageScreen.getString("Message_Ad_Type");
                    String messageBannerId = messageScreen.getString("Message_Banner_Id");
                    String messageNativeId = messageScreen.getString("Message_Native_Id");

                    AdPlacement.setNativeAdLabelColor(nativeAdLabelColor);
                    AdPlacement.setNativeAdButtonColor(nativeAdButtonColor);
                    AdPlacement.setMessageAdShow(messageAdShow);
                    AdPlacement.setMessageAdType(messageAdType);
                    AdPlacement.setMessageBannerId(messageBannerId);
                    AdPlacement.setMessageNativeId(messageNativeId);
                    AdPlacement.setRemoteConfigApplied(true);
                    AdPlacement.cacheMessageAdConfig(MessagesPopActivity.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            loadMessageAds();
        });
    }

    private int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    private void loadMessageAds() {
        if (adsLoaded || isFinishing() || isDestroyed()) {
            return;
        }
        adsLoaded = true;

        if (!AdPlacement.getMessageAdShow()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            }

            rlAdView.setVisibility(View.GONE);
            return;
        }

        rlAdView.setVisibility(View.VISIBLE);

        if ("banner".equalsIgnoreCase(AdPlacement.getMessageAdType())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            }

            rlBannerAdView.setVisibility(View.VISIBLE);
            rlNativeAdView.setVisibility(View.GONE);
            AdPlacement.loadAdaptiveBannerAd(this, AdPlacement.getMessageBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd);
        } else {
            rlBannerAdView.setVisibility(View.GONE);
            rlNativeAdView.setVisibility(View.VISIBLE);
            AdPlacement.loadNativeAd(this, AdPlacement.getMessageNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "large");
        }
    }

    private void processIntent(Intent intent) {
        if (intent != null) {
            final String name = intent.getStringExtra("name") == null ? "" : intent.getStringExtra("name");
            final String number = intent.getStringExtra("number") == null ? "" : intent.getStringExtra("number");
            final String message = intent.getStringExtra("message") == null ? "" : intent.getStringExtra("message");
            currentNumber = number;
            Bitmap photo = getContactPhoto(this, number);

            if (photo != null) {
                ivImage.setVisibility(VISIBLE);
                tvName.setVisibility(GONE);
                ivImage.setImageBitmap(photo);
                ivImage.setPadding(0, 0, 0, 0);
            } else if (!name.isEmpty() && !name.equals(number)) {
                ivImage.setVisibility(GONE);
                tvName.setVisibility(VISIBLE);
                tvName.setText(name.substring(0, 1).toUpperCase());
            } else {
                ivImage.setVisibility(VISIBLE);
                tvName.setVisibility(GONE);
                ivImage.setImageResource(R.drawable.ic_user);
                ivImage.setPadding(26, 26, 26, 26);
            }

            tvUserName.setText(name);
            tvUserNumber.setText(number);
            tvUserMessage.setText(message);

            if (isReplyable(number)) {
                llMessageSend.setVisibility(VISIBLE);
                rlSend.setOnClickListener(v -> sendMessage());
            } else {
                llMessageSend.setVisibility(GONE);
            }
        }
    }

    private void sendMessage() {
        String replyMessage = etMessage.getText().toString().trim();
        if (replyMessage.isEmpty()) {
            Toast.makeText(this, "Please Enter Message !", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isDefaultSmsApp()) {
            pendingSendMessage = true;
            requestDefaultSmsApp();
            return;
        }

        try {
            long currentTime = System.currentTimeMillis();
            etMessage.setText("");

            String destinationNumber = get10DigitNumber(currentNumber);
            if (destinationNumber.isEmpty()) {
                destinationNumber = currentNumber;
            }

            Utils.sendSms(this, destinationNumber, replyMessage);

            ContentValues values = new ContentValues();
            values.put("address", currentNumber);
            values.put("body", replyMessage);
            values.put("date", currentTime);
            values.put("type", 2);
            getContentResolver().insert(Uri.parse("content://sms/sent"), values);

            try {
                ContentValues readValues = new ContentValues();
                readValues.put("read", 1);
                getContentResolver().update(Uri.parse("content://sms/inbox"), readValues, "address LIKE ? AND read = 0", new String[]{"%" + destinationNumber});
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            try {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(Utils.getNotificationId(currentNumber));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            finish();
            overridePendingTransition(0, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Message failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isDefaultSmsApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_SMS);
            }
            return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void requestDefaultSmsApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    defaultSmsLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS));
                    return;
                }
            }
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivity(intent);
            overridePendingTransition(0, 0);
        } catch (Exception e) {
            pendingSendMessage = false;
            e.printStackTrace();
        }
    }

    private Bitmap getContactPhoto(Context context, String phoneNumber) {
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.PHOTO_URI}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String photoUri = cursor.getString(0);
                    if (photoUri != null) {
                        InputStream input = context.getContentResolver().openInputStream(Uri.parse(photoUri));
                        cursor.close();
                        return Utils.decodeScaledBitmap(input, 256);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isReplyable(String address) {
        if (address == null) return false;
        address = address.trim();
        String cleanNumber = address.replaceAll("[^\\d+]", "");
        if (cleanNumber.matches(".*[a-zA-Z]+.*")) return false;
        if (cleanNumber.length() < 7) return false;
        return android.util.Patterns.PHONE.matcher(cleanNumber).matches();
    }

    private String get10DigitNumber(String input) {
        String digits = input.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        if (digits.length() == 10) {
            return digits;
        }
        return "";
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processIntent(intent);
    }

    @Override
    protected void onPause() {
        Utils.clearWindowFocusSafely(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Utils.clearWindowFocusSafely(this);
        super.onDestroy();
    }
}