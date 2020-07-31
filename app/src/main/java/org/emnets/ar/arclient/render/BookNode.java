package org.emnets.ar.arclient.render;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import org.emnets.ar.arclient.ARActivity;
import org.emnets.ar.arclient.R;

public class BookNode extends Node implements Expandable{
    ViewRenderable renderable;
    ViewRenderable label;
    public TextView page;
    public TextView button;
    public LinearLayout pageUpdateLayout;
    public EditText pageUpdateNum;
    public Button pageUpdateLayoutButton;


    public BookNode(NodeParent parent, Context context) {
        this.setParent(parent);
        this.setEnabled(true);
        setupView(context);
        setupLabel(context);
        pageUpdateLayout = ((ARActivity)context).pageUpdateLayout;
        pageUpdateNum = ((ARActivity)context).pageUpdateNum;
        pageUpdateLayoutButton = ((ARActivity)context).pageUpdateLayoutButton;
    }

    private void buttonOnClick(View view){
        pageUpdateLayout.setVisibility(View.VISIBLE);
        pageUpdateLayoutButton.setOnClickListener(this::pageUpdateLayoutButtonOnClick);
    }

    private void pageUpdateLayoutButtonOnClick(View view){
        page.setText(String.format("%s页", pageUpdateNum.getText().toString()));
        pageUpdateLayout.setVisibility(View.INVISIBLE);
    }

    private void lableOnClick(View view) {
        this.setRenderable(renderable);
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
                            textView.setText("书本");
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
                .setView(context, R.layout.book_view)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.renderable = renderable;
                            View flower = (View)renderable.getView();
                            page = flower.findViewById(R.id.page_label);
                            button = flower.findViewById(R.id.page_update_button);
                            button.setOnClickListener(this::buttonOnClick);
                            renderable.setSizer(view -> new Vector3(0.35f,0.2f,1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });

    }
}
