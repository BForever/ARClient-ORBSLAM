package com.google.ar.sceneform.samples.augmentedimage;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

public class LabelBox extends Node {
    private String name;
    private float center_x,center_y,width,height;//origin is left-top
    private float imageScale;

    private Node infoCard;
    private Context context;
    private static final float INFO_CARD_Y_POS_COEFF = 0.55f;

    public LabelBox(Context context,String name,float center_x,float center_y,float width,float height){
        this.context=context;
        this.name=name;
        this.center_x=center_x;
        this.center_y=center_y;
        this.width=width;
        this.height=height;
    }
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void onActivate() {
        if (getScene() == null) {
            throw new IllegalStateException("Scene is null!");
        }

        if (infoCard == null) {
            infoCard = new Node();
            infoCard.setParent(this);
            infoCard.setEnabled(true);
            infoCard.setLocalPosition(new Vector3(0.0f, height * INFO_CARD_Y_POS_COEFF, 0.0f));

            ViewRenderable.builder()
                    .setView(context, R.layout.card_view)
                    .build()
                    .thenAccept(
                            (renderable) -> {
                                infoCard.setRenderable(renderable);
                                TextView textView = (TextView) renderable.getView();
                                textView.setText(name);
                            })
                    .exceptionally(
                            (throwable) -> {
                                throw new AssertionError("Could not load plane card view.", throwable);
                            });
        }
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        if (infoCard == null) {
            return;
        }

        // Typically, getScene() will never return null because onUpdate() is only called when the node
        // is in the scene.
        // However, if onUpdate is called explicitly or if the node is removed from the scene on a
        // different thread during onUpdate, then getScene may be null.
        if (getScene() == null) {
            return;
        }
        Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
        Vector3 cardPosition = infoCard.getWorldPosition();
        Vector3 direction = Vector3.subtract(cameraPosition, cardPosition);
        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
        infoCard.setWorldRotation(lookRotation);
    }
}
