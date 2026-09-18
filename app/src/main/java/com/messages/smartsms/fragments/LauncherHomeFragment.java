package com.messages.smartsms.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.speech.RecognizerIntent;
import android.telecom.TelecomManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.smartsms.R;
import com.messages.smartsms.activities.LauncherHomeActivity;
import com.messages.smartsms.activities.LauncherSettingsActivity;
import com.messages.smartsms.activities.NewsActivity;
import com.messages.smartsms.adapters.AppsAdapter;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.Utils;
import com.messages.smartsms.dialogs.LauncherAppsBottomSheet;
import com.messages.smartsms.helpers.DefaultHomePromptHelper;
import com.messages.smartsms.helpers.LauncherAppsIconCache;
import com.messages.smartsms.interfaces.OnAppsUninstallClickListener;
import com.messages.smartsms.models.AppsModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class LauncherHomeFragment extends Fragment implements OnAppsUninstallClickListener {
    private RelativeLayout rlQuickAction, rlSetAsDefault;
    private LinearLayout llGoogleSearch, llRightSwipe, llDateTime, llDefault, llGoogleFolder, llToolsFolder, llContacts, llSetting, llBottomApps;
    private GridLayout gvGoogleApps, gvToolsApps;
    private CardView cvGoogleFolder, cvToolsFolder;
    private AppCompatImageView ivGoogleVoiceSearch, ivNews, ivArrow, ivTopAppIcon1, ivTopAppIcon2, ivAppIcon1, ivAppIcon2, ivAppIcon3, ivAppIcon4;
    private AppCompatTextView tvGoogle, tvTools, ivTopAppName1, ivTopAppName2, tvDate, tvTime;

    private SharedPreferences sharedPreferences;
    private ObjectAnimator animatorArrow, animatorBottomApps;
    private volatile ArrayList<AppsModel> arrayListGoogleApps = new ArrayList<>();
    private volatile ArrayList<AppsModel> arrayListToolsApps = new ArrayList<>();
    private final Object homeAppsLoadLock = new Object();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMMM", Locale.ENGLISH);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
    private static final String GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final long SHORTCUT_CLICK_DEBOUNCE_MS = 800L;
    private static final int FOLDER_PREVIEW_COLUMNS = 3;
    private static final int FOLDER_PREVIEW_MAX_ICONS = 9;
    private boolean receiverRegistered;
    private boolean timeReceiverRegistered;
    private long lastShortcutLaunchElapsedMs;

    private int touchSlop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    private VelocityTracker velocityTracker;
    private float touchDownX;
    private float touchDownY;
    private boolean appsSwipeResolved;
    private boolean appsSheetDragging;
    private LauncherAppsBottomSheet interactiveAppsSheet;
    private final DefaultHomePromptHelper defaultHomePromptHelper = new DefaultHomePromptHelper(this);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        defaultHomePromptHelper.registerRoleLauncher();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_launcher_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findIDs(view);
    }

    private void findIDs(View view) {
        rlQuickAction = view.findViewById(R.id.rlQuickAction);
        llGoogleSearch = view.findViewById(R.id.llGoogleSearch);
        ivGoogleVoiceSearch = view.findViewById(R.id.ivGoogleVoiceSearch);
        ivNews = view.findViewById(R.id.ivNews);
        llRightSwipe = view.findViewById(R.id.llRightSwipe);
        llDateTime = view.findViewById(R.id.llDateTime);
        tvDate = view.findViewById(R.id.tvDate);
        tvTime = view.findViewById(R.id.tvTime);
        llDefault = view.findViewById(R.id.llDefault);
        rlSetAsDefault = view.findViewById(R.id.rlSetAsDefault);

        llGoogleFolder = view.findViewById(R.id.llGoogleFolder);
        cvGoogleFolder = view.findViewById(R.id.cvGoogleFolder);
        gvGoogleApps = view.findViewById(R.id.gvGoogleApps);
        tvGoogle = view.findViewById(R.id.tvGoogle);
        llToolsFolder = view.findViewById(R.id.llToolsFolder);
        cvToolsFolder = view.findViewById(R.id.cvToolsFolder);
        gvToolsApps = view.findViewById(R.id.gvToolsApps);
        tvTools = view.findViewById(R.id.tvTools);
        llContacts = view.findViewById(R.id.llContacts);
        llSetting = view.findViewById(R.id.llSetting);
        ivTopAppIcon1 = view.findViewById(R.id.ivTopAppIcon1);
        ivTopAppIcon2 = view.findViewById(R.id.ivTopAppIcon2);
        ivTopAppName1 = view.findViewById(R.id.ivTopAppName1);
        ivTopAppName2 = view.findViewById(R.id.ivTopAppName2);

        ivArrow = view.findViewById(R.id.ivArrow);
        llBottomApps = view.findViewById(R.id.llBottomApps);

        ivAppIcon1 = view.findViewById(R.id.ivAppIcon1);
        ivAppIcon2 = view.findViewById(R.id.ivAppIcon2);
        ivAppIcon3 = view.findViewById(R.id.ivAppIcon3);
        ivAppIcon4 = view.findViewById(R.id.ivAppIcon4);

        updateRightSwipeTutorialVisibility();
        defaultHomePromptHelper.bind(llDefault);
        initialEvents();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialEvents() {
        ViewConfiguration vc = ViewConfiguration.get(requireContext());
        touchSlop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        sharedPreferences = requireContext().getSharedPreferences("launcherFirstTutorial", Context.MODE_PRIVATE);

        if (!sharedPreferences.getBoolean("swipe_up_done", false)) {
            animatorArrow = ObjectAnimator.ofFloat(ivArrow, "translationY", 0f, -30f, 0f);
            animatorArrow.setDuration(1200);
            animatorArrow.setRepeatCount(ValueAnimator.INFINITE);
            animatorArrow.start();

            animatorBottomApps = ObjectAnimator.ofFloat(llBottomApps, "translationY", 0f, -30f, 0f);
            animatorBottomApps.setDuration(1200);
            animatorBottomApps.setRepeatCount(ValueAnimator.INFINITE);
            animatorBottomApps.start();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        requireActivity().registerReceiver(appChangeReceiver, filter);
        receiverRegistered = true;

        updateNewsIconVisibility();

        reloadHomeAppFolders();

        updateFolderSize();
        updateHomeIconSize();
        updateHomeTextSize();
        updateLabelVisibility();
        loadDockAppIcons();

        rlQuickAction.setOnClickListener(view -> {
            if (!(requireActivity() instanceof LauncherHomeActivity)) {
                return;
            }
            ((LauncherHomeActivity) requireActivity()).showMainFragment(false);
        });

        rlSetAsDefault.setOnClickListener(view -> defaultHomePromptHelper.handleSetAsDefaultClick());

        llGoogleSearch.setOnClickListener(view -> {
            Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(GOOGLE_APP_PACKAGE);
            if (intent != null) {
                startActivity(intent);
                Utils.clearActivityTransition(getActivity());
            }
        });

        ivGoogleVoiceSearch.setOnClickListener(view -> {
            if (!canLaunchShortcut()) {
                return;
            }
            launchGoogleVoiceSearch();
        });

        ivNews.setOnClickListener(view -> {
            Intent intent = new Intent(requireActivity(), NewsActivity.class);
            startActivity(intent);
            Utils.clearActivityTransition(getActivity());
        });

        llGoogleFolder.setOnClickListener(v -> {
            ArrayList<AppsModel> googleApps = copyApps(arrayListGoogleApps);
            if (!googleApps.isEmpty()) {
                dialogAppsFolder("Google", googleApps);
            }
        });

        llToolsFolder.setOnClickListener(v -> {
            ArrayList<AppsModel> toolsApps = copyApps(arrayListToolsApps);
            if (!toolsApps.isEmpty()) {
                dialogAppsFolder("Tools", toolsApps);
            }
        });

        llContacts.setOnClickListener(view -> {
            if (requireActivity() instanceof LauncherHomeActivity) {
                ((LauncherHomeActivity) requireActivity()).showMainFragment(true);
            }
        });

        llSetting.setOnClickListener(view -> {
            Intent intent = new Intent(requireActivity(), LauncherSettingsActivity.class);
            startActivity(intent);
            Utils.clearActivityTransition(getActivity());
        });

        ivAppIcon1.setOnClickListener(view -> Utils.openDialer(requireActivity(), ""));

        ivAppIcon2.setOnClickListener(view -> {
            if (requireActivity() instanceof LauncherHomeActivity) {
                ((LauncherHomeActivity) requireActivity()).showMainFragment(false);
            }
        });

        ivAppIcon3.setOnClickListener(view -> {
            Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage("com.android.chrome");
            if (intent != null) {
                startActivity(intent);
                Utils.clearActivityTransition(getActivity());
            } else {
                Toast.makeText(requireContext(), "Chrome not installed", Toast.LENGTH_SHORT).show();
            }
        });

        ivAppIcon4.setOnClickListener(view -> {
            try {
                Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                startActivity(intent);
                Utils.clearActivityTransition(getActivity());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void reloadHomeAppFolders() {
        Executors.newSingleThreadExecutor().execute(() -> {
            synchronized (homeAppsLoadLock) {
                loadGoogleApps();
                loadToolsApps();
                loadApps();
            }
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                setGoogleAppsFolder();
                setToolsAppsFolder();
            });
        });
    }

    @NonNull
    private ArrayList<AppsModel> copyApps(@Nullable ArrayList<AppsModel> apps) {
        if (apps == null) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(apps);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private boolean isAppAlreadyAdded(ArrayList<AppsModel> list, String packageName) {
        for (AppsModel appsModel : list) {
            if (appsModel.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void loadGoogleApps() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        ArrayList<AppsModel> tempList = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        String[] requiredApps = {"com.android.chrome", "com.google.android.apps.docs", "com.google.android.gm", "com.google.android.googlequicksearchbox", "com.google.android.apps.maps", "com.google.android.apps.photos", "com.google.android.youtube", "com.android.vending", "com.google.android.calendar",};

        for (String packageName : requiredApps) {
            try {
                packageManager.getPackageInfo(packageName, 0);
                Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    String appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString();
                    Drawable appIcon = packageManager.getApplicationIcon(packageName);
                    long installTime;
                    PackageInfo packageInfo;
                    try {
                        packageInfo = packageManager.getPackageInfo(packageName, 0);
                        installTime = packageInfo.firstInstallTime;
                    } catch (PackageManager.NameNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    if (!isAppAlreadyAdded(tempList, packageName)) {
                        tempList.add(new AppsModel(appName, packageName, appIcon, installTime));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        arrayListGoogleApps = tempList;
    }

    private void loadToolsApps() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        ArrayList<AppsModel> tempList = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        String[] requiredApps = {"com.google.android.calculator", "com.sec.android.app.popupcalculator", "com.miui.calculator", "com.google.android.deskclock", "com.sec.android.app.clockpackage", "com.android.deskclock", "com.google.android.keep", "com.miui.notes", "com.samsung.android.app.notes", "com.google.android.calendar", "com.android.calendar", "com.google.android.documentsui", "com.mi.android.globalFileexplorer", "com.sec.android.app.myfiles", "com.coloros.filemanager", "com.android.settings"};

        for (String packageName : requiredApps) {
            try {
                packageManager.getPackageInfo(packageName, 0);
                Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    String appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString();
                    Drawable appIcon = packageManager.getApplicationIcon(packageName);
                    long installTime;
                    PackageInfo packageInfo;
                    try {
                        packageInfo = packageManager.getPackageInfo(packageName, 0);
                        installTime = packageInfo.firstInstallTime;
                    } catch (PackageManager.NameNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    if (!isAppAlreadyAdded(tempList, packageName)) {
                        tempList.add(new AppsModel(appName, packageName, appIcon, installTime));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        arrayListToolsApps = tempList;
    }

    private void setGoogleAppsFolder() {
        bindFolderPreview(gvGoogleApps, cvGoogleFolder, arrayListGoogleApps);
    }

    private void setToolsAppsFolder() {
        bindFolderPreview(gvToolsApps, cvToolsFolder, arrayListToolsApps);
    }

    private void bindFolderPreview(@Nullable GridLayout gridLayout, @Nullable CardView folderCard, ArrayList<AppsModel> apps) {
        Context context = getContext();
        if (gridLayout == null || context == null || !isAdded()) {
            return;
        }
        gridLayout.removeAllViews();

        ArrayList<AppsModel> previewApps = new ArrayList<>();
        for (AppsModel model : copyApps(apps)) {
            if (model != null && model.getAppIcon() != null) {
                previewApps.add(model);
            }
        }
        if (previewApps.isEmpty()) {
            return;
        }

        int folderSizePx = resolveFolderSizePx(folderCard, context);
        int margin = Utils.dpToPx(context.getApplicationContext(), 1);
        int iconSize = getFolderPreviewIconSizePx(folderSizePx, gridLayout, margin, context);
        int cornerRadiusPx = Utils.getLauncherAppIconCornerRadiusPx(context);
        LauncherAppsIconCache.warm(context, previewApps, iconSize, cornerRadiusPx);
        int previewCount = Math.min(previewApps.size(), FOLDER_PREVIEW_MAX_ICONS);
        int rowCount = (previewCount + FOLDER_PREVIEW_COLUMNS - 1) / FOLDER_PREVIEW_COLUMNS;

        gridLayout.setColumnCount(FOLDER_PREVIEW_COLUMNS);
        gridLayout.setRowCount(rowCount);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        gridLayout.setUseDefaultMargins(false);

        for (int index = 0; index < previewCount; index++) {
            if (index >= previewApps.size()) {
                break;
            }
            AppsModel app = previewApps.get(index);
            if (app == null || app.getAppIcon() == null) {
                continue;
            }
            ImageView imageView = new ImageView(context);
            bindAppIcon(imageView, app.getPackageName(), app.getAppIcon(), iconSize);

            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(GridLayout.spec(index / FOLDER_PREVIEW_COLUMNS, 1f), GridLayout.spec(index % FOLDER_PREVIEW_COLUMNS, 1f));
            layoutParams.width = iconSize;
            layoutParams.height = iconSize;
            layoutParams.setMargins(margin, margin, margin, margin);
            imageView.setLayoutParams(layoutParams);
            gridLayout.addView(imageView);
        }
    }

    private int resolveFolderSizePx(@Nullable CardView folderCard, @NonNull Context context) {
        if (folderCard != null && folderCard.getLayoutParams() != null && folderCard.getLayoutParams().width > 0) {
            return folderCard.getLayoutParams().width;
        }
        return Utils.dpToPx(context.getApplicationContext(), Utils.getAppIconSize(context.getApplicationContext()));
    }

    private int getFolderPreviewIconSizePx(int folderSizePx, @NonNull ViewGroup container, int margin, @NonNull Context context) {
        int horizontalPadding = container.getPaddingLeft() + container.getPaddingRight();
        if (horizontalPadding <= 0) {
            horizontalPadding = Utils.dpToPx(context.getApplicationContext(), 2) * 2;
        }
        int availableWidth = folderSizePx - horizontalPadding;
        int iconSize = (availableWidth - (FOLDER_PREVIEW_COLUMNS * 2 * margin)) / FOLDER_PREVIEW_COLUMNS;
        return Math.max(Utils.dpToPx(context.getApplicationContext(), 6), iconSize);
    }

    @Nullable
    private String getDefaultDialerPackageName() {
        if (!isAdded()) {
            return null;
        }

        Context context = requireContext();
        try {
            TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null) {
                String packageCall = telecomManager.getDefaultDialerPackage();
                if (packageCall != null && !packageCall.isEmpty()) {
                    return packageCall;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            PackageManager packageManager = context.getPackageManager();
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            ResolveInfo resolveInfo = packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null && resolveInfo.activityInfo.packageName != null && !resolveInfo.activityInfo.packageName.isEmpty()) {
                return resolveInfo.activityInfo.packageName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void loadDockAppIcons() {
        if (!isAdded() || ivAppIcon1 == null) {
            return;
        }
        PackageManager packageManager = requireContext().getPackageManager();

        try {
            String dialerPackage = getDefaultDialerPackageName();
            if (dialerPackage != null) {
                bindDockAppIcon(ivAppIcon1, dialerPackage, packageManager.getApplicationIcon(dialerPackage));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String smsPackage = Telephony.Sms.getDefaultSmsPackage(requireContext());
            if (smsPackage != null) {
                bindDockAppIcon(ivAppIcon2, smsPackage, packageManager.getApplicationIcon(smsPackage));
            } else {
                bindDockAppIcon(ivAppIcon2, requireContext().getPackageName(), packageManager.getApplicationIcon(requireContext().getPackageName()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String packageChrome = "com.android.chrome";
            bindDockAppIcon(ivAppIcon3, packageChrome, packageManager.getApplicationIcon(packageChrome));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        ResolveInfo resolveInfo = packageManager.resolveActivity(intentCamera, 0);
        if (resolveInfo != null) {
            String cameraPackage = resolveInfo.activityInfo != null ? resolveInfo.activityInfo.packageName : null;
            bindDockAppIcon(ivAppIcon4, cameraPackage, resolveInfo.loadIcon(packageManager));
        }
    }

    private void bindDockAppIcon(@Nullable AppCompatImageView iconView, @Nullable String packageName, @Nullable Drawable icon) {
        if (iconView == null || getContext() == null) {
            return;
        }
        int iconSizePx = Utils.dpToPx(requireContext(), Utils.getAppIconSize(requireContext()));
        bindAppIcon(iconView, packageName, icon, iconSizePx);
    }

    private void bindAppIcon(@Nullable ImageView iconView, @Nullable String packageName, @Nullable Drawable icon, int iconSizePx) {
        if (iconView == null || getContext() == null) {
            return;
        }
        iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int cornerRadiusPx = Utils.getLauncherAppIconCornerRadiusPx(requireContext());
        Drawable displayIcon = LauncherAppsIconCache.resolveDisplayIcon(requireContext(), packageName, icon, iconSizePx, cornerRadiusPx);
        if (displayIcon != null) {
            iconView.setImageDrawable(displayIcon);
        } else {
            iconView.setImageResource(R.mipmap.ic_launcher);
        }
    }

    @SuppressLint("InflateParams")
    private void dialogAppsFolder(String folderName, ArrayList<AppsModel> arrayListApps) {
        LauncherHomeActivity.appsFolderDialog = new Dialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_apps_folder, null);
        LauncherHomeActivity.appsFolderDialog.setContentView(view);

        Window window = LauncherHomeActivity.appsFolderDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int margin = (int) (40 * getResources().getDisplayMetrics().density);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            window.setLayout(screenWidth - (margin * 2), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        AppCompatTextView tvTitle = view.findViewById(R.id.tvTitle);
        RecyclerView rvApps = view.findViewById(R.id.rvApps);

        tvTitle.setVisibility(Utils.getLabelVisibility(requireContext()) ? VISIBLE : GONE);
        tvTitle.setText(folderName);
        tvTitle.setTextSize(Utils.getAppLabelSize(requireContext().getApplicationContext()) + 2);

        rvApps.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        int iconSizePx = Utils.dpToPx(requireContext(), Utils.getAppIconSize(requireContext()));
        int cornerRadiusPx = Utils.getLauncherAppIconCornerRadiusPx(requireContext());
        LauncherAppsIconCache.warm(requireContext(), arrayListApps, iconSizePx, cornerRadiusPx);
        rvApps.setAdapter(new AppsAdapter(requireContext(), arrayListApps, this));

        LauncherHomeActivity.appsFolderDialog.show();
    }

    private void updateFolderSize() {
        if (cvGoogleFolder == null || cvToolsFolder == null || getContext() == null || !isAdded()) {
            return;
        }
        int appIconDp = Utils.getAppIconSize(requireContext().getApplicationContext());
        int folderSizePx = Utils.dpToPx(requireContext().getApplicationContext(), appIconDp);

        ViewGroup.LayoutParams googleCardParams = cvGoogleFolder.getLayoutParams();
        googleCardParams.width = folderSizePx;
        googleCardParams.height = folderSizePx;
        cvGoogleFolder.setLayoutParams(googleCardParams);

        ViewGroup.LayoutParams toolsCardParams = cvToolsFolder.getLayoutParams();
        toolsCardParams.width = folderSizePx;
        toolsCardParams.height = folderSizePx;
        cvToolsFolder.setLayoutParams(toolsCardParams);

        setGoogleAppsFolder();
        setToolsAppsFolder();
    }

    private void updateHomeIconSize() {
        if (ivTopAppIcon1 == null || getContext() == null) {
            return;
        }
        int sizePx = Utils.dpToPx(requireContext().getApplicationContext(), Utils.getAppIconSize(requireContext().getApplicationContext()));

        AppCompatImageView[] imageViews = {ivTopAppIcon1, ivTopAppIcon2, ivAppIcon1, ivAppIcon2, ivAppIcon3, ivAppIcon4};
        for (AppCompatImageView imageView : imageViews) {
            ViewGroup.LayoutParams params = imageView.getLayoutParams();
            params.width = sizePx;
            params.height = sizePx;
            imageView.setLayoutParams(params);
        }
        loadDockAppIcons();
    }

    private void updateHomeTextSize() {
        if (tvGoogle == null || getContext() == null) {
            return;
        }
        AppCompatTextView[] textViews = {tvGoogle, tvTools, ivTopAppName1, ivTopAppName2};
        for (AppCompatTextView textView : textViews) {
            textView.setTextSize(Utils.getAppLabelSize(requireContext().getApplicationContext()));
        }
    }

    private void updateLabelVisibility() {
        if (tvGoogle == null || getContext() == null) {
            return;
        }
        boolean isLabelVisible = Utils.getLabelVisibility(requireContext().getApplicationContext());

        int visibility = isLabelVisible ? VISIBLE : GONE;

        tvGoogle.setVisibility(visibility);
        tvTools.setVisibility(visibility);
        ivTopAppName1.setVisibility(visibility);
        ivTopAppName2.setVisibility(visibility);
    }

    private void loadApps() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        ArrayList<AppsModel> tempList = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> apps = Utils.queryLauncherActivities(packageManager);
        if (apps == null) {
            return;
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
                long installTime;
                PackageInfo packageInfo;
                try {
                    packageInfo = packageManager.getPackageInfo(packageName, 0);
                    installTime = packageInfo.firstInstallTime;
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
                tempList.add(new AppsModel(appName, packageName, appIcon, installTime));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String serialize = Utils.getAppSerialize(context);
        if ("d".equals(serialize)) {
            tempList.sort((a, b) -> b.getAppName().compareToIgnoreCase(a.getAppName()));
        } else if ("i".equals(serialize)) {
            tempList.sort((a, b) -> Long.compare(b.getInstallTime(), a.getInstallTime()));
        } else {
            tempList.sort((a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
        }

        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            LauncherHomeActivity.arrayListApps.clear();
            LauncherHomeActivity.arrayListApps.addAll(tempList);

            LauncherHomeActivity.arrayListAppsSearch.clear();
            LauncherHomeActivity.arrayListAppsSearch.addAll(tempList);
        });
    }

    public boolean isAppsSheetDragging() {
        return appsSheetDragging;
    }

    public boolean onHostTouchEvent(MotionEvent event) {
        if (!isAdded() || event == null) {
            return false;
        }

        Fragment existing = getParentFragmentManager().findFragmentByTag(LauncherAppsBottomSheet.TAG);
        if (existing instanceof LauncherAppsBottomSheet) {
            LauncherAppsBottomSheet sheet = (LauncherAppsBottomSheet) existing;
            if (!sheet.isInteractiveSessionActive() && !appsSheetDragging) {
                return false;
            }
            if (interactiveAppsSheet == null) {
                interactiveAppsSheet = sheet;
            }
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                touchDownY = event.getY();
                appsSwipeResolved = false;
                appsSheetDragging = false;
                break;

            case MotionEvent.ACTION_MOVE:
                handleAppsSwipeMove(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleAppsSwipeEnd(event);
                break;

            default:
                break;
        }
        return appsSheetDragging;
    }

    private void handleAppsSwipeMove(@NonNull MotionEvent event) {
        if (!appsSheetDragging && isHostHorizontalSwipeInProgress()) {
            return;
        }

        float dx = event.getX() - touchDownX;
        float dy = event.getY() - touchDownY;

        if (!appsSwipeResolved) {
            if (Math.abs(dx) < touchSlop && Math.abs(dy) < touchSlop) {
                return;
            }
            appsSwipeResolved = true;

            if (!isVerticalSwipeUp(dx, dy)) {
                return;
            }
            float dragged = touchDownY - event.getY();
            float openDistance = getAppsSheetOpenDistance();
            float progress = openDistance > 0f ? dragged / openDistance : 0f;
            if (!beginInteractiveAppsSheet(progress)) {
                return;
            }
            appsSheetDragging = true;
            markSwipeUpTutorialDone();
        }

        if (!appsSheetDragging || interactiveAppsSheet == null) {
            return;
        }

        float dragged = touchDownY - event.getY();
        float openDistance = getAppsSheetOpenDistance();
        float progress = openDistance > 0f ? dragged / openDistance : 0f;
        interactiveAppsSheet.updateInteractiveProgress(progress);
    }

    private void handleAppsSwipeEnd(@NonNull MotionEvent event) {
        float velocityY = 0f;
        if (velocityTracker != null) {
            velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
            velocityY = velocityTracker.getYVelocity();
            if (Math.abs(velocityY) < minFlingVelocity * 0.5f) {
                velocityY = 0f;
            }
        }

        if (appsSheetDragging && interactiveAppsSheet != null) {
            interactiveAppsSheet.endInteractiveSession(velocityY);
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            maybeOpenFromQuickFling(event, velocityY);
        }

        recycleVelocityTracker();
        appsSheetDragging = false;
        interactiveAppsSheet = null;
        appsSwipeResolved = false;
    }

    private void maybeOpenFromQuickFling(@NonNull MotionEvent event, float velocityY) {
        if (getParentFragmentManager().findFragmentByTag(LauncherAppsBottomSheet.TAG) != null) {
            return;
        }
        if (isHostHorizontalSwipeInProgress()) {
            return;
        }

        float dx = event.getX() - touchDownX;
        float dy = event.getY() - touchDownY;
        boolean flingUp = velocityY <= -minFlingVelocity;
        if (!isVerticalSwipeUp(dx, dy) || !flingUp) {
            return;
        }

        float openDistance = getAppsSheetOpenDistance();
        float progress = openDistance > 0f ? Math.min(0.35f, (-dy) / openDistance) : 0.2f;
        if (!beginInteractiveAppsSheet(progress)) {
            return;
        }
        markSwipeUpTutorialDone();
        interactiveAppsSheet.endInteractiveSession(velocityY);
    }

    private boolean beginInteractiveAppsSheet(float initialProgress) {
        if (!isAdded()) {
            return false;
        }
        Fragment existing = getParentFragmentManager().findFragmentByTag(LauncherAppsBottomSheet.TAG);
        if (existing instanceof LauncherAppsBottomSheet) {
            LauncherAppsBottomSheet sheet = (LauncherAppsBottomSheet) existing;
            if (sheet.isInteractiveSessionActive()) {
                interactiveAppsSheet = sheet;
                interactiveAppsSheet.updateInteractiveProgress(initialProgress);
                return true;
            }
            return false;
        }

        interactiveAppsSheet = LauncherAppsBottomSheet.newInstance(true);
        interactiveAppsSheet.setInitialInteractiveProgress(initialProgress);
        interactiveAppsSheet.show(getParentFragmentManager(), LauncherAppsBottomSheet.TAG);
        getParentFragmentManager().executePendingTransactions();
        if (!interactiveAppsSheet.isAdded()) {
            interactiveAppsSheet = null;
            return false;
        }
        interactiveAppsSheet.updateInteractiveProgress(initialProgress);
        return true;
    }

    private float getAppsSheetOpenDistance() {
        if (getView() != null && getView().getHeight() > 0) {
            return getView().getHeight();
        }
        return getResources().getDisplayMetrics().heightPixels;
    }

    private boolean isHostHorizontalSwipeInProgress() {
        if (getActivity() instanceof LauncherHomeActivity) {
            return ((LauncherHomeActivity) getActivity()).isHorizontalPageSwipeInProgress();
        }
        return false;
    }

    private boolean isVerticalSwipeUp(float dx, float dy) {
        return dy < 0f && Math.abs(dy) > Math.abs(dx) * LauncherHomeActivity.SWIPE_AXIS_DOMINANCE_RATIO;
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private final BroadcastReceiver appChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (LauncherHomeActivity.appsFolderDialog != null && LauncherHomeActivity.appsFolderDialog.isShowing()) {
                    LauncherHomeActivity.appsFolderDialog.dismiss();
                }
            });

            reloadHomeAppFolders();
        }
    };

    private void applyStatusBarFromWallpaper() {
        try {
            if (!isAdded()) {
                return;
            }
            Utils.WallpaperUiColors colors = Utils.getWallpaperUiColors(requireActivity());
            boolean applyStatusBar = !(requireActivity() instanceof LauncherHomeActivity) || !((LauncherHomeActivity) requireActivity()).isShowingMainFragment();
            if (!applyStatusBar) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = requireActivity().getWindow().getInsetsController();
                if (controller != null) {
                    if (colors.isDark) {
                        controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                    } else {
                        controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                    }
                }
            } else {
                View decor = requireActivity().getWindow().getDecorView();
                decor.setSystemUiVisibility(colors.isDark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshWallpaperStatusBar() {
        if (!isAdded() || getView() == null) {
            return;
        }
        applyStatusBarFromWallpaper();
    }

    @Override
    public void onAppsUninstallClick(String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(android.net.Uri.parse("package:" + packageName));
        startActivityForResult(intent, 200);
        Utils.clearActivityTransition(getActivity());
    }

    private void refreshHomeUi() {
        if (!isAdded() || llGoogleSearch == null) {
            return;
        }
        updateNewsIconVisibility();
        updateFolderSize();
        updateHomeIconSize();
        updateHomeTextSize();
        updateLabelVisibility();

        View root = getView();
        if (root == null) {
            return;
        }
        root.postDelayed(() -> {
            if (!isAdded() || !isResumed()) {
                return;
            }
            reloadHomeAppFolders();
        }, 280);
    }

    private void updateNewsIconVisibility() {
        if (ivNews == null) {
            return;
        }
        ivNews.setVisibility(AdPlacement.getNewsScreenShow() ? VISIBLE : GONE);
    }

    private void updateDateTime() {
        if (!isAdded() || tvDate == null || tvTime == null) {
            return;
        }
        Date now = Calendar.getInstance().getTime();
        tvDate.setText(dateFormat.format(now));

        String formattedTime = timeFormat.format(now).toUpperCase(Locale.ENGLISH);
        int amPmIndex = formattedTime.lastIndexOf(' ');
        if (amPmIndex > 0) {
            SpannableString spannable = new SpannableString(formattedTime);
            spannable.setSpan(new RelativeSizeSpan(0.45f), amPmIndex + 1, formattedTime.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvTime.setText(spannable);
        } else {
            tvTime.setText(formattedTime);
        }
    }

    private void startDateTimeUpdates() {
        updateDateTime();
        if (timeReceiverRegistered || getContext() == null) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        requireContext().registerReceiver(timeChangeReceiver, filter);
        timeReceiverRegistered = true;
    }

    private void stopDateTimeUpdates() {
        if (!timeReceiverRegistered || getContext() == null) {
            return;
        }
        try {
            requireContext().unregisterReceiver(timeChangeReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        timeReceiverRegistered = false;
    }

    private final BroadcastReceiver timeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDateTime();
        }
    };

    private void updateRightSwipeTutorialVisibility() {
        if (!isAdded()) {
            return;
        }
        boolean tutorialShown = Utils.isRightSwipeTutorialShown(requireContext());
        if (llRightSwipe != null) {
            llRightSwipe.setVisibility(tutorialShown ? GONE : VISIBLE);
        }
        if (llDateTime != null) {
            llDateTime.setVisibility(tutorialShown ? VISIBLE : GONE);
        }
    }

    public void onRightSwipeNavigationCompleted() {
        if (!isAdded()) {
            return;
        }
        if (Utils.isRightSwipeTutorialShown(requireContext())) {
            return;
        }
        Utils.setRightSwipeTutorialShown(requireContext(), true);
        updateRightSwipeTutorialVisibility();
    }

    public void hideRightSwipeTutorial() {
        onRightSwipeNavigationCompleted();
    }

    private void markSwipeUpTutorialDone() {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putBoolean("swipe_up_done", true).apply();
        }
        if (animatorArrow != null) {
            animatorArrow.cancel();
        }
        if (animatorBottomApps != null) {
            animatorBottomApps.cancel();
        }
        if (ivArrow != null) {
            ivArrow.setTranslationY(0f);
        }
        if (llBottomApps != null) {
            llBottomApps.setTranslationY(0f);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        defaultHomePromptHelper.onResume();
        updateRightSwipeTutorialVisibility();
        startDateTimeUpdates();
        if (requireActivity() instanceof LauncherHomeActivity && ((LauncherHomeActivity) requireActivity()).shouldSuppressHomeRefresh()) {
            return;
        }
        refreshHomeUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopDateTimeUpdates();
        dismissAppsFolderDialog();
    }

    @Override
    public void onStop() {
        super.onStop();
        dismissAppsFolderDialog();
    }

    private void dismissAppsFolderDialog() {
        try {
            if (LauncherHomeActivity.appsFolderDialog != null && LauncherHomeActivity.appsFolderDialog.isShowing()) {
                LauncherHomeActivity.appsFolderDialog.dismiss();
                LauncherHomeActivity.appsFolderDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean canLaunchShortcut() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastShortcutLaunchElapsedMs < SHORTCUT_CLICK_DEBOUNCE_MS) {
            return false;
        }
        lastShortcutLaunchElapsedMs = now;
        return true;
    }

    private void launchGoogleVoiceSearch() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        PackageManager packageManager = requireContext().getPackageManager();

        Intent googleVoiceSearch = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        googleVoiceSearch.setPackage(GOOGLE_APP_PACKAGE);
        googleVoiceSearch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (startResolvedActivity(googleVoiceSearch, packageManager)) {
            return;
        }

        Intent systemVoiceCommand = new Intent(Intent.ACTION_VOICE_COMMAND);
        systemVoiceCommand.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (startResolvedActivity(systemVoiceCommand, packageManager)) {
            return;
        }

        Intent systemVoiceSearch = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        systemVoiceSearch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startResolvedActivity(systemVoiceSearch, packageManager);
    }

    private boolean startResolvedActivity(Intent intent, PackageManager packageManager) {
        if (intent == null || packageManager == null || !isAdded()) {
            return false;
        }
        try {
            if (intent.resolveActivity(packageManager) == null) {
                return false;
            }
            startActivity(intent);
            Utils.clearActivityTransition(getActivity());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public void onDestroyView() {
        defaultHomePromptHelper.release();
        stopDateTimeUpdates();
        recycleVelocityTracker();
        appsSheetDragging = false;
        interactiveAppsSheet = null;
        appsSwipeResolved = false;
        if (receiverRegistered) {
            try {
                requireActivity().unregisterReceiver(appChangeReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            receiverRegistered = false;
        }
        if (animatorArrow != null) {
            animatorArrow.cancel();
        }
        if (animatorBottomApps != null) {
            animatorBottomApps.cancel();
        }
        ivGoogleVoiceSearch = null;
        tvDate = null;
        tvTime = null;
        llDefault = null;
        super.onDestroyView();
    }
}