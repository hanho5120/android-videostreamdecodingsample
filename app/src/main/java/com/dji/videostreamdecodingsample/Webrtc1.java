package com.dji.videostreamdecodingsample;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CapturerObserver;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.NV21Buffer;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnectionFactory.InitializationOptions;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import android.os.Environment;

import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.Toast;

import com.dji.videostreamdecodingsample.media.DJIVideoStreamDecoder;
import com.dji.videostreamdecodingsample.media.NativeHelper;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;



import dji.gs.control.WarnAreaAlertController;
import dji.flysafe.LimitAreaSource;
import dji.flysafe.LimitArea;
import dji.flysafe.LimitAreaLevel;
import dji.flysafe.AppUnlockSpacesInfo;
import dji.flysafe.LimitAreaShape;
import dji.flysafe.LimitAreaType;
import dji.pilot.flyforbid.sdk.SDKFlyZoneWarningState;
import dji.pilot.flyforbid.FlyforbidEventManager;


import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.flysafe.WarningEventType;
import dji.thirdparty.afinal.core.AsyncTask;
import dji.ux.widget.FPVOverlayWidget;
import dji.ux.widget.FPVWidget;
import io.socket.client.Socket;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//webrtc



public class Webrtc1 extends Activity implements DJICodecManager.YuvDataCallback, SignallingClient.SignalingInterface, RemoteSocketInterface.SocketListner {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MSG_WHAT_SHOW_TOAST = 0;
    private static final int MSG_WHAT_UPDATE_TITLE = 1;
    private SurfaceHolder.Callback surfaceCallback;

    private enum DemoType { USE_TEXTURE_VIEW, USE_SURFACE_VIEW, USE_SURFACE_VIEW_DEMO_DECODER}
    private static DemoType demoType = DemoType.USE_SURFACE_VIEW;
    private VideoFeeder.VideoFeed standardVideoFeeder;

    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    private TextView titleTv;
    public Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WHAT_SHOW_TOAST:
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_WHAT_UPDATE_TITLE:
                    if (titleTv != null) {
                        titleTv.setText((String) msg.obj);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private FPVWidget fpvWidget;
    private FPVOverlayWidget fpvOverlayWidget;
    private TextureView videostreamPreviewTtView;
    private SurfaceView videostreamPreviewSf;
    private SurfaceHolder videostreamPreviewSh;
    private Camera mCamera;
    private DJICodecManager mCodecManager;
    private TextView savePath;

    private RelativeLayout screenShot;
    private ImageView img_screenShot;
    private TextView text_screenShot;

    private StringBuilder stringBuilder;
    private int videoViewWidth;
    private int videoViewHeight;
    private int count;
    private Socket socket;
    private boolean started = false;
     ////////// ==== webrtc =====

    PeerConnectionFactory peerConnectionFactory;
    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;
    AudioManager am;


    SurfaceTextureHelper surfaceTextureHelper;

    SurfaceViewRenderer localVideoView;
    SurfaceViewRenderer remoteVideoView;

    Button hangup;
    PeerConnection localPeer;
    List<IceServer> iceServers;
    EglBase rootEglBase;

    List<PeerConnection> Localpeerlist;

    boolean gotUserMedia;
    List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();

    final int ALL_PERMISSIONS_CODE = 1;
    VideoCapturer videoCapturerAndroid;

    //========================================================= new webrtc ========================================================
    public int mWidth = 854;
    public int mHeight = 480;
    public int mFps = 20;
    public int mVideoBitrate = 1600;
    public int mAudioBitrate = 32;
    public int x_google_start_bitrate = 1000;
    public int x_google_min_bitrate = 1000;
    public int x_google_max_bitrate = 5000;
    public int x_google_max_quantization = 56;
    public PeerConnectionFactory mPeerConnectionFactory;
    public MediaConstraints mPeerConnConstraints;
    public MediaConstraints mAudioConstraints;
    public AudioTrack mLocalAudioTrack;
    public VideoTrack mLocalVideoTrack;
    public SurfaceTextureHelper mSurfaceTextureHelper;
    public VideoSource mVideoSource;
    public AudioSource mAudioSource;
    public List<PeerConnection.IceServer> mPeerIceServers = new ArrayList<>();
    public ArrayList<RemoteDTO> mRemoteUsers = new ArrayList<>();
    public AudioDeviceModule mAudioAdm;
    protected long myBaseTime;
    protected String easy_outTime;
    protected int mDeviceWidth;
    protected int mDeviceHeight;
    public EglBase mEglBase;
    public VideoCapturer mVideoCapturer;


    public void webRtcInit() {


        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initViews();
        initVideos();

        // Ice Server
        getIceServers();

        // initPeerConnectionFactoryOptions
        PeerConnectionFactory.initialize(initPeerConnectionFactoryOptions());

        // AudioModule
        mAudioAdm = createJavaAudioDevice();

        // PeerConnectionFactory option
        PeerConnectionFactory.Options peerConnctionFactoryOption = new PeerConnectionFactory.Options();
        peerConnctionFactoryOption.networkIgnoreMask = 0;

        mPeerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(peerConnctionFactoryOption)
                .setAudioDeviceModule(mAudioAdm)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(mEglBase.getEglBaseContext(), true, false))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext()))
                .createPeerConnectionFactory();

        // AudioModule Release
        mAudioAdm.release();

        // PeerConnConstraints
        mPeerConnConstraints = new MediaConstraints();
        mPeerConnConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("IceRestart", "true"));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        // AudioConstraints
        mAudioConstraints = new MediaConstraints();
        mAudioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        mAudioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        mAudioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("echoCancellation", "true"));
        mAudioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("noiseSuppression", "true"));
        am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        am.setSpeakerphoneOn(true);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        // Video
        //mVideoCapturer = createCameraCapturer(new Camera1Enumerator(false));


        mVideoCapturer = new VideoCapturer() {
            @Override
            public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {

            }

            @Override
            public void startCapture(int i, int i1, int i2) {

            }

            @Override
            public void stopCapture() throws InterruptedException {

            }

            @Override
            public void changeCaptureFormat(int i, int i1, int i2) {

            }

            @Override
            public void dispose() {

            }

            @Override
            public boolean isScreencast() {
                return false;
            }
        };



        mSurfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), mEglBase.getEglBaseContext());
        mVideoSource = mPeerConnectionFactory.createVideoSource(mVideoCapturer.isScreencast());
        mVideoSource.adaptOutputFormat(mWidth, mHeight, mFps);

        mVideoCapturer.initialize(mSurfaceTextureHelper, this, mVideoSource.getCapturerObserver());
        mLocalVideoTrack = mPeerConnectionFactory.createVideoTrack("100", mVideoSource);

        mVideoCapturer.startCapture(mWidth, mHeight, mFps);

        // Audio
        mAudioSource = mPeerConnectionFactory.createAudioSource(mAudioConstraints);
        mLocalAudioTrack = mPeerConnectionFactory.createAudioTrack("101", mAudioSource);

    }

    @Override
    public void onConnect() {
        //count++;
        //showToast(" "+count);
        showToast(RemoteSocketClient.getInstance().count+"========"+RemoteSocketClient.getInstance().count1);
        try {
            JSONObject datalogin = new JSONObject();
            try {
                datalogin.put("login_key", Userdata.getInstance()._login_key);
                datalogin.put("login_id", "don");
                datalogin.put("status", "Y");
                datalogin.put("type", "D");
                datalogin.put("name", "드론");
                datalogin.put("group_id", "b7172bde-297e-4a0e-8df9-9cd915a460b0");
                datalogin.put("group_name", "드론");
                datalogin.put("'profile_photo'", "");
                datalogin.put("room_key", Userdata.getInstance()._room_key );
            }catch(JSONException e) {
                e.printStackTrace();
            }

            RemoteSocketClient.getInstance().getSocket().emit("reg_login_id", datalogin);
            RemoteSocketClient.getInstance().getSocket().emit("join_room", Userdata.getInstance()._room_id,Userdata.getInstance()._room_key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectError(String psMsg) {

    }

    @Override
    public void onDisConnect(String psMsg) {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void user_joined(String psId, int piCount, ArrayList<String> poClients, ArrayList<String> tmep) {
        for (String clientSocketId : poClients) {
            // 내영상은 Array 에 담을 필요가 없음
            if (clientSocketId.equals(RemoteSocketClient.getInstance().getSocketId())) {
                continue;
            }

            if (getRemoteUserIndex(clientSocketId) > -1) {
                continue;
            }



            RemoteDTO remoteDTO = new RemoteDTO();
            remoteDTO.setSocketId(clientSocketId);


            remoteDTO.createPeerConnection(mPeerConnectionFactory, mPeerIceServers, mPeerConnConstraints, new RemoteSocketInterface.PeerCreateLister() {
                @Override
                public void onIceCandidate(IceCandidate poIceCandidate, String psSocketId) {
                    if (poIceCandidate != null) {

                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("type", "candidate");
                            jsonObject.put("sdpMLineIndex", poIceCandidate.sdpMLineIndex);
                            jsonObject.put("sdpMid", poIceCandidate.sdpMid);
                            jsonObject.put("candidate", poIceCandidate.sdp);

                            JSONObject jsonObject_content = new JSONObject();
                            jsonObject_content.put("ice", jsonObject);

                            RemoteSocketClient.getInstance().getSocket().emit("signal", psSocketId, jsonObject_content.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onAddStream(MediaStream mediaStream, String socketId) {

                }
            });

            remoteDTO.addSteam(mPeerConnectionFactory, mLocalAudioTrack, mLocalVideoTrack);
            mRemoteUsers.add(remoteDTO);


        }



        if (piCount >= 2) {
            if (psId.equals(RemoteSocketClient.getInstance().getSocketId())) {
                return;
            }

            int index = getRemoteUserIndex(psId);

            if (index == -1) {
                return;
            }

            mRemoteUsers.get(index).createOffer(mPeerConnConstraints, sessionDescription -> {
                try {
                    JSONObject jsonObject = new JSONObject();

                    String ls_sdp = changeSdp(changeSdp(sessionDescription.description, "video", mVideoBitrate), "audio", mAudioBitrate);
                    ls_sdp = changeBitrate(ls_sdp);

                    jsonObject.put("sdp", ls_sdp);
                    jsonObject.put("type", sessionDescription.type.canonicalForm());

                    JSONObject jsonObject_content = new JSONObject();
                    jsonObject_content.put("sdp", jsonObject);

                    RemoteSocketClient.getInstance().getSocket().emit("signal", psId, jsonObject_content.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }

    @Override
    public void user_left(String psId) {
        int index = getRemoteUserIndex(psId);

        if (index < 0) {
            return;
        }

        mRemoteUsers.get(index).remove(mLocalAudioTrack, mLocalVideoTrack, () -> {
            mRemoteUsers.remove(index);
        });

    }

    @Override
    public void gotMessageFromServer(String psId, JSONObject signal) {
        int index = getRemoteUserIndex(psId);

        if (index == -1) {
            return;
        }

        RemoteDTO data = mRemoteUsers.get(index);

        if (!data.getSocketId().equals(RemoteSocketClient.getInstance().getSocketId())) {
            try {
                if (signal.has("sdp")) {
                    JSONObject content_data = new JSONObject(signal.getString("sdp"));
                    String ls_type = content_data.getString("type");

                    data.getPeerConnection().setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription((ls_type.equals("answer") ? SessionDescription.Type.ANSWER : SessionDescription.Type.OFFER), content_data.getString("sdp")));

                    if (ls_type.equals("offer")) {
                        data.getPeerConnection().createAnswer(new CustomSdpObserver("localCreateAns") {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void onCreateSuccess(SessionDescription sessionDescription) {
                                super.onCreateSuccess(sessionDescription);

                                try {
                                    data.getPeerConnection().setLocalDescription(new CustomSdpObserver("localSetLocal"), sessionDescription);

                                    JSONObject jsonObject = new JSONObject();

                                    String ls_sdp = changeSdp(changeSdp(sessionDescription.description, "video", mVideoBitrate), "audio", mAudioBitrate);
                                    ls_sdp = changeBitrate(ls_sdp);

                                    jsonObject.put("sdp", ls_sdp);
                                    jsonObject.put("type", sessionDescription.type.canonicalForm());

                                    JSONObject jsonObject_content = new JSONObject();
                                    jsonObject_content.put("sdp", jsonObject);

                                    RemoteSocketClient.getInstance().getSocket().emit("signal", data.getSocketId(), jsonObject_content.toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onCreateFailure(String s) {
                                super.onCreateFailure(s);
                            }
                        }, mPeerConnConstraints);
                    }
                }

                if (signal.has("ice")) {
                    JSONObject content_data = new JSONObject(signal.getString("ice"));
                    data.getPeerConnection().addIceCandidate(new IceCandidate(content_data.getString("sdpMid"), content_data.getInt("sdpMLineIndex"), content_data.getString("candidate")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void goChatMsg(JSONObject poData) {

    }


    @Override
    public void onClosed() {

    }

    @Override
    public void onReloaded() {

    }

    @Override
    public void goZoom(String psMsg) {

    }

    @Override
    public void goFlash(String psMsg) {

    }

    @Override
    public void goDraw(JSONObject data) {

    }

    @Override
    public void goMark(JSONObject data) {

    }

    @Override
    public void goScreen(String psMsg) {

    }

    @Override
    public void onZoomChanged(int level) {

    }

    @Override
    public void onFlashChanged() {

    }


    //==============================================================================================================================


    void Changeframe(byte[] videoBuffer, int size, int width, int height)
    {

        try{
            long timestampNS = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
            NV21Buffer buffer = new NV21Buffer(videoBuffer, width, height, null);


            VideoFrame videoFrame = new VideoFrame(buffer, 0, timestampNS);
            mVideoSource.getCapturerObserver().onFrameCaptured(videoFrame);

            videoFrame.release();
        }
        catch (Exception e)
        {

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ALL_PERMISSIONS_CODE
                && grantResults.length == 2
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // all permissions granted
            //start();
        } else {
            finish();
        }
    }

    private void initViews() {

        localVideoView = findViewById(R.id.surface_view);
        remoteVideoView = findViewById(R.id.surface_view_remote);


        //-----------------------짐벌ㄹㄹㄹㄹㄹㄹㄹㄹㄹㄹㄹㄹㄹ-----------------------------
        fpvWidget = (FPVWidget) findViewById(R.id.fpv_widget);
        fpvWidget.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });
        fpvOverlayWidget = (FPVOverlayWidget) findViewById(R.id.fpv_overlay_widget);
        fpvOverlayWidget.setGimbalControlEnabled(true);
    }

    private void initVideos() {
        mEglBase = EglBase.create();
        localVideoView.release();
        remoteVideoView.release();
        localVideoView.init(mEglBase.getEglBaseContext(), null);
        remoteVideoView.init(mEglBase.getEglBaseContext(), null);
        localVideoView.setZOrderMediaOverlay(true);
        remoteVideoView.setZOrderMediaOverlay(true);
        localVideoView.surfaceDestroyed(videostreamPreviewSh);
    }

    private void getIceServers() {
        //get Ice servers using xirsys


        ArrayList<String> arr_url = new ArrayList<>();
        arr_url.add("stun:stun.services.mozilla.com");
        arr_url.add("stun:stun.l.google.com:19302");

        //arr_url.add("stun:tk-turn1.xirsys.com");



        ArrayList<String> arr_turn_url = new ArrayList<>();
        arr_turn_url.add("turn:tk-turn1.xirsys.com:80?transport=udp");
        arr_turn_url.add("turn:tk-turn1.xirsys.com:3478?transport=udp");
        arr_turn_url.add("turn:tk-turn1.xirsys.com:80?transport=tcp");
        arr_turn_url.add("turn:tk-turn1.xirsys.com:3478?transport=tcp");
        arr_turn_url.add("turns:tk-turn1.xirsys.com:443?transport=tcp");

        for(int i = 0;i<arr_turn_url.size();i++)
        {
           // PeerConnection.IceServer turn = PeerConnection.IceServer.builder(arr_turn_url.get(i)).setUsername("uOrIfTdlrlv9U729TM2ds_twRB2_ucq2VxqO4fudd3pqgysVKae9ds-Mw50a9INvAAAAAF9XWCloYW5ob2tpbQ==").setPassword("457a62ee-f1bb-11ea-b26c-0242ac140004").createIceServer();
           // peerIceServers.add(turn);
        }



        PeerConnection.IceServer stun = PeerConnection.IceServer.builder(arr_url).createIceServer();
        //PeerConnection.IceServer turn = PeerConnection.IceServer.builder("turn:tk-turn1.xirsys.com:3478?").setUsername("uOrIfTdlrlv9U729TM2ds_twRB2_ucq2VxqO4fudd3pqgysVKae9ds-Mw50a9INvAAAAAF9XWCloYW5ob2tpbQ==").setPassword("457a62ee-f1bb-11ea-b26c-0242ac140004").createIceServer();

        PeerConnection.IceServer turn = PeerConnection.IceServer.builder("turn:101.55.28.31:3478").setUsername("deepfine").setPassword("ckddjq0323").createIceServer();
        //PeerConnection.IceServer turn1 = PeerConnection.IceServer.builder("turn:101.55.28.31:3478?transport=udp").setUsername("deepfine").setPassword("ckddjq0323").createIceServer();
        //PeerConnection.IceServer turn = PeerConnection.IceServer.builder("turn:numb.viagenie.ca:3478?transport=tcp").setUsername("hanhokim@gmail.com").setPassword("rlagksgh").createIceServer();
        mPeerIceServers.add(stun);
        mPeerIceServers.add(turn);
        //peerIceServers.add(turn1);

/*

        byte[] data = new byte[0];
        try {
            data = ("hanhokim:78c8495c-f1b8-11ea-8f90-0242ac150003").getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String authToken = "Basic " + Base64.encodeToString(data, Base64.NO_WRAP);
        Utils.getInstance().getRetrofitInstance().getIceCandidates(authToken).enqueue(new Callback<TurnServerPojo>() {
            @Override
            public void onResponse(@NonNull Call<TurnServerPojo> call, @NonNull Response<TurnServerPojo> response) {
                TurnServerPojo body = response.body();
                if (body != null) {
                    iceServers = body.iceServerList.iceServers;
                }
                for (IceServer iceServer : iceServers) {
                    if (iceServer.credential == null) {
                        PeerConnection.IceServer peerIceServer = PeerConnection.IceServer.builder(iceServer.url).createIceServer();
                        peerIceServers.add(peerIceServer);
                    } else {
                        PeerConnection.IceServer peerIceServer = PeerConnection.IceServer.builder(iceServer.url)
                                .setUsername(iceServer.username)
                                .setPassword(iceServer.credential)
                                .createIceServer();
                        peerIceServers.add(peerIceServer);
                    }
                }
                Log.d("onApiResponse", "IceServers\n" + iceServers.toString());
            }

            @Override
            public void onFailure(@NonNull Call<TurnServerPojo> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
*/

    }


    public void start()
    {
        //count++;
        //showToast(" "+count);
        if(started)
            return;

        started=true;

        webRtcInit();
        RemoteSocketClient.getInstance().init(this);

        localVideoView.setVisibility(View.VISIBLE);
        mLocalVideoTrack.addSink(localVideoView);

        localVideoView.setMirror(false);
        remoteVideoView.setMirror(false);

    }

    public void start_old() {



        if (localPeer != null) {
            localPeer.close();
        }
        localPeer = null;


        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initViews();
        initVideos();
        getIceServers();

        //SignallingClient.getInstance().init(this);
        RemoteSocketClient.getInstance().init(this);

        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

        //Now create a VideoCapturer instance.


        videoCapturerAndroid = new VideoCapturer() {
            @Override
            public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {

            }

            @Override
            public void startCapture(int i, int i1, int i2) {

            }

            @Override
            public void stopCapture() throws InterruptedException {

            }

            @Override
            public void changeCaptureFormat(int i, int i1, int i2) {

            }

            @Override
            public void dispose() {

            }

            @Override
            public boolean isScreencast() {
                return false;
            }
        };


        // PeerConnConstraints
        videoConstraints = new MediaConstraints();
        videoConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("IceRestart", "true"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        // AudioConstraints
        audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("echoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("noiseSuppression", "true"));

        //Create a VideoSource instance
        if (videoCapturerAndroid != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
            videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid.isScreencast());
            videoCapturerAndroid.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        }
        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);

        if (videoCapturerAndroid != null) {
            videoCapturerAndroid.startCapture(mWidth, mHeight, mFps);
        }

        localVideoView.setVisibility(View.VISIBLE);
        // And finally, with our VideoRenderer ready, we
        // can add our renderer to the VideoTrack.
        localVideoTrack.addSink(localVideoView);

        localVideoView.setMirror(false);
        remoteVideoView.setMirror(false);

        gotUserMedia = true;



        if (SignallingClient.getInstance().isInitiator) {
            onTryToStart();
        }


    }


    public String changeBitrate(String psSdp) {
        String lsSdp = psSdp;

        String[] lines = lsSdp.split("\r\n");
        int lineIndex = -1;
        String vp8RtpMap = null;
        Pattern vp8Pattern = Pattern.compile("^a=rtpmap:(\\d+) VP8/90000[\r]?$");

        for (int i = 0; i < lines.length; i++) {
            Matcher vp8Matcher = vp8Pattern.matcher(lines[i]);
            if (vp8Matcher.matches()) {
                vp8RtpMap = vp8Matcher.group(1);
                lineIndex = i;
                break;
            }
        }

        if (vp8RtpMap == null) {
            return lsSdp;
        }

        StringBuilder newSdpDescription = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            newSdpDescription.append(lines[i]).append("\r\n");

            if (i == lineIndex) {
                String bitrateSet = "a=fmtp:" + vp8RtpMap + " x-google-start-bitrate="+x_google_start_bitrate+"; x-google-min-bitrate="+x_google_min_bitrate+"; x-google-max-bitrate="+x_google_max_bitrate+"; x-google-max-quantization="+x_google_max_quantization;
                newSdpDescription.append(bitrateSet).append("\r\n");
            }
        }

        return newSdpDescription.toString();
    }

    /**
     * sdp 변경
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String changeSdp(String psSdp, String psMediaType, int piBitrate) {
        String lsSdp = psSdp;
        String[] lsSdps = lsSdp.split("\n");

        ArrayList<String> strs = new ArrayList<String>(Arrays.asList(lsSdps));

        int li_line = -1;

        for ( int i = 0; i < strs.size(); i++ ) {
            if ( strs.get(i).indexOf("m="+psMediaType) == 0 ) {
                li_line = i;
                break;
            }
        }

        if ( li_line == -1 ) {
            return lsSdp;
        }

        li_line++;

        while ( strs.get(li_line).indexOf("i=") == 0 || strs.get(li_line).indexOf("c=") == 0 ) {
            li_line++;
        }

        if ( strs.get(li_line).indexOf("b") == 0 ) {
            strs.set(li_line, "b=AS:"+piBitrate);
            lsSdp = String.join("\n", strs);
            return lsSdp;
        }

        List<String> newStrs = strs.subList(0, li_line);
        newStrs.add("b=AS:"+piBitrate);
        newStrs.addAll(strs.subList(li_line+1, strs.size()));

        lsSdp = String.join("\n", newStrs) + "\n";

        return lsSdp;
    }

    /**
     * 피어소켓 인덱스 구하기
     */
    public int getRemoteUserIndex(String psSocketId) {
        if ( mRemoteUsers.size() < 1 ) {
            return -1;
        }

        int li_index = -1;

        for ( int i = 0; i < mRemoteUsers.size(); i++ ) {
            RemoteDTO lo_data = mRemoteUsers.get(i);

            if ( lo_data.getSocketId().equals(psSocketId) ) {
                li_index = i;
                break;
            }
        }

        return li_index;
    }



    /**



    /**
     * PeerConnectionFactory.InitializationOptions
     */
    public PeerConnectionFactory.InitializationOptions initPeerConnectionFactoryOptions() {
        return PeerConnectionFactory.InitializationOptions
                .builder(getApplicationContext())
                .setEnableInternalTracer(false) // 추가한거
                .createInitializationOptions();
    }

    /**
     * Audio Module
     */
    public AudioDeviceModule createJavaAudioDevice() {
        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = new JavaAudioDeviceModule.AudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
            }

            @Override
            public void onWebRtcAudioRecordStartError(JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
            }
        };

        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = new JavaAudioDeviceModule.AudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
            }

            @Override
            public void onWebRtcAudioTrackStartError(JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
            }
        };

        return JavaAudioDeviceModule.builder(this)
                .setSamplesReadyCallback(null)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .createAudioDeviceModule();
    }



    /**
     * This method will be called directly by the app when it is the initiator and has got the local media
     * or when the remote peer sends a message through socket that it is ready to transmit AV data
     */
    @Override
    public void onTryToStart() {
        runOnUiThread(() -> {
            if (!SignallingClient.getInstance().isStarted && localVideoTrack != null && SignallingClient.getInstance().isChannelReady) {
                createPeerConnection();
                SignallingClient.getInstance().isStarted = true;
                if (SignallingClient.getInstance().isInitiator) {
                    doCall();
                }
            }
        });
    }

    /**
     * Creating the local peerconnection instance
     */
    private void createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(peerIceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.RELAY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;

        RemoteDTO remoteDTO = new RemoteDTO();


        localPeer= peerConnectionFactory.createPeerConnection(rtcConfig, new CustomPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                showToast("Received Remote stream");
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }
        });


        addStreamToLocalPeer();
    }

    /**
     * Adding the stream to the localpeer
     */
    private void addStreamToLocalPeer() {
        //creating local mediastream
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        Localpeerlist.get(Localpeerlist.size()).addStream(stream);
    }

    /**
     * This method is called when the app is the initiator - We generate the offer and send it over through socket
     * to remote peer
     */
    private void doCall() {
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        localPeer.createOffer(new CustomSdpObserver("localCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                Log.d("onCreateSuccess", "SignallingClient emit ");
                SignallingClient.getInstance().emitMessage(sessionDescription);
            }
        }, sdpConstraints);
    }

    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
    private void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        runOnUiThread(() -> {
            try {
                remoteVideoView.setVisibility(View.VISIBLE);
                videoTrack.addSink(remoteVideoView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Received local ice candidate. Send it to remote peer through signalling for negotiation
     */
    public void onIceCandidateReceived(IceCandidate iceCandidate) {
        //we have received ice candidate. We can set it to the other peer.
        SignallingClient.getInstance().emitIceCandidate(iceCandidate);
    }

    /**
     * SignallingCallback - called when the room is created - i.e. you are the initiator
     */
    @Override
    public void onCreatedRoom() {
        showToast("You created the room " + gotUserMedia);
        if (gotUserMedia) {
            SignallingClient.getInstance().emitMessage("got user media");
        }
    }

    /**
     * SignallingCallback - called when you join the room - you are a participant
     */
    @Override
    public void onJoinedRoom() {
        showToast("You joined the room " + gotUserMedia);
        if (gotUserMedia) {
            SignallingClient.getInstance().emitMessage("got user media");
        }


    }

    @Override
    public void onNewPeerJoined() {
        showToast("Remote Peer Joined");
    }

    @Override
    public void onRemoteHangUp(String msg) {
        showToast("Remote Peer hungup");
        runOnUiThread(this::hangup);
    }

    /**
     * SignallingCallback - Called when remote peer sends offer
     */
    @Override
    public void onOfferReceived(final JSONObject data) {
        showToast("Received Offer");
        runOnUiThread(() -> {
            if (!SignallingClient.getInstance().isInitiator && !SignallingClient.getInstance().isStarted) {
                onTryToStart();
            }

            try {
                localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription(SessionDescription.Type.OFFER, data.getString("sdp")));
                doAnswer();
                updateVideoViews(true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void doAnswer() {
        localPeer.createAnswer(new CustomSdpObserver("localCreateAns") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocal"), sessionDescription);
                SignallingClient.getInstance().emitMessage(sessionDescription);
            }
        }, new MediaConstraints());
    }

    /**
     * SignallingCallback - Called when remote peer sends answer to your offer
     */

    @Override
    public void onAnswerReceived(JSONObject data) {
        showToast("Received Answer");
        try {
            localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription(SessionDescription.Type.fromCanonicalForm(data.getString("type").toLowerCase()), data.getString("sdp")));
            updateVideoViews(true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remote IceCandidate received
     */
    @Override
    public void onIceCandidateReceived(JSONObject data) {
        try {

            if (localPeer == null) {
                return;
            }
            localPeer.addIceCandidate(new IceCandidate(data.getString("id"), data.getInt("label"), data.getString("candidate")));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateVideoViews(final boolean remoteVisible) {
        runOnUiThread(() -> {
                if(localVideoView!=null)
                {
                    ViewGroup.LayoutParams params = localVideoView.getLayoutParams();
                    if (remoteVisible) {
                        //params.height = dpToPx(100);
                        //params.width = dpToPx(100);
                    } else {
                        params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    }

                    localVideoView.setLayoutParams(params);
                }


        });
    }

    /**
     * Closing up - normal hangup and app destroye
     */



    private void hangup() {
        try {



            for(int i = 0;i<mRemoteUsers.size();i++)
            {
                mRemoteUsers.get(i).getPeerConnection().close();
                mRemoteUsers.get(i).peerConnection = null;
            }

            RemoteSocketClient.getInstance().close();
            started= false;
            //updateVideoViews(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Util Methods
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }


    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

//  =================================


    @Override
    protected void onResume() {
        super.onResume();
        initSurfaceOrTextureView();
        notifyStatusChange();
    }

    private void initSurfaceOrTextureView(){
        switch (demoType) {
            case USE_SURFACE_VIEW:
                initPreviewerSurfaceView();
                break;
            case USE_SURFACE_VIEW_DEMO_DECODER:
                /**
                 * we also need init the textureView because the pre-transcoded video steam will display in the textureView
                 */
                initPreviewerTextureView();

                /**
                 * we use standardVideoFeeder to pass the transcoded video data to DJIVideoStreamDecoder, and then display it
                 * on surfaceView
                 */
                initPreviewerSurfaceView();
                break;
            case USE_TEXTURE_VIEW:
                initPreviewerTextureView();
                break;
        }
    }

    @Override
    protected void onPause() {
        if (mCamera != null) {
            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataListener);
            }
            if (standardVideoFeeder != null) {
                standardVideoFeeder.removeVideoDataListener(mReceivedVideoDataListener);
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager.destroyCodec();
        }
        hangup();

        if(SignallingClient.getInstance()!=null)
        {
            SignallingClient.getInstance().close();
        }

        super.onDestroy();



        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webrtc1);
        initUi();

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        Point outPoint = new Point();
        display.getRealSize(outPoint);
        mDeviceHeight = outPoint.y;
        mDeviceWidth = outPoint.x;
        //Toast.makeText(getApplicationContext(), mDeviceHeight , Toast.LENGTH_LONG).show();
        //Toast.makeText(getApplicationContext(), "SDK registration succeeded!", Toast.LENGTH_LONG).show();

        //Intent intent = getIntent();
        //Bundle bundle = intent.getExtras();
        //String getdroneid = bundle.getString("droneid");
        //String getdronepw = bundle.getString("dronepw");

        //TextView_getid.setText(getdroneid);
        //TextView_getpw.setText(getdronepw);


    }

    private void showToast(String s) {
        mainHandler.sendMessage(
                mainHandler.obtainMessage(MSG_WHAT_SHOW_TOAST, s)
        );
    }

    private void updateTitle(String s) {
        mainHandler.sendMessage(
                mainHandler.obtainMessage(MSG_WHAT_UPDATE_TITLE, s)
        );
    }

    private void initUi() {
        savePath = (TextView) findViewById(R.id.activity_main_save_path);

        screenShot = (RelativeLayout) findViewById(R.id.activity_main_screen_shot);
        screenShot.setSelected(false);

        text_screenShot = (TextView) findViewById(R.id.text_screenShot);
        img_screenShot = (ImageView) findViewById(R.id.img_screenShot);

        titleTv = (TextView) findViewById(R.id.title_tv);
        videostreamPreviewTtView = (TextureView) findViewById(R.id.livestream_preview_ttv);
        videostreamPreviewSf = (SurfaceView) findViewById(R.id.livestream_preview_sf);
        videostreamPreviewSf.setClickable(true);
        videostreamPreviewSf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float rate = VideoFeeder.getInstance().getTranscodingDataRate();
                showToast("current rate:" + rate + "Mbps");
                if (rate < 10) {
                    VideoFeeder.getInstance().setTranscodingDataRate(10.0f);
                    showToast("set rate to 10Mbps");
                } else {
                    VideoFeeder.getInstance().setTranscodingDataRate(3.0f);
                    showToast("set rate to 3Mbps");
                }
            }
        });
        updateUIVisibility();
    }

    private void updateUIVisibility(){
        switch (demoType) {
            case USE_SURFACE_VIEW:
                videostreamPreviewSf.setVisibility(View.VISIBLE);
                videostreamPreviewTtView.setVisibility(View.GONE);
                break;
            case USE_SURFACE_VIEW_DEMO_DECODER:
                /**
                 * we need display two video stream at the same time, so we need let them to be visible.
                 */
                videostreamPreviewSf.setVisibility(View.VISIBLE);
                videostreamPreviewTtView.setVisibility(View.VISIBLE);
                break;

            case USE_TEXTURE_VIEW:
                videostreamPreviewSf.setVisibility(View.GONE);
                videostreamPreviewTtView.setVisibility(View.VISIBLE);
                break;
        }
    }
    private long lastupdate;
    private void notifyStatusChange() {


        final BaseProduct product = MApplication.getProductInstance();

        Log.d(TAG, "notifyStatusChange: " + (product == null ? "Disconnect" : (product.getModel() == null ? "null model" : product.getModel().name())));
        if (product != null && product.isConnected() && product.getModel() != null) {
            updateTitle(product.getModel().name() + " Connected " + demoType.name());
        } else {
            updateTitle("Disconnected");
        }

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (System.currentTimeMillis() - lastupdate > 1000) {
                    Log.d(TAG, "camera recv video data size: " + size);
                    updateTitle("camera recv video data size:"+size);
                    lastupdate = System.currentTimeMillis();
                }


                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        if (mCodecManager != null) {
                            mCodecManager.sendDataToDecoder(videoBuffer, size);
                        }
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        /**
                         we use standardVideoFeeder to pass the transcoded video data to DJIVideoStreamDecoder, and then display it
                         * on surfaceView
                         */


                        DJIVideoStreamDecoder.getInstance().parse(videoBuffer, size);
                        break;

                    case USE_TEXTURE_VIEW:
                        if (mCodecManager != null) {
                            mCodecManager.sendDataToDecoder(videoBuffer, size);
                        }
                        break;
                }

            }
        };

        if (null == product || !product.isConnected()) {
            mCamera = null;
            showToast("Disconnected");
        } else {
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                mCamera = product.getCamera();
                mCamera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast("can't change mode of camera, error:"+djiError.getDescription());
                        }
                    }
                });

                if (demoType == DemoType.USE_SURFACE_VIEW_DEMO_DECODER && isTranscodedVideoFeedNeeded()) {
                    standardVideoFeeder = VideoFeeder.getInstance().provideTranscodedVideoFeed();
                    standardVideoFeeder.addVideoDataListener(mReceivedVideoDataListener);
                    return;
                }
                if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                    VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
                }

            }
        }
    }

    /**
     * Init a fake texture view to for the codec manager, so that the video raw data can be received
     * by the camera
     */
    private void initPreviewerTextureView() {
        videostreamPreviewTtView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "real onSurfaceTextureAvailable");
                videoViewWidth = width;
                videoViewHeight = height;
                Log.d(TAG, "real onSurfaceTextureAvailable: width " + videoViewWidth + " height " + videoViewHeight);
                if (mCodecManager == null) {
                    mCodecManager = new DJICodecManager(getApplicationContext(), surface, width, height);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                videoViewWidth = width;
                videoViewHeight = height;
                Log.d(TAG, "real onSurfaceTextureAvailable2: width " + videoViewWidth + " height " + videoViewHeight);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mCodecManager != null) {
                    mCodecManager.cleanSurface();
                }
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    /**
     * Init a surface view for the DJIVideoStreamDecoder
     */
    private void initPreviewerSurfaceView() {


        videostreamPreviewSh = videostreamPreviewSf.getHolder();



        surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {




                Log.d(TAG, "real onSurfaceTextureAvailable");
                videoViewWidth = videostreamPreviewSf.getWidth();
                videoViewHeight = videostreamPreviewSf.getHeight();

                Log.d(TAG, "real onSurfaceTextureAvailable3: width " + videoViewWidth + " height " + videoViewHeight);
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        if (mCodecManager == null) {
                            mCodecManager = new DJICodecManager(getApplicationContext(), holder, videoViewWidth,
                                    videoViewHeight);
                        }
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        // This demo might not work well on P3C and OSMO.
                        NativeHelper.getInstance().init();
                        DJIVideoStreamDecoder.getInstance().init(getApplicationContext(), holder.getSurface());
                        DJIVideoStreamDecoder.getInstance().resume();
                        break;
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                videoViewWidth = width;
                videoViewHeight = height;
                Log.d(TAG, "real onSurfaceTextureAvailable4: width " + videoViewWidth + " height " + videoViewHeight);
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        //mCodecManager.onSurfaceSizeChanged(videoViewWidth, videoViewHeight, 0);
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        DJIVideoStreamDecoder.getInstance().changeSurface(holder.getSurface());
                        break;
                }

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                switch (demoType) {
                    case USE_SURFACE_VIEW:
                        if (mCodecManager != null) {
                            mCodecManager.cleanSurface();
                            mCodecManager.destroyCodec();
                            mCodecManager = null;
                        }
                        break;
                    case USE_SURFACE_VIEW_DEMO_DECODER:
                        DJIVideoStreamDecoder.getInstance().stop();
                        NativeHelper.getInstance().release();
                        break;
                }

            }
        };

        videostreamPreviewSh.addCallback(surfaceCallback);
    }




    @Override
    public void onYuvDataReceived(MediaFormat format, final ByteBuffer yuvFrame, int dataSize, final int width, final int height) {
        //In this demo, we test the YUV data by saving it into JPG files.
        //DJILog.d(TAG, "onYuvDataReceived " + dataSize);
        if (yuvFrame != null) {
            final byte[] bytes = new byte[dataSize];
            yuvFrame.get(bytes);
            //DJILog.d(TAG, "onYuvDataReceived2 " + dataSize);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    // two samples here, it may has other color format.
                    int colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                    switch (colorFormat) {
                        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                            //NV12
                            if (Build.VERSION.SDK_INT <= 23) {
                                oldSaveYuvDataToJPEG(bytes, width, height);
                            } else {
                                newSaveYuvDataToJPEG(bytes, width, height);
                            }
                            break;
                        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                            //YUV420P
                            newSaveYuvDataToJPEG420P(bytes, width, height);
                            break;
                        default:
                            break;
                    }

                }
            });
        }
    }

    // For android API <= 23
    private void oldSaveYuvDataToJPEG(byte[] yuvFrame, int width, int height){
        if (yuvFrame.length < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return;
        }

        byte[] y = new byte[width * height];
        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        byte[] nu = new byte[width * height / 4]; //
        byte[] nv = new byte[width * height / 4];

        System.arraycopy(yuvFrame, 0, y, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            v[i] = yuvFrame[y.length + 2 * i];
            u[i] = yuvFrame[y.length + 2 * i + 1];
        }
        int uvWidth = width / 2;
        int uvHeight = height / 2;
        for (int j = 0; j < uvWidth / 2; j++) {
            for (int i = 0; i < uvHeight / 2; i++) {
                byte uSample1 = u[i * uvWidth + j];
                byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
                byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
                byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
                nu[2 * (i * uvWidth + j)] = uSample1;
                nu[2 * (i * uvWidth + j) + 1] = uSample1;
                nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
                nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
                nv[2 * (i * uvWidth + j)] = vSample1;
                nv[2 * (i * uvWidth + j) + 1] = vSample1;
                nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
                nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
            }
        }
        //nv21test
        byte[] bytes = new byte[yuvFrame.length];
        System.arraycopy(y, 0, bytes, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            bytes[y.length + (i * 2)] = nv[i];
            bytes[y.length + (i * 2) + 1] = nu[i];
        }
        Log.d(TAG,
                "onYuvDataReceived: frame index: "
                        + DJIVideoStreamDecoder.getInstance().frameIndex
                        + ",array length: "
                        + bytes.length);
        screenShot(bytes, Environment.getExternalStorageDirectory() + "/DJI_ScreenShot", width, height);
    }

    private void newSaveYuvDataToJPEG(byte[] yuvFrame, int width, int height){
        if (yuvFrame.length < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return;
        }
        int length = width * height;

        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        for (int i = 0; i < u.length; i++) {
            v[i] = yuvFrame[length + 2 * i];
            u[i] = yuvFrame[length + 2 * i + 1];
        }
        for (int i = 0; i < u.length; i++) {
            yuvFrame[length + 2 * i] = u[i];
            yuvFrame[length + 2 * i + 1] = v[i];
        }
        screenShot(yuvFrame,Environment.getExternalStorageDirectory() + "/DJI_ScreenShot", width, height);
    }

    private void newSaveYuvDataToJPEG420P(byte[] yuvFrame, int width, int height) {
        if (yuvFrame.length < width * height) {
            return;
        }
        int length = width * height;

        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];

        for (int i = 0; i < u.length; i ++) {
            u[i] = yuvFrame[length + i];
            v[i] = yuvFrame[length + u.length + i];
        }
        for (int i = 0; i < u.length; i++) {
            yuvFrame[length + 2 * i] = v[i];
            yuvFrame[length + 2 * i + 1] = u[i];
        }
        screenShot(yuvFrame, Environment.getExternalStorageDirectory() + "/DJI_ScreenShot", width, height);
    }

    /**
     * Save the buffered data into a JPG image file
     */
    private void screenShot(byte[] buf, String shotDir, int width, int height) {

        int size = buf.length;
        Changeframe(buf,size,width,height);
        /*
        File dir = new File(shotDir);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        YuvImage yuvImage = new YuvImage(buf,
                ImageFormat.NV21,
                width,
                height,
                null);
        OutputStream outputFile;


        String timeStamp = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        final String path = dir + "/ScreenShot_" + timeStamp + ".jpg";


        try {
            outputFile = new FileOutputStream(new File(path));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "test screenShot: new bitmap output file error: " + e);
            return;
        }
        if (outputFile != null) {
            yuvImage.compressToJpeg(new Rect(0,
                    0,
                    width,
                    height), 100, outputFile);
        }
        try {
            outputFile.close();
        } catch (IOException e) {
            Log.e(TAG, "test screenShot: compress yuv image error: " + e);
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayPath(path);
            }
        });

         */
    }


    public void onClick(View v) throws InterruptedException {

        if (v.getId() == R.id.activity_main_screen_shot) {
            handleYUVClick();
        } else {
            DemoType newDemoType = null;
            if (v.getId() == R.id.activity_main_screen_texture) {
                try{
                    //start();

                }catch (Exception e)
                {

                }
                //newDemoType = DemoType.USE_TEXTURE_VIEW;
            } else if (v.getId() == R.id.activity_main_screen_surface) {
                newDemoType = DemoType.USE_SURFACE_VIEW;
            } else if (v.getId() == R.id.activity_main_screen_surface_with_own_decoder) {
                newDemoType = DemoType.USE_SURFACE_VIEW_DEMO_DECODER;
            }

            if (newDemoType != null && newDemoType != demoType) {
                // Although finish will trigger onDestroy() is called, but it is not called before OnCreate of new activity.
                if (mCodecManager != null) {
                    mCodecManager.cleanSurface();
                    mCodecManager.destroyCodec();
                    mCodecManager = null;
                }
                demoType = newDemoType;
                finish();

                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            }
        }
    }

    private void clickyuv()
    {
        //screenShot.setText("YUV Screen Shot");
        //screenShot.setSelected(false);


        switch (demoType) {
            case USE_SURFACE_VIEW:

                break;
            case USE_TEXTURE_VIEW:
                mCodecManager.enabledYuvData(false);
                mCodecManager.setYuvDataCallback(null);
                // ToDo:
                break;
            case USE_SURFACE_VIEW_DEMO_DECODER:
                DJIVideoStreamDecoder.getInstance().changeSurface(videostreamPreviewSh.getSurface());
                DJIVideoStreamDecoder.getInstance().setYuvDataListener(null);
                break;
        }
        savePath.setText("");
        savePath.setVisibility(View.INVISIBLE);
        stringBuilder = null;
    }

    private void handleYUVClick() throws InterruptedException {

        if (screenShot.isSelected()) {
            text_screenShot.setText("관제 시작");
            text_screenShot.setTextColor(Color.WHITE);
            img_screenShot.setBackgroundResource(R.drawable.video_camera);
            screenShot.setSelected(false);


            switch (demoType) {
                case USE_SURFACE_VIEW:

                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);

                    /*
                    mCodecManager.enabledYuvData(false);
                    mCodecManager.setYuvDataCallback(null);
                    startActivity(getIntent());
                    hangup();
                    /*
                    localVideoView.setVisibility(View.INVISIBLE);
                    localVideoView = null;
                    hangup();

                    mCodecManager.enabledYuvData(false);
                    mCodecManager.setYuvDataCallback(null);
                    initPreviewerSurfaceView();
                    */

                    break;
                case USE_TEXTURE_VIEW:
                    mCodecManager.enabledYuvData(false);
                    mCodecManager.setYuvDataCallback(null);

                    // ToDo:
                    break;
                case USE_SURFACE_VIEW_DEMO_DECODER:
                    DJIVideoStreamDecoder.getInstance().changeSurface(videostreamPreviewSh.getSurface());
                    DJIVideoStreamDecoder.getInstance().setYuvDataListener(null);
                    break;
            }
            savePath.setText("");
            savePath.setVisibility(View.INVISIBLE);
            stringBuilder = null;
        } else {
            img_screenShot.setBackgroundResource(R.drawable.video_camera_pink);
            text_screenShot.setText("관제 중지");
            startBlinkingAnimation(text_screenShot);
            text_screenShot.setTextColor(Color.RED);
            screenShot.setSelected(true);

            switch (demoType) {
                case USE_TEXTURE_VIEW:

                    break;
                case USE_SURFACE_VIEW:
                    mCodecManager.enabledYuvData(true);
                    mCodecManager.setYuvDataCallback(this);
                    start();
                    break;
                case USE_SURFACE_VIEW_DEMO_DECODER:
                    DJIVideoStreamDecoder.getInstance().changeSurface(null);
                    DJIVideoStreamDecoder.getInstance().setYuvDataListener(Webrtc1.this);
                    break;
            }
            savePath.setText("");
            savePath.setVisibility(View.INVISIBLE);
        }

    }

    //깜빡
    public void startBlinkingAnimation(View view) {
        TextView textView = (TextView) findViewById(R.id.text_screenShot);
        Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);
        textView.startAnimation(startAnimation);
    }

    private void displayPath(String path) {
        if (stringBuilder == null) {
            stringBuilder = new StringBuilder();
        }

        path = path + "\n";
        stringBuilder.append(path);
        savePath.setText(stringBuilder.toString());
    }

    private boolean isTranscodedVideoFeedNeeded() {
        if (VideoFeeder.getInstance() == null) {
            return false;
        }

        return VideoFeeder.getInstance().isFetchKeyFrameNeeded() || VideoFeeder.getInstance()
                .isLensDistortionCalibrationNeeded();
    }
}
