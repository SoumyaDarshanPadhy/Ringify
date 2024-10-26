//package com.soumya.phone.helpers;
//
//import android.Manifest;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//
//import androidx.core.app.ActivityCompat;
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.NotificationManagerCompat;
//
//import com.soumya.phone.MainActivity;
//import com.soumya.phone.R;
//import com.soumya.phone.activities.InCallActivity;
//import com.soumya.phone.activities.IncomingCallActivity;
//import com.soumya.phone.services.CallService;
//
//public class CallNotificationHelper {
//    private static final int INCOMING_CALL_NOTIFICATION_ID = 1;
//    private static final int ONGOING_CALL_NOTIFICATION_ID = 2;
//
//    // For incoming call notification
//    public static void showIncomingCallNotification(Context context, String phoneNumber) {
//        // Main Intent - Opens IncomingCallActivity when notification is tapped
//        Intent fullScreenIntent = new Intent(context, IncomingCallActivity.class);
//        fullScreenIntent.putExtra("PHONE_NUMBER", phoneNumber);
//        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
//                context,
//                0,
//                fullScreenIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // Intent for answer action
//        Intent answerIntent = new Intent(context, IncomingCallActivity.class);
//        answerIntent.putExtra("PHONE_NUMBER", phoneNumber);
//        answerIntent.putExtra("ACTION", "ANSWER");
//        answerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        PendingIntent answerPendingIntent = PendingIntent.getActivity(
//                context,
//                1,
//                answerIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // Intent for reject action
//        Intent rejectIntent = new Intent(context, IncomingCallActivity.class);
//        rejectIntent.putExtra("PHONE_NUMBER", phoneNumber);
//        rejectIntent.putExtra("ACTION", "REJECT");
//        rejectIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        PendingIntent rejectPendingIntent = PendingIntent.getActivity(
//                context,
//                2,
//                rejectIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // Build the notification
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_phone)
//                .setContentTitle("Incoming Call")
//                .setContentText(phoneNumber)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setCategory(NotificationCompat.CATEGORY_CALL)
//                .setFullScreenIntent(fullScreenPendingIntent, true)  // For heads-up notification
//                .setContentIntent(fullScreenPendingIntent)  // For notification tap
//                .setAutoCancel(false)
//                .setOngoing(true)
//                .addAction(R.drawable.ic_call_answer, "Answer", answerPendingIntent)
//                .addAction(R.drawable.ic_call_reject, "Reject", rejectPendingIntent);
//
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
//                == PackageManager.PERMISSION_GRANTED) {
//            notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, builder.build());
//        }
//    }
//
//    // For ongoing call notification
//    public static void showOngoingCallNotification(Context context, String phoneNumber) {
//        // Intent for opening InCallActivity
//        Intent intent = new Intent(context, InCallActivity.class);
//        intent.putExtra("PHONE_NUMBER", phoneNumber);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context,
//                3,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // Intent for ending call
//        Intent endCallIntent = new Intent(context, CallService.class);
//        endCallIntent.setAction("END_CALL");
//
//        PendingIntent endCallPendingIntent = PendingIntent.getService(
//                context,
//                4,
//                endCallIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // Build the notification
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_phone)
//                .setContentTitle("Ongoing Call")
//                .setContentText(phoneNumber)
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setCategory(NotificationCompat.CATEGORY_CALL)
//                .setOngoing(true)
//                .setAutoCancel(false)
//                .setContentIntent(pendingIntent)
//                .addAction(R.drawable.ic_call_end, "End Call", endCallPendingIntent);
//
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
//                == PackageManager.PERMISSION_GRANTED) {
//            notificationManager.notify(ONGOING_CALL_NOTIFICATION_ID, builder.build());
//        }
//    }
//
//    // Remove specific notification
//    public static void removeIncomingCallNotification(Context context) {
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//        notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID);
//    }
//
//    public static void removeOngoingCallNotification(Context context) {
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//        notificationManager.cancel(ONGOING_CALL_NOTIFICATION_ID);
//    }
//
//    // Remove all notifications
//    public static void removeAllCallNotifications(Context context) {
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//        notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID);
//        notificationManager.cancel(ONGOING_CALL_NOTIFICATION_ID);
//    }
//}
package com.soumya.phone.helpers;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.app.Notification;

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

    // For incoming call notification
    public static void showIncomingCallNotification(Context context, String phoneNumber) {
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
    }

    // For ongoing call notification - Modified to return Notification object
    public static Notification createOngoingCallNotification(Context context, String phoneNumber) {
        Intent intent = new Intent(context, InCallActivity.class);
        intent.putExtra("PHONE_NUMBER", phoneNumber);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

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
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)  // Added for better foreground service handling
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_call_end, "End Call", endCallPendingIntent)
                .build();
    }

    // Show ongoing call notification
    public static void showOngoingCallNotification(Context context, String phoneNumber) {
        Notification notification = createOngoingCallNotification(context, phoneNumber);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(ONGOING_CALL_NOTIFICATION_ID, notification);
        }
    }

    // Remove specific notification
    public static void removeIncomingCallNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(INCOMING_CALL_NOTIFICATION_ID);
    }

    public static void removeOngoingCallNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(ONGOING_CALL_NOTIFICATION_ID);
    }

    // Remove all notifications
    public static void removeAllCallNotifications(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID);
        notificationManager.cancel(ONGOING_CALL_NOTIFICATION_ID);
    }
}