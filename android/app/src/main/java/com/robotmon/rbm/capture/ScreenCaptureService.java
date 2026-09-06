package com.robotmon.rbm.capture;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.content.pm.ServiceInfo;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.robotmon.rbm.MainActivity;
import com.robotmon.rbm.RbmApp;
import com.robotmon.rbm.geometry.GameCoordinateMapper;
import com.robotmon.rbm.scan.BoardMapper;
import com.robotmon.rbm.scan.ScanEngine;
import com.robotmon.rbm.swipe.SwipeQueue;
import com.robotmon.rbm.util.DebugFrameSaver;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground service that owns the MediaProjection screen capture, crops each
 * frame to the play area, resizes it, and hands it to ScanEngine once per
 * capture interval.
 *
 * This is the "scanning the screen" entry point: frame capture happens here
 * on a dedicated background thread (imageReaderThread's Handler); the actual
 * multithreaded pixel analysis happens inside ScanEngine.scanAndEnqueue().
 *
 * Ported from Tsum.prototype.playScreenshotSquare() (the equivalent capture
 * call in the original script).
 */
public class ScreenCaptureService extends Service {

    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_DATA = "resultData";

    private static final String TAG = "ScreenCaptureService";
    private static final String CHANNEL_ID = "rbm_capture";
    private static final int NOTIFICATION_ID = 1;

    /** Minimum spacing between scanned frames; throttles capture to roughly match how fast the board can change. */
    private static final long CAPTURE_INTERVAL_MS = 150;

    private MediaProjection mediaProjection;
    private MediaProjection.Callback mediaProjectionCallback;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread imageReaderThread;
    private Handler imageReaderHandler;
    private ExecutorService processingExecutor;

    private ScanEngine scanEngine;
    private GameCoordinateMapper coordinateMapper;
    private final DebugFrameSaver debugFrameSaver = new DebugFrameSaver();
    private volatile long lastCaptureAt = 0;
    private static final java.util.concurrent.atomic.AtomicBoolean paused = 
            new java.util.concurrent.atomic.AtomicBoolean(false);

    public static void setPaused(boolean pause) {
        paused.set(pause);
    }
    
    public static boolean isPaused() {
        return paused.get();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        processingExecutor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, buildNotification());
        }

        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent resultData;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
        } else {
            resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        }
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            Log.e(TAG, "Missing MediaProjection permission result");
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData);
        if (mediaProjection == null) {
            Log.e(TAG, "Unable to create MediaProjection");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        mediaProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.i(TAG, "MediaProjection stopped");
                stopSelf();
            }
        };
        mediaProjection.registerCallback(mediaProjectionCallback, new Handler(getMainLooper()));

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        coordinateMapper = RbmApp.getCoordinateMapper();
        coordinateMapper.init(width, height);

        SwipeQueue swipeQueue = RbmApp.getSwipeQueue();
        BoardMapper boardMapper = new BoardMapper(
                coordinateMapper.getPlayOffsetX(), coordinateMapper.getPlayOffsetY(),
                coordinateMapper.getPlayWidth(), coordinateMapper.getPlayHeight(),
                com.robotmon.rbm.config.GameConfig.CAPTURE_SIZE, com.robotmon.rbm.config.GameConfig.CAPTURE_SIZE);
        scanEngine = new ScanEngine(swipeQueue, boardMapper);
        RbmApp.setBoardMapper(boardMapper);

        imageReaderThread = new HandlerThread("ImageReaderThread");
        imageReaderThread.start();
        imageReaderHandler = new Handler(imageReaderThread.getLooper());

        long imageUsage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ? android.hardware.HardwareBuffer.USAGE_CPU_READ_OFTEN
            : 0;
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3, imageUsage);
        imageReader.setOnImageAvailableListener(this::onImageAvailable, imageReaderHandler);

        int displayFlags = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
            | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "RbmCapture", width, height, density,
            displayFlags,
                imageReader.getSurface(), null, imageReaderHandler);
        Log.i(TAG, "Virtual display created: " + (virtualDisplay != null)
            + ", surface valid: " + imageReader.getSurface().isValid());

        return START_STICKY;
    }

    private void onImageAvailable(ImageReader reader) {
        Log.i(TAG, "ImageReader frame received");
        long now = System.currentTimeMillis();
        Image image = reader.acquireLatestImage();
        if (image == null) {
            Log.w(TAG, "ImageReader callback had no image");
            return;
        }

        if(paused.get()) {
            image.close();
            return;
        }

        if (now - lastCaptureAt < CAPTURE_INTERVAL_MS) {
            image.close();
            return;
        }
        lastCaptureAt = now;

        Mat frame = imageToMat(image);
        image.close();
        Mat debugFrame = frame.clone();

        Mat board = new Mat(frame, new Rect(coordinateMapper.getPlayOffsetX(), coordinateMapper.getPlayOffsetY(),
                coordinateMapper.getPlayWidth(), coordinateMapper.getPlayHeight()));
        Mat resizedBoard = new Mat();
        int captureSize = com.robotmon.rbm.config.GameConfig.CAPTURE_SIZE;
        Imgproc.resize(board, resizedBoard, new Size(captureSize, captureSize));
        Imgproc.cvtColor(resizedBoard, resizedBoard, Imgproc.COLOR_RGBA2BGR);
        board.release();
        RbmApp.getBoardFrameHolder().set(resizedBoard);

        // Full-screen frame downscaled by resizerRatio, matching
        // // Tsum.prototype.screenshot() -- feeds PageDetector's color reads.
        double resizerRatio = coordinateMapper.getResizerRatio();
        Mat resizedFull = new Mat();
        Imgproc.resize(frame, resizedFull,
                new Size(Math.max(1, frame.cols() / resizerRatio), Math.max(1, frame.rows() / resizerRatio)));
        frame.release();
        RbmApp.getFrameHolder().set(resizedFull);
        resizedFull.release();

        processingExecutor.submit(() -> {
            try {
                debugFrameSaver.save(this, debugFrame, scanEngine.scanAndEnqueue(resizedBoard));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.e(TAG, "Scan failed", e);
            } finally {
                resizedBoard.release();
                debugFrame.release();
            }
        });
    }

    private Mat imageToMat(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();

        Mat mat = new Mat(image.getHeight(), image.getWidth() + rowPadding / pixelStride, CvType.CV_8UC4);
        buffer.rewind();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        mat.put(0, 0, bytes);
        return mat.submat(0, image.getHeight(), 0, image.getWidth());
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Screen scanning", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Intent contentIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RBM scanning screen")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        if (virtualDisplay != null) { virtualDisplay.release(); }
        if (imageReader != null) { imageReader.close(); }
        if (mediaProjection != null) {
            if (mediaProjectionCallback != null) {
                mediaProjection.unregisterCallback(mediaProjectionCallback);
            }
            mediaProjection.stop();
        }
        if (imageReaderThread != null) { imageReaderThread.quitSafely(); }
        if (processingExecutor != null) { processingExecutor.shutdownNow(); }
        if (scanEngine != null) { scanEngine.shutdown(); }
        super.onDestroy();
    }
}