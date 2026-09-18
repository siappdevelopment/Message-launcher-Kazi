package com.messages.smartsms.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.messages.smartsms.R;
import com.messages.smartsms.common.Utils;

import java.util.ArrayList;
import java.util.List;

public class ClMoreOptionFragment extends Fragment {
    private LinearLayout llAddToContact, llSendMessage, llSendMail, llCalendar, llWeb;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clmore_option, container, false);

        findIDs(view);

        return view;
    }

    private void findIDs(View view) {
        llAddToContact = view.findViewById(R.id.llAddToContact);
        llSendMessage = view.findViewById(R.id.llSendMessage);
        llSendMail = view.findViewById(R.id.llSendMail);
        llCalendar = view.findViewById(R.id.llCalendar);
        llWeb = view.findViewById(R.id.llWeb);

        initialEvents();
    }

    private void initialEvents() {
        llAddToContact.setOnClickListener(view -> startAndClose(new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI), new Intent(Intent.ACTION_INSERT).setType(ContactsContract.Contacts.CONTENT_TYPE), new Intent(Intent.ACTION_INSERT).setType("vnd.android.cursor.dir/contact")));

        llSendMessage.setOnClickListener(view -> startAndClose(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))));

        llSendMail.setOnClickListener(view -> startAndClose(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))));

        llCalendar.setOnClickListener(view -> startAndClose(calendarIntents()));

        llWeb.setOnClickListener(view -> startAndClose(webIntents()));
    }

    private Intent[] calendarIntents() {
        List<Intent> intents = new ArrayList<>();
        addLaunchIntent(intents, "com.google.android.calendar");
        addLaunchIntent(intents, "com.samsung.android.calendar");
        addLaunchIntent(intents, "com.android.calendar");

        Intent categoryIntent = new Intent(Intent.ACTION_MAIN);
        categoryIntent.addCategory(Intent.CATEGORY_APP_CALENDAR);
        intents.add(categoryIntent);

        Intent viewTime = new Intent(Intent.ACTION_VIEW);
        viewTime.setData(Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis()));
        intents.add(viewTime);

        Intent insertEvent = new Intent(Intent.ACTION_INSERT);
        insertEvent.setData(CalendarContract.Events.CONTENT_URI);
        intents.add(insertEvent);

        return intents.toArray(new Intent[0]);
    }

    private Intent[] webIntents() {
        List<Intent> intents = new ArrayList<>();
        addLaunchIntent(intents, "com.google.android.googlequicksearchbox");
        addLaunchIntent(intents, "com.android.chrome");
        intents.add(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")));
        return intents.toArray(new Intent[0]);
    }

    private void addLaunchIntent(List<Intent> intents, String packageName) {
        if (getContext() == null) {
            return;
        }
        Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            intents.add(launchIntent);
        }
    }

    private void startAndClose(Intent... intents) {
        if (!isAdded()) {
            return;
        }
        for (Intent intent : intents) {
            if (intent == null) {
                continue;
            }
            try {
                startActivity(intent);
                Utils.clearActivityTransition(getActivity());
                if (getActivity() != null) {
                    getActivity().finish();
                    Utils.clearActivityTransition(getActivity());
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}