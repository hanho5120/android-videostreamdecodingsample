package com.dji.videostreamdecodingsample;
import android.annotation.SuppressLint;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class RemoteSocketClient {
    protected static Socket mSocket;
    protected static RemoteSocketClient INSTANCE;
    protected static RemoteSocketInterface.SocketListner mCallback;
    public static int count1;
    public static int count;

    public static synchronized RemoteSocketClient getInstance() {
        if ( null == INSTANCE ) {
            INSTANCE = new RemoteSocketClient();
        }

        return INSTANCE;
    }

    @SuppressLint("TrustAllX509TrustManager") final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
        public void checkClientTrusted(X509Certificate[] chain, String authType) { }
        public void checkServerTrusted(X509Certificate[] chain, String authType) { }
    }};

    public static Emitter.Listener onConnect = (args) -> {
        count++;
        mCallback.onConnect();
    };

    public static Emitter.Listener onConnectError = (args) -> {
        mCallback.onConnectError(args[0].toString());
    };

    public static Emitter.Listener onDisConnect = (args) -> {
        mCallback.onDisConnect(args[0].toString());
    };

    public static Emitter.Listener user_joined = (args) -> {
        String lsId = "";
        int liCount = 0;
        ArrayList<String> clients = new ArrayList<>();
        ArrayList<String> temp = new ArrayList<>();

        try {
            lsId = args[0].toString();
            liCount = Integer.parseInt(args[1].toString());

            JSONArray jsonArray = new JSONArray(args[2].toString());

            for ( int i = 0; i < jsonArray.length(); i++ ) {
                clients.add(jsonArray.getString(i));
            }

            JSONArray jsonArray1 = new JSONArray(args[3].toString());

            for ( int i = 0; i < jsonArray.length(); i++ ) {
                temp.add(jsonArray.getString(i));
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        finally {
            mCallback.user_joined(lsId, liCount, clients, temp);
        }
    };

    public static Emitter.Listener gotMessageFromServer = (args) -> {
        try {
            mCallback.gotMessageFromServer(args[0].toString(), new JSONObject(args[1].toString()));
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    };

/*
    public static Emitter.Listener user_left = (args) -> {
        mCallback.user_left(args[0].toString());
    };

    public static Emitter.Listener goChatMsg = (args) -> {
        mCallback.goChatMsg((JSONObject) args[0]);
    };

    public static Emitter.Listener goZoom = (args) -> {
        mCallback.goZoom(args[0].toString());
    };

    public static Emitter.Listener goFlash = (args) -> {
        mCallback.goFlash(args[0].toString());
    };

    public static Emitter.Listener goDraw = (args) -> {
        mCallback.goDraw((JSONObject) args[0]);
    };

    public static Emitter.Listener goMark = (args) -> {
        mCallback.goMark((JSONObject) args[0]);
    };

    public static Emitter.Listener goScreen = (args) -> {
        mCallback.goScreen(args[0].toString());
    };
    */


    /**
     * 소켓 연결
     * @param poCallback
     */
    public void init(RemoteSocketInterface.SocketListner poCallback) {
        if(mSocket!=null)
            return;

        this.mCallback = poCallback;

        close();
        count1++;
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, trustAllCerts, null);
            IO.setDefaultHostnameVerifier((hostname, session) -> true);
            IO.setDefaultSSLContext(sslcontext);
            mSocket = IO.socket(Userdata.getInstance()._server_url);
            mSocket.on(Socket.EVENT_CONNECT, onConnect);
            mSocket.on(Socket.EVENT_DISCONNECT, onDisConnect);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.on("user-joined", user_joined);
            mSocket.on("signal", gotMessageFromServer);

            mSocket.connect();

        }
        catch ( Exception e ) {
            e.printStackTrace();
            mSocket.close();
            mSocket = null;
        }
    }

    /**
     * 소켓 닫기
     */
    public void close() {
        if ( null != mSocket && mSocket.connected() ) {
            mSocket.disconnect();
        }

        if ( null != mSocket ) {
            mSocket.close();
            mSocket = null;
        }
    }

    /**
     * 소켓 연결상태 확인
     * @return
     */
    public boolean getSocketStatus() {
        return (null != mSocket && mSocket.connected());
    }

    /**
     * 소켓 객체 정보
     * @return
     */
    public Socket getSocket() {
        return mSocket;
    }

    /**
     * 소켓 아이디
     * @return
     */
    public String getSocketId() {
        return mSocket.id();
    }

}