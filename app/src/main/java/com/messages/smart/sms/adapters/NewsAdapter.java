package com.messages.smart.sms.adapters;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.messages.smart.sms.R;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.models.NewsModel;

import java.util.ArrayList;

public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private final ArrayList<NewsModel> arrayListNewsModel;

    private final int TYPE_AD = 1;
    private boolean isAd1Loaded;
    private boolean isAd2Loaded;

    public NewsAdapter(Context context, ArrayList<NewsModel> arrayListNewsModel) {
        this.context = context;
        this.arrayListNewsModel = arrayListNewsModel;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_AD) {
            return new AdsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_news_ads, parent, false));
        } else {
            return new NewsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_news, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NewsViewHolder) {
            NewsViewHolder newsViewHolder = (NewsViewHolder) holder;
            NewsModel newsModel = arrayListNewsModel.get(position);
            Glide.with(context).load(newsModel.getImage()).into(newsViewHolder.ivNewsImage);
            newsViewHolder.tvNewsTitle.setText(newsModel.getTitle());
            newsViewHolder.tvNewsDesc.setText(newsModel.getDescription());

            newsViewHolder.llReadMore.setOnClickListener(view -> {
                String newsUrl = newsModel.getUrl();
                if (newsUrl == null || newsUrl.trim().isEmpty()) {
                    return;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newsUrl));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    Utils.clearActivityTransition(context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else if (holder instanceof AdsViewHolder) {
            bindAdViewHolder((AdsViewHolder) holder, arrayListNewsModel.get(position));
        }
    }

    private void bindAdViewHolder(AdsViewHolder adsViewHolder, NewsModel adModel) {
        int adSlot = adModel.getAdSlot();
        String nativeId = adSlot == 1 ? AdPlacement.getNewsNativeId1() : AdPlacement.getNewsNativeId2();
        boolean shouldShow = adSlot == 1 ? AdPlacement.shouldShowNewsNativeAd1() : AdPlacement.shouldShowNewsNativeAd2();

        if (!shouldShow) {
            hideAdRow(adsViewHolder);
            return;
        }

        resetAdRow(adsViewHolder);
        adsViewHolder.rlNativeAdView.setVisibility(VISIBLE);

        boolean alreadyLoaded = adSlot == 1 ? isAd1Loaded : isAd2Loaded;
        if (adsViewHolder.flNativeAd.getChildCount() == 0 && !alreadyLoaded) {
            if (adSlot == 1) {
                isAd1Loaded = true;
            } else {
                isAd2Loaded = true;
            }
            AdPlacement.loadNativeAd(adsViewHolder.itemView.getContext(), nativeId, adsViewHolder.rlNativeAdView, adsViewHolder.slNativeShimmer, adsViewHolder.flNativeAd, "large");
        }
    }

    private void hideAdRow(AdsViewHolder adsViewHolder) {
        adsViewHolder.rlNativeAdView.setVisibility(GONE);
        adsViewHolder.slNativeShimmer.setVisibility(GONE);
        adsViewHolder.flNativeAd.setVisibility(GONE);
        adsViewHolder.itemView.setVisibility(GONE);

        ViewGroup.LayoutParams layoutParams = adsViewHolder.itemView.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = 0;
            adsViewHolder.itemView.setLayoutParams(layoutParams);
        }
    }

    private void resetAdRow(AdsViewHolder adsViewHolder) {
        adsViewHolder.itemView.setVisibility(VISIBLE);
        ViewGroup.LayoutParams layoutParams = adsViewHolder.itemView.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            adsViewHolder.itemView.setLayoutParams(layoutParams);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return arrayListNewsModel.get(position).isAdShow() ? TYPE_AD : 0;
    }

    @Override
    public int getItemCount() {
        return arrayListNewsModel.size();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView ivNewsImage;
        private final AppCompatTextView tvNewsTitle, tvNewsDesc;
        private final LinearLayout llReadMore;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivNewsImage = itemView.findViewById(R.id.ivNewsImage);
            tvNewsTitle = itemView.findViewById(R.id.tvNewsTitle);
            tvNewsDesc = itemView.findViewById(R.id.tvNewsDesc);
            llReadMore = itemView.findViewById(R.id.llReadMore);
        }
    }

    public static class AdsViewHolder extends RecyclerView.ViewHolder {
        private final RelativeLayout rlNativeAdView;
        private final ShimmerFrameLayout slNativeShimmer;
        private final FrameLayout flNativeAd;

        public AdsViewHolder(@NonNull View itemView) {
            super(itemView);
            rlNativeAdView = itemView.findViewById(R.id.rlNativeAdView);
            slNativeShimmer = itemView.findViewById(R.id.slNativeShimmer);
            flNativeAd = itemView.findViewById(R.id.flNativeAd);
        }
    }
}