package org.emnets.ar.arclient.helpers;

import android.util.Log;

import com.pedro.rtsp.utils.ConnectCheckerRtsp;

public class RtspConnectionChecker implements ConnectCheckerRtsp {
    @Override
    public void onConnectionSuccessRtsp() {
        Log.e("RTSP", "ConnectionSuccess");
    }

    @Override
    public void onConnectionFailedRtsp(String reason) {
        Log.e("RTSP", "ConnectionFailed");
    }

    @Override
    public void onNewBitrateRtsp(long bitrate) {
        Log.e("RTSP", "NewBitrateRtsp:" + Long.toString(bitrate));
    }

    @Override
    public void onDisconnectRtsp() {
        Log.e("RTSP", "Disconnect");
    }

    @Override
    public void onAuthErrorRtsp() {
        Log.e("RTSP", "AuthError");
    }

    @Override
    public void onAuthSuccessRtsp() {
        Log.e("RTSP", "AuthSuccess");
    }
}
