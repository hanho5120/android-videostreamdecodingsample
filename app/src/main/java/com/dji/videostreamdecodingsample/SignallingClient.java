package com.dji.videostreamdecodingsample;

import android.annotation.SuppressLint;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Webrtc_Step3
 * Created by vivek-3102 on 11/03/17.
 */

class SignallingClient {
    private static SignallingClient instance;
    private String roomName = null;
    private Socket socket;
    boolean isChannelReady = false;
    boolean isInitiator = false;
    boolean isStarted = false;
    private SignalingInterface callback;

    //This piece of code should not go into production!!
    //This will help in cases where the node server is running in non-https server and you want to ignore the warnings
    @SuppressLint("TrustAllX509TrustManager")
    private final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) {
        }
    }};

    public static SignallingClient getInstance() {
        if (instance == null) {
            instance = new SignallingClient();
        }
        if (instance.roomName == null) {
            //set the room name here
            instance.roomName = "12345";
        }
        return instance;
    }

    public void init(SignalingInterface signalingInterface) {
        this.callback = signalingInterface;
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, trustAllCerts, null);
            IO.setDefaultHostnameVerifier((hostname, session) -> true);
            IO.setDefaultSSLContext(sslcontext);
            //set the socket.io url here
            socket = IO.socket("https://101.55.28.64:444");
            socket.connect();
            Log.d("SignallingClient", "init() called");

            if (!roomName.isEmpty()) {
                emitInitStatement(roomName);
            }

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
                socket.emit("reg_login_id",datalogin);
            }catch(JSONException e) {
                e.printStackTrace();
            }

            socket.emit("join_room",Userdata.getInstance()._room_id,Userdata.getInstance()._room_key);
            socket.emit("join_room","roomid","roomkey");

            //room created event.
            socket.on("dron_created", args -> {
                Log.d("SignallingClient", "created call() called with: args = [" + Arrays.toString(args) + "]");
                isInitiator = true;
                callback.onCreatedRoom();
            });

            //room is full event
            socket.on("dron_full", args -> Log.d("SignallingClient", "full call() called with: args = [" + Arrays.toString(args) + "]"));

            //peer joined event
            socket.on("join", args -> {
                Log.d("SignallingClient", "join call() called with: args = [" + Arrays.toString(args) + "]");
                isChannelReady = true;
                callback.onNewPeerJoined();
            });

            //when you joined a chat room successfully
            socket.on("dron_joined", args -> {
                Log.d("SignallingClient", "joined call() called with: args = [" + Arrays.toString(args) + "]");
                isChannelReady = true;
                callback.onJoinedRoom();
            });

            //log event
            socket.on("log", args -> Log.d("SignallingClient", "log call() called with: args = [" + Arrays.toString(args) + "]"));

            //bye event
            socket.on("bye", args -> callback.onRemoteHangUp((String) args[0]));

            //messages - SDP and ICE candidates are transferred through this
            socket.on("signal", args -> {
                Log.d("SignallingClient", "message call() called with: args = [" + Arrays.toString(args) + "]");
                if (args[0] instanceof String) {
                    Log.d("SignallingClient", "String received :: " + args[0]);
                    String data = (String) args[0];
                    if (data.equalsIgnoreCase("got user media")) {
                        callback.onTryToStart();
                    }
                    if (data.equalsIgnoreCase("bye")) {
                        callback.onRemoteHangUp(data);
                    }
                } else if (args[0] instanceof JSONObject) {
                    try {

                        JSONObject data = (JSONObject) args[0];
                        Log.d("SignallingClient", "Json Received :: " + data.toString());
                        String type = data.getString("type");
                        if (type.equalsIgnoreCase("offer")) {
                            callback.onOfferReceived(data);
                        } else if (type.equalsIgnoreCase("answer") && isStarted) {
                            callback.onAnswerReceived(data);
                        } else if (type.equalsIgnoreCase("candidate") && isStarted) {
                            if(data!=null)
                            {
                               // Sendcandi(new IceCandidate(data.getString("id"), data.getInt("label"), data.getString("candidate")));
                               callback.onIceCandidateReceived(data);
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    private void emitInitStatement(String message) {
        Log.d("SignallingClient", "emitInitStatement() called with: event = [" + "create or join" + "], message = [" + message + "]");
        socket.emit("create or join", message);
    }

    public void emitMessage(String message) {
        Log.d("SignallingClient", "emitMessage() called with: message = [" + message + "]");
        socket.emit("signal", message);
    }

    public void Sendcandi(IceCandidate iceCandidate)
    {

        try {
            JSONObject object = new JSONObject();
            object.put("type", "candidate");
            object.put("label", iceCandidate.sdpMLineIndex);
            object.put("id", iceCandidate.sdpMid);
            object.put("candidate", iceCandidate.sdp);
            socket.emit("signal", object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void emitMessage(SessionDescription message) {
        try {
            Log.d("SignallingClient", "emitMessage() called with: message = [" + message + "]");
            JSONObject obj = new JSONObject();
            obj.put("type", message.type.canonicalForm());
            obj.put("sdp", message.description);
            Log.d("emitMessage", obj.toString());
            socket.emit("signal", obj);
            Log.d("vivek1794", obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void emitIceCandidate(IceCandidate iceCandidate) {
        try {
            JSONObject object = new JSONObject();
            object.put("type", "candidate");
            object.put("label", iceCandidate.sdpMLineIndex);
            object.put("id", iceCandidate.sdpMid);
            object.put("candidate", iceCandidate.sdp);
            socket.emit("signal", object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void close() {
        if(socket==null) return;
        socket.emit("bye", "12345");
        socket.disconnect();
        socket.close();
        socket = null;
        instance = null;
    }

    interface SignalingInterface {
        void onRemoteHangUp(String msg);

        void onOfferReceived(JSONObject data);

        void onAnswerReceived(JSONObject data);

        void onIceCandidateReceived(JSONObject data);

        void onTryToStart();

        void onCreatedRoom();

        void onJoinedRoom();

        void onNewPeerJoined();
    }
}
