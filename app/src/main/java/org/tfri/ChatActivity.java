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
    private List<List<String>> history;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String response = intent.getStringExtra(CallRecordingService.EXTRA_STRING);
            addChat(response, ChatContent.Type.RECEIVE);
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
        editText = findViewById(R.id.Main_etContent);
        btnSend = findViewById(R.id.Main_btnSend);
        openSettings = findViewById(R.id.open_settings);
        clear = findViewById(R.id.clear);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void initData() {
        getPermissions();

        btnSend.setOnClickListener(this);
        openSettings.setOnClickListener(view -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        clear.setOnClickListener(view -> {
            chatContents.clear();
            history.clear();
            chatAdapter.notifyDataSetChanged();
        });

        chatContents = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatContents);
        rViewChat.setAdapter(chatAdapter);
        rViewChat.setLayoutManager(new LinearLayoutManager(this));
        history = new ArrayList<>();

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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.Main_btnSend) {
            addChat(editText.getText().toString(), ChatContent.Type.SEND);
            sendMessageToLLM(editText.getText().toString());
            editText.setText("");
        }
    }

    private void addChat(String chatText, ChatContent.Type type) {
        chatContents.add(new ChatContent(chatText, type));
        chatAdapter.notifyItemInserted(chatContents.size() - 1);
        rViewChat.smoothScrollToPosition(chatContents.size() - 1);

        if (type == ChatContent.Type.SEND) {
            List<String> pair = new ArrayList<>();
            pair.add(editText.getText().toString());
            pair.add("");
            history.add(pair);
        } else if (type == ChatContent.Type.RECEIVE) {
            if (!history.isEmpty()) {
                history.get(history.size() - 1).set(1, chatText);
            } else {
                List<String> pair = new ArrayList<>();
                pair.add("");
                pair.add(editText.getText().toString());
                history.add(pair);
            }
        }
    }

    private void sendMessageToLLM(String mes) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("prompt", mes);
        params.put("history", history);
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
}