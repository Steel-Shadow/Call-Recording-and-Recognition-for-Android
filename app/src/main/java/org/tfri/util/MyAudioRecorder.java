package org.tfri.util;

import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MyAudioRecorder {
    private final MediaRecorder recorder = new MediaRecorder();
    private final String logTag = MyAudioRecorder.class.getSimpleName();
    private String path;
    public boolean isRecording = false;

    public enum Mode {
        rename,
        overwrite
    }

    private final Mode mode;

    /**
     * don't use file extension .mp3/.mp4 in constructor
     */
    public MyAudioRecorder(@NonNull String path, Mode mode) {
        path += ".mp4";
        this.path = path;
        this.mode = mode;
    }

    /**
     * Starts a new recording.
     */
    public void start() {
        if (mode == Mode.rename)
            path = rename(path);

        File file = new File(path);

        // make sure the directory we plan to store the recording in exists
        File directory = file.getParentFile();
        assert directory != null;
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(logTag, "Path to file could not be created.");
        }

        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioSamplingRate(16000);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(file.getAbsolutePath());
        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            Log.e(logTag, e.toString());
        }

        isRecording = true;
    }

    private String rename(String path) {
        File file = new File(path.replace(".mp4", ".mp3"));
        if (file.exists()) {
            // rename file with current time
            LocalDateTime currentTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
            String currentTimeString = currentTime.format(formatter);

            String regex = "_\\d{4}-\\d{2}-\\d{2}_\\d{2}:\\d{2}:\\d{2}\\.mp4";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                return matcher.replaceFirst("_" + currentTimeString + ".mp4");
            } else {
                return path.replace(".mp4", "_" + currentTimeString + ".mp4");
            }
        }
        return path;
    }

    /**
     * Stops a recording that has been previously started.
     */
    public void stop() {
        if (!isRecording) {
            return;
        }
        recorder.stop();
        isRecording = false;

        // convert .mp4 to .mp3
        try {
            AudioExtractor.genVideoUsingMuxer(path, path.replace(".mp4", ".mp3"), -1, -1, true, false);
        } catch (Exception e) {
            Log.e(logTag, e.toString());
        }

        // remove .mp4
        if (!new File(path).delete()) {
            Log.e(logTag, "Failed to delete .mp4: " + path);
        }
    }

    public void release() {
        recorder.release();
    }
}
