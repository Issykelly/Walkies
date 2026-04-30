package com.example.walkies.tamagotchi;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.walkies.R;

public class HungryReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "hungry_dog_channel_v2";
    private static final String TAG = "HungryReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Notification alarm received");
        
        Intent notifyIntent = new Intent(context, Tamagotchi.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notifyIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.food_icon)
                .setContentTitle("Your dog is hungry!")
                .setContentText("Your dog's hunger bar is empty. Come back and feed them!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        
        boolean canNotify = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            canNotify = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        if (canNotify && notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(1, builder.build());
            Log.d(TAG, "Notification posted successfully");
        } else {
            Log.e(TAG, "Cannot post notification: Permission=" + canNotify + ", Enabled=" + notificationManager.areNotificationsEnabled());
        }
    }
}
