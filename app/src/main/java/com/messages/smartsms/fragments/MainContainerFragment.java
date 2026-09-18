package com.messages.smartsms.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.messages.smartsms.R;
import com.messages.smartsms.activities.LauncherHomeActivity;
import com.messages.smartsms.activities.MainActivity;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.IPAddressHelper;
import com.messages.smartsms.common.Utils;
import com.messages.smartsms.helpers.RemoteConfigHelper;

public class MainContainerFragment extends Fragment {
    private LinearLayout llMessages, llContacts, llSettings, llBannerAd, llBottomSection;
    private AppCompatImageView ivMessages, ivContacts, ivSettings;
    private AppCompatTextView tvMessages, tvContacts, tvSettings;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer, slNativeShimmer;
    private FrameLayout flNativeAd;

    private static final String TAG_MESSAGES = "MESSAGES";
    private static final String TAG_CONTACTS = "CONTACTS";
    private static final String TAG_SETTINGS = "SETTINGS";
    private static final String PREFS_DEFAULT_HOME_POPUP = "default_home_popup";
    private static final String KEY_LAST_DEFAULT_HOME_POPUP_MS = "last_shown_ms";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private Fragment messagesFragment;
    private Fragment contactsFragment;
    private Fragment settingsFragment;

    private OnBackPressedCallback backPressedCallback;
    private ActivityResultLauncher<Intent> homeRoleLauncher;
    private boolean defaultHomeRoleRequestInFlight;
    private boolean defaultHomePromptCheckPosted;
    private final Handler mainAdsReloadHandler = new Handler(Looper.getMainLooper());
    private final Runnable mainAdsReloadRunnable = this::onMainAdsReloadTick;
    private int mainAdLoadToken;
    private boolean mainAdLoading;
    private boolean mainAdLoaded;
    private boolean lastMainAdWasNative;
    private AdView mainBannerAdView;
    private NativeAd mainNativeAd;
    private boolean keyboardVisible;
    private boolean bottomSectionHiddenForKeyboard;
    private View keyboardListenRoot;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        homeRoleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> defaultHomeRoleRequestInFlight = false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(requireContext().getColor(R.color.white));
        applyMainWindowUi();
        findIDs(view);
    }

    @Override
    public void onDestroyView() {
        stopMainAdsReload();
        clearMainAdViews();
        mainAdLoadToken++;
        mainAdLoading = false;
        mainAdLoaded = false;
        teardownKeyboardBottomSectionHandling();
        if (backPressedCallback != null) {
            backPressedCallback.remove();
            backPressedCallback = null;
        }
        llMessages = null;
        llContacts = null;
        llSettings = null;
        llBannerAd = null;
        llBottomSection = null;
        ivMessages = null;
        ivContacts = null;
        ivSettings = null;
        tvMessages = null;
        tvContacts = null;
        tvSettings = null;
        rlAdView = null;
        rlBannerAdView = null;
        rlNativeAdView = null;
        slBannerShimmer = null;
        slNativeShimmer = null;
        flNativeAd = null;
        super.onDestroyView();
    }

    private void findIDs(View view) {
        llBottomSection = view.findViewById(R.id.llBottomSection);
        Utils.applyNavigationBarPadding(llBottomSection);

        llMessages = view.findViewById(R.id.llMessages);
        llContacts = view.findViewById(R.id.llContacts);
        llSettings = view.findViewById(R.id.llSettings);

        ivMessages = view.findViewById(R.id.ivMessages);
        ivContacts = view.findViewById(R.id.ivContacts);
        ivSettings = view.findViewById(R.id.ivSettings);

        tvMessages = view.findViewById(R.id.tvMessages);
        tvContacts = view.findViewById(R.id.tvContacts);
        tvSettings = view.findViewById(R.id.tvSettings);

        rlAdView = view.findViewById(R.id.rlAdView);
        rlBannerAdView = view.findViewById(R.id.rlBannerAdView);
        slBannerShimmer = view.findViewById(R.id.slBannerShimmer);
        llBannerAd = view.findViewById(R.id.llBannerAd);
        rlNativeAdView = view.findViewById(R.id.rlNativeAdView);
        slNativeShimmer = view.findViewById(R.id.slNativeShimmer);
        flNativeAd = view.findViewById(R.id.flNativeAd);

        initialEvents();
    }

    private void initialEvents() {
        setupFragments();
        setupBannerAd();
        setupKeyboardBottomSectionHandling();
        setupIpLookup();
        setupBottomNavListeners();
        setupBackPress();
    }

    private void setupFragments() {
        FragmentManager fragmentManager = getChildFragmentManager();
        messagesFragment = fragmentManager.findFragmentByTag(TAG_MESSAGES);
        contactsFragment = fragmentManager.findFragmentByTag(TAG_CONTACTS);
        settingsFragment = fragmentManager.findFragmentByTag(TAG_SETTINGS);

        if (messagesFragment == null || contactsFragment == null || settingsFragment == null) {
            messagesFragment = new MessagesFragment();
            contactsFragment = new ContactsFragment();
            settingsFragment = new SettingsFragment();
            fragmentManager.beginTransaction().add(R.id.flData, messagesFragment, TAG_MESSAGES).add(R.id.flData, contactsFragment, TAG_CONTACTS).hide(contactsFragment).add(R.id.flData, settingsFragment, TAG_SETTINGS).hide(settingsFragment).commitNow();
        }

        if (Utils.isFromContacts) {
            Utils.isFromContacts = false;
            MainActivity.currentTab = "ContactsFragment";
        } else if (MainActivity.currentTab == null || MainActivity.currentTab.isEmpty()) {
            MainActivity.currentTab = "MessagesFragment";
        }

        showTab(MainActivity.currentTab);
    }

    private void setupBannerAd() {
        setupBannerAd(false);
    }

    private boolean setupBannerAd(boolean refresh) {
        if (!isAdded() || rlAdView == null) {
            return false;
        }
        if (!AdPlacement.isNetworkAvailable(requireContext()) && !AdPlacement.shouldUseQuizPriority()) {
            hideMainBannerAd();
            stopMainAdsReload();
            applyBottomSectionKeyboardVisibility(keyboardVisible);
            return false;
        }
        if (!AdPlacement.shouldShowMainAd()) {
            hideMainBannerAd();
            stopMainAdsReload();
            applyBottomSectionKeyboardVisibility(keyboardVisible);
            return false;
        }
        if (mainAdLoading) {
            if (refresh && isMainBannerHostVisible()) {
                scheduleMainAdsReload();
            }
            return false;
        }

        boolean wantNative = AdPlacement.isSmallNativeAdType(AdPlacement.getMainAdType());
        if (refresh || wantNative != lastMainAdWasNative) {
            clearMainAdViews();
            mainAdLoaded = false;
            lastMainAdWasNative = wantNative;
        }

        mainAdLoadToken++;
        final int loadToken = mainAdLoadToken;
        mainAdLoading = true;

        rlAdView.setVisibility(VISIBLE);
        if (wantNative) {
            showMainNativeAd(loadToken);
        } else {
            showMainBannerAd(loadToken);
        }
        if (isMainBannerHostVisible() && getMainAdRefreshIntervalMs() > 0L) {
            scheduleMainAdsReload();
        }
        applyBottomSectionKeyboardVisibility(keyboardVisible);
        return true;
    }

    private void showMainBannerAd(int loadToken) {
        if (rlNativeAdView != null) {
            rlNativeAdView.setVisibility(GONE);
        }
        if (slNativeShimmer != null) {
            slNativeShimmer.setVisibility(GONE);
        }
        if (flNativeAd != null) {
            flNativeAd.removeAllViews();
            flNativeAd.setVisibility(GONE);
        }
        if (rlBannerAdView == null) {
            mainAdLoading = false;
            return;
        }
        destroyMainBannerAdView();
        rlBannerAdView.setVisibility(VISIBLE);
        if (slBannerShimmer != null) {
            slBannerShimmer.setVisibility(VISIBLE);
        }
        if (llBannerAd != null) {
            llBannerAd.setVisibility(GONE);
        }
        AdPlacement.loadBannerAd(requireActivity(), AdPlacement.getMainBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd, () -> handleMainAdLoadFailed(loadToken), adView -> {
            if (loadToken != mainAdLoadToken || !isAdded()) {
                destroyMainBannerAdView(adView);
                return;
            }
            mainAdLoading = false;
            mainAdLoaded = true;
            mainBannerAdView = adView;
            AdPlacement.markMainContainerAdLoaded();
            if (rlBannerAdView != null) {
                rlBannerAdView.setVisibility(VISIBLE);
            }
        });
    }

    private void showMainNativeAd(int loadToken) {
        if (rlBannerAdView != null) {
            rlBannerAdView.setVisibility(GONE);
        }
        if (slBannerShimmer != null) {
            slBannerShimmer.setVisibility(GONE);
        }
        destroyMainBannerAdView();
        if (llBannerAd != null) {
            llBannerAd.setVisibility(GONE);
        }
        if (rlNativeAdView == null || slNativeShimmer == null || flNativeAd == null) {
            mainAdLoading = false;
            return;
        }
        destroyMainNativeAd();
        if (flNativeAd.getChildCount() > 0) {
            flNativeAd.removeAllViews();
        }
        rlNativeAdView.setVisibility(VISIBLE);
        AdPlacement.loadNativeAd(requireActivity(), AdPlacement.getMainNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "small", nativeAd -> {
            if (loadToken != mainAdLoadToken || !isAdded()) {
                nativeAd.destroy();
                return;
            }
            destroyMainNativeAd();
            mainNativeAd = nativeAd;
            mainAdLoading = false;
            mainAdLoaded = true;
            AdPlacement.markMainContainerAdLoaded();
            if (rlNativeAdView != null) {
                rlNativeAdView.setVisibility(VISIBLE);
            }
        }, () -> handleMainAdLoadFailed(loadToken));
    }

    private void handleMainAdLoadFailed(int loadToken) {
        if (loadToken != mainAdLoadToken || !isAdded()) {
            return;
        }
        mainAdLoading = false;
        if (mainAdLoaded) {
            return;
        }
        if (slBannerShimmer != null) {
            slBannerShimmer.setVisibility(GONE);
        }
        if (slNativeShimmer != null) {
            slNativeShimmer.setVisibility(GONE);
        }
    }

    private void clearMainAdViews() {
        destroyMainBannerAdView();
        destroyMainNativeAd();
        if (llBannerAd != null) {
            llBannerAd.removeAllViews();
            llBannerAd.setVisibility(GONE);
        }
        if (flNativeAd != null) {
            flNativeAd.removeAllViews();
            flNativeAd.setVisibility(GONE);
        }
    }

    private void destroyMainNativeAd() {
        if (mainNativeAd != null) {
            mainNativeAd.destroy();
            mainNativeAd = null;
        }
    }

    private void destroyMainBannerAdView() {
        destroyMainBannerAdView(mainBannerAdView);
        mainBannerAdView = null;
        if (llBannerAd == null) {
            return;
        }
        for (int i = llBannerAd.getChildCount() - 1; i >= 0; i--) {
            View child = llBannerAd.getChildAt(i);
            if (child instanceof AdView) {
                destroyMainBannerAdView((AdView) child);
                llBannerAd.removeViewAt(i);
            }
        }
    }

    private void destroyMainBannerAdView(AdView adView) {
        if (adView == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) adView.getParent();
        if (parent != null) {
            parent.removeView(adView);
        }
        adView.destroy();
    }

    private void hideMainBannerAd() {
        mainAdLoadToken++;
        mainAdLoading = false;
        mainAdLoaded = false;
        if (rlAdView != null) {
            rlAdView.setVisibility(GONE);
        }
        if (rlBannerAdView != null) {
            rlBannerAdView.setVisibility(GONE);
        }
        if (rlNativeAdView != null) {
            rlNativeAdView.setVisibility(GONE);
        }
        if (slBannerShimmer != null) {
            slBannerShimmer.setVisibility(GONE);
        }
        if (slNativeShimmer != null) {
            slNativeShimmer.setVisibility(GONE);
        }
        clearMainAdViews();
    }

    private boolean isMainBannerHostVisible() {
        if (!isAdded() || getView() == null || !isResumed()) {
            return false;
        }
        if (requireActivity() instanceof LauncherHomeActivity) {
            return ((LauncherHomeActivity) requireActivity()).isShowingMainFragment();
        }
        return true;
    }

    private long getMainAdRefreshIntervalMs() {
        if (!AdPlacement.getMainAdAutoRefresh()) {
            return 0L;
        }
        int seconds = RemoteConfigHelper.getMainAdAutoSecond();
        if (seconds <= 0) {
            return AdPlacement.getMainAdReloadIntervalMs();
        }
        return seconds * 1000L;
    }

    private void scheduleMainAdsReload() {
        stopMainAdsReload();
        if (!AdPlacement.shouldShowMainAd()) {
            return;
        }
        long intervalMs = getMainAdRefreshIntervalMs();
        if (intervalMs <= 0L) {
            return;
        }
        mainAdsReloadHandler.postDelayed(mainAdsReloadRunnable, intervalMs);
    }

    private void stopMainAdsReload() {
        mainAdsReloadHandler.removeCallbacks(mainAdsReloadRunnable);
    }

    private void onMainAdsReloadTick() {
        if (!isAdded() || getView() == null || !isMainBannerHostVisible()) {
            return;
        }
        if (getMainAdRefreshIntervalMs() <= 0L) {
            return;
        }
        setupBannerAd(true);
    }

    private void ensureExistingMainAdsVisible() {
        if (!isAdded() || rlAdView == null) {
            return;
        }
        if (!AdPlacement.shouldShowMainAd()) {
            hideMainBannerAd();
            return;
        }
        if (mainAdLoaded) {
            rlAdView.setVisibility(VISIBLE);
            boolean wantNative = AdPlacement.isSmallNativeAdType(AdPlacement.getMainAdType());
            if (wantNative) {
                if (rlNativeAdView != null) {
                    rlNativeAdView.setVisibility(VISIBLE);
                }
                if (rlBannerAdView != null) {
                    rlBannerAdView.setVisibility(GONE);
                }
            } else {
                if (rlBannerAdView != null) {
                    rlBannerAdView.setVisibility(VISIBLE);
                }
                if (rlNativeAdView != null) {
                    rlNativeAdView.setVisibility(GONE);
                }
            }
            if (slBannerShimmer != null) {
                slBannerShimmer.setVisibility(GONE);
            }
            if (slNativeShimmer != null) {
                slNativeShimmer.setVisibility(GONE);
            }
        } else if (!mainAdLoading) {
            setupBannerAd(false);
        }
        if (isMainBannerHostVisible() && getMainAdRefreshIntervalMs() > 0L) {
            scheduleMainAdsReload();
        }
        applyBottomSectionKeyboardVisibility(keyboardVisible);
    }

    private void setupKeyboardBottomSectionHandling() {
        View root = getView();
        if (root == null) {
            return;
        }
        teardownKeyboardBottomSectionHandling();
        keyboardListenRoot = root;
        keyboardLayoutListener = () -> {
            if (!isAdded() || keyboardListenRoot == null || llBottomSection == null) {
                return;
            }
            Rect visibleFrame = new Rect();
            keyboardListenRoot.getWindowVisibleDisplayFrame(visibleFrame);
            int rootHeight = keyboardListenRoot.getRootView().getHeight();
            if (rootHeight <= 0) {
                return;
            }
            int heightDiff = rootHeight - visibleFrame.height();
            boolean imeVisible = heightDiff > Math.max(rootHeight * 0.15f, keyboardListenRoot.getResources().getDisplayMetrics().density * 100f);
            applyBottomSectionKeyboardVisibility(imeVisible);
        };
        keyboardListenRoot.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }

    private void teardownKeyboardBottomSectionHandling() {
        if (keyboardListenRoot != null && keyboardLayoutListener != null) {
            ViewTreeObserver observer = keyboardListenRoot.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.removeOnGlobalLayoutListener(keyboardLayoutListener);
            }
        }
        keyboardListenRoot = null;
        keyboardLayoutListener = null;
        keyboardVisible = false;
        bottomSectionHiddenForKeyboard = false;
    }

    private void applyBottomSectionKeyboardVisibility(boolean imeVisible) {
        if (!isAdded() || llBottomSection == null) {
            return;
        }
        keyboardVisible = imeVisible;

        if (imeVisible) {
            if (llBottomSection.getVisibility() != GONE) {
                bottomSectionHiddenForKeyboard = true;
            }
            llBottomSection.setVisibility(GONE);
            return;
        }

        if (bottomSectionHiddenForKeyboard || llBottomSection.getVisibility() != VISIBLE) {
            bottomSectionHiddenForKeyboard = false;
            llBottomSection.setVisibility(VISIBLE);
        }
    }

    public void reloadMainAds(boolean applyReloadInterval, boolean reloadListBanner) {
        if (!isAdded() || getView() == null) {
            return;
        }
        if (applyReloadInterval && !AdPlacement.hasMainContainerAdReloadIntervalElapsed()) {
            getView().post(() -> {
                if (!isAdded()) {
                    return;
                }
                ensureExistingMainAdsVisible();
                if (reloadListBanner) {
                    reloadMessagesListBannerAd();
                }
            });
            return;
        }
        getView().post(() -> {
            if (!isAdded()) {
                return;
            }
            setupBannerAd(true);
            if (reloadListBanner) {
                reloadMessagesListBannerAd();
            }
        });
    }

    public void reloadMessagesListBannerAd() {
        if (messagesFragment instanceof MessagesFragment && messagesFragment.isAdded()) {
            ((MessagesFragment) messagesFragment).reloadListBannerAd();
        }
    }

    private void setupIpLookup() {
        if (!AdPlacement.getClEndBackAdCountryIP()) {
            return;
        }
        IPAddressHelper.getCountryName(new IPAddressHelper.IPCallback() {
            @Override
            public void onResponse(String countryName) {
                if (countryName != null && !countryName.isEmpty() && getContext() != null) {
                    AdPlacement.setIpCountryName(requireContext().getApplicationContext(), countryName);
                }
            }

            @Override
            public void onFailure(Exception e) {
            }
        });
    }

    private void setupBottomNavListeners() {
        llMessages.setOnClickListener(view -> selectTab("MessagesFragment"));
        llContacts.setOnClickListener(view -> selectTab("ContactsFragment"));
        llSettings.setOnClickListener(view -> selectTab("SettingsFragment"));
    }

    private void selectTab(String tabName) {
        if (tabName != null && tabName.equals(MainActivity.currentTab)) {
            return;
        }
        if (AdPlacement.getInterstitialAdShowSwitchFragment()) {
            AdPlacement.loadInterstitialAdFragmentSwitch(requireActivity(), AdPlacement.getOtherInterstitialId(), () -> {
                if (!isAdded()) {
                    MainActivity.currentTab = tabName;
                    return;
                }
                showTab(tabName);
            });
        } else {
            showTab(tabName);
        }
    }

    public void switchToContactsTab() {
        selectTab("ContactsFragment");
    }

    public void showTab(String tab) {
        if (!isAdded() || getView() == null) {
            MainActivity.currentTab = tab != null ? tab : "MessagesFragment";
            return;
        }

        if (messagesFragment == null || contactsFragment == null || settingsFragment == null) {
            if (tab != null && !tab.isEmpty()) {
                MainActivity.currentTab = tab;
            }
            setupFragments();
            return;
        }

        Fragment targetFragment;
        String selectedTab;
        if ("ContactsFragment".equals(tab)) {
            targetFragment = contactsFragment;
            selectedTab = "ContactsFragment";
        } else if ("SettingsFragment".equals(tab)) {
            targetFragment = settingsFragment;
            selectedTab = "SettingsFragment";
        } else {
            targetFragment = messagesFragment;
            selectedTab = "MessagesFragment";
        }

        MainActivity.currentTab = selectedTab;
        updateBottomNavUi();
        switchFragment(targetFragment);
    }

    private void updateBottomNavUi() {
        if (tvMessages == null || ivMessages == null || ivContacts == null || ivSettings == null) {
            return;
        }

        Context localized = Utils.localeResourcesContext(requireContext());
        tvMessages.setText(localized.getResources().getText(R.string.messages));
        tvContacts.setText(localized.getResources().getText(R.string.contacts));
        tvSettings.setText(localized.getResources().getText(R.string.settings));

        String currentFragment = MainActivity.currentTab != null ? MainActivity.currentTab : "MessagesFragment";
        if ("MessagesFragment".equals(currentFragment)) {
            ivMessages.setImageResource(R.drawable.ic_message_select);
            tvMessages.setTextColor(getResources().getColor(R.color.primary));
            ivContacts.setImageResource(R.drawable.ic_contact_un_select);
            tvContacts.setTextColor(getResources().getColor(R.color.gray));
            ivSettings.setImageResource(R.drawable.ic_setting_un_select);
            tvSettings.setTextColor(getResources().getColor(R.color.gray));
        } else if ("ContactsFragment".equals(currentFragment)) {
            ivMessages.setImageResource(R.drawable.ic_message_un_select);
            tvMessages.setTextColor(getResources().getColor(R.color.gray));
            ivContacts.setImageResource(R.drawable.ic_contact_select);
            tvContacts.setTextColor(getResources().getColor(R.color.primary));
            ivSettings.setImageResource(R.drawable.ic_setting_un_select);
            tvSettings.setTextColor(getResources().getColor(R.color.gray));
        } else {
            ivMessages.setImageResource(R.drawable.ic_message_un_select);
            tvMessages.setTextColor(getResources().getColor(R.color.gray));
            ivContacts.setImageResource(R.drawable.ic_contact_un_select);
            tvContacts.setTextColor(getResources().getColor(R.color.gray));
            ivSettings.setImageResource(R.drawable.ic_setting_select);
            tvSettings.setTextColor(getResources().getColor(R.color.primary));
        }
    }

    private void setupBackPress() {
        if (backPressedCallback != null) {
            backPressedCallback.remove();
        }

        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (requireActivity() instanceof LauncherHomeActivity) {
                    ((LauncherHomeActivity) requireActivity()).showLauncherHomeFragment();
                } else {
                    requireActivity().finish();
                    requireActivity().overridePendingTransition(0, 0);
                }
            }
        };

        boolean enableInHost = !(requireActivity() instanceof LauncherHomeActivity);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);
        backPressedCallback.setEnabled(enableInHost);
    }

    private void switchFragment(Fragment fragment) {
        if (fragment == null || !isAdded() || messagesFragment == null || contactsFragment == null || settingsFragment == null) {
            return;
        }
        getChildFragmentManager().beginTransaction().setReorderingAllowed(true).hide(messagesFragment).hide(contactsFragment).hide(settingsFragment).show(fragment).commitNowAllowingStateLoss();
    }

    public void applyInitialTabFromHome() {
        if (Utils.isFromContacts || "ContactsFragment".equals(MainActivity.currentTab)) {
            Utils.isFromContacts = false;
            showTab("ContactsFragment");
            return;
        }
        if (MainActivity.currentTab == null || MainActivity.currentTab.isEmpty()) {
            MainActivity.currentTab = "MessagesFragment";
        }
        showTab(MainActivity.currentTab);
    }

    public void showMessagesTabDefault() {
        Utils.isFromContacts = false;
        showTab("MessagesFragment");
    }

    public void prepareFreshMessagesOpen() {
        Utils.isFromContacts = false;
        MainActivity.currentTab = "MessagesFragment";
        if (messagesFragment == null || contactsFragment == null || settingsFragment == null) {
            return;
        }
        showTab("MessagesFragment");
    }

    public void prepareContactsOpen() {
        Utils.isFromContacts = false;
        MainActivity.currentTab = "ContactsFragment";
        if (messagesFragment == null || contactsFragment == null || settingsFragment == null) {
            return;
        }
        showTab("ContactsFragment");
    }

    public void reapplyLocalizedTexts() {
        if (!isAdded() || getView() == null || tvMessages == null) {
            return;
        }
        updateBottomNavUi();

        if (messagesFragment instanceof MessagesFragment && messagesFragment.isAdded()) {
            ((MessagesFragment) messagesFragment).refreshLocaleUi();
        }
        if (contactsFragment instanceof ContactsFragment && contactsFragment.isAdded()) {
            ((ContactsFragment) contactsFragment).refreshContactsTexts();
        }
        if (settingsFragment instanceof SettingsFragment && settingsFragment.isAdded()) {
            ((SettingsFragment) settingsFragment).refreshSettingsTexts();
        }
    }

    public void applyMainWindowUi() {
        if (!isAdded()) {
            return;
        }
        if (requireActivity() instanceof LauncherHomeActivity && !isResumed() && !((LauncherHomeActivity) requireActivity()).isShowingMainFragment()) {
            return;
        }

        Window window = requireActivity().getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(requireContext().getColor(R.color.white));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            View decor = window.getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (backPressedCallback != null) {
            backPressedCallback.setEnabled(!(requireActivity() instanceof LauncherHomeActivity));
        }

        if (Utils.consumeLocaleChanged()) {
            Utils.applySavedLocaleToActivity(requireActivity());
            reapplyLocalizedTexts();
        } else {
            updateBottomNavUi();
        }
        applyMainWindowUi();
        View view = getView();
        if (view != null) {
            view.post(this::applyMainWindowUi);
        }
        scheduleDefaultHomeRolePromptCheck();
        if (isMainBannerHostVisible()) {
            scheduleMainAdsReload();
        }
    }

    @Override
    public void onPause() {
        stopMainAdsReload();
        super.onPause();
        if (backPressedCallback != null && !(requireActivity() instanceof LauncherHomeActivity)) {
            backPressedCallback.setEnabled(false);
        }
    }

    public void onMainUiShown() {
        applyMainWindowUi();
        scheduleDefaultHomeRolePromptCheck();
        if (isMainBannerHostVisible()) {
            scheduleMainAdsReload();
        }
    }

    private void scheduleDefaultHomeRolePromptCheck() {
        if (!isAdded() || getView() == null || defaultHomePromptCheckPosted) {
            return;
        }
        defaultHomePromptCheckPosted = true;
        getView().post(() -> {
            defaultHomePromptCheckPosted = false;
            maybeShowDefaultHomeRolePrompt();
        });
    }

    private void maybeShowDefaultHomeRolePrompt() {
        if (!isAdded() || getContext() == null || defaultHomeRoleRequestInFlight) {
            return;
        }
        if (!AdPlacement.getDefaultAppPopupShow()) {
            return;
        }
        if (!isMainUiVisible()) {
            return;
        }
        if (Utils.isDefaultHomeApp(requireContext())) {
            return;
        }

        int popupCount = AdPlacement.getDefaultAppPopupCount();
        if (popupCount <= 0) {
            return;
        }

        long intervalMs = DAY_MS / popupCount;
        SharedPreferences prefs = requireContext().getApplicationContext().getSharedPreferences(PREFS_DEFAULT_HOME_POPUP, Context.MODE_PRIVATE);
        long lastShownMs = prefs.getLong(KEY_LAST_DEFAULT_HOME_POPUP_MS, 0L);
        long now = System.currentTimeMillis();
        if (lastShownMs > 0L && (now - lastShownMs) < intervalMs) {
            return;
        }

        if (!launchDefaultHomeRolePrompt()) {
            return;
        }
        prefs.edit().putLong(KEY_LAST_DEFAULT_HOME_POPUP_MS, now).apply();
    }

    private boolean isMainUiVisible() {
        if (!isAdded() || isHidden() || getView() == null || !isResumed()) {
            return false;
        }
        if (requireActivity() instanceof LauncherHomeActivity) {
            return ((LauncherHomeActivity) requireActivity()).isShowingMainFragment();
        }
        return true;
    }

    private boolean launchDefaultHomeRolePrompt() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = requireActivity().getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        return false;
                    }
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
                    if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                        defaultHomeRoleRequestInFlight = true;
                        homeRoleLauncher.launch(intent);
                        return true;
                    }
                }
            }

            Intent homeChooser = new Intent(Intent.ACTION_MAIN);
            homeChooser.addCategory(Intent.CATEGORY_HOME);
            if (homeChooser.resolveActivity(requireActivity().getPackageManager()) != null) {
                defaultHomeRoleRequestInFlight = true;
                homeRoleLauncher.launch(homeChooser);
                return true;
            }
        } catch (Exception exception) {
            defaultHomeRoleRequestInFlight = false;
            return false;
        }
        return false;
    }

    public static void openContactsTab(@Nullable Fragment child) {
        if (child == null) {
            MainActivity.currentTab = "ContactsFragment";
            return;
        }
        Fragment parent = child.getParentFragment();
        if (parent instanceof MainContainerFragment) {
            ((MainContainerFragment) parent).switchToContactsTab();
        } else {
            MainActivity.currentTab = "ContactsFragment";
        }
    }
}