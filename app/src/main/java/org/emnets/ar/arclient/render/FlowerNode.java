package org.emnets.ar.arclient.render;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import org.emnets.ar.arclient.R;

public class FlowerNode extends Node implements Expandable{
    ViewRenderable renderable;
    ViewRenderable label;
    public TextView water,water_distance;
    public TextView humi;
    public TextView button;


    public FlowerNode(NodeParent parent, Context context) {
        this.setParent(parent);
        this.setEnabled(true);
        setupView(context);
        setupLabel(context);
    }

    private void buttonOnClick(View view){
        water.setText("6月1日 19:18");
        water_distance.setText("0小时");
        humi.setText("78%");
    }

    private void lableOnClick(View view) {
        this.setRenderable(renderable);
        button.setOnClickListener(this::buttonOnClick);
    }
    private void setupLabel(Context context) {
        ViewRenderable.builder()
                .setView(context, R.layout.card_view_green)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.setRenderable(renderable);
                            this.label = renderable;
                            TextView textView = (TextView) renderable.getView();
                            textView.setText("花盆");
                            textView.setOnClickListener(this::lableOnClick);
                            renderable.setSizer(view -> new Vector3(0.15f, 0.1f, 1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });
    }
    @Override
    public void unExpand() {
        this.setRenderable(label);
    }
    private void setupView(Context context){
        ViewRenderable.builder()
                .setView(context, R.layout.flower_view)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.renderable = renderable;
                            View flower = (View)renderable.getView();
                            water = flower.findViewById(R.id.water_time);
                            water_distance = flower.findViewById(R.id.water_distance);
                            humi = flower.findViewById(R.id.humi_lable);
                            button = flower.findViewById(R.id.watering_button);
                            renderable.setSizer(view -> new Vector3(0.7f,0.4f,1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });

    }
}
