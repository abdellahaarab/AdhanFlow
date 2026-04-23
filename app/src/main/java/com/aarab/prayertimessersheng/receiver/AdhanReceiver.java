package com.aarab.prayertimessersheng.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.aarab.prayertimessersheng.MainActivity;
import com.aarab.prayertimessersheng.R;

public class AdhanReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "adhan_channel";
    public static MediaPlayer activePlayer;   // kept static so toggle receiver can stop it

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerName = intent.getStringExtra("prayer_name");
        String prayerKey  = intent.getStringExtra("prayer_key");
        if (prayerName == null) prayerName = "الصلاة";

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        // Global enable/disable
        if (!prefs.getBoolean("adhan_enabled", true)) return;

        // Per-prayer enable flag
        if (prayerKey != null && !prefs.getBoolean("adhan_" + prayerKey + "_enabled", true)) return;

        // ── Play Adhan ───────────────────────────────────────────────────────
        try {
            if (activePlayer != null) { activePlayer.release(); activePlayer = null; }
            activePlayer = MediaPlayer.create(context, R.raw.adhan);
            if (activePlayer != null) {
                float vol = prefs.getInt("adhan_volume", 100) / 100f;
                activePlayer.setVolume(vol, vol);
                activePlayer.setOnCompletionListener(mp -> mp.release());
                activePlayer.start();
            }
        } catch (Exception e) { e.printStackTrace(); }

        // ── Notification ─────────────────────────────────────────────────────
        ensureChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Stop adhan action
        Intent stopIntent = new Intent(context, AdhanToggleReceiver.class);
        PendingIntent stopPi = PendingIntent.getBroadcast(context, 9900, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String finalPrayerName = prayerName;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle("حان وقت " + finalPrayerName)
                .setContentText("اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ")
                .addAction(R.drawable.ic_off, "إيقاف", stopPi)
                .setContentIntent(openPi)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(prayerName.hashCode(), builder.build());
    }

    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "إشعارات الأذان", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("إشعارات مواقيت الصلاة");
            ch.enableLights(true);
            ch.enableVibration(true);
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
