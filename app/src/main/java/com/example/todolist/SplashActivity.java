package com.example.todolist;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int DISPLAY_MS = 2500;
    private static final String QUOTE_API = "https://api.quotable.io/random?tags=productivity|motivation|wisdom&maxLength=120";

    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private final Random random = new Random();

    private TextView tvQuote;
    private TextView tvAuthor;
    private ProgressBar progressBar;

    // ── Local fallback quotes (productivity, focus, habits) ──
    private static final List<Quote> LOCAL_QUOTES = new ArrayList<>();
    static {
        LOCAL_QUOTES.add(new Quote("积微致著，累浅成深。", "格言"));
        LOCAL_QUOTES.add(new Quote("不积跬步，无以至千里。", "荀子"));
        LOCAL_QUOTES.add(new Quote("今日事，今日毕。", "谚语"));
        LOCAL_QUOTES.add(new Quote("自律给我自由。", "康德"));
        LOCAL_QUOTES.add(new Quote("时间就像海绵里的水，挤一挤总会有的。", "鲁迅"));
        LOCAL_QUOTES.add(new Quote("专注一件事，做到极致。", "格言"));
        LOCAL_QUOTES.add(new Quote("千里之行，始于足下。", "老子"));
        LOCAL_QUOTES.add(new Quote("每一天都是新的开始。", "谚语"));
        LOCAL_QUOTES.add(new Quote("习惯形成性格，性格决定命运。", "约·凯恩斯"));
        LOCAL_QUOTES.add(new Quote("完成比完美更重要。", "格言"));
        LOCAL_QUOTES.add(new Quote("少壮不努力，老大徒伤悲。", "《长歌行》"));
        LOCAL_QUOTES.add(new Quote("行动是治愈恐惧的良药。", "卡耐基"));
        LOCAL_QUOTES.add(new Quote("效率是把事情做对，效能是做对的事情。", "德鲁克"));
        LOCAL_QUOTES.add(new Quote("锲而不舍，金石可镂。", "荀子"));
        LOCAL_QUOTES.add(new Quote("最好的时机是昨天，其次是现在。", "谚语"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tvQuote = findViewById(R.id.tv_quote);
        tvAuthor = findViewById(R.id.tv_quote_author);
        progressBar = findViewById(R.id.progress_bar);

        // Fade-in animation
        View root = findViewById(android.R.id.content);
        root.setAlpha(0f);
        ObjectAnimator anim = ObjectAnimator.ofFloat(root, "alpha", 0f, 1f);
        anim.setDuration(600);
        anim.start();

        // Try fetching a quote from network, fall back to local
        exec.execute(this::fetchQuote);

        // Navigate to main activity after delay
        handler.postDelayed(this::goToMain, DISPLAY_MS);
    }

    private void fetchQuote() {
        Quote quote = null;
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder().url(QUOTE_API).build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                QuoteApiItem item = gson.fromJson(json, QuoteApiItem.class);
                if (item != null && item.content != null) {
                    String author = (item.author != null && !item.author.isEmpty())
                            ? item.author : "名言";
                    quote = new Quote(item.content, author);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Fetch quote failed, using local: " + e.getMessage());
        }

        // Fallback to local
        if (quote == null) {
            quote = LOCAL_QUOTES.get(random.nextInt(LOCAL_QUOTES.size()));
        }

        final Quote finalQuote = quote;
        handler.post(() -> {
            tvQuote.setText("「" + finalQuote.content + "」");
            tvAuthor.setText("—— " + finalQuote.author);
            progressBar.setVisibility(View.GONE);
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exec.shutdownNow();
        handler.removeCallbacksAndMessages(null);
    }

    // ── Data classes ──

    private static class Quote {
        final String content;
        final String author;
        Quote(String content, String author) {
            this.content = content;
            this.author = author;
        }
    }

    private static class QuoteApiItem {
        @SerializedName("content")
        String content;
        @SerializedName("author")
        String author;
    }
}
