package com.aarab.prayertimessersheng;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Legacy entry point — redirects to the new SettingsActivity.
 * Kept so any old PendingIntents or deep links don't crash.
 */
public class CitySelectionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, SettingsActivity.class));
        finish();
    }
}
