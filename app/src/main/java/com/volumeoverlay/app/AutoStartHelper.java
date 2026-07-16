package com.volumeoverlay.app;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

/**
 * ColorOS keeps its own "auto-start" background-permission list that is separate from
 * (and stronger than) the standard Android battery-optimization whitelist. There is no
 * public AOSP API for it, so we jump straight to the known settings screen by component
 * name and fall back to the generic app-details page if none of them exist on this build.
 */
final class AutoStartHelper {

    private AutoStartHelper() {
    }

    private static final String[][] KNOWN_COMPONENTS = {
            {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"},
            {"com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"},
            {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"},
            {"com.coloros.privacypermissionsentry", "com.coloros.privacypermissionsentry.PermissionTopActivity"},
    };

    static void openAutoStartSettings(Context context) {
        for (String[] component : KNOWN_COMPONENTS) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(component[0], component[1]));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            } catch (Exception ignored) {
                // Component not present on this ColorOS build; try the next one.
            }
        }
        openAppDetailsSettings(context);
    }

    private static void openAppDetailsSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
        }
    }
}
