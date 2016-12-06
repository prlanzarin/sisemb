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
 * A basic Camera preview class
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    public Bitmap scaledBmp = null;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // creates a surface onto which we shall exhibit the camera preview image
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    // rotate/etc events
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // restart preview with changed settings (e.g rotated view)
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /* takes a snap photo from the camera preview data. Sets the scaledBmp field
       from a YUVImage that is generated from the byte array. The byte array (@data)
       comes from the setOneShotPreviewCallback, which captures changes in the camera
       preview media flow. The image is compressed to allow conversion to bitmap, which
       is useful for comparison later.
      */
    public void takeSnapPhoto() {
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                byte[] imgByteArray = null;
                // we are gathering the camera dimensions and preview image format here
                Camera.Parameters parameters = camera.getParameters();
                int imgFormat = parameters.getPreviewFormat();
                if (imgFormat == ImageFormat.NV21
                        || imgFormat == ImageFormat.YUY2
                        || imgFormat == ImageFormat.NV16) {
                    int width = parameters.getPreviewSize().width;
                    int height = parameters.getPreviewSize().height;
                    // image extraction from preview byte array
                    YuvImage yuv_image = new YuvImage(data, imgFormat, width, height, null);
                    // image compression (rect is for squaring part of the image.
                    // in this case, we want the whole image to be compressed
                    Rect squaringFactor = new Rect(0, 0, width, height);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    yuv_image.compressToJpeg(squaringFactor, 100, outputStream);
                    imgByteArray = outputStream.toByteArray();
                    // we convert the byte array to bitmap
                    scaledBmp = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray.length);
                }
            }
        });
    }

    public Bitmap getScaledBmp() {
        return scaledBmp;
    }
}