package com.smartsec.sisemb.smartsec;

import android.Manifest;
import android.content.pm.PackageManager;
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
                    }
                }, 0, 1, TimeUnit.SECONDS);
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
