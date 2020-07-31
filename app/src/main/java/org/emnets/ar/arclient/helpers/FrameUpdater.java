package org.emnets.ar.arclient.helpers;

import android.os.SystemClock;
import android.util.Log;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import org.emnets.ar.arclient.render.CameraPose;

public class FrameUpdater extends Thread {
    private static final String TAG="FrameUpdater";
    public static final int delayedFrames = 3;
    public boolean running = false;
    private final CameraPose cameraPose;
    private long last_update;
    private int count=0;
    private Vector3 translationFirst = null;
    private Vector3 translationLatest = null;
    private Quaternion rotationFirst = null;
    private Quaternion rotationLatest = null;

    public FrameUpdater(CameraPose cameraPose) {
        this.cameraPose = cameraPose;
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
                sleep(4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long elapsed = SystemClock.uptimeMillis()-last_update;
            if (cameraPose.updated) {
                count=0;
                elapsed = 0;
                synchronized (cameraPose){
                    cameraPose.updated=false;
                }
                Log.w(TAG,"lerp count: "+count);
                if(translationFirst ==null){
                    translationLatest = translationFirst = cameraPose.getHistoryTranslation();
                    rotationLatest = rotationFirst = cameraPose.getHistoryRotation();
                }
                Quaternion slerped = Quaternion.slerp(rotationFirst, rotationLatest,1f/(delayedFrames-1));
                Vector3 lerped = Vector3.lerp(translationFirst, translationLatest,1f/(delayedFrames-1));
                cameraPose.camera.setWorldRotation(slerped);
                cameraPose.camera.setWorldPosition(lerped);
                // 更新状态
                rotationFirst = slerped;
                rotationLatest = cameraPose.getRotation();
                translationFirst = lerped;
                translationLatest = cameraPose.getTranslation();

                count++;
                last_update = SystemClock.uptimeMillis();
            }else {
                if(elapsed>33|| rotationLatest ==null){
                    continue;
                }
                Log.w(TAG,"lerp count: "+count);
                cameraPose.camera.setWorldRotation(Quaternion.slerp(rotationFirst, rotationLatest,elapsed/(33f*(delayedFrames-1))));
                cameraPose.camera.setWorldPosition(Vector3.lerp(translationFirst, translationLatest,elapsed/(33f*(delayedFrames-1))));
                count++;
            }
        }
    }
}
