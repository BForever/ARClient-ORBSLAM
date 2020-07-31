package org.emnets.ar.arclient.render;

import android.os.SystemClock;
import android.util.Log;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import org.emnets.ar.arclient.Matrix;
import org.emnets.ar.arclient.MatrixBlock;
import org.emnets.ar.arclient.helpers.CircleQueue;
import org.emnets.ar.arclient.helpers.FrameUpdater;
import org.emnets.ar.arclient.helpers.GeoHelper;
import org.emnets.ar.arclient.helpers.ToStringHelper;

public class CameraPose {
    static final String TAG = "CameraPose";
    public Camera camera;
    public boolean updated = false;
    private com.google.ar.sceneform.math.Matrix viewMatrix = new com.google.ar.sceneform.math.Matrix();
    private com.google.ar.sceneform.math.Matrix worldModeMatrix = new com.google.ar.sceneform.math.Matrix();
    private Vector3 translation = new Vector3();
    private Quaternion rotation = new Quaternion();
    private final int queueLength= FrameUpdater.delayedFrames-1;
    private CircleQueue translations = new CircleQueue(queueLength);
    private CircleQueue rotations = new CircleQueue(queueLength);


    public CameraPose(Camera camera) {
        this.camera = camera;
    }


    public void setViewMatrix(MatrixBlock viewMatrix) {
        float[] temp = {(float) viewMatrix.getR0C0(), (float) viewMatrix.getR1C0(), (float) viewMatrix.getR2C0(), (float) viewMatrix.getR3C0(),
                (float) viewMatrix.getR0C1(), (float) viewMatrix.getR1C1(), (float) viewMatrix.getR2C1(), (float) viewMatrix.getR3C1(),
                (float) viewMatrix.getR0C2(), (float) viewMatrix.getR1C2(), (float) viewMatrix.getR2C2(), (float) viewMatrix.getR3C2(),
                (float) viewMatrix.getR0C3(), (float) viewMatrix.getR1C3(), (float) viewMatrix.getR2C3(), (float) viewMatrix.getR3C3()};
        com.google.ar.sceneform.math.Matrix transform = new com.google.ar.sceneform.math.Matrix();
        transform.set(com.google.ar.sceneform.math.Matrix.IDENTITY_DATA);
        transform.data[5] = -1;
        transform.data[10] = -1;

        synchronized (this) {
            this.worldModeMatrix.set(temp);
            com.google.ar.sceneform.math.Matrix.multiply(transform, this.worldModeMatrix, this.worldModeMatrix);
            com.google.ar.sceneform.math.Matrix.multiply(this.worldModeMatrix, transform, this.worldModeMatrix);
            com.google.ar.sceneform.math.Matrix.invert(worldModeMatrix, this.viewMatrix);
            updated = true;
        }
        Vector3 t = new Vector3();
        Quaternion r = new Quaternion();
        worldModeMatrix.decomposeTranslation(t);
        translations.push(t);
        worldModeMatrix.extractQuaternion(r);
        rotations.push(r);
    }

    public Vector3 getTranslation() {
        Vector3 res = (Vector3) translations.getLatest();
        if (res == null) {
            return new Vector3();
        } else {
            return res;
        }
    }
    public Vector3 getPreviousTranslation(){
        if(translations.size>=2){
            Object[] list = translations.getSortedQueue();
            return (Vector3)list[queueLength-2];
        }else{
            return getTranslation();
        }
    }
    public Vector3 getHistoryTranslation(){
        if(translations.size>=2){
            Object[] list = translations.getSortedQueue();
            return (Vector3)list[0];
        }else{
            return getTranslation();
        }
    }

    public Vector3 getTranslationSmoothed() {
        if (translations.full()) {
            Object[] list = translations.getSortedQueue();
            Vector3 lerped = Vector3.lerp((Vector3) list[0], (Vector3) list[1], 0.7f);
            return lerped;
        }
        return (Vector3) translations.getLatest();
    }

    public Quaternion getRotation() {
        Quaternion res = (Quaternion) rotations.getLatest();
        if (res == null) {
            return new Quaternion();
        } else {
            return res;
        }
    }
    public Quaternion getPreviousRotation(){
        if(rotations.size>=2){
            Object[] list = rotations.getSortedQueue();
            return (Quaternion)list[queueLength-2];
        }else{
            return getRotation();
        }
    }
    public Quaternion getHistoryRotation(){
        if(rotations.size>=2){
            Object[] list = rotations.getSortedQueue();
            return (Quaternion)list[0];
        }else{
            return getRotation();
        }
    }
    public Quaternion getRotationSmoothed() {
        if (rotations.full()) {
            Object[] list = rotations.getSortedQueue();
            Quaternion slerped = Quaternion.slerp((Quaternion) list[0], (Quaternion) list[1], 0.7f);
            return slerped;
        }
        return (Quaternion) rotations.getLatest();
    }

    public Vector3 getUpVector() {
        synchronized (this) {
            Vector3 up = new Vector3();
            up.x = viewMatrix.data[1];
            up.y = viewMatrix.data[5];
            up.z = viewMatrix.data[9];
            up = up.normalized();
            return up;
        }

    }

    public com.google.ar.sceneform.math.Matrix getViewMatrix() {
        synchronized (this) {
            return viewMatrix;
        }
    }
}
