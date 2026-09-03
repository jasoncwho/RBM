package com.robotmon.rbm.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.os.Environment;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import android.util.Log;

import com.robotmon.rbm.model.Point2D;

/** Saves throttled full-screen debug frames with accepted swipe paths overlaid. */
public class DebugFrameSaver {
    private static final String TAG = "DebugFrameSaver";
    private static final long MIN_SAVE_INTERVAL_MS = 1000;
    private long lastSaveAt;

    public synchronized void save(Context context, Mat frame, List<List<Point2D>> paths) {
        Log.i(TAG, "Saving debug frame with " + paths.size() + " paths");
        long now = System.currentTimeMillis();
        if (now - lastSaveAt < MIN_SAVE_INTERVAL_MS) {
            return;
        }
        lastSaveAt = now;

        Mat annotated = frame.clone();
        try {
            Scalar pathColor = new Scalar(0, 0, 255, 255);
            Scalar pointColor = new Scalar(0, 255, 255, 255);
            for (List<Point2D> path : paths) {
                for (int i = 1; i < path.size(); i++) {
                    Point2D from = path.get(i - 1);
                    Point2D to = path.get(i);
                    Imgproc.line(annotated, new Point(from.x, from.y), new Point(to.x, to.y), pathColor, 5);
                }
                for (Point2D point : path) {
                    Imgproc.circle(annotated, new Point(point.x, point.y), 10, pointColor, 3);
                }
            }

            MatOfByte encoded = new MatOfByte();
            if (!Imgcodecs.imencode(".png", annotated, encoded)) {
                Log.e(TAG, "OpenCV PNG encoding failed");
                encoded.release();
                return;
            }
            byte[] png = new byte[(int) (encoded.total() * encoded.elemSize())];
            encoded.get(0, 0, png);
            encoded.release();

            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, "RBM_debug_" + now + ".png");
            values.put(MediaStore.Downloads.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/");
                values.put(MediaStore.Downloads.IS_PENDING, 1);
            }
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Log.e(TAG, "Unable to create Downloads entry");
                return;
            }
            try (OutputStream output = resolver.openOutputStream(uri)) {
                if (output == null) {
                    Log.e(TAG, "Unable to open Downloads entry");
                    return;
                }
                output.write(png);
            } catch (IOException error) {
                Log.e(TAG, "Unable to write debug PNG", error);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues ready = new ContentValues();
                ready.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(uri, ready, null, null);
            }
            Log.i(TAG, "Saved " + values.getAsString(MediaStore.Downloads.DISPLAY_NAME));
        } finally {
            annotated.release();
        }
    }
}