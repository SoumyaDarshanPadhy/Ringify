package com.soumya.phone.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.soumya.phone.R;
import com.soumya.phone.helpers.CallNotificationHelper;

public class IncomingCallActivity extends AppCompatActivity {
    private String phoneNumber;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        // Keep screen on and show above lock screen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        // Acquire wake lock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "app:IncomingCallLock"
        );
        wakeLock.acquire(10*60*1000L /*10 minutes*/);

        phoneNumber = getIntent().getStringExtra("PHONE_NUMBER");

        updatePhoneNumberUI();

        // Handle intent
        handleIntent(getIntent());

        // Setup buttons
        setupButtons();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            phoneNumber = intent.getStringExtra("PHONE_NUMBER");
            String action = intent.getStringExtra("ACTION");

            updatePhoneNumberUI();

            if (action != null) {
                switch (action) {
                    case "ANSWER":
                        answerCall();
                        break;
                    case "REJECT":
                        rejectCall();
                        break;
                }
            }
        }
    }

    private void setupButtons() {
        ImageButton answerButton = findViewById(R.id.answerButton);
        ImageButton rejectButton = findViewById(R.id.rejectButton);

        answerButton.setOnClickListener(v -> answerCall());
        rejectButton.setOnClickListener(v -> rejectCall());
    }

    private void updatePhoneNumberUI() {
        if (phoneNumber != null) {
            TextView phoneNumberTextView = findViewById(R.id.phoneNumberTextView);
            TextView formattedNumberTextView = findViewById(R.id.formattedNumberTextView);

            // Display original number
            phoneNumberTextView.setText(phoneNumber);

            // Display formatted number
            String formattedNumber = formatPhoneNumber(phoneNumber);
            if (!formattedNumber.equals(phoneNumber)) {
                formattedNumberTextView.setText(formattedNumber);
                formattedNumberTextView.setVisibility(View.VISIBLE);
            } else {
                formattedNumberTextView.setVisibility(View.GONE);
            }

            // Try to get contact name if available
            String contactName = getContactName(phoneNumber);
            if (contactName != null) {
                phoneNumberTextView.setText(contactName);
                formattedNumberTextView.setText(formattedNumber);
                formattedNumberTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    private String formatPhoneNumber(String number) {
        if (number == null) return "";

        // Remove any non-digit characters except +
        String cleaned = number.replaceAll("[^\\d+]", "");

        // If it's an international number starting with +
        if (cleaned.startsWith("+")) {
            if (cleaned.length() > 12) {
                // Format international number: +XX XXX XXX XXXX
                return cleaned.substring(0, 3) + " " +
                        cleaned.substring(3, 6) + " " +
                        cleaned.substring(6, 9) + " " +
                        cleaned.substring(9);
            }
            return cleaned;
        }

        // For local numbers (assuming US format)
        if (cleaned.length() == 10) {
            return "(" + cleaned.substring(0, 3) + ") " +
                    cleaned.substring(3, 6) + "-" +
                    cleaned.substring(6);
        }

        return number; // Return original if no formatting applies
    }

    private String getContactName(String phoneNumber) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber));
            String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};

            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void answerCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null &&
                    checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS)
                            == PackageManager.PERMISSION_GRANTED) {
                try {
                    telecomManager.acceptRingingCall();
                    CallNotificationHelper.removeIncomingCallNotification(this);
                    CallNotificationHelper.showOngoingCallNotification(this, phoneNumber);

                    // Launch InCallActivity
                    Intent inCallIntent = new Intent(this, InCallActivity.class);
                    inCallIntent.putExtra("phoneNumber", phoneNumber);
                    inCallIntent.putExtra("isIncomingCall", true);
                    startActivity(inCallIntent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to answer call", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void rejectCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null &&
                    checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS)
                            == PackageManager.PERMISSION_GRANTED) {
                try {
                    telecomManager.endCall();
                    CallNotificationHelper.removeIncomingCallNotification(this);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to reject call", Toast.LENGTH_SHORT).show();
                }
            }
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from closing the activity
        super.onBackPressed();
        Toast.makeText(this, "Please answer or reject the call", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}