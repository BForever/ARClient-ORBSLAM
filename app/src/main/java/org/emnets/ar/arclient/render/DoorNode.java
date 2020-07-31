package org.emnets.ar.arclient.render;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import org.emnets.ar.arclient.R;

public class DoorNode extends Node implements Expandable{
    static final String TAG="DoorNode";
    private Button button;
    private ViewRenderable renderable;
    private ViewRenderable label;

    public DoorNode(NodeParent parent, Context context) {
        this.setParent(parent);
        this.setEnabled(true);
        setupView(context);
        setupLabel(context);
    }

    void buttonOnClick (View view) {
        Log.v(TAG,"onClick");
    }

    private void lableOnClick(View view) {
        this.setRenderable(renderable);
    }

    @Override
    public void unExpand() {
        this.setRenderable(label);
    }

    private void setupLabel(Context context) {
        ViewRenderable.builder()
                .setView(context, R.layout.card_view)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.setRenderable(renderable);
                            this.label = renderable;
                            TextView textView = (TextView) renderable.getView();
                            textView.setText("会议室104");
                            textView.setOnClickListener(this::lableOnClick);
                            renderable.setSizer(view -> new Vector3(0.4f, 0.2f, 1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });
    }
    private void setupView(Context context){
        ViewRenderable.builder()
                .setView(context, R.layout.door_view)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.renderable =renderable;
                            View doorView = renderable.getView();
                            button = doorView.findViewById(R.id.enter_button);
                            renderable.setSizer(view -> new Vector3(1.2f,0.6f,1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });

    }
}
