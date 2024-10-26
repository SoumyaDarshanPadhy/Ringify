package com.soumya.phone.services;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.CallScreeningService;

import com.soumya.phone.activities.IncomingCallActivity;
import com.soumya.phone.helpers.CallNotificationHelper;

public class CallScreening extends CallScreeningService {
    private static final String TAG = "CallScreeningService";

    @Override
    public void onScreenCall(Call.Details callDetails) {
        String phoneNumber = callDetails.getHandle().getSchemeSpecificPart();

        CallResponse.Builder response = new CallResponse.Builder();
        response.setDisallowCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false);

        respondToCall(callDetails, response.build());

        showIncomingCallUI(phoneNumber);
    }

    private void showIncomingCallUI(String phoneNumber) {
        CallNotificationHelper.removeIncomingCallNotification(this);
        CallNotificationHelper.showIncomingCallNotification(this, phoneNumber);

        Intent intent = new Intent(this, IncomingCallActivity.class);
        intent.putExtra("PHONE_NUMBER", phoneNumber);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }
}