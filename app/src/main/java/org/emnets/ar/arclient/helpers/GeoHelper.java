package org.emnets.ar.arclient.helpers;

import android.util.Log;

import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.MathHelper;
import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import org.emnets.ar.arclient.render.CameraPose;

import static com.google.ar.sceneform.math.Quaternion.multiply;

public class GeoHelper {
    static private String TAG = "GeoHelper";

    static public Vector3 MatrixToEularAngles(Matrix m) {
        double sy = Math.sqrt(m.data[0] * m.data[0] + m.data[4] * m.data[4]);
        boolean singular = sy < 1e-6; // If

        double x, y, z;
        if (!singular) {
            x = Math.atan2(m.data[9],m.data[10]);
            y = Math.atan2(-m.data[8],sy);
            z = Math.atan2(m.data[4],m.data[0]);
        } else {
            x = Math.atan2(-m.data[6],m.data[5]);
            y = Math.atan2(-m.data[8],sy);
            z = 0;
        }
        return new Vector3((float)x, (float)y, (float)z);
    }

    static public Vector3 ToEulerAngles(Quaternion q) {
        Vector3 angles = new Vector3();

        // roll (x-axis rotation)
        double sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
        double cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);
        angles.x = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        double sinp = 2 * (q.w * q.y - q.z * q.x);
        if (Math.abs(sinp) >= 1)
            angles.y = (float) Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        else
            angles.y = (float) Math.asin(sinp);

        // yaw (z-axis rotation)
        double siny_cosp = 2 * (q.w * q.z + q.x * q.y);
        double cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);
        angles.z = (float) Math.atan2(siny_cosp, cosy_cosp);

        return angles;
    }

    public static Ray cameraToRay(CameraPose cameraPose) {
        Vector3 var3 = new Vector3();
        Vector3 var4 = new Vector3();
        unproject(cameraPose, 0.0F, var3);
        Log.e(TAG, "origin: " + var3);
        unproject(cameraPose, 1.0F, var4);
        Log.e(TAG, "target: " + var4);
        Vector3 var5 = Vector3.subtract(var4, var3).normalized();
        Log.e(TAG, "direction: " + var5);
        return new Ray(var3, var5);
    }

    private static boolean unproject(CameraPose cameraPose, float var3, Vector3 var4) {
        Matrix var5 = new Matrix();
        Matrix.multiply(cameraPose.camera.getProjectionMatrix(), cameraPose.getViewMatrix(), var5);
        Matrix.invert(var5, var5);
        float var1 = 0f;
        float var2 = 0F;
        var3 = var3 * 2.0F - 1.0F;
        var4.x = var1 * var5.data[0] + var2 * var5.data[4] + var3 * var5.data[8] + 1.0F * var5.data[12];
        var4.y = var1 * var5.data[1] + var2 * var5.data[5] + var3 * var5.data[9] + 1.0F * var5.data[13];
        var4.z = var1 * var5.data[2] + var2 * var5.data[6] + var3 * var5.data[10] + 1.0F * var5.data[14];
        float var8;
        if (MathHelper.almostEqualRelativeAndAbs(var8 = var1 * var5.data[3] + var2 * var5.data[7] + var3 * var5.data[11] + 1.0F * var5.data[15], 0.0F)) {
            var4.set(0.0F, 0.0F, 0.0F);
            return false;
        } else {
            var8 = 1.0F / var8;
            var4.set(var4.scaled(var8));
            return true;
        }
    }
}
