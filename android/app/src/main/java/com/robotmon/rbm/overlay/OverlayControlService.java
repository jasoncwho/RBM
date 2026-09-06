package com.robotmon.rbm.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.robotmon.rbm.CapturePermissionActivity;
import com.robotmon.rbm.MainActivity;
import com.robotmon.rbm.RbmApp;
import com.robotmon.rbm.SettingsActivity;
import com.robotmon.rbm.capture.ScreenCaptureService;
import com.robotmon.rbm.swipe.SwipeExecutor;

/**
 * Foreground service that draws a small draggable cluster of 4 icon buttons
 * -- Enable Accessibility / Start / Stop / Settings -- on top of every other
 * app, replacing the old full-screen control panel. This is what
 * {@link MainActivity} hands off to once "draw over other apps" permission
 * is granted.
 */
public class OverlayControlService extends Service {

    private static final String CHANNEL_ID = "rbm_overlay";
    private static final int NOTIFICATION_ID = 2;
    private static final int ICON_SIZE_DP = 48;
    private static final int ICON_MARGIN_DP = 6;

    // Handled in onStartCommand() to stop from the notification action instead of
    // the floating overlay icon. While the accessibility service is continuously
    // dispatching synthetic swipe gestures, that gee stream can occupy the
    // touch input pipeline almost 100% of the time, so a real tap on the small
    // Floating stop icon can go undelivered fhile (needing many attempts to
    // land in a gap). The notification shade is a seperate system-UI surface and 
    // its action buttons are delivered via pendingIntent, not raw touch dispatch,
    // so this path stop reliably even while swiping is active.
    public static final String ACTION_STOP = "com.robotmon.rbm.overlay.ACTION_STOP";
    private WindowManager windowManager;
    private LinearLayout container;
    private WindowManager.LayoutParams layoutParams;
    private TextView pauseIcon;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            closeRbmCompletely();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        if (container == null) {
            showOverlay();
        }
        return START_STICKY;
    }

    private void showOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);

        container.addView(buildIcon("A", Color.parseColor("#1976D2"), v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));
       
        container.addView(buildIcon("\u25B6", Color.parseColor("#388E3C"), v ->
                startActivity(new Intent(this, CapturePermissionActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));
        pauseIcon = buildIcon("\u23F8", Color.parseColor("#F57C00"), v -> togglePause());
        container.addView(pauseIcon);
        container.addView(buildIcon("\u25A0", Color.parseColor("#D32F2F"), v -> {
            closeRbmCompletely();
        }));

        container.addView(buildIcon("\u2699", Color.parseColor("#616161"), v ->
                startActivity(new Intent(this, SettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = dp(ICON_MARGIN_DP);
        layoutParams.y = dp(80);

        container.setOnTouchListener(new DragTouchListener());
        windowManager.addView(container, layoutParams);
    }

    private TextView buildIcon(String label, int color, View.OnClickListener onClick) {
        TextView icon = new TextView(this);
        icon.setText(label);
        icon.setTextColor(Color.WHITE);
        icon.setTextSize(18);
        icon.setGravity(Gravity.CENTER);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(color);
        icon.setBackground(background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(ICON_SIZE_DP), dp(ICON_SIZE_DP));
        params.leftMargin = dp(ICON_MARGIN_DP);
        icon.setLayoutParams(params);
        icon.setOnClickListener(onClick);
        return icon;
    }

    /**
     * Single touch listener on the whole cluster: drags the cluster around
     * the screen, and taps under the slop threshold are hit-tested against
     * the children and forwarded via {@code performClick()} (the container
     * consumes all touches, so children never see them directly).
     */
    private class DragTouchListener implements View.OnTouchListener {
        private static final int TOUCH_SLOP_PX = 12;

        private float downRawX;
        private float downRawY;
        private int downLayoutX;
        private int downLayoutY;
        private boolean dragging;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    downLayoutX = layoutParams.x;
                    downLayoutY = layoutParams.y;
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    int dx = (int) (event.getRawX() - downRawX);
                    int dy = (int) (event.getRawY() - downRawY);
                    if (!dragging && (Math.abs(dx) > TOUCH_SLOP_PX || Math.abs(dy) > TOUCH_SLOP_PX)) {
                        dragging = true;
                    }
                    if (dragging) {
                        layoutParams.x = downLayoutX + dx;
                        layoutParams.y = downLayoutY + dy;
                        windowManager.updateViewLayout(container, layoutParams);
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                    if (!dragging) {
                        dispatchClick(event.getRawX(), event.getRawY());
                    }
                    return true;
                default:
                    return false;
            }
        }
    
        private void dispatchClick(float rawX, float rawY) {
            int[] location = new int[2];
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                child.getLocationOnScreen(location);
                if (rawX >= location[0] && rawX <= location[0] + child.getWidth()
                    && rawY >= location[1] && rawY <= location[1] + child.getHeight()) {
                    child.performClick();
                    return;
                }
            }
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Floating controls", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        Intent contentIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        Intent stopIntent = new Intent(this, OverlayControlService.class).setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RBM floating controls active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .setOngoing(true)
                .build();
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void togglePause(){
        SwipeExecutor executor = RbmApp.getSwipeExecutor();
        boolean nowPaused = !executor.isPaused();
        if(nowPaused){
            executor.pause();
            ScreenCaptureService.setPaused(true);
        }else{
            executor.resume();
            ScreenCaptureService.setPaused(false);
        }
        if (pauseIcon != null) {
            pauseIcon.setText(nowPaused ? "\u25B6" : "\u23F8");
        }
    }
    private void closeRbmCompletely() {
        try {
            RbmApp.getBotOrchestrator().stop();
            RbmApp.getSwipeExecutor().stop();
            stopService(new Intent(this, ScreenCaptureService.class));
            if (windowManager != null && container != null) {
                windowManager.removeViewImmediate(container);
                container = null;
            }
            stopSelf();
        } catch (Throwable error) {
            android.util.Log.e("OverlayControlService", "Shutdown cleanup failed", error);
        } finally {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    public void onDestroy() {
        if (windowManager != null && container != null) {
            try {
                windowManager.removeViewImmediate(container);
            } catch (IllegalArgumentException ignored) {
            }
            container = null;
        }
        super.onDestroy();
    }
}