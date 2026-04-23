package com.aarab.prayertimessersheng;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aarab.prayertimessersheng.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding b;
    private SharedPreferences prefs;

    // Parallel arrays — English key stored for API, Arabic shown in UI
    private String[] citiesEn;
    private String[] citiesAr;
    private int[]    methodIds;
    private String[] methodNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        loadArrays();
        setupCitySpinner();
        setupMethodSpinner();
        loadCurrentSettings();
        setupSaveButton();
    }

    // ── Spinner setup ─────────────────────────────────────────────────────────

    private void loadArrays() {
        citiesEn    = getResources().getStringArray(R.array.morocco_cities);
        citiesAr    = getResources().getStringArray(R.array.morocco_cities_ar);
        methodNames = getResources().getStringArray(R.array.calc_methods);
        int[] rawIds = getResources().getIntArray(R.array.calc_method_ids);
        methodIds = rawIds;
    }

    private void setupCitySpinner() {
        // Show Arabic names but store English city (for API)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, citiesAr);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spinnerCity.setAdapter(adapter);
    }

    private void setupMethodSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, methodNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spinnerMethod.setAdapter(adapter);
    }

    // ── Load saved settings into UI ───────────────────────────────────────────

    private void loadCurrentSettings() {
        // City
        String savedCity = prefs.getString("selected_city", "Ifrane");
        for (int i = 0; i < citiesEn.length; i++) {
            if (citiesEn[i].equals(savedCity)) { b.spinnerCity.setSelection(i); break; }
        }

        // Method
        int savedMethod = prefs.getInt("calculation_method", 21);
        for (int i = 0; i < methodIds.length; i++) {
            if (methodIds[i] == savedMethod) { b.spinnerMethod.setSelection(i); break; }
        }

        // Madhhab
        int school = prefs.getInt("madhhab", 0);
        b.rgMadhhab.check(school == 0 ? R.id.rbShafi : R.id.rbHanafi);

        // Adhan global
        b.switchAdhan.setChecked(prefs.getBoolean("adhan_enabled", true));

        // Volume
        b.seekVolume.setProgress(prefs.getInt("adhan_volume", 100));

        // Per-prayer
        b.switchFajr.setChecked(prefs.getBoolean("adhan_fajr_enabled",    true));
        b.switchDhuhr.setChecked(prefs.getBoolean("adhan_dhuhr_enabled",   true));
        b.switchAsr.setChecked(prefs.getBoolean("adhan_asr_enabled",      true));
        b.switchMaghrib.setChecked(prefs.getBoolean("adhan_maghrib_enabled", true));
        b.switchIsha.setChecked(prefs.getBoolean("adhan_isha_enabled",     true));
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    private void setupSaveButton() {
        b.btnSave.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        int cityPos   = b.spinnerCity.getSelectedItemPosition();
        int methodPos = b.spinnerMethod.getSelectedItemPosition();
        int school    = (b.rgMadhhab.getCheckedRadioButtonId() == R.id.rbHanafi) ? 1 : 0;

        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("selected_city",    citiesEn[cityPos]);
        ed.putString("selected_city_ar", citiesAr[cityPos]);
        ed.putInt("calculation_method",  methodIds[methodPos]);
        ed.putInt("madhhab",             school);
        ed.putBoolean("adhan_enabled",   b.switchAdhan.isChecked());
        ed.putInt("adhan_volume",        b.seekVolume.getProgress());
        ed.putBoolean("adhan_fajr_enabled",    b.switchFajr.isChecked());
        ed.putBoolean("adhan_dhuhr_enabled",   b.switchDhuhr.isChecked());
        ed.putBoolean("adhan_asr_enabled",     b.switchAsr.isChecked());
        ed.putBoolean("adhan_maghrib_enabled",  b.switchMaghrib.isChecked());
        ed.putBoolean("adhan_isha_enabled",    b.switchIsha.isChecked());
        ed.apply();

        // Show confirmation
        b.tvSaved.setVisibility(View.VISIBLE);
        b.tvSaved.postDelayed(() -> b.tvSaved.setVisibility(View.INVISIBLE), 2500);

        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show();
    }
}
