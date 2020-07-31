package org.emnets.ar.arclient.render;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import org.emnets.ar.arclient.R;

public class ProjectNode extends Node implements Expandable{
    static final String TAG = "ProjectNode";
    //    private ProjectView projectView;
    private TextView textView;
    private Button button;
    private ViewRenderable renderable;
    private ViewRenderable label;

    public ProjectNode(NodeParent parent, Context context) {
        this.setParent(parent);
        this.setEnabled(true);
        setupView(context);
        setupLabel(context);
    }

    private void onClick(View view) {
        if (textView.getText().equals("开")) {
            textView.setText("关");
            button.setText("打开投影仪");
        } else {
            textView.setText("开");
            button.setText("关闭投影仪");
        }
    }
    @Override
    public void unExpand() {
        this.setRenderable(label);
    }
    private void lableOnClick(View view) {
        this.setRenderable(renderable);
        View project_view = (View) renderable.getView();
        textView = project_view.findViewById(R.id.cur_project);
        button = project_view.findViewById(R.id.project_button);
        button.setOnClickListener(this::onClick);
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
                            textView.setText("投影仪");
                            textView.setOnClickListener(this::lableOnClick);
                            renderable.setSizer(view -> new Vector3(0.2f, 0.1f, 1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });
    }

    private void setupView(Context context) {
        ViewRenderable.builder()
                .setView(context, R.layout.project_view)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.renderable = renderable;
                            renderable.setSizer(view -> new Vector3(0.6f, 0.3f, 1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });

    }
}
