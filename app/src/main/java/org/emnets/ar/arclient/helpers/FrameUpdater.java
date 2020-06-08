package org.emnets.ar.arclient.helpers;

import android.os.SystemClock;
import android.util.Log;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import org.emnets.ar.arclient.render.CameraPose;

public class FrameUpdater extends Thread {
    private static final String TAG="FrameUpdater";
    public boolean running = false;
    private final CameraPose cameraPose;
    private long last_update;
    private int count=0;

    public FrameUpdater(CameraPose cameraPose) {
        this.cameraPose = cameraPose;
        Thread thread = new Thread();
    }

    @Override
    public synchronized void start() {
        running = true;
        last_update = SystemClock.uptimeMillis();
        super.start();
    }

    @Override
    public void run() {
        while (running) {
            try {
                sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long elapsed = SystemClock.uptimeMillis()-last_update;
            if (cameraPose.updated) {
                count=0;
                synchronized (cameraPose){
                    cameraPose.updated=false;
                }
                Log.v(TAG,"lerp count: "+count);
                cameraPose.camera.setWorldRotation(Quaternion.slerp(cameraPose.getPreviousRotation(),cameraPose.getRotation(),elapsed/33f));
                cameraPose.camera.setWorldPosition(Vector3.lerp(cameraPose.getPreviousTranslation(),cameraPose.getTranslation(),elapsed/33f));
                count++;
                last_update = SystemClock.uptimeMillis();
            }else {
                Log.v(TAG,"lerp count: "+count);
                cameraPose.camera.setWorldRotation(Quaternion.slerp(cameraPose.getPreviousRotation(),cameraPose.getRotation(),elapsed/33f));
                cameraPose.camera.setWorldPosition(Vector3.lerp(cameraPose.getPreviousTranslation(),cameraPose.getTranslation(),elapsed/33f));
                count++;
            }

        }
    }
}
