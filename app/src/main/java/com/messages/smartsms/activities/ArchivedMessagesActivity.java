package com.messages.smartsms.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.messages.smartsms.R;
import com.messages.smartsms.adapters.MessagesAdapter;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.helpers.ArchiveHelper;
import com.messages.smartsms.helpers.BlockHelper;
import com.messages.smartsms.helpers.PinHelper;
import com.messages.smartsms.helpers.RecycleBinHelper;
import com.messages.smartsms.helpers.SmsUnreadHelper;
import com.messages.smartsms.interfaces.OnMessageActionListener;
import com.messages.smartsms.models.MessagesModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArchivedMessagesActivity extends AppCompatActivity {
    private LinearLayout llTitleView, llSelectedView, llNoArchivedMessages;
    private AppCompatImageView ivBack, ivCloseMenu, ivPinToTop, ivUnarchived, ivBlockSpam, ivRecycleBin;
    private AppCompatTextView tvTitle, tvSelectedCount, tvNoArchivedMessages;
    private RecyclerView rvArchivedMessages;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer, slNativeShimmer;
    public LinearLayout llBannerAd, llIndicator;
    private FrameLayout flNativeAd;

    private final ArrayList<MessagesModel> arrayListArchivedMessages = new ArrayList<>();
    private MessagesAdapter messagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived_messages);

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
        llTitleView = findViewById(R.id.llTitleView);
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);

        llSelectedView = findViewById(R.id.llSelectedView);
        ivCloseMenu = findViewById(R.id.ivCloseMenu);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        ivPinToTop = findViewById(R.id.ivPinToTop);
        ivUnarchived = findViewById(R.id.ivUnarchived);
        ivBlockSpam = findViewById(R.id.ivBlockSpam);
        ivRecycleBin = findViewById(R.id.ivRecycleBin);

        rvArchivedMessages = findViewById(R.id.rvArchivedMessages);
        llNoArchivedMessages = findViewById(R.id.llNoArchivedMessages);
        tvNoArchivedMessages = findViewById(R.id.tvNoArchivedMessages);

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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (llSelectedView.getVisibility() == VISIBLE) {
                    updateViewAdapter();
                } else {
                    ArchivedMessagesActivity.this.finish();
                    overridePendingTransition(0, 0);
                }
            }
        });

        ivBack.setOnClickListener(view -> {
            ArchivedMessagesActivity.this.finish();
            overridePendingTransition(0, 0);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                ArchivedMessagesActivity.this.finish();
                overridePendingTransition(0, 0);
            }
        });

        ivCloseMenu.setOnClickListener(view -> updateViewAdapter());

        ivUnarchived.setOnClickListener(view -> {
            messageUnarchived();
            updateViewAdapter();
        });

        ivBlockSpam.setOnClickListener(view -> dialogBlockSpam());

        ivRecycleBin.setOnClickListener(view -> dialogRecycleBin());
    }

    private void getArchivedMessages() {
        arrayListArchivedMessages.clear();

        List<MessagesModel> archivedMessages = ArchiveHelper.getArchivedMessages(this);
        for (MessagesModel messagesModel : archivedMessages) {
            messagesModel.setPinned(PinHelper.isPinned(this, messagesModel.getThreadId()));
        }
        arrayListArchivedMessages.addAll(archivedMessages);

        setArchivedMessagesAdapter();
        sortList();
    }

    private void setArchivedMessagesAdapter() {
        messagesAdapter = new MessagesAdapter(getApplicationContext(), arrayListArchivedMessages);
        rvArchivedMessages.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        rvArchivedMessages.setAdapter(messagesAdapter);

        messagesAdapter.setActionListener(new OnMessageActionListener() {
            @Override
            public void onMessageClick(int position, int selectedCount) {
                updateSelectionView(selectedCount);
            }

            @Override
            public void onMessageLongClick(int position, int selectedCount) {
                updateSelectionView(selectedCount);
            }
        });

        messagesAdapter.setOpenListener(messagesModel -> {
            if (AdPlacement.getMessageListInterstitialAdShow()) {
                AdPlacement.loadInterstitialAdMessageList(ArchivedMessagesActivity.this, AdPlacement.getMessageListInterstitialId(), () -> openChatScreen(messagesModel));
            } else {
                openChatScreen(messagesModel);
            }
        });

        if (!arrayListArchivedMessages.isEmpty()) {
            rvArchivedMessages.setVisibility(VISIBLE);
            llNoArchivedMessages.setVisibility(GONE);
        } else {
            rvArchivedMessages.setVisibility(GONE);
            llNoArchivedMessages.setVisibility(VISIBLE);
        }
    }

    private void sortList() {
        arrayListArchivedMessages.sort((a, b) -> {
            if (a.isPinned() && !b.isPinned()) return -1;
            if (!a.isPinned() && b.isPinned()) return 1;
            return Long.compare(b.getDate(), a.getDate());
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void openChatScreen(MessagesModel model) {
        if (model == null) {
            return;
        }
        SmsUnreadHelper.markThreadAsRead(this, model.getThreadId(), model.getAddress());
        model.setUnreadCount(0);
        if (messagesAdapter != null) {
            messagesAdapter.notifyDataSetChanged();
        }

        Intent intent = new Intent(this, MessagesContentActivity.class);
        intent.putExtra(MessagesContentActivity.EXTRA_THREAD_ID, model.getThreadId());
        intent.putExtra(MessagesContentActivity.EXTRA_NAME, model.getName());
        intent.putExtra(MessagesContentActivity.EXTRA_ADDRESS, model.getAddress());
        intent.putExtra(MessagesContentActivity.EXTRA_PHOTO_URI, model.getContactPhotoUri());
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @SuppressLint("SetTextI18n")
    private void updateSelectionView(int selectedCount) {
        if (selectedCount == 0) {
            llTitleView.setVisibility(VISIBLE);
            llSelectedView.setVisibility(GONE);
            messagesAdapter.setSelectionMode(false);
            return;
        }

        llTitleView.setVisibility(GONE);
        llSelectedView.setVisibility(VISIBLE);
        messagesAdapter.setSelectionMode(true);

        tvSelectedCount.setText(selectedCount + " " + getResources().getString(R.string.selected));

        for (MessagesModel messagesModel : arrayListArchivedMessages) {
            if (messagesModel.isSelected()) {
                if (messagesModel.isPinned()) {
                    ivPinToTop.setImageResource(R.drawable.ic_unpin_menu);
                    ivPinToTop.setOnClickListener(view -> messageUnpin());
                } else {
                    ivPinToTop.setImageResource(R.drawable.ic_pin_menu);
                    ivPinToTop.setOnClickListener(view -> messagePin());
                }
            }
        }
    }

    private void messagePin() {
        List<MessagesModel> selected = new ArrayList<>(messagesAdapter.getSelectedMessages());
        if (selected.isEmpty()) {
            return;
        }

        for (MessagesModel messagesModel : selected) {
            PinHelper.pin(this, messagesModel.getThreadId());
            messagesModel.setPinned(true);
        }

        updateViewAdapter();
        sortList();
        Toast.makeText(this, R.string.toast_pinned, Toast.LENGTH_SHORT).show();
    }

    private void messageUnpin() {
        List<MessagesModel> selected = new ArrayList<>(messagesAdapter.getSelectedMessages());
        if (selected.isEmpty()) {
            return;
        }

        for (MessagesModel messagesModel : selected) {
            if (messagesModel.isPinned()) {
                PinHelper.unpin(this, messagesModel.getThreadId());
                messagesModel.setPinned(false);
            }
        }

        updateViewAdapter();
        sortList();
        Toast.makeText(this, R.string.toast_unpinned, Toast.LENGTH_SHORT).show();
    }

    private void messageUnarchived() {
        List<MessagesModel> selected = new ArrayList<>(messagesAdapter.getSelectedMessages());
        if (selected.isEmpty()) {
            return;
        }

        for (MessagesModel messagesModel : selected) {
            ArchiveHelper.unarchive(this, messagesModel);
            removeFromList(messagesModel.getThreadId());
        }

        updateViewAdapter();
        Toast.makeText(this, R.string.toast_unarchived, Toast.LENGTH_SHORT).show();
    }

    private void dialogBlockSpam() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
        bottomSheetDialog.setContentView(R.layout.dialog_blockspam);

        AppCompatTextView tvCancel = bottomSheetDialog.findViewById(R.id.tvCancel);
        AppCompatTextView tvBlock = bottomSheetDialog.findViewById(R.id.tvBlock);

        tvCancel.setOnClickListener(view -> bottomSheetDialog.dismiss());

        tvBlock.setOnClickListener(view -> {
            messageBlockSpam();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void messageBlockSpam() {
        List<MessagesModel> selected = new ArrayList<>(messagesAdapter.getSelectedMessages());
        if (selected.isEmpty()) {
            return;
        }

        for (MessagesModel messagesModel : selected) {
            ArchiveHelper.unarchive(this, messagesModel);
            removeFromList(messagesModel.getThreadId());
            BlockHelper.block(this, messagesModel);
        }

        updateViewAdapter();
        Toast.makeText(this, R.string.toast_blocked, Toast.LENGTH_SHORT).show();
    }

    private void dialogRecycleBin() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
        bottomSheetDialog.setContentView(R.layout.dialog_recycle_bin);

        AppCompatTextView tvCancel = bottomSheetDialog.findViewById(R.id.tvCancel);
        AppCompatTextView tvMovetoBin = bottomSheetDialog.findViewById(R.id.tvMovetoBin);

        tvCancel.setOnClickListener(view -> bottomSheetDialog.dismiss());

        tvMovetoBin.setOnClickListener(view -> {
            messageRecycleBin();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void messageRecycleBin() {
        List<MessagesModel> selected = new ArrayList<>(messagesAdapter.getSelectedMessages());
        if (selected.isEmpty()) {
            return;
        }

        for (MessagesModel messagesModel : selected) {
            ArchiveHelper.unarchive(this, messagesModel);
            removeFromList(messagesModel.getThreadId());
            RecycleBinHelper.moveToBin(this, messagesModel);
        }

        updateViewAdapter();
        Toast.makeText(this, R.string.toast_moved_to_recycle_bin, Toast.LENGTH_SHORT).show();
    }

    private void removeFromList(String threadId) {
        for (int i = arrayListArchivedMessages.size() - 1; i >= 0; i--) {
            if (arrayListArchivedMessages.get(i).getThreadId().equals(threadId)) {
                arrayListArchivedMessages.remove(i);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateViewAdapter() {
        llTitleView.setVisibility(View.VISIBLE);
        llSelectedView.setVisibility(View.GONE);

        messagesAdapter.setSelectionMode(false);
        messagesAdapter.notifyDataSetChanged();

        if (!arrayListArchivedMessages.isEmpty()) {
            rvArchivedMessages.setVisibility(VISIBLE);
            llNoArchivedMessages.setVisibility(GONE);
        } else {
            rvArchivedMessages.setVisibility(GONE);
            llNoArchivedMessages.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTitle != null) {
            tvTitle.setText(getString(R.string.archived));
            tvNoArchivedMessages.setText(getString(R.string.no_archived_messages));
        }

        getArchivedMessages();
    }
}