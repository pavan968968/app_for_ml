package com.example.mainactivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private TextView predictionTextView;
    private Handler frameHandler;
    private Runnable frameRunnable;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private boolean isCameraInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Log.d(TAG, "onCreate started");

        initializeViews();
        checkCameraPermission();
    }

    private void initializeViews() {
        surfaceView = findViewById(R.id.surfaceView);
        predictionTextView = findViewById(R.id.predictionTextView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");
                if (ContextCompat.checkSelfPermission(CameraActivity.this,
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    initializeCamera();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged");
                if (camera != null) {
                    try {
                        camera.stopPreview();
                        configureCameraParameters();
                        camera.setPreviewDisplay(holder);
                        camera.startPreview();
                    } catch (IOException e) {
                        Log.e(TAG, "Error setting camera preview", e);
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");
                releaseCameraResources();
            }
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting camera permission");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            Log.d(TAG, "Camera permission already granted");
            initializeCamera();
        }
    }

    private void initializeCamera() {
        try {
            Log.d(TAG, "Initializing camera");
            if (isCameraInitialized) {
                Log.d(TAG, "Camera already initialized");
                return;
            }

            releaseCameraResources();
            camera = Camera.open(currentCameraId);

            if (camera == null) {
                Log.e(TAG, "Failed to open camera");
                showToast("Failed to open camera");
                return;
            }

            camera.setPreviewDisplay(surfaceHolder);
            configureCameraParameters();
            setCameraDisplayOrientation();
            camera.startPreview();
            startFrameProcessing();

            isCameraInitialized = true;
            Log.d(TAG, "Camera initialized successfully");
        } catch (RuntimeException e) {
            Log.e(TAG, "Camera not available: " + e.getMessage(), e);
            showToast("Camera not available: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage(), e);
            showToast("Error setting camera preview: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing camera: " + e.getMessage(), e);
            showToast("Error initializing camera: " + e.getMessage());
        }
    }

    private void configureCameraParameters() {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalSize = getOptimalPreviewSize(sizes,
                    surfaceView.getWidth(), surfaceView.getHeight());

            if (optimalSize != null) {
                parameters.setPreviewSize(optimalSize.width, optimalSize.height);
                camera.setParameters(parameters);
                adjustViewSize(optimalSize.width, optimalSize.height);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error configuring camera parameters", e);
        }
    }

    private void adjustViewSize(int previewWidth, int previewHeight) {
        ViewGroup parent = (ViewGroup) surfaceView.getParent();
        if (parent == null) return;

        int containerWidth = parent.getWidth();
        int containerHeight = parent.getHeight();

        if (containerWidth == 0 || containerHeight == 0) return; // Prevent division by zero

        // Swap width and height if the camera is in portrait mode
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (isPortrait) {
            int temp = previewWidth;
            previewWidth = previewHeight;
            previewHeight = temp;
        }

        float previewRatio = (float) previewWidth / previewHeight;
        float containerRatio = (float) containerWidth / containerHeight;

        int newWidth, newHeight;

        if (previewRatio > containerRatio) {
            // The preview is wider than the container
            newWidth = containerWidth;
            newHeight = (int) (containerWidth / previewRatio);
        } else {
            // The preview is taller or has the same ratio as the container
            newHeight = containerHeight;
            newWidth = (int) (containerHeight * previewRatio);
        }

        // Make sure the preview does not exceed the container dimensions
        newWidth = Math.min(newWidth, containerWidth);
        newHeight = Math.min(newHeight, containerHeight);

        // Set the new layout parameters
        ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
        layoutParams.width = newWidth;
        layoutParams.height = newHeight;
        surfaceView.setLayoutParams(layoutParams);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        if (sizes == null) return null;

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;

            double diff = Math.abs(size.height - h);
            if (diff < minDiff) {
                optimalSize = size;
                minDiff = diff;
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                double diff = Math.abs(size.height - h);
                if (diff < minDiff) {
                    optimalSize = size;
                    minDiff = diff;
                }
            }
        }

        return optimalSize;
    }

    private void setCameraDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(currentCameraId, info);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:   degrees = 0; break;
            case Surface.ROTATION_90:  degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }

        camera.setDisplayOrientation(result);
    }

    private void startFrameProcessing() {
        frameHandler = new Handler();
        frameRunnable = new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.setOneShotPreviewCallback((data, camera) -> {
                        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                            Camera.Parameters parameters = camera.getParameters();
                            Camera.Size size = parameters.getPreviewSize();
                            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,
                                    size.width, size.height, null);
                            yuvImage.compressToJpeg(new Rect(0, 0, size.width, size.height),
                                    80, out);
                            byte[] imageBytes = out.toByteArray();
                            // Process image bytes here.
                            // For example, update UI:
                            runOnUiThread(() -> predictionTextView.setText("Frame captured"));
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing frame", e);
                        }
                    });
                }
                frameHandler.postDelayed(this, 1000); // Capture every second
            }
        };
        frameHandler.post(frameRunnable);
    }

    private void stopFrameProcessing() {
        if (frameHandler != null) {
            frameHandler.removeCallbacks(frameRunnable);
        }
    }

    public void switchCamera(View view) {
        if (Camera.getNumberOfCameras() < 2) {
            showToast("No secondary camera available");
            return;
        }

        currentCameraId = (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) ?
                Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;

        isCameraInitialized = false;
        new Handler().postDelayed(this::initializeCamera, 200);
    }

    private void releaseCameraResources() {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
                isCameraInitialized = false;
                Log.d(TAG, "Camera resources released");
            } catch (Exception e) {
                Log.e(TAG, "Error releasing camera resources", e);
            }
        }
        stopFrameProcessing();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!isCameraInitialized) {
            initializeCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        releaseCameraResources();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted");
                initializeCamera();
            } else {
                Log.d(TAG, "Camera permission denied");
                showToast("Camera permission is required for this app");
                finish();
            }
        }
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(CameraActivity.this, message,
                Toast.LENGTH_SHORT).show());
    }
}