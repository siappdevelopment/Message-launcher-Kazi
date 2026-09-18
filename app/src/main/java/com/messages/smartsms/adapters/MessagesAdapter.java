package com.messages.smartsms.adapters;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.messages.smartsms.R;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.Utils;
import com.messages.smartsms.interfaces.OnMessageActionListener;
import com.messages.smartsms.interfaces.OnMessageOpenListener;
import com.messages.smartsms.models.MessagesModel;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private final List<MessagesModel> listMessages;

    private OnMessageActionListener onMessageActionListener;
    private OnMessageOpenListener onMessageOpenListener;
    private boolean selectionMode;
    private final boolean showBannerAd;
    private boolean showBannerAdSlot = true;
    private boolean isBannerAdLoading = false;
    private boolean isBannerAdLoaded = false;
    private boolean bannerAdRemovePending = false;
    private boolean isQuizBannerBound = false;
    private boolean lastBoundAsQuizPriority = false;
    private boolean lastBoundAsNative = false;
    private boolean isNativeAdLoading = false;
    private boolean isNativeAdLoaded = false;
    private int bannerAdLoadToken = 0;
    private AdView cachedBannerAdView;
    private NativeAd cachedNativeAd;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Object QUIZ_BANNER_TAG = new Object();

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_AD = 1;

    public MessagesAdapter(Context context, List<MessagesModel> listMessages) {
        this(context, listMessages, false);
    }

    public MessagesAdapter(Context context, List<MessagesModel> listMessages, boolean showBannerAd) {
        this.context = context;
        this.listMessages = listMessages;
        this.showBannerAd = showBannerAd;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_AD) {
            return new AdsViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_messages_ads, parent, false));
        }
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_messages, parent, false));
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AdsViewHolder) {
            bindAdViewHolder((AdsViewHolder) holder);
            return;
        }

        ViewHolder messageHolder = (ViewHolder) holder;
        int messageIndex = getMessageIndex(position);
        MessagesModel message = listMessages.get(messageIndex);

        messageHolder.llMessageView.setBackgroundColor(selectionMode && message.isSelected() ? Color.LTGRAY : Color.WHITE);

        Bitmap photo = getContactPhoto(context, message.getAddress());
        String name = message.getName();

        if (photo != null) {
            messageHolder.ivImage.setVisibility(VISIBLE);
            messageHolder.tvName.setVisibility(GONE);
            messageHolder.ivImage.setImageBitmap(photo);
            messageHolder.ivImage.setPadding(0, 0, 0, 0);
        } else if (name != null && !name.isEmpty() && !name.equals(message.getAddress())) {
            messageHolder.ivImage.setVisibility(GONE);
            messageHolder.tvName.setVisibility(VISIBLE);
            messageHolder.tvName.setText(name.substring(0, 1).toUpperCase());
        } else {
            messageHolder.ivImage.setVisibility(VISIBLE);
            messageHolder.tvName.setVisibility(GONE);
            messageHolder.ivImage.setImageResource(R.drawable.ic_user);
            messageHolder.ivImage.setPadding(26, 26, 26, 26);
        }

        messageHolder.tvTitle.setText(name);
        messageHolder.tvDesc.setText(message.getBody());
        messageHolder.tvDate.setText(formatTime(message.getDate()));

        String draft = Utils.getDraft(context, message.getAddress());
        if (!draft.isEmpty()) {
            SpannableString spannable = new SpannableString("Draft: " + draft);
            spannable.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.primary)), 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.ITALIC), 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            messageHolder.tvDesc.setText(spannable);
        } else {
            messageHolder.tvDesc.setText(message.getBody());
        }

        if (message.getUnreadCount() > 0) {
            messageHolder.ivUnread.setVisibility(View.VISIBLE);
        } else {
            messageHolder.ivUnread.setVisibility(View.GONE);
        }

        messageHolder.ivPinned.setVisibility(message.isPinned() ? View.VISIBLE : View.GONE);

        if (messageIndex == listMessages.size() - 1) {
            messageHolder.ivDividerLine.setVisibility(INVISIBLE);
        } else {
            messageHolder.ivDividerLine.setVisibility(VISIBLE);
        }

        messageHolder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                message.setSelected(!message.isSelected());
                notifyItemChanged(position);
                if (onMessageActionListener != null) {
                    onMessageActionListener.onMessageClick(messageIndex, getSelectedCount());
                }
            } else if (onMessageOpenListener != null) {
                onMessageOpenListener.onMessageOpen(message);
            }
        });

        messageHolder.itemView.setOnLongClickListener(v -> {
            boolean wasSelectionMode = selectionMode;
            if (!selectionMode) {
                selectionMode = true;
            }
            boolean selectionChanged = !message.isSelected();
            message.setSelected(true);
            if (!wasSelectionMode) {
                notifyDataSetChanged();
            } else if (selectionChanged) {
                notifyItemChanged(position);
            }
            if (onMessageActionListener != null) {
                onMessageActionListener.onMessageLongClick(messageIndex, getSelectedCount());
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listMessages.size() + (shouldShowAd() ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (shouldShowAd() && position == 0) {
            return TYPE_AD;
        }
        return TYPE_MESSAGE;
    }

    public static class AdsViewHolder extends RecyclerView.ViewHolder {
        private final RelativeLayout rlAdView;
        private final RelativeLayout rlBannerAdView;
        private final RelativeLayout rlNativeAdView;
        private final ShimmerFrameLayout slBannerShimmer;
        private final ShimmerFrameLayout slNativeShimmer;
        private final LinearLayout llBannerAd;
        private final FrameLayout flNativeAd;

        public AdsViewHolder(@NonNull View itemView) {
            super(itemView);
            rlAdView = itemView.findViewById(R.id.rlAdView);
            rlBannerAdView = itemView.findViewById(R.id.rlBannerAdView);
            rlNativeAdView = itemView.findViewById(R.id.rlNativeAdView);
            slBannerShimmer = itemView.findViewById(R.id.slBannerShimmer);
            slNativeShimmer = itemView.findViewById(R.id.slNativeShimmer);
            llBannerAd = itemView.findViewById(R.id.llBannerAd);
            flNativeAd = itemView.findViewById(R.id.flNativeAd);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout llMessageView;
        private final AppCompatImageView ivImage, ivUnread, ivPinned, ivDividerLine;
        private final AppCompatTextView tvName, tvTitle, tvDate, tvDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            llMessageView = itemView.findViewById(R.id.llMessageView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvName = itemView.findViewById(R.id.tvName);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            ivUnread = itemView.findViewById(R.id.ivUnread);
            ivPinned = itemView.findViewById(R.id.ivPinned);
            ivDividerLine = itemView.findViewById(R.id.ivDividerLine);
        }
    }

    public void setActionListener(OnMessageActionListener onMessageActionListener) {
        this.onMessageActionListener = onMessageActionListener;
    }

    public void setOpenListener(OnMessageOpenListener onMessageOpenListener) {
        this.onMessageOpenListener = onMessageOpenListener;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        if (!selectionMode) {
            clearSelection();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearSelection() {
        for (MessagesModel messagesModel : listMessages) {
            messagesModel.setSelected(false);
        }
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        if (shouldShowAd() && position == 0) {
            return Long.MIN_VALUE;
        }
        int messageIndex = getMessageIndex(position);
        if (messageIndex < 0 || messageIndex >= listMessages.size()) {
            return RecyclerView.NO_ID;
        }

        MessagesModel message = listMessages.get(messageIndex);
        if (message == null) {
            return RecyclerView.NO_ID;
        }

        String threadId = message.getThreadId();
        if (threadId == null || threadId.trim().isEmpty()) {
            String address = message.getAddress();
            if (address != null && !address.isEmpty()) {
                return address.hashCode();
            }
            return messageIndex;
        }

        try {
            return Long.parseLong(threadId);
        } catch (NumberFormatException exception) {
            return threadId.hashCode();
        }
    }

    public int getAdRowOffset() {
        return shouldShowAd() ? 1 : 0;
    }

    public int getAdapterPositionForMessageIndex(int messageIndex) {
        return messageIndex + getAdRowOffset();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void bindAdViewHolder(AdsViewHolder holder) {
        if (!shouldShowAd()) {
            hideAdRow(holder);
            return;
        }

        if (bannerAdRemovePending) {
            hideAdRow(holder);
            return;
        }

        boolean wantNative = AdPlacement.isSmallNativeAdType(AdPlacement.getMessageListAdType());
        if (wantNative != lastBoundAsNative) {
            resetAdStateForTypeChange(holder);
            lastBoundAsNative = wantNative;
        }

        if (wantNative) {
            bindNativeAdViewHolder(holder);
            return;
        }

        bindBannerAdViewHolder(holder);
    }

    private void resetAdStateForTypeChange(AdsViewHolder holder) {
        bannerAdLoadToken++;
        disposeCachedBannerAd();
        disposeCachedNativeAd();
        clearQuizBannerTag(holder);
        clearNativeAdContainer(holder);
        isBannerAdLoaded = false;
        isBannerAdLoading = false;
        isNativeAdLoaded = false;
        isNativeAdLoading = false;
        isQuizBannerBound = false;
        lastBoundAsQuizPriority = false;
    }

    private void bindBannerAdViewHolder(AdsViewHolder holder) {
        showBannerAdContainer(holder);
        boolean wantQuiz = shouldUseQuizBanner();
        if (wantQuiz != lastBoundAsQuizPriority && (isBannerAdLoaded || isBannerAdLoading || cachedBannerAdView != null || isQuizBannerBound)) {
            bannerAdLoadToken++;
            disposeCachedBannerAd();
            clearQuizBannerTag(holder);
            isBannerAdLoaded = false;
            isBannerAdLoading = false;
            isQuizBannerBound = false;
            lastBoundAsQuizPriority = wantQuiz;
        }

        if (wantQuiz || isQuizBannerBound) {
            bindQuizBanner(holder);
            return;
        }

        if (isBannerAdLoaded && cachedBannerAdView != null) {
            attachCachedBannerAd(holder);
            return;
        }

        if (!AdPlacement.hasMessageListAdReloadIntervalElapsed() && !isBannerAdLoading) {
            return;
        }

        if (isBannerAdLoading && hasInFlightGoogleBanner(holder)) {
            resetAdRow(holder);
            holder.slBannerShimmer.setVisibility(VISIBLE);
            holder.llBannerAd.setVisibility(GONE);
            return;
        }

        resetAdRow(holder);
        isBannerAdLoading = true;
        final int loadToken = bannerAdLoadToken;
        AdPlacement.loadBannerAd(context, AdPlacement.getMessageListBannerId(), holder.rlBannerAdView, holder.slBannerShimmer, holder.llBannerAd, () -> {
            if (loadToken != bannerAdLoadToken) {
                return;
            }
            onBannerAdFailed();
        }, adView -> {
            if (loadToken != bannerAdLoadToken) {
                destroyBannerAdView(adView);
                return;
            }
            isBannerAdLoading = false;
            isBannerAdLoaded = true;
            AdPlacement.markMessageListAdLoaded();
            lastBoundAsQuizPriority = false;
            if (adView == null) {
                isQuizBannerBound = true;
                cachedBannerAdView = null;
                holder.llBannerAd.setTag(QUIZ_BANNER_TAG);
                holder.slBannerShimmer.setVisibility(GONE);
                holder.llBannerAd.setVisibility(VISIBLE);
                holder.rlBannerAdView.setVisibility(VISIBLE);
                return;
            }
            isQuizBannerBound = false;
            cachedBannerAdView = adView;
            holder.slBannerShimmer.setVisibility(GONE);
            holder.llBannerAd.setVisibility(VISIBLE);
            holder.rlBannerAdView.setVisibility(VISIBLE);
            attachCachedBannerAd(holder);
        });
    }

    private void bindNativeAdViewHolder(AdsViewHolder holder) {
        showNativeAdContainer(holder);
        if (isNativeAdLoaded && cachedNativeAd != null && holder.flNativeAd.getChildCount() > 0) {
            resetAdRow(holder);
            holder.slNativeShimmer.setVisibility(GONE);
            holder.flNativeAd.setVisibility(VISIBLE);
            holder.rlNativeAdView.setVisibility(VISIBLE);
            return;
        }
        if (isNativeAdLoaded && cachedNativeAd != null && holder.flNativeAd.getChildCount() == 0) {
            attachCachedNativeAd(holder);
            return;
        }

        if (!AdPlacement.hasMessageListAdReloadIntervalElapsed() && !isNativeAdLoading) {
            return;
        }

        if (isNativeAdLoading) {
            resetAdRow(holder);
            holder.slNativeShimmer.setVisibility(VISIBLE);
            holder.flNativeAd.setVisibility(GONE);
            return;
        }

        resetAdRow(holder);
        clearNativeAdContainer(holder);
        isNativeAdLoading = true;
        final int loadToken = bannerAdLoadToken;
        AdPlacement.loadNativeAd(context, AdPlacement.getMessageListNativeId(), holder.rlNativeAdView, holder.slNativeShimmer, holder.flNativeAd, "small", nativeAd -> {
            if (loadToken != bannerAdLoadToken) {
                nativeAd.destroy();
                return;
            }
            disposeCachedNativeAd();
            cachedNativeAd = nativeAd;
            isNativeAdLoading = false;
            isNativeAdLoaded = true;
            AdPlacement.markMessageListAdLoaded();
            holder.slNativeShimmer.setVisibility(GONE);
            holder.flNativeAd.setVisibility(VISIBLE);
            holder.rlNativeAdView.setVisibility(VISIBLE);
        }, () -> {
            if (loadToken != bannerAdLoadToken) {
                return;
            }
            onNativeAdFailed();
        });
    }

    private void onNativeAdFailed() {
        isNativeAdLoading = false;
        isNativeAdLoaded = false;
    }

    private void showBannerAdContainer(AdsViewHolder holder) {
        holder.rlNativeAdView.setVisibility(GONE);
        holder.slNativeShimmer.setVisibility(GONE);
        holder.flNativeAd.setVisibility(GONE);
        holder.rlBannerAdView.setVisibility(VISIBLE);
    }

    private void showNativeAdContainer(AdsViewHolder holder) {
        holder.rlBannerAdView.setVisibility(GONE);
        holder.slBannerShimmer.setVisibility(GONE);
        holder.llBannerAd.setVisibility(GONE);
        holder.rlNativeAdView.setVisibility(VISIBLE);
    }

    private void clearNativeAdContainer(AdsViewHolder holder) {
        if (holder.flNativeAd != null) {
            holder.flNativeAd.removeAllViews();
            holder.flNativeAd.setVisibility(GONE);
        }
    }

    private void attachCachedNativeAd(AdsViewHolder holder) {
        if (cachedNativeAd == null || holder.flNativeAd == null) {
            return;
        }
        resetAdRow(holder);
        holder.flNativeAd.removeAllViews();
        NativeAdView adView = (NativeAdView) LayoutInflater.from(context).inflate(R.layout.native_small_ad_layout, holder.flNativeAd, false);
        AdPlacement.populateNativeAdView(cachedNativeAd, adView, "small");
        holder.flNativeAd.addView(adView);
        holder.slNativeShimmer.setVisibility(GONE);
        holder.flNativeAd.setVisibility(VISIBLE);
        holder.rlNativeAdView.setVisibility(VISIBLE);
    }

    private void disposeCachedNativeAd() {
        if (cachedNativeAd != null) {
            cachedNativeAd.destroy();
            cachedNativeAd = null;
        }
    }

    private boolean shouldUseQuizBanner() {
        return AdPlacement.shouldUseQuizPriority();
    }

    private void bindQuizBanner(AdsViewHolder holder) {
        resetAdRow(holder);
        if (isQuizBannerBound && holder.llBannerAd.getChildCount() > 0 && QUIZ_BANNER_TAG.equals(holder.llBannerAd.getTag())) {
            holder.slBannerShimmer.setVisibility(GONE);
            holder.llBannerAd.setVisibility(VISIBLE);
            holder.rlBannerAdView.setVisibility(VISIBLE);
            return;
        }
        if (isBannerAdLoading) {
            holder.slBannerShimmer.setVisibility(VISIBLE);
            holder.llBannerAd.setVisibility(GONE);
            return;
        }

        disposeCachedBannerAd();
        isBannerAdLoading = true;
        final int loadToken = bannerAdLoadToken;
        AdPlacement.loadBannerAd(context, AdPlacement.getMessageListBannerId(), holder.rlBannerAdView, holder.slBannerShimmer, holder.llBannerAd, () -> {
            if (loadToken != bannerAdLoadToken) {
                return;
            }
            isQuizBannerBound = false;
            onBannerAdFailed();
        }, adView -> {
            if (loadToken != bannerAdLoadToken) {
                destroyBannerAdView(adView);
                return;
            }
            isBannerAdLoading = false;
            isBannerAdLoaded = true;
            AdPlacement.markMessageListAdLoaded();
            isQuizBannerBound = true;
            lastBoundAsQuizPriority = true;
            cachedBannerAdView = null;
            if (adView != null) {
                destroyBannerAdView(adView);
            }
            holder.llBannerAd.setTag(QUIZ_BANNER_TAG);
            holder.slBannerShimmer.setVisibility(GONE);
            holder.llBannerAd.setVisibility(VISIBLE);
            holder.rlBannerAdView.setVisibility(VISIBLE);
        });
    }

    private void clearQuizBannerTag(AdsViewHolder holder) {
        holder.llBannerAd.setTag(null);
        holder.llBannerAd.removeAllViews();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void reloadBannerAd() {
        if (!showBannerAd) {
            return;
        }

        if (!AdPlacement.hasMessageListAdReloadIntervalElapsed()) {
            if (!isBannerAdLoaded && !isNativeAdLoaded && cachedBannerAdView == null) {
                return;
            }
            showBannerAdSlot = true;
            if (AdPlacement.shouldShowMessageListAd() && !listMessages.isEmpty()) {
                try {
                    notifyItemChanged(0);
                } catch (IllegalStateException exception) {
                    mainHandler.post(this::notifyDataSetChanged);
                }
            }
            return;
        }

        if ((isBannerAdLoading || isNativeAdLoading) && !isBannerAdLoaded && !isNativeAdLoaded && cachedBannerAdView == null) {
            return;
        }

        bannerAdLoadToken++;
        disposeCachedBannerAd();
        disposeCachedNativeAd();
        isBannerAdLoaded = false;
        isBannerAdLoading = false;
        isQuizBannerBound = false;
        lastBoundAsQuizPriority = AdPlacement.shouldUseQuizPriority();
        lastBoundAsNative = AdPlacement.isSmallNativeAdType(AdPlacement.getMessageListAdType());
        isNativeAdLoaded = false;
        isNativeAdLoading = false;
        bannerAdRemovePending = false;

        boolean wasShowingSlot = showBannerAdSlot;
        showBannerAdSlot = true;

        if (!AdPlacement.shouldShowMessageListAd() || listMessages.isEmpty()) {
            return;
        }

        try {
            if (!wasShowingSlot) {
                notifyItemInserted(0);
            } else {
                notifyItemChanged(0);
            }
        } catch (IllegalStateException exception) {
            mainHandler.post(this::notifyDataSetChanged);
        }
    }

    private void disposeCachedBannerAd() {
        destroyBannerAdView(cachedBannerAdView);
        cachedBannerAdView = null;
    }

    private void destroyBannerAdView(AdView adView) {
        if (adView == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) adView.getParent();
        if (parent != null) {
            parent.removeView(adView);
        }
        adView.destroy();
    }

    private void onBannerAdFailed() {
        isBannerAdLoading = false;
        isBannerAdLoaded = false;
        isQuizBannerBound = false;
        lastBoundAsQuizPriority = false;
        disposeCachedBannerAd();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void removeBannerAdSlotSafely() {
        if (!showBannerAdSlot || bannerAdRemovePending) {
            return;
        }
        bannerAdRemovePending = true;
        mainHandler.post(() -> {
            bannerAdRemovePending = false;
            if (!showBannerAdSlot) {
                return;
            }
            showBannerAdSlot = false;
            try {
                notifyItemRemoved(0);
            } catch (IllegalStateException exception) {
                mainHandler.post(this::notifyDataSetChanged);
            }
        });
    }

    private void attachCachedBannerAd(AdsViewHolder holder) {
        resetAdRow(holder);
        holder.slBannerShimmer.setVisibility(GONE);
        holder.llBannerAd.setVisibility(VISIBLE);
        holder.rlBannerAdView.setVisibility(VISIBLE);
        holder.itemView.setVisibility(VISIBLE);

        if (cachedBannerAdView == null) {
            return;
        }

        ViewGroup currentParent = (ViewGroup) cachedBannerAdView.getParent();
        if (currentParent != null && currentParent != holder.llBannerAd) {
            currentParent.removeView(cachedBannerAdView);
        }
        if (cachedBannerAdView.getParent() == null) {
            holder.llBannerAd.removeAllViews();
            holder.llBannerAd.addView(cachedBannerAdView);
        }
    }

    private boolean hasInFlightGoogleBanner(AdsViewHolder holder) {
        if (cachedBannerAdView != null) {
            return true;
        }
        if (holder.llBannerAd.getChildCount() == 0) {
            return false;
        }
        View child = holder.llBannerAd.getChildAt(0);
        return child instanceof AdView;
    }

    private boolean shouldShowAd() {
        return showBannerAd && showBannerAdSlot && AdPlacement.shouldShowMessageListAd() && !listMessages.isEmpty();
    }

    private void hideAdRow(AdsViewHolder holder) {
        if (holder.rlAdView != null) {
            holder.rlAdView.setVisibility(GONE);
        }
        holder.rlBannerAdView.setVisibility(GONE);
        holder.rlNativeAdView.setVisibility(GONE);
        holder.slBannerShimmer.setVisibility(GONE);
        holder.slNativeShimmer.setVisibility(GONE);
        holder.llBannerAd.setVisibility(GONE);
        holder.flNativeAd.setVisibility(GONE);
        holder.itemView.setVisibility(GONE);
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = 0;
            holder.itemView.setLayoutParams(layoutParams);
        }
    }

    private void resetAdRow(AdsViewHolder holder) {
        holder.itemView.setVisibility(VISIBLE);
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.itemView.setLayoutParams(layoutParams);
        }
        if (holder.rlAdView != null) {
            holder.rlAdView.setVisibility(VISIBLE);
        }
    }

    private int getMessageIndex(int adapterPosition) {
        return shouldShowAd() ? adapterPosition - 1 : adapterPosition;
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

    private String formatTime(long dateMillis) {
        Calendar messageCalendar = Calendar.getInstance();
        messageCalendar.setTimeInMillis(dateMillis);
        Calendar todayCalendar = Calendar.getInstance();

        if (isSameDay(messageCalendar, todayCalendar)) {
            Locale locale = Locale.forLanguageTag(Utils.getAppLanguageNew(context));
            return new SimpleDateFormat("h:mm a", locale).format(new Date(dateMillis));
        }

        Calendar yesterdayCalendar = Calendar.getInstance();
        yesterdayCalendar.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(messageCalendar, yesterdayCalendar)) {
            Context localized = Utils.localeResourcesContext(context);
            return localized.getString(R.string.yesterday);
        }

        if (messageCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) && messageCalendar.get(Calendar.WEEK_OF_YEAR) == todayCalendar.get(Calendar.WEEK_OF_YEAR)) {
            return new SimpleDateFormat("EEE", Locale.ENGLISH).format(new Date(dateMillis));
        }

        return new SimpleDateFormat("d MMM", Locale.ENGLISH).format(new Date(dateMillis));
    }

    private boolean isSameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    public List<MessagesModel> getSelectedMessages() {
        List<MessagesModel> selected = new ArrayList<>();
        for (MessagesModel messagesModel : listMessages) {
            if (messagesModel.isSelected()) {
                selected.add(messagesModel);
            }
        }
        return selected;
    }

    public int getSelectedCount() {
        int count = 0;
        for (MessagesModel messagesModel : listMessages) {
            if (messagesModel.isSelected()) {
                count++;
            }
        }
        return count;
    }
}