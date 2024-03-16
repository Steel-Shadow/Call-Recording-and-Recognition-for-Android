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
import androidx.core.app.NotificationCompat;

import org.tfri.util.HttpUtil;
import org.tfri.util.MyAudioRecorder;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CallRecordingService extends AccessibilityService
        implements RecognitionListener {
    public static final String tag = CallRecordingService.class.getSimpleName();
    private MyAudioRecorder recorder;
    private Timer timer;
    private Model model;
    private SpeechStreamService speechStreamService;

    @Override
    public void onCreate() {
        super.onCreate();
        recorder = new MyAudioRecorder(getFilesDir() + "/" + tag, MyAudioRecorder.Mode.overwrite);
        Log.d(tag, tag + " create");
        initModel();

        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (timer != null) {
                            timer.cancel();
                        }
                        if (speechStreamService != null) {
                            speechStreamService.stop();
                        }
                        recorder.stop();
                        Log.d(tag, "Call State: Idle. Stop recording.");
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(tag, "Call State: Ringing");
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        recorder.start();
                        Log.d(tag, "Call State: Off-hook. Start recording.");
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                recorder.stop();
                                recognizeFile();
                                recorder.start();
                            }
                        }, 10_000, 10_000);
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
        HttpUtil.httpPost("", params, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    assert response.body() != null;
                    Log.d(tag, "Response: " + response.body().string());
                } catch (IOException e) {
                    Log.e(tag, "Error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(tag, "Error: " + e.getMessage());
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
        Notification notification = new NotificationCompat.Builder(this, tag)
                .setContentTitle(tag)
                .setContentText("record call automatically")
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
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
        recorder.release();
        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {

    }

    @Override
    public void onResult(String hypothesis) {

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

    private void recognizeFile() {
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
        } else {
            try {
                Recognizer rec = new Recognizer(model, 16000.f);

                InputStream ais = Files.newInputStream(Paths.get(getFilesDir() + "/" + tag + ".mp3"));
                if (ais.skip(44) != 44) throw new IOException("File too short");

                speechStreamService = new SpeechStreamService(rec, ais, 16000);
                speechStreamService.start(this);
            } catch (IOException e) {
                Log.e(tag, "Error: " + e.getMessage());
            }
        }
    }
}