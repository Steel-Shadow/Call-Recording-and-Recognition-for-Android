package org.tfri;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;
import org.tfri.base.BaseActivity;
import org.tfri.data.ChatContent;
import org.tfri.util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChatActivity extends BaseActivity implements View.OnClickListener {
    private static final String tag = ChatActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS = 200;
    private RecyclerView rViewChat;
    private EditText editText;
    private Button btnSend;
    private Button openSettings;
    private Button clear;
    private ChatAdapter chatAdapter;
    private List<ChatContent> chatContents;
    private boolean hideFirstUser;
    private String hidedFirstUserMes = "";
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideFirstUser = true;
            hidedFirstUserMes = intent.getStringExtra(CallRecordingService.EXTRA_STRING_USER);
            // addChat(user, ChatContent.Type.SEND);

            String bot = intent.getStringExtra(CallRecordingService.EXTRA_STRING_BOT);
            addChat(bot, ChatContent.Type.RECEIVE);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.chat_activity;
    }

    @Override
    protected void initView() {
        rViewChat = findViewById(R.id.recycler_view);
        editText = findViewById(R.id.edit_mes);
        btnSend = findViewById(R.id.btn_send_mes);
        openSettings = findViewById(R.id.open_settings);
        clear = findViewById(R.id.clear);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void initData() {
        getPermissions();

        btnSend.setOnClickListener(v -> {
            sendMessageToLLM(editText.getText().toString());
            addChat(editText.getText().toString(), ChatContent.Type.SEND);
            editText.setText("");
        });
        openSettings.setOnClickListener(view -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        clear.setOnClickListener(view -> {
            chatContents.clear();
            chatAdapter.notifyDataSetChanged();
            hideFirstUser = false;
            hidedFirstUserMes = "";
        });

        chatContents = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatContents);
        rViewChat.setAdapter(chatAdapter);
        rViewChat.setLayoutManager(new LinearLayoutManager(this));

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, new IntentFilter(CallRecordingService.ACTION_SEND_STRING), RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, new IntentFilter(CallRecordingService.ACTION_SEND_STRING));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void getPermissions() {
        // request permissions
        PackageManager packageManager = getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(tag, "Package name not found", e);
        }
        assert packageInfo != null;
        String[] permissions = packageInfo.requestedPermissions;
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permissionsAccepted = false;
        for (int i = 0; i < permissions.length; i++) {
            if (requestCode == REQUEST_PERMISSIONS) {
                permissionsAccepted = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
            if (!permissionsAccepted) {
                Log.e(tag, "Permission denied: " + permissions[i]);
//                finish();
            } else {
                Log.d(tag, "Permission granted: " + permissions[i]);
            }
        }
    }

    private void addChat(String chatText, ChatContent.Type type) {
        chatContents.add(new ChatContent(chatText, type));
        chatAdapter.notifyItemInserted(chatContents.size() - 1);
        rViewChat.smoothScrollToPosition(chatContents.size() - 1);
    }

    private void sendMessageToLLM(String mes) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("prompt", mes);
        params.put("history", getHistory());

        HttpUtil.httpPostForObject(HttpUtil.chat, params, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    assert response.body() != null;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    String reply = jsonObject.getString("response");
                    runOnUiThread(() -> addChat(reply, ChatContent.Type.RECEIVE));
                } catch (JSONException | IOException e) {
                    Log.e(tag, "Failed to send message to LLM: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> addChat("Failed to send message", ChatContent.Type.RECEIVE));
            }
        });
    }

    @NonNull
    private List<List<String>> getHistory() {
        List<List<String>> history = new ArrayList<>();
        if (hideFirstUser) {
            List<String> firstPair = new ArrayList<>();
            firstPair.add(hidedFirstUserMes);
            firstPair.add(chatContents.get(0).getContent());
            history.add(firstPair);
        }

        for (int i = hideFirstUser ? 1 : 0; i < chatContents.size() - 1; i += 2) {
            List<String> tempPair = new ArrayList<>();
            ChatContent user = chatContents.get(i);
            ChatContent bot = chatContents.get(i + 1);
            tempPair.add(user.getContent());
            tempPair.add(bot.getContent());
            history.add(tempPair);
        }
        return history;
    }
}