package se.olahallmarker.camerastream;

import se.olahallmarker.camerastream.UdpHandler;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;

// Android imports
import android.util.Log;
import android.widget.Button;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.List;
//import java.util.logging.Handler;

import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.os.Handler;
import android.view.Surface;
import java.util.Arrays;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.graphics.ImageFormat;
import android.util.Size;
import java.io.ByteArrayOutputStream;
import android.content.Intent;
import android.widget.Toast;
import android.content.SharedPreferences;


// Camera imports
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraCharacteristics.Key;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;


// Encoder imports
import android.media.MediaMuxer;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Looper;
import android.os.Message;
import android.widget.EditText;

import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;

public class CameraRecorder extends AppCompatActivity {

    private static UdpHandler udpClient;
    public int mVal = 1;
    private MediaPlayer mp1;
    private MediaPlayer mp2;

    private void onAlert(int requestNum, String buyer)
    {
        // TODO: change this to mp2... =)
        mp1.start();

        if (requestNum == 1)
        {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Reporter alert!!!");
            alertDialog.setMessage("Turn on your camera");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Log.d("Alert", "Topic accept");
                            udpClient.setResponse(1);
                        }
                    });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Log.d("Alert", "Topic denied");
                            udpClient.setResponse(2);
                        }
                    });
            alertDialog.show();
        }
        else if (requestNum == 2)
        {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("New offer!!!");
            alertDialog.setMessage(buyer + " wants to buy your feed");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Log.d("Alert", "Offer Yes");
                            udpClient.setResponse(3);
                        }
                    });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Log.d("Alert", "Offer No");
                            udpClient.setResponse(4);
                        }
                    });
            alertDialog.show();
        }
        else if (requestNum == 3)
        {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Session ended");
            alertDialog.setMessage("Camera can now be turned off");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Log.d("Alert", "Offer Yes");
                            udpClient.setResponse(5);
                        }
                    });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Log.d("Alert", "Offer No");
                            udpClient.setResponse(6);
                        }
                    });
            alertDialog.show();
        }


    }

    private void onMoney(int money)
    {
        // TODO
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mp1 = MediaPlayer.create(this, R.raw.pling);
        mp2 = MediaPlayer.create(this, R.raw.pling2);

        setContentView(R.layout.activity_camera_recorder);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup UDP handler
        {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String host = "192.168.43.224";
            //String host = "192.168.43.38";
            String port = "41234"; //prefs.getString("bazingaport", "9000");
            Integer server_port = Integer.parseInt(port);
            String contributor_id = prefs.getString("contributor-id", "cam");
            InetAddress server_host;

            try {
                server_host = InetAddress.getByName(host);
            }
            catch(java.net.UnknownHostException e){
                e.printStackTrace();
                return;
            }

            udpClient = new UdpHandler(server_host, server_port, contributor_id);
        }





        mMessagingThread = new Thread() {
            @Override
            public void run() {

                Log.i("Cam", "Messaging thread starts");

                while (true) {

                    // Get info from Udp Server
                    final int request = udpClient.request();
                    //Log.i("Udp", "Request=" + request + " val=" + mVal);

                    if (request == 1 || request == 2 || request == 3)
                    {
                        udpClient.setRequest(0);
                        runOnUiThread(new Runnable() {
                            private int requestNum = request;
                            private String buyer = udpClient.buyer();
                            @Override
                            public void run() {
                                onAlert(requestNum, buyer);
                            }
                        });
                    }
                    else if (request == 4)
                    {
                        udpClient.setRequest(0);
                        runOnUiThread(new Runnable() {
                            private int money = udpClient.money();
                            @Override
                            public void run() {
                                onMoney(money);
                            }
                        });
                    }


                    //Log.i("MSG", "messaging thread");
                    Sleep(100);
                }
            }
        };
        mMessagingThread.start();






        TextureView previewView = (TextureView) findViewById(R.id.preview);

        if (previewView == null)
        {
            Log.d("Cam", "Preview window is empty");
            return;
        }

        previewView.setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d("Cam", "Found texture available");
                mPreviewSurfaceTexture = surface;

                try {
                    initCamera();
                } catch (android.hardware.camera2.CameraAccessException e) {
                    Log.e("Cam", "initCamera Failed: " + e.getMessage());
                    return;
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                //mCamera.stopPreview();
                //mCamera.release();
                Log.d("Cam", "onSurfaceTextureDestroyed");
                if (mCamera != null) {
                    mCamera.close();
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // Update your view here!
                //Log.d("Cam", "onSurfaceTextureUpdated");
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Ignored, the Camera does all the work for us
                Log.d("Cam", "onSurfaceTextureSizeChanged");

                if (mCamera != null) {
                    mCamera.close();
                    mPreviewSurfaceTexture = surface;
                    try {
                        createCaptureSession();
                        //startCapture();
                    } catch (android.hardware.camera2.CameraAccessException e) {
                        Log.e("Cam", "initCamera Failed: " + e.getMessage());
                        return;
                    }
                }
            }
        });
    }

    public void onClick(View v) {
        Log.d("Cam", "onClick");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera_recorder, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, MyPreferencesActivity.class);
            startActivity(i);
            return true;
        }
        else if (id == R.id.action_about) {
            Toast toast = Toast.makeText(this, "Net Insignt Camera Streamer v0.1", Toast.LENGTH_SHORT);
            toast.show();
        }

        return super.onOptionsItemSelected(item);
    }

    private int STATE_IDLE = 0;
    private int STATE_RECORDING = 1;
    private int mCurrentState = STATE_IDLE;

    private int getStreamState() {
        return mCurrentState;
    }

    public void onRecord(View v) {

        // Using onRecord2 instead

        /*
        if (!(v instanceof Button))
        {
            Log.d("Cam", "not a button");
            return;
        }

        Log.d("Cam", "onRecord: " + mCurrentState);
        Button button = (Button) v;

        if (mCurrentState == STATE_IDLE) {
            button.setText("Stop");
            mCurrentState = STATE_RECORDING;
            Log.d("Cam", "onRecord - Set state to recording: " + mCurrentState);

            startMediaCodecRecording();
            try {
                startCapture();
            }
            catch(android.hardware.camera2.CameraAccessException e){
                Log.e("Cam", "initCamera Failed: " + e.getMessage());
                return;
            }
        } else {
            button.setText("Record");
            mCurrentState = STATE_IDLE;
            Log.d("Cam", "onRecord - Set state to idle: " + mCurrentState);

            // Drain the encoder?

            try {
                startCapture();
            }
            catch(android.hardware.camera2.CameraAccessException e){
                Log.e("Cam", "initCamera Failed: " + e.getMessage());
                return;
            }
        }

        */
    }





    public void onRecord2(View v) {

        if (!(v instanceof ImageButton))
        {
            Log.d("Cam", "not a button");
            return;
        }

        Log.d("Cam", "onRecord2: " + mCurrentState);
        ImageButton button = (ImageButton) v;

        mp1.start();

        if (mCurrentState == STATE_IDLE) {
            button.setImageResource(R.drawable.stop);

            mCurrentState = STATE_RECORDING;
            Log.d("Cam", "onRecord2 - Set state to recording: " + mCurrentState);

            startMediaCodecRecording();
            try {
                startCapture();
            }
            catch(android.hardware.camera2.CameraAccessException e){
                Log.e("Cam", "initCamera Failed: " + e.getMessage());
                return;
            }
        } else {
            //button.setText("Record");
            //button.setBackground("images_2");

            //Drawable replacer = getResources().getDrawable(R.drawable.images_2);
            //button.setEnabled(false);
            button.setImageResource(R.drawable.images_2);
            //button.destroyDrawingCache();
            //button.setBackground(replacer);
            //button.setEnabled(true);

            mCurrentState = STATE_IDLE;
            Log.d("Cam", "onRecord2 - Set state to idle: " + mCurrentState);

            // Drain the encoder?

            try {
                startCapture();
            }
            catch(android.hardware.camera2.CameraAccessException e){
                Log.e("Cam", "initCamera Failed: " + e.getMessage());
                return;
            }
        }


    }





    // Surfaces
    private SurfaceTexture mPreviewSurfaceTexture;
    private Surface mPreviewSurface;
    private Surface jpegCaptureSurface;
    private Surface mEncoderSurface;

    // Camera members
    private CameraManager mCameraManager;
    private CameraDevice mCamera = null;
    private CameraCaptureSession mSession;

    // Encoding members
    private MediaMuxer mMuxer;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private Thread mRecordingThread;
    private Thread mMessagingThread;
    private static final String MIME_TYPE = "video/avc"; // H.264 AVC encoding
    private static final int FRAME_RATE = 30; // 30fps
    private static final int IFRAME_INTERVAL = 1; // 1 seconds between I-frames
    private static final int TIMEOUT_USEC = 10000; // Timeout value 10ms.
    private byte[] mInitData;

    // Network members
    private DatagramSocket senderSocket = null;
    //private String SERVER_ADDRESS = "192.168.120.122";
    private String SERVER_ADDRESS = "192.168.1.6";


    private void initCamera() throws android.hardware.camera2.CameraAccessException {
        Log.d("Cam", "initCamera");

        // Create a CameraManager
        mCameraManager = (CameraManager) getSystemService(this.CAMERA_SERVICE);

        // Get camera characteristics
        String[] lCameraCharacteristics = mCameraManager.getCameraIdList();
        CameraCharacteristics lCC = null;
        String lCameraId = "";

        // Cycle through available cameras
        for (int i = 0; i < lCameraCharacteristics.length; i++) {
            Log.d("Cam", "Found cam: " + lCameraCharacteristics[i]);
            CameraCharacteristics lChar = mCameraManager.getCameraCharacteristics(lCameraCharacteristics[i]);
            List<Key<?>> lKeys = lChar.getKeys();

            for (int j = 0; j < lKeys.size(); j++) {
                Log.d("Cam", "Characteristic: " + lKeys.get(j) + " = " + lChar.get(lKeys.get(j)));
            }

            // Check camera facing
            if (lChar.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                lCameraId = lCameraCharacteristics[i];
                lCC = lChar;
            }
        }

        // Check that we found a nice camera
        if (lCameraId.isEmpty()) {
            Log.e("Cam", "Could not find a LENS_FACING_BACK camera");
            return;
        }

        // Check permissions
        Log.d("Cam", "Check permissions");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }


        /*
        Log.d("Cam", "Create JPEG reader");
        StreamConfigurationMap streamConfigs = lCC.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] jpegSizes = streamConfigs.getOutputSizes(ImageFormat.JPEG);

        int jpegWidth = jpegSizes[0].getWidth();
        int jpegHeight = jpegSizes[0].getHeight();

        ImageReader jpegImageReader = ImageReader.newInstance(jpegWidth, jpegHeight, ImageFormat.JPEG, 1);
        jpegImageReader.setOnImageAvailableListener(new OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d("Cam", "Got JPEFG frame");
            }
        }, new Handler(){

        });
        jpegCaptureSurface = jpegImageReader.getSurface();
        */


        // Open the camera
        Log.d("Cam", "Open camera");
        mCameraManager.openCamera(lCameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                Log.d("Cam", "connected");
                mCamera = camera;

                try{
                    configureMediaCodecEncoder();
                    createCaptureSession();
                }
                catch (android.hardware.camera2.CameraAccessException e) {
                    Log.e("Cam", "initCamera Failed: " + e.getMessage());
                    return;
                }
                catch (java.io.IOException e) {
                    Log.e("Cam", "initCamera Failed: " + e.getMessage());
                    return;
                }
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                Log.e("Cam", "disconnected");
            }

            @Override
            public void onError(CameraDevice camera, int aError) {
                Log.e("Cam", "error:" + aError);
            }
        }, new Handler(){

        });
    }

    private void createCaptureSession() throws android.hardware.camera2.CameraAccessException {

        // Create a capture session
        Log.d("Cam", "Create a capture session");
        mPreviewSurface = new Surface(mPreviewSurfaceTexture);
        Log.d("Cam", "a");

        List<Surface> surfaces = Arrays.asList(mPreviewSurface, mEncoderSurface);
        Log.d("Cam", "b");
        mCamera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

            @Override
            public void onConfigured(CameraCaptureSession session) {
                Log.d("Cam", "Created capture session");
                mSession = session;

                try{
                    startCapture();
                }
                catch (android.hardware.camera2.CameraAccessException e) {
                    Log.e("Cam", "initCamera Failed: " + e.getMessage());
                    return;
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Log.e("Cam", "Failed to create a capture session");
            }
        }, new Handler(){

        });

    }

    private void startCapture() throws android.hardware.camera2.CameraAccessException {
        CaptureRequest.Builder request = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);  // TEMPLATE_PREVIEW

        request.addTarget(mPreviewSurface);

        // Only add the encoder surface when doing a recording
        if (getStreamState() == STATE_RECORDING) {
            request.addTarget(mEncoderSurface);
        }

        // set capture options: fine-tune manual focus, white balance, etc.

        mSession.setRepeatingRequest(request.build(), new CaptureCallback() {
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                // updated values can be found here
                //Log.d("Cam", "RepeatingRequest");
            }
        }, null);
    }


    private void configureMediaCodecEncoder() throws  java.io.IOException {
        Log.i("Cam", "Initializing media encoder");


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String qualityStr = prefs.getString("quality", "3");
        int quality = Integer.parseInt(qualityStr);

        int width = 320;
        int height = 240;
        int bitrate = 100000;

        if (quality == 1) {
            width = 320;
            height = 240;
            bitrate = 100000;
        } else if (quality == 2)
        {
            width = 320;
            height = 240;
            bitrate = 300000;
        } else if (quality == 3)
        {
            width = 640;
            height = 480;
            bitrate = 600000;
        } else if (quality == 4)
        {
            width = 640;
            height = 480;
            bitrate = 1000000;
        } else if (quality == 5)
        {
            width = 1440;
            height = 1080;
            bitrate = 2000000;
        }

        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);


        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        Log.i("Cam", "configure video encoding format: " + format);

        // Create/configure a MediaCodec encoder.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoderSurface = mEncoder.createInputSurface();
        mEncoder.start();
    }

    private void Sleep(int ms)
    {
        try {
            Thread.sleep(ms);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void doMediaCodecEncoding(boolean notifyEndOfStream) {
        //Log.i("Enc", "doMediaCodecEncoding(" + notifyEndOfStream + ")");

        if (notifyEndOfStream) {
            mEncoder.signalEndOfInputStream();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String host = prefs.getString("bazingaserver", "127.0.0.1");
        String port = prefs.getString("bazingaport", "9000");
        Integer server_port = Integer.parseInt(port);
        InetAddress local;

        try {
            local = InetAddress.getByName(host);
        }
        catch(java.net.UnknownHostException e){
            e.printStackTrace();
            return;
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        boolean notDone = true;
        while (notDone) {

            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!notifyEndOfStream) {
                    /**
                     * Break out of the while loop because the encoder is not
                     * ready to output anything yet.
                     */
                    notDone = false;
                } else {
                    Log.i("Enc", "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // generic case for mediacodec, not likely occurs for encoder.
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                /**
                 * should happen before receiving buffers, and should only
                 * happen once
                 */
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.i("Enc", "encoder output format changed: " + newFormat);
            } else if (encoderStatus < 0) {
                Log.w("Enc", "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
            } else {
                // Normal flow: get output encoded buffer, send to muxer.
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    /**
                     * The codec config data was pulled out and fed to the muxer
                     * when we got the INFO_OUTPUT_FORMAT_CHANGED status. Ignore
                     * it.
                     */
                    Log.i("Enc", "got BUFFER_FLAG_CODEC_CONFIG");

                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mInitData = new byte[encodedData.remaining()];
                    encodedData.get(mInitData);

                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    /**
                     * It's usually necessary to adjust the ByteBuffer values to
                     * match BufferInfo.
                     */
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    //mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    //Log.i("Enc", "encoded " + mBufferInfo.size + " bytes");

                    try {
                        if (senderSocket == null)
                        {
                            senderSocket = new DatagramSocket();
                        }

                        int lBytes = 0;
                        // Send access unit delimiter
                        byte[] delimBbuf = new byte[] {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x09, (byte)0xe0};
                        DatagramPacket delim = new DatagramPacket(delimBbuf, delimBbuf.length, local, server_port);
                        senderSocket.send(delim);
                        lBytes += delimBbuf.length;
                        Sleep(2);


                        // Send H264 data to server
                        byte[] data = new byte[encodedData.remaining()];
                        encodedData.get(data);


                        // Check for IDR Frame
                        byte lIDR = 0x1f;
                        byte lType = (byte)((byte)data[4] & (byte)lIDR);
                        //Log.d("Enc", "NAL type=" + lType);
                        if (lType == 5)
                        {
                            //Log.d("Enc", "Sending Encoder config: " + mInitData.length);
                            DatagramPacket packet = new DatagramPacket(mInitData, mInitData.length, local, server_port);
                            senderSocket.send(packet);
                            lBytes += mInitData.length;
                            Sleep(2);
                        }


                        lBytes += data.length;
                        int lNumPackets = data.length / 1000;
                        int lRest = data.length % 1000;
                        //Log.d("enc", "Num packets: " + lNumPackets + " rest=" + lRest);
                        int lPos = 0;
                        for (int i = 0; i < lNumPackets; ++i)
                        {
                            byte[] arr = Arrays.copyOfRange(data, lPos, lPos + 1000);
                            lPos += 1000;
                            DatagramPacket packet = new DatagramPacket(arr, arr.length, local, server_port);
                            senderSocket.send(packet);
                            Sleep(2);
                        }

                        if (lRest > 0)
                        {
                            byte[] arr = Arrays.copyOfRange(data, lPos, lPos + lRest);
                            //Log.d("enc", "Rest: " + arr.length + " rest=" + lRest);
                            DatagramPacket packet = new DatagramPacket(arr, arr.length, local, server_port);
                            senderSocket.send(packet);
                            Sleep(2);
                        }

                        if (lType == 5)
                        {
                            Log.d("Enc", "Writing IDR frame of size: " + lBytes);
                        }
                    }
                    catch(java.net.SocketException e)
                    {
                        e.printStackTrace();
                    }
                    catch(java.io.IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!notifyEndOfStream) {
                        Log.w("Enc", "reached end of stream unexpectedly");
                    } else {
                        Log.i("Enc", "end of stream reached");
                    }
                    // Finish encoding.
                    notDone = false;
                }
            }
        } // End of while(notDone)
    }


    private void startMediaCodecRecording() {
        /**
         * Start video recording asynchronously. we need a loop to handle output
         * data for each frame.
         */
        mRecordingThread = new Thread() {
            @Override
            public void run() {

                Log.i("Cam", "Recording thread starts");

                while (getStreamState() == STATE_RECORDING) {
                    // Feed encoder output into the muxer until recording stops.
                    doMediaCodecEncoding(/* notifyEndOfStream */false);
                }

                Log.i("Cam", "Recording thread completes");

                return;
            }
        };
        mRecordingThread.start();
    }

}
