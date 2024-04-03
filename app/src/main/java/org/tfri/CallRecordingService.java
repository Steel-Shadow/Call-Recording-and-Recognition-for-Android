package org.tfri;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.tfri.util.HttpUtil;
import org.tfri.util.NotificationHelper;
import org.tfri.util.VibrationHelper;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CallRecordingService extends AccessibilityService
        implements RecognitionListener {
    private static final String tag = CallRecordingService.class.getSimpleName();
    private static final int NOTIFICATION_ID_SERVICE = 2;
    private String text = "";
    private Model model;
    private SpeechService speechService;
    private Timer timer;
    public static final String ACTION_SEND_STRING = CallRecordingService.class.getName() + ".SEND_STRING";
    public static final String EXTRA_STRING_USER = CallRecordingService.class.getName() + "user";
    public static final String EXTRA_STRING_BOT = CallRecordingService.class.getName() + "bot";

    private void sendMesToChat(String user, String bot) {
        Log.i(tag, "Send message to chat: " + bot + " " + user);

        Intent intent = new Intent();
        intent.setAction(ACTION_SEND_STRING)
                .putExtra(EXTRA_STRING_BOT, bot)
                .putExtra(EXTRA_STRING_USER, user)
                .setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void startRecording() {
        try {
            Recognizer rec = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(rec, 16000.0f);
            speechService.startListening(CallRecordingService.this);
        } catch (IOException e) {
            Log.e(tag, "Error: " + e.getMessage());
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                upload(text);
            }
        }, 10000, 10000); // 每隔10秒执行一次
    }

    private void stopRecording() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    // @noinspection deprecation
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(tag, tag + " create");
        initModel();

//        upload("测试：通话开始");
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.d(tag, "Call State: Idle. Stop recording & recognizing.");
                        stopRecording();
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(tag, "Call State: Ringing");
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(tag, "Call State: Off-hook. Start recording & recognizing.");
                        startRecording();
                        break;
                }
            }
        };
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        startForegroundService();
    }

    private void initModel() {
        StorageService.unpack(this, "model-cn", "model",
                (model) -> this.model = model,
                (exception) -> Log.e(tag, "Failed to unpack the model" + exception.getMessage()));
    }

    private void upload(String text) {
        HashMap<String, String> params = new HashMap<>();
        params.put("prompt", text);
        HttpUtil.httpPost(HttpUtil.send, params, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    assert response.body() != null;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    Log.d(tag, "Upload response: " + jsonObject);
                    if (jsonObject.getBoolean("judge")) {
                        NotificationHelper.showNotification(CallRecordingService.this, "通话检测异常", "可能为电信诈骗");
                        VibrationHelper.vibrate(CallRecordingService.this, 100);

                        String user = jsonObject.getJSONArray("history").getJSONArray(0).getString(0);
                        sendMesToChat(user, jsonObject.getString("response"));
                        stopRecording();
                    }
                } catch (Exception e) {
                    Log.e(tag, "Upload error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                sendMesToChat("", "网络请求失败，请检查网络连接");
                Log.e(tag, "Upload fail: " + e.getMessage());
            }
        });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
//        Log.e(logTag, "Event :" + event.getEventType());
    }


    @Override
    public void onInterrupt() {
        Log.d(tag, "Interrupted");
    }

    private void startForegroundService() {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, ChatActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, tag)
                .setContentTitle(tag)
                .setContentText("通话检测服务正在运行")
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID_SERVICE, notification);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                tag,
                "Recording Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @Override
    public void onPartialResult(String hypothesis) {

    }

    /**
     * @param hypothesis 从Vosk识别出的文本
     * @apiNote 上传文本到服务器，返回是否为电信诈骗信息
     */
    @Override
    public void onResult(String hypothesis) {
        try {
            JSONObject jsonObject = new JSONObject(hypothesis);
            String temp = jsonObject.getString("text");
            text += temp.isEmpty() ? "" : temp + "\n";
            Log.d(tag, "Rec result up to now: \n" + text);
        } catch (JSONException e) {
            Log.e(tag, "JSON Error: " + e.getMessage());
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
        Log.d(tag, "Final result: " + hypothesis);
        // upload(hypothesis);
    }

    @Override
    public void onError(Exception exception) {
        Log.e(tag, "Error: " + exception.getMessage());
    }

    @Override
    public void onTimeout() {
        Log.e(tag, "Timeout");
    }

}