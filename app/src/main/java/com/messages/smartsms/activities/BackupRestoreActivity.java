package com.messages.smartsms.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.messages.smartsms.R;
import com.messages.smartsms.adapters.BackupListAdapter;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.helpers.BackupHelper;
import com.messages.smartsms.helpers.MessageListSyncHelper;
import com.messages.smartsms.models.BackupData;
import com.messages.smartsms.models.BackupFileInfo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupRestoreActivity extends AppCompatActivity {
    private AppCompatImageView ivBack;
    private LinearLayout llBackupLocation, llRestore, llBackupNow, llProgressPanel;
    private AppCompatTextView tvTitle, tvBackupLocationTitle, tvBackupLocation, tvRestoreTitle, tvSelectBackup, tvBackupInfo, tvProgressTitle, tvProgressSummary, tvStopRestore, tvBackupNow;
    private ProgressBar progressBar;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer, slNativeShimmer;
    public LinearLayout llBannerAd, llIndicator;
    private FrameLayout flNativeAd;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean stopRestore = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private ActivityResultLauncher<Uri> folderPickerLauncher;
    private Runnable finishProgressRunnable;

    private static final int PERMISSION_READ_SMS = 3001;
    private static final int SMS_APP_REQUEST = 42389;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        registerFolderPicker();
        initViews();
        updateBackupLocationUi();
    }

    private void registerFolderPicker() {
        folderPickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            if (uri == null) {
                return;
            }
            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(uri, takeFlags);

            String folderName = getFolderDisplayName(uri);
            BackupHelper.saveBackupLocation(this, uri, folderName);
            updateBackupLocationUi();
        });
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        llBackupLocation = findViewById(R.id.llBackupLocation);
        tvBackupLocationTitle = findViewById(R.id.tvBackupLocationTitle);
        llRestore = findViewById(R.id.llRestore);
        tvRestoreTitle = findViewById(R.id.tvRestoreTitle);
        tvSelectBackup = findViewById(R.id.tvSelectBackup);
        tvBackupInfo = findViewById(R.id.tvBackupInfo);
        llBackupNow = findViewById(R.id.llBackupNow);
        tvBackupNow = findViewById(R.id.tvBackupNow);
        tvBackupLocation = findViewById(R.id.tvBackupLocation);
        llProgressPanel = findViewById(R.id.llProgressPanel);
        tvProgressTitle = findViewById(R.id.tvProgressTitle);
        tvProgressSummary = findViewById(R.id.tvProgressSummary);
        tvStopRestore = findViewById(R.id.tvStopRestore);
        progressBar = findViewById(R.id.progressBar);

        rlAdView = findViewById(R.id.rlAdView);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);

        clickEvents();
    }

    @SuppressLint("SetTextI18n")
    private void clickEvents() {
        if (!AdPlacement.getOtherAdShow()) {
            rlAdView.setVisibility(View.GONE);
        } else {
            rlAdView.setVisibility(View.VISIBLE);

            if ("banner".equalsIgnoreCase(AdPlacement.getOtherAdType())) {
                rlBannerAdView.setVisibility(View.VISIBLE);
                rlNativeAdView.setVisibility(View.GONE);
                AdPlacement.loadBannerAd(this, AdPlacement.getOtherBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd);
            } else {
                rlBannerAdView.setVisibility(View.GONE);
                rlNativeAdView.setVisibility(View.VISIBLE);
                AdPlacement.loadNativeAd(this, AdPlacement.getOtherNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "small");
            }
        }

        ivBack.setOnClickListener(view -> {
            BackupRestoreActivity.this.finish();
            overridePendingTransition(0, 0);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                BackupRestoreActivity.this.finish();
                overridePendingTransition(0, 0);
            }
        });

        llBackupLocation.setOnClickListener(v -> folderPickerLauncher.launch(null));

        llBackupNow.setOnClickListener(v -> startBackup());

        llRestore.setOnClickListener(v -> showRestoreDialog());

        tvStopRestore.setOnClickListener(v -> showStopRestoreDialog());
    }

    private void updateBackupLocationUi() {
        String folderName = BackupHelper.getBackupFolderName(this);
        if (folderName != null && !folderName.isEmpty()) {
            tvBackupLocation.setText(folderName);
        } else {
            tvBackupLocation.setText(getString(R.string.set_folder));
        }
    }

    private String getFolderDisplayName(Uri treeUri) {
        DocumentFile folder = DocumentFile.fromTreeUri(this, treeUri);
        if (folder != null && folder.getName() != null) {
            return folder.getName();
        }
        String lastSegment = treeUri.getLastPathSegment();
        if (lastSegment != null && lastSegment.contains(":")) {
            return lastSegment.substring(lastSegment.lastIndexOf(':') + 1);
        }
        return getString(R.string.set_folder);
    }

    private void startBackup() {
        if (isRunning.get()) {
            return;
        }
        if (!BackupHelper.hasBackupLocation(this)) {
            Toast.makeText(this, R.string.backup_select_location_first, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasReadSmsPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSION_READ_SMS);
            return;
        }
        performBackup();
    }

    private void performBackup() {
        Uri treeUri = BackupHelper.getBackupTreeUri(this);
        if (treeUri == null) {
            Toast.makeText(this, R.string.backup_select_location_first, Toast.LENGTH_SHORT).show();
            return;
        }

        isRunning.set(true);
        setActionsEnabled(false);
        showProgressPanel(getString(R.string.backup_backing_up), "", false);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(0);

        executor.execute(() -> {
            try {
                BackupData backupData = BackupHelper.readAllSms(this, (stage, current, total) -> mainHandler.post(() -> {
                    if (!canShowUi()) {
                        return;
                    }
                    updateProgress(getString(R.string.backup_backing_up), current, total);
                }));
                BackupHelper.writeBackup(this, treeUri, backupData, (stage, current, total) -> mainHandler.post(() -> {
                    if (!canShowUi()) {
                        return;
                    }
                    progressBar.setIndeterminate(true);
                    tvProgressSummary.setText(getString(R.string.backup_saving));
                }));
                mainHandler.post(() -> {
                    if (!canShowUi()) {
                        return;
                    }
                    finishProgress(getString(R.string.backup_backing_up));
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!canShowUi()) {
                        return;
                    }
                    hideProgressPanel();
                    showErrorDialog(getString(R.string.backup_error_read));
                    resetRunningState();
                });
            }
        });
    }

    private void showRestoreDialog() {
        if (isRunning.get()) {
            return;
        }
        if (!BackupHelper.hasBackupLocation(this)) {
            Toast.makeText(this, R.string.backup_select_location_first, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri treeUri = BackupHelper.getBackupTreeUri(this);
        executor.execute(() -> {
            List<BackupFileInfo> files = BackupHelper.listBackupFiles(this, treeUri);
            mainHandler.post(() -> {
                if (!canShowUi()) {
                    return;
                }
                showBackupListDialog(files);
            });
        });
    }

    private boolean canShowUi() {
        return !isFinishing() && !isDestroyed();
    }

    private void showDialogSafely(AlertDialog dialog) {
        if (dialog == null || !canShowUi()) {
            return;
        }
        try {
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showBackupListDialog(List<BackupFileInfo> files) {
        if (!canShowUi()) {
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.backup_list_dialog, null);
        AppCompatTextView tvDialogFolder = dialogView.findViewById(R.id.tvDialogFolder);
        RecyclerView rvBackups = dialogView.findViewById(R.id.rvBackups);
        AppCompatTextView tvEmptyBackups = dialogView.findViewById(R.id.tvEmptyBackups);

        String folderName = BackupHelper.getBackupFolderName(this);
        tvDialogFolder.setText(folderName != null ? "/" + folderName : "");

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).setNegativeButton(R.string.cancel, null).create();

        if (files == null || files.isEmpty()) {
            rvBackups.setVisibility(GONE);
            tvEmptyBackups.setVisibility(VISIBLE);
        } else {
            rvBackups.setLayoutManager(new LinearLayoutManager(this));
            BackupListAdapter adapter = new BackupListAdapter(info -> {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                confirmRestore(info);
            });
            adapter.setItems(files);
            rvBackups.setAdapter(adapter);
        }

        showDialogSafely(dialog);
    }

    private void confirmRestore(BackupFileInfo backupFileInfo) {
        if (!hasReadSmsPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSION_READ_SMS);
            return;
        }
        if (!isDefaultSmsApp()) {
            showDefaultSmsDialog(this);
            return;
        }

        showDialogSafely(new AlertDialog.Builder(this).setTitle(R.string.backup_restore_confirm_title).setMessage(R.string.backup_restore_confirm_message).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.restore, (d, which) -> performRestore(backupFileInfo)).create());
    }

    private void performRestore(BackupFileInfo backupFileInfo) {
        isRunning.set(true);
        stopRestore.set(false);
        setActionsEnabled(false);
        showProgressPanel(getString(R.string.backup_restoring), getString(R.string.backup_parsing), true);
        progressBar.setIndeterminate(true);

        executor.execute(() -> {
            try {
                BackupData backupData = BackupHelper.readBackupFile(this, backupFileInfo.getUri());
                if (stopRestore.get()) {
                    mainHandler.post(() -> {
                        if (canShowUi()) {
                            resetRunningState();
                        }
                    });
                    return;
                }

                mainHandler.post(() -> {
                    if (!canShowUi()) {
                        return;
                    }
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(backupData.getMessageCount());
                    progressBar.setProgress(0);
                });

                BackupHelper.restoreBackup(this, backupData, (stage, current, total) -> {
                    if (stopRestore.get()) {
                        return;
                    }
                    mainHandler.post(() -> {
                        if (!canShowUi()) {
                            return;
                        }
                        if ("parsing".equals(stage)) {
                            tvProgressSummary.setText(getString(R.string.backup_parsing));
                        } else if ("syncing".equals(stage)) {
                            progressBar.setIndeterminate(true);
                            tvProgressSummary.setText(getString(R.string.backup_syncing));
                        } else {
                            progressBar.setIndeterminate(false);
                            progressBar.setMax(total);
                            progressBar.setProgress(current);
                            tvProgressSummary.setText(getString(R.string.backup_progress, current, total));
                        }
                    });
                }, stopRestore);

                if (stopRestore.get()) {
                    mainHandler.post(() -> {
                        if (canShowUi()) {
                            resetRunningState();
                        }
                    });
                    return;
                }

                mainHandler.post(() -> {
                    if (!canShowUi()) {
                        return;
                    }
                    MessageListSyncHelper.notifySyncRequired();
                    finishProgress(getString(R.string.backup_restoring));
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!canShowUi()) {
                        return;
                    }
                    hideProgressPanel();
                    showErrorDialog(getString(R.string.backup_error_read));
                    resetRunningState();
                });
            }
        });
    }

    private void showStopRestoreDialog() {
        showDialogSafely(new AlertDialog.Builder(this).setTitle(R.string.backup_stop_restore_title).setMessage(R.string.backup_stop_restore_message).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.stop, (d, w) -> {
            stopRestore.set(true);
            hideProgressPanel();
            resetRunningState();
        }).create());
    }

    private void showProgressPanel(String title, String summary, boolean showStop) {
        llProgressPanel.setVisibility(VISIBLE);
        llBackupNow.setVisibility(GONE);
        tvProgressTitle.setText(title);
        tvProgressSummary.setText(summary);
        tvStopRestore.setVisibility(showStop ? VISIBLE : GONE);
    }

    private void updateProgress(String title, int current, int total) {
        tvProgressTitle.setText(title);
        if (total > 0) {
            progressBar.setMax(total);
            progressBar.setProgress(current);
            tvProgressSummary.setText(getString(R.string.backup_progress, current, total));
        }
    }

    private void finishProgress(String title) {
        progressBar.setIndeterminate(false);
        progressBar.setProgress(progressBar.getMax());
        tvProgressTitle.setText(title);
        tvProgressSummary.setText(getString(R.string.backup_finished));
        tvStopRestore.setVisibility(GONE);

        if (finishProgressRunnable != null) {
            mainHandler.removeCallbacks(finishProgressRunnable);
        }
        finishProgressRunnable = () -> {
            hideProgressPanel();
            resetRunningState();
        };
        mainHandler.postDelayed(finishProgressRunnable, 1000);
    }

    private void hideProgressPanel() {
        llProgressPanel.setVisibility(GONE);
        llBackupNow.setVisibility(VISIBLE);
        if (finishProgressRunnable != null) {
            mainHandler.removeCallbacks(finishProgressRunnable);
        }
    }

    private void resetRunningState() {
        isRunning.set(false);
        stopRestore.set(false);
        setActionsEnabled(true);
        hideProgressPanel();
    }

    private void setActionsEnabled(boolean enabled) {
        llBackupLocation.setEnabled(enabled);
        llRestore.setEnabled(enabled);
        llBackupNow.setEnabled(enabled);
        float alpha = enabled ? 1f : 0.5f;
        llBackupLocation.setAlpha(alpha);
        llRestore.setAlpha(alpha);
        llBackupNow.setAlpha(alpha);
    }

    private void showErrorDialog(String message) {
        showDialogSafely(new AlertDialog.Builder(this).setTitle(R.string.backup_error_title).setMessage(message).setPositiveButton(R.string.okay, null).create());
    }

    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isDefaultSmsApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_SMS);
            }
            String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this);
            return getPackageName().equals(defaultSmsPackage);
        } catch (Exception e) {
            return false;
        }
    }

    private void showDefaultSmsDialog(Activity activity) {
        Toast.makeText(activity, R.string.backup_default_app_required, Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = activity.getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                activity.startActivityForResult(intent, SMS_APP_REQUEST);
                activity.overridePendingTransition(0, 0);
            }
        } else {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_READ_SMS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (requestCode == PERMISSION_READ_SMS) {
            Toast.makeText(this, R.string.backup_permission_required, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTitle != null) {
            tvTitle.setText(getString(R.string.backup_and_restore));
            tvBackupLocationTitle.setText(getString(R.string.backup_location));
            tvRestoreTitle.setText(getString(R.string.restore));
            tvSelectBackup.setText(getString(R.string.select_a_backup));
            tvBackupInfo.setText(getString(R.string.currently_only_sms_is_supported_by_backup_and_restore_mms_support_and_scheduled_backups_will_be_coming_soon));
            tvStopRestore.setText(getString(R.string.stop));
            tvBackupNow.setText(getString(R.string.backup_now));
            updateBackupLocationUi();
        }
    }

    @Override
    protected void onDestroy() {
        if (finishProgressRunnable != null) {
            mainHandler.removeCallbacks(finishProgressRunnable);
        }
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }
}