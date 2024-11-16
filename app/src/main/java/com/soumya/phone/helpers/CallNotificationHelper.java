package com.soumya.phone.helpers;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.app.Notification;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.soumya.phone.MainActivity;
import com.soumya.phone.R;
import com.soumya.phone.activities.InCallActivity;
import com.soumya.phone.activities.IncomingCallActivity;
import com.soumya.phone.services.CallService;

public class CallNotificationHelper {
    public static final int INCOMING_CALL_NOTIFICATION_ID = 1;
    public static final int ONGOING_CALL_NOTIFICATION_ID = 2;

    public static Notification showIncomingCallNotification(Context context, String phoneNumber) {
        Intent fullScreenIntent = new Intent(context, IncomingCallActivity.class);
        fullScreenIntent.putExtra("PHONE_NUMBER", phoneNumber);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent answerIntent = new Intent(context, IncomingCallActivity.class);
        answerIntent.putExtra("PHONE_NUMBER", phoneNumber);
        answerIntent.putExtra("ACTION", "ANSWER");
        answerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent answerPendingIntent = PendingIntent.getActivity(
                context,
                1,
                answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent rejectIntent = new Intent(context, IncomingCallActivity.class);
        rejectIntent.putExtra("PHONE_NUMBER", phoneNumber);
        rejectIntent.putExtra("ACTION", "REJECT");
        rejectIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent rejectPendingIntent = PendingIntent.getActivity(
                context,
                2,
                rejectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_phone)
                .setContentTitle("Incoming Call")
                .setContentText(phoneNumber)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(R.drawable.ic_call_answer, "Answer", answerPendingIntent)
                .addAction(R.drawable.ic_call_reject, "Reject", rejectPendingIntent);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(INCOMING_CALL_NOTIFICATION_ID, builder.build());
        }
        return null;
    }


    public static Notification createOngoingCallNotification(Context context, String phoneNumber) {
        Intent intent = new Intent(context, InCallActivity.class);
        intent.putExtra("phoneNumber", phoneNumber);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                3,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent endCallIntent = new Intent(context, CallService.class);
        endCallIntent.setAction("END_CALL");

        PendingIntent endCallPendingIntent = PendingIntent.getService(
                context,
                4,
                endCallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_phone)
                .setContentTitle("Ongoing Call")
                .setContentText(phoneNumber)
                .setPriority(NotificationCompat.PRIORITY_HIGH)  // Changed to HIGH priority
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(pendingIntent, true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)  // Added for better foreground service handling
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_call_end, "End Call", endCallPendingIntent)
                .build();
    }

    public static void showOngoingCallNotification(Context context, String phoneNumber) {
        // Main Intent - Opens InCallActivity when notification is tapped
        Intent fullScreenIntent = new Intent(context, InCallActivity.class);
        fullScreenIntent.putExtra("phoneNumber", phoneNumber);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                3,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent for ending call
        Intent endCallIntent = new Intent(context, CallService.class);
        endCallIntent.setAction("END_CALL");

        PendingIntent endCallPendingIntent = PendingIntent.getService(
                context,
                4,
                endCallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_phone)
                .setContentTitle("Ongoing Call")
                .setContentText(phoneNumber)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)  // Added fullScreenIntent
                .setContentIntent(fullScreenPendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_call_end, "End Call", endCallPendingIntent);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(ONGOING_CALL_NOTIFICATION_ID, builder.build());
        }
    }

    public static void removeIncomingCallNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(INCOMING_CALL_NOTIFICATION_ID);
    }

    public static void removeOngoingCallNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(ONGOING_CALL_NOTIFICATION_ID);
    }

    public static void removeAllCallNotifications(Context context) {
        Log.d("CallNotificationHelper", "Removing ongoing call notification");
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID);
        notificationManager.cancel(ONGOING_CALL_NOTIFICATION_ID);
    }
}