# Call Recording and Recognition for Android

This demo implements call recording and offline speech recognition for mobile applications.

## call recording

The demo use `AccessibilityService` and `MediaRecorder(VOICE_RECOGNITION)` to record call voice.

## voice recognition

Get the call recognition result in `org.tfri.CallRecordingService.onFinalResult()`.

## language model

In models folder, you can find the pre-trained Vosk models.

The demo use Chinese Model (English one installed but not use in code).

If you want to use other languages, you can download models
from [Vosk Website](https://alphacephei.com/vosk/models) and add them in `models` folder. Then global search `model-cn` in the project and replace it with your new model name.

## Vosk Documentation

For details about the language model, please visit the [Vosk Website](https://alphacephei.com/vosk/android).
