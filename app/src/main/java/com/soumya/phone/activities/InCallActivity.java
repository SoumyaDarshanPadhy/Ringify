package com.soumya.phone.activities;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.soumya.phone.R;
import com.soumya.phone.helpers.CallNotificationHelper;
import com.soumya.phone.services.CallService;

public class InCallActivity extends AppCompatActivity {
    private static final String TAG = "InCallActivity";

    // UI Components
    private TextView callerNameText;
    private TextView phoneNumberText;
    private TextView callDurationText;
    private TextView callStateText;
    private ImageButton muteButton;
    private ImageButton speakerButton;
    private ImageButton endCallButton;

    // Call related variables
    private String phoneNumber;
    private long callStartTime;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private Handler durationHandler;
    private AudioManager audioManager;
    private boolean isTimerStarted = false;
    private CallService callService;

    private final Runnable durationRunnable = new Runnable() {
        @Override
        public void run() {
            updateCallDuration();
            durationHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_call);

        // Keep screen on during call
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        initializeViews();
        setupAudioManager();

        // Get CallService instance
        callService = CallService.getInstance();
        if (callService != null) {
            setupCallStateListener();
        }

        // Get data from intent
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        if (phoneNumber != null) {
            setupCallerInfo();
            CallNotificationHelper.showOngoingCallNotification(this, phoneNumber);
        } else {
            finish();
        }
    }

    private void initializeViews() {
        callerNameText = findViewById(R.id.callerNameText);
        phoneNumberText = findViewById(R.id.phoneNumberText);
        callDurationText = findViewById(R.id.callDurationText);
        callStateText = findViewById(R.id.callStateText);
        muteButton = findViewById(R.id.muteButton);
        speakerButton = findViewById(R.id.speakerButton);
        endCallButton = findViewById(R.id.endCallButton);

        // Setup button click listeners
        muteButton.setOnClickListener(v -> toggleMute());
        speakerButton.setOnClickListener(v -> toggleSpeaker());
        endCallButton.setOnClickListener(v -> endCall());
    }

    private void setupCallStateListener() {
        callService.setCallStateListener(new CallService.CallStateListener() {
            @Override
            public void onIncomingCall(String number) {
                // Not needed here - handled by IncomingCallActivity
            }

            @Override
            public void onOutgoingCallStarted(String number) {
                runOnUiThread(() -> {
                    callStateText.setText("Calling...");
                    if (!isTimerStarted) {
                        startCallDurationTimer();
                        isTimerStarted = true;
                    }
                });
            }

            @Override
            public void onCallAnswered(String number) {
                runOnUiThread(() -> {
                    callStateText.setText("Connected");
                    if (!isTimerStarted) {
                        startCallDurationTimer();
                        isTimerStarted = true;
                    }
                });
            }

            @Override
            public void onCallEnded() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Call ended callback received");
                    finishAndCleanup();
                });
            }

            @Override
            public void onCallFailed(String reason) {
                runOnUiThread(() -> {
                    stopCallDurationTimer();
                    Toast.makeText(InCallActivity.this,
                            "Call failed: " + reason, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onCallRejected() {
                runOnUiThread(() -> {
                    stopCallDurationTimer();
                    finish();
                });
            }
        });
    }

    private void setupAudioManager() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    private void setupCallerInfo() {
        phoneNumberText.setText(phoneNumber);

        // Look up contact information
        String[] projection = new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI
        };

        try (Cursor cursor = getContentResolver().query(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                        .appendPath(phoneNumber).build(),
                projection, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int nameColumnIndex = cursor.getColumnIndex(
                        ContactsContract.PhoneLookup.DISPLAY_NAME);

                if (nameColumnIndex != -1) {
                    String name = cursor.getString(nameColumnIndex);
                    callerNameText.setText(name != null ? name : "Unknown Caller");
                } else {
                    callerNameText.setText("Unknown Caller");
                }
            } else {
                callerNameText.setText("Unknown Caller");
            }
        } catch (Exception e) {
            callerNameText.setText("Unknown Caller");
            e.printStackTrace();
        }
    }

    private void startCallDurationTimer() {
        callStartTime = SystemClock.elapsedRealtime();
        durationHandler = new Handler();
        durationHandler.post(durationRunnable);
    }

    private void updateCallDuration() {
        long duration = SystemClock.elapsedRealtime() - callStartTime;
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        String durationText = String.format("%02d:%02d", minutes, seconds);
        callDurationText.setText(durationText);
    }

    private void stopCallDurationTimer() {
        if (durationHandler != null) {
            durationHandler.removeCallbacks(durationRunnable);
        }
    }

    private void toggleMute() {
        isMuted = !isMuted;
        audioManager.setMicrophoneMute(isMuted);
        muteButton.setImageResource(isMuted ?
                R.drawable.ic_mic_off : R.drawable.ic_mic);
        Toast.makeText(this, isMuted ? "Muted" : "Unmuted",
                Toast.LENGTH_SHORT).show();
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        audioManager.setSpeakerphoneOn(isSpeakerOn);
        speakerButton.setImageResource(isSpeakerOn ?
                R.drawable.ic_speaker_on : R.drawable.ic_speaker);
        Toast.makeText(this, isSpeakerOn ? "Speaker On" : "Speaker Off",
                Toast.LENGTH_SHORT).show();
    }

    private void endCall() {
        if (callService != null) {
            callService.endCall();
            CallNotificationHelper.removeOngoingCallNotification(this);
        }
        finish();
    }

    private void finishAndCleanup() {
        stopCallDurationTimer();
        if (audioManager != null) {
            audioManager.setMicrophoneMute(false);
            audioManager.setSpeakerphoneOn(false);
        }
        CallNotificationHelper.removeOngoingCallNotification(getApplicationContext());
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (callService != null && !callService.isCallActive()) {
            finishAndCleanup();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCallDurationTimer();

        // Reset audio settings
        if (audioManager != null) {
            audioManager.setMicrophoneMute(false);
            audioManager.setSpeakerphoneOn(false);
        }

        CallNotificationHelper.removeOngoingCallNotification(this);
    }

    @Override
    public void onBackPressed() {
        // Disable back button during call
        // super.onBackPressed(); // Remove this if you want to completely disable back button
        super.onBackPressed();
    }
}