package org.emnets.ar.arclient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Manager extends CameraDevice.StateCallback{
    public enum Facing {
        BACK, FRONT
    }
    private final String TAG = "Camera2Manager";

    private CameraDevice cameraDevice;
    private Surface surface;
    private SurfaceView surfaceView;
    private TextureView textureView;
    private Surface surfaceEncoder; //input surfaceEncoder from videoEncoder
    private CameraManager cameraManager;
    private Handler cameraHandler;
    private CameraCaptureSession cameraCaptureSession;
    private boolean prepared = false;
    private int cameraId = -1;
    private boolean isFrontCamera = false;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureRequest.Builder builderInputSurface;
    private float fingerSpacing = 0;
    private float zoomLevel = 1.0f;
    private boolean lanternEnable = false;
    private boolean running = false;

    //Face detector
    public interface FaceDetectorCallback {
        void onGetFaces(Face[] faces);
    }
    private boolean faceDetectionEnabled = false;
    private int faceDetectionMode;

    public Camera2Manager(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void prepareCamera(SurfaceView surfaceView, Surface surface) {
        this.surfaceView = surfaceView;
        this.surfaceEncoder = surface;
        prepared = true;
    }

    public void prepareCamera(TextureView textureView, Surface surface) {
        this.textureView = textureView;
        this.surfaceEncoder = surface;
        prepared = true;
    }

    public void prepareCamera(Surface surface1, Surface surface) {
        this.surface = surface1;
        this.surfaceEncoder = surface;
        prepared = true;
    }

    public void prepareCamera(Surface surface) {
        this.surfaceEncoder = surface;
        prepared = true;
    }

    public void prepareCamera(SurfaceTexture surfaceTexture, int width, int height) {
        surfaceTexture.setDefaultBufferSize(width, height);
        this.surfaceEncoder = new Surface(surfaceTexture);
        prepared = true;
    }

    public boolean isPrepared() {
        return prepared;
    }

    private void startPreview(CameraDevice cameraDevice) {
        try {
            final List<Surface> listSurfaces = new ArrayList<>();
            Surface preview = addPreviewSurface();
            if (preview != null) listSurfaces.add(preview);
            if (surfaceEncoder != null) listSurfaces.add(surfaceEncoder);

            cameraDevice.createCaptureSession(listSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Camera2Manager.this.cameraCaptureSession = cameraCaptureSession;
                    try {
                        CaptureRequest captureRequest = drawSurface(listSurfaces);
                        if (captureRequest != null) {
                            cameraCaptureSession.setRepeatingRequest(captureRequest,
                                    faceDetectionEnabled ? cb : null, cameraHandler);
                            Log.i(TAG, "Camera configured");
                        } else {
                            Log.e(TAG, "Error, captureRequest is null");
                        }
                    } catch (CameraAccessException | NullPointerException e) {
                        Log.e(TAG, "Error", e);
                    } catch (IllegalStateException e) {
                        reOpenCamera(cameraId != -1 ? cameraId : 0);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    cameraCaptureSession.close();
                    Log.e(TAG, "Configuration failed");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
        } catch (IllegalStateException e) {
            reOpenCamera(cameraId != -1 ? cameraId : 0);
        }
    }

    private Surface addPreviewSurface() {
        Surface surface = null;
        if (surfaceView != null) {
            surface = surfaceView.getHolder().getSurface();
        } else if (textureView != null) {
            final SurfaceTexture texture = textureView.getSurfaceTexture();
            surface = new Surface(texture);
        } else if (this.surface != null) {
            surface = this.surface;
        }
        return surface;
    }

    private CaptureRequest drawSurface(List<Surface> surfaces) {
        try {
            builderInputSurface = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            for (Surface surface : surfaces){
                if (surface != null) builderInputSurface.addTarget(surface);
            }
            builderInputSurface.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            return builderInputSurface.build();
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG, "Error", e);
            return null;
        }
    }

    public int getLevelSupported() {
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
            return cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG, "Error", e);
            return -1;
        }
    }

    public void openCamera() {
        openCameraBack();
    }

    public void openCameraBack() {
        openCameraFacing(Facing.BACK);
    }

    public void openCameraFront() {
        openCameraFacing(Facing.FRONT);
    }

    public void openLastCamera() {
        if (cameraId == -1) {
            openCameraBack();
        } else {
            openCameraId(cameraId);
        }
    }

    public Size[] getCameraResolutionsBack() {
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                    != CameraCharacteristics.LENS_FACING_BACK) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics("1");
            }
            StreamConfigurationMap streamConfigurationMap =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
            return new Size[0];
        }
    }

    public void getCameraFpsBack() {
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                    != CameraCharacteristics.LENS_FACING_BACK) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics("1");
            }
            Range<Integer> range[] = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            for(Range<Integer> r : range){
                Log.v(TAG,r.toString());
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
        }
    }

    public Size[] getCameraResolutionsFront() {
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                    != CameraCharacteristics.LENS_FACING_FRONT) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics("1");
            }
            StreamConfigurationMap streamConfigurationMap =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
            return new Size[0];
        }
    }

    public CameraCharacteristics getCameraCharacteristics() {
        return cameraCharacteristics;
    }

    /**
     * Select camera facing
     *
     * @param cameraFacing - CameraCharacteristics.LENS_FACING_FRONT, CameraCharacteristics.LENS_FACING_BACK,
     *                     CameraCharacteristics.LENS_FACING_EXTERNAL
     */
    public void openCameraFacing(Facing cameraFacing) {
        int facing = cameraFacing == Facing.BACK ? CameraMetadata.LENS_FACING_BACK
                : CameraMetadata.LENS_FACING_FRONT;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                openCameraId(0);
            } else {
                openCameraId(cameraManager.getCameraIdList().length - 1);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
        }
    }

    public boolean isLanternSupported() {
        return (cameraCharacteristics != null ? cameraCharacteristics.get(
                CameraCharacteristics.FLASH_INFO_AVAILABLE) : false);
    }

    public boolean isLanternEnabled() {
        return lanternEnable;
    }

    /**
     * @required: <uses-permission android:name="android.permission.FLASHLIGHT"/>
     */
    public void enableLantern() throws Exception {
        if ((cameraCharacteristics != null) && cameraCharacteristics.get(
                CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
            if (builderInputSurface != null) {
                try {
                    builderInputSurface.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                    cameraCaptureSession.setRepeatingRequest(builderInputSurface.build(),
                            faceDetectionEnabled ? cb : null, null);
                    lanternEnable = true;
                } catch (Exception e) {
                    Log.e(TAG, "Error", e);
                }
            }
        } else {
            Log.e(TAG, "Lantern unsupported");
            throw new Exception("Lantern unsupported");
        }
    }

    /**
     * @required: <uses-permission android:name="android.permission.FLASHLIGHT"/>
     */
    public void disableLantern() {
        if ((cameraCharacteristics != null) && cameraCharacteristics.get(
                CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
            if (builderInputSurface != null) {
                try {
                    builderInputSurface.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    cameraCaptureSession.setRepeatingRequest(builderInputSurface.build(),
                            faceDetectionEnabled ? cb : null, null);
                    lanternEnable = false;
                } catch (Exception e) {
                    Log.e(TAG, "Error", e);
                }
            }
        }
    }


    private void setFaceDetect(CaptureRequest.Builder requestBuilder, int faceDetectMode) {
        if (faceDetectionEnabled) {
            requestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, faceDetectMode);
        }
    }

    private void prepareFaceDetectionCallback() {
        try {
            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.setRepeatingRequest(builderInputSurface.build(),
                    faceDetectionEnabled ? cb : null, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
        }
    }

    private final CameraCaptureSession.CaptureCallback cb =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
                }
            };

    @SuppressLint("MissingPermission")
    public void openCameraId(Integer cameraId) {
        this.cameraId = cameraId;
        if (prepared) {
            HandlerThread cameraHandlerThread = new HandlerThread(TAG + " Id = " + cameraId);
            cameraHandlerThread.start();
            cameraHandler = new Handler(cameraHandlerThread.getLooper());
            try {
                cameraManager.openCamera(cameraId.toString(), this, cameraHandler);
                cameraCharacteristics = cameraManager.getCameraCharacteristics(Integer.toString(cameraId));
                running = true;
                isFrontCamera =
                        (LENS_FACING_FRONT == cameraCharacteristics.get(CameraCharacteristics.LENS_FACING));
            } catch (CameraAccessException | SecurityException e) {
                Log.e(TAG, "Error", e);
            }
        } else {
            Log.e(TAG, "Camera2ApiManager need be prepared, Camera2ApiManager not enabled");
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void switchCamera() {
        int cameraId = cameraDevice == null ? 0 : Integer.parseInt(cameraDevice.getId()) == 1 ? 0 : 1;
        reOpenCamera(cameraId);
    }

    private void reOpenCamera(int cameraId) {
        if (cameraDevice != null) {
            closeCamera(false);
            if (textureView != null) {
                prepareCamera(textureView, surfaceEncoder);
            } else if (surfaceView != null) {
                prepareCamera(surfaceView, surfaceEncoder);
            } else if (surface != null) {
                prepareCamera(surface, surfaceEncoder);
            } else {
                prepareCamera(surfaceEncoder);
            }
            openCameraId(cameraId);
        }
    }

    public float getMaxZoom() {
        return (cameraCharacteristics != null ? cameraCharacteristics.get(
                CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) : 1);
    }

    public Float getZoom() {
        return zoomLevel;
    }

    public void setZoom(Float level) {
        try {
            float maxZoom = getMaxZoom();
            Rect m = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            if ((level <= maxZoom) && (level >= 1)) {
                zoomLevel = level;
                int minW = (int) (m.width() / (maxZoom * 10));
                int minH = (int) (m.height() / (maxZoom * 10));
                int difW = m.width() - minW;
                int difH = m.height() - minH;
                int cropW = (int) (difW / 10 * level);
                int cropH = (int) (difH / 10 * level);
                cropW -= cropW & 3;
                cropH -= cropH & 3;
                Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
                builderInputSurface.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                cameraCaptureSession.setRepeatingRequest(builderInputSurface.build(),
                        faceDetectionEnabled ? cb : null, null);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error", e);
        }
    }

    public void setZoom(MotionEvent event) {
        float currentFingerSpacing;

        if (event.getPointerCount() > 1) {
            // Multi touch logic
            currentFingerSpacing = getFingerSpacing(event);
            if (fingerSpacing != 0) {
                if (currentFingerSpacing > fingerSpacing && getMaxZoom() > zoomLevel) {
                    zoomLevel += 0.1f;
                } else if (currentFingerSpacing < fingerSpacing && zoomLevel > 1) {
                    zoomLevel -= 0.1f;
                }
                setZoom(zoomLevel);
            }
            fingerSpacing = currentFingerSpacing;
        }
    }
    public static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
    public boolean isFrontCamera() {
        return isFrontCamera;
    }

    private void resetCameraValues() {
        lanternEnable = false;
        zoomLevel = 1.0f;
    }

    public void stopRepeatingEncoder() {
        if (cameraCaptureSession != null) {
            try {
                cameraCaptureSession.stopRepeating();
                surfaceEncoder = null;
                CaptureRequest captureRequest = drawSurface(Collections.singletonList(addPreviewSurface()));
                if (captureRequest != null) {
                    cameraCaptureSession.setRepeatingRequest(captureRequest, null, cameraHandler);
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "Error", e);
            }
        }
    }

    public void closeCamera() {
        closeCamera(true);
    }

    public void closeCamera(boolean resetSurface) {
        resetCameraValues();
        cameraCharacteristics = null;
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (cameraHandler != null) {
            cameraHandler.getLooper().quitSafely();
            cameraHandler = null;
        }
        if (resetSurface) {
            surfaceEncoder = null;
            builderInputSurface = null;
        }
        prepared = false;
        running = false;
    }

    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
        this.cameraDevice = cameraDevice;
        startPreview(cameraDevice);
        Log.i(TAG, "Camera opened");
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
        cameraDevice.close();
        Log.i(TAG, "Camera disconnected");
    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int i) {
        cameraDevice.close();
        Log.e(TAG, "Open failed");
    }
}