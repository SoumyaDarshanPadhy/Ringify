package com.soumya.phone.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.soumya.phone.helpers.CallNotificationHelper;

public class CallService extends Service {
    private static final String TAG = "CallService";
    private final IBinder binder = new LocalBinder();
    private TelecomManager telecomManager;
    private TelephonyManager telephonyManager;
    private CustomTelephonyCallback customTelephonyCallback;
    private CallStateListener callStateListener;
    private boolean isInitialized = false;
    private boolean isInCall = false;
    private String currentCallNumber = null;
    private long callStartTime = 0;
    private static final String ACTION_START_FOREGROUND = "START_FOREGROUND";
    private static final String ACTION_STOP_FOREGROUND = "STOP_FOREGROUND";
    private static final String ACTION_END_CALL = "END_CALL";


    public interface CallStateListener {
        void onOutgoingCallStarted(String phoneNumber);
        void onCallEnded();
        void onCallFailed(String reason);
    }

    public class LocalBinder extends Binder {
        public CallService getService() {
            return CallService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // First check permissions and initialize if needed
        if (!isInitialized && hasRequiredPermissions()) {
            initPhoneStateListener();
            isInitialized = true;
        }

        // Handle different actions
        if (intent != null) {
            String action = intent.getAction();
            String phoneNumber = intent.getStringExtra("PHONE_NUMBER");

            switch (action != null ? action : "") {
                case ACTION_START_FOREGROUND:
                    Notification notification = CallNotificationHelper
                            .createOngoingCallNotification(this, phoneNumber != null ? phoneNumber : "Unknown");
                    startForeground(CallNotificationHelper.ONGOING_CALL_NOTIFICATION_ID, notification);
                    break;

                case ACTION_STOP_FOREGROUND:
                    stopForeground(true);
                    break;

                case ACTION_END_CALL:
                    handleEndCall();
                    break;
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallStateListener(CallStateListener listener) {
        this.callStateListener = listener;
    }

    private void handleEndCall() {
        // Add your call ending logic here
        stopForeground(true);
        stopSelf();
    }

    private boolean hasRequiredPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private class CustomTelephonyCallback extends TelephonyCallback
            implements TelephonyCallback.CallStateListener {
        private boolean isCallStarted = false;

        @Override
        public void onCallStateChanged(int state) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (!isCallStarted && currentCallNumber != null) {
                        isCallStarted = true;
                        callStartTime = SystemClock.elapsedRealtime();
                        if (callStateListener != null) {
                            callStateListener.onOutgoingCallStarted(currentCallNumber);
                        }
                    }
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    if (isCallStarted) {
                        isCallStarted = false;
                        if (callStateListener != null) {
                            callStateListener.onCallEnded();
                        }
                        currentCallNumber = null;
                    }
                    break;
            }
        }
    }

    private void initPhoneStateListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Modern approach for Android 12 (API 31) and above
            customTelephonyCallback = new CustomTelephonyCallback();
            telephonyManager.registerTelephonyCallback(
                    getMainExecutor(),
                    customTelephonyCallback
            );
        } else {
            // Fallback for older versions
            PhoneStateListener phoneStateListener = new PhoneStateListener() {
                private boolean isCallStarted = false;

                @Override
                public void onCallStateChanged(int state, String phoneNumber) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    switch (state) {
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            if (!isCallStarted && currentCallNumber != null) {
                                isCallStarted = true;
                                if (callStateListener != null) {
                                    callStateListener.onOutgoingCallStarted(currentCallNumber);
                                }
                            }
                            break;

                        case TelephonyManager.CALL_STATE_IDLE:
                            if (isCallStarted) {
                                isCallStarted = false;
                                if (callStateListener != null) {
                                    callStateListener.onCallEnded();
                                }
                                currentCallNumber = null;
                            }
                            break;
                    }
                }
            };

            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    public void makeCall(String phoneNumber) {
        try {
            if (telecomManager != null &&
                    telecomManager.getDefaultDialerPackage().equals(getPackageName())) {
                phoneNumber = formatPhoneNumber(phoneNumber);
                currentCallNumber = phoneNumber;
                isInCall = true;  // Set this before making the call

                // Notify listener before making the call
                if (callStateListener != null) {
                    callStateListener.onOutgoingCallStarted(phoneNumber);
                }

                Uri uri = Uri.parse("tel:" + phoneNumber);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Bundle extras = new Bundle();
                    extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false);

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                            == PackageManager.PERMISSION_GRANTED) {
                        telecomManager.placeCall(uri, extras);
                    } else {
                        isInCall = false;  // Reset if permission denied
                        if (callStateListener != null) {
                            callStateListener.onCallFailed("CALL_PHONE permission not granted");
                        }
                    }
                } else {
                    Intent callIntent = new Intent(Intent.ACTION_CALL, uri);
                    callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(callIntent);
                }
            } else {
                if (callStateListener != null) {
                    callStateListener.onCallFailed("App is not default dialer");
                }
            }
        } catch (SecurityException e) {
            isInCall = false;  // Reset on exception
            Log.e(TAG, "Call failed due to permission issue", e);
            if (callStateListener != null) {
                callStateListener.onCallFailed("Permission denied");
            }
        } catch (Exception e) {
            isInCall = false;  // Reset on exception
            Log.e(TAG, "Call failed", e);
            if (callStateListener != null) {
                callStateListener.onCallFailed(e.getMessage());
            }
        }
    }

    public void endCall() {
        try {
            if (telecomManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS)
                            == PackageManager.PERMISSION_GRANTED) {
                        telecomManager.endCall();
                        isInCall = false;
                        currentCallNumber = null;
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to end call", e);
        }
    }

    public boolean isCallActive() {
        return isInCall;
    }

    public String getCurrentCallNumber() {
        return currentCallNumber;
    }

    public long getCallStartTime() {
        return callStartTime;
    }

    private String formatPhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("[^0-9+]", "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && customTelephonyCallback != null) {
            telephonyManager.unregisterTelephonyCallback(customTelephonyCallback);
        }
        isInCall = false;
        currentCallNumber = null;
    }
}