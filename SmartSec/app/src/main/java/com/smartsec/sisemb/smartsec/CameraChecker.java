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

import java.sql.Time;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;
import static java.util.concurrent.TimeUnit.SECONDS;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_checker);

        txtphoneNo = (EditText) findViewById(R.id.editText);

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

        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        mPreview.takeSnapPhoto();
                        Log.d(TAG, "TAKING SNAP PHOTO");
                        if(!CURRENT_PICTURE) {
                            Log.d(TAG, "setting pic 1");
                            img1 = mPreview.getImgByteArray();
                            sbmp1 = mPreview.getScaledBmp();
                            CURRENT_PICTURE = true;
                        }
                        else {
                            Log.d(TAG, "setting pic 2");
                            img2 = mPreview.getImgByteArray();
                            sbmp2 = mPreview.getScaledBmp();
                            CURRENT_PICTURE = false;
                        }

                        if(img1 != null & img2 != null && sbmp1 != null && sbmp2 != null) {
                            // Comparison using a simple Array.equals (to compare byte arrays) without resize always
                            // shows difference. Will test with resizing to 8x8 to see what's up
                            //if(!Arrays.equals(img1, img2)) {
                            //    Log.d(TAG, "DIFFERENT IMAGES!");
                            //}
                            // Comparison option using the link I found on stackoverflow, still does not work
                            //Bitmap bmp1 = BitmapFactory.decodeByteArray(img1 , 0, img1.length);
                            //Bitmap bmp2 = BitmapFactory.decodeByteArray(img2 , 0, img2.length);
                            compare();
                            if(!match) {
                                Log.d(TAG, "DIFFERENT IMAGES!");
                            }

                        }
                    }
                }, 0, 1, TimeUnit.SECONDS);
    }

    /* BMP comparison method I found on stackoverflow, not sure if it works properly */
    boolean SameAs(Bitmap A, Bitmap B) {

        // Allocate arrays - OK because at worst we have 3 bytes + Alpha (?)
        int w = A.getWidth();
        int h = A.getHeight();

        int[] argbA = new int[w*h];
        int[] argbB = new int[w*h];

        A.getPixels(argbA, 0, w, 0, 0, w, h);
        B.getPixels(argbB, 0, w, 0, 0, w, h);

        // Alpha channel special check
        if (A.getConfig() == Bitmap.Config.ALPHA_8) {
            // in this case we have to manually compare the alpha channel as the rest is garbage.
            final int length = w * h;
            for (int i = 0 ; i < length ; i++) {
                if ((argbA[i] & 0xFF000000) != (argbB[i] & 0xFF000000)) {
                    return false;
                }
            }
            return true;
        }

        return Arrays.equals(argbA, argbB);
    }

    protected void sendSMSMessage() {
        phoneNo = txtphoneNo.getText().toString();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
    }

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
        // convert to gray images.
        // how big are each section
        int blocksx = (int)(sbmp1.getWidth() / 8);
        int blocksy = (int)(sbmp2.getHeight() / 8);
        // set to a match by default, if a change is found then flag non-match
        this.match = true;
        // loop through whole image and compare individual blocks of images
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Bitmap temp1 = sbmp1;
                Bitmap temp2 = sbmp2;
                Bitmap croppedBitmap1 = Bitmap.createBitmap(temp1, x*blocksx, y*blocksy, blocksx - 1, blocksy - 1);
                Bitmap croppedBitmap2 = Bitmap.createBitmap(temp2, x*blocksx, y*blocksy, blocksx - 1, blocksy - 1);
                int b1 = getAverageBrightness(croppedBitmap1, 1);
                int b2 = getAverageBrightness(croppedBitmap2, 1);
                int diff = Math.abs(b1 - b2);
                if (diff > 10) { // the difference in a certain region has passed the threshold value of factorA
                    // draw an indicator on the change image to show where change was detected.
                    this.match = false;
                }
            }
        }
    }

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
