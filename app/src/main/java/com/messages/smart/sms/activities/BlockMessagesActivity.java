package com.messages.smart.sms.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.messages.smart.sms.R;
import com.messages.smart.sms.adapters.BlockMessagesAdapter;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.helpers.BlockHelper;
import com.messages.smart.sms.helpers.SmsUnreadHelper;
import com.messages.smart.sms.interfaces.OnUnblockClickListener;
import com.messages.smart.sms.models.MessagesModel;

import java.util.ArrayList;
import java.util.List;

public class BlockMessagesActivity extends AppCompatActivity implements OnUnblockClickListener {
    private AppCompatImageView ivBack;
    private AppCompatTextView tvTitle, tvNoBlockMessages;
    private RecyclerView rvBlockMessages;
    private LinearLayout llNoBlockMessages;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer, slNativeShimmer;
    public LinearLayout llBannerAd, llIndicator;
    private FrameLayout flNativeAd;

    private final ArrayList<MessagesModel> arrayListBlockMessages = new ArrayList<>();
    private BlockMessagesAdapter blockMessagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_messages);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        initViews();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        rvBlockMessages = findViewById(R.id.rvBlockMessages);
        llNoBlockMessages = findViewById(R.id.llNoBlockMessages);
        tvNoBlockMessages = findViewById(R.id.tvNoBlockMessages);

        rlAdView = findViewById(R.id.rlAdView);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);

        clickEvents();
    }

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
            BlockMessagesActivity.this.finish();
            overridePendingTransition(0, 0);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                BlockMessagesActivity.this.finish();
                overridePendingTransition(0, 0);
            }
        });
    }

    private void getBlockMessages() {
        arrayListBlockMessages.clear();

        List<MessagesModel> blockMessages = BlockHelper.getBlockedMessages(this);
        arrayListBlockMessages.addAll(blockMessages);

        setBlockMessagesAdapter();
        sortList();
    }

    private void setBlockMessagesAdapter() {
        blockMessagesAdapter = new BlockMessagesAdapter(this, arrayListBlockMessages, this);
        rvBlockMessages.setLayoutManager(new LinearLayoutManager(this));
        rvBlockMessages.setAdapter(blockMessagesAdapter);

        blockMessagesAdapter.setOpenListener(messagesModel -> {
            if (AdPlacement.getMessageListInterstitialAdShow()) {
                AdPlacement.loadInterstitialAdMessageList(BlockMessagesActivity.this, AdPlacement.getMessageListInterstitialId(), () -> openChatScreen(messagesModel));
            } else {
                openChatScreen(messagesModel);
            }
        });

        if (!arrayListBlockMessages.isEmpty()) {
            rvBlockMessages.setVisibility(VISIBLE);
            llNoBlockMessages.setVisibility(GONE);
        } else {
            rvBlockMessages.setVisibility(GONE);
            llNoBlockMessages.setVisibility(VISIBLE);
        }
    }

    private void sortList() {
        arrayListBlockMessages.sort((a, b) -> Long.compare(b.getDate(), a.getDate()));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void openChatScreen(MessagesModel model) {
        if (model == null) {
            return;
        }
        SmsUnreadHelper.markThreadAsRead(this, model.getThreadId(), model.getAddress());
        model.setUnreadCount(0);
        if (blockMessagesAdapter != null) {
            blockMessagesAdapter.notifyDataSetChanged();
        }

        Intent intent = new Intent(this, MessagesContentActivity.class);
        intent.putExtra(MessagesContentActivity.EXTRA_THREAD_ID, model.getThreadId());
        intent.putExtra(MessagesContentActivity.EXTRA_NAME, model.getName());
        intent.putExtra(MessagesContentActivity.EXTRA_ADDRESS, model.getAddress());
        intent.putExtra(MessagesContentActivity.EXTRA_PHOTO_URI, model.getContactPhotoUri());
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void removeFromList(String threadId) {
        for (int i = arrayListBlockMessages.size() - 1; i >= 0; i--) {
            if (arrayListBlockMessages.get(i).getThreadId().equals(threadId)) {
                arrayListBlockMessages.remove(i);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateViewAdapter() {
        blockMessagesAdapter.notifyDataSetChanged();

        if (!arrayListBlockMessages.isEmpty()) {
            rvBlockMessages.setVisibility(VISIBLE);
            llNoBlockMessages.setVisibility(GONE);
        } else {
            rvBlockMessages.setVisibility(GONE);
            llNoBlockMessages.setVisibility(VISIBLE);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onUnblockClick(ArrayList<MessagesModel> arrayListMessagesModel, int position) {
        BlockHelper.unblock(this, arrayListMessagesModel.get(position));
        removeFromList(arrayListMessagesModel.get(position).getThreadId());
        updateViewAdapter();
        Toast.makeText(this, R.string.toast_unblocked, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTitle != null) {
            tvTitle.setText(getString(R.string.block));
            tvNoBlockMessages.setText(getString(R.string.no_block_messages));
        }

        getBlockMessages();
    }
}