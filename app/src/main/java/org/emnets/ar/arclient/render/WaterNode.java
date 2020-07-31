package org.emnets.ar.arclient.render;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import org.emnets.ar.arclient.R;

public class WaterNode extends Node implements Expandable{
    ViewRenderable renderable1;
    ViewRenderable renderable2;
    ViewRenderable label;
    public TextView bookingButton;
    public TextView buyButton;


    public WaterNode(NodeParent parent, Context context) {
        this.setParent(parent);
        this.setEnabled(true);
        setupView(context);
        setupLabel(context);
    }

    private void bookingButtonOnClick(View view){
        this.setRenderable(renderable2);
    }

    private void buyButtonOnClick(View view){
        ((TextView)view).setText("预定成功");
    }

    private void lableOnClick(View view) {
        this.setRenderable(renderable1);
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
                            textView.setText("饮水机E301");
                            textView.setOnClickListener(this::lableOnClick);
                            renderable.setSizer(view -> new Vector3(0.25f, 0.1f, 1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });
    }
    @Override
    public void unExpand() {
        if(this.getRenderable()==renderable2){
            this.setRenderable(renderable1);
        }else {
            this.setRenderable(label);
        }
    }
    private void setupView(Context context){
        ViewRenderable.builder()
                .setView(context, R.layout.water_view)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.renderable1 = renderable;
                            View water = (View)renderable.getView();
                            bookingButton = water.findViewById(R.id.water_booking_button);
                            bookingButton.setOnClickListener(this::bookingButtonOnClick);
                            renderable.setSizer(view -> new Vector3(0.4f,0.3f,1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });
        ViewRenderable.builder()
                .setView(context, R.layout.water_booking)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.renderable2 = renderable;
                            View water = (View)renderable.getView();
                            buyButton = water.findViewById(R.id.water_booking_buy_button);
                            buyButton.setOnClickListener(this::buyButtonOnClick);
                            renderable.setSizer(view -> new Vector3(0.3f,0.2f,1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });
    }
}
