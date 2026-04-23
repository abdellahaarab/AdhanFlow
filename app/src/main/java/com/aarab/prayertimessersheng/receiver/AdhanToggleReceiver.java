package com.aarab.prayertimessersheng.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Stops the currently playing Adhan when the notification "إيقاف" button is tapped. */
public class AdhanToggleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (AdhanReceiver.activePlayer != null) {
            try {
                if (AdhanReceiver.activePlayer.isPlaying()) {
                    AdhanReceiver.activePlayer.stop();
                }
                AdhanReceiver.activePlayer.release();
            } catch (Exception ignored) {}
            AdhanReceiver.activePlayer = null;
        }
    }
}
