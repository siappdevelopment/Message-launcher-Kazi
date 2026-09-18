package com.messages.smart.sms.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.role.RoleManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.messages.smart.sms.R;
import com.messages.smart.sms.adapters.MessagesContentAdapter;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.helpers.ArchiveHelper;
import com.messages.smart.sms.helpers.BlockHelper;
import com.messages.smart.sms.helpers.ContactUtils;
import com.messages.smart.sms.helpers.ConversationDeleteHelper;
import com.messages.smart.sms.helpers.MessageListSyncHelper;
import com.messages.smart.sms.helpers.RecycleBinHelper;
import com.messages.smart.sms.helpers.SmsSendHelper;
import com.messages.smart.sms.helpers.SmsUnreadHelper;
import com.messages.smart.sms.interfaces.OnMessageContentItemLongClickListener;
import com.messages.smart.sms.models.ChatMessageItem;
import com.messages.smart.sms.models.MessagesModel;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class MessagesContentActivity extends AppCompatActivity implements OnMessageContentItemLongClickListener {
    private RelativeLayout rlBack, rlCall, rlMenu, rlCloseMenu, rlBlockedView, rlUnblock, rlPlus, rlSend, rlBannerAdView;
    private AppCompatImageView ivImage, ivSelectionDelete, ivSelectionCopy;
    private AppCompatTextView tvName, tvTitle, tvSelectedCount, tvBlockText, tvBottomText;
    private RecyclerView rvMessagesContent;
    private LinearLayout llDefaultActionBar, llSelectionActionBar, llNoSend, llMessageSend, llBannerAd;
    private AppCompatEditText etMessage;
    private ShimmerFrameLayout slBannerShimmer;

    private MessagesContentAdapter messagesContentAdapter;
    private ContentObserver smsObserver;
    private ActivityResultLauncher<Intent> contactPickLauncher;
    private ActivityResultLauncher<Intent> defaultSmsLauncher;
    private boolean pendingDeleteConversation = false;
    private boolean pendingSendMessage = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final int PERMISSION_SEND_SMS = 2001;

    public static final String EXTRA_THREAD_ID = "thread_id";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_PHOTO_URI = "photo_uri";

    public static String currentOpenNumber = "";

    private String threadId;
    private String contactName;
    private String address;
    private String photoUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactPickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                return;
            }
            handleContactPickResult(result.getData());
            overridePendingTransition(0, 0);
        });

        defaultSmsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (pendingDeleteConversation && isDefaultSmsApp()) {
                pendingDeleteConversation = false;
                dialogDelete();
            } else {
                pendingDeleteConversation = false;
            }
            if (pendingSendMessage && isDefaultSmsApp()) {
                pendingSendMessage = false;
                sendMessage();
            } else {
                pendingSendMessage = false;
            }
        });

        setContentView(R.layout.activity_messages_content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        readIntentExtras();
        initViews();
        setupHeader();
        setupChatList();
        setupActions();
        updateReplyUi();

        loadMessages();
        markThreadAsRead();
        registerSmsObserver();
    }

    private void readIntentExtras() {
        threadId = getIntent().getStringExtra(EXTRA_THREAD_ID);
        contactName = getIntent().getStringExtra(EXTRA_NAME);
        address = getIntent().getStringExtra(EXTRA_ADDRESS);
        photoUri = getIntent().getStringExtra(EXTRA_PHOTO_URI);

        if (TextUtils.isEmpty(address)) {
            address = getIntent().getStringExtra("number");
        }
        if (TextUtils.isEmpty(contactName) && !TextUtils.isEmpty(address)) {
            contactName = address;
        }
        if (TextUtils.isEmpty(threadId) && !TextUtils.isEmpty(address)) {
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                    threadId = String.valueOf(Telephony.Threads.getOrCreateThreadId(this, address));
                }
            } catch (SecurityException | IllegalArgumentException exception) {
                exception.printStackTrace();
                threadId = "";
            }
        }
    }

    private void initViews() {
        llDefaultActionBar = findViewById(R.id.llDefaultActionBar);
        llSelectionActionBar = findViewById(R.id.llSelectionActionBar);

        rlBack = findViewById(R.id.rlBack);
        ivImage = findViewById(R.id.ivImage);
        tvName = findViewById(R.id.tvName);
        tvTitle = findViewById(R.id.tvTitle);
        rlCall = findViewById(R.id.rlCall);
        rlMenu = findViewById(R.id.rlMenu);

        rlCloseMenu = findViewById(R.id.rlCloseMenu);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        ivSelectionDelete = findViewById(R.id.ivSelectionDelete);
        ivSelectionCopy = findViewById(R.id.ivSelectionCopy);

        rlBlockedView = findViewById(R.id.rlBlockedView);
        tvBlockText = findViewById(R.id.tvBlockText);
        rlUnblock = findViewById(R.id.rlUnblock);
        rvMessagesContent = findViewById(R.id.rvMessagesContent);

        llNoSend = findViewById(R.id.llNoSend);
        tvBottomText = findViewById(R.id.tvBottomText);
        llMessageSend = findViewById(R.id.llMessageSend);
        rlPlus = findViewById(R.id.rlPlus);
        etMessage = findViewById(R.id.etMessage);
        rlSend = findViewById(R.id.rlSend);

        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
    }

    private void setupHeader() {
        refreshContactHeader();

        String draft = Utils.getDraft(this, address);
        if (!draft.isEmpty()) {
            etMessage.setText(draft);
            etMessage.setSelection(draft.length());
        }
    }

    private void refreshContactHeader() {
        tvTitle.setText(contactName);

        if (photoUri != null && !photoUri.isEmpty()) {
            tvName.setVisibility(GONE);
            ivImage.setVisibility(VISIBLE);
            ivImage.setPadding(0, 0, 0, 0);
            Glide.with(this).load(Uri.parse(photoUri)).transform(new CircleCrop()).placeholder(R.drawable.ic_user).into(ivImage);
            return;
        }

        Bitmap photo = getContactPhoto(this, address);
        if (photo != null) {
            tvName.setVisibility(GONE);
            ivImage.setVisibility(VISIBLE);
            ivImage.setImageBitmap(photo);
            ivImage.setPadding(0, 0, 0, 0);
        } else if (contactName != null && !contactName.isEmpty() && !contactName.equals(address)) {
            ivImage.setVisibility(GONE);
            tvName.setVisibility(VISIBLE);
            tvName.setText(contactName.substring(0, 1).toUpperCase(Locale.ENGLISH));
        } else {
            tvName.setVisibility(GONE);
            ivImage.setVisibility(VISIBLE);
            ivImage.setImageResource(R.drawable.ic_user);
            ivImage.setPadding(22, 22, 22, 22);
        }
    }

    private void refreshContactInfo() {
        if (TextUtils.isEmpty(address)) {
            return;
        }

        ContactUtils.ContactDetails details = ContactUtils.lookup(this, address);
        String resolvedName = details.getName();
        String resolvedPhotoUri = details.getPhotoUri();

        boolean changed = false;
        if (!TextUtils.isEmpty(resolvedName) && !TextUtils.equals(resolvedName, contactName)) {
            contactName = resolvedName;
            changed = true;
        }
        if (!TextUtils.equals(photoUri, resolvedPhotoUri)) {
            photoUri = resolvedPhotoUri;
            changed = true;
        }

        if (!changed) {
            return;
        }

        refreshContactHeader();
        if (!TextUtils.isEmpty(threadId)) {
            MessageListSyncHelper.notifyContactUpdated(threadId, contactName, address, photoUri);
        }
    }

    private void setupChatList() {
        messagesContentAdapter = new MessagesContentAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessagesContent.setLayoutManager(layoutManager);
        rvMessagesContent.setAdapter(messagesContentAdapter);
    }

    private void setupActions() {
        rlBack.setOnClickListener(v -> {
            if (AdPlacement.getMessageContentBackInterstitialAdShow()) {
                AdPlacement.loadInterstitialAdMessageContentBack(MessagesContentActivity.this, AdPlacement.getMessageContentBackInterstitialId(), this::finishInstant);
            } else {
                finishInstant();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (AdPlacement.getMessageContentBackInterstitialAdShow()) {
                    AdPlacement.loadInterstitialAdMessageContentBack(MessagesContentActivity.this, AdPlacement.getMessageContentBackInterstitialId(), () -> finishInstant());
                } else {
                    finishInstant();
                }
            }
        });

        rlCall.setOnClickListener(v -> {
            if (TextUtils.isEmpty(address)) {
                return;
            }
            String dialNumber = get10DigitNumber(address);
            if (TextUtils.isEmpty(dialNumber)) {
                dialNumber = address;
            }
            Utils.openDialer(this, dialNumber);
        });

        rlMenu.setOnClickListener(v -> setupMenuDropdownPopup());

        rlCloseMenu.setOnClickListener(v -> {
            if (messagesContentAdapter != null) {
                messagesContentAdapter.clearSelection();
            }
        });

        ivSelectionDelete.setOnClickListener(v -> {
            if (messagesContentAdapter == null) {
                return;
            }
            if (isDefaultSmsApp()) {
                List<Long> selectedIds = messagesContentAdapter.getSelectedMessageIds();
                for (Long id : selectedIds) {
                    try {
                        getContentResolver().delete(Uri.parse("content://sms/" + id), null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                messagesContentAdapter.deleteSelectedMessages();
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                MessageListSyncHelper.notifySyncRequired();
                if (!messagesContentAdapter.hasConversationMessages()) {
                    finishInstant();
                } else {
                    loadMessages();
                }
            } else {
                requestDefaultSmsApp();
            }
        });

        ivSelectionCopy.setOnClickListener(v -> {
            if (messagesContentAdapter == null) {
                return;
            }
            List<String> selectedBodies = messagesContentAdapter.getSelectedMessageBodies();
            if (selectedBodies.isEmpty()) {
                return;
            }
            StringBuilder copiedText = new StringBuilder();
            for (String body : selectedBodies) {
                if (copiedText.length() > 0) {
                    copiedText.append("\n");
                }
                copiedText.append(body);
            }
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", copiedText.toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            messagesContentAdapter.clearSelection();
        });

        rlPlus.setOnClickListener(v -> launchContactPicker());

        rlSend.setOnClickListener(v -> sendMessage());

        rlUnblock.setOnClickListener(v -> {
            if (TextUtils.isEmpty(threadId)) {
                return;
            }
            MessagesModel model = buildThreadModel();
            BlockHelper.unblock(getApplicationContext(), model);
            Toast.makeText(this, R.string.toast_unblocked, Toast.LENGTH_SHORT).show();
            updateReplyUi();
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateReplyUi() {
        boolean blocked = isBlocked();
        boolean replyable = isReplyable(address) && !blocked;

        rlBlockedView.setVisibility(blocked ? VISIBLE : GONE);
        if (blocked) {
            tvBlockText.setText(getString(R.string.to_move_this_conversation_out_of_blocking_and_get_messages_again_unblock) + " '" + contactName + "'");
            tvBottomText.setText(getString(R.string.this_conversation_blocked_by_you));
        }

        rlCall.setVisibility(replyable ? VISIBLE : GONE);
        rlPlus.setVisibility(replyable ? VISIBLE : GONE);
        updateMenuVisibility();

        if (replyable) {
            llNoSend.setVisibility(GONE);
            llMessageSend.setVisibility(VISIBLE);
            rlBannerAdView.setVisibility(GONE);
        } else {
            llNoSend.setVisibility(VISIBLE);
            llMessageSend.setVisibility(GONE);
            if (!blocked) {
                tvBottomText.setText(getString(R.string.sender_does_not_support_replies));
            }

            if (AdPlacement.getMessageContentBannerAdShow()) {
                rlBannerAdView.setVisibility(VISIBLE);
                AdPlacement.loadBannerAd(this, AdPlacement.getMessageContentBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd);
            } else {
                rlBannerAdView.setVisibility(GONE);
            }
        }
    }

    private void loadMessages() {
        runOnBackground(() -> {
            List<ChatMessageItem> items = fetchThreadMessages();
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed() || messagesContentAdapter == null) {
                    return;
                }
                messagesContentAdapter.setItems(items);
                scrollToBottom();
                updateMenuVisibility();
            });
        });
    }

    private List<ChatMessageItem> fetchThreadMessages() {
        List<ChatMessageItem> items = new ArrayList<>();
        if (TextUtils.isEmpty(threadId) || !hasReadSmsPermission()) {
            return items;
        }

        try (Cursor cursor = getContentResolver().query(Uri.parse("content://sms"), null, "thread_id = ?", new String[]{threadId}, "date ASC")) {
            if (cursor == null) {
                return items;
            }
            String lastDateLabel = null;
            while (cursor.moveToNext()) {
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                long messageId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));

                String dateLabel = formatDateLabel(date);
                if (!dateLabel.equals(lastDateLabel)) {
                    items.add(ChatMessageItem.dateHeader(dateLabel));
                    lastDateLabel = dateLabel;
                }

                if (isSentMessage(type)) {
                    items.add(ChatMessageItem.sent(body, date, messageId));
                } else {
                    items.add(ChatMessageItem.received(body, date, messageId));
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return items;
    }

    private String formatDateLabel(long dateMillis) {
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(dateMillis);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        if (isSameDay(msgCal, today)) {
            return getString(R.string.today);
        }
        if (isSameDay(msgCal, yesterday)) {
            return getString(R.string.yesterday);
        }
        return new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(new Date(dateMillis));
    }

    private boolean isSameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(address)) {
            return;
        }

        if (!isDefaultSmsApp()) {
            pendingSendMessage = true;
            requestDefaultSmsApp();
            return;
        }

        if (!hasSendSmsPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS);
            Toast.makeText(this, R.string.send_sms_permission_required, Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();
        threadId = SmsSendHelper.ensureThreadId(this, address, threadId);
        String dateLabel = formatDateLabel(now);

        messagesContentAdapter.appendSentMessage(text, now, dateLabel);
        scrollToBottom();
        etMessage.setText("");
        Utils.setDraft(this, address, "");

        MessageListSyncHelper.notifyOutboundMessage(threadId, contactName, address, text, now, photoUri);

        final String messageText = text;
        final String resolvedThreadId = threadId;
        final long sentAt = now;
        runOnBackground(() -> {
            try {
                SmsSendHelper.sendAndStore(MessagesContentActivity.this, address, messageText, resolvedThreadId, sentAt);
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    MessageListSyncHelper.notifySyncRequired();
                    loadMessages();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(MessagesContentActivity.this, R.string.send_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private boolean hasSendSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_SEND_SMS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendMessage();
        }
    }

    private boolean isSentMessage(int type) {
        return type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX || type == Telephony.Sms.MESSAGE_TYPE_QUEUED || type == Telephony.Sms.MESSAGE_TYPE_FAILED;
    }

    private boolean isReplyable(String phoneAddress) {
        if (phoneAddress == null) {
            return false;
        }
        String cleanNumber = phoneAddress.trim().replaceAll("[^\\d+]", "");
        if (cleanNumber.matches(".*[a-zA-Z]+.*")) {
            return false;
        }
        if (cleanNumber.length() < 7) {
            return false;
        }
        return Patterns.PHONE.matcher(cleanNumber).matches();
    }

    private boolean isBlocked() {
        return !TextUtils.isEmpty(threadId) && BlockHelper.isBlocked(this, threadId);
    }

    private void updateMenuVisibility() {
        if (rlMenu == null || messagesContentAdapter == null) {
            return;
        }
        boolean showMenu = !isBlocked() && messagesContentAdapter.hasConversationMessages();
        rlMenu.setVisibility(showMenu ? VISIBLE : GONE);
    }

    private boolean shouldShowAddToContact() {
        return isReplyable(address) && !ContactUtils.isSavedInContacts(this, address);
    }

    private MessagesModel buildThreadModel() {
        String body = "";
        long date = System.currentTimeMillis();
        int type = Telephony.Sms.MESSAGE_TYPE_INBOX;
        int read = 1;
        if (!TextUtils.isEmpty(threadId) && hasReadSmsPermission()) {
            try (Cursor cursor = getContentResolver().query(Uri.parse("content://sms"), null, "thread_id = ?", new String[]{threadId}, "date DESC LIMIT 1")) {
                if (cursor != null && cursor.moveToFirst()) {
                    body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                    type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                    read = cursor.getInt(cursor.getColumnIndexOrThrow("read"));
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        return new MessagesModel(threadId, contactName, address, body, date, type, read, photoUri, SmsUnreadHelper.getThreadUnreadCount(this, threadId));
    }

    private String getMenuType() {
        if (RecycleBinHelper.isInRecycleBin(this, threadId)) {
            return "RecycleBinMessagesAdapter";
        }
        if (ArchiveHelper.isArchived(this, threadId)) {
            return "ArchivedMessagesAdapter";
        }
        if (BlockHelper.isBlocked(this, threadId)) {
            return "BlockMessagesAdapter";
        }
        return "MessagesAdapter";
    }

    @SuppressLint({"InflateParams", "NotifyDataSetChanged"})
    private void setupMenuDropdownPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.menu_content_dropdown_popup, null);

        PopupWindow popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setElevation(10f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        int y = (int) (10 * getResources().getDisplayMetrics().density);
        popupWindow.showAsDropDown(rlMenu, 0, y);

        AppCompatImageView ivMenuBg = popupView.findViewById(R.id.ivMenuBg);
        LinearLayout llAddtoContact = popupView.findViewById(R.id.llAddtoContact);
        AppCompatImageView ivDividerLineAddtoContact = popupView.findViewById(R.id.ivDividerLineAddtoContact);
        LinearLayout llArchivedUnarchived = popupView.findViewById(R.id.llArchivedUnarchived);
        AppCompatImageView ivArchivedUnarchived = popupView.findViewById(R.id.ivArchivedUnarchived);
        AppCompatTextView tvArchivedUnarchived = popupView.findViewById(R.id.tvArchivedUnarchived);
        AppCompatImageView ivDividerLineArchivedUnarchived = popupView.findViewById(R.id.ivDividerLineArchivedUnarchived);
        LinearLayout llBlockUnblock = popupView.findViewById(R.id.llBlockUnblock);
        AppCompatTextView tvBlockUnblock = popupView.findViewById(R.id.tvBlockUnblock);
        AppCompatImageView ivDividerLineBlockUnblock = popupView.findViewById(R.id.ivDividerLineBlockUnblock);
        LinearLayout llRecycleBinRestoreBin = popupView.findViewById(R.id.llRecycleBinRestoreBin);
        AppCompatImageView ivRecycleBinRestoreBin = popupView.findViewById(R.id.ivRecycleBinRestoreBin);
        AppCompatTextView tvRecycleBinRestoreBin = popupView.findViewById(R.id.tvRecycleBinRestoreBin);
        AppCompatImageView ivDividerLineRecycleBinRestoreBin = popupView.findViewById(R.id.ivDividerLineRecycleBinRestoreBin);
        LinearLayout llDeleteConversation = popupView.findViewById(R.id.llDeleteConversation);

        String menuType = getMenuType();
        boolean showAddToContact = shouldShowAddToContact();

        if ("MessagesAdapter".equals(menuType) || "ArchivedMessagesAdapter".equals(menuType)) {
            ivMenuBg.setImageResource(R.drawable.img_popup_bg_2);
            llAddtoContact.setVisibility(showAddToContact ? VISIBLE : GONE);
            ivDividerLineAddtoContact.setVisibility(showAddToContact ? VISIBLE : GONE);
            llArchivedUnarchived.setVisibility(VISIBLE);
            ivDividerLineArchivedUnarchived.setVisibility(VISIBLE);
            llBlockUnblock.setVisibility(VISIBLE);
            ivDividerLineBlockUnblock.setVisibility(VISIBLE);
            llRecycleBinRestoreBin.setVisibility(VISIBLE);
            ivDividerLineRecycleBinRestoreBin.setVisibility(VISIBLE);
            llDeleteConversation.setVisibility(VISIBLE);
        } else if ("RecycleBinMessagesAdapter".equals(menuType)) {
            ivMenuBg.setImageResource(R.drawable.img_popup_bg_3);
            llAddtoContact.setVisibility(GONE);
            ivDividerLineAddtoContact.setVisibility(GONE);
            llArchivedUnarchived.setVisibility(GONE);
            ivDividerLineArchivedUnarchived.setVisibility(GONE);
            llBlockUnblock.setVisibility(GONE);
            ivDividerLineBlockUnblock.setVisibility(GONE);
            llRecycleBinRestoreBin.setVisibility(VISIBLE);
            ivDividerLineRecycleBinRestoreBin.setVisibility(VISIBLE);
            llDeleteConversation.setVisibility(VISIBLE);
        } else {
            llAddtoContact.setVisibility(GONE);
            ivDividerLineAddtoContact.setVisibility(GONE);
            llArchivedUnarchived.setVisibility(GONE);
            ivDividerLineArchivedUnarchived.setVisibility(GONE);
            llBlockUnblock.setVisibility(GONE);
            ivDividerLineBlockUnblock.setVisibility(GONE);
            llRecycleBinRestoreBin.setVisibility(GONE);
            ivDividerLineRecycleBinRestoreBin.setVisibility(GONE);
            llDeleteConversation.setVisibility(GONE);
        }

        boolean isArchived = ArchiveHelper.isArchived(this, threadId);
        boolean isBlock = BlockHelper.isBlocked(this, threadId);
        boolean isRecycleBin = RecycleBinHelper.isInRecycleBin(this, threadId);

        if (isArchived) {
            ivArchivedUnarchived.setImageResource(R.drawable.ic_unarchived_menu);
            tvArchivedUnarchived.setText(getString(R.string.unarchived));
        } else {
            ivArchivedUnarchived.setImageResource(R.drawable.ic_archived_menu);
            tvArchivedUnarchived.setText(getString(R.string.archived));
        }

        tvBlockUnblock.setText(isBlock ? getString(R.string.unblock) : getString(R.string.block));

        if (isRecycleBin) {
            ivRecycleBinRestoreBin.setImageResource(R.drawable.ic_restore_bin_menu);
            tvRecycleBinRestoreBin.setText(getString(R.string.restore_from_bin));
        } else {
            ivRecycleBinRestoreBin.setImageResource(R.drawable.ic_recycle_bin_menu);
            tvRecycleBinRestoreBin.setText(getString(R.string.recycle_bin));
        }

        llAddtoContact.setOnClickListener(view -> {
            popupWindow.dismiss();
            Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        llArchivedUnarchived.setOnClickListener(view -> {
            popupWindow.dismiss();
            MessagesModel model = buildThreadModel();
            if (isRecycleBin) {
                RecycleBinHelper.restoreFromBin(this, model);
                Toast.makeText(this, R.string.toast_restored_from_bin, Toast.LENGTH_SHORT).show();
            } else if (isArchived) {
                ArchiveHelper.unarchive(this, model);
                Toast.makeText(this, R.string.toast_unarchived, Toast.LENGTH_SHORT).show();
            } else {
                if (isBlock) {
                    BlockHelper.unblock(this, model);
                }
                ArchiveHelper.archive(this, model);
                Toast.makeText(this, R.string.toast_archived, Toast.LENGTH_SHORT).show();
            }
            updateReplyUi();
        });

        llBlockUnblock.setOnClickListener(view -> {
            popupWindow.dismiss();
            if (isBlock) {
                BlockHelper.unblock(this, buildThreadModel());
                Toast.makeText(this, R.string.toast_unblocked, Toast.LENGTH_SHORT).show();
                updateReplyUi();
            } else {
                dialogBlock();
            }
        });

        llRecycleBinRestoreBin.setOnClickListener(view -> {
            popupWindow.dismiss();
            MessagesModel model = buildThreadModel();
            if (isRecycleBin) {
                RecycleBinHelper.restoreFromBin(this, model);
                Toast.makeText(this, R.string.toast_restored_from_bin, Toast.LENGTH_SHORT).show();
                updateReplyUi();
            } else {
                dialogRecycleBin();
            }
        });

        llDeleteConversation.setOnClickListener(view -> {
            popupWindow.dismiss();
            if (isDefaultSmsApp()) {
                dialogDelete();
            } else {
                pendingDeleteConversation = true;
                requestDefaultSmsApp();
            }
        });
    }

    private void dialogBlock() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
        bottomSheetDialog.setContentView(R.layout.dialog_blockspam);

        AppCompatTextView tvCancel = bottomSheetDialog.findViewById(R.id.tvCancel);
        AppCompatTextView tvBlock = bottomSheetDialog.findViewById(R.id.tvBlock);

        tvCancel.setOnClickListener(view -> bottomSheetDialog.dismiss());

        tvBlock.setOnClickListener(view -> {
            MessagesModel model = buildThreadModel();
            if (ArchiveHelper.isArchived(this, threadId)) {
                ArchiveHelper.removeFromArchive(this, threadId);
            }
            if (RecycleBinHelper.isInRecycleBin(this, threadId)) {
                RecycleBinHelper.removeFromRecycleBin(this, threadId);
            }
            BlockHelper.block(this, model);
            bottomSheetDialog.dismiss();
            Toast.makeText(this, R.string.toast_blocked, Toast.LENGTH_SHORT).show();
            updateReplyUi();
        });

        bottomSheetDialog.show();
    }

    private void dialogRecycleBin() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
        bottomSheetDialog.setContentView(R.layout.dialog_recycle_bin);

        AppCompatTextView tvCancel = bottomSheetDialog.findViewById(R.id.tvCancel);
        AppCompatTextView tvMovetoBin = bottomSheetDialog.findViewById(R.id.tvMovetoBin);

        tvCancel.setOnClickListener(view -> bottomSheetDialog.dismiss());

        tvMovetoBin.setOnClickListener(view -> {
            MessagesModel model = buildThreadModel();
            if (ArchiveHelper.isArchived(this, threadId)) {
                ArchiveHelper.removeFromArchive(this, threadId);
            }
            if (BlockHelper.isBlocked(this, threadId)) {
                BlockHelper.unblock(this, model);
            }
            RecycleBinHelper.moveToBin(this, model);
            bottomSheetDialog.dismiss();
            Toast.makeText(this, R.string.toast_moved_to_recycle_bin, Toast.LENGTH_SHORT).show();
            updateReplyUi();
        });

        bottomSheetDialog.show();
    }

    private void dialogDelete() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
        bottomSheetDialog.setContentView(R.layout.dialog_delete);

        AppCompatTextView tvCancel = bottomSheetDialog.findViewById(R.id.tvCancel);
        AppCompatTextView tvDelete = bottomSheetDialog.findViewById(R.id.tvDelete);

        tvCancel.setOnClickListener(view -> bottomSheetDialog.dismiss());

        tvDelete.setOnClickListener(view -> {
            ConversationDeleteHelper.deleteConversation(this, threadId, address);
            bottomSheetDialog.dismiss();
            Toast.makeText(this, R.string.toast_conversation_deleted, Toast.LENGTH_SHORT).show();
            finishInstant();
        });

        bottomSheetDialog.show();
    }

    private void launchContactPicker() {
        Intent[] candidates = new Intent[]{new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI), new Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE), new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), new Intent(Intent.ACTION_GET_CONTENT).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)};
        for (Intent intent : candidates) {
            try {
                contactPickLauncher.launch(intent);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(this, R.string.contacts, Toast.LENGTH_SHORT).show();
    }

    private void handleContactPickResult(Intent data) {
        Uri contactUri = data.getData();
        if (contactUri == null) {
            return;
        }

        String name = "";
        String number = "";
        String contactId = null;
        try (Cursor cursor = getContentResolver().query(contactUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = readCursorString(cursor, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.Contacts.DISPLAY_NAME);
                number = readCursorString(cursor, ContactsContract.CommonDataKinds.Phone.NUMBER, "data1");
                contactId = readCursorString(cursor, ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.Contacts._ID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(number)) {
            number = lookupPhoneNumber(contactId, contactUri);
        }
        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(number)) {
            return;
        }

        String contactText = "Contact Details" + "\n\nName : " + name + "\nPhone : " + number;
        etMessage.setText(contactText);
        if (etMessage.getText() != null) {
            etMessage.setSelection(etMessage.getText().length());
        }
        etMessage.requestFocus();
    }

    @Nullable
    private String lookupPhoneNumber(@Nullable String contactId, @NonNull Uri contactUri) {
        String resolvedId = contactId;
        if (TextUtils.isEmpty(resolvedId)) {
            resolvedId = contactUri.getLastPathSegment();
        }
        if (TextUtils.isEmpty(resolvedId)) {
            return "";
        }
        try (Cursor phoneCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{resolvedId}, null)) {
            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                return readCursorString(phoneCursor, ContactsContract.CommonDataKinds.Phone.NUMBER, "data1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @NonNull
    private static String readCursorString(@NonNull Cursor cursor, @NonNull String... columnNames) {
        for (String columnName : columnNames) {
            int index = cursor.getColumnIndex(columnName);
            if (index >= 0) {
                String value = cursor.getString(index);
                if (!TextUtils.isEmpty(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    private boolean isDefaultSmsApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_SMS);
            }
            return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void requestDefaultSmsApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    defaultSmsLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS));
                    return;
                }
            }
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivity(intent);
            overridePendingTransition(0, 0);
        } catch (Exception e) {
            pendingDeleteConversation = false;
            pendingSendMessage = false;
            e.printStackTrace();
        }
    }

    private String get10DigitNumber(String input) {
        if (input == null) {
            return "";
        }
        String digits = input.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        if (digits.length() == 10) {
            return digits;
        }
        return "";
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onMessageContentItemLongClickListener(boolean isSelectionMode, int selectedCount) {
        if (isSelectionMode) {
            llDefaultActionBar.setVisibility(GONE);
            llSelectionActionBar.setVisibility(VISIBLE);
            tvSelectedCount.setText(selectedCount + " " + getString(R.string.selected));
        } else {
            llDefaultActionBar.setVisibility(VISIBLE);
            llSelectionActionBar.setVisibility(GONE);
        }
    }

    private void markThreadAsRead() {
        if (TextUtils.isEmpty(threadId)) {
            return;
        }
        runOnBackground(() -> SmsUnreadHelper.markThreadAsRead(this, threadId, address));
    }

    private void runOnBackground(Runnable runnable) {
        if (isFinishing() || isDestroyed() || executor.isShutdown()) {
            return;
        }
        try {
            executor.execute(runnable);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    private void registerSmsObserver() {
        smsObserver = new ContentObserver(mainHandler) {
            @Override
            public void onChange(boolean selfChange) {
                loadMessages();
                markThreadAsRead();
            }
        };
        getContentResolver().registerContentObserver(Uri.parse("content://sms"), true, smsObserver);
    }

    private void scrollToBottom() {
        if (messagesContentAdapter.getItemCount() > 0) {
            rvMessagesContent.scrollToPosition(messagesContentAdapter.getItemCount() - 1);
        }
    }

    private Bitmap getContactPhoto(Context context, String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.trim().isEmpty()) {
            return null;
        }
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.PHOTO_URI}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String contactPhotoUri = cursor.getString(0);
                    if (contactPhotoUri != null) {
                        InputStream input = context.getContentResolver().openInputStream(Uri.parse(contactPhotoUri));
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

    @Override
    protected void onResume() {
        super.onResume();
        refreshContactInfo();
        markThreadAsRead();
        loadMessages();
        if (!TextUtils.isEmpty(address)) {
            currentOpenNumber = address;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (etMessage != null && address != null) {
            Utils.setDraft(this, address, etMessage.getText().toString());
        }
        currentOpenNumber = "";
    }

    private void finishInstant() {
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (smsObserver != null) {
            getContentResolver().unregisterContentObserver(smsObserver);
        }
        executor.shutdownNow();
    }
}