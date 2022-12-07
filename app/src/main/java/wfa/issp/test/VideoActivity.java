package wfa.issp.test;


/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */



import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Arrays;

import com.serenegiant.common.BaseActivity;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.utils.ViewAnimationHelper;
import com.serenegiant.widget.CameraViewInterface;

public final class VideoActivity extends BaseActivity implements CameraDialog.CameraDialogParent,View.OnTouchListener {
    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "MainActivity";

    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     *  by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private static final boolean USE_SURFACE_ENCODER = false;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_WIDTH = 640;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_HEIGHT = 480;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 1;

    protected static final int SETTINGS_HIDE_DELAY_MS = 2500;
    /**
     * for accessing USB
     */
    private USBMonitor mUSBMonitor;
    /**
     * Handler to execute camera related methods sequentially on private thread
     */
    private UVCCameraHandler mCameraHandler;
    /**
     * for camera preview display
     */
    private CameraViewInterface mUVCCameraView;
    /**
     * for open&start / stop&close camera preview
     */
    private ToggleButton mCameraButton;
    /**
     * button for start/stop recording
     */
    private ImageButton mCaptureButton;

    ImageView imageView;

    private View mBrightnessButton, mContrastButton;
    private View mResetButton;
    private View mToolsLayout, mValueLayout;
    private SeekBar mSettingSeekbar;

    // Intent check-values
    static final int GET_INPUT_DATA = 1;

    Button confirm1Button;
    Button confirm2Button;
    Button mPreset1Button;
    Button mPreset2Button;
    Button mPreset3Button;
    Button mPreset4Button;
    Button gobackButton;

    EditText editText1;
    EditText editText2;

    Float dX;
    Float dY;
    int lastAction;

    boolean VertActive = true; //lines can move left right
    boolean HorActive = false; //lines can move up down

    double pixelToCmRatio;
    private  double referenceObjectSide = 1;

    private  String unitOfMeasurement = "mm";

    public static RectF calculeRectOnScreen(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new RectF(location[0], location[1], location[0] + view.getMeasuredWidth(), location[1] + view.getMeasuredHeight());
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.v(TAG, "onCreate:");
        setContentView(R.layout.activity_video);
        mCameraButton = (ToggleButton)findViewById(R.id.camera_button);
        mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
        final View view = findViewById(R.id.camera_view);
        //view.setOnLongClickListener(mOnLongClickListener);
        mUVCCameraView = (CameraViewInterface)view;
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);

        mBrightnessButton = findViewById(R.id.brightness_button);
        mBrightnessButton.setOnClickListener(mOnClickListener);
        mContrastButton = findViewById(R.id.contrast_button);
        mContrastButton.setOnClickListener(mOnClickListener);
        mResetButton = findViewById(R.id.reset_button);
        mResetButton.setOnClickListener(mOnClickListener);
        mSettingSeekbar = (SeekBar)findViewById(R.id.setting_seekbar);
        mSettingSeekbar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        gobackButton = findViewById(R.id.gobackButton);


        mToolsLayout = findViewById(R.id.tools_layout);
        mToolsLayout.setVisibility(View.INVISIBLE);
        mValueLayout = findViewById(R.id.value_layout);
        mValueLayout.setVisibility(View.INVISIBLE);

        Switch leftHandSwitch = (Switch) findViewById(R.id.lefthand_button);
        TextView leftHandText = findViewById(R.id.lefthand_text);




        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);

        // Initialize UI
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle("");
        setSupportActionBar(myToolbar);

        View draggableViewVert1 = findViewById(R.id.dragViewVert1);

        View draggableViewVert2 = findViewById(R.id.dragViewVert2);

        View draggableViewVertBold1 = findViewById(R.id.dragViewVertBold1);
        draggableViewVertBold1.setOnTouchListener(this);

        View draggableViewVertBold2 = findViewById(R.id.dragViewVertBold2);
        draggableViewVertBold2.setOnTouchListener(this);

        View draggableViewHor1 = findViewById(R.id.dragViewHor1);

        View draggableViewHor2 = findViewById(R.id.dragViewHor2);

        View draggableViewHor3 = findViewById(R.id.dragViewHor3);

        View draggableViewHorBold1 = findViewById(R.id.dragViewHorBold1);
        draggableViewHorBold1.setOnTouchListener(this);

        View draggableViewHorBold2 = findViewById(R.id.dragViewHorBold2);
        draggableViewHorBold2.setOnTouchListener(this);

        View draggableViewHorBold3 = findViewById(R.id.dragViewHorBold3);
        draggableViewHorBold3.setOnTouchListener(this);

        SharedPreferences sharedPref = VideoActivity.this.getPreferences(Context.MODE_PRIVATE);


        editText1  =  (EditText) findViewById(R.id.editText1);
        editText2  =  (EditText) findViewById(R.id.editText2);

        editText1.setText(sharedPref.getString("preset1Name",""));
        editText2.setText(sharedPref.getString("preset2Name",""));

        gobackButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                VideoActivity.this.recreate();
            }
        });

        confirm1Button = (Button) findViewById(R.id.referenceButton);
        confirm1Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RectF firstRect = calculeRectOnScreen(draggableViewVert1);
                RectF otherRect = calculeRectOnScreen(draggableViewVert2);
                float distance = 0;
                if(firstRect.left < otherRect.left)
                {
                    distance = Math.abs(firstRect.right - otherRect.left);
                }
                else if(firstRect.left > otherRect.left)
                {
                    distance = Math.abs(otherRect.right - firstRect.left);
                }
                else if(firstRect.left == otherRect.left)
                {
                    distance = 0;
                }

                pixelToCmRatio = referenceObjectSide / distance;

                draggableViewVert1.setVisibility(View.GONE);
                draggableViewVert2.setVisibility(View.GONE);
                draggableViewVertBold1.setVisibility(View.GONE);
                draggableViewVertBold2.setVisibility(View.GONE);
                draggableViewHor1.setVisibility(View.VISIBLE);
                draggableViewHor2.setVisibility(View.VISIBLE);
                draggableViewHor3.setVisibility(View.VISIBLE);
                draggableViewHorBold1.setVisibility(View.VISIBLE);
                draggableViewHorBold2.setVisibility(View.VISIBLE);
                draggableViewHorBold3.setVisibility(View.VISIBLE);

                VertActive = false;
                HorActive = true;
                confirm1Button.setVisibility(View.GONE);
                confirm2Button.setVisibility(View.VISIBLE);
                gobackButton.setVisibility(View.VISIBLE);
                leftHandText.setVisibility(View.VISIBLE);
                leftHandSwitch.setVisibility(View.VISIBLE);
            }
        });





        confirm2Button = (Button) findViewById(R.id.measureButton);
        confirm2Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                RectF firstRect = calculeRectOnScreen(draggableViewHor1);
                RectF otherRect = calculeRectOnScreen(draggableViewHor2);
                RectF thirdRect = calculeRectOnScreen(draggableViewHor3);

                float distance = 0;

                float[] topsBottoms = {firstRect.top, firstRect.bottom, otherRect.top,
                        otherRect.bottom, thirdRect.top, thirdRect.bottom};
                Arrays.sort(topsBottoms); //first value should be highest on the screen

                float BaselineToNeedleHeight = topsBottoms[2] - topsBottoms[1];
                float NeedleHeight = topsBottoms[4] - topsBottoms[2];

                double realHeightBaseline = BaselineToNeedleHeight * pixelToCmRatio;
                double realHeightNeedle = NeedleHeight * pixelToCmRatio;

                String formattedHeightBaseline = String.format("%.2f",realHeightBaseline);
                String formattedHeightSecond = String.format("%.2f",realHeightNeedle);

                Toast.makeText(VideoActivity.this, "Baseline to tip: "
                        +formattedHeightBaseline+" "+unitOfMeasurement+"\n"+
                        "Tip to bottom: "+formattedHeightSecond+" "+
                        unitOfMeasurement, Toast.LENGTH_LONG).show();
                //Toast.makeText(VideoActivity.this, "Bottom to tip:"+formattedHeightSecond+"mm", Toast.LENGTH_SHORT).show();
            }
        });

        mPreset1Button = (Button) findViewById(R.id.presetButton1);
        mPreset1Button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = VideoActivity.this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();


                if(VertActive)
                {
                    editor.putFloat("Vertical1", draggableViewVert1.getX()); //zapisujemy pionowe kreski do preferences
                    editor.putFloat("Vertical2", draggableViewVert2.getX());
                    editor.putFloat("Vertical1Bold", draggableViewVertBold1.getX());
                    editor.putFloat("Vertical2Bold", draggableViewVertBold2.getX());
                    editor.putString("preset1Name", String.valueOf(editText1.getText()));
                    editor.commit();
                }
                else if(HorActive)
                {
                    editor.putFloat("Horizontal1", draggableViewHor1.getY()); //zapisujemy poziome kreski do preferences
                    editor.putFloat("Horizontal2", draggableViewHor2.getY());
                    editor.putFloat("Horizontal3", draggableViewHor3.getY());
                    editor.putFloat("Horizontal1Bold", draggableViewHorBold1.getY()); //zapisujemy poziome kreski do preferences
                    editor.putFloat("Horizontal2Bold", draggableViewHorBold2.getY());
                    editor.putFloat("Horizontal3Bold", draggableViewHorBold3.getY());
                    editor.putString("preset1Name", String.valueOf(editText1.getText()));
                    editor.commit();
                }
            }
        });

        mPreset2Button = (Button )findViewById(R.id.presetButton2);
        mPreset2Button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = VideoActivity.this.getPreferences(Context.MODE_PRIVATE);
                if(VertActive) {
                    Float Vert1X = sharedPref.getFloat("Vertical1", 0);
                    Float Vert2X = sharedPref.getFloat("Vertical2", 0);
                    draggableViewVert1.setX(Vert1X);
                    draggableViewVert2.setX(Vert2X);

                    Float Vert1XBold = sharedPref.getFloat("Vertical1Bold", 0);
                    Float Vert2XBold = sharedPref.getFloat("Vertical2Bold", 0);
                    draggableViewVertBold1.setX(Vert1XBold);
                    draggableViewVertBold2.setX(Vert2XBold);
                }
                else if(HorActive)
                {
                    Float Hor1Y = sharedPref.getFloat("Horizontal1", 0);
                    Float Hor2Y = sharedPref.getFloat("Horizontal2", 0);
                    Float Hor3Y = sharedPref.getFloat("Horizontal3", 0);
                    draggableViewHor1.setY(Hor1Y);
                    draggableViewHor2.setY(Hor2Y);
                    draggableViewHor3.setY(Hor3Y);

                    Float Hor1YBold = sharedPref.getFloat("Horizontal1Bold", 0);
                    Float Hor2YBold = sharedPref.getFloat("Horizontal2Bold", 0);
                    Float Hor3YBold = sharedPref.getFloat("Horizontal3Bold", 0);
                    draggableViewHorBold1.setY(Hor1YBold);
                    draggableViewHorBold2.setY(Hor2YBold);
                    draggableViewHorBold3.setY(Hor3YBold);
                }
            }
        });



        mPreset3Button = (Button) findViewById(R.id.presetButton3);
        mPreset3Button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = VideoActivity.this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();


                if(VertActive)
                {
                    editor.putFloat("Vertical1Prim", draggableViewVert1.getX()); //zapisujemy pionowe kreski do preferences
                    editor.putFloat("Vertical2Prim", draggableViewVert2.getX());
                    editor.putFloat("Vertical1BoldPrim", draggableViewVertBold1.getX());
                    editor.putFloat("Vertical2BoldPrim", draggableViewVertBold2.getX());
                    editor.putString("preset2Name", String.valueOf(editText2.getText()));
                    editor.commit();
                }
                else if(HorActive)
                {
                    editor.putFloat("Horizontal1Prim", draggableViewHor1.getY()); //zapisujemy poziome kreski do preferences
                    editor.putFloat("Horizontal2Prim", draggableViewHor2.getY());
                    editor.putFloat("Horizontal3Prim", draggableViewHor3.getY());
                    editor.putFloat("Horizontal1BoldPrim", draggableViewHorBold1.getY()); //zapisujemy poziome kreski do preferences
                    editor.putFloat("Horizontal2BoldPrim", draggableViewHorBold2.getY());
                    editor.putFloat("Horizontal3BoldPrim", draggableViewHorBold3.getY());
                    editor.putString("preset2Name", String.valueOf(editText2.getText()));
                    editor.commit();
                }
            }
        });

        mPreset4Button = (Button )findViewById(R.id.presetButton4);
        mPreset4Button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = VideoActivity.this.getPreferences(Context.MODE_PRIVATE);
                if(VertActive) {
                    Float Vert1X = sharedPref.getFloat("Vertical1Prim", 0);
                    Float Vert2X = sharedPref.getFloat("Vertical2Prim", 0);
                    draggableViewVert1.setX(Vert1X);
                    draggableViewVert2.setX(Vert2X);

                    Float Vert1XBold = sharedPref.getFloat("Vertical1BoldPrim", 0);
                    Float Vert2XBold = sharedPref.getFloat("Vertical2BoldPrim", 0);
                    draggableViewVertBold1.setX(Vert1XBold);
                    draggableViewVertBold2.setX(Vert2XBold);
                }
                else if(HorActive)
                {
                    Float Hor1Y = sharedPref.getFloat("Horizontal1Prim", 0);
                    Float Hor2Y = sharedPref.getFloat("Horizontal2Prim", 0);
                    Float Hor3Y = sharedPref.getFloat("Horizontal3Prim", 0);
                    draggableViewHor1.setY(Hor1Y);
                    draggableViewHor2.setY(Hor2Y);
                    draggableViewHor3.setY(Hor3Y);

                    Float Hor1YBold = sharedPref.getFloat("Horizontal1BoldPrim", 0);
                    Float Hor2YBold = sharedPref.getFloat("Horizontal2BoldPrim", 0);
                    Float Hor3YBold = sharedPref.getFloat("Horizontal3BoldPrim", 0);
                    draggableViewHorBold1.setY(Hor1YBold);
                    draggableViewHorBold2.setY(Hor2YBold);
                    draggableViewHorBold3.setY(Hor3YBold);
                }
            }
        });

        leftHandSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) draggableViewHorBold1.getLayoutParams();
                RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) draggableViewHorBold2.getLayoutParams();
                RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) draggableViewHorBold3.getLayoutParams();
                if (isChecked)
                {
                    params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    params2.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    params3.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    params2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    params3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    draggableViewHorBold1.setLayoutParams(params);
                    draggableViewHorBold2.setLayoutParams(params2);
                    draggableViewHorBold3.setLayoutParams(params3);
                }
                else
                {
                    params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    params2.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    params3.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    params2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    params3.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

                    draggableViewHorBold1.setLayoutParams(params);
                    draggableViewHorBold2.setLayoutParams(params2);
                    draggableViewHorBold3.setLayoutParams(params3);
                }
            }
        });


    }





    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_MOVE:
                if(VertActive)
                {
                    v.setX(event.getRawX() + dX);
                    View draggableViewVert1 = findViewById(R.id.dragViewVert1);
                    View draggableViewVertBold1 = findViewById(R.id.dragViewVertBold1);
                    draggableViewVert1.setX(draggableViewVertBold1.getX());

                    View draggableViewVert2 = findViewById(R.id.dragViewVert2);
                    View draggableViewVertBold2 = findViewById(R.id.dragViewVertBold2);
                    draggableViewVert2.setX(draggableViewVertBold2.getX());
                }
                if(HorActive)
                {
                    v.setY(event.getRawY() + dY);
                    View draggableViewHor1 = findViewById(R.id.dragViewHor1);
                    View draggableViewHorBold1 = findViewById(R.id.dragViewHorBold1);
                    draggableViewHor1.setY(draggableViewHorBold1.getY());

                    View draggableViewHor2 = findViewById(R.id.dragViewHor2);
                    View draggableViewHorBold2 = findViewById(R.id.dragViewHorBold2);
                    draggableViewHor2.setY(draggableViewHorBold2.getY());

                    View draggableViewHor3 = findViewById(R.id.dragViewHor3);
                    View draggableViewHorBold3 = findViewById(R.id.dragViewHorBold3);
                    draggableViewHor3.setY(draggableViewHorBold3.getY());
                }
                lastAction = MotionEvent.ACTION_MOVE;
                break;

            case MotionEvent.ACTION_DOWN:
                if(VertActive)
                {
                    dX = v.getX() - event.getRawX();
                }
                if(HorActive)
                {
                    dY = v.getY() - event.getRawY();
                }
                lastAction = MotionEvent.ACTION_DOWN;
                break;

            case MotionEvent.ACTION_UP:
                if (lastAction == MotionEvent.ACTION_DOWN)
                    Toast.makeText(VideoActivity.this, "Clicked!", Toast.LENGTH_SHORT).show();
                break;

            default:
                return false;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) Log.v(TAG, "onStart:");
        mUSBMonitor.register();
        if (mUVCCameraView != null)
            mUVCCameraView.onResume();
    }

    @Override
    protected void onStop() {
        if (DEBUG) Log.v(TAG, "onStop:");
        mCameraHandler.close();
        if (mUVCCameraView != null)
            mUVCCameraView.onPause();
        setCameraButton(false);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.v(TAG, "onDestroy:");
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraView = null;
        mCameraButton = null;
        mCaptureButton = null;
        super.onDestroy();
    }

    /**
     * event handler when click camera / capture button
     */
    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {

                case R.id.brightness_button:
                    showSettings(UVCCamera.PU_BRIGHTNESS);
                    break;
                case R.id.contrast_button:
                    showSettings(UVCCamera.PU_CONTRAST);
                    break;
                case R.id.reset_button:
                    resetSettings();
                    break;
            }
        }
    };

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener
            = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
            switch (compoundButton.getId()) {
                case R.id.camera_button:
                    if (isChecked && !mCameraHandler.isOpened()) {
                        CameraDialog.showDialog(VideoActivity.this);
                    } else {
                        mCameraHandler.close();
                        setCameraButton(false);
                    }
                    break;
            }
        }
    };

    /**
     * capture still image when you long click on preview image(not on buttons)
     */
     /*private final OnLongClickListener mOnLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(final View view) {
           switch (view.getId()) {
                case R.id.camera_view:
                    if (mCameraHandler.isOpened()) {
                        if (checkPermissionWriteExternalStorage()) {
                            mCameraHandler.captureStill();
                        }
                        return true;
                    }
            }
            return false;
        }
    };*/

    private void setCameraButton(final boolean isOn) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCameraButton != null) {
                    try {
                        mCameraButton.setOnCheckedChangeListener(null);
                        mCameraButton.setChecked(isOn);
                    } finally {
                        mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
                    }
                }
                if (!isOn && (mCaptureButton != null)) {
                    mCaptureButton.setVisibility(View.INVISIBLE);
                }
            }
        }, 0);
        updateItems();
    }

    private void startPreview() {
        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
        mCameraHandler.startPreview(new Surface(st));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               // mCaptureButton.setVisibility(View.VISIBLE);
            }
        });
        updateItems();
    }

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(VideoActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG) Log.v(TAG, "onConnect:");
            mCameraHandler.open(ctrlBlock);
            startPreview();
            updateItems();
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:");
            if (mCameraHandler != null) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCameraHandler.close();
                    }
                }, 0);
                setCameraButton(false);
                updateItems();
            }
        }
        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(VideoActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
            setCameraButton(false);
        }
    };

    /**
     * to access from CameraDialog
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
        if (canceled) {
            setCameraButton(false);
        }
    }

    //================================================================================
    private boolean isActive() {
        return mCameraHandler != null && mCameraHandler.isOpened();
    }

    private boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    private int getValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    private int setValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    private int resetValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }

    private void updateItems() {
        runOnUiThread(mUpdateItemsOnUITask, 100);
    }

    private final Runnable mUpdateItemsOnUITask = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;
            mToolsLayout.setVisibility(visible_active);
            mBrightnessButton.setVisibility(
                    checkSupportFlag(UVCCamera.PU_BRIGHTNESS)
                            ? visible_active : View.INVISIBLE);
            mContrastButton.setVisibility(
                    checkSupportFlag(UVCCamera.PU_CONTRAST)
                            ? visible_active : View.INVISIBLE);
        }
    };

    private int mSettingMode = -1;
    /**
     * 設定画面を表示
     * @param mode
     */
    private final void showSettings(final int mode) {
        if (DEBUG) Log.v(TAG, String.format("showSettings:%08x", mode));
        hideSetting(false);
        if (isActive()) {
            switch (mode) {
                case UVCCamera.PU_BRIGHTNESS:
                case UVCCamera.PU_CONTRAST:
                    mSettingMode = mode;
                    mSettingSeekbar.setProgress(getValue(mode));
                    ViewAnimationHelper.fadeIn(mValueLayout, -1, 0, mViewAnimationListener);
                    break;
            }
        }
    }

    private void resetSettings() {
        if (isActive()) {
            switch (mSettingMode) {
                case UVCCamera.PU_BRIGHTNESS:
                case UVCCamera.PU_CONTRAST:
                    mSettingSeekbar.setProgress(resetValue(mSettingMode));
                    break;
            }
        }
        mSettingMode = -1;
        ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener);
    }

    /**
     * 設定画面を非表示にする
     * @param fadeOut trueならばフェードアウトさせる, falseなら即座に非表示にする
     */
    protected final void hideSetting(final boolean fadeOut) {
        removeFromUiThread(mSettingHideTask);
        if (fadeOut) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener);
                }
            }, 0);
        } else {
            try {
                mValueLayout.setVisibility(View.GONE);
            } catch (final Exception e) {
                // ignore
            }
            mSettingMode = -1;
        }
    }

    protected final Runnable mSettingHideTask = new Runnable() {
        @Override
        public void run() {
            hideSetting(true);
        }
    };

    /**
     * 設定値変更用のシークバーのコールバックリスナー
     */
    private final SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
            // 設定が変更された時はシークバーの非表示までの時間を延長する
            if (fromUser) {
                runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
            }
        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(final SeekBar seekBar) {
            // シークバーにタッチして値を変更した時はonProgressChangedへ
            // 行かないみたいなのでここでも非表示までの時間を延長する
            runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
            if (isActive() && checkSupportFlag(mSettingMode)) {
                switch (mSettingMode) {
                    case UVCCamera.PU_BRIGHTNESS:
                    case UVCCamera.PU_CONTRAST:
                        setValue(mSettingMode, seekBar.getProgress());
                        break;
                }
            }	// if (active)
        }
    };

    private final ViewAnimationHelper.ViewAnimationListener
            mViewAnimationListener = new ViewAnimationHelper.ViewAnimationListener() {
        @Override
        public void onAnimationStart(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
        }

        @Override
        public void onAnimationEnd(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
            final int id = target.getId();
            switch (animationType) {
                case ViewAnimationHelper.ANIMATION_FADE_IN:
                case ViewAnimationHelper.ANIMATION_FADE_OUT:
                {
                    final boolean fadeIn = animationType == ViewAnimationHelper.ANIMATION_FADE_IN;
                    if (id == R.id.value_layout) {
                        if (fadeIn) {
                            runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
                        } else {
                            mValueLayout.setVisibility(View.GONE);
                            mSettingMode = -1;
                        }
                    } else if (!fadeIn) {
//					target.setVisibility(View.GONE);
                    }
                    break;
                }
            }
        }

        @Override
        public void onAnimationCancel(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* If data coming from SettingsActivity exists, the method sets variable values accordingly.
          If a value doesn't exist, the previous value for the variable is selected. */
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == GET_INPUT_DATA) {
            Log.v(TAG, "Requestcode OK.");
            if(resultCode == RESULT_OK)
            {
                referenceObjectSide = data.getDoubleExtra("referenceObjectSide",referenceObjectSide);

            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // Create a new intent and pass parameters for settingsActivity when Settings button is clicked
                Log.d("onOptionsItemSelected", "Action_settings selected.");
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra("referenceObjectSide", Double.toString(referenceObjectSide));
                startActivityForResult(intent, 1);
                return true;



        }
        return true;
    }

}
