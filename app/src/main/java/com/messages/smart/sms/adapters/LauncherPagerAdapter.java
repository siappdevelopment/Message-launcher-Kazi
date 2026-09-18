package com.messages.smart.sms.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.messages.smart.sms.fragments.LauncherHomeFragment;
import com.messages.smart.sms.fragments.MainContainerFragment;
import com.messages.smart.sms.fragments.SubContainerFragment;

public class LauncherPagerAdapter extends FragmentStateAdapter {
    public static final int PAGE_MAIN = 0;
    public static final int PAGE_HOME = 1;
    public static final int PAGE_SUB = 2;

    private static final long ID_MAIN = 100L;
    private static final long ID_HOME = 101L;
    private static final long ID_SUB = 102L;

    public LauncherPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == PAGE_MAIN) {
            return new MainContainerFragment();
        }
        if (position == PAGE_SUB) {
            return new SubContainerFragment();
        }
        return new LauncherHomeFragment();
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    @Override
    public long getItemId(int position) {
        if (position == PAGE_MAIN) {
            return ID_MAIN;
        }
        if (position == PAGE_SUB) {
            return ID_SUB;
        }
        return ID_HOME;
    }

    @Override
    public boolean containsItem(long itemId) {
        return itemId == ID_MAIN || itemId == ID_HOME || itemId == ID_SUB;
    }
}