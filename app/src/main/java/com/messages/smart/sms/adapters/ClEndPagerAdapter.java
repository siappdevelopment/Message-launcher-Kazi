package com.messages.smart.sms.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.messages.smart.sms.fragments.ClMessageFragment;
import com.messages.smart.sms.fragments.ClMoreOptionFragment;
import com.messages.smart.sms.fragments.ClReminderFragment;
import com.messages.smart.sms.fragments.ClWeatherFragment;

public class ClEndPagerAdapter extends FragmentStateAdapter {
    public ClEndPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return new ClMessageFragment();
            case 2:
                return new ClReminderFragment();
            case 3:
                return new ClMoreOptionFragment();
            default:
                return new ClWeatherFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}