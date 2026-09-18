package com.messages.smartsms.dialogs;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.messages.smartsms.R;
import com.messages.smartsms.activities.LauncherHomeActivity;
import com.messages.smartsms.activities.LauncherSettingsActivity;
import com.messages.smartsms.adapters.AppsAdapter;
import com.messages.smartsms.adapters.FixedViewAdapter;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.Utils;
import com.messages.smartsms.helpers.DefaultHomePromptHelper;
import com.messages.smartsms.interfaces.OnAppsUninstallClickListener;
import com.messages.smartsms.models.AppsModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherAppsBottomSheet extends BottomSheetDialogFragment implements OnAppsUninstallClickListener {
    public static final String TAG = "LauncherAppsBottomSheet";
    private static final String ARG_INTERACTIVE_OPEN = "interactive_open";
    private static final float SHEET_DIM_AMOUNT = 0.25f;
    private static final int SETTLE_MIN_MS = 160;
    private static final int SETTLE_MAX_MS = 280;
    private static final int EXPANDED_SHEET_COLOR = Color.WHITE;

    private LinearLayout llRecentApps, llDefault;
    private AppCompatImageView ivMore, ivDivider2;
    private AppCompatEditText etSearch;
    private RecyclerView rvRecentApps, rvApps;
    private AppCompatTextView btnSetNow, tvNoApps, tvMoreApps;
    private final DefaultHomePromptHelper defaultHomePromptHelper = new DefaultHomePromptHelper(this);
    private RelativeLayout rlBannerAdView;
    private ShimmerFrameLayout slBannerShimmer;
    private LinearLayout llBannerAd;
    private View headerView;
    private View footerView;
    private View contentRoot;
    private View statusBarScrim;

    private final ArrayList<AppsModel> arrayListRecentApps = new ArrayList<>();
    private AppsAdapter recentAppsAdapter;
    private AppsAdapter appsAdapter;
    private FixedViewAdapter headerAdapter;
    private FixedViewAdapter footerAdapter;
    private ConcatAdapter concatAdapter;

    private int rvAppsDefaultMarginTop;
    private static boolean bannerAdLoadInProgress;
    private AdView bannerAdView;
    private boolean receiverRegistered;
    private boolean appsShown;
    private boolean enrichmentDone;
    private boolean enrichScheduled;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private BottomSheetBehavior<FrameLayout> sheetBehavior;
    private FrameLayout bottomSheetView;
    private boolean interactiveOpen;
    private boolean interactiveSessionActive;
    private float interactiveProgress;

    private boolean hostSystemBarsSaved;
    private int savedHostStatusBarColor;
    private int savedHostNavigationBarColor;
    private boolean savedHostLightStatusBars;
    private boolean savedHostLightNavigationBars;
    private boolean expandedSystemBarsApplied;
    private boolean windowInsetsListenerAttached;
    private int contentRootDefaultPaddingTop;
    private int appliedContentPaddingTop = -1;
    private int appliedContentPaddingBottom = -1;
    private int appliedRecyclerPaddingBottom = -1;
    private int appliedStatusBarScrimHeight = -1;
    private int appliedStatusBarScrimColor = Color.TRANSPARENT;
    private boolean animatedDismissRequested;
    private OnBackPressedCallback backDismissCallback;

    private final BottomSheetBehavior.BottomSheetCallback sheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (interactiveSessionActive) {
                return;
            }
            if (newState == BottomSheetBehavior.STATE_EXPANDED || newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                applyExpandedSystemBarAppearance();
                postApplyExpandedSystemBarAppearance();
                scheduleEnrichment();
            } else if (newState == BottomSheetBehavior.STATE_DRAGGING || newState == BottomSheetBehavior.STATE_SETTLING) {
                clearExpandedSystemBarAppearance();
            }
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                animatedDismissRequested = false;
                clearExpandedSystemBarAppearance();
                dismissAllowingStateLoss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            if (interactiveSessionActive) {
                return;
            }
            if (slideOffset >= 0.999f) {
                applyExpandedSystemBarAppearance();
            } else if (slideOffset < 0.85f) {
                clearExpandedSystemBarAppearance();
            }
        }
    };

    public static LauncherAppsBottomSheet newInstance() {
        return newInstance(false);
    }

    public static LauncherAppsBottomSheet newInstance(boolean interactiveOpen) {
        LauncherAppsBottomSheet sheet = new LauncherAppsBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_INTERACTIVE_OPEN, interactiveOpen);
        sheet.setArguments(args);
        return sheet;
    }

    private boolean isFragmentReady() {
        return isAdded() && getActivity() != null;
    }

    private boolean hasContext() {
        return isAdded() && getContext() != null;
    }

    public boolean isInteractiveSessionActive() {
        return interactiveSessionActive;
    }

    public void setInitialInteractiveProgress(float progress) {
        interactiveProgress = clamp01(progress);
    }

    public void updateInteractiveProgress(float progress) {
        if (!interactiveSessionActive && !interactiveOpen) {
            return;
        }
        interactiveProgress = clamp01(progress);
        if (interactiveSessionActive) {
            applyInteractiveProgress(interactiveProgress);
        }
    }

    public void endInteractiveSession(float velocityY) {
        if (!interactiveSessionActive || !isAdded()) {
            return;
        }
        interactiveSessionActive = false;
        clearInteractiveWindowFlags();

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(true);
        }

        boolean expand;
        if (Math.abs(velocityY) >= 1000f) {
            expand = velocityY < 0f;
        } else {
            expand = interactiveProgress >= 0.5f;
        }

        if (sheetBehavior != null) {
            sheetBehavior.setDraggable(true);
        }

        if (expand) {
            settleInteractiveExpand(velocityY);
        } else {
            settleInteractiveDismiss(velocityY);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        interactiveOpen = args != null && args.getBoolean(ARG_INTERACTIVE_OPEN, false);
        if (savedInstanceState != null) {
            interactiveOpen = false;
        }
        setStyle(STYLE_NORMAL, R.style.LauncherAppsBottomSheetTheme);
        setCancelable(true);
        defaultHomePromptHelper.registerRoleLauncher();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), getTheme()) {
            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                if (interactiveOpen) {
                    Window window = getWindow();
                    if (window != null) {
                        window.setWindowAnimations(0);
                        window.setDimAmount(0f);
                        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                }
            }
        };
        dialog.setCanceledOnTouchOutside(!interactiveOpen);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }
        FrameLayout bottomSheet = ((BottomSheetDialog) dialog).findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }

        bottomSheetView = bottomSheet;
        ensureStatusBarScrim();
        bottomSheet.setBackgroundResource(android.R.color.transparent);
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheet.setLayoutParams(layoutParams);

        sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setFitToContents(false);
        sheetBehavior.setExpandedOffset(0);
        sheetBehavior.setSkipCollapsed(true);
        sheetBehavior.setHideable(true);
        sheetBehavior.removeBottomSheetCallback(sheetCallback);
        sheetBehavior.addBottomSheetCallback(sheetCallback);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        if (interactiveOpen) {
            interactiveSessionActive = true;
            sheetBehavior.setDraggable(false);
            prepareInteractiveWindow(dialog);
            float seedTy = getResources().getDisplayMetrics().heightPixels;
            bottomSheet.setTranslationY(seedTy);
            bottomSheet.post(() -> {
                if (!isAdded() || !interactiveSessionActive) {
                    return;
                }
                applyInteractiveProgress(interactiveProgress);
            });
        } else {
            sheetBehavior.setDraggable(true);
            configureExpandedWindow(dialog);
            applyExpandedSystemBarAppearance();
            postApplyExpandedSystemBarAppearance();
            mainHandler.postDelayed(this::scheduleEnrichment, 280);
        }
        setupBackDismissHandler(dialog);
    }

    private void setupBackDismissHandler(@NonNull Dialog dialog) {
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }
        if (backDismissCallback != null) {
            backDismissCallback.remove();
        }
        backDismissCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requestAnimatedDismiss();
            }
        };
        ((BottomSheetDialog) dialog).getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backDismissCallback);
    }

    public void requestAnimatedDismiss() {
        if (!isAdded() || animatedDismissRequested) {
            return;
        }
        Dialog dialog = getDialog();
        if (dialog == null || !dialog.isShowing()) {
            return;
        }

        prepareDismissUI();

        if (sheetBehavior != null && !interactiveSessionActive) {
            int state = sheetBehavior.getState();
            if (state == BottomSheetBehavior.STATE_HIDDEN || state == BottomSheetBehavior.STATE_COLLAPSED) {
                dismissAllowingStateLoss();
                return;
            }
            animatedDismissRequested = true;
            sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }

        dismissAllowingStateLoss();
    }

    private void prepareDismissUI() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            Utils.clearWindowFocusSafely(dialog.getWindow());
        }
        if (etSearch == null) {
            return;
        }
        Context context = getContext();
        if (context != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null && etSearch.getWindowToken() != null) {
                inputMethodManager.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            }
        }
        etSearch.clearFocus();
    }

    private void configureExpandedWindow(@NonNull Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            window.setDimAmount(SHEET_DIM_AMOUNT);
        }
    }

    private void applyExpandedSystemBarAppearance() {
        if (!isAdded() || interactiveSessionActive || expandedSystemBarsApplied) {
            return;
        }
        if (bottomSheetView != null && bottomSheetView.getTranslationY() > 0f) {
            return;
        }
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        applyExpandedDialogSystemUI(window);
        applyBottomSheetSurface(bottomSheetView, EXPANDED_SHEET_COLOR);
        applyExpandedWindowInsets(EXPANDED_SHEET_COLOR);
        if (!hostSystemBarsSaved) {
            saveHostSystemBarState();
        }
        applyHostSystemBars();
        expandedSystemBarsApplied = true;
    }

    private void postApplyExpandedSystemBarAppearance() {
        View anchor = bottomSheetView != null ? bottomSheetView : contentRoot;
        if (anchor == null) {
            return;
        }
        anchor.post(() -> {
            if (isFullyExpanded()) {
                applyExpandedSystemBarAppearance();
            }
        });
    }

    private void clearExpandedSystemBarAppearance() {
        clearGestureDialogSystemUI();
        clearExpandedWindowInsets();
        if (expandedSystemBarsApplied) {
            restoreHostSystemBarState();
            expandedSystemBarsApplied = false;
        }
    }

    private void ensureStatusBarScrim() {
        if (statusBarScrim != null || bottomSheetView == null || getContext() == null) {
            return;
        }
        statusBarScrim = new View(requireContext());
        statusBarScrim.setVisibility(GONE);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        layoutParams.gravity = Gravity.TOP;
        bottomSheetView.addView(statusBarScrim, 0, layoutParams);
    }

    private void applyExpandedDialogSystemUI(@NonNull Window window) {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(EXPANDED_SHEET_COLOR);
        window.setBackgroundDrawable(new ColorDrawable(EXPANDED_SHEET_COLOR));
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        updateStatusBarScrimIfNeeded(EXPANDED_SHEET_COLOR, resolveStatusBarHeight(window));
    }

    private void clearGestureDialogSystemUI() {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        Utils.clearWindowFocusSafely(window);
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        updateStatusBarScrimIfNeeded(Color.TRANSPARENT, 0);
    }

    private void applyExpandedWindowInsets(@ColorInt int statusBarColor) {
        if (bottomSheetView == null || contentRoot == null) {
            return;
        }
        if (!windowInsetsListenerAttached) {
            windowInsetsListenerAttached = true;
            ViewCompat.setOnApplyWindowInsetsListener(bottomSheetView, (view, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                updateStatusBarScrimIfNeeded(statusBarColor, statusBarHeight);
                applyContentInsetsIfNeeded(statusBarHeight, navigationBarHeight);
                applyRecyclerImePaddingIfNeeded(imeInsets.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(bottomSheetView);
        }
    }

    private void applyContentInsetsIfNeeded(int statusBarHeight, int navigationBarHeight) {
        if (contentRoot == null) {
            return;
        }
        int paddingTop = contentRootDefaultPaddingTop + statusBarHeight;
        if (appliedContentPaddingTop == paddingTop && appliedContentPaddingBottom == navigationBarHeight) {
            return;
        }
        appliedContentPaddingTop = paddingTop;
        appliedContentPaddingBottom = navigationBarHeight;
        contentRoot.setPadding(contentRoot.getPaddingLeft(), paddingTop, contentRoot.getPaddingRight(), navigationBarHeight);
    }

    private void applyRecyclerImePaddingIfNeeded(int imeBottom) {
        if (rvApps == null) {
            return;
        }
        int paddingBottom = Math.max(imeBottom, 0);
        if (appliedRecyclerPaddingBottom == paddingBottom) {
            return;
        }
        appliedRecyclerPaddingBottom = paddingBottom;
        rvApps.setPadding(rvApps.getPaddingLeft(), rvApps.getPaddingTop(), rvApps.getPaddingRight(), paddingBottom);
    }

    private void updateStatusBarScrimIfNeeded(@ColorInt int color, int height) {
        if (statusBarScrim == null) {
            return;
        }
        int safeHeight = Math.max(height, 0);
        if (appliedStatusBarScrimHeight == safeHeight && appliedStatusBarScrimColor == color) {
            return;
        }
        appliedStatusBarScrimHeight = safeHeight;
        appliedStatusBarScrimColor = color;
        ViewGroup.LayoutParams layoutParams = statusBarScrim.getLayoutParams();
        layoutParams.height = safeHeight;
        statusBarScrim.setLayoutParams(layoutParams);
        statusBarScrim.setBackgroundColor(color);
        statusBarScrim.setVisibility(safeHeight > 0 && Color.alpha(color) > 0 ? VISIBLE : GONE);
    }

    private void clearExpandedWindowInsets() {
        if (bottomSheetView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomSheetView, null);
        }
        windowInsetsListenerAttached = false;
        appliedContentPaddingTop = -1;
        appliedContentPaddingBottom = -1;
        appliedRecyclerPaddingBottom = -1;
        appliedStatusBarScrimHeight = -1;
        appliedStatusBarScrimColor = Color.TRANSPARENT;
        if (contentRoot != null) {
            contentRoot.setPadding(contentRoot.getPaddingLeft(), contentRootDefaultPaddingTop, contentRoot.getPaddingRight(), 0);
        }
        if (rvApps != null && rvApps.getPaddingBottom() != 0) {
            rvApps.setPadding(rvApps.getPaddingLeft(), rvApps.getPaddingTop(), rvApps.getPaddingRight(), 0);
        }
        updateStatusBarScrimIfNeeded(Color.TRANSPARENT, 0);
    }

    private static int resolveStatusBarHeight(@NonNull Window window) {
        View decorView = window.getDecorView();
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decorView);
        if (insets != null) {
            return insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
        }
        return 0;
    }

    private boolean isFullyExpanded() {
        return isAdded() && !interactiveSessionActive && !interactiveOpen && bottomSheetView != null && bottomSheetView.getTranslationY() <= 0f;
    }

    private void applyBottomSheetSurface(@NonNull View bottomSheet, @ColorInt int surfaceColor) {
        bottomSheet.setElevation(0f);
        bottomSheet.setOutlineProvider(null);
        bottomSheet.setClipToOutline(false);
        bottomSheet.setBackground(new ColorDrawable(surfaceColor));
        bottomSheet.setBackgroundTintList(null);
    }

    private void applyHostSystemBars() {
        if (getActivity() == null) {
            return;
        }
        Window window = getActivity().getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(EXPANDED_SHEET_COLOR);
        window.setNavigationBarColor(EXPANDED_SHEET_COLOR);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void saveHostSystemBarState() {
        if (hostSystemBarsSaved || getActivity() == null) {
            return;
        }
        Window window = getActivity().getWindow();
        savedHostStatusBarColor = window.getStatusBarColor();
        savedHostNavigationBarColor = window.getNavigationBarColor();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        savedHostLightStatusBars = controller.isAppearanceLightStatusBars();
        savedHostLightNavigationBars = controller.isAppearanceLightNavigationBars();
        hostSystemBarsSaved = true;
    }

    private void restoreHostSystemBarState() {
        if (!hostSystemBarsSaved || getActivity() == null) {
            return;
        }
        Window window = getActivity().getWindow();
        window.setStatusBarColor(savedHostStatusBarColor);
        window.setNavigationBarColor(savedHostNavigationBarColor);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(savedHostLightStatusBars);
        controller.setAppearanceLightNavigationBars(savedHostLightNavigationBars);
        hostSystemBarsSaved = false;
    }

    private void maintainExpandedSheetOnSearchFocus() {
        if (etSearch == null) {
            return;
        }
        etSearch.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus || !isAdded() || interactiveSessionActive || sheetBehavior == null) {
                return;
            }
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            if (bottomSheetView != null) {
                bottomSheetView.setTranslationY(0f);
            }
            Dialog dialog = getDialog();
            if (dialog != null) {
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setDimAmount(SHEET_DIM_AMOUNT);
                }
            }
        });
    }

    private void prepareInteractiveWindow(@NonNull Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setWindowAnimations(0);
        window.setDimAmount(0f);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void clearInteractiveWindowFlags() {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void applyInteractiveProgress(float progress) {
        if (bottomSheetView == null) {
            return;
        }
        int height = bottomSheetView.getHeight();
        if (height <= 0) {
            height = getResources().getDisplayMetrics().heightPixels;
        }
        bottomSheetView.setTranslationY(height * (1f - progress));

        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(SHEET_DIM_AMOUNT * progress);
            }
        }
    }

    private void settleInteractiveExpand(float velocityY) {
        if (bottomSheetView == null) {
            finishInteractiveExpand();
            return;
        }
        bottomSheetView.animate().cancel();
        bottomSheetView.animate().translationY(0f).setDuration(settleDurationMs(bottomSheetView.getTranslationY(), 0f, velocityY)).setInterpolator(new DecelerateInterpolator()).withEndAction(this::finishInteractiveExpand).start();

        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(SHEET_DIM_AMOUNT);
            }
        }
    }

    private void finishInteractiveExpand() {
        if (!isAdded()) {
            return;
        }
        if (bottomSheetView != null) {
            bottomSheetView.setTranslationY(0f);
        }
        interactiveOpen = false;
        if (sheetBehavior != null) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        Dialog dialog = getDialog();
        if (dialog != null && bottomSheetView != null) {
            configureExpandedWindow(dialog);
            applyExpandedSystemBarAppearance();
            postApplyExpandedSystemBarAppearance();
        }
        scheduleEnrichment();
    }

    private void settleInteractiveDismiss(float velocityY) {
        if (bottomSheetView == null) {
            dismissAllowingStateLoss();
            return;
        }
        float height = bottomSheetView.getHeight();
        if (height <= 0f) {
            height = getResources().getDisplayMetrics().heightPixels;
        }
        final float targetTy = height;
        bottomSheetView.animate().cancel();
        bottomSheetView.animate().translationY(targetTy).setDuration(settleDurationMs(bottomSheetView.getTranslationY(), targetTy, velocityY)).setInterpolator(new DecelerateInterpolator()).withEndAction(() -> {
            if (isAdded()) {
                dismissAllowingStateLoss();
            }
        }).start();
    }

    private int settleDurationMs(float fromTy, float toTy, float velocityY) {
        float distance = Math.abs(toTy - fromTy);
        float speed = Math.abs(velocityY);
        if (speed > 1f) {
            int duration = (int) (distance / speed * 1000f);
            return Math.max(SETTLE_MIN_MS, Math.min(SETTLE_MAX_MS, duration));
        }
        return SETTLE_MAX_MS;
    }

    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        return Math.min(value, 1f);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_launcher_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contentRoot = view;
        contentRootDefaultPaddingTop = view.getPaddingTop();
        findIDs(view);
        setupShellUI();
    }

    @SuppressLint("InflateParams")
    private void findIDs(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        ivMore = view.findViewById(R.id.ivMore);
        llDefault = view.findViewById(R.id.llDefault);
        btnSetNow = view.findViewById(R.id.btnSetNow);
        rvApps = view.findViewById(R.id.rvApps);
        captureAppsRecyclerDefaultMargins();
        rlBannerAdView = view.findViewById(R.id.rlBannerAdView);
        slBannerShimmer = view.findViewById(R.id.slBannerShimmer);
        llBannerAd = view.findViewById(R.id.llBannerAd);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        headerView = inflater.inflate(R.layout.bottom_sheet_launcher_apps_header, null, false);
        footerView = inflater.inflate(R.layout.bottom_sheet_launcher_apps_footer, null, false);

        llRecentApps = headerView.findViewById(R.id.llRecentApps);
        rvRecentApps = headerView.findViewById(R.id.rvRecentApps);

        ivDivider2 = footerView.findViewById(R.id.ivDivider2);
        tvNoApps = footerView.findViewById(R.id.tvNoApps);
        tvMoreApps = footerView.findViewById(R.id.tvMoreApps);
    }

    private void setupShellUI() {
        showAd();
        defaultHomePromptHelper.bind(llDefault);
        btnSetNow.setOnClickListener(view -> defaultHomePromptHelper.handleSetAsDefaultClick());

        rvApps.setItemAnimator(null);

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 4);
        layoutManager.setItemPrefetchEnabled(false);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (concatAdapter == null || appsAdapter == null) {
                    return 4;
                }
                int headerCount = isRecentHeaderAttached() ? 1 : 0;
                int appsCount = appsAdapter.getItemCount();
                if (position < headerCount || position >= headerCount + appsCount) {
                    return 4;
                }
                return 1;
            }
        });
        rvApps.setLayoutManager(layoutManager);

        recentAppsAdapter = new AppsAdapter(requireContext(), arrayListRecentApps, this);
        rvRecentApps.setItemAnimator(null);
        rvRecentApps.setNestedScrollingEnabled(false);
        rvRecentApps.setHasFixedSize(false);
        GridLayoutManager recentLayoutManager = new GridLayoutManager(requireContext(), 4) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }

            @Override
            public boolean canScrollHorizontally() {
                return false;
            }
        };
        recentLayoutManager.setItemPrefetchEnabled(false);
        rvRecentApps.setLayoutManager(recentLayoutManager);
        rvRecentApps.setAdapter(recentAppsAdapter);

        showCachedAppsInstantly();
        maintainExpandedSheetOnSearchFocus();

        ivMore.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), LauncherSettingsActivity.class));
            Utils.clearActivityTransition(getActivity());
            mainHandler.postDelayed(this::searchClearView, 100);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!appsShown) {
                    return;
                }
                searchApps(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        tvMoreApps.setOnClickListener(v -> {
            String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + query));
                intent.setPackage("com.android.vending");
                startActivity(intent);
                Utils.clearActivityTransition(getActivity());
            } catch (Exception e) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(query) + "&c=apps"));
                startActivity(intent);
                Utils.clearActivityTransition(getActivity());
            }
            mainHandler.postDelayed(this::searchClearView, 100);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showCachedAppsInstantly() {
        if (appsShown || !isAdded()) {
            return;
        }
        if (LauncherHomeActivity.arrayListAppsSearch.isEmpty()) {
            rvApps.setVisibility(INVISIBLE);
            rvApps.setAlpha(0f);
            return;
        }

        LauncherHomeActivity.arrayListApps.clear();
        LauncherHomeActivity.arrayListApps.addAll(LauncherHomeActivity.arrayListAppsSearch);

        ArrayList<AppsModel> recent = buildRecentApps(requireContext());
        arrayListRecentApps.clear();
        arrayListRecentApps.addAll(recent);
        setRecentAppsVisibility(arrayListRecentApps.isEmpty() ? GONE : VISIBLE);
        recentAppsAdapter.notifyDataSetChanged();

        attachAppsAdapterIfNeeded();
        appsShown = true;
        updateAppsListVisibility();
        if (rvApps.getVisibility() == VISIBLE) {
            rvApps.setAlpha(1f);
        }
    }

    private void captureAppsRecyclerDefaultMargins() {
        if (rvApps == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = rvApps.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
            rvAppsDefaultMarginTop = marginParams.topMargin;
        }
    }

    private void attachAppsAdapterIfNeeded() {
        if (appsAdapter != null || !isAdded()) {
            return;
        }
        appsAdapter = new AppsAdapter(requireContext(), LauncherHomeActivity.arrayListApps, this);
        headerAdapter = new FixedViewAdapter(headerView);
        footerAdapter = new FixedViewAdapter(footerView);
        ConcatAdapter.Config config = new ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build();
        if (shouldShowRecentAppsSection()) {
            concatAdapter = new ConcatAdapter(config, headerAdapter, appsAdapter, footerAdapter);
        } else {
            concatAdapter = new ConcatAdapter(config, appsAdapter, footerAdapter);
        }
        rvApps.setAdapter(concatAdapter);
        applyAppsRecyclerSpacing(isRecentHeaderAttached());
    }

    private boolean isRecentHeaderAttached() {
        return concatAdapter != null && headerAdapter != null && concatAdapter.getAdapters().contains(headerAdapter);
    }

    private void syncRecentHeaderAdapter(boolean showRecent) {
        if (concatAdapter == null || headerAdapter == null) {
            return;
        }
        boolean attached = isRecentHeaderAttached();
        if (showRecent && !attached) {
            concatAdapter.addAdapter(0, headerAdapter);
        } else if (!showRecent && attached) {
            concatAdapter.removeAdapter(headerAdapter);
        }
        RecyclerView.LayoutManager layoutManager = rvApps != null ? rvApps.getLayoutManager() : null;
        if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager.SpanSizeLookup spanSizeLookup = ((GridLayoutManager) layoutManager).getSpanSizeLookup();
            spanSizeLookup.invalidateSpanIndexCache();
            spanSizeLookup.invalidateSpanGroupIndexCache();
        }
        if (rvApps != null) {
            rvApps.getRecycledViewPool().clear();
            rvApps.requestLayout();
        }
    }

    private boolean shouldShowRecentAppsSection() {
        return !arrayListRecentApps.isEmpty() && !isSearchActive();
    }

    private void applyAppsRecyclerSpacing(boolean recentSectionVisible) {
        if (rvApps == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = rvApps.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int targetTopMargin = recentSectionVisible ? rvAppsDefaultMarginTop : 0;
        if (marginParams.topMargin == targetTopMargin) {
            return;
        }
        marginParams.topMargin = targetTopMargin;
        rvApps.setLayoutParams(marginParams);
    }

    private void updateAppsListVisibility() {
        if (rvApps == null) {
            return;
        }
        boolean hasApps = !LauncherHomeActivity.arrayListAppsSearch.isEmpty();
        if (!hasApps && !appsShown) {
            rvApps.setVisibility(INVISIBLE);
            rvApps.setAlpha(0f);
            return;
        }
        rvApps.setVisibility(VISIBLE);
        if (appsShown) {
            rvApps.setAlpha(1f);
        }
    }

    private void scheduleEnrichment() {
        if (enrichmentDone || enrichScheduled || !isAdded()) {
            return;
        }
        enrichScheduled = true;
        mainHandler.post(this::enrichContentAfterOpen);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void enrichContentAfterOpen() {
        if (!isAdded() || enrichmentDone) {
            return;
        }
        enrichmentDone = true;

        final boolean needsFullReload = LauncherHomeActivity.arrayListAppsSearch.isEmpty();

        bgExecutor.execute(() -> {
            Context context = getContext();
            if (context == null) {
                return;
            }

            ArrayList<AppsModel> loadedApps = null;
            if (needsFullReload) {
                loadedApps = loadInstalledApps(context);
            }
            ArrayList<AppsModel> recent = buildRecentApps(context);

            if (!isAdded()) {
                return;
            }
            final ArrayList<AppsModel> installedApps = loadedApps;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }

                if (installedApps != null) {
                    applyInstalledApps(installedApps);
                }

                arrayListRecentApps.clear();
                arrayListRecentApps.addAll(recent);
                boolean showRecent = !arrayListRecentApps.isEmpty();
                setRecentAppsVisibility(showRecent ? VISIBLE : GONE);
                recentAppsAdapter.notifyDataSetChanged();

                if (!appsShown) {
                    if (!LauncherHomeActivity.arrayListAppsSearch.isEmpty()) {
                        LauncherHomeActivity.arrayListApps.clear();
                        LauncherHomeActivity.arrayListApps.addAll(LauncherHomeActivity.arrayListAppsSearch);
                    }
                    attachAppsAdapterIfNeeded();
                    appsShown = true;
                    updateAppsListVisibility();
                    if (rvApps.getVisibility() == VISIBLE) {
                        rvApps.animate().alpha(1f).setDuration(100).start();
                    }
                } else if (appsAdapter != null && needsFullReload) {
                    appsAdapter.notifyDataSetChanged();
                }

                registerPackageReceiver();
            });
        });
    }

    private void showAd() {
        if (rlBannerAdView == null || slBannerShimmer == null || llBannerAd == null || !isFragmentReady()) {
            return;
        }

        if (!shouldShowBannerAd()) {
            hideBannerAdContainer();
            return;
        }

        if (bannerAdView != null) {
            attachCachedBannerAd();
            return;
        }

        bannerAdLoadInProgress = true;
        rlBannerAdView.setVisibility(VISIBLE);

        AdPlacement.loadBannerAd(requireActivity(), AdPlacement.getLauncherAppBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd, () -> {
            bannerAdLoadInProgress = false;
            hideBannerAdContainer();
        }, adView -> {
            bannerAdLoadInProgress = false;
            bannerAdView = adView;
            if (slBannerShimmer != null) {
                slBannerShimmer.stopShimmer();
            }
            if (!hasContext()) {
                return;
            }
            AdPlacement.setLauncherAppBannerLastShowTime(getContext(), System.currentTimeMillis());
        });
    }

    private boolean shouldShowBannerAd() {
        if (!AdPlacement.isNetworkAvailable(requireActivity()) || !AdPlacement.getLauncherAppBannerAdShow() || bannerAdLoadInProgress) {
            return false;
        }
        if (bannerAdView != null) {
            return true;
        }
        return AdPlacement.canShowLauncherAppBannerAd(requireContext());
    }

    private void attachCachedBannerAd() {
        if (bannerAdView == null || llBannerAd == null) {
            return;
        }
        rlBannerAdView.setVisibility(VISIBLE);
        slBannerShimmer.setVisibility(GONE);
        llBannerAd.setVisibility(VISIBLE);
        ViewGroup currentParent = (ViewGroup) bannerAdView.getParent();
        if (currentParent != null && currentParent != llBannerAd) {
            currentParent.removeView(bannerAdView);
        }
        if (bannerAdView.getParent() == null) {
            llBannerAd.removeAllViews();
            llBannerAd.addView(bannerAdView);
        }
    }

    private void destroyBannerAdView() {
        if (bannerAdView != null) {
            ViewGroup parent = (ViewGroup) bannerAdView.getParent();
            if (parent != null) {
                parent.removeView(bannerAdView);
            }
            bannerAdView.destroy();
            bannerAdView = null;
        }
        if (llBannerAd != null) {
            for (int i = llBannerAd.getChildCount() - 1; i >= 0; i--) {
                View child = llBannerAd.getChildAt(i);
                if (child instanceof AdView) {
                    AdView adView = (AdView) child;
                    adView.destroy();
                    llBannerAd.removeViewAt(i);
                }
            }
            llBannerAd.removeAllViews();
        }
    }

    private void hideBannerAdContainer() {
        if (rlBannerAdView != null) {
            rlBannerAdView.setVisibility(GONE);
        }
    }

    private void registerPackageReceiver() {
        if (receiverRegistered || getContext() == null) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        requireContext().registerReceiver(appChangeReceiver, filter);
        receiverRegistered = true;
    }

    private ArrayList<AppsModel> buildRecentApps(Context context) {
        ArrayList<AppsModel> recent = new ArrayList<>();
        SharedPreferences sharedPreferences = context.getSharedPreferences("recentThread", MODE_PRIVATE);
        String data = sharedPreferences.getString("packages", "");
        if (data.isEmpty()) {
            return recent;
        }

        HashMap<String, AppsModel> byPackage = new HashMap<>();
        List<AppsModel> searchSnapshot;
        synchronized (LauncherHomeActivity.arrayListAppsSearch) {
            searchSnapshot = new ArrayList<>(LauncherHomeActivity.arrayListAppsSearch);
        }
        for (AppsModel model : searchSnapshot) {
            if (model == null || model.getPackageName() == null) {
                continue;
            }
            byPackage.put(model.getPackageName(), model);
        }

        PackageManager packageManager = context.getPackageManager();
        for (String packageName : data.split(",")) {
            if (packageName == null || packageName.isEmpty()) {
                continue;
            }
            AppsModel cached = byPackage.get(packageName);
            if (cached != null) {
                recent.add(cached);
                continue;
            }
            try {
                Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
                if (launchIntent == null) {
                    continue;
                }
                String appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString();
                Drawable appIcon = packageManager.getApplicationIcon(packageName);
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                recent.add(new AppsModel(appName, packageName, appIcon, packageInfo.firstInstallTime));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return recent;
    }

    @Nullable
    private ArrayList<AppsModel> loadInstalledApps(Context context) {
        ArrayList<AppsModel> tempList = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> apps = Utils.queryLauncherActivities(packageManager);
        if (apps == null) {
            return null;
        }
        try {
            apps.sort(new ResolveInfo.DisplayNameComparator(packageManager));
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (ResolveInfo info : apps) {
            try {
                String appName = info.loadLabel(packageManager).toString();
                String packageName = info.activityInfo.packageName;
                if (packageName.equals(context.getPackageName())) {
                    continue;
                }
                Drawable appIcon = packageManager.getApplicationIcon(packageName);
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                tempList.add(new AppsModel(appName, packageName, appIcon, packageInfo.firstInstallTime));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sortApps(context, tempList);
        return tempList;
    }

    private void applyInstalledApps(@NonNull ArrayList<AppsModel> tempList) {
        LauncherHomeActivity.arrayListApps.clear();
        LauncherHomeActivity.arrayListApps.addAll(tempList);
        LauncherHomeActivity.arrayListAppsSearch.clear();
        LauncherHomeActivity.arrayListAppsSearch.addAll(tempList);
    }

    private void sortApps(Context context, List<AppsModel> list) {
        String serialize = Utils.getAppSerialize(context);
        if ("a".equals(serialize)) {
            list.sort((a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
        } else if ("d".equals(serialize)) {
            list.sort((a, b) -> b.getAppName().compareToIgnoreCase(a.getAppName()));
        } else if ("i".equals(serialize)) {
            list.sort((a, b) -> Long.compare(b.getInstallTime(), a.getInstallTime()));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void searchApps(String query) {
        Context context = getContext();
        if (context == null || appsAdapter == null) {
            return;
        }
        String search = query.trim().toLowerCase();
        LauncherHomeActivity.arrayListApps.clear();

        if (search.isEmpty()) {
            LauncherHomeActivity.arrayListApps.addAll(LauncherHomeActivity.arrayListAppsSearch);

            setRecentAppsVisibility(arrayListRecentApps.isEmpty() ? GONE : VISIBLE);

            ivDivider2.setVisibility(GONE);
            tvNoApps.setVisibility(GONE);
            tvMoreApps.setVisibility(GONE);

            appsAdapter.notifyDataSetChanged();
            return;
        }

        for (AppsModel appsModel : LauncherHomeActivity.arrayListAppsSearch) {
            String appName = appsModel.getAppName();
            if (appName != null && appName.toLowerCase().contains(search)) {
                LauncherHomeActivity.arrayListApps.add(appsModel);
            }
        }

        setRecentAppsVisibility(GONE);
        appsAdapter.notifyDataSetChanged();
        updateEmptyState(search);
    }

    @SuppressLint("SetTextI18n")
    private void updateEmptyState(String query) {
        tvMoreApps.setVisibility(VISIBLE);

        if (LauncherHomeActivity.arrayListApps.isEmpty()) {
            ivDivider2.setVisibility(GONE);
            tvNoApps.setVisibility(VISIBLE);
            tvNoApps.setText("No " + query + " Apps Found");
        } else {
            ivDivider2.setVisibility(VISIBLE);
            tvNoApps.setVisibility(GONE);
        }
    }

    private boolean isSearchActive() {
        return etSearch != null && etSearch.getText() != null && !etSearch.getText().toString().trim().isEmpty();
    }

    private void setRecentAppsVisibility(int visibility) {
        if (llRecentApps == null) {
            return;
        }
        if (visibility == INVISIBLE) {
            visibility = GONE;
        }
        if (isSearchActive()) {
            visibility = GONE;
        }
        boolean showRecent = visibility == VISIBLE;
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) llRecentApps.getLayoutParams();
        Context context = getContext();
        int topMargin = (showRecent && context != null) ? Utils.dpToPx(context, 6) : 0;
        boolean visibilityChanged = llRecentApps.getVisibility() != visibility;
        boolean marginChanged = params != null && params.topMargin != topMargin;
        boolean headerAttached = isRecentHeaderAttached();
        if (!visibilityChanged && !marginChanged && headerAttached == showRecent) {
            return;
        }
        if (visibilityChanged) {
            llRecentApps.setVisibility(visibility);
        }
        if (marginChanged) {
            params.topMargin = topMargin;
            llRecentApps.setLayoutParams(params);
        }
        syncRecentHeaderAdapter(showRecent);
        applyAppsRecyclerSpacing(showRecent);
        if (rvRecentApps != null) {
            rvRecentApps.requestLayout();
        }
        if (rvApps != null) {
            rvApps.requestLayout();
        }
    }

    private void searchClearView() {
        if (etSearch == null) {
            return;
        }

        ivDivider2.setVisibility(GONE);
        tvNoApps.setVisibility(GONE);
        tvMoreApps.setVisibility(GONE);

        Context context = getContext();
        if (context != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            }
        }

        etSearch.clearFocus();
        if (etSearch.getText() != null && etSearch.getText().length() > 0) {
            etSearch.setText("");
        } else {
            setRecentAppsVisibility(arrayListRecentApps.isEmpty() ? GONE : VISIBLE);
        }
    }

    private final BroadcastReceiver appChangeReceiver = new BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isAdded() || !enrichmentDone) {
                return;
            }
            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                String action = intent.getAction();

                if (Intent.ACTION_PACKAGE_REMOVED.equals(action) && intent.getData() != null) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    LauncherHomeActivity.arrayListApps.removeIf(app -> app.getPackageName().equals(packageName));
                    LauncherHomeActivity.arrayListAppsSearch.removeIf(app -> app.getPackageName().equals(packageName));
                    arrayListRecentApps.removeIf(app -> app.getPackageName().equals(packageName));
                    setRecentAppsVisibility(arrayListRecentApps.isEmpty() ? GONE : VISIBLE);
                    if (appsAdapter != null) {
                        appsAdapter.notifyDataSetChanged();
                    }
                    if (recentAppsAdapter != null) {
                        recentAppsAdapter.notifyDataSetChanged();
                    }
                } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                    bgExecutor.execute(() -> {
                        Context appContext = getContext();
                        if (appContext == null) {
                            return;
                        }
                        ArrayList<AppsModel> loadedApps = loadInstalledApps(appContext);
                        ArrayList<AppsModel> recent = buildRecentApps(appContext);
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) {
                                return;
                            }
                            if (loadedApps != null) {
                                applyInstalledApps(loadedApps);
                            }
                            arrayListRecentApps.clear();
                            arrayListRecentApps.addAll(recent);
                            setRecentAppsVisibility(arrayListRecentApps.isEmpty() ? GONE : VISIBLE);
                            if (appsAdapter != null) {
                                appsAdapter.notifyDataSetChanged();
                            }
                            if (recentAppsAdapter != null) {
                                recentAppsAdapter.notifyDataSetChanged();
                            }
                        });
                    });
                }
            });
        }
    };

    private void restoreExpandedStateIfNeeded() {
        if (!isAdded() || interactiveSessionActive || sheetBehavior == null) {
            return;
        }
        int state = sheetBehavior.getState();
        if (state == BottomSheetBehavior.STATE_HIDDEN || state == BottomSheetBehavior.STATE_COLLAPSED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        if (bottomSheetView != null) {
            bottomSheetView.setTranslationY(0f);
        }
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(SHEET_DIM_AMOUNT);
            }
        }
    }

    @Override
    public void onAppsUninstallClick(String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
        Utils.clearActivityTransition(getActivity());
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        defaultHomePromptHelper.onResume();
        restoreExpandedStateIfNeeded();
        if (bottomSheetView != null && isFullyExpanded()) {
            applyExpandedSystemBarAppearance();
            postApplyExpandedSystemBarAppearance();
        }
        if (!enrichmentDone) {
            return;
        }
        bgExecutor.execute(() -> {
            Context context = getContext();
            if (context == null) {
                return;
            }
            ArrayList<AppsModel> recent = buildRecentApps(context);
            sortApps(context, LauncherHomeActivity.arrayListApps);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                arrayListRecentApps.clear();
                arrayListRecentApps.addAll(recent);
                setRecentAppsVisibility(arrayListRecentApps.isEmpty() ? GONE : VISIBLE);
                if (recentAppsAdapter != null) {
                    recentAppsAdapter.notifyDataSetChanged();
                    recentAppsAdapter.updateSizeVisiblity();
                }
                if (appsAdapter != null) {
                    appsAdapter.notifyDataSetChanged();
                    appsAdapter.updateSizeVisiblity();
                }
            });
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        clearExpandedSystemBarAppearance();
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroyView() {
        defaultHomePromptHelper.release();
        mainHandler.removeCallbacksAndMessages(null);
        clearExpandedSystemBarAppearance();
        bannerAdLoadInProgress = false;
        destroyBannerAdView();
        if (bottomSheetView != null) {
            bottomSheetView.animate().cancel();
            bottomSheetView = null;
        }
        contentRoot = null;
        statusBarScrim = null;
        llDefault = null;
        btnSetNow = null;
        interactiveSessionActive = false;
        clearInteractiveWindowFlags();
        if (sheetBehavior != null) {
            sheetBehavior.removeBottomSheetCallback(sheetCallback);
            sheetBehavior = null;
        }
        if (backDismissCallback != null) {
            backDismissCallback.remove();
            backDismissCallback = null;
        }
        animatedDismissRequested = false;
        if (receiverRegistered) {
            try {
                requireContext().unregisterReceiver(appChangeReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            receiverRegistered = false;
        }
        searchClearView();
        concatAdapter = null;
        headerAdapter = null;
        footerAdapter = null;
        appsAdapter = null;
        rlBannerAdView = null;
        slBannerShimmer = null;
        llBannerAd = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        bannerAdLoadInProgress = false;
        bgExecutor.shutdownNow();
        super.onDestroy();
    }
}