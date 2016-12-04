package com.smartsec.sisemb.smartsec;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import static android.content.ContentValues.TAG;

public class CameraChecker extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA =0 ;
    private Camera mCamera;
    private CameraPreview mPreview;
    int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_checker);

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
    }

    public static Camera getCameraInstance()
    {
        Camera c = null;
        try
        {
            c = Camera.open();}
        catch (Exception e)
        { System.out.println("blamjjjh");}
        return c;
    }
    public void releasec(){
        mCamera.release();
    }

    @Override
    protected void onStop()
    {

        super.onStop();
        releasec();
    }
}
