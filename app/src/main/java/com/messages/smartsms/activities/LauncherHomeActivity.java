package com.messages.smartsms.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.google.firebase.FirebaseApp;
import com.messages.smartsms.R;
import com.messages.smartsms.adapters.LauncherPagerAdapter;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.Utils;
import com.messages.smartsms.dialogs.LauncherAppsBottomSheet;
import com.messages.smartsms.fragments.LauncherHomeFragment;
import com.messages.smartsms.fragments.MainContainerFragment;
import com.messages.smartsms.fragments.SubContainerFragment;
import com.messages.smartsms.helpers.RemoteConfigHelper;
import com.messages.smartsms.models.AppsModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherHomeActivity extends AppCompatActivity {
    public static final float SWIPE_AXIS_DOMINANCE_RATIO = 1.2f;
    private static final long REMOTE_CONFIG_INTERVAL = 12 * 60 * 60 * 1000L;
    private static final String PREFS_LAUNCHER_REMOTE = "launcher_remote_config";
    private static final String KEY_LAST_REMOTE_FETCH_TIME = "last_remote_fetch_time";
    private static final String STATE_SHOWING_MAIN = "state_showing_main";

    private ConnectivityManager.NetworkCallback networkCallback;

    public static Dialog appsFolderDialog;
    public static List<AppsModel> arrayListApps = Collections.synchronizedList(new ArrayList<>());
    public static List<AppsModel> arrayListAppsSearch = Collections.synchronizedList(new ArrayList<>());

    private ViewPager2 vpLauncher;
    private LauncherPagerAdapter pagerAdapter;
    private boolean showingMainFragment;
    private boolean skipRightSwipeAd;
    private boolean isPageChangeFromCode;
    private boolean pendingApplyMainTab;
    private boolean pendingRightSwipeMessagesOpen;
    private boolean pendingMessagesShortcutOpen;
    private boolean pendingApplyMainAdsReloadInterval;
    private boolean isPagerAnimating;
    private boolean skipNextIdleWindowUi;
    private boolean suppressHomeRefresh;
    private boolean pagerInitialized;
    private int lastSettledPagerPage = LauncherPagerAdapter.PAGE_HOME;

    private int touchSlop;
    private float touchDownX;
    private float touchDownY;
    private float lastDragX;
    private boolean swipeGestureResolved;
    private boolean isHorizontalSwipeGesture;
    private boolean blockChildTouchForSwipe;
    private boolean preparedUserSwipeToMain;

    private final ExecutorService backgroundTaskExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Utils.wrapContext(newBase));
    }

    private boolean handlingAfterDefaultFlow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (shouldHandleAfterDefaultSetup()) {
            completeAfterDefaultSetupWithAd();
            return;
        }
        setContentView(R.layout.activity_launcher_home);

        FirebaseApp.initializeApp(this);
        registerNetworkCallback();

        try {
            it();
        } catch (Exception e) {
            e.printStackTrace();
        }

        AdPlacement.gatherConsent(this, error -> getRemoteData());

        if (savedInstanceState != null) {
            showingMainFragment = savedInstanceState.getBoolean(STATE_SHOWING_MAIN, false);
        }

        setupViewPager();
        setupBackPress();
        handleLauncherHomeIntent(getIntent());

        if (showingMainFragment) {
            applyMainWindowUi();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SHOWING_MAIN, showingMainFragment);
    }

    private void setupViewPager() {
        if (pagerInitialized) {
            return;
        }
        pagerInitialized = true;

        vpLauncher = findViewById(R.id.vpLauncher);
        pagerAdapter = new LauncherPagerAdapter(this);
        vpLauncher.setSaveEnabled(false);
        vpLauncher.setAdapter(pagerAdapter);
        vpLauncher.setOffscreenPageLimit(2);
        vpLauncher.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        vpLauncher.setOverScrollMode(View.OVER_SCROLL_NEVER);
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        int startPage = showingMainFragment ? LauncherPagerAdapter.PAGE_MAIN : LauncherPagerAdapter.PAGE_HOME;
        lastSettledPagerPage = startPage;
        isPageChangeFromCode = true;
        vpLauncher.setCurrentItem(startPage, false);
        vpLauncher.setUserInputEnabled(false);

        vpLauncher.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                showingMainFragment = position == LauncherPagerAdapter.PAGE_MAIN;
                if (showingMainFragment) {
                    applyMainWindowUi();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING || state == ViewPager2.SCROLL_STATE_SETTLING) {
                    isPagerAnimating = true;
                    return;
                }

                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    isPagerAnimating = false;
                    boolean wasFromCode = isPageChangeFromCode;
                    isPageChangeFromCode = false;

                    int position = vpLauncher.getCurrentItem();
                    boolean isMain = position == LauncherPagerAdapter.PAGE_MAIN;
                    boolean enteredSubPage = position == LauncherPagerAdapter.PAGE_SUB && lastSettledPagerPage != LauncherPagerAdapter.PAGE_SUB;
                    lastSettledPagerPage = position;
                    showingMainFragment = isMain;
                    vpLauncher.setUserInputEnabled(false);

                    if (skipNextIdleWindowUi) {
                        skipNextIdleWindowUi = false;
                        if (isMain) {
                            applyMainWindowUi();
                            onMainPageOpened(!wasFromCode);
                        }
                        preparedUserSwipeToMain = false;
                        return;
                    }

                    if (isMain) {
                        applyMainWindowUi();
                        onMainPageOpened(!wasFromCode);
                    } else {
                        if (preparedUserSwipeToMain) {
                            pendingApplyMainTab = false;
                            pendingRightSwipeMessagesOpen = false;
                        }
                        applyLauncherWindowUi();
                    }
                    preparedUserSwipeToMain = false;
                }
            }
        });
    }

    private void onMainPageOpened(boolean fromUserSwipe) {
        if (fromUserSwipe) {
            completeRightSwipeTutorialIfNeeded();
        }

        MainContainerFragment mainFragment = findMainContainerFragment();
        boolean reloadListBanner = pendingRightSwipeMessagesOpen || pendingMessagesShortcutOpen;
        boolean applyReloadInterval = fromUserSwipe || pendingApplyMainAdsReloadInterval;
        pendingMessagesShortcutOpen = false;
        pendingApplyMainAdsReloadInterval = false;
        if (pendingApplyMainTab || Utils.isFromContacts || pendingRightSwipeMessagesOpen) {
            boolean openMessagesFromSwipe = pendingRightSwipeMessagesOpen;
            pendingRightSwipeMessagesOpen = false;
            pendingApplyMainTab = false;
            if (mainFragment != null) {
                if (openMessagesFromSwipe) {
                    mainFragment.showMessagesTabDefault();
                } else {
                    mainFragment.applyInitialTabFromHome();
                }
            }
        }

        if (mainFragment != null) {
            mainFragment.reapplyLocalizedTexts();
            mainFragment.reloadMainAds(applyReloadInterval, reloadListBanner);
            mainFragment.onMainUiShown();
        }

        if (fromUserSwipe && !skipRightSwipeAd && AdPlacement.getRightSwipeInterstitialAdShow()) {
            AdPlacement.loadRightSwipeInterstitialAd(this, AdPlacement.getOtherInterstitialId(), () -> {
            });
        }
        skipRightSwipeAd = false;
    }

    private void completeRightSwipeTutorialIfNeeded() {
        Fragment homeFragment = findPagerFragment(LauncherPagerAdapter.PAGE_HOME);
        if (homeFragment instanceof LauncherHomeFragment) {
            ((LauncherHomeFragment) homeFragment).onRightSwipeNavigationCompleted();
            return;
        }
        if (homeFragment != null && homeFragment.getView() != null) {
            View tutorial = homeFragment.getView().findViewById(R.id.llRightSwipe);
            View dateTime = homeFragment.getView().findViewById(R.id.llDateTime);
            if (tutorial != null) {
                tutorial.setVisibility(View.GONE);
            }
            if (dateTime != null) {
                dateTime.setVisibility(View.VISIBLE);
            }
            Utils.setRightSwipeTutorialShown(this, true);
        }
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isOnMainPage() || isOnSubPage()) {
                    showLauncherHomeFragment();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLauncherHomeIntent(intent);
    }

    private void handleLauncherHomeIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        boolean isHomeIntent = Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_HOME);
        if (!isHomeIntent) {
            return;
        }
        dismissAppsOverlays();
        if (isOnMainPage() || isOnSubPage()) {
            returnToLauncherHome(true);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        dismissAppsFolderDialog();
    }

    public void dismissAppsOverlays() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LauncherAppsBottomSheet.TAG);
        if (fragment instanceof LauncherAppsBottomSheet) {
            LauncherAppsBottomSheet sheet = (LauncherAppsBottomSheet) fragment;
            if (sheet.isAdded()) {
                sheet.requestAnimatedDismiss();
            }
        }
        dismissAppsFolderDialog();
    }

    private void dismissAppsFolderDialog() {
        if (appsFolderDialog != null && appsFolderDialog.isShowing()) {
            try {
                appsFolderDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isShowingMainFragment() {
        return isOnMainPage();
    }

    public boolean isShowingSubFragment() {
        return isOnSubPage();
    }

    public boolean shouldSuppressHomeRefresh() {
        return suppressHomeRefresh;
    }

    private boolean isOnMainPage() {
        if (vpLauncher != null) {
            return vpLauncher.getCurrentItem() == LauncherPagerAdapter.PAGE_MAIN;
        }
        return showingMainFragment;
    }

    private boolean isOnHomePage() {
        return vpLauncher != null && vpLauncher.getCurrentItem() == LauncherPagerAdapter.PAGE_HOME;
    }

    private boolean isOnSubPage() {
        return vpLauncher != null && vpLauncher.getCurrentItem() == LauncherPagerAdapter.PAGE_SUB;
    }

    public void showMainFragment(boolean openContacts) {
        if (vpLauncher == null) {
            return;
        }

        if (openContacts) {
            Utils.isFromContacts = true;
            MainActivity.currentTab = "ContactsFragment";
            skipRightSwipeAd = true;
            pendingApplyMainTab = true;
            pendingRightSwipeMessagesOpen = false;
            pendingMessagesShortcutOpen = false;
        } else {
            Utils.isFromContacts = false;
            pendingRightSwipeMessagesOpen = false;
            if (MainActivity.currentTab == null || MainActivity.currentTab.isEmpty()) {
                MainActivity.currentTab = "MessagesFragment";
            }
        }

        if (isOnMainPage()) {
            MainContainerFragment mainFragment = findMainContainerFragment();
            if (mainFragment != null) {
                if (openContacts) {
                    mainFragment.prepareContactsOpen();
                    mainFragment.reloadMainAds(true, false);
                } else {
                    mainFragment.prepareFreshMessagesOpen();
                    mainFragment.reloadMainAds(true, true);
                }
            }
            return;
        }

        if (isPagerAnimating) {
            return;
        }

        resetSwipeTouchState();
        if (vpLauncher.isFakeDragging() || vpLauncher.getScrollState() != ViewPager2.SCROLL_STATE_IDLE) {
            vpLauncher.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    showMainFragment(openContacts);
                }
            });
            return;
        }

        isPageChangeFromCode = true;
        showingMainFragment = true;
        pendingApplyMainTab = true;
        pendingApplyMainAdsReloadInterval = true;
        preparedUserSwipeToMain = false;
        if (!openContacts) {
            pendingMessagesShortcutOpen = true;
        }

        MainContainerFragment preparingMain = findMainContainerFragment();
        if (preparingMain != null) {
            if (openContacts) {
                preparingMain.prepareContactsOpen();
            } else {
                preparingMain.prepareFreshMessagesOpen();
            }
        }

        applyMainWindowUi();
        Utils.clearWindowFocusSafely(this);
        vpLauncher.setCurrentItem(LauncherPagerAdapter.PAGE_MAIN, true);
    }

    public void showLauncherHomeFragment() {
        returnToLauncherHome(false);
    }

    private void returnToLauncherHome(boolean fromSystemHome) {
        if (vpLauncher == null) {
            return;
        }

        if (isOnHomePage()) {
            showingMainFragment = false;
            return;
        }

        resetSwipeTouchState();

        if (vpLauncher.isFakeDragging() || vpLauncher.getScrollState() != ViewPager2.SCROLL_STATE_IDLE) {
            vpLauncher.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    returnToLauncherHome(fromSystemHome);
                }
            });
            return;
        }

        isPageChangeFromCode = true;
        showingMainFragment = false;
        skipNextIdleWindowUi = fromSystemHome;
        suppressHomeRefresh = fromSystemHome;
        isPagerAnimating = false;
        Utils.clearWindowFocusSafely(this);

        if (fromSystemHome) {
            vpLauncher.setCurrentItem(LauncherPagerAdapter.PAGE_HOME, false);
            applyLauncherWindowUi();
            vpLauncher.post(() -> {
                isPageChangeFromCode = false;
                isPagerAnimating = false;
                suppressHomeRefresh = false;
                skipNextIdleWindowUi = false;
            });
            return;
        }

        vpLauncher.setCurrentItem(LauncherPagerAdapter.PAGE_HOME, true);
    }

    private void prepareUserSwipeToMain() {
        if (preparedUserSwipeToMain) {
            return;
        }
        preparedUserSwipeToMain = true;
        Utils.isFromContacts = false;
        MainActivity.currentTab = "MessagesFragment";
        skipRightSwipeAd = false;
        pendingApplyMainTab = true;
        pendingRightSwipeMessagesOpen = true;
        pendingMessagesShortcutOpen = false;
        isPageChangeFromCode = false;

        MainContainerFragment mainFragment = findMainContainerFragment();
        if (mainFragment != null) {
            mainFragment.prepareFreshMessagesOpen();
        }
    }

    private boolean isAppsOverlayShowing() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LauncherAppsBottomSheet.TAG);
        if (fragment != null && fragment.isAdded()) {
            return true;
        }
        return appsFolderDialog != null && appsFolderDialog.isShowing();
    }

    private void resetSwipeTouchState() {
        swipeGestureResolved = false;
        isHorizontalSwipeGesture = false;
        if (vpLauncher != null && vpLauncher.isFakeDragging()) {
            try {
                vpLauncher.endFakeDrag();
            } catch (Exception ignored) {
            }
        }
    }

    private void cancelChildTouchTargets(MotionEvent ev) {
        MotionEvent cancelEvent = MotionEvent.obtain(ev);
        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
        super.dispatchTouchEvent(cancelEvent);
        cancelEvent.recycle();
    }

    private void handleHomePageSwipeTouch(MotionEvent ev) {
        if (vpLauncher == null || pagerAdapter == null || isAppsOverlayShowing()) {
            return;
        }

        boolean inProgressSwipe = isHorizontalSwipeGesture || vpLauncher.isFakeDragging();
        if (isOnMainPage() && !inProgressSwipe) {
            return;
        }

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (isOnMainPage()) {
                    return;
                }
                touchDownX = ev.getX();
                touchDownY = ev.getY();
                lastDragX = touchDownX;
                swipeGestureResolved = false;
                isHorizontalSwipeGesture = false;
                blockChildTouchForSwipe = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isPagerAnimating && !vpLauncher.isFakeDragging()) {
                    return;
                }

                float dxFromDown = ev.getX() - touchDownX;
                float dyFromDown = ev.getY() - touchDownY;

                if (!swipeGestureResolved) {
                    if (Math.abs(dxFromDown) < touchSlop && Math.abs(dyFromDown) < touchSlop) {
                        return;
                    }
                    swipeGestureResolved = true;
                    boolean isHorizontal = Math.abs(dxFromDown) > Math.abs(dyFromDown) * SWIPE_AXIS_DOMINANCE_RATIO;
                    int currentPage = vpLauncher.getCurrentItem();
                    boolean canSwipeRight = dxFromDown > 0 && currentPage > LauncherPagerAdapter.PAGE_MAIN;
                    boolean canSwipeLeft = dxFromDown < 0 && currentPage < pagerAdapter.getItemCount() - 1;
                    if (currentPage == LauncherPagerAdapter.PAGE_MAIN) {
                        canSwipeLeft = false;
                        canSwipeRight = false;
                    }
                    isHorizontalSwipeGesture = isHorizontal && (canSwipeRight || canSwipeLeft);
                    if (isHorizontalSwipeGesture && !vpLauncher.isFakeDragging()) {
                        boolean started;
                        try {
                            started = vpLauncher.beginFakeDrag();
                        } catch (Exception ignored) {
                            started = false;
                        }
                        if (started) {
                            if (canSwipeRight && currentPage == LauncherPagerAdapter.PAGE_HOME) {
                                prepareUserSwipeToMain();
                            }
                            try {
                                vpLauncher.fakeDragBy(dxFromDown);
                            } catch (Exception ignored) {
                            }
                            lastDragX = ev.getX();
                        } else {
                            isHorizontalSwipeGesture = false;
                        }
                    }
                }

                if (isHorizontalSwipeGesture && vpLauncher.isFakeDragging()) {
                    float dragDx = ev.getX() - lastDragX;
                    if (dragDx != 0f) {
                        try {
                            vpLauncher.fakeDragBy(dragDx);
                        } catch (Exception ignored) {
                        }
                    }
                    lastDragX = ev.getX();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetSwipeTouchState();
                break;

            default:
                break;
        }
    }

    public boolean isHorizontalPageSwipeInProgress() {
        return isHorizontalSwipeGesture || (vpLauncher != null && vpLauncher.isFakeDragging());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            blockChildTouchForSwipe = false;
        }

        boolean trackHomeSwipe = !isOnMainPage() || isHorizontalSwipeGesture || (vpLauncher != null && vpLauncher.isFakeDragging());
        boolean wasHorizontalSwipe = isHorizontalSwipeGesture;
        if (trackHomeSwipe) {
            handleHomePageSwipeTouch(ev);
        }

        boolean appsSheetDragging = false;
        if (isOnHomePage()) {
            Fragment homeFragment = findPagerFragment(LauncherPagerAdapter.PAGE_HOME);
            if (homeFragment instanceof LauncherHomeFragment) {
                LauncherHomeFragment launcherHomeFragment = (LauncherHomeFragment) homeFragment;
                boolean horizontalPageSwipe = isHorizontalPageSwipeInProgress();
                if (!horizontalPageSwipe || launcherHomeFragment.isAppsSheetDragging()) {
                    appsSheetDragging = launcherHomeFragment.onHostTouchEvent(ev);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    launcherHomeFragment.onHostTouchEvent(ev);
                }
            }
        }

        if ((isHorizontalSwipeGesture && !wasHorizontalSwipe) || (appsSheetDragging && !blockChildTouchForSwipe)) {
            blockChildTouchForSwipe = true;
            cancelChildTouchTargets(ev);
            return true;
        }

        if (blockChildTouchForSwipe || isHorizontalSwipeGesture || appsSheetDragging) {
            blockChildTouchForSwipe = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL;
            return true;
        }

        return super.dispatchTouchEvent(ev);
    }

    @Nullable
    private MainContainerFragment findMainContainerFragment() {
        Fragment fragment = findPagerFragment(LauncherPagerAdapter.PAGE_MAIN);
        if (fragment instanceof MainContainerFragment) {
            return (MainContainerFragment) fragment;
        }
        return null;
    }

    @Nullable
    private SubContainerFragment findSubContainerFragment() {
        Fragment fragment = findPagerFragment(LauncherPagerAdapter.PAGE_SUB);
        if (fragment instanceof SubContainerFragment) {
            return (SubContainerFragment) fragment;
        }
        return null;
    }

    @Nullable
    private Fragment findPagerFragment(int position) {
        if (pagerAdapter == null) {
            return null;
        }
        return getSupportFragmentManager().findFragmentByTag("f" + pagerAdapter.getItemId(position));
    }

    private void applyMainWindowUi() {
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.white));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        MainContainerFragment mainFragment = findMainContainerFragment();
        if (mainFragment != null) {
            mainFragment.applyMainWindowUi();
        }
    }

    private void applyLauncherWindowUi() {
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.navigationBars());
            }
        }

        Fragment homeFragment = findPagerFragment(LauncherPagerAdapter.PAGE_HOME);
        if (homeFragment instanceof LauncherHomeFragment && homeFragment.getView() != null) {
            ((LauncherHomeFragment) homeFragment).refreshWallpaperStatusBar();
        }
    }

    private void registerNetworkCallback() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> maybeFetchRemoteDataFromNetwork());
            }

            @Override
            public void onLost(@NonNull Network network) {
            }
        };

        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    private void getRemoteData() {
        RemoteConfigHelper.fetchRemoteConfig(this, this::saveRemoteFetchTimestampIfSuccessful);
    }

    private void maybeFetchRemoteDataFromNetwork() {
        if (isRemoteConfigFetchWithinInterval()) {
            return;
        }
        getRemoteData();
    }

    private boolean isRemoteConfigFetchWithinInterval() {
        SharedPreferences preferences = getSharedPreferences(PREFS_LAUNCHER_REMOTE, MODE_PRIVATE);
        long lastFetchTime = preferences.getLong(KEY_LAST_REMOTE_FETCH_TIME, 0L);
        return System.currentTimeMillis() - lastFetchTime < REMOTE_CONFIG_INTERVAL;
    }

    private void saveRemoteFetchTimestampIfSuccessful(boolean success) {
        if (!success) {
            return;
        }
        getSharedPreferences(PREFS_LAUNCHER_REMOTE, MODE_PRIVATE).edit().putLong(KEY_LAST_REMOTE_FETCH_TIME, System.currentTimeMillis()).apply();
    }

    private void it() {
        Uri referrer = getReferrer();
        if (referrer != null && getPreferences(MODE_PRIVATE).getBoolean(referrer.toString(), false)) {
            return;
        }

        InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(this).build();
        backgroundTaskExecutor.execute(() -> getInstallReferrerFromClient(referrerClient));
    }

    public void getInstallReferrerFromClient(InstallReferrerClient referrerClient) {
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    try {
                        ReferrerDetails response = referrerClient.getInstallReferrer();
                        String referrerUrl = response.getInstallReferrer();

                        AdPlacement.setReferrerUrl(LauncherHomeActivity.this, referrerUrl);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    referrerClient.endConnection();
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {

            }
        });
    }

    private boolean shouldHandleAfterDefaultSetup() {
        return Utils.isCompletingDefaultAppSetup(this) && Utils.isDefaultHomeApp(this);
    }

    private void completeAfterDefaultSetupWithAd() {
        if (handlingAfterDefaultFlow) {
            return;
        }
        if (!shouldHandleAfterDefaultSetup()) {
            return;
        }

        overridePendingTransition(0, 0);
        View blank = new View(this);
        blank.setBackgroundColor(android.graphics.Color.BLACK);
        setContentView(blank);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.black);
        }

        if (!Utils.tryClaimAfterDefaultFlow(this)) {
            return;
        }

        Utils.setAwaitingDefaultRoleResult(this, false);
        handlingAfterDefaultFlow = true;
        Runnable continueSetup = () -> {
            handlingAfterDefaultFlow = false;
            Utils.navigateAfterDefaultAppSetup(this);
        };

        if (!AdPlacement.getAfterDefaultAdShow()) {
            continueSetup.run();
            return;
        }

        AdPlacement.loadAfterDefaultAd(this, continueSetup::run);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldHandleAfterDefaultSetup()) {
            completeAfterDefaultSetupWithAd();
            return;
        }
        if (Utils.isLocaleChangedPending()) {
            Utils.applySavedLocaleToActivity(this);
            MainContainerFragment mainFragment = findMainContainerFragment();
            if (mainFragment != null && mainFragment.isAdded()) {
                mainFragment.reapplyLocalizedTexts();
            }
        }
        if (isOnMainPage()) {
            applyMainWindowUi();
        } else if (isOnSubPage()) {
            applyLauncherWindowUi();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        backgroundTaskExecutor.shutdown();
        SubContainerFragment.clearCachedNativeAd();
    }
}