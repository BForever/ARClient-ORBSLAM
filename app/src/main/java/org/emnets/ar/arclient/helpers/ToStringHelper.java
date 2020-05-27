package org.emnets.ar.arclient.helpers;

import com.google.ar.sceneform.math.Matrix;

public class ToStringHelper {
    public static String matrixToString(Matrix matrix){
        StringBuilder stringBuilder = new StringBuilder();
        for (int i=0;i<4;i++){
            if(i==0){
                stringBuilder.append("\n[");
            }else {
                stringBuilder.append(" ");
            }
            for(int j=0;j<4;j++){
                stringBuilder.append(" ");
                stringBuilder.append(matrix.data[i*4+j]);
                stringBuilder.append(",");
            }
            if(i==3){
                stringBuilder.append("]");
            }else {
                stringBuilder.append("\n");
            }
        }

        return stringBuilder.toString();
    }
}
