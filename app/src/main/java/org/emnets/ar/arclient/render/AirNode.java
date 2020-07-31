package org.emnets.ar.arclient.render;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import org.emnets.ar.arclient.R;

public class AirNode extends Node implements Expandable{
    ViewRenderable renderable;
    ViewRenderable label;
    public SeekBar seekBar;
    public TextView textView;
    SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            Log.v("AirNode","progress");
            textView.setText(String.format("%d℃", i));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };


    public AirNode(NodeParent parent, Context context) {
        this.setParent(parent);
        this.setEnabled(true);
        setupView(context);
        setupLabel(context);
    }
    private void lableOnClick(View view) {
        this.setRenderable(renderable);
       seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
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
                            textView.setText("中央空调");
                            textView.setOnClickListener(this::lableOnClick);
                            renderable.setSizer(view -> new Vector3(0.2f, 0.1f, 1f));
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
                .setView(context, R.layout.air_view)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.renderable = renderable;
                            renderable.setSizer(view -> new Vector3(0.6f,0.8f,1f));
                            View view = renderable.getView();
                            seekBar = view.findViewById(R.id.tempBar);
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });

    }
}
