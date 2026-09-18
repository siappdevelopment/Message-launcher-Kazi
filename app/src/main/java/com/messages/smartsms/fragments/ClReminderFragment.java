package com.messages.smartsms.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.messages.smartsms.R;
import com.messages.smartsms.adapters.ClReminderAdapter;
import com.messages.smartsms.interfaces.OnReminderDeleteListener;
import com.messages.smartsms.models.ReminderModel;
import com.messages.smartsms.services.ReminderReceiver;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ClReminderFragment extends Fragment implements OnReminderDeleteListener {
    private AppCompatTextView tvCreateReminder, tvTime, btnNo, btnYes;
    private RecyclerView rvReminder;
    private LinearLayout llAddReminderView;
    private AppCompatEditText etAbout;
    private AppCompatImageView ivRed, ivBlack, ivGray, ivBlue, ivGreen, ivPurple, ivYellow, ivOrange, ivSkyblue;

    private final ArrayList<ReminderModel> arrayListReminder = new ArrayList<>();
    private ClReminderAdapter clReminderAdapter;
    private long selectedReminderTime = 0;
    private int selectedColor = Color.parseColor("#FF0000");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clreminder, container, false);

        findIDs(view);

        return view;
    }

    private void findIDs(View view) {
        tvCreateReminder = view.findViewById(R.id.tvCreateReminder);
        rvReminder = view.findViewById(R.id.rvReminder);

        llAddReminderView = view.findViewById(R.id.llAddReminderView);
        etAbout = view.findViewById(R.id.etAbout);
        tvTime = view.findViewById(R.id.tvTime);

        ivRed = view.findViewById(R.id.ivRed);
        ivBlack = view.findViewById(R.id.ivBlack);
        ivGray = view.findViewById(R.id.ivGray);
        ivBlue = view.findViewById(R.id.ivBlue);
        ivGreen = view.findViewById(R.id.ivGreen);
        ivPurple = view.findViewById(R.id.ivPurple);
        ivYellow = view.findViewById(R.id.ivYellow);
        ivOrange = view.findViewById(R.id.ivOrange);
        ivSkyblue = view.findViewById(R.id.ivSkyblue);

        btnNo = view.findViewById(R.id.btnNo);
        btnYes = view.findViewById(R.id.btnYes);

        initialEvents();
    }

    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    private void initialEvents() {
        clReminderAdapter = new ClReminderAdapter(getContext(), arrayListReminder, this);
        rvReminder.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReminder.setAdapter(clReminderAdapter);
        loadReminderList();

        AppCompatImageView selectedImage = ivRed;
        if (selectedColor == Color.parseColor("#000000")) {
            selectedImage = ivBlack;
        } else if (selectedColor == Color.parseColor("#858E97")) {
            selectedImage = ivGray;
        } else if (selectedColor == Color.parseColor("#1E5EFF")) {
            selectedImage = ivBlue;
        } else if (selectedColor == Color.parseColor("#00B140")) {
            selectedImage = ivGreen;
        } else if (selectedColor == Color.parseColor("#7A00D4")) {
            selectedImage = ivPurple;
        } else if (selectedColor == Color.parseColor("#F3CF3C")) {
            selectedImage = ivYellow;
        } else if (selectedColor == Color.parseColor("#FF6A1A")) {
            selectedImage = ivOrange;
        } else if (selectedColor == Color.parseColor("#12A4E8")) {
            selectedImage = ivSkyblue;
        }

        updateSelectedColorBorder(selectedImage, selectedColor, ivRed, ivBlack, ivGray, ivBlue, ivGreen, ivPurple, ivYellow, ivOrange, ivSkyblue);

        tvCreateReminder.setOnClickListener(view -> {
            tvCreateReminder.setVisibility(GONE);
            rvReminder.setVisibility(GONE);
            llAddReminderView.setVisibility(VISIBLE);
        });

        tvTime.setOnClickListener(view -> showDateTimePicker());

        View.OnClickListener colorSelectlistener = v -> {
            int color = Color.parseColor("#FF0000");
            AppCompatImageView selectedColorImage = ivRed;
            if (v.getId() == R.id.ivBlack) {
                color = Color.parseColor("#000000");
                selectedColorImage = ivBlack;
            } else if (v.getId() == R.id.ivGray) {
                color = Color.parseColor("#858E97");
                selectedColorImage = ivGray;
            } else if (v.getId() == R.id.ivBlue) {
                color = Color.parseColor("#1E5EFF");
                selectedColorImage = ivBlue;
            } else if (v.getId() == R.id.ivGreen) {
                color = Color.parseColor("#00B140");
                selectedColorImage = ivGreen;
            } else if (v.getId() == R.id.ivPurple) {
                color = Color.parseColor("#7A00D4");
                selectedColorImage = ivPurple;
            } else if (v.getId() == R.id.ivYellow) {
                color = Color.parseColor("#F3CF3C");
                selectedColorImage = ivYellow;
            } else if (v.getId() == R.id.ivOrange) {
                color = Color.parseColor("#FF6A1A");
                selectedColorImage = ivOrange;
            } else if (v.getId() == R.id.ivSkyblue) {
                color = Color.parseColor("#12A4E8");
                selectedColorImage = ivSkyblue;
            }

            selectedColor = color;
            updateSelectedColorBorder(selectedColorImage, color, ivRed, ivBlack, ivGray, ivBlue, ivGreen, ivPurple, ivYellow, ivOrange, ivSkyblue);
        };

        ivRed.setOnClickListener(colorSelectlistener);
        ivBlack.setOnClickListener(colorSelectlistener);
        ivGray.setOnClickListener(colorSelectlistener);
        ivBlue.setOnClickListener(colorSelectlistener);
        ivGreen.setOnClickListener(colorSelectlistener);
        ivPurple.setOnClickListener(colorSelectlistener);
        ivYellow.setOnClickListener(colorSelectlistener);
        ivOrange.setOnClickListener(colorSelectlistener);
        ivSkyblue.setOnClickListener(colorSelectlistener);

        btnNo.setOnClickListener(view -> {
            tvCreateReminder.setVisibility(VISIBLE);
            rvReminder.setVisibility(VISIBLE);
            llAddReminderView.setVisibility(GONE);
        });

        btnYes.setOnClickListener(view -> {
            String title = etAbout.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Please Enter Remind About", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedReminderTime == 0) {
                Toast.makeText(getContext(), "Please Select Date & Time", Toast.LENGTH_SHORT).show();
                return;
            }

            int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            ReminderModel reminderModel = new ReminderModel(title, selectedReminderTime, selectedColor, requestCode);
            arrayListReminder.add(0, reminderModel);
            scheduleReminder(reminderModel);
            saveReminderList();
            clReminderAdapter.notifyItemInserted(0);
            rvReminder.scrollToPosition(0);

            etAbout.setText("");
            tvTime.setText("Select date & time");
            selectedReminderTime = 0;

            tvCreateReminder.setVisibility(VISIBLE);
            rvReminder.setVisibility(VISIBLE);
            llAddReminderView.setVisibility(GONE);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadReminderList() {
        SharedPreferences preferences = requireContext().getSharedPreferences("reminder_preferences", Context.MODE_PRIVATE);
        String json = preferences.getString("reminder_list", "");
        if (!json.isEmpty()) {
            Type type = new TypeToken<ArrayList<ReminderModel>>() {
            }.getType();

            ArrayList<ReminderModel> savedList = new Gson().fromJson(json, type);
            arrayListReminder.clear();
            if (savedList != null) {
                arrayListReminder.addAll(savedList);
            }

            arrayListReminder.sort((a, b) -> Long.compare(b.getTime(), a.getTime()));
            clReminderAdapter.notifyDataSetChanged();
        }
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), R.style.CustomDatePickerTheme, (datePicker, year, month, day) -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), R.style.CustomTimePickerTheme, (timePicker, hour, minute) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, day, hour, minute, 0);
                selectedReminderTime = selectedCalendar.getTimeInMillis();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault());
                tvTime.setText(simpleDateFormat.format(selectedCalendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
            timePickerDialog.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateSelectedColorBorder(AppCompatImageView selectedView, int selectedColor, AppCompatImageView... views) {
        for (AppCompatImageView appCompatImageView : views) {
            ViewGroup.LayoutParams params = appCompatImageView.getLayoutParams();
            if (appCompatImageView == selectedView) {
                params.width = dpToPx(40);
                params.height = dpToPx(40);
                appCompatImageView.setPadding(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
                appCompatImageView.setBackgroundResource(R.drawable.custom_color_selected);
                appCompatImageView.setBackgroundTintList(null);
                ViewCompat.setBackgroundTintList(appCompatImageView, ColorStateList.valueOf(selectedColor));
            } else {
                params.width = dpToPx(30);
                params.height = dpToPx(30);
                appCompatImageView.setPadding(0, 0, 0, 0);
                appCompatImageView.setBackgroundResource(R.drawable.custom_circle);
            }

            appCompatImageView.setLayoutParams(params);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void saveReminderList() {
        SharedPreferences preferences = requireContext().getSharedPreferences("reminder_preferences", Context.MODE_PRIVATE);
        String json = new Gson().toJson(arrayListReminder);
        preferences.edit().putString("reminder_list", json).apply();
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleReminder(ReminderModel reminderModel) {
        Intent intent = new Intent(requireContext(), ReminderReceiver.class);
        intent.putExtra("title", reminderModel.getTitle());
        intent.putExtra("requestCode", reminderModel.getRequestCode());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), reminderModel.getRequestCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderModel.getTime(), pendingIntent);
        }
    }

    @Override
    public void onReminderDeleteListener(int position) {
        if (position >= 0 && position < arrayListReminder.size()) {
            ReminderModel reminderModel = arrayListReminder.get(position);

            Intent intent = new Intent(requireContext(), ReminderReceiver.class);
            intent.putExtra("requestCode", reminderModel.getRequestCode());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), reminderModel.getRequestCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            pendingIntent.cancel();

            arrayListReminder.remove(position);
            saveReminderList();
            clReminderAdapter.notifyItemRemoved(position);
        }
    }
}