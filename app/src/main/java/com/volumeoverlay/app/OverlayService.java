package com.volumeoverlay.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "overlay_channel";
    private static final int NOTIF_ID = 1;
    private static final String POS_PREFS = "overlay_position_prefs";
    private static final int CLICK_SLOP_PX = 18;

    private WindowManager windowManager;
    private View floatView;
    private WindowManager.LayoutParams params;
    private AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        addOverlayView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatView != null && windowManager != null) {
            windowManager.removeView(floatView);
            floatView = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private android.app.Notification buildNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openApp,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void addOverlayView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.overlay_button, null);

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        SharedPreferences posPrefs = getSharedPreferences(POS_PREFS, MODE_PRIVATE);
        params.x = posPrefs.getInt("pos_x", 0);
        params.y = posPrefs.getInt("pos_y", 400);

        windowManager.addView(floatView, params);
        attachTouchHandler();
    }

    private void attachTouchHandler() {
        final TextView plusView = floatView.findViewById(R.id.btnPlus);
        final TextView minusView = floatView.findViewById(R.id.btnMinus);

        floatView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        savePosition(params.x, params.y);
                        if (Math.hypot(dx, dy) < CLICK_SLOP_PX) {
                            handleTap(event.getRawX(), event.getRawY(), plusView, minusView);
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    private void handleTap(float rawX, float rawY, View plusView, View minusView) {
        if (isInsideView(plusView, rawX, rawY)) {
            adjustVolume(AudioManager.ADJUST_RAISE);
        } else if (isInsideView(minusView, rawX, rawY)) {
            adjustVolume(AudioManager.ADJUST_LOWER);
        }
    }

    private boolean isInsideView(View view, float rawX, float rawY) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        Rect rect = new Rect(location[0], location[1],
                location[0] + view.getWidth(), location[1] + view.getHeight());
        return rect.contains((int) rawX, (int) rawY);
    }

    private void adjustVolume(int direction) {
        if (audioManager != null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
        }
    }

    private void savePosition(int x, int y) {
        getSharedPreferences(POS_PREFS, MODE_PRIVATE)
                .edit()
                .putInt("pos_x", x)
                .putInt("pos_y", y)
                .apply();
    }
}
