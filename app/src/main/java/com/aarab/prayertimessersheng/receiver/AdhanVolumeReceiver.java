package com.aarab.prayertimessersheng.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Adjusts Adhan volume by ±10 % when user taps vol-up / vol-down in notification. */
public class AdhanVolumeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean up = intent.getBooleanExtra("volume_up", true);
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        int vol = prefs.getInt("adhan_volume", 100);
        vol = up ? Math.min(100, vol + 10) : Math.max(0, vol - 10);
        prefs.edit().putInt("adhan_volume", vol).apply();

        // Apply to currently playing player if any
        if (AdhanReceiver.activePlayer != null) {
            try {
                float v = vol / 100f;
                AdhanReceiver.activePlayer.setVolume(v, v);
            } catch (Exception ignored) {}
        }
    }
}
