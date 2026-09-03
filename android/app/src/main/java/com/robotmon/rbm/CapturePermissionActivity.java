package com.robotmon.rbm;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.robotmon.rbm.bot.BotSettings;
import com.robotmon.rbm.bot.BotSettingsStore;
import com.robotmon.rbm.capture.ScreenCaptureService;

/**
 * Invisible trampoline activity: the system MediaProjection consent dialog
 * can only be launched for a result from an Activity, but the "Start"
 * control now lives in the floating icon cluster drawn by
 * {@link com.robotmon.rbm.overlay.OverlayControlService}, not a normal
 * activity. Tapping that floating icon launches this activity (themed
 * translucent so it's invisible to the user), which immediately requests
 * screen-capture permission, starts the capture service/swipe executor/bot
 * orchestrator on success, then finishes itself.
 */
public class CapturePermissionActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> projectionLauncher;
    private boolean requestLaunched;
    private boolean resultHandled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        projectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (resultHandled) {
                        return;
                    }
                    resultHandled = true;
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
                    serviceIntent.putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.getResultCode());
                    serviceIntent.putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.getData());
                    startForegroundService(serviceIntent);
                    finish();
                    RbmApp.getSwipeExecutor().start();
                    BotSettings settings = new BotSettingsStore(this).load();
                    RbmApp.getBotOrchestrator().start(settings);
                });

        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (savedInstanceState == null) {
            requestLaunched = true;
            projectionLauncher.launch(projectionManager.createScreenCaptureIntent());
        }
    }
}