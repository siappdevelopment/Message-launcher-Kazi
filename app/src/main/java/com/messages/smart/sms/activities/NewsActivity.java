package com.messages.smart.sms.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.smart.sms.R;
import com.messages.smart.sms.adapters.NewsAdapter;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.models.NewsModel;
import com.messages.smart.sms.models.NewsResponseModel;
import com.messages.smart.sms.services.APIClient;
import com.messages.smart.sms.services.APIInterface;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsActivity extends AppCompatActivity {
    private AppCompatEditText etSearch;
    private AppCompatImageView ivSearch;
    private ProgressBar pbDataLoad;
    private RecyclerView rvNews;

    private final ArrayList<NewsModel> arrayListNewsModel = new ArrayList<>();
    private String stringSearch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.light(Color.WHITE, Color.WHITE));
        setContentView(R.layout.activity_news);

        initViews();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        ivSearch = findViewById(R.id.ivSearch);
        pbDataLoad = findViewById(R.id.pbDataLoad);
        rvNews = findViewById(R.id.rvNews);

        clickEvents();
    }

    private void clickEvents() {
        Utils.trackScreen(this, "NEWS_SCREEN_SHOW");
        getNews();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                stringSearch = charSequence.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        ivSearch.setOnClickListener(view -> {
            if (!stringSearch.isEmpty()) {
                String url = "https://www.google.com/search?q=" + Uri.encode(stringSearch);
                openUrl(url);
                etSearch.setText("");
                etSearch.clearFocus();
            } else {
                Toast.makeText(NewsActivity.this, "Please Enter Search !", Toast.LENGTH_SHORT).show();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NewsActivity.this.finish();
                overridePendingTransition(0, 0);
            }
        });
    }

    private void getNews() {
        pbDataLoad.setVisibility(VISIBLE);

        APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);
        Call<NewsResponseModel> call = apiInterface.getLatestNews("TBX2Bm48azyFOI8l53wfhOItaZlKSnHXb286agRi79Y0B-al", "en");
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponseModel> call, @NonNull Response<NewsResponseModel> response) {
                if (response.isSuccessful() && response.body().getNews() != null) {
                    arrayListNewsModel.clear();
                    arrayListNewsModel.addAll(response.body().getNews());

                    if (AdPlacement.shouldShowNewsNativeAd1() && arrayListNewsModel.size() > 1) {
                        NewsModel newsModel1 = new NewsModel();
                        newsModel1.setAdShow(true);
                        newsModel1.setAdSlot(1);
                        arrayListNewsModel.add(1, newsModel1);
                    }

                    if (AdPlacement.shouldShowNewsNativeAd2() && arrayListNewsModel.size() > 5) {
                        NewsModel newsModel5 = new NewsModel();
                        newsModel5.setAdShow(true);
                        newsModel5.setAdSlot(2);
                        arrayListNewsModel.add(5, newsModel5);
                    }

                    setNewsAdapter();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponseModel> call, @NonNull Throwable t) {
            }
        });
    }

    private void setNewsAdapter() {
        pbDataLoad.setVisibility(GONE);

        NewsAdapter newsAdapter = new NewsAdapter(getApplicationContext(), arrayListNewsModel);
        rvNews.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        rvNews.setAdapter(newsAdapter);
    }

    private void openUrl(String url) {
        Uri uri = Uri.parse(url);
        Intent chromeIntent = new Intent(Intent.ACTION_VIEW, uri);
        chromeIntent.setPackage("com.android.chrome");
        try {
            startActivity(chromeIntent);
            overridePendingTransition(0, 0);
            return;
        } catch (Exception ignored) {
        }

        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(browserIntent);
            overridePendingTransition(0, 0);
        } catch (Exception e) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
        }
    }
}