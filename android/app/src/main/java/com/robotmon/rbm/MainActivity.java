package com.robotmon.rbm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.robotmon.rbm.overlay.OverlayControlService;

/**
 * Launcher entry point. Its only job is to get "draw over other apps"
 * permission granted, then hand off to {@link OverlayControlService}, which
 * shows a small draggable cluster of icon buttons (Enable Accessibility /
 * Start / Stop / Settings) that floats on top of every other app -- this
 * activity closes itself right after the overlay is showing.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> startOverlayIfGranted());

        findViewById(R.id.buttonGrantOverlay).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(intent);
        });

        startOverlayIfGranted();
    }

    private void startOverlayIfGranted() {
        if (!Settings.canDrawOverlays(this)) {
            return;
        }
        startForegroundService(new Intent(this, OverlayControlService.class));
        Toast.makeText(this, "Floating controls shown -- drag the icons anywhere", Toast.LENGTH_LONG).show();
        finish();
    }
}