package com.messages.smartsms.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.messages.smartsms.models.AppsModel;

import java.util.ArrayList;
import java.util.List;

public final class LauncherAppsIconCache {
    private static final LruCache<String, Drawable> CACHE = new LruCache<>(512);

    private LauncherAppsIconCache() {
    }

    @Nullable
    public static Drawable get(@NonNull String packageName, int sizePx) {
        return CACHE.get(cacheKey(packageName, sizePx));
    }

    @Nullable
    public static Drawable resolveDisplayIcon(@NonNull Context context, @Nullable String packageName, @Nullable Drawable source, int sizePx, int cornerRadiusPx) {
        Drawable displayIcon = packageName != null ? get(packageName, sizePx) : null;
        if (displayIcon == null) {
            displayIcon = createDisplayIcon(context, source, sizePx, cornerRadiusPx);
        }
        if (displayIcon != null) {
            return displayIcon;
        }
        return source;
    }

    public static void warm(@NonNull Context context, @Nullable List<AppsModel> apps, int sizePx, int cornerRadiusPx) {
        if (apps == null || apps.isEmpty() || sizePx <= 0) {
            return;
        }
        Context appContext = context.getApplicationContext();
        List<AppsModel> snapshot = new ArrayList<>(apps);
        for (AppsModel app : snapshot) {
            if (app == null) {
                continue;
            }
            String packageName = app.getPackageName();
            if (packageName == null || packageName.isEmpty() || CACHE.get(cacheKey(packageName, sizePx)) != null) {
                continue;
            }
            Drawable displayIcon = createDisplayIcon(appContext, app.getAppIcon(), sizePx, cornerRadiusPx);
            if (displayIcon != null) {
                CACHE.put(cacheKey(packageName, sizePx), displayIcon);
            }
        }
    }

    @Nullable
    public static Drawable createDisplayIcon(@NonNull Context context, @Nullable Drawable source, int sizePx, int cornerRadiusPx) {
        if (source == null || sizePx <= 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        source.setBounds(0, 0, sizePx, sizePx);
        source.draw(canvas);
        RoundedBitmapDrawable rounded = RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
        rounded.setCornerRadius(cornerRadiusPx);
        rounded.setAntiAlias(true);
        rounded.setFilterBitmap(true);
        return rounded;
    }

    @NonNull
    private static String cacheKey(@NonNull String packageName, int sizePx) {
        return packageName + "@" + sizePx;
    }
}