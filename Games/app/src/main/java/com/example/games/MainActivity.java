package com.example.games;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Objects;


import io.flutter.Log;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends AppCompatActivity {
    Button gamesButton;
    String json;
    FlutterEngine flutterEngine;
    Button leaderboardButton;

    private static final String CHANNEL = "flutterChannel";
    public static final String STREAM = "eventChannel";
    private EventChannel.EventSink attachEvent;
    final String TAG_NAME = "From_Native";
    private int count = 1;
    private Handler handler;

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            int TOTAL_COUNT = 100;
            if (count > TOTAL_COUNT) {
                attachEvent.endOfStream();
            } else {
                double percentage = ((double) count / TOTAL_COUNT);
                Log.w(TAG_NAME, "\nParsing From Native:  " + percentage);
                attachEvent.success(percentage);
            }
            count++;
            handler.postDelayed(this, 200);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        flutterEngine = new FlutterEngine(this);
        flutterEngine.getDartExecutor().executeDartEntrypoint(
                DartExecutor.DartEntrypoint.createDefault()
        );
        FlutterEngineCache
                .getInstance()
                .put("my_engine_id", flutterEngine);


        setContentView(R.layout.activity_main);

        gamesButton = findViewById(R.id.games_button);

        leaderboardButton = findViewById(R.id.leaderboard_button);

        new EventChannel(Objects.requireNonNull(flutterEngine).getDartExecutor().getBinaryMessenger(), STREAM).setStreamHandler(
                new EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object args, final EventChannel.EventSink events) {
                        Log.w(TAG_NAME, "Adding listener");
                        attachEvent = events;
                        count = 1;
                        handler = new Handler();
                        runnable.run();
                    }

                    @Override
                    public void onCancel(Object args) {
                        Log.w(TAG_NAME, "Cancelling listener");
                        handler.removeCallbacks(runnable);
                        handler = null;
                        count = 1;
                        attachEvent = null;
                        System.out.println("StreamHandler - onCanceled: ");
                    }
                }
        );

        gamesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callFlutterFun(view);
            }
        });

        leaderboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ScoreActivity.class));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        handler = null;
        attachEvent = null;
    }

    void callFlutterFun(View view) {
        flutterEngine.getNavigationChannel().setInitialRoute("/");
        GeneratedPluginRegistrant.registerWith(flutterEngine);

        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL).setMethodCallHandler(
                (call, result) -> {
                    if (call.method.equals("sendData")) {
                        result.success(json + call.argument("data"));
                    }
                });
        startActivity(
                FlutterActivity
                        .withCachedEngine("my_engine_id")
                        .build(this)
        );
    }
}
