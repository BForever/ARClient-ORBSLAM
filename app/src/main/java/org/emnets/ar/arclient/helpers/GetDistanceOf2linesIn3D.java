package org.emnets.ar.arclient.helpers;

import android.util.Log;

import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Vector3;

public class GetDistanceOf2linesIn3D {
    private final String TAG = "GetDistanceOf2linesIn3D";
    private Ray rayA,rayB;
    private boolean Aset=false;

    private Vector3 PonA,PonB;//两直线最近点之A线上的点
    private float distance;//两直线距离
    public void setRay(Ray ray){
        if(Aset){
            Log.e(TAG,"line b set");
            rayB = ray;
            Aset = false;
        }else {
            Log.e(TAG,"line a set");
            rayA = ray;
            Aset = true;
        }
    }

    public boolean twoLineSet(){
        return !Aset;
    }
    //用SetLineA、SetLineB输入A、B方程后
    //调用本函数解出结果
    public void compute() {
        Vector3 e = Vector3.subtract(rayB.getOrigin(),rayA.getOrigin());
        Vector3 cross_e_db = Vector3.cross(e,rayB.getDirection());
        Vector3 cross_e_da = Vector3.cross(e,rayA.getDirection());
        Vector3 cross_da_db = Vector3.cross(rayA.getDirection(),rayB.getDirection());
        float t1, t2;
        t1 = Vector3.dot(cross_e_db,cross_da_db);
        t2 = Vector3.dot(cross_e_da,cross_da_db);
        double dd =cross_da_db.length();
        t1 /= dd * dd;
        t2 /= dd * dd;
        PonA = Vector3.add(rayA.getOrigin(),rayA.getDirection().scaled(t1));
        PonB = Vector3.add(rayB.getOrigin(),rayB.getDirection().scaled(t2));
        distance = Vector3.subtract(PonB,PonA).length();
    }

    public float getDistance(){
        Log.e(TAG,"distance: "+distance);
        return distance;
    }

    public Vector3 getPonA() {
        return PonA;
    }

    public Vector3 getPonB() {
        return PonB;
    }

    public Ray getRayA() {
        return rayA;
    }

    public Ray getRayB() {
        return rayB;
    }

    public Vector3 getMidPoint(){
        return Vector3.add(PonA,Vector3.subtract(PonB,PonA).scaled(0.5f));
    }
}
