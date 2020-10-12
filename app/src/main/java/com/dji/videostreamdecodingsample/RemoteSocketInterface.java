package com.dji.videostreamdecodingsample;

import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

public class RemoteSocketInterface {
    interface SocketListner {
        void onConnect();
        void onConnectError(String psMsg) ;
        void onDisConnect(String psMsg);
        void user_joined(String psId, int piCount, ArrayList<String> poClients , ArrayList<String> temp);
        void user_left(String psId);
        void gotMessageFromServer(String psId, JSONObject signal);
        void goChatMsg(JSONObject poData);
        void onClosed();
        void onReloaded();
        void goZoom(String psMsg);
        void goFlash(String psMsg);
        void goDraw(JSONObject data);
        void goMark(JSONObject data);
        void goScreen(String psMsg);
        void onZoomChanged(int level);
        void onFlashChanged();
    }

    interface PeerCreateLister{
        void onIceCandidate(IceCandidate poIceCandidate, String psSocketId);
        void onAddStream(MediaStream mediaStream, String socketId);
    }

    interface  PeerCreateOfferListner{
         void onOffer(SessionDescription sessionDescription);
    }
}
