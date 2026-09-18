package com.messages.smartsms.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.messages.smartsms.R;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.Utils;

public class IntroSwipePagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_INTRO = 0;
    private static final int TYPE_NATIVE = 1;
    private static final int NATIVE_PAGE_POSITION = 2;
    private static final int INDICATOR_COUNT = 4;

    public interface OnIntroSwipeListener {
        void onNextClick(int position);
    }

    private final Context context;
    private final OnIntroSwipeListener listener;

    public IntroSwipePagerAdapter(Context context, OnIntroSwipeListener listener) {
        this.context = context;
        this.listener = listener;
    }

    private boolean isNativeAdEnabled() {
        return AdPlacement.getIntroSwipeNativeAdShow();
    }

    private int getPageCount() {
        return isNativeAdEnabled() ? 5 : 4;
    }

    private boolean isNativePage(int position) {
        return isNativeAdEnabled() && position == NATIVE_PAGE_POSITION;
    }

    private int getIntroContentIndex(int position) {
        if (isNativePage(position)) {
            return -1;
        }
        if (!isNativeAdEnabled()) {
            return position;
        }
        return position < NATIVE_PAGE_POSITION ? position : position - 1;
    }

    private int getActiveIndicatorIndex(int position) {
        if (!isNativeAdEnabled()) {
            return position;
        }
        if (position <= 1) {
            return position;
        }
        if (position == NATIVE_PAGE_POSITION) {
            return 2;
        }
        return position - 1;
    }

    @Override
    public int getItemViewType(int position) {
        return isNativePage(position) ? TYPE_NATIVE : TYPE_INTRO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_NATIVE) {
            return new NativeViewHolder(inflater.inflate(R.layout.item_intro_swipe_native_page, parent, false));
        }
        return new IntroViewHolder(inflater.inflate(R.layout.item_intro_swipe_page, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof IntroViewHolder) {
            ((IntroViewHolder) holder).bind(position);
        } else if (holder instanceof NativeViewHolder) {
            ((NativeViewHolder) holder).bind(position);
        }
    }

    @Override
    public int getItemCount() {
        return getPageCount();
    }

    private void setupIndicators(LinearLayout llIndicator, int activePosition) {
        llIndicator.removeAllViews();
        int activeIndex = getActiveIndicatorIndex(activePosition);
        int activeWidth = Utils.dpToPx(context, 18);
        int inactiveWidth = Utils.dpToPx(context, 6);
        int dotHeight = Utils.dpToPx(context, 6);
        int margin = Utils.dpToPx(context, 2);

        for (int i = 0; i < INDICATOR_COUNT; i++) {
            AppCompatImageView dot = new AppCompatImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(i == activeIndex ? activeWidth : inactiveWidth, dotHeight);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setAdjustViewBounds(true);
            dot.setImageResource(i == activeIndex ? R.drawable.custom_button_round : R.drawable.custom_circle);
            dot.setColorFilter(ContextCompat.getColor(context, i == activeIndex ? R.color.primary : R.color.gray));
            llIndicator.addView(dot);
        }
    }

    private class IntroViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView ivIntroImage;
        private final LinearLayout llIndicator;
        private final AppCompatTextView tvTitle;
        private final AppCompatTextView tvDescription;
        private final AppCompatTextView tvNext;
        private final ShimmerFrameLayout btnNext;
        private final RelativeLayout rlNativeAdView;
        private final ShimmerFrameLayout slNativeShimmer;
        private final FrameLayout flNativeAd;

        IntroViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIntroImage = itemView.findViewById(R.id.ivIntroImage);
            llIndicator = itemView.findViewById(R.id.llIndicator);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvNext = itemView.findViewById(R.id.tvNext);
            btnNext = itemView.findViewById(R.id.btnNext);
            rlNativeAdView = itemView.findViewById(R.id.rlNativeAdView);
            slNativeShimmer = itemView.findViewById(R.id.slNativeShimmer);
            flNativeAd = itemView.findViewById(R.id.flNativeAd);
        }

        void bind(int position) {
            int introIndex = getIntroContentIndex(position);
            switch (introIndex) {
                case 0:
                    ivIntroImage.setImageResource(R.drawable.img_intro_1);
                    tvTitle.setText(context.getString(R.string.intro_1_title));
                    tvDescription.setText(context.getString(R.string.intro_1_description));
                    break;
                case 1:
                    ivIntroImage.setImageResource(R.drawable.img_intro_2);
                    tvTitle.setText(context.getString(R.string.intro_2_title));
                    tvDescription.setText(context.getString(R.string.intro_2_description));
                    break;
                case 2:
                    ivIntroImage.setImageResource(R.drawable.img_intro_3);
                    tvTitle.setText(context.getString(R.string.intro_3_title));
                    tvDescription.setText(context.getString(R.string.intro_3_description));
                    break;
                case 3:
                    ivIntroImage.setImageResource(R.drawable.img_intro_4);
                    tvTitle.setText(context.getString(R.string.intro_4_title));
                    tvDescription.setText(context.getString(R.string.intro_4_description));
                    break;
                default:
                    break;
            }

            if (tvNext != null) {
                tvNext.setText(context.getString(R.string.next));
            }
            setupIndicators(llIndicator, position);
            btnNext.setOnClickListener(view -> listener.onNextClick(position));
            setupBottomNativeAd(introIndex);
        }

        private void setupBottomNativeAd(int introIndex) {
            rlNativeAdView.setVisibility(View.GONE);
            flNativeAd.removeAllViews();

            if (!isNativeAdEnabled()) {
                return;
            }

            String nativeId = null;
            if (introIndex == 0) {
                nativeId = AdPlacement.getIntroSwipeNativeId1();
            } else if (introIndex == 3) {
                nativeId = AdPlacement.getIntroSwipeNativeId3();
            }

            if (nativeId == null || nativeId.trim().isEmpty()) {
                return;
            }

            rlNativeAdView.setVisibility(View.VISIBLE);
            AdPlacement.loadNativeAd(context, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, "large");
        }
    }

    private class NativeViewHolder extends RecyclerView.ViewHolder {
        private final RelativeLayout rlNativeAdView;
        private final ShimmerFrameLayout slNativeShimmer;
        private final FrameLayout flNativeAd;
        private final AppCompatImageView ivClose;

        NativeViewHolder(@NonNull View itemView) {
            super(itemView);
            rlNativeAdView = itemView.findViewById(R.id.rlNativeAdView);
            slNativeShimmer = itemView.findViewById(R.id.slNativeShimmer);
            flNativeAd = itemView.findViewById(R.id.flNativeAd);
            ivClose = itemView.findViewById(R.id.ivClose);
        }

        void bind(int position) {
            ivClose.setOnClickListener(view -> listener.onNextClick(position));
            flNativeAd.removeAllViews();

            String nativeId = AdPlacement.getIntroSwipeNativeId2();
            if (nativeId == null || nativeId.trim().isEmpty()) {
                rlNativeAdView.setVisibility(View.GONE);
                return;
            }

            rlNativeAdView.setVisibility(View.VISIBLE);
            AdPlacement.loadNativeAd(context, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, "full");
        }
    }
}