package org.emnets.ar.arclient.render;

import android.content.Context;
import android.widget.TextView;

import com.google.ar.core.Pose;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import org.emnets.ar.arclient.R;

public class LabelNode extends Node {
    private boolean lookRotate = true;
    private static final float INFO_CARD_Y_POS_COEFF = 0f;

    public LabelNode(NodeParent parent, Context context, String text) {
        this.setParent(parent);
        this.setEnabled(true);
        this.setLocalPosition(new Vector3(0.0f, INFO_CARD_Y_POS_COEFF, -1f));

        setupView(context,text);
    }

    public LabelNode(NodeParent parent, Context context, String text, Vector3 position) {
        this.setParent(parent);
        this.setEnabled(true);
        this.setLocalPosition(position);

        setupView(context,text);
    }

    public LabelNode(NodeParent parent, Context context, String text, Vector3 position,Quaternion rotation) {
        this.setParent(parent);
        this.setEnabled(true);
        this.setLocalPosition(position);
        this.setLocalRotation(rotation);
        this.lookRotate = false;

        setupView(context,text);
    }

    public LabelNode(NodeParent parent, Context context, Pose pose, String text) {
        this.setParent(parent);
        this.setEnabled(true);
        this.setLocalPosition(new Vector3(pose.tx(), pose.ty(), -pose.tz()));

        setupView(context,text);
    }

    private void setupView(Context context,String text){
        ViewRenderable.builder()
                .setView(context, R.layout.planet_card_view)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.setRenderable(renderable);
                            TextView textView = (TextView) renderable.getView();
                            textView.setText(text);
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load plane card view.", throwable);
                        });
    }
    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
        if(lookRotate) {
            Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
            Vector3 cardPosition = this.getWorldPosition();
            Vector3 direction = Vector3.subtract(cameraPosition, cardPosition);
            Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
            this.setWorldRotation(lookRotation);
        }
    }
}
