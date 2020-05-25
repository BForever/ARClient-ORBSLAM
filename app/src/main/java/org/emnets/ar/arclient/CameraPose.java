package org.emnets.ar.arclient;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import org.emnets.ar.arclient.helpers.GeoHelper;

public class CameraPose {
    public Camera camera;
    public com.google.ar.sceneform.math.Matrix viewmatrix;
    public com.google.ar.sceneform.math.Matrix worldModeMatrix;

    CameraPose(Camera camera){
        this.camera = camera;
        worldModeMatrix = new com.google.ar.sceneform.math.Matrix();
    }

    void setViewMatrix(Matrix viewMatrix){
        float[] temp = {(float) viewMatrix.getR0C0(), (float) viewMatrix.getR1C0(), (float) viewMatrix.getR2C0(), (float) viewMatrix.getR3C0(),
                (float) viewMatrix.getR0C1(), (float) viewMatrix.getR1C1(), (float) viewMatrix.getR2C1(), (float) viewMatrix.getR3C1(),
                (float) viewMatrix.getR0C2(), (float) viewMatrix.getR1C2(), (float) viewMatrix.getR2C2(), (float) viewMatrix.getR3C2(),
                (float) viewMatrix.getR0C3(), (float) viewMatrix.getR1C3(), (float) viewMatrix.getR2C3(), (float) viewMatrix.getR3C3()};
        viewmatrix = new com.google.ar.sceneform.math.Matrix(temp);
        com.google.ar.sceneform.math.Matrix.invert(viewmatrix,worldModeMatrix);
    }

    Vector3 getTranslation(){
        Vector3 translation = new Vector3();
        worldModeMatrix.decomposeTranslation(translation);
        translation.x = -translation.x;
        return translation;
    }

    Quaternion getRotation(){
        Quaternion rotation = new Quaternion();
        worldModeMatrix.extractQuaternion(rotation);
        Vector3 angles = GeoHelper.ToEulerAngles(rotation);
        return GeoHelper.eulerAngles(angles);
    }

    Vector3 getUpVector(){
        Vector3 up = new Vector3();
        up.y = worldModeMatrix.data[1];
        up.x = worldModeMatrix.data[5];
        up.z = worldModeMatrix.data[9];
        up = up.normalized();
        return GeoHelper.ToEulerAngles(GeoHelper.eulerAngles(up));
    }

    public com.google.ar.sceneform.math.Matrix getViewMatrix() {
        return viewmatrix;
    }
}
