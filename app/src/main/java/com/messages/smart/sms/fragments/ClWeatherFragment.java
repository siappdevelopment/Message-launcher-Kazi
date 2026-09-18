package com.messages.smart.sms.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import com.messages.smart.sms.R;

import java.util.Calendar;

public class ClWeatherFragment extends Fragment {
    private AppCompatImageView ivWishIcon;
    private AppCompatTextView tvWish, tvDesc;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clweather, container, false);

        findIDs(view);

        return view;
    }

    private void findIDs(View view) {
        ivWishIcon = view.findViewById(R.id.ivWishIcon);
        tvWish = view.findViewById(R.id.tvWish);
        tvDesc = view.findViewById(R.id.tvDesc);

        initialEvents();
    }

    private void initialEvents() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        int month = calendar.get(Calendar.MONTH) + 1;
        boolean isSummer = month >= 3 && month <= 6;
        boolean isMonsoon = month >= 7 && month <= 9;

        int icon;
        String wish;
        String desc;

        if (hour >= 6 && hour < 12) {
            icon = R.drawable.ic_good_morning;
            wish = "Good Morning!";

        } else if (hour >= 12 && hour < 18) {
            icon = R.drawable.ic_good_afternoon;
            wish = "Good Afternoon!";

        } else if (hour >= 18 && hour < 21) {
            icon = R.drawable.ic_good_evening;
            wish = "Good Evening!";
        } else {
            icon = R.drawable.ic_good_night;
            wish = "Good Night!";
        }

        if (isSummer) {
            desc = "Today, the sun rises at 5:58 and sets at 19:08";
        } else if (isMonsoon) {
            desc = "Today, the sun rises at 6:15 and sets at 18:52";
        } else {
            desc = "Today, the sun rises at 6:48 and sets at 18:03";
        }

        ivWishIcon.setImageResource(icon);
        tvWish.setText(wish);
        tvDesc.setText(desc);
    }
}