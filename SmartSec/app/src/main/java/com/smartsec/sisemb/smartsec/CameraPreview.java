package com.smartsec.sisemb.smartsec;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static android.content.ContentValues.TAG;

/**
 * Created by Marcelo on 03/12/2016.
 */

/**
 * A basic Camera preview class
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    public byte[] imgByteArray = null;
    public Bitmap scaledBmp = null;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void takeSnapPhoto() {
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                Camera.Parameters parameters = camera.getParameters();
                int format = parameters.getPreviewFormat();
                //YUV formats require more conversion
                if (format == ImageFormat.NV21 || format == ImageFormat.YUY2 || format == ImageFormat.NV16) {
                    int w = parameters.getPreviewSize().width;
                    int h = parameters.getPreviewSize().height;
                    // Get the YuV image
                    YuvImage yuv_image = new YuvImage(data, format, w, h, null);
                    // Convert YuV to Jpeg
                    Rect rect = new Rect(0, 0, w, h);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    yuv_image.compressToJpeg(rect, 100, outputStream);
                    imgByteArray = outputStream.toByteArray();
                    Bitmap resizedBmp = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray.length);
                    //scaledBmp = Bitmap.createScaledBitmap(resizedBmp, 8, 8, true);
                    scaledBmp = resizedBmp;
                    Log.d(TAG, "TEST   =>>> " + scaledBmp.getHeight() + " " + scaledBmp.getWidth());
                }
            }
        });
    }

    public byte[] getImgByteArray() {
        return imgByteArray;
    }

    public Bitmap getScaledBmp() {
        return scaledBmp;
    }


    /* Resize function for bmps */
    byte[] resizeImage(byte[] input, int height, int width) {
        Bitmap original = BitmapFactory.decodeByteArray(input , 0, input.length);
        Bitmap resized = Bitmap.createScaledBitmap(original, height, width, true);

        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 100, blob);

        return blob.toByteArray();
    }
}