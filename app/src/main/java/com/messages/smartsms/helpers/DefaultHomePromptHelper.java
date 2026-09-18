package com.messages.smartsms.helpers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.messages.smartsms.common.Utils;

public final class DefaultHomePromptHelper {
    public static final long POLL_INTERVAL_MS = 500L;

    @NonNull
    private final Fragment fragment;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout llDefault;
    private ActivityResultLauncher<Intent> roleLauncher;
    private boolean waitingForDefaultHome;
    private boolean defaultHomeSelectionStarted;
    private boolean launcherRegistered;

    public DefaultHomePromptHelper(@NonNull Fragment fragment) {
        this.fragment = fragment;
    }

    public void registerRoleLauncher() {
        if (launcherRegistered) {
            return;
        }
        launcherRegistered = true;
        ActivityResultContracts.StartActivityForResult contract = new ActivityResultContracts.StartActivityForResult();
        roleLauncher = fragment.registerForActivityResult(contract, result -> onRoleResult());
    }

    public void bind(@Nullable LinearLayout llDefault) {
        this.llDefault = llDefault;
        updateVisibility();
    }

    public void updateVisibility() {
        if (llDefault == null || !isActive()) {
            return;
        }
        llDefault.setVisibility(Utils.isDefaultHomeApp(getContext()) ? GONE : VISIBLE);
    }

    public void handleSetAsDefaultClick() {
        if (!isActive()) {
            return;
        }
        Activity hostActivity = getHostActivity();
        if (hostActivity == null) {
            return;
        }
        if (Utils.isDefaultHomeApp(getContext())) {
            completeSuccess();
            return;
        }
        waitingForDefaultHome = true;
        defaultHomeSelectionStarted = true;
        Intent roleIntent = Utils.createDefaultHomeRoleRequestIntent(getContext());
        if (roleIntent != null && roleLauncher != null) {
            roleLauncher.launch(roleIntent);
            return;
        }
        if (!Utils.openDefaultHomeChooser(hostActivity)) {
            completeCancelled();
            return;
        }
        startPolling();
    }

    public void onResume() {
        if (waitingForDefaultHome && isActive() && Utils.isDefaultHomeApp(getContext())) {
            completeSuccess();
        } else {
            updateVisibility();
        }
    }

    public void release() {
        waitingForDefaultHome = false;
        defaultHomeSelectionStarted = false;
        stopPolling();
        llDefault = null;
    }

    private void onRoleResult() {
        if (!waitingForDefaultHome) {
            updateVisibility();
            return;
        }
        if (!isActive()) {
            return;
        }
        if (Utils.isDefaultHomeApp(getContext())) {
            completeSuccess();
        } else {
            completeCancelled();
        }
    }

    private void completeSuccess() {
        waitingForDefaultHome = false;
        defaultHomeSelectionStarted = false;
        stopPolling();
        updateVisibility();
        if (!isActive() || !Utils.isDefaultHomeApp(getContext())) {
            return;
        }
        Utils.setDefaultHomeFlowCompleted(getContext(), true);
        Utils.trackScreenOnce(getContext(), "DEFAULT_HOME_APP_SET");
    }

    private void completeCancelled() {
        waitingForDefaultHome = false;
        stopPolling();
        updateVisibility();
        if (defaultHomeSelectionStarted && isActive()) {
            Utils.trackScreenOnce(getContext(), "DEFAULT_HOME_APP_CANCEL");
            defaultHomeSelectionStarted = false;
        }
    }

    private void startPolling() {
        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void stopPolling() {
        handler.removeCallbacks(pollRunnable);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isActive() || !waitingForDefaultHome) {
                return;
            }
            Activity hostActivity = getHostActivity();
            if (hostActivity == null || hostActivity.isFinishing()) {
                return;
            }
            if (Utils.isDefaultHomeApp(getContext())) {
                completeSuccess();
                return;
            }
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    private boolean isActive() {
        return fragment.isAdded();
    }

    @Nullable
    private Activity getHostActivity() {
        return fragment.getActivity();
    }

    @Nullable
    private Context getContext() {
        return fragment.getContext();
    }
}