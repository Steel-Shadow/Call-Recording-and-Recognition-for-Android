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

    /** @noinspection deprecation*/
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(tag, tag + " create");
        initModel();

        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.d(tag, "Call State: Idle. Stop recording & recognizing.");
                        if (speechService != null) {
                            speechService.stop();
                            speechService = null;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(tag, "Call State: Ringing");
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(tag, "Call State: Off-hook. Start recording & recognizing.");
                        try {
                            Recognizer rec = new Recognizer(model, 16000.0f);
                            speechService = new SpeechService(rec, 16000.0f);
                            speechService.startListening(CallRecordingService.this);
                        } catch (IOException e) {
                            Log.e(tag, "Error: " + e.getMessage());
                        }
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
        params.put("text", text);
        HttpUtil.httpPost("https://mock.apifox.com/m1/3370080-0-default/check", params, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    assert response.body() != null;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    if (jsonObject.getBoolean("bad")) {
                        NotificationHelper.showNotification(CallRecordingService.this, "1", "2");
                        VibrationHelper.vibrate(CallRecordingService.this, 100);
                    }
                } catch (Exception e) {
                    Log.e(tag, "Upload error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
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
        Intent notificationIntent = new Intent(this, VoskActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, tag)
                .setContentTitle(tag)
                .setContentText("record call automatically")
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
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }
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
            upload(text);
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