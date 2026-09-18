package com.messages.smart.sms.fragments;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.messages.smart.sms.R;
import com.messages.smart.sms.common.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ContactsFragment extends Fragment {
    private AppCompatTextView tvTitle, tvContacts;
    private AppCompatTextView tvPermissionRequired, tvPermissionContactDescription, tvBtnAllow;
    private LinearLayout llContactData, llPermissionData, llPermissionAllow;
    private ShimmerFrameLayout btnAllow;

    private Fragment allcontactsFragment;

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int REQUIRED_CONTACTS_DENIALS_FOR_SETTINGS = 2;
    private static final String KEY_CONTACTS_PERMISSION_DENY_COUNT = "contacts_permission_deny_count";
    private static final String TAG_ALL_CONTACTS = "ALLCONTACTS";
    private static final String KEY_CHILD_TAB = "contacts_child_tab";
    private static final String KEY_CONTACTS_UI_READY = "contacts_ui_ready";
    private String currentTab;
    private boolean contactsUiReady;
    private boolean isRequestingContactsPermissions = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getString(KEY_CHILD_TAB);
            contactsUiReady = savedInstanceState.getBoolean(KEY_CONTACTS_UI_READY, false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentTab != null) {
            outState.putString(KEY_CHILD_TAB, currentTab);
        }
        outState.putBoolean(KEY_CONTACTS_UI_READY, contactsUiReady);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        initViews(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.post(this::requestPermissionsWhenShown);
    }

    public void requestPermissionsWhenShown() {
        if (!isAdded() || isHidden() || getContext() == null || !areViewsReady()) {
            return;
        }
        refreshContactsTab();
    }

    private boolean areViewsReady() {
        return getView() != null && llContactData != null && llPermissionData != null && llPermissionAllow != null && tvContacts != null;
    }

    private void initViews(View viewInside) {
        tvTitle = viewInside.findViewById(R.id.tvTitle);

        llContactData = viewInside.findViewById(R.id.llContactData);
        llPermissionData = viewInside.findViewById(R.id.llPermissionData);
        llPermissionAllow = viewInside.findViewById(R.id.llPermissionAllow);
        btnAllow = viewInside.findViewById(R.id.btnAllow);
        tvContacts = viewInside.findViewById(R.id.tvContacts);
        tvPermissionRequired = viewInside.findViewById(R.id.tvPermissionRequired);
        tvPermissionContactDescription = viewInside.findViewById(R.id.tvPermissionContactDescription);
        tvBtnAllow = viewInside.findViewById(R.id.tvBtnAllow);

        clickEvents();
    }

    private void clickEvents() {
        tvTitle.setText(getContext().getResources().getText(R.string.contacts));

        btnAllow.setOnClickListener(view -> onAllowButtonClicked());
    }

    private void onAllowButtonClicked() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (hasAllPermissions()) {
            setupContactFragments();
            return;
        }

        showPermissionUi();
        requestContactsPermissions(true);
    }

    private void requestContactsPermissions(boolean fromAllowButton) {
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (hasAllPermissions()) {
            if (!contactsUiReady) {
                setupContactFragments();
            }
            return;
        }

        showPermissionUi();

        ArrayList<String> permissionList = buildMissingPermissionsList();
        if (permissionList.isEmpty()) {
            return;
        }

        boolean permanentlyDenied = isContactsPermissionPermanentlyDenied(permissionList);
        int denyCount = getContactsPermissionDenyCount();

        if (fromAllowButton && (permanentlyDenied || denyCount >= REQUIRED_CONTACTS_DENIALS_FOR_SETTINGS)) {
            dialogSettingPermission();
            return;
        }

        if (permanentlyDenied) {
            return;
        }

        if (!isRequestingContactsPermissions) {
            isRequestingContactsPermissions = true;
            savePermissionAsked(permissionList);
            requestPermissions(permissionList.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private ArrayList<String> buildMissingPermissionsList() {
        ArrayList<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_CONTACTS);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CALL_PHONE);
        }
        return permissionList;
    }

    private boolean isContactsPermissionPermanentlyDenied(ArrayList<String> permissionList) {
        for (String permission : permissionList) {
            if (isPermissionAskedBefore(permission) && !shouldShowRequestPermissionRationale(permission)) {
                return true;
            }
        }
        return false;
    }

    private void showPermissionUi() {
        if (!areViewsReady()) {
            return;
        }
        contactsUiReady = false;
        llContactData.setVisibility(GONE);
        llPermissionData.setVisibility(VISIBLE);
        llPermissionAllow.setVisibility(VISIBLE);
        tvContacts.setVisibility(VISIBLE);
    }

    private int getContactsPermissionDenyCount() {
        if (getActivity() == null) {
            return 0;
        }
        return getActivity().getSharedPreferences("permission", MODE_PRIVATE).getInt(KEY_CONTACTS_PERMISSION_DENY_COUNT, 0);
    }

    private void incrementContactsPermissionDenyCount() {
        if (getActivity() == null) {
            return;
        }
        SharedPreferences prefs = getActivity().getSharedPreferences("permission", MODE_PRIVATE);
        prefs.edit().putInt(KEY_CONTACTS_PERMISSION_DENY_COUNT, prefs.getInt(KEY_CONTACTS_PERMISSION_DENY_COUNT, 0) + 1).apply();
    }

    private void resetContactsPermissionDenyCount() {
        if (getActivity() == null) {
            return;
        }
        getActivity().getSharedPreferences("permission", MODE_PRIVATE).edit().putInt(KEY_CONTACTS_PERMISSION_DENY_COUNT, 0).apply();
    }

    private boolean isPermissionAskedBefore(String permission) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("permission", MODE_PRIVATE);
        return sharedPreferences.getBoolean(permission, false);
    }

    private void savePermissionAsked(List<String> permissions) {
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("permission", MODE_PRIVATE).edit();
        for (String permission : permissions) {
            editor.putBoolean(permission, true);
        }
        editor.apply();
    }

    private void dialogSettingPermission() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
        bottomSheetDialog.setContentView(R.layout.dialog_setting_permission);

        AppCompatTextView tvCancel = bottomSheetDialog.findViewById(R.id.tvCancel);
        AppCompatTextView tvOpenSettings = bottomSheetDialog.findViewById(R.id.tvOpenSettings);

        tvCancel.setOnClickListener(view -> bottomSheetDialog.dismiss());

        tvOpenSettings.setOnClickListener(view -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
            Utils.clearActivityTransition(getActivity());
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            isRequestingContactsPermissions = false;
            savePermissionAsked(Arrays.asList(permissions));

            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    trackContactsPermissionGranted(permissions[i]);
                } else {
                    allGranted = false;
                }
            }

            if (allGranted) {
                resetContactsPermissionDenyCount();
                setupContactFragments();
            } else {
                incrementContactsPermissionDenyCount();
                showPermissionUi();
            }
        }
    }

    private void trackContactsPermissionGranted(String permission) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        if (Manifest.permission.READ_CONTACTS.equals(permission)) {
            Utils.trackScreenOnce(requireContext(), "READ_CONTACTS");
        } else if (Manifest.permission.READ_PHONE_STATE.equals(permission)) {
            Utils.trackScreenOnce(requireContext(), "READ_PHONE_STATE");
        } else if (Manifest.permission.CALL_PHONE.equals(permission)) {
            Utils.trackScreenOnce(requireContext(), "CALL_PHONE");
        }
    }

    private void setupContactFragments() {
        if (!isAdded() || getActivity() == null || !areViewsReady()) {
            return;
        }

        llContactData.setVisibility(VISIBLE);
        llPermissionData.setVisibility(GONE);
        llPermissionAllow.setVisibility(GONE);
        tvContacts.setVisibility(GONE);

        FragmentManager fragmentManager = getChildFragmentManager();

        allcontactsFragment = fragmentManager.findFragmentByTag(TAG_ALL_CONTACTS);

        if (allcontactsFragment == null) {
            allcontactsFragment = new AllContactsFragment();
        }

        contactsUiReady = true;

        if (currentTab == null || "AllContactsFragment".equals(currentTab)) {
            showAllContactsTab();
        }
    }

    private void showAllContactsTab() {
        currentTab = "AllContactsFragment";
        switchFragment(allcontactsFragment);
    }

    private void switchFragment(Fragment fragment) {
        if (!isAdded() || fragment == null) return;
        getChildFragmentManager().beginTransaction().replace(R.id.flDataContacts, fragment, ContactsFragment.TAG_ALL_CONTACTS).commitAllowingStateLoss();
    }

    private void restoreTabState() {
        if (!contactsUiReady || currentTab == null || !isAdded() || getActivity() == null || !areViewsReady()) {
            return;
        }

        llContactData.setVisibility(VISIBLE);
        llPermissionData.setVisibility(GONE);
        llPermissionAllow.setVisibility(GONE);
        tvContacts.setVisibility(GONE);

        FragmentManager fragmentManager = getChildFragmentManager();
        allcontactsFragment = fragmentManager.findFragmentByTag(TAG_ALL_CONTACTS);

        if (allcontactsFragment == null) {
            allcontactsFragment = new AllContactsFragment();
        }

        Fragment visibleFragment = fragmentManager.findFragmentById(R.id.flDataContacts);
        if (visibleFragment != allcontactsFragment) {
            switchFragment(allcontactsFragment);
        }
    }

    private boolean hasAllPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshContactsTab() {
        if (!isAdded() || getContext() == null || !areViewsReady()) {
            return;
        }

        if (tvTitle != null) {
            tvTitle.setText(getContext().getResources().getText(R.string.contacts));
        }

        if (hasAllPermissions()) {
            if (!contactsUiReady) {
                setupContactFragments();
            } else {
                restoreTabState();
            }
        } else {
            showPermissionUi();
            requestContactsPermissions(false);
        }
    }

    public void refreshContactsTexts() {
        if (!isAdded() || getContext() == null || tvTitle == null) {
            return;
        }
        Context localized = Utils.localeResourcesContext(getContext());
        tvTitle.setText(localized.getResources().getText(R.string.contacts));
        refreshPermissionLocaleTexts();
        Fragment fragment = getChildFragmentManager().findFragmentByTag(TAG_ALL_CONTACTS);
        if (fragment instanceof AllContactsFragment) {
            ((AllContactsFragment) fragment).refreshLocaleUi();
        }
    }

    private void refreshPermissionLocaleTexts() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        Context localized = Utils.localeResourcesContext(getContext());
        if (tvPermissionRequired != null) {
            tvPermissionRequired.setText(localized.getResources().getText(R.string.permission_required));
        }
        if (tvPermissionContactDescription != null) {
            tvPermissionContactDescription.setText(localized.getResources().getText(R.string.permission_contact_description));
        }
        if (tvBtnAllow != null) {
            tvBtnAllow.setText(localized.getResources().getText(R.string.allow));
        }
        if (tvContacts != null) {
            tvContacts.setText(localized.getResources().getText(R.string.your_contacts_will_appear_here));
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            clearChildSearch();
            return;
        }
        refreshContactsTexts();
        refreshContactsTab();
    }

    private void clearChildSearch() {
        Fragment fragment = getChildFragmentManager().findFragmentByTag(TAG_ALL_CONTACTS);
        if (fragment instanceof AllContactsFragment) {
            ((AllContactsFragment) fragment).clearSearch();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isAdded() || getContext() == null) {
            return;
        }

        if (!isVisible() || isHidden()) {
            return;
        }

        refreshContactsTexts();
        requestPermissionsWhenShown();
    }
}