package com.volumeoverlay.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    public static final String PREFS = "volume_overlay_prefs";
    public static final String KEY_ENABLED = "enabled";
    private static final int REQ_OVERLAY = 100;
    private static final int REQ_BATTERY_OPT = 101;

    private Switch toggleSwitch;
    private TextView statusText;
    private TextView batteryStatusText;
    private SharedPreferences prefs;
    private boolean suppressToggleCallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        toggleSwitch = findViewById(R.id.toggleSwitch);
        statusText = findViewById(R.id.statusText);
        batteryStatusText = findViewById(R.id.batteryStatusText);
        Button grantOverlayBtn = findViewById(R.id.grantOverlayBtn);
        Button batteryOptBtn = findViewById(R.id.batteryOptBtn);

        grantOverlayBtn.setOnClickListener(v -> requestOverlayPermission());
        batteryOptBtn.setOnClickListener(v -> requestIgnoreBatteryOptimizations());

        toggleSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (suppressToggleCallback) {
                return;
            }
            if (isChecked) {
                if (hasOverlayPermission()) {
                    prefs.edit().putBoolean(KEY_ENABLED, true).apply();
                    startOverlayService();
                } else {
                    // Revert switch until permission is actually granted.
                    setToggleChecked(false);
                    requestOverlayPermission();
                }
            } else {
                prefs.edit().putBoolean(KEY_ENABLED, false).apply();
                stopOverlayService();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        refreshBatteryStatus();
        boolean wantsEnabled = prefs.getBoolean(KEY_ENABLED, false);
        setToggleChecked(wantsEnabled && hasOverlayPermission());

        // If the user granted the permission while we were away and had
        // previously asked for the overlay to be shown, start it now.
        if (wantsEnabled && hasOverlayPermission()) {
            startOverlayService();
        }
    }

    private void setToggleChecked(boolean checked) {
        suppressToggleCallback = true;
        toggleSwitch.setChecked(checked);
        suppressToggleCallback = false;
    }

    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void requestOverlayPermission() {
        if (hasOverlayPermission()) {
            return;
        }
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQ_OVERLAY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            refreshStatus();
            if (hasOverlayPermission() && prefs.getBoolean(KEY_ENABLED, false)) {
                startOverlayService();
                setToggleChecked(true);
            }
        }
    }

    private void refreshStatus() {
        statusText.setText(hasOverlayPermission()
                ? getString(R.string.status_permission_ok)
                : getString(R.string.status_permission_missing));
    }

    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void requestIgnoreBatteryOptimizations() {
        if (isIgnoringBatteryOptimizations()) {
            return;
        }
        Intent intent = new Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQ_BATTERY_OPT);
    }

    private void refreshBatteryStatus() {
        batteryStatusText.setText(isIgnoringBatteryOptimizations()
                ? getString(R.string.battery_opt_ok)
                : getString(R.string.battery_opt_missing));
    }

    private void startOverlayService() {
        Intent serviceIntent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopOverlayService() {
        stopService(new Intent(this, OverlayService.class));
    }
}
