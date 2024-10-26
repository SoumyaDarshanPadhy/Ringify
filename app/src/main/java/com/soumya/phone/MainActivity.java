package com.soumya.phone;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.soumya.phone.activities.InCallActivity;
import com.soumya.phone.services.CallService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ID = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.WAKE_LOCK
    };

    public static final String CHANNEL_ID = "ongoing_call_channel";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 101;

    // UI Components
    private TextView phoneNumberText;
    private ImageButton backspaceButton;
    private FloatingActionButton callButton;
    private View bottomNavigation;
    private StringBuilder phoneNumber;

    // Service related
    private CallService callService;
    private boolean serviceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CallService.LocalBinder binder = (CallService.LocalBinder) service;
            callService = binder.getService();
            serviceBound = true;
            setupCallService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        phoneNumber = new StringBuilder();

        if (hasAllPermissions()) {
            startAndBindCallService();
            requestDefaultDialerRole();
        } else {
            requestPermissions();
        }

        createNotificationChannel();
        requestNotificationPermission();
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ongoing Call",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Shows when a call is in progress");

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);

            notificationManager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkActiveCall();
    }

    private void checkActiveCall() {
        if (serviceBound && callService != null) {
            if (callService.isCallActive()) {
                String number = callService.getCurrentCallNumber();
                if (number != null) {
                    launchInCallActivity(number);
                }
            }
        }
    }

    private void initializeViews() {
        phoneNumberText = findViewById(R.id.phoneNumberText);
        backspaceButton = findViewById(R.id.backspaceButton);
        callButton = findViewById(R.id.callButton);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Initialize dialpad buttons
        int[] buttonIds = {
                R.id.button0, R.id.button1, R.id.button2, R.id.button3,
                R.id.button4, R.id.button5, R.id.button6, R.id.button7,
                R.id.button8, R.id.button9
        };

        for (int id : buttonIds) {
            Button button = findViewById(id);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }
    }

    private void setupClickListeners() {
        // Backspace button click and long click
        backspaceButton.setOnClickListener(v -> {
            if (phoneNumber.length() > 0) {
                phoneNumber.deleteCharAt(phoneNumber.length() - 1);
                updatePhoneNumberDisplay();
            }
        });

        backspaceButton.setOnLongClickListener(v -> {
            phoneNumber.setLength(0);
            updatePhoneNumberDisplay();
            return true;
        });

        // Call button
        callButton.setOnClickListener(v -> initiateCall());

        // Bottom navigation click listeners
//        if (bottomNavigation != null) {
//            View contactsButton = bottomNavigation.findViewById(R.id.contactsTab);
//            View recentsButton = bottomNavigation.findViewById(R.id.recentsTab);
//
//            if (contactsButton != null) {
//                contactsButton.setOnClickListener(v -> {
//                    Intent intent = new Intent(this, ContactsActivity.class);
//                    startActivity(intent);
//                });
//            }
//
//            if (recentsButton != null) {
//                recentsButton.setOnClickListener(v -> {
//                    Intent intent = new Intent(this, RecentsActivity.class);
//                    startActivity(intent);
//                });
//            }
//        }
    }

    private void startAndBindCallService() {
        Intent intent = new Intent(this, CallService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupCallService() {
        if (serviceBound && callService != null) {
            callService.setCallStateListener(new CallService.CallStateListener() {
                @Override
                public void onOutgoingCallStarted(String number) {
                    launchInCallActivity(number);
//                    finish();
                }

                @Override
                public void onCallEnded() {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Call ended",
                                    Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onCallFailed(String reason) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Call failed: " + reason,
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v instanceof Button) {
            String digit = ((Button) v).getText().toString();
            phoneNumber.append(digit);
            updatePhoneNumberDisplay();
        }
    }

    private void updatePhoneNumberDisplay() {
        phoneNumberText.setText(phoneNumber.toString());
    }

    private void initiateCall() {
        if (phoneNumber.length() > 0) {
            if (hasAllPermissions()) {
                if (serviceBound && callService != null) {
                    if (!callService.isCallActive()) {
                        String number = phoneNumber.toString();
                        // Launch InCallActivity first
                        launchInCallActivity(number);
                        // Then make the call
                        callService.makeCall(number);
                    } else {
                        Toast.makeText(this, "Call already in progress",
                                Toast.LENGTH_SHORT).show();
                        launchInCallActivity(callService.getCurrentCallNumber());
                    }
                } else {
                    Toast.makeText(this, "Service not bound",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                requestPermissions();
            }
        } else {
            Toast.makeText(this, "Please enter a phone number",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void launchInCallActivity(String phoneNumber) {
        runOnUiThread(() -> {
            Intent inCallIntent = new Intent(this, InCallActivity.class);
            inCallIntent.putExtra("phoneNumber", phoneNumber);
            inCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(inCallIntent);
        });
    }

    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    private void requestDefaultDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
                    startActivityForResult(intent, REQUEST_ID);
                }
            }
        } else { // API level below 29
            TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
            if (telecomManager != null && !getPackageName().equals(telecomManager.getDefaultDialerPackage())) {
                Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
                intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, getPackageName());
                startActivityForResult(intent, REQUEST_ID);
            } else {
                // App is already the default dialer
                Toast.makeText(this, "Your app is already the default dialer", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ID) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Successfully set as default dialer",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to set as default dialer",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasAllPermissions()) {
                startAndBindCallService();
                requestDefaultDialerRole();
            } else {
                showPermissionExplanationDialog();
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app needs phone and contacts permissions to function as your default dialer.")
                .setPositiveButton("Grant Permissions", (dialog, which) -> requestPermissions())
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}