package com.aarab.prayertimessersheng;

import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.aarab.prayertimessersheng.databinding.ActivityQiblaBinding;

import java.util.HashMap;
import java.util.Map;

/**
 * Shows a compass needle pointing toward Mecca.
 * Uses accelerometer + magnetic field → rotation matrix → azimuth.
 */
public class QiblaActivity extends AppCompatActivity implements SensorEventListener {

    // Mecca coordinates
    private static final double MECCA_LAT = 21.4225;
    private static final double MECCA_LON = 39.8262;

    // City coordinates lookup (matches cities.xml order)
    private static final Map<String, double[]> CITY_COORDS = new HashMap<>();
    static {
        CITY_COORDS.put("Ifrane",      new double[]{33.5228, -5.1108});
        CITY_COORDS.put("Fes",         new double[]{34.0181, -5.0078});
        CITY_COORDS.put("Rabat",       new double[]{34.0209, -6.8416});
        CITY_COORDS.put("Casablanca",  new double[]{33.5731, -7.5898});
        CITY_COORDS.put("Marrakech",   new double[]{31.6295, -7.9811});
        CITY_COORDS.put("Tanger",      new double[]{35.7595, -5.8340});
        CITY_COORDS.put("Nador",       new double[]{35.1740, -2.9290});
        CITY_COORDS.put("Agadir",      new double[]{30.4278, -9.5981});
        CITY_COORDS.put("Meknes",      new double[]{33.8935, -5.5473});
        CITY_COORDS.put("Tetouan",     new double[]{35.5789, -5.3626});
        CITY_COORDS.put("Oujda",       new double[]{34.6805, -1.9077});
        CITY_COORDS.put("Safi",        new double[]{32.2994, -9.2372});
        CITY_COORDS.put("Kenitra",     new double[]{34.2610, -6.5802});
        CITY_COORDS.put("Khouribga",   new double[]{32.8833, -6.9167});
        CITY_COORDS.put("Beni Mellal", new double[]{32.3369, -6.3498});
        CITY_COORDS.put("El Jadida",   new double[]{33.2316, -8.5007});
        CITY_COORDS.put("Taza",        new double[]{34.2100, -4.0100});
        CITY_COORDS.put("Laayoune",    new double[]{27.1418, -13.1625});
        CITY_COORDS.put("Dakhla",      new double[]{23.6848, -15.9570});
    }

    private ActivityQiblaBinding b;
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;

    private final float[] gravity = new float[3];
    private final float[] geomagnetic = new float[3];
    private boolean hasGravity = false, hasMagneto = false;

    private double qiblaBearing = 0;  // degrees: bearing from city to Mecca
    private double cityLat, cityLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityQiblaBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Get selected city and resolve coordinates
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String city = prefs.getString("selected_city", "Ifrane");
        double[] coords = CITY_COORDS.getOrDefault(city, new double[]{33.5228, -5.1108});
        cityLat = coords[0];
        cityLon = coords[1];

        qiblaBearing = bearingToMecca(cityLat, cityLon);
        double dist   = haversineKm(cityLat, cityLon, MECCA_LAT, MECCA_LON);

        b.tvQiblaDegrees.setText(String.format("%.1f° نحو القبلة", qiblaBearing));
        b.tvQiblaDistance.setText(String.format("المسافة إلى مكة المكرمة: %.0f كم", dist));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magnetometer,  SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    // ── SensorEventListener ───────────────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, 3);
            hasGravity = true;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, 3);
            hasMagneto = true;
        }

        if (!hasGravity || !hasMagneto) return;

        float[] R = new float[9], I = new float[9];
        if (!SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) return;

        float[] orientation = new float[3];
        SensorManager.getOrientation(R, orientation);

        double azimuth = Math.toDegrees(orientation[0]);  // degrees from north
        // Angle to rotate the compass needle so it points to Qibla
        double needleAngle = (qiblaBearing - azimuth + 360) % 360;

        b.compassView.setQiblaAngle((float) needleAngle);
        b.tvQiblaDegrees.setText(String.format("%.1f° نحو القبلة", qiblaBearing));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ── Math helpers ──────────────────────────────────────────────────────────

    /** Great-circle bearing from (lat1,lon1) to Mecca in [0, 360). */
    private double bearingToMecca(double lat1, double lon1) {
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(MECCA_LAT);
        double Δλ = Math.toRadians(MECCA_LON - lon1);
        double x  = Math.sin(Δλ) * Math.cos(φ2);
        double y  = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(Δλ);
        return (Math.toDegrees(Math.atan2(x, y)) + 360) % 360;
    }

    /** Haversine distance in km. */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
