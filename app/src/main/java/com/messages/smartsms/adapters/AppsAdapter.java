package com.messages.smartsms.adapters;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.smartsms.R;
import com.messages.smartsms.activities.LauncherHomeActivity;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.Utils;
import com.messages.smartsms.helpers.LauncherAppsIconCache;
import com.messages.smartsms.interfaces.OnAppsUninstallClickListener;
import com.messages.smartsms.models.AppsModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.ViewHolder> {
    private final Context context;
    private final List<AppsModel> arrayListApps;
    private final OnAppsUninstallClickListener onAppsUninstallClickListener;

    private int iconSize;
    private int cornerRadiusPx;
    private boolean labelVisible;
    private float labelSize;

    public AppsAdapter(Context context, List<AppsModel> arrayListApps, OnAppsUninstallClickListener onAppsUninstallClickListener) {
        this.context = context;
        this.arrayListApps = arrayListApps;
        this.onAppsUninstallClickListener = onAppsUninstallClickListener;

        refreshSizeVisibility();
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_apps, parent, false));
        holder.ivAppIcon.setScaleType(AppCompatImageView.ScaleType.CENTER_CROP);
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            AppsModel appsModel = getAppAt(adapterPosition);
            if (appsModel == null) {
                return;
            }
            String packageName = appsModel.getPackageName();
            if (context instanceof Activity) {
                AdPlacement.handleLauncherAppClickAd((Activity) context, () -> launchApp(packageName));
            } else {
                launchApp(packageName);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            AppsModel appsModel = getAppAt(adapterPosition);
            if (appsModel == null) {
                return false;
            }
            showAppDialog(v, appsModel.getAppName(), appsModel.getPackageName());
            return true;
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppsModel appsModel = getAppAt(position);
        if (appsModel == null) {
            return;
        }

        String packageName = appsModel.getPackageName();

        if (holder.boundIconSizePx != iconSize) {
            holder.setIconSize(iconSize);
            holder.boundIconSizePx = iconSize;
        }

        Drawable displayIcon = packageName == null ? null : LauncherAppsIconCache.get(packageName, iconSize);
        if (displayIcon == null) {
            displayIcon = LauncherAppsIconCache.resolveDisplayIcon(context, packageName, appsModel.getAppIcon(), iconSize, cornerRadiusPx);
        }
        if (displayIcon != null) {
            if (holder.ivAppIcon.getDrawable() != displayIcon) {
                holder.ivAppIcon.setImageDrawable(displayIcon);
            }
        } else {
            holder.ivAppIcon.setImageResource(R.mipmap.ic_launcher);
        }

        holder.tvAppName.setText(appsModel.getAppName());
        if (holder.boundLabelSizeSp != labelSize) {
            holder.tvAppName.setTextSize(labelSize);
            holder.boundLabelSizeSp = labelSize;
        }
        int labelVisibility = labelVisible ? View.VISIBLE : View.GONE;
        if (holder.tvAppName.getVisibility() != labelVisibility) {
            holder.tvAppName.setVisibility(labelVisibility);
        }
    }

    @Override
    public int getItemCount() {
        synchronized (arrayListApps) {
            return arrayListApps.size();
        }
    }

    @Override
    public long getItemId(int position) {
        AppsModel appsModel = getAppAt(position);
        if (appsModel == null) {
            return RecyclerView.NO_ID;
        }
        String packageName = appsModel.getPackageName();
        return packageName != null ? packageName.hashCode() : position;
    }

    @Nullable
    private AppsModel getAppAt(int position) {
        if (position < 0) {
            return null;
        }
        try {
            synchronized (arrayListApps) {
                if (position >= arrayListApps.size()) {
                    return null;
                }
                return arrayListApps.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView ivAppIcon;
        private final AppCompatTextView tvAppName;
        int boundIconSizePx = -1;
        float boundLabelSizeSp = -1f;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
        }

        public void setIconSize(int iconSize) {
            ViewGroup.LayoutParams params = ivAppIcon.getLayoutParams();
            params.width = iconSize;
            params.height = iconSize;
            ivAppIcon.setLayoutParams(params);
        }
    }

    private void launchApp(String packageName) {
        try {
            if (LauncherHomeActivity.appsFolderDialog != null && LauncherHomeActivity.appsFolderDialog.isShowing()) {
                LauncherHomeActivity.appsFolderDialog.dismiss();
            }

            setRecentApp(packageName);
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                Utils.clearActivityTransition(context);
            }
        } catch (Exception ignored) {
        }
    }

    private void setRecentApp(String packageName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("recentThread", MODE_PRIVATE);
        String oldData = sharedPreferences.getString("packages", "");

        ArrayList<String> list = new ArrayList<>();
        if (!oldData.isEmpty()) {
            list.addAll(Arrays.asList(oldData.split(",")));
        }
        list.remove(packageName);
        list.add(0, packageName);

        while (list.size() > 4) {
            list.remove(list.size() - 1);
        }

        sharedPreferences.edit().putString("packages", TextUtils.join(",", list)).apply();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSizeVisiblity() {
        refreshSizeVisibility();
        notifyDataSetChanged();
    }

    private void refreshSizeVisibility() {
        iconSize = Utils.dpToPx(context, Utils.getAppIconSize(context));
        cornerRadiusPx = Utils.getLauncherAppIconCornerRadiusPx(context);
        labelVisible = Utils.getLabelVisibility(context);
        labelSize = Utils.getAppLabelSize(context);
    }

    @SuppressLint("InflateParams")
    private void showAppDialog(View view, String appName, String appPackage) {
        Context context = view.getContext();
        View popupView = LayoutInflater.from(context).inflate(R.layout.dialog_app_action, null);
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(10f);

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = popupView.getMeasuredHeight();

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        if (location[1] + view.getHeight() + popupHeight > screenHeight) {
            popupWindow.showAsDropDown(view, 0, -(popupHeight + view.getHeight()));
        } else {
            popupWindow.showAsDropDown(view);
        }

        AppCompatTextView tvAppName = popupView.findViewById(R.id.tvAppName);
        AppCompatImageView ivAppInfo = popupView.findViewById(R.id.ivAppInfo);
        LinearLayout llAppOpen = popupView.findViewById(R.id.llAppOpen);
        LinearLayout llAppUninstall = popupView.findViewById(R.id.llAppUninstall);

        if (appPackage.equals(context.getPackageName())) {
            llAppUninstall.setVisibility(GONE);
        } else {
            llAppUninstall.setVisibility(VISIBLE);
        }

        tvAppName.setText(appName);

        ivAppInfo.setOnClickListener(view1 -> {
            popupWindow.dismiss();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + appPackage));
            context.startActivity(intent);
            Utils.clearActivityTransition(context);
        });

        llAppOpen.setOnClickListener(view1 -> {
            popupWindow.dismiss();
            if (context instanceof Activity) {
                AdPlacement.handleLauncherAppClickAd((Activity) context, () -> launchApp(appPackage));
            } else {
                launchApp(appPackage);
            }
        });

        llAppUninstall.setOnClickListener(view1 -> {
            popupWindow.dismiss();
            onAppsUninstallClickListener.onAppsUninstallClick(appPackage);
        });
    }
}