package com.messages.smart.sms.fragments;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.messages.smart.sms.R;
import com.messages.smart.sms.activities.ArchivedMessagesActivity;
import com.messages.smart.sms.activities.BlockMessagesActivity;
import com.messages.smart.sms.activities.AppLanguageActivity;
import com.messages.smart.sms.activities.LauncherSettingsActivity;
import com.messages.smart.sms.activities.MainActivity;
import com.messages.smart.sms.activities.MessagesContentActivity;
import com.messages.smart.sms.activities.RecycleBinMessagesActivity;
import com.messages.smart.sms.adapters.MessagesAdapter;
import com.messages.smart.sms.adapters.MessagesCategoryAdapter;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.helpers.ArchiveHelper;
import com.messages.smart.sms.helpers.BlockHelper;
import com.messages.smart.sms.helpers.ContactUtils;
import com.messages.smart.sms.helpers.MessageFilterHelper;
import com.messages.smart.sms.helpers.MessageListSyncHelper;
import com.messages.smart.sms.helpers.PinHelper;
import com.messages.smart.sms.helpers.RecycleBinHelper;
import com.messages.smart.sms.helpers.SmsUnreadHelper;
import com.messages.smart.sms.interfaces.OnMessageActionListener;
import com.messages.smart.sms.models.MessagesCategoryModel;
import com.messages.smart.sms.models.MessagesModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessagesFragment extends Fragment implements ArchiveHelper.ArchiveChangeListener, BlockHelper.BlockChangeListener, RecycleBinHelper.RecycleBinChangeListener {
    private static final int PAGE_SIZE = 100;
    private static final int SMS_SYNC_DELAY_MS = 300;
    private static final int INITIAL_SHOW_COUNT = 25;
    private static final int LOAD_UPDATE_BATCH = 50;

    private MessagesAdapter messagesAdapter;
    private MessagesCategoryAdapter categoryAdapter;

    private final ArrayList<MessagesCategoryModel> categoryList = new ArrayList<>();
    private final Map<String, MessagesModel> messagesMap = new LinkedHashMap<>();
    private final ArrayList<MessagesModel> displayedMessages = new ArrayList<>();
    private final List<MessagesModel> allMessagesList = Collections.synchronizedList(new ArrayList<>());
    private final List<MessagesModel> unreadList = Collections.synchronizedList(new ArrayList<>());
    private final List<MessagesModel> personalList = Collections.synchronizedList(new ArrayList<>());
    private final List<MessagesModel> transactionList = Collections.synchronizedList(new ArrayList<>());
    private final List<MessagesModel> otpList = Collections.synchronizedList(new ArrayList<>());
    private final List<MessagesModel> offerList = Collections.synchronizedList(new ArrayList<>());

    private String currentCategory;
    private int currentCategoryIndex;
    private String categoryPersonal;
    private String categoryTransaction;
    private String categoryOtp;
    private String categoryOffer;
    private static final String CATEGORY_OTHER = "Other";

    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isDataFetchingComplete = false;
    private volatile boolean isFragmentDestroyed = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Runnable autoLoadRunnable;
    private Runnable pendingSmsSyncRunnable;
    private ContentObserver smsContentObserver;
    private boolean pendingSyncAfterLoad = false;
    private int loadGeneration = 0;

    private LinearLayout llTitleView, llSelectedView, llMessageData, llPermissionData, llPermissionAllow, llOverlayPermissionAllow;
    private ShimmerFrameLayout btnAllow, btnOverlayAllow;
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;
    private ActivityResultLauncher<Intent> defaultSmsLauncher;
    private ActivityResultLauncher<String[]> smsPermissionLauncher;
    private boolean isWaitingForOverlayPermission = false;
    private boolean overlayGrantHandled = false;
    private boolean pendingMarkAllAsRead = false;
    private boolean launchedDefaultSmsSettings = false;
    private boolean isRequestingSmsPermissions = false;
    private boolean suppressSearchCallback = false;
    private boolean pendingSearchRefresh = false;
    private Runnable overlayPermissionChecker;
    private AppCompatTextView tvTitle, tvSelectedCount, tvConversation, tvNoSearchResults;
    private AppCompatTextView tvPermissionRequired, tvPermissionMessageDescription, tvBtnAllow;
    private AppCompatTextView tvOverlayPermissionRequired, tvOverlayPermissionDescription, tvBtnOverlayAllow;
    private RelativeLayout rlMenu, rlCloseMenu, rlPinToTop, rlArchived, rlBlockSpam, rlRecycleBin;
    private AppCompatEditText etSearch;
    private RecyclerView rvMessagesCategory, rvMessages;
    private AppCompatImageView ivPinToTop, ivNewMessage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overlayPermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleOverlayPermissionGranted(false));
        defaultSmsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> finishDefaultSmsRequest());
        smsPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::handleSmsPermissionResult);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        initViews(view);

        return view;
    }

    private void initViews(View viewInside) {
        isFragmentDestroyed = false;

        llTitleView = viewInside.findViewById(R.id.llTitleView);
        tvTitle = viewInside.findViewById(R.id.tvTitle);
        rlMenu = viewInside.findViewById(R.id.rlMenu);

        llSelectedView = viewInside.findViewById(R.id.llSelectedView);
        rlCloseMenu = viewInside.findViewById(R.id.rlCloseMenu);
        tvSelectedCount = viewInside.findViewById(R.id.tvSelectedCount);
        rlPinToTop = viewInside.findViewById(R.id.rlPinToTop);
        ivPinToTop = viewInside.findViewById(R.id.ivPinToTop);
        rlArchived = viewInside.findViewById(R.id.rlArchived);
        rlBlockSpam = viewInside.findViewById(R.id.rlBlockSpam);
        rlRecycleBin = viewInside.findViewById(R.id.rlRecycleBin);

        llMessageData = viewInside.findViewById(R.id.llMessageData);
        llPermissionData = viewInside.findViewById(R.id.llPermissionData);
        llPermissionAllow = viewInside.findViewById(R.id.llPermissionAllow);
        btnAllow = viewInside.findViewById(R.id.btnAllow);
        tvConversation = viewInside.findViewById(R.id.tvConversation);
        llOverlayPermissionAllow = viewInside.findViewById(R.id.llOverlayPermissionAllow);
        btnOverlayAllow = viewInside.findViewById(R.id.btnOverlayAllow);
        tvPermissionRequired = viewInside.findViewById(R.id.tvPermissionRequired);
        tvPermissionMessageDescription = viewInside.findViewById(R.id.tvPermissionMessageDescription);
        tvBtnAllow = viewInside.findViewById(R.id.tvBtnAllow);
        tvOverlayPermissionRequired = viewInside.findViewById(R.id.tvOverlayPermissionRequired);
        tvOverlayPermissionDescription = viewInside.findViewById(R.id.tvOverlayPermissionDescription);
        tvBtnOverlayAllow = viewInside.findViewById(R.id.tvBtnOverlayAllow);

        etSearch = viewInside.findViewById(R.id.etSearch);
        rvMessagesCategory = viewInside.findViewById(R.id.rvMessagesCategory);
        rvMessages = viewInside.findViewById(R.id.rvMessages);
        tvNoSearchResults = viewInside.findViewById(R.id.tvNoSearchResults);
        ivNewMessage = viewInside.findViewById(R.id.ivNewMessage);

        rlMenu.setOnClickListener(view -> setupMenuDropdownPopup());

        ivNewMessage.setOnClickListener(view -> {
            Fragment parent = getParentFragment();
            if (parent instanceof MainContainerFragment) {
                ((MainContainerFragment) parent).switchToContactsTab();
            } else if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToContactsTab();
            }
        });

        initCategoryNames();
        setupCategoryTabs();
        setupMessageList();
        setupSearch();
        setupSelectionActions(viewInside);

        ArchiveHelper.setListener(this);
        BlockHelper.setListener(this);
        RecycleBinHelper.setListener(this);
        MessageListSyncHelper.setListener(messageListSyncListener);

        btnAllow.setOnClickListener(view -> onAllowButtonClicked());
        btnOverlayAllow.setOnClickListener(view -> requestOverlayPermission());
        registerSmsContentObserver();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.post(this::requestPermissionsWhenShown);
    }

    public void requestPermissionsWhenShown() {
        if (!isAdded() || isHidden() || getContext() == null || !areViewsReady()) {
            return;
        }
        refreshMessagesTab();
    }

    private boolean areViewsReady() {
        return getView() != null && llMessageData != null && llPermissionData != null && ivNewMessage != null;
    }

    private static final int REQUIRED_SMS_DENIALS_FOR_SETTINGS = 2;
    private static final String KEY_SMS_PERMISSION_DENY_COUNT = "sms_permission_deny_count";

    private void onAllowButtonClicked() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (hasSmsPermission()) {
            onSmsPermissionsGranted();
            requestRemainingRuntimePermissions(buildMissingSmsPermissionsList());
            return;
        }

        showPermissionUi();
        requestSmsPermissions(true);
    }

    private void requestSmsPermissions(boolean fromAllowButton) {
        if (!isAdded() || getContext() == null) {
            return;
        }

        ArrayList<String> permissionList = buildMissingSmsPermissionsList();

        if (hasSmsPermission()) {
            if (areViewsReady()) {
                showMessageContent();
            }
            if (!isDataFetchingComplete || messagesMap.isEmpty()) {
                startRefresh();
            }
            requestRemainingRuntimePermissions(permissionList);
            return;
        }

        if (areViewsReady()) {
            showPermissionUi();
        }

        if (permissionList.isEmpty()) {
            onSmsPermissionsGranted();
            return;
        }

        boolean permanentlyDenied = isSmsPermissionPermanentlyDenied(permissionList);
        int denyCount = getSmsPermissionDenyCount();

        if (fromAllowButton && (permanentlyDenied || denyCount >= REQUIRED_SMS_DENIALS_FOR_SETTINGS)) {
            dialogSettingPermission();
            return;
        }

        if (permanentlyDenied) {
            return;
        }

        if (isRequestingSmsPermissions || smsPermissionLauncher == null) {
            return;
        }

        isRequestingSmsPermissions = true;
        smsPermissionLauncher.launch(permissionList.toArray(new String[0]));
    }

    private void requestRemainingRuntimePermissions(ArrayList<String> permissionList) {
        if (permissionList == null || permissionList.isEmpty()) {
            return;
        }
        if (isRequestingSmsPermissions || smsPermissionLauncher == null) {
            return;
        }
        if (isAnyPermissionPermanentlyDenied(permissionList)) {
            return;
        }
        isRequestingSmsPermissions = true;
        smsPermissionLauncher.launch(permissionList.toArray(new String[0]));
    }

    private ArrayList<String> buildMissingSmsPermissionsList() {
        ArrayList<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_SMS);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.RECEIVE_SMS);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_CONTACTS);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CALL_PHONE);
        }
        return permissionList;
    }

    private boolean isAnyPermissionPermanentlyDenied(ArrayList<String> permissionList) {
        for (String permission : permissionList) {
            if (isPermissionAskedBefore(permission) && !shouldShowRequestPermissionRationale(permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSmsPermissionPermanentlyDenied(ArrayList<String> permissionList) {
        if (getSmsPermissionDenyCount() <= 0) {
            return false;
        }
        for (String permission : permissionList) {
            if (isPermissionAskedBefore(permission) && !shouldShowRequestPermissionRationale(permission)) {
                return true;
            }
        }
        return false;
    }

    private int getSmsPermissionDenyCount() {
        if (getActivity() == null) {
            return 0;
        }
        return getActivity().getSharedPreferences("permission", MODE_PRIVATE).getInt(KEY_SMS_PERMISSION_DENY_COUNT, 0);
    }

    private void incrementSmsPermissionDenyCount() {
        if (getActivity() == null) {
            return;
        }
        SharedPreferences prefs = getActivity().getSharedPreferences("permission", MODE_PRIVATE);
        prefs.edit().putInt(KEY_SMS_PERMISSION_DENY_COUNT, prefs.getInt(KEY_SMS_PERMISSION_DENY_COUNT, 0) + 1).apply();
    }

    private void resetSmsPermissionDenyCount() {
        if (getActivity() == null) {
            return;
        }
        getActivity().getSharedPreferences("permission", MODE_PRIVATE).edit().putInt(KEY_SMS_PERMISSION_DENY_COUNT, 0).apply();
    }

    private void showMessageContent() {
        if (!areViewsReady()) {
            return;
        }
        llMessageData.setVisibility(VISIBLE);
        llPermissionData.setVisibility(GONE);
        ivNewMessage.setVisibility(VISIBLE);
        updateOverlayPermissionUi();
    }

    private void showPermissionUi() {
        if (!areViewsReady()) {
            return;
        }
        llMessageData.setVisibility(GONE);
        llPermissionData.setVisibility(VISIBLE);
        ivNewMessage.setVisibility(GONE);
        if (llOverlayPermissionAllow != null) {
            llOverlayPermissionAllow.setVisibility(GONE);
        }
    }

    private boolean hasOverlayPermission(Context context) {
        try {
            return Settings.canDrawOverlays(context);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void requestOverlayPermission() {
        try {
            if (!isAdded() || getContext() == null) {
                return;
            }
            if (!hasOverlayPermission(requireContext())) {
                overlayGrantHandled = false;
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + requireContext().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startOverlayPermissionPolling();
                overlayPermissionLauncher.launch(intent);
            } else {
                updateOverlayPermissionUi();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startOverlayPermissionPolling() {
        stopOverlayPermissionPolling();
        isWaitingForOverlayPermission = true;
        overlayPermissionChecker = new Runnable() {
            @Override
            public void run() {
                if (!isWaitingForOverlayPermission || !isAdded() || getContext() == null) {
                    return;
                }
                if (hasOverlayPermission(requireContext())) {
                    handleOverlayPermissionGranted(true);
                    return;
                }
                mainHandler.postDelayed(this, 300);
            }
        };
        mainHandler.postDelayed(overlayPermissionChecker, 300);
    }

    private void stopOverlayPermissionPolling() {
        isWaitingForOverlayPermission = false;
        if (overlayPermissionChecker != null) {
            mainHandler.removeCallbacks(overlayPermissionChecker);
            overlayPermissionChecker = null;
        }
    }

    private void handleOverlayPermissionGranted(boolean bringMainToFront) {
        stopOverlayPermissionPolling();
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (!hasOverlayPermission(requireContext())) {
            updateOverlayPermissionUi();
            return;
        }

        if (bringMainToFront && !overlayGrantHandled) {
            overlayGrantHandled = true;
            bringMainActivityToFront();
            mainHandler.postDelayed(() -> overlayGrantHandled = false, 1500);
        }

        updateOverlayPermissionUi();
        trackOverlayPermissionIfGranted();
    }

    private void bringMainActivityToFront() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        try {
            ActivityManager activityManager = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                activityManager.moveTaskToFront(activity.getTaskId(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        Utils.clearActivityTransition(activity);
    }

    private void updateOverlayPermissionUi() {
        if (llOverlayPermissionAllow == null || !isAdded() || getContext() == null) {
            return;
        }
        if (!hasSmsPermission() || hasOverlayPermission(requireContext()) || Utils.isDefaultHomeApp(requireContext())) {
            llOverlayPermissionAllow.setVisibility(GONE);
        } else {
            llOverlayPermissionAllow.setVisibility(VISIBLE);
        }
    }

    private void trackOverlayPermissionIfGranted() {
        if (isAdded() && getContext() != null && hasOverlayPermission(requireContext())) {
            Utils.trackScreenOnce(requireContext(), "OVERLAY");
        }
    }

    private void onSmsPermissionsGranted() {
        showMessageContent();
        startRefresh();
        if (!hasOverlayPermission(requireContext())) {
            requestOverlayPermission();
        }
    }

    private boolean isPermissionAskedBefore(String permission) {
        if (getActivity() == null) {
            return false;
        }
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("permission", MODE_PRIVATE);
        return sharedPreferences.getBoolean(permission, false);
    }

    private void savePermissionAsked(String[] permissions) {
        if (getActivity() == null) {
            return;
        }
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("permission", MODE_PRIVATE).edit();
        for (String permission : permissions) {
            editor.putBoolean(permission, true);
        }
        editor.apply();
    }

    private void dialogSettingPermission() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
        bottomSheetDialog.setContentView(R.layout.dialog_setting_permission);

        AppCompatTextView tvCancel = bottomSheetDialog.findViewById(R.id.tvCancel);
        AppCompatTextView tvOpenSettings = bottomSheetDialog.findViewById(R.id.tvOpenSettings);

        tvCancel.setOnClickListener(view -> bottomSheetDialog.dismiss());

        tvOpenSettings.setOnClickListener(view -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
            Utils.clearActivityTransition(getActivity());
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    @SuppressLint({"InflateParams", "NotifyDataSetChanged"})
    private void setupMenuDropdownPopup() {
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.menu_dropdown_popup, null);

        PopupWindow popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setElevation(10f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        int y = (int) (-6 * getResources().getDisplayMetrics().density);
        popupWindow.showAsDropDown(rlMenu, 0, y);

        LinearLayout llMarkAsAllRead = popupView.findViewById(R.id.llMarkAsAllRead);
        LinearLayout llArchived = popupView.findViewById(R.id.llArchived);
        LinearLayout llBlock = popupView.findViewById(R.id.llBlock);
        LinearLayout llRecycleBin = popupView.findViewById(R.id.llRecycleBin);
        LinearLayout llChangeLanguage = popupView.findViewById(R.id.llChangeLanguage);
        LinearLayout llLauncherSettings = popupView.findViewById(R.id.llLauncherSettings);

        llMarkAsAllRead.setOnClickListener(view -> {
            popupWindow.dismiss();
            clearSearch();
            handleMarkAllAsRead();
        });

        llArchived.setOnClickListener(view -> {
            popupWindow.dismiss();
            clearSearchForNavigation();
            if (AdPlacement.getOtherInterstitialAdShow()) {
                AdPlacement.loadInterstitialAd(getActivity(), AdPlacement.getOtherInterstitialId(), () -> openActivity(new Intent(getActivity(), ArchivedMessagesActivity.class)));
            } else {
                openActivity(new Intent(getActivity(), ArchivedMessagesActivity.class));
            }
        });

        llBlock.setOnClickListener(view -> {
            popupWindow.dismiss();
            clearSearchForNavigation();
            if (AdPlacement.getOtherInterstitialAdShow()) {
                AdPlacement.loadInterstitialAd(getActivity(), AdPlacement.getOtherInterstitialId(), () -> openActivity(new Intent(getActivity(), BlockMessagesActivity.class)));
            } else {
                openActivity(new Intent(getActivity(), BlockMessagesActivity.class));
            }
        });

        llRecycleBin.setOnClickListener(view -> {
            popupWindow.dismiss();
            clearSearchForNavigation();
            if (AdPlacement.getOtherInterstitialAdShow()) {
                AdPlacement.loadInterstitialAd(getActivity(), AdPlacement.getOtherInterstitialId(), () -> openActivity(new Intent(getActivity(), RecycleBinMessagesActivity.class)));
            } else {
                openActivity(new Intent(getActivity(), RecycleBinMessagesActivity.class));
            }
        });

        llChangeLanguage.setOnClickListener(view -> {
            popupWindow.dismiss();
            clearSearchForNavigation();
            Utils.isAppLanguageStarting = false;
            if (AdPlacement.getOtherInterstitialAdShow()) {
                AdPlacement.loadInterstitialAd(getActivity(), AdPlacement.getOtherInterstitialId(), () -> openActivity(new Intent(getActivity(), AppLanguageActivity.class)));
            } else {
                openActivity(new Intent(getActivity(), AppLanguageActivity.class));
            }
        });

        llLauncherSettings.setOnClickListener(view -> {
            popupWindow.dismiss();
            clearSearchForNavigation();
            if (AdPlacement.getOtherInterstitialAdShow()) {
                AdPlacement.loadInterstitialAd(getActivity(), AdPlacement.getOtherInterstitialId(), () -> openActivity(new Intent(getActivity(), LauncherSettingsActivity.class)));
            } else {
                openActivity(new Intent(getActivity(), LauncherSettingsActivity.class));
            }
        });
    }

    private void hideKeyboard() {
        if (!isAdded() || etSearch == null) {
            return;
        }
        etSearch.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    private void clearSearch() {
        if (etSearch == null) {
            return;
        }
        boolean hadSearch = !etSearch.getText().toString().trim().isEmpty();
        clearSearchTextOnly();
        if (hadSearch) {
            refreshList();
        }
    }

    private void clearSearchForNavigation() {
        if (etSearch == null) {
            return;
        }
        hideKeyboard();
        if (!etSearch.getText().toString().trim().isEmpty()) {
            suppressSearchCallback = true;
            etSearch.setText("");
            suppressSearchCallback = false;
            pendingSearchRefresh = true;
        }
    }

    private void clearSearchTextOnly() {
        if (etSearch == null) {
            return;
        }
        hideKeyboard();
        if (!etSearch.getText().toString().trim().isEmpty()) {
            suppressSearchCallback = true;
            etSearch.setText("");
            suppressSearchCallback = false;
        }
    }

    private void applyPendingSearchRefresh() {
        if (!pendingSearchRefresh || !isAdded() || isHidden()) {
            return;
        }
        pendingSearchRefresh = false;
        mainHandler.post(this::refreshList);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            clearSearchForNavigation();
            return;
        }
        applyPendingSearchRefresh();
        refreshLocaleUi();
        requestPermissionsWhenShown();
    }

    private void refreshMessagesTab() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (hasSmsPermission()) {
            showMessageContent();
            if (!isDataFetchingComplete || messagesMap.isEmpty()) {
                startRefresh();
            } else {
                purgeHiddenThreadsFromMemory();
                scheduleIncomingSmsSync();
                refreshUnreadCounts();
            }
            updateOverlayPermissionUi();
            if (launchedDefaultSmsSettings) {
                launchedDefaultSmsSettings = false;
                finishDefaultSmsRequest();
            }
        } else {
            showPermissionUi();
            requestSmsPermissions(false);
        }
    }

    public void reloadListBannerAd() {
        if (!isAdded() || messagesAdapter == null) {
            return;
        }
        messagesAdapter.reloadBannerAd();
    }

    public void refreshLocaleUi() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        Context localized = Utils.localeResourcesContext(getContext());

        if (tvTitle != null) {
            tvTitle.setText(localized.getResources().getText(R.string.messages));
        }

        if (etSearch != null) {
            etSearch.setHint(localized.getResources().getText(R.string.search_message));
        }

        if (tvNoSearchResults != null) {
            tvNoSearchResults.setText(localized.getResources().getText(R.string.no_data_found));
            if (etSearch != null) {
                updateSearchEmptyState(etSearch.getText().toString());
            }
        }

        refreshPermissionLocaleTexts();
        refreshCategoryLocale();
    }

    private void refreshPermissionLocaleTexts() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        Context localized = Utils.localeResourcesContext(getContext());

        if (tvPermissionRequired != null) {
            tvPermissionRequired.setText(localized.getResources().getText(R.string.permission_required));
        }
        if (tvPermissionMessageDescription != null) {
            tvPermissionMessageDescription.setText(localized.getResources().getText(R.string.permission_message_description));
        }
        if (tvBtnAllow != null) {
            tvBtnAllow.setText(localized.getResources().getText(R.string.allow));
        }
        if (tvConversation != null) {
            tvConversation.setText(localized.getResources().getText(R.string.your_conversation_will_appear_here));
        }
        if (tvOverlayPermissionRequired != null) {
            tvOverlayPermissionRequired.setText(localized.getResources().getText(R.string.overlay_permission_required));
        }
        if (tvOverlayPermissionDescription != null) {
            tvOverlayPermissionDescription.setText(localized.getResources().getText(R.string.overlay_permission_description));
        }
        if (tvBtnOverlayAllow != null) {
            tvBtnOverlayAllow.setText(localized.getResources().getText(R.string.allow));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshLocaleUi();

        if (isHidden()) {
            return;
        }

        applyPendingSearchRefresh();
        requestPermissionsWhenShown();
    }

    public void requestSmsSync() {
        scheduleIncomingSmsSync();
    }

    public void requestImmediateSmsSync() {
        if (isFragmentDestroyed || !hasSmsPermission() || !isDataFetchingComplete) {
            return;
        }
        cancelPendingSmsSync();
        executor.execute(this::fetchAndApplyLatestSms);
    }

    public void applyOutboundMessage(String threadId, String name, String address, String body, long date, String photoUri) {
        if (!isAdded() || isFragmentDestroyed) {
            return;
        }
        Context context = getContext();
        if (context != null && MessageFilterHelper.isHiddenFromMainList(context, threadId)) {
            return;
        }

        MessagesModel model = new MessagesModel(threadId, name, address, body, date, Telephony.Sms.MESSAGE_TYPE_SENT, 1, photoUri, 0);
        model.category = getCategory(body, name, address);
        if (context != null) {
            model.setPinned(PinHelper.isPinned(context, threadId));
        }
        applyIncomingMessage(model, true);
    }

    private void applyContactUpdate(String threadId, String name, String address, String photoUri) {
        if (!isAdded() || isFragmentDestroyed || TextUtils.isEmpty(threadId)) {
            return;
        }
        MessagesModel existing = messagesMap.get(threadId);
        if (existing == null) {
            return;
        }
        if (TextUtils.equals(existing.getName(), name) && TextUtils.equals(existing.getContactPhotoUri(), photoUri)) {
            return;
        }

        MessagesModel updated = new MessagesModel(threadId, name, address, existing.getBody(), existing.getDate(), existing.getType(), existing.getRead(), photoUri, existing.getUnreadCount());
        updated.category = existing.category;
        updated.setPinned(existing.isPinned());
        updated.setSelected(existing.isSelected());

        messagesMap.put(threadId, updated);
        removeFromAllLists(threadId);
        insertIntoCategoryLists(updated);
        updateDisplayedContactItem(threadId, updated);
    }

    private void updateDisplayedContactItem(String threadId, MessagesModel updated) {
        if (messagesAdapter == null) {
            return;
        }
        for (int i = 0; i < displayedMessages.size(); i++) {
            if (threadId.equals(displayedMessages.get(i).getThreadId())) {
                displayedMessages.set(i, updated);
                messagesAdapter.notifyItemChanged(messagesAdapter.getAdapterPositionForMessageIndex(i));
                return;
            }
        }
    }

    private final MessageListSyncHelper.Listener messageListSyncListener = new MessageListSyncHelper.Listener() {
        @Override
        public void onOutboundMessage(String threadId, String name, String address, String body, long date, String photoUri) {
            applyOutboundMessage(threadId, name, address, body, date, photoUri);
        }

        @Override
        public void onSyncRequired() {
            requestImmediateSmsSync();
        }

        @Override
        public void onThreadDeleted(String threadId) {
            removeDeletedThreadFromMainList(threadId);
        }

        @Override
        public void onContactUpdated(String threadId, String name, String address, String photoUri) {
            applyContactUpdate(threadId, name, address, photoUri);
        }
    };

    private void handleMarkAllAsRead() {
        if (!isAdded() || getContext() == null || !hasSmsPermission()) {
            return;
        }
        if (isDefaultSmsApp()) {
            markAllMessagesAsRead();
        } else {
            pendingMarkAllAsRead = true;
            requestDefaultSmsApp();
        }
    }

    private boolean isDefaultSmsApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = requireContext().getSystemService(RoleManager.class);
                return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_SMS);
            }
            String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(requireContext());
            return requireContext().getPackageName().equals(defaultSmsPackage);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void requestDefaultSmsApp() {
        if (!isAdded() || getActivity() == null) {
            pendingMarkAllAsRead = false;
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = requireActivity().getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    defaultSmsLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS));
                    return;
                }
            }
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, requireContext().getPackageName());
            launchedDefaultSmsSettings = true;
            startActivity(intent);
            Utils.clearActivityTransition(getActivity());
        } catch (Exception e) {
            pendingMarkAllAsRead = false;
            launchedDefaultSmsSettings = false;
            e.printStackTrace();
        }
    }

    private void finishDefaultSmsRequest() {
        if (!pendingMarkAllAsRead || !isAdded() || getContext() == null) {
            return;
        }
        pendingMarkAllAsRead = false;
        if (isDefaultSmsApp()) {
            markAllMessagesAsRead();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void markAllMessagesAsRead() {
        if (!isAdded() || getContext() == null || !hasSmsPermission() || !isDefaultSmsApp()) {
            return;
        }

        Context context = requireContext();
        executor.execute(() -> {
            SmsUnreadHelper.markAllAsRead(context);

            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                for (MessagesModel model : messagesMap.values()) {
                    model.setUnreadCount(0);
                }
                unreadList.clear();
                if (messagesAdapter != null) {
                    messagesAdapter.notifyDataSetChanged();
                }
                if (currentCategoryIndex == 1) {
                    refreshList();
                }
            });
        });
    }

    private void refreshUnreadCounts() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        executor.execute(() -> {
            Map<String, Integer> unreadCounts = SmsUnreadHelper.getUnreadCountsByThread(context);
            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                applyUnreadCounts(unreadCounts);
            });
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyUnreadCounts(Map<String, Integer> unreadCounts) {
        List<MessagesModel> snapshot = new ArrayList<>(messagesMap.values());
        for (MessagesModel model : snapshot) {
            model.setUnreadCount(unreadCounts.getOrDefault(model.getThreadId(), 0));
        }
        rebuildUnreadList();
        if (messagesAdapter != null) {
            messagesAdapter.notifyDataSetChanged();
        }
    }

    private void rebuildUnreadList() {
        unreadList.clear();
        for (MessagesModel model : allMessagesList) {
            if (model.getUnreadCount() > 0) {
                unreadList.add(model);
            }
        }
        sortByDate(unreadList);
    }

    @Override
    public void onDestroyView() {
        ArchiveHelper.setListener(null);
        BlockHelper.setListener(null);
        RecycleBinHelper.setListener(null);
        MessageListSyncHelper.setListener(null);
        unregisterSmsContentObserver();
        cancelPendingSmsSync();
        stopOverlayPermissionPolling();
        isRequestingSmsPermissions = false;
        isFragmentDestroyed = true;
        stopAutomaticPagination();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void registerSmsContentObserver() {
        if (smsContentObserver != null || getContext() == null) {
            return;
        }
        smsContentObserver = new ContentObserver(mainHandler) {
            @Override
            public void onChange(boolean selfChange) {
                scheduleIncomingSmsSync();
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                scheduleIncomingSmsSync();
            }
        };
        requireContext().getContentResolver().registerContentObserver(Uri.parse("content://sms"), true, smsContentObserver);
    }

    private void unregisterSmsContentObserver() {
        if (smsContentObserver != null && getContext() != null) {
            requireContext().getContentResolver().unregisterContentObserver(smsContentObserver);
            smsContentObserver = null;
        }
    }

    private void scheduleIncomingSmsSync() {
        if (isFragmentDestroyed || !hasSmsPermission()) {
            return;
        }
        if (!isDataFetchingComplete) {
            pendingSyncAfterLoad = true;
            return;
        }
        cancelPendingSmsSync();
        pendingSmsSyncRunnable = () -> executor.execute(this::fetchAndApplyLatestSms);
        mainHandler.postDelayed(pendingSmsSyncRunnable, SMS_SYNC_DELAY_MS);
    }

    private void cancelPendingSmsSync() {
        if (pendingSmsSyncRunnable != null) {
            mainHandler.removeCallbacks(pendingSmsSyncRunnable);
            pendingSmsSyncRunnable = null;
        }
    }

    private void fetchAndApplyLatestSms() {
        fetchAndApplyLatestSms(false);
    }

    private void fetchAndApplyLatestSms(boolean isRetry) {
        Context context = getContext();
        if (context == null || isFragmentDestroyed) {
            return;
        }

        ArrayList<MessagesModel> latestPerThread = collectLatestMessagesPerThread(context);
        if (latestPerThread.isEmpty()) {
            if (!isRetry) {
                mainHandler.postDelayed(() -> executor.execute(() -> fetchAndApplyLatestSms(true)), 700);
            }
            return;
        }

        mainHandler.post(() -> {
            if (!isAdded()) {
                return;
            }
            boolean changed = false;
            for (MessagesModel model : latestPerThread) {
                if (applyIncomingMessage(model, false)) {
                    changed = true;
                }
            }
            if (changed) {
                refreshDisplayedListAfterSync(true);
            } else if (!isRetry) {
                mainHandler.postDelayed(() -> executor.execute(() -> fetchAndApplyLatestSms(true)), 700);
            }
        });
    }

    private ArrayList<MessagesModel> collectLatestMessagesPerThread(Context context) {
        ArrayList<MessagesModel> latestPerThread = new ArrayList<>();
        Set<String> seenThreads = new HashSet<>();

        try (Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms"), null, null, null, "date DESC")) {
            if (cursor == null) {
                return latestPerThread;
            }
            while (cursor.moveToNext() && seenThreads.size() < 20) {
                String threadId = cursor.getString(cursor.getColumnIndexOrThrow("thread_id"));
                if (threadId == null || seenThreads.contains(threadId)) {
                    continue;
                }
                if (MessageFilterHelper.isHiddenFromMainList(context, threadId)) {
                    continue;
                }
                seenThreads.add(threadId);
                latestPerThread.add(parseMessageFromCursor(context, cursor, 0));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return latestPerThread;
        }

        for (MessagesModel model : latestPerThread) {
            model.setUnreadCount(SmsUnreadHelper.getThreadUnreadCount(context, model.getThreadId()));
        }
        return latestPerThread;
    }

    private MessagesModel parseMessageFromCursor(Context context, Cursor cursor, int unreadCount) {
        return parseMessageFromCursor(context, cursor, unreadCount, null);
    }

    private MessagesModel parseMessageFromCursor(Context context, Cursor cursor, int unreadCount, Map<String, ContactUtils.ContactDetails> contactCache) {
        String threadId = cursor.getString(cursor.getColumnIndexOrThrow("thread_id"));
        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        if (address == null) {
            address = "";
        }
        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
        int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
        int read = cursor.getInt(cursor.getColumnIndexOrThrow("read"));

        ContactUtils.ContactDetails contact;
        if (contactCache != null) {
            contact = contactCache.get(address);
            if (contact == null) {
                contact = ContactUtils.lookup(context, address);
                contactCache.put(address, contact);
            }
        } else {
            contact = ContactUtils.lookup(context, address);
        }

        MessagesModel messagesModel = new MessagesModel(threadId, contact.getName(), address, body, date, type, read, contact.getPhotoUri(), unreadCount);
        messagesModel.category = getCategory(body, contact.getName(), address);
        messagesModel.setPinned(PinHelper.isPinned(context, threadId));
        return messagesModel;
    }

    private boolean applyIncomingMessage(MessagesModel model, boolean refreshDisplayImmediately) {
        if (model == null || model.getThreadId() == null) {
            return false;
        }
        Context context = getContext();
        if (context != null && MessageFilterHelper.isHiddenFromMainList(context, model.getThreadId())) {
            return false;
        }

        MessagesModel existing = messagesMap.get(model.getThreadId());
        if (context != null) {
            model.setUnreadCount(SmsUnreadHelper.getThreadUnreadCount(context, model.getThreadId()));
            model.setPinned(PinHelper.isPinned(context, model.getThreadId()));
        }

        if (existing != null && existing.getDate() == model.getDate() && textEquals(existing.getBody(), model.getBody()) && existing.getRead() == model.getRead() && existing.getUnreadCount() == model.getUnreadCount()) {
            return false;
        }

        removeFromAllLists(model.getThreadId());
        messagesMap.put(model.getThreadId(), model);
        insertIntoCategoryLists(model);

        if (refreshDisplayImmediately) {
            refreshDisplayedListAfterSync(true);
        }
        return true;
    }

    private static boolean textEquals(String first, String second) {
        if (first == null) {
            return second == null || second.isEmpty();
        }
        return first.equals(second);
    }

    private void removeFromAllLists(String threadId) {
        removeByThreadId(allMessagesList, threadId);
        removeByThreadId(unreadList, threadId);
        removeByThreadId(personalList, threadId);
        removeByThreadId(transactionList, threadId);
        removeByThreadId(otpList, threadId);
        removeByThreadId(offerList, threadId);
    }

    private void removeByThreadId(List<MessagesModel> list, String threadId) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (Objects.equals(list.get(i).getThreadId(), threadId)) {
                list.remove(i);
            }
        }
    }

    private void insertIntoCategoryLists(MessagesModel model) {
        removeFromAllLists(model.getThreadId());
        allMessagesList.add(model);
        sortByDate(allMessagesList);

        if (model.getUnreadCount() > 0) {
            unreadList.add(model);
            sortByDate(unreadList);
        }

        if (categoryPersonal.equals(model.category)) {
            personalList.add(model);
            sortByDate(personalList);
        } else if (categoryTransaction.equals(model.category)) {
            transactionList.add(model);
            sortByDate(transactionList);
        } else if (categoryOtp.equals(model.category)) {
            otpList.add(model);
            sortByDate(otpList);
        } else if (categoryOffer.equals(model.category)) {
            offerList.add(model);
            sortByDate(offerList);
        }
    }

    private void refreshDisplayedListAfterSync() {
        refreshDisplayedListAfterSync(false);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshDisplayedListAfterSync(boolean scrollToTop) {
        String searchText = etSearch.getText().toString();
        if (!searchText.trim().isEmpty()) {
            searchMessages(searchText);
            return;
        }

        List<MessagesModel> currentList = getCurrentCategoryList();
        displayedMessages.clear();

        int end = Math.min(Math.max(currentPage, 1) * PAGE_SIZE, currentList.size());
        if (end == 0 && !currentList.isEmpty()) {
            end = Math.min(PAGE_SIZE, currentList.size());
            currentPage = 1;
        }
        if (end > 0) {
            displayedMessages.addAll(currentList.subList(0, end));
        }

        messagesAdapter.notifyDataSetChanged();

        if (scrollToTop && !displayedMessages.isEmpty()) {
            rvMessages.scrollToPosition(0);
        }
    }

    private void initCategoryNames() {
        currentCategoryIndex = 0;
        currentCategory = getString(R.string.all);
        categoryPersonal = getString(R.string.personal);
        categoryTransaction = getString(R.string.transaction);
        categoryOtp = getString(R.string.otp);
        categoryOffer = getString(R.string.offer);
    }

    private int getSelectedCategoryIndex() {
        return currentCategoryIndex;
    }

    private void refreshCategoryLocale() {
        if (!isAdded() || getContext() == null || categoryAdapter == null || categoryList.isEmpty()) {
            return;
        }

        Context localized = Utils.localeResourcesContext(getContext());
        int selectedIndex = getSelectedCategoryIndex();

        categoryPersonal = localized.getString(R.string.personal);
        categoryTransaction = localized.getString(R.string.transaction);
        categoryOtp = localized.getString(R.string.otp);
        categoryOffer = localized.getString(R.string.offer);

        for (MessagesModel model : allMessagesList) {
            model.category = getCategory(model.getBody(), model.getName(), model.getAddress());
        }

        categoryList.get(0).setName(localized.getString(R.string.all));
        categoryList.get(1).setName(localized.getString(R.string.unread));
        categoryList.get(2).setName(localized.getString(R.string.personal));
        categoryList.get(3).setName(localized.getString(R.string.transaction));
        categoryList.get(4).setName(localized.getString(R.string.otp));
        categoryList.get(5).setName(localized.getString(R.string.offer));

        currentCategoryIndex = selectedIndex;
        currentCategory = categoryList.get(selectedIndex).getName();
        categoryAdapter.selectTab(selectedIndex);
        rebuildCategoryListsFromAll();

        if (isDataFetchingComplete) {
            refreshList();
        }
    }

    private void setupCategoryTabs() {
        categoryList.clear();
        categoryList.add(new MessagesCategoryModel(getString(R.string.all), true));
        categoryList.add(new MessagesCategoryModel(getString(R.string.unread), false));
        categoryList.add(new MessagesCategoryModel(getString(R.string.personal), false));
        categoryList.add(new MessagesCategoryModel(getString(R.string.transaction), false));
        categoryList.add(new MessagesCategoryModel(getString(R.string.otp), false));
        categoryList.add(new MessagesCategoryModel(getString(R.string.offer), false));

        categoryAdapter = new MessagesCategoryAdapter(requireContext(), categoryList, position -> {
            currentCategoryIndex = position;
            currentCategory = categoryList.get(position).getName();
            categoryAdapter.selectTab(position);
            clearSearchTextOnly();
            refreshList();
        });

        rvMessagesCategory.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMessagesCategory.setAdapter(categoryAdapter);
        rvMessagesCategory.setNestedScrollingEnabled(false);
    }

    private void setupMessageList() {
        messagesAdapter = new MessagesAdapter(requireContext(), displayedMessages, true);
        messagesAdapter.setActionListener(new OnMessageActionListener() {
            @Override
            public void onMessageClick(int position, int selectedCount) {
                updateSelectionView(selectedCount);
            }

            @Override
            public void onMessageLongClick(int position, int selectedCount) {
                updateSelectionView(selectedCount);
            }
        });

        messagesAdapter.setOpenListener(messagesModel -> {
            if (AdPlacement.getMessageListInterstitialAdShow()) {
                AdPlacement.loadInterstitialAdMessageList(requireActivity(), AdPlacement.getMessageListInterstitialId(), () -> openChatScreen(messagesModel));
            } else {
                openChatScreen(messagesModel);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setInitialPrefetchItemCount(8);

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);

        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messagesAdapter);
        rvMessages.setItemViewCacheSize(20);
        rvMessages.setItemAnimator(animator);
        addScrollListener();
    }

    private void setupSearch() {
        configureSearchInput(etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressSearchCallback) {
                    return;
                }
                searchMessages(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void configureSearchInput(AppCompatEditText editText) {
        if (editText == null) {
            return;
        }
        editText.setSingleLine(true);
        editText.setMaxLines(1);
        editText.setHorizontallyScrolling(true);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50), (source, start, end, dest, dstart, dend) -> {
            if (source == null) {
                return null;
            }
            StringBuilder filtered = new StringBuilder();
            for (int i = start; i < end; i++) {
                char character = source.charAt(i);
                if (character != '\n' && character != '\r') {
                    filtered.append(character);
                }
            }
            if (filtered.length() == end - start) {
                return null;
            }
            return filtered;
        }});
        editText.setOnEditorActionListener((textView, actionId, event) -> {
            hideKeyboard();
            return true;
        });
    }

    private void openChatScreen(MessagesModel model) {
        clearSearchForNavigation();
        markMessageAsRead(model);
        Intent intent = new Intent(requireContext(), MessagesContentActivity.class);
        intent.putExtra(MessagesContentActivity.EXTRA_THREAD_ID, model.getThreadId());
        intent.putExtra(MessagesContentActivity.EXTRA_NAME, model.getName());
        intent.putExtra(MessagesContentActivity.EXTRA_ADDRESS, model.getAddress());
        intent.putExtra(MessagesContentActivity.EXTRA_PHOTO_URI, model.getContactPhotoUri());
        startActivity(intent);
        Utils.clearActivityTransition(getActivity());
    }

    private void openActivity(Intent intent) {
        startActivity(intent);
        Utils.clearActivityTransition(getActivity());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void markMessageAsRead(MessagesModel model) {
        Context context = getContext();
        if (context == null || model == null) {
            return;
        }

        SmsUnreadHelper.markThreadAsRead(context, model.getThreadId(), model.getAddress());
        model.setUnreadCount(0);
        rebuildUnreadList();
        if (messagesAdapter != null) {
            messagesAdapter.notifyDataSetChanged();
        }
        if (currentCategoryIndex == 1) {
            refreshList();
        }
    }

    private void setupSelectionActions(View view) {
        view.findViewById(R.id.rlCloseMenu).setOnClickListener(v -> exitSelectionMode());
        view.findViewById(R.id.rlArchived).setOnClickListener(v -> archiveSelectedMessages());
        view.findViewById(R.id.rlBlockSpam).setOnClickListener(v -> blockSelectedMessages());
        view.findViewById(R.id.rlRecycleBin).setOnClickListener(v -> moveSelectedToBin());
    }

    @SuppressLint("SetTextI18n")
    private void updateSelectionView(int selectedCount) {
        if (selectedCount > 0) {
            llTitleView.setVisibility(View.GONE);
            llSelectedView.setVisibility(View.VISIBLE);
            tvSelectedCount.setText(selectedCount + " " + getString(R.string.selected));
            messagesAdapter.setSelectionMode(true);
            updatePinButtonState();
        } else {
            exitSelectionMode();
        }
    }

    private void updatePinButtonState() {
        if (ivPinToTop == null || rlPinToTop == null || messagesAdapter == null) {
            return;
        }

        List<MessagesModel> selected = messagesAdapter.getSelectedMessages();
        for (MessagesModel messagesModel : selected) {
            if (messagesModel.isPinned()) {
                ivPinToTop.setImageResource(R.drawable.ic_unpin_menu);
                rlPinToTop.setOnClickListener(view -> messageUnpin());
            } else {
                ivPinToTop.setImageResource(R.drawable.ic_pin_menu);
                rlPinToTop.setOnClickListener(view -> messagePin());
            }
        }
    }

    private void messagePin() {
        List<MessagesModel> selected = new ArrayList<>(messagesAdapter.getSelectedMessages());
        if (selected.isEmpty()) {
            return;
        }

        for (MessagesModel model : selected) {
            PinHelper.pin(requireContext(), model.getThreadId());
            model.setPinned(true);
        }

        sortAllLists();
        messagesAdapter.clearSelection();
        exitSelectionMode();
        refreshList();
        Toast.makeText(requireContext(), R.string.toast_pinned, Toast.LENGTH_SHORT).show();
    }

    private void messageUnpin() {
        List<MessagesModel> selected = new ArrayList<>(messagesAdapter.getSelectedMessages());
        if (selected.isEmpty()) {
            return;
        }

        for (MessagesModel model : selected) {
            if (model.isPinned()) {
                PinHelper.unpin(requireContext(), model.getThreadId());
                model.setPinned(false);
            }
        }

        sortAllLists();
        messagesAdapter.clearSelection();
        exitSelectionMode();
        refreshList();
        Toast.makeText(requireContext(), R.string.toast_unpinned, Toast.LENGTH_SHORT).show();
    }

    private void sortAllLists() {
        sortByDate(allMessagesList);
        sortByDate(unreadList);
        sortByDate(personalList);
        sortByDate(transactionList);
        sortByDate(otpList);
        sortByDate(offerList);
    }

    private void exitSelectionMode() {
        llTitleView.setVisibility(View.VISIBLE);
        llSelectedView.setVisibility(View.GONE);
        messagesAdapter.setSelectionMode(false);
    }

    private void archiveSelectedMessages() {
        List<MessagesModel> selected = new ArrayList<>(messagesAdapter.getSelectedMessages());
        if (selected.isEmpty()) {
            return;
        }

        for (MessagesModel model : selected) {
            ArchiveHelper.archive(requireContext(), model);
        }

        messagesAdapter.clearSelection();
        exitSelectionMode();
        Toast.makeText(requireContext(), R.string.toast_archived, Toast.LENGTH_SHORT).show();
    }

    private void blockSelectedMessages() {
        List<MessagesModel> selected = new ArrayList<>(messagesAdapter.getSelectedMessages());
        if (selected.isEmpty()) {
            return;
        }

        for (MessagesModel model : selected) {
            BlockHelper.block(requireContext(), model);
        }

        messagesAdapter.clearSelection();
        exitSelectionMode();
        Toast.makeText(requireContext(), R.string.toast_blocked, Toast.LENGTH_SHORT).show();
    }

    private void moveSelectedToBin() {
        List<MessagesModel> selected = new ArrayList<>(messagesAdapter.getSelectedMessages());
        if (selected.isEmpty()) {
            return;
        }

        for (MessagesModel model : selected) {
            RecycleBinHelper.moveToBin(requireContext(), model);
        }

        messagesAdapter.clearSelection();
        exitSelectionMode();
        Toast.makeText(requireContext(), R.string.toast_moved_to_recycle_bin, Toast.LENGTH_SHORT).show();
    }

    private void removeMessageFromMainList(String threadId) {
        messagesMap.remove(threadId);
        removeFromAllLists(threadId);
        removeByThreadId(displayedMessages, threadId);
    }

    private void removeDeletedThreadFromMainList(String threadId) {
        if (TextUtils.isEmpty(threadId) || !isAdded() || isFragmentDestroyed) {
            return;
        }
        removeMessageFromMainList(threadId);
        if (messagesAdapter != null) {
            refreshDisplayedListAfterSync();
        }
    }

    private void purgeHiddenThreadsFromMemory() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        ArrayList<String> hiddenThreadIds = new ArrayList<>();
        for (String threadId : messagesMap.keySet()) {
            if (MessageFilterHelper.isHiddenFromMainList(context, threadId)) {
                hiddenThreadIds.add(threadId);
            }
        }
        for (String threadId : hiddenThreadIds) {
            removeMessageFromMainList(threadId);
        }
        boolean changed = !hiddenThreadIds.isEmpty() || deduplicateMessagesInMemory();
        if (changed && messagesAdapter != null && isAdded()) {
            refreshDisplayedListAfterSync();
        }
    }

    private boolean deduplicateMessagesInMemory() {
        if (allMessagesList.size() <= 1) {
            return false;
        }
        Map<String, MessagesModel> uniqueByThread = new LinkedHashMap<>();
        for (MessagesModel model : allMessagesList) {
            if (model == null || TextUtils.isEmpty(model.getThreadId())) {
                continue;
            }
            MessagesModel existing = uniqueByThread.get(model.getThreadId());
            if (existing == null || model.getDate() > existing.getDate()) {
                uniqueByThread.put(model.getThreadId(), model);
            }
        }
        if (uniqueByThread.size() == allMessagesList.size()) {
            return false;
        }
        allMessagesList.clear();
        allMessagesList.addAll(uniqueByThread.values());
        sortByDate(allMessagesList);
        messagesMap.clear();
        messagesMap.putAll(uniqueByThread);
        rebuildCategoryListsFromAll();
        removeDuplicateThreadIdsFromDisplayedList();
        return true;
    }

    private void rebuildCategoryListsFromAll() {
        unreadList.clear();
        personalList.clear();
        transactionList.clear();
        otpList.clear();
        offerList.clear();
        for (MessagesModel model : allMessagesList) {
            if (model.getUnreadCount() > 0) {
                unreadList.add(model);
            }
            if (categoryPersonal.equals(model.category)) {
                personalList.add(model);
            } else if (categoryTransaction.equals(model.category)) {
                transactionList.add(model);
            } else if (categoryOtp.equals(model.category)) {
                otpList.add(model);
            } else if (categoryOffer.equals(model.category)) {
                offerList.add(model);
            }
        }
        sortByDate(unreadList);
        sortByDate(personalList);
        sortByDate(transactionList);
        sortByDate(otpList);
        sortByDate(offerList);
    }

    private void removeDuplicateThreadIdsFromDisplayedList() {
        Set<String> seen = new HashSet<>();
        for (int i = displayedMessages.size() - 1; i >= 0; i--) {
            String threadId = displayedMessages.get(i).getThreadId();
            if (!seen.add(threadId)) {
                displayedMessages.remove(i);
            }
        }
    }

    private MessagesModel fetchLatestModelForThread(Context context, String targetThreadId) {
        if (context == null || TextUtils.isEmpty(targetThreadId)) {
            return null;
        }
        Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms"), null, "thread_id = ?", new String[]{targetThreadId}, "date DESC LIMIT 1");
        try (cursor) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToFirst()) {
                int unreadCount = SmsUnreadHelper.getThreadUnreadCount(context, targetThreadId);
                return parseMessageFromCursor(context, cursor, unreadCount);
            }
        }
        return null;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onArchived(MessagesModel model) {
        if (model == null || TextUtils.isEmpty(model.getThreadId())) {
            return;
        }
        removeMessageFromMainList(model.getThreadId());
        if (!isAdded()) {
            return;
        }
        if (messagesAdapter != null) {
            messagesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onUnarchived(MessagesModel model) {
        restoreMessageToMainList(model);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBlocked(MessagesModel model) {
        if (model == null || TextUtils.isEmpty(model.getThreadId())) {
            return;
        }
        removeMessageFromMainList(model.getThreadId());
        if (!isAdded()) {
            return;
        }
        if (messagesAdapter != null) {
            messagesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onUnblocked(MessagesModel model) {
        restoreMessageToMainList(model);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onMovedToBin(MessagesModel model) {
        if (model == null || TextUtils.isEmpty(model.getThreadId())) {
            return;
        }
        removeMessageFromMainList(model.getThreadId());
        if (!isAdded()) {
            return;
        }
        if (messagesAdapter != null) {
            messagesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRestoredFromBin(MessagesModel model) {
        restoreMessageToMainList(model);
    }

    private void restoreMessageToMainList(MessagesModel model) {
        if (model == null || TextUtils.isEmpty(model.getThreadId())) {
            return;
        }
        Context context = getContext();
        if (context != null && MessageFilterHelper.isHiddenFromMainList(context, model.getThreadId())) {
            return;
        }

        removeFromAllLists(model.getThreadId());
        removeByThreadId(displayedMessages, model.getThreadId());

        MessagesModel toRestore = fetchLatestModelForThread(context, model.getThreadId());
        if (toRestore == null) {
            toRestore = model;
        }
        toRestore.setSelected(false);
        if (context != null) {
            toRestore.setPinned(PinHelper.isPinned(context, toRestore.getThreadId()));
            toRestore.setUnreadCount(SmsUnreadHelper.getThreadUnreadCount(context, toRestore.getThreadId()));
        }

        messagesMap.put(toRestore.getThreadId(), toRestore);
        insertIntoCategoryLists(toRestore);

        if (!isAdded()) {
            return;
        }
        refreshList();
    }

    private void startRefresh() {
        stopAutomaticPagination();
        isDataFetchingComplete = false;
        loadGeneration++;
        executor.execute(this::loadMessagesFromDevice);
    }

    private void loadMessagesFromDevice() {
        Context context = getContext();
        if (context == null || isFragmentDestroyed) {
            return;
        }

        final int generation = loadGeneration;

        Map<String, MessagesModel> tempMap = new LinkedHashMap<>();
        ArrayList<MessagesModel> tempAllMessages = new ArrayList<>();
        ArrayList<MessagesModel> tempUnread = new ArrayList<>();
        ArrayList<MessagesModel> tempPersonal = new ArrayList<>();
        ArrayList<MessagesModel> tempTransaction = new ArrayList<>();
        ArrayList<MessagesModel> tempOtp = new ArrayList<>();
        ArrayList<MessagesModel> tempOffer = new ArrayList<>();
        Map<String, ContactUtils.ContactDetails> contactCache = new HashMap<>();

        Set<String> hiddenIds = new HashSet<>();
        hiddenIds.addAll(ArchiveHelper.getArchivedIds(context));
        hiddenIds.addAll(BlockHelper.getBlockedIds(context));
        hiddenIds.addAll(RecycleBinHelper.getRecycleBinIds(context));

        Map<String, Integer> unreadCounts = SmsUnreadHelper.getUnreadCountsByThread(context);

        Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms"), null, null, null, "date DESC");

        int count = 0;
        boolean hasPublishedInitial = false;

        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (isFragmentDestroyed || generation != loadGeneration) {
                    cursor.close();
                    return;
                }

                String threadId = cursor.getString(cursor.getColumnIndexOrThrow("thread_id"));
                if (threadId == null || hiddenIds.contains(threadId)) {
                    continue;
                }
                if (tempMap.containsKey(threadId)) {
                    continue;
                }

                int unreadCount = unreadCounts.getOrDefault(threadId, 0);
                MessagesModel messagesModel = parseMessageFromCursor(context, cursor, unreadCount, contactCache);

                tempMap.put(threadId, messagesModel);
                tempAllMessages.add(messagesModel);

                if (messagesModel.getUnreadCount() > 0) {
                    tempUnread.add(messagesModel);
                }

                if (categoryPersonal.equals(messagesModel.category)) {
                    tempPersonal.add(messagesModel);
                } else if (categoryTransaction.equals(messagesModel.category)) {
                    tempTransaction.add(messagesModel);
                } else if (categoryOtp.equals(messagesModel.category)) {
                    tempOtp.add(messagesModel);
                } else if (categoryOffer.equals(messagesModel.category)) {
                    tempOffer.add(messagesModel);
                }

                count++;
                if (!hasPublishedInitial && count >= INITIAL_SHOW_COUNT) {
                    publishLoadedSnapshot(generation, tempMap, tempAllMessages, tempUnread, tempPersonal, tempTransaction, tempOtp, tempOffer, false);
                    hasPublishedInitial = true;
                } else if (count % LOAD_UPDATE_BATCH == 0) {
                    publishLoadedSnapshot(generation, tempMap, tempAllMessages, tempUnread, tempPersonal, tempTransaction, tempOtp, tempOffer, false);
                }
            }
            cursor.close();
        }

        publishLoadedSnapshot(generation, tempMap, tempAllMessages, tempUnread, tempPersonal, tempTransaction, tempOtp, tempOffer, true);
    }

    private void publishLoadedSnapshot(int generation, Map<String, MessagesModel> tempMap, List<MessagesModel> tempAllMessages, List<MessagesModel> tempUnread, List<MessagesModel> tempPersonal, List<MessagesModel> tempTransaction, List<MessagesModel> tempOtp, List<MessagesModel> tempOffer, boolean isFinal) {
        final Map<String, MessagesModel> mapCopy = new LinkedHashMap<>(tempMap);
        final List<MessagesModel> allCopy = new ArrayList<>(tempAllMessages);
        final List<MessagesModel> unreadCopy = new ArrayList<>(tempUnread);
        final List<MessagesModel> personalCopy = new ArrayList<>(tempPersonal);
        final List<MessagesModel> transactionCopy = new ArrayList<>(tempTransaction);
        final List<MessagesModel> otpCopy = new ArrayList<>(tempOtp);
        final List<MessagesModel> offerCopy = new ArrayList<>(tempOffer);

        mainHandler.post(() -> applyLoadedSnapshot(generation, mapCopy, allCopy, unreadCopy, personalCopy, transactionCopy, otpCopy, offerCopy, isFinal));
    }

    private void applyLoadedSnapshot(int generation, Map<String, MessagesModel> mapCopy, List<MessagesModel> allCopy, List<MessagesModel> unreadCopy, List<MessagesModel> personalCopy, List<MessagesModel> transactionCopy, List<MessagesModel> otpCopy, List<MessagesModel> offerCopy, boolean isFinal) {
        if (generation != loadGeneration || !isAdded() || isFragmentDestroyed) {
            return;
        }

        messagesMap.clear();
        messagesMap.putAll(mapCopy);
        allMessagesList.clear();
        allMessagesList.addAll(allCopy);
        unreadList.clear();
        unreadList.addAll(unreadCopy);
        personalList.clear();
        personalList.addAll(personalCopy);
        transactionList.clear();
        transactionList.addAll(transactionCopy);
        otpList.clear();
        otpList.addAll(otpCopy);
        offerList.clear();
        offerList.addAll(offerCopy);

        if (isFinal) {
            sortByDate(allMessagesList);
            sortByDate(unreadList);
            sortByDate(personalList);
            sortByDate(transactionList);
            sortByDate(otpList);
            sortByDate(offerList);
        }

        isDataFetchingComplete = isFinal;

        if (hasSmsPermission()) {
            showMessageContent();
        }

        String searchText = etSearch.getText().toString();
        if (!searchText.trim().isEmpty()) {
            searchMessages(searchText);
            return;
        }

        if (displayedMessages.isEmpty() && !allMessagesList.isEmpty()) {
            currentPage = 0;
            loadNextPage();
            startAutomaticPagination();
        } else if (!displayedMessages.isEmpty()) {
            if (isFinal) {
                refreshDisplayedListAfterSync();
            }
            startAutomaticPagination();
        }

        if (isFinal && pendingSyncAfterLoad) {
            pendingSyncAfterLoad = false;
            scheduleIncomingSmsSync();
        }
    }

    private void sortByDate(List<MessagesModel> list) {
        list.sort((a, b) -> {
            if (a.isPinned() && !b.isPinned()) {
                return -1;
            }
            if (!a.isPinned() && b.isPinned()) {
                return 1;
            }
            return Long.compare(b.getDate(), a.getDate());
        });
    }

    private String getCategory(String body, String name, String address) {
        String text = body != null ? body.toLowerCase(Locale.getDefault()) : "";
        if (text.contains("otp") || text.contains("verification") || text.contains("code")) {
            return categoryOtp;
        }
        if (text.contains("bank") || text.contains("credit") || text.contains("debit") || text.contains("balance") || text.contains("transaction")) {
            return categoryTransaction;
        }
        if (text.contains("offer") || text.contains("discount") || text.contains("promo") || text.contains("off")) {
            return categoryOffer;
        }
        if (name != null && !name.equals(address != null ? address : "")) {
            return categoryPersonal;
        }
        return CATEGORY_OTHER;
    }

    private List<MessagesModel> getCurrentCategoryList() {
        switch (currentCategoryIndex) {
            case 1:
                return unreadList;
            case 2:
                return personalList;
            case 3:
                return transactionList;
            case 4:
                return otpList;
            case 5:
                return offerList;
            default:
                return allMessagesList;
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshList() {
        updateSearchEmptyState("");
        stopAutomaticPagination();
        currentPage = 0;
        displayedMessages.clear();
        rvMessages.scrollToPosition(0);
        loadNextPage();
        if (displayedMessages.isEmpty()) {
            messagesAdapter.notifyDataSetChanged();
        }
        startAutomaticPagination();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadNextPage() {
        if (isLoading || isFragmentDestroyed || !isAdded()) {
            return;
        }

        List<MessagesModel> currentList = getCurrentCategoryList();
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, currentList.size());

        if (start < currentList.size()) {
            isLoading = true;
            int previousSize = displayedMessages.size();
            for (int i = start; i < end; i++) {
                displayedMessages.add(currentList.get(i));
            }
            int insertedCount = displayedMessages.size() - previousSize;
            currentPage++;
            if (insertedCount > 0) {
                if (previousSize == 0) {
                    messagesAdapter.notifyDataSetChanged();
                } else {
                    messagesAdapter.notifyItemRangeInserted(messagesAdapter.getAdapterPositionForMessageIndex(previousSize), insertedCount);
                }
            }
            isLoading = false;
        } else if (isDataFetchingComplete) {
            stopAutomaticPagination();
        }
    }

    private void startAutomaticPagination() {
        stopAutomaticPagination();

        autoLoadRunnable = new Runnable() {
            @Override
            public void run() {
                List<MessagesModel> currentList = getCurrentCategoryList();
                if (currentPage * PAGE_SIZE < currentList.size()) {
                    loadNextPage();
                    mainHandler.postDelayed(this, 200);
                } else {
                    stopAutomaticPagination();
                }
            }
        };

        mainHandler.postDelayed(autoLoadRunnable, 200);
    }

    private void stopAutomaticPagination() {
        if (autoLoadRunnable != null) {
            mainHandler.removeCallbacks(autoLoadRunnable);
            autoLoadRunnable = null;
        }
    }

    private void addScrollListener() {
        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                List<MessagesModel> currentList = getCurrentCategoryList();
                if (isDataFetchingComplete && displayedMessages.size() == currentList.size()) {
                    return;
                }

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() >= displayedMessages.size() - 5) {
                    loadNextPage();
                }
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void searchMessages(String query) {
        ArrayList<MessagesModel> searchList = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            refreshList();
            return;
        }

        String search = query.toLowerCase(Locale.getDefault());
        List<MessagesModel> snapshot = new ArrayList<>(getCurrentCategoryList());
        Context context = getContext();
        for (MessagesModel messagesModel : snapshot) {
            if (context != null && MessageFilterHelper.isHiddenFromMainList(context, messagesModel.getThreadId())) {
                continue;
            }
            String name = messagesModel.getName() != null ? messagesModel.getName().toLowerCase(Locale.getDefault()) : "";
            String body = messagesModel.getBody() != null ? messagesModel.getBody().toLowerCase(Locale.getDefault()) : "";
            if (name.contains(search) || body.contains(search)) {
                searchList.add(messagesModel);
            }
        }

        sortByDate(searchList);
        stopAutomaticPagination();
        displayedMessages.clear();
        displayedMessages.addAll(searchList);
        messagesAdapter.notifyDataSetChanged();
        updateSearchEmptyState(query);
    }

    private void updateSearchEmptyState(String query) {
        if (tvNoSearchResults == null || rvMessages == null) {
            return;
        }
        boolean isSearching = query != null && !query.trim().isEmpty();
        if (!isSearching) {
            tvNoSearchResults.setVisibility(GONE);
            rvMessages.setVisibility(VISIBLE);
            return;
        }
        boolean noResults = displayedMessages.isEmpty();
        tvNoSearchResults.setVisibility(noResults ? VISIBLE : GONE);
        rvMessages.setVisibility(noResults ? GONE : VISIBLE);
    }

    private boolean hasSmsPermission() {
        Context context = getContext();
        return context != null && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void handleSmsPermissionResult(@NonNull Map<String, Boolean> result) {
        isRequestingSmsPermissions = false;

        if (!isAdded() || getContext() == null) {
            return;
        }

        ArrayList<String> askedPermissions = new ArrayList<>(result.keySet());
        savePermissionAsked(askedPermissions.toArray(new String[0]));

        boolean smsGranted = Boolean.TRUE.equals(result.get(Manifest.permission.READ_SMS)) || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;

        if (Boolean.TRUE.equals(result.get(Manifest.permission.READ_SMS))) {
            Utils.trackScreenOnce(requireContext(), "READ_SMS");
        }
        if (Boolean.TRUE.equals(result.get(Manifest.permission.RECEIVE_SMS))) {
            Utils.trackScreenOnce(requireContext(), "RECEIVE_SMS");
        }
        if (Boolean.TRUE.equals(result.get(Manifest.permission.READ_CONTACTS))) {
            Utils.trackScreenOnce(requireContext(), "READ_CONTACTS");
        }
        if (Boolean.TRUE.equals(result.get(Manifest.permission.READ_PHONE_STATE))) {
            Utils.trackScreenOnce(requireContext(), "READ_PHONE_STATE");
        }
        if (Boolean.TRUE.equals(result.get(Manifest.permission.CALL_PHONE))) {
            Utils.trackScreenOnce(requireContext(), "CALL_PHONE");
        }

        if (smsGranted) {
            resetSmsPermissionDenyCount();
            onSmsPermissionsGranted();
            return;
        }

        boolean smsDenied = result.containsKey(Manifest.permission.READ_SMS) && !Boolean.TRUE.equals(result.get(Manifest.permission.READ_SMS));
        if (smsDenied) {
            incrementSmsPermissionDenyCount();
        }
        showPermissionUi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAutomaticPagination();
        executor.shutdownNow();
    }
}