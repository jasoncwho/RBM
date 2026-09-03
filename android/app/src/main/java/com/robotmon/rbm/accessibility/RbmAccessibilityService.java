package com.robotmon.rbm.accessibility;

import android.accessibilityservice.AccessibilityService; 
import android. accessibilityservice.GestureDescription;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
*Minimal AccessibilityService whose only job is dispatching the gestures
*built by SwipeExecutor. Replaces the tap/tapDown/moveTo/tapUp calls the
*original Auto.js script made through its (rooted-device) automation layer.
*The user must enable this service manually once, from
*Settings > Accessibility, the same way Auto. js itself required.
**/

public class RbmAccessibilityService extends AccessibilityService {
    private static final String TAG = "RbmAccessibilityService"; 
    private static volatile RbmAccessibilityService instance;

    public static RbmAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super. onServiceConnected();
        instance = this;
        Log.i(TAG, "Accessibility service connected");
    }

    @Override
    public void onDestroy() {
        instance = null;
        super. onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Intentionally empty: this build only dispatches gestures, it does
        // not read page/window state off accessibility events. See the
        // README for how the original script's page-detection state machine
        // could be added here if needed.
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted");
    }

    /** Dispatches a gesture and blocks the calling thread until it completes or times out. */
    public boolean dispatchGestureBlocking(GestureDescription gesture){
        CountDownLatch latch = new CountDownLatch (1);
        boolean accepted = dispatchGesture(gesture, new GestureResultCallback(){
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                latch.countDown ();
            }

            @Override
            public void onCancelled (GestureDescription gestureDescription) {
                latch.countDown();
            }
        }, null);

        if (!accepted) {
            return false;
        }
        try {
            return latch.await (2, TimeUnit. SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}