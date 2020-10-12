package com.dji.videostreamdecodingsample;

import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoTrack;

import java.util.List;

public class RemoteDTO {
    public interface RemoteDTODeleteLister {
        void deleteSucc();
    }

    private String socketId;
    public PeerConnection peerConnection = null;
    private VideoTrack videoTrack;
    private AudioTrack audioTrack;
    private MediaStream stream;
    private boolean sharedFlag = false;

    public RemoteDTO() {

    }

    public String getSocketId(){
        return socketId;
    }

    public void setSocketId(String socketid)
    {
        socketId = socketid;
    }

    public PeerConnection getPeerConnection()
    {
        return peerConnection;
    }

    public void createPeerConnection(PeerConnectionFactory poFactory, List<PeerConnection.IceServer> poPeerIceServers, MediaConstraints poPeerConnConstraints, RemoteSocketInterface.PeerCreateLister poCallback) {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(poPeerIceServers);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;

        peerConnection = poFactory.createPeerConnection(rtcConfig, poPeerConnConstraints, new CustomPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                poCallback.onIceCandidate(iceCandidate, socketId);
            }

            @Override

            public void onAddStream(MediaStream mediaStream) {
                //mediaStream.audioTracks.get(0).setEnabled(false);
                super.onAddStream(mediaStream);
                if ( isSharedFlag() ) {
                    try {
                        videoTrack = mediaStream.videoTracks.get(0);
                    }
                    catch ( Exception e ) {
                        videoTrack = null;
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * addStream
     */
    public void addSteam(PeerConnectionFactory poFactory, AudioTrack poLocalAudioTrack, VideoTrack poLocalVideoTrack) {
        stream = poFactory.createLocalMediaStream("102");
        stream.addTrack(poLocalVideoTrack);
        stream.addTrack(poLocalAudioTrack);
       // stream.audioTracks.get(0).setEnabled(false);
        peerConnection.addStream(stream);
    }

    /**
     * 오퍼 생성
     */
    public void createOffer(MediaConstraints poPeerConnConstraints, RemoteSocketInterface.PeerCreateOfferListner poCallback) {
        peerConnection.createOffer(new CustomSdpObserver("localCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                poCallback.onOffer(sessionDescription);
            }

            @Override
            public void onCreateFailure(String s) {
                super.onCreateFailure(s);
            }

            @Override
            public void onSetFailure(String s) {
                super.onSetFailure(s);
            }

            @Override
            public void onSetSuccess() {
                super.onSetSuccess();
            }

        }, poPeerConnConstraints);
    }

    public void remove(AudioTrack poLocalAudioTrack, VideoTrack poLocalVideoTrack, RemoteDTODeleteLister poCallback) {
    }

    public boolean isSharedFlag()
    {
        return true;
    }


}