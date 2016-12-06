package com.smartsec.sisemb.smartsec;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    EditText txtphoneNo;
    String phoneNo;
    String message = "SmartSec: different beahaviour detected at the camera.";

    ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

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
                            img1 = mPreview.getImgByteArray();
                            CURRENT_PICTURE = true;
                        }
                        else {
                            img2 = mPreview.getImgByteArray();
                            CURRENT_PICTURE = false;
                        }

                        if(img1 != null & img2 != null) {
                            // Comparison using a simple Array.equals (to compare byte arrays) without resize always
                            // shows difference. Will test with resizing to 8x8 to see what's up
                            /*if(!Arrays.equals(img1, img2)) {
                                Log.d(TAG, "DIFFERENT IMAGES!");
                            }*/
                             // Comparison option using the link I found on stackoverflow, still does not work
                            Bitmap bmp1 = BitmapFactory.decodeByteArray(img1 , 0, img1.length);
                            Bitmap bmp2 = BitmapFactory.decodeByteArray(img2 , 0, img2.length);
                            Log.d(TAG, "oi");
                            if(SameAs(bmp1, bmp2)) {
                                Log.d(TAG, "SAME IMAGES!");
                            }
                        }
                    }
                }, 0, 1, TimeUnit.SECONDS);
    }

    /* BMP comparison method I found on stackoverflow, not sure if it works properly */
    boolean SameAs(Bitmap A, Bitmap B) {

        // Different types of image
        if(A.getConfig() != B.getConfig())
            return false;

        // Different sizes
        if (A.getWidth() != B.getWidth())
            return false;
        if (A.getHeight() != B.getHeight())
            return false;

        // Allocate arrays - OK because at worst we have 3 bytes + Alpha (?)
        int w = A.getWidth();
        int h = A.getHeight();

        int[] argbA = new int[w*h];
        int[] argbB = new int[w*h];

        A.getPixels(argbA, 0, w, 0, 0, w, h);
        B.getPixels(argbB, 0, w, 0, 0, w, h);

        int[] histogramA = new int[255];
        int[] histogramB = new int[255];
        int[] diffHistogram = new int[255];

        for(int i = 0; i < argbA.length; i++) {
            int red = (argbA[i] & 0xFF000000) >> 6;
            int green = (argbA[i] & 0x00FF0000) >> 4;
            int blue = (argbA[i] & 0x0000FF00) >> 2;
            int gscale = (red + green + blue) / 3;
            histogramA[gscale] ++;
        }

        for(int i = 0; i < argbB.length; i++) {
            int red = (argbB[i] & 0xFF000000) >> 6;
            int green = (argbB[i] & 0x00FF0000) >> 4;
            int blue = (argbB[i] & 0x0000FF00) >> 2;
            int gscale = (red + green + blue) / 3;
            histogramB[gscale] ++;
        }

        int avg = 0;
        for(int i = 0; i < 255; i++)
            avg += Math.abs(histogramA[i] - histogramB[i]);
        avg = avg / 255;

        Log.d(TAG, Integer.toString(avg));

        if(avg > 5)
            return true;
        else
            return false;

        /*
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
        */
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

}
