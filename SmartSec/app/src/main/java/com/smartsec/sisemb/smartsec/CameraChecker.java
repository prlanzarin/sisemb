package com.smartsec.sisemb.smartsec;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

public class CameraChecker extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private Camera mCamera;
    private CameraPreview mPreview;
    private boolean CURRENT_PICTURE = false;
    private byte[] img1 = null, img2 = null;
    private Bitmap sbmp1 = null, sbmp2 = null;
    EditText txtphoneNo;
    String phoneNo;
    String message = "SmartSec: different beahaviour detected at the camera.";

    ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private boolean match;
    Calendar lastSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_checker);

        txtphoneNo = (EditText) findViewById(R.id.editText);

        /* checks for camera permissions */
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            }
        }

        // start preview with new settings
        try {
            // Create an instance of Camera
            mCamera = getCameraInstance();
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Image comparison scheduler. It takes a picture each X seconds and compare
        // then using the compare() function which sets the @match variable
        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        mPreview.takeSnapPhoto();
                        Log.d(TAG, "TAKING SNAP PHOTO");
                        if(!CURRENT_PICTURE) {
                            Log.d(TAG, "setting pic 1");
                            sbmp1 = mPreview.getScaledBmp();
                            CURRENT_PICTURE = true;
                        }
                        else {
                            Log.d(TAG, "setting pic 2");
                            sbmp2 = mPreview.getScaledBmp();
                            CURRENT_PICTURE = false;
                        }

                        // comparison of bitmaps
                        if(sbmp1 != null && sbmp2 != null) {
                            compare();
                            if(!match) {
                                // sending SMS
                                if(canSendSMS()) {
                                    sendSMSMessage();
                                    Log.d(TAG, "SMS");
                                }
                                Log.d(TAG, "DIFFERENT IMAGES!");
                            }

                        }
                    }
                }, 0, 1, TimeUnit.SECONDS);
    }

    // checks if it is possible to send SMS within the specified timeframe
    protected boolean canSendSMS() {
        Calendar c = Calendar.getInstance();

        if(lastSent != null) {
            if (c.getTimeInMillis() - lastSent.getTimeInMillis() > 30000) {
                lastSent = c;
                return true;
            } else {
                return false;
            }
        }
        else {
            lastSent = c;
            return true;
        }
    }

    // sends a SMS message warning the desired user of movement detection
    protected void sendSMSMessage() {
        phoneNo = txtphoneNo.getText().toString();
        // permission checking
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
        else if (!phoneNo.isEmpty()){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }

    // gets a android camera instance
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e) {
            System.out.println("blamjjjh");
        }
        return c;
    }

    public void releasec() {
        mCamera.release();
    }

    @Override
    protected void onStop() {

        super.onStop();
        releasec();
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            // seding SMS message
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNo, null, message, null, null);
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    // compare the two images in this object.
    public void compare() {
        int blocksx = (int)(sbmp1.getWidth() / 8);
        int blocksy = (int)(sbmp2.getHeight() / 8);
        this.match = true;
        // loop through the image comparing blocks of 8x8 pixels taking into account their average
        // brightness difference.
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Bitmap temp1 = sbmp1;
                Bitmap temp2 = sbmp2;
                Bitmap croppedBitmap1 = Bitmap.createBitmap(temp1, x*blocksx, y*blocksy, blocksx - 1, blocksy - 1);
                Bitmap croppedBitmap2 = Bitmap.createBitmap(temp2, x*blocksx, y*blocksy, blocksx - 1, blocksy - 1);
                int b1 = getAverageBrightness(croppedBitmap1, 1);
                int b2 = getAverageBrightness(croppedBitmap2, 1);
                int diff = Math.abs(b1 - b2);
                // brightness thresold comparison
                if (diff > 23)
                    this.match = false;
            }
        }
    }

    // method to get average brightness from image
    public int getAverageBrightness(android.graphics.Bitmap bitmap, int pixelSpacing) {
        int R = 0; int G = 0; int B = 0;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int n = 0;
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i += pixelSpacing) {
            int color = pixels[i];
            R += Color.red(color);
            G += Color.green(color);
            B += Color.blue(color);
            n++;
        }
        return (R + B + G) / (n * 3);
    }
}
