package com.messages.smart.sms.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.smart.sms.R;
import com.messages.smart.sms.adapters.AllContactsAdapter;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.models.ContactsModel;

import java.util.ArrayList;
import java.util.HashSet;

public class AllContactsFragment extends Fragment {
    private AppCompatEditText etSearch;
    private AppCompatTextView tvNoSearchResults;
    private ProgressBar pbDataLoad;
    private RecyclerView rvAllContacts;

    private final ArrayList<ContactsModel> arrayListContactsModel = new ArrayList<>();
    private AllContactsAdapter allContactsAdapter;
    private TextWatcher searchTextWatcher;
    private boolean dataLoaded = false;
    private boolean suppressSearchCallback = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_contacts, container, false);

        initViews(view);

        return view;
    }

    private void initViews(View viewInside) {
        etSearch = viewInside.findViewById(R.id.etSearch);
        tvNoSearchResults = viewInside.findViewById(R.id.tvNoSearchResults);
        pbDataLoad = viewInside.findViewById(R.id.pbDataLoad);
        rvAllContacts = viewInside.findViewById(R.id.rvAllContacts);

        clickEvents();
    }

    private void clickEvents() {
        etSearch.setHint(getContext().getResources().getText(R.string.search_contact));
        configureSearchInput(etSearch);

        if (searchTextWatcher == null) {
            searchTextWatcher = new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    if (suppressSearchCallback) {
                        return;
                    }
                    searchContact(charSequence.toString());
                }

                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            };
            etSearch.addTextChangedListener(searchTextWatcher);
        }

        if (!dataLoaded) {
            if (hasReadContactsPermission()) {
                new getContactsBackground().execute();
            } else {
                pbDataLoad.setVisibility(GONE);
                bindListToView();
            }
        } else {
            pbDataLoad.setVisibility(GONE);
            bindListToView();
        }
    }

    private boolean hasReadContactsPermission() {
        Context context = getContext();
        return context != null && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void configureSearchInput(AppCompatEditText editText) {
        if (editText == null) {
            return;
        }
        editText.setSingleLine(true);
        editText.setMaxLines(1);
        editText.setHorizontallyScrolling(true);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50), (source, start, end, dest, dstart, dend) -> {
            if (source == null) {
                return null;
            }
            StringBuilder filtered = new StringBuilder();
            for (int i = start; i < end; i++) {
                char character = source.charAt(i);
                if (character != '\n' && character != '\r') {
                    filtered.append(character);
                }
            }
            if (filtered.length() == end - start) {
                return null;
            }
            return filtered;
        }
        });
        editText.setOnEditorActionListener((textView, actionId, event) -> {
            hideKeyboard();
            return true;
        });
    }

    private void bindListToView() {
        setContactsAdapter();
        if (etSearch != null && etSearch.getText() != null) {
            searchContact(etSearch.getText().toString());
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class getContactsBackground extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pbDataLoad.setVisibility(VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            getContacts();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (getActivity() == null || getContext() == null) return;
            pbDataLoad.setVisibility(GONE);
            dataLoaded = true;
            setContactsAdapter();
        }
    }

    private void getContacts() {
        Context context = getContext();
        if (context == null) return;
        arrayListContactsModel.clear();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try (Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            HashSet<String> unique = new HashSet<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String photo = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String normalized = PhoneNumberUtils.normalizeNumber(number);
                    if (!unique.contains(normalized)) {
                        unique.add(normalized);
                        arrayListContactsModel.add(new ContactsModel(photo, name, number));
                    }
                }
            }
        } catch (SecurityException ignored) {
            arrayListContactsModel.clear();
        }
        arrayListContactsModel.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    private void setContactsAdapter() {
        allContactsAdapter = new AllContactsAdapter(getContext(), arrayListContactsModel, etSearch);
        rvAllContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAllContacts.setAdapter(allContactsAdapter);
        setupScrollKeyboardDismiss();
    }

    private void setupScrollKeyboardDismiss() {
        if (rvAllContacts == null) {
            return;
        }
        rvAllContacts.clearOnScrollListeners();
        rvAllContacts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard();
                }
            }
        });
    }

    private void searchContact(String name) {
        if (allContactsAdapter == null) {
            return;
        }
        ArrayList<ContactsModel> searchList = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            searchList.addAll(arrayListContactsModel);
        } else {
            String search = name.toLowerCase();
            for (ContactsModel contactsModel : arrayListContactsModel) {
                boolean nameMatch = contactsModel.getName() != null && contactsModel.getName().toLowerCase().contains(search);
                boolean numberMatch = contactsModel.getNumber() != null && contactsModel.getNumber().toLowerCase().contains(search);
                if (nameMatch || numberMatch) {
                    searchList.add(contactsModel);
                }
            }
        }

        allContactsAdapter.updateList(searchList);
        updateSearchEmptyState(name, searchList.isEmpty());
    }

    private void updateSearchEmptyState(String query, boolean noResults) {
        if (tvNoSearchResults == null || rvAllContacts == null) {
            return;
        }
        boolean isSearching = query != null && !query.trim().isEmpty();
        if (!isSearching) {
            tvNoSearchResults.setVisibility(GONE);
            rvAllContacts.setVisibility(VISIBLE);
            return;
        }
        tvNoSearchResults.setVisibility(noResults ? VISIBLE : GONE);
        rvAllContacts.setVisibility(noResults ? GONE : VISIBLE);
    }

    public void clearSearch() {
        if (etSearch == null) {
            return;
        }
        hideKeyboard();
        if (!etSearch.getText().toString().trim().isEmpty()) {
            suppressSearchCallback = true;
            etSearch.setText("");
            suppressSearchCallback = false;
            searchContact("");
        } else if (allContactsAdapter != null) {
            searchContact("");
        }
        updateSearchEmptyState("", false);
    }

    private void hideKeyboard() {
        if (!isAdded() || etSearch == null) {
            return;
        }
        etSearch.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        allContactsAdapter = null;
        searchTextWatcher = null;
        etSearch = null;
        tvNoSearchResults = null;
        pbDataLoad = null;
        rvAllContacts = null;
    }

    public void refreshLocaleUi() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        Context localized = Utils.localeResourcesContext(getContext());
        if (etSearch != null) {
            etSearch.setHint(localized.getResources().getText(R.string.search_contact));
        }
        if (tvNoSearchResults != null) {
            tvNoSearchResults.setText(localized.getResources().getText(R.string.no_data_found));
            if (etSearch != null) {
                String query = etSearch.getText().toString();
                boolean noResults = allContactsAdapter != null && allContactsAdapter.getItemCount() == 0;
                updateSearchEmptyState(query, noResults);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshLocaleUi();
    }
}