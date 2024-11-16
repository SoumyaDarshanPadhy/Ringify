package com.soumya.phone.services;

import android.content.Context;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.DisconnectCause;
import android.telecom.InCallService;
import android.telecom.TelecomManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import com.soumya.phone.helpers.CallNotificationHelper;

public class CallService extends InCallService {
    private static final String TAG = "CallService";
    private TelecomManager telecomManager;
    private Call currentCall;
    private Call.Callback callCallback;
    private CallStateListener callStateListener;
    private long callStartTime = 0;
    private static CallService instance;

    public boolean isCallActive() {
        if (currentCall != null) {
            int state = currentCall.getState();
            return state == Call.STATE_DIALING ||
                    state == Call.STATE_RINGING ||
                    state == Call.STATE_CONNECTING ||
                    state == Call.STATE_ACTIVE;
        }
        return false;
    }

    public String getCurrentCallNumber() {
        if (currentCall != null && currentCall.getDetails().getHandle() != null) {
            // The number will be in the format "tel:1234567890"
            // We need to remove the "tel:" prefix
            String number = currentCall.getDetails().getHandle().getSchemeSpecificPart();
            return number != null ? number.replace("tel:", "") : null;
        }
        return null;
    }

    public interface CallStateListener {
        void onIncomingCall(String phoneNumber);
        void onOutgoingCallStarted(String phoneNumber);
        void onCallAnswered(String phoneNumber);
        void onCallEnded();
        void onCallFailed(String reason);
        void onCallRejected();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
    }

    public static CallService getInstance() {
        return instance;
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        currentCall = call;

        callCallback = new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                handleCallStateChange(call, state);
            }
        };

        call.registerCallback(callCallback);
        handleCallStateChange(call, call.getState());
    }

    private void handleCallStateChange(Call call, int state) {
        String phoneNumber = call.getDetails().getHandle() != null ?
                call.getDetails().getHandle().getSchemeSpecificPart() : "Unknown";

        Log.d(TAG, "Call state changed to: " + getCallStateString(state) +
                " for number: " + phoneNumber +
                " Direction: " + (call.getDetails().getCallDirection() == Call.Details.DIRECTION_INCOMING ? "Incoming" : "Outgoing"));

        switch (state) {
            case Call.STATE_RINGING:
                // Incoming call is ringing
                if (callStateListener != null) {
                    callStateListener.onIncomingCall(phoneNumber);
                }
                startForeground(
                        CallNotificationHelper.ONGOING_CALL_NOTIFICATION_ID,
                        CallNotificationHelper.showIncomingCallNotification(this, phoneNumber)
                );
                break;

            case Call.STATE_DIALING:
            case Call.STATE_CONNECTING:
                // Outgoing call is being placed
                if (callStateListener != null) {
                    callStateListener.onOutgoingCallStarted(phoneNumber);
                }
                startForeground(
                        CallNotificationHelper.ONGOING_CALL_NOTIFICATION_ID,
                        CallNotificationHelper.createOngoingCallNotification(this, phoneNumber)
                );
                break;

            case Call.STATE_ACTIVE:
                // Call is connected (either incoming or outgoing)
                callStartTime = System.currentTimeMillis();
                if (callStateListener != null) {
                    callStateListener.onCallAnswered(phoneNumber);
                }
                updateNotificationForActiveCall(phoneNumber);
                break;

            case Call.STATE_DISCONNECTING:
            case Call.STATE_DISCONNECTED:
                // Call has ended
                DisconnectCause disconnectCause = call.getDetails().getDisconnectCause();
                if (disconnectCause != null) {
                    if (disconnectCause.getCode() == DisconnectCause.REJECTED) {
                        if (callStateListener != null) {
                            callStateListener.onCallRejected();
                        }
                    } else if (disconnectCause.getCode() == DisconnectCause.ERROR) {
                        if (callStateListener != null) {
                            callStateListener.onCallFailed((String) disconnectCause.getLabel());
                        }
                    } else {
                        if (callStateListener != null) {
                            callStateListener.onCallEnded();
                        }
                    }
                } else {
                    if (callStateListener != null) {
                        callStateListener.onCallEnded();
                    }
                }
                cleanup();
                break;



        }
    }

    private void updateNotificationForActiveCall(String phoneNumber) {
        startForeground(
                CallNotificationHelper.ONGOING_CALL_NOTIFICATION_ID,
                CallNotificationHelper.createOngoingCallNotification(this, phoneNumber)
        );
    }

    private String getCallStateString(int state) {
        switch (state) {
            case Call.STATE_NEW: return "NEW";
            case Call.STATE_RINGING: return "RINGING";
            case Call.STATE_DIALING: return "DIALING";
            case Call.STATE_ACTIVE: return "ACTIVE";
            case Call.STATE_HOLDING: return "HOLDING";
            case Call.STATE_DISCONNECTED: return "DISCONNECTED";
            case Call.STATE_CONNECTING: return "CONNECTING";
            case Call.STATE_DISCONNECTING: return "DISCONNECTING";
            case Call.STATE_SELECT_PHONE_ACCOUNT: return "SELECT_PHONE_ACCOUNT";
            case Call.STATE_SIMULATED_RINGING: return "SIMULATED_RINGING";
            case Call.STATE_AUDIO_PROCESSING: return "AUDIO_PROCESSING";
            default: return "UNKNOWN";
        }
    }

    private void cleanup() {
        if (currentCall != null && callCallback != null) {
            currentCall.unregisterCallback(callCallback);
        }
        CallNotificationHelper.removeOngoingCallNotification(this);
        stopForeground(true);
        currentCall = null;
        callStartTime = 0;
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        if (call != null && callCallback != null) {
            call.unregisterCallback(callCallback);
        }
        cleanup();
    }

    public void setCallStateListener(CallStateListener listener) {
        this.callStateListener = listener;
    }

    public void makeCall(String phoneNumber) {
        if (telecomManager == null || !telecomManager.getDefaultDialerPackage().equals(getPackageName())) {
            if (callStateListener != null) {
                callStateListener.onCallFailed("App is not default dialer");
            }
            return;
        }

        try {
            phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");
            Uri uri = Uri.parse("tel:" + phoneNumber);

            Bundle extras = new Bundle();
            extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                telecomManager.placeCall(uri, extras);
            } else {
                if (callStateListener != null) {
                    callStateListener.onCallFailed("CALL_PHONE permission not granted");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Call failed", e);
            if (callStateListener != null) {
                callStateListener.onCallFailed(e.getMessage());
            }
        }
    }

    public void answerCall() {
        if (currentCall != null) {
            currentCall.answer(currentCall.getDetails().getVideoState());
        }
    }

    public void rejectCall() {
        if (currentCall != null) {
            currentCall.reject(false, null);
        }
    }

    public void endCall() {
        if (currentCall != null) {
            currentCall.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        cleanup();
        instance = null;
        super.onDestroy();
    }
}