package org.emnets.ar.arclient.render;

import android.util.Log;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import org.emnets.ar.arclient.Matrix;
import org.emnets.ar.arclient.helpers.GeoHelper;
import org.emnets.ar.arclient.helpers.ToStringHelper;

public class CameraPose {
    static final String TAG="CameraPose";
    public Camera camera;
    private com.google.ar.sceneform.math.Matrix viewMatrix= new com.google.ar.sceneform.math.Matrix();
    private com.google.ar.sceneform.math.Matrix worldModeMatrix= new com.google.ar.sceneform.math.Matrix();
    Vector3 translation = new Vector3();
    Quaternion rotation = new Quaternion();


    public CameraPose(Camera camera){
        this.camera = camera;
    }


    public void setViewMatrix(Matrix viewMatrix){
        float[] temp = {(float) viewMatrix.getR0C0(), (float) viewMatrix.getR1C0(), (float) viewMatrix.getR2C0(), (float) viewMatrix.getR3C0(),
                (float) viewMatrix.getR0C1(), (float) viewMatrix.getR1C1(), (float) viewMatrix.getR2C1(), (float) viewMatrix.getR3C1(),
                (float) viewMatrix.getR0C2(), (float) viewMatrix.getR1C2(), (float) viewMatrix.getR2C2(), (float) viewMatrix.getR3C2(),
                (float) viewMatrix.getR0C3(), (float) viewMatrix.getR1C3(), (float) viewMatrix.getR2C3(), (float) viewMatrix.getR3C3()};
        com.google.ar.sceneform.math.Matrix transform = new com.google.ar.sceneform.math.Matrix();
        transform.set(com.google.ar.sceneform.math.Matrix.IDENTITY_DATA);
        transform.data[5]=-1;
        transform.data[10]=-1;

        synchronized (this){
            this.worldModeMatrix.set(temp);
            com.google.ar.sceneform.math.Matrix.multiply(transform,this.worldModeMatrix,this.worldModeMatrix);
            com.google.ar.sceneform.math.Matrix.multiply(this.worldModeMatrix,transform,this.worldModeMatrix);
            com.google.ar.sceneform.math.Matrix.invert(worldModeMatrix,this.viewMatrix);
//            Log.e(TAG,"after trans: "+ToStringHelper.matrixToString(this.worldModeMatrix));
            worldModeMatrix.decomposeTranslation(translation);
            worldModeMatrix.extractQuaternion(rotation);
        }

    }

    public Vector3 getTranslation(){
        synchronized (this){
            return translation;
        }
    }

    public Quaternion getRotation(){
        synchronized (this){
            return rotation;
        }

    }

    Vector3 getUpVector(){
        synchronized (this){
            Vector3 up = new Vector3();
            up.x = viewMatrix.data[1];
            up.y = viewMatrix.data[5];
            up.z = viewMatrix.data[9];
            up = up.normalized();
            return up;
        }

    }

    public com.google.ar.sceneform.math.Matrix getViewMatrix() {
        synchronized (this){
            return viewMatrix;
        }
    }
}
