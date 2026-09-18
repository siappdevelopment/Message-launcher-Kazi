package com.messages.smart.sms.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.messages.smart.sms.R;
import com.messages.smart.sms.activities.AboutMessagesActivity;
import com.messages.smart.sms.activities.BackupRestoreActivity;
import com.messages.smart.sms.activities.LanguageActivity;
import com.messages.smart.sms.activities.LauncherSettingsActivity;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.Utils;

import java.util.Objects;

public class SettingsFragment extends Fragment {
    private AppCompatTextView tvTitle, tvListTitle1, tvListSubTitle1, tvListTitle2, tvListSubTitle2, tvListTitle3, tvListSubTitle3, tvListTitle4, tvListSubTitle4, tvListTitle5, tvListSubTitle5, tvListTitle6, tvListTitle7, tvListTitle8;
    private LinearLayout llAppLanguage, llDelaySending, llSignatures, llBackupRestore, llLauncherSettings, llAboutMessages, llInviteFriends, llRateUs;

    private int delay = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        initViews(view);

        return view;
    }

    private void initViews(View viewInside) {
        tvTitle = viewInside.findViewById(R.id.tvTitle);
        llAppLanguage = viewInside.findViewById(R.id.llAppLanguage);
        tvListTitle1 = viewInside.findViewById(R.id.tvListTitle1);
        tvListSubTitle1 = viewInside.findViewById(R.id.tvListSubTitle1);
        llDelaySending = viewInside.findViewById(R.id.llDelaySending);
        tvListTitle2 = viewInside.findViewById(R.id.tvListTitle2);
        tvListSubTitle2 = viewInside.findViewById(R.id.tvListSubTitle2);
        llSignatures = viewInside.findViewById(R.id.llSignatures);
        tvListTitle3 = viewInside.findViewById(R.id.tvListTitle3);
        tvListSubTitle3 = viewInside.findViewById(R.id.tvListSubTitle3);
        llBackupRestore = viewInside.findViewById(R.id.llBackupRestore);
        tvListTitle4 = viewInside.findViewById(R.id.tvListTitle4);
        tvListSubTitle4 = viewInside.findViewById(R.id.tvListSubTitle4);
        llLauncherSettings = viewInside.findViewById(R.id.llLauncherSettings);
        tvListTitle5 = viewInside.findViewById(R.id.tvListTitle5);
        tvListSubTitle5 = viewInside.findViewById(R.id.tvListSubTitle5);
        llAboutMessages = viewInside.findViewById(R.id.llAboutMessages);
        tvListTitle6 = viewInside.findViewById(R.id.tvListTitle6);
        llInviteFriends = viewInside.findViewById(R.id.llInviteFriends);
        tvListTitle7 = viewInside.findViewById(R.id.tvListTitle7);
        llRateUs = viewInside.findViewById(R.id.llRateUs);
        tvListTitle8 = viewInside.findViewById(R.id.tvListTitle8);

        clickEvents();
    }

    private void clickEvents() {
        tvTitle.setText(getContext().getResources().getText(R.string.settings));

        tvListTitle1.setText(getContext().getResources().getText(R.string.app_language));
        tvListSubTitle1.setText(Utils.getAppLanguageNameNew(getContext()));

        tvListTitle2.setText(getContext().getResources().getText(R.string.delay_sending));
        if (!Utils.getDelaySending(getContext()).isEmpty()) {
            tvListSubTitle2.setText(Utils.getDelaySending(getContext()));
        } else {
            tvListSubTitle2.setText(getContext().getResources().getText(R.string.no_delay));
        }

        tvListTitle3.setText(getContext().getResources().getText(R.string.signatures));

        if (!Utils.getSignaturesName(getContext()).isEmpty()) {
            tvListSubTitle3.setText(Utils.getSignaturesName(getContext()));
        } else {
            tvListSubTitle3.setText(getContext().getResources().getText(R.string.add_your_signature));
        }

        tvListTitle4.setText(getContext().getResources().getText(R.string.backup_and_restore));
        tvListSubTitle4.setText(getContext().getResources().getText(R.string.backup_and_restore_message_easily));

        tvListTitle5.setText(getContext().getResources().getText(R.string.launcher_settings));
        tvListSubTitle5.setText(getContext().getResources().getText(R.string.customize_your_layout_and_icons));

        tvListTitle6.setText(getContext().getResources().getText(R.string.about_messages));
        tvListTitle7.setText(getContext().getResources().getText(R.string.invite_friends));
        tvListTitle8.setText(getContext().getResources().getText(R.string.rate_us));

        llAppLanguage.setOnClickListener(view -> {
            Utils.isAppLanguageStarting = false;
            if (AdPlacement.getOtherInterstitialAdShow()) {
                AdPlacement.loadInterstitialAd(getActivity(), AdPlacement.getOtherInterstitialId(), () -> openActivity(new Intent(getActivity(), LanguageActivity.class)));
            } else {
                openActivity(new Intent(getActivity(), LanguageActivity.class));
            }
        });

        llDelaySending.setOnClickListener(view -> dialogDelaySending());

        llSignatures.setOnClickListener(view -> dialogSignatures());

        llBackupRestore.setOnClickListener(view -> {
            if (AdPlacement.getOtherInterstitialAdShow()) {
                AdPlacement.loadInterstitialAd(getActivity(), AdPlacement.getOtherInterstitialId(), () -> openActivity(new Intent(getActivity(), BackupRestoreActivity.class)));
            } else {
                openActivity(new Intent(getActivity(), BackupRestoreActivity.class));
            }
        });

        llLauncherSettings.setOnClickListener(view -> {
            if (AdPlacement.getOtherInterstitialAdShow()) {
                AdPlacement.loadInterstitialAd(getActivity(), AdPlacement.getOtherInterstitialId(), () -> openActivity(new Intent(getActivity(), LauncherSettingsActivity.class)));
            } else {
                openActivity(new Intent(getActivity(), LauncherSettingsActivity.class));
            }
        });

        llAboutMessages.setOnClickListener(view -> openActivity(new Intent(getActivity(), AboutMessagesActivity.class)));

        llInviteFriends.setOnClickListener(view -> {
            try {
                String appPackageName = requireActivity().getPackageName();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                String shareMessage = "Manage Your SMS - Fast, Smart & Secure! Download this amazing messaging app:\n\n" + "https://play.google.com/store/apps/details?id=" + appPackageName;
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, "Share Using"));
                Utils.clearActivityTransition(getActivity());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        llRateUs.setOnClickListener(view -> {
            Uri uri = Uri.parse("market://details?id=" + requireActivity().getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
                Utils.clearActivityTransition(getActivity());
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + requireActivity().getPackageName())));
                Utils.clearActivityTransition(getActivity());
            }
        });
    }

    private void dialogDelaySending() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getActivity());
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
        bottomSheetDialog.setContentView(R.layout.dialog_delay_sending);

        RadioGroup rgDelay = bottomSheetDialog.findViewById(R.id.rgDelay);
        AppCompatTextView tvCancel = bottomSheetDialog.findViewById(R.id.tvCancel);
        AppCompatTextView tvOkay = bottomSheetDialog.findViewById(R.id.tvOkay);

        if (Utils.getDelaySending(getContext()).equals(getString(R.string.no_delay))) {
            rgDelay.check(R.id.rbNoDelay);
        } else if (Utils.getDelaySending(getContext()).equals(getString(R.string._3_sec_delay))) {
            rgDelay.check(R.id.rbThreeDelay);
        } else if (Utils.getDelaySending(getContext()).equals(getString(R.string._5_sec_delay))) {
            rgDelay.check(R.id.rbFiveDelay);
        } else if (Utils.getDelaySending(getContext()).equals(getString(R.string._10_sec_delay))) {
            rgDelay.check(R.id.rbTenDelay);
        }

        rgDelay.setOnCheckedChangeListener((radioGroup, i) -> {
            if (i == R.id.rbNoDelay) {
                delay = 0;
            } else if (i == R.id.rbThreeDelay) {
                delay = 3;
            } else if (i == R.id.rbFiveDelay) {
                delay = 5;
            } else if (i == R.id.rbTenDelay) {
                delay = 10;
            }
        });

        tvCancel.setOnClickListener(view -> bottomSheetDialog.dismiss());

        tvOkay.setOnClickListener(view -> {
            if (delay == 0) {
                Utils.setDelaySending(getContext(), getString(R.string.no_delay));
            } else if (delay == 3) {
                Utils.setDelaySending(getContext(), getString(R.string._3_sec_delay));
            } else if (delay == 5) {
                Utils.setDelaySending(getContext(), getString(R.string._5_sec_delay));
            } else if (delay == 10) {
                Utils.setDelaySending(getContext(), getString(R.string._10_sec_delay));
            }

            tvListSubTitle2.setText(Utils.getDelaySending(getContext()));

            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void dialogSignatures() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getActivity());
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
        bottomSheetDialog.setContentView(R.layout.dialog_signatures);

        AppCompatTextView tvDelete = bottomSheetDialog.findViewById(R.id.tvDelete);
        AppCompatEditText etSignaturesName = bottomSheetDialog.findViewById(R.id.etSignaturesName);
        AppCompatTextView tvCancel = bottomSheetDialog.findViewById(R.id.tvCancel);
        AppCompatTextView tvSave = bottomSheetDialog.findViewById(R.id.tvSave);

        etSignaturesName.setText(Utils.getSignaturesName(getContext()));

        if (!Utils.getSignaturesName(getContext()).isEmpty()) {
            tvDelete.setVisibility(VISIBLE);
        } else {
            tvDelete.setVisibility(GONE);
        }

        tvDelete.setOnClickListener(view -> {
            Utils.setSignaturesName(getContext(), "");

            if (!Utils.getSignaturesName(getContext()).isEmpty()) {
                tvListSubTitle3.setText(Utils.getSignaturesName(getContext()));
            } else {
                tvListSubTitle3.setText(getContext().getResources().getText(R.string.add_your_signature));
            }

            bottomSheetDialog.dismiss();
        });

        tvCancel.setOnClickListener(view -> bottomSheetDialog.dismiss());

        tvSave.setOnClickListener(view -> {
            if (!etSignaturesName.getText().toString().isEmpty()) {
                Utils.setSignaturesName(getContext(), etSignaturesName.getText().toString());

                if (!Utils.getSignaturesName(getContext()).isEmpty()) {
                    tvListSubTitle3.setText(Utils.getSignaturesName(getContext()));
                } else {
                    tvListSubTitle3.setText(getContext().getResources().getText(R.string.add_your_signature));
                }

                bottomSheetDialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Please Enter Text !", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.show();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshSettingsTexts();
        }
    }

    public void refreshSettingsTexts() {
        if (!isAdded() || getContext() == null || tvTitle == null) {
            return;
        }

        Context localized = Utils.localeResourcesContext(getContext());

        tvTitle.setText(localized.getResources().getText(R.string.settings));

        tvListTitle1.setText(localized.getResources().getText(R.string.app_language));
        tvListSubTitle1.setText(Utils.getAppLanguageNameNew(getContext()));

        tvListTitle2.setText(localized.getResources().getText(R.string.delay_sending));
        if (!Utils.getDelaySending(getContext()).isEmpty()) {
            tvListSubTitle2.setText(Utils.getDelaySending(getContext()));
        } else {
            tvListSubTitle2.setText(localized.getResources().getText(R.string.no_delay));
        }

        tvListTitle3.setText(localized.getResources().getText(R.string.signatures));

        if (!Utils.getSignaturesName(getContext()).isEmpty()) {
            tvListSubTitle3.setText(Utils.getSignaturesName(getContext()));
        } else {
            tvListSubTitle3.setText(localized.getResources().getText(R.string.add_your_signature));
        }

        tvListTitle4.setText(localized.getResources().getText(R.string.backup_and_restore));
        tvListSubTitle4.setText(localized.getResources().getText(R.string.backup_and_restore_message_easily));

        tvListTitle5.setText(localized.getResources().getText(R.string.launcher_settings));
        tvListSubTitle5.setText(localized.getResources().getText(R.string.customize_your_layout_and_icons));

        tvListTitle6.setText(localized.getResources().getText(R.string.about_messages));
        tvListTitle7.setText(localized.getResources().getText(R.string.invite_friends));
        tvListTitle8.setText(localized.getResources().getText(R.string.rate_us));
    }

    private void openActivity(Intent intent) {
        startActivity(intent);
        Utils.clearActivityTransition(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSettingsTexts();
    }
}