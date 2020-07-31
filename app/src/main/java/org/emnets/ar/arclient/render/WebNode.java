package org.emnets.ar.arclient.render;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.google.ar.core.Pose;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.rendering.ViewSizer;

import org.emnets.ar.arclient.R;
import org.emnets.ar.arclient.helpers.GeoHelper;

class MyWebChromeClient extends WebChromeClient {
    private WebFileChoseListener webFileChoseListener;

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        webFileChoseListener.getFile(filePathCallback);
        return true;
    }

    public void setWebFileChoseListener(WebFileChoseListener webFileChoseListener) {
        this.webFileChoseListener = webFileChoseListener;
    }
}

public class WebNode extends Node implements Expandable{
    private String name = "";
    private CameraPose cameraPose = null;
    private MyWebChromeClient webChromeClient = new MyWebChromeClient();
    private ViewRenderable renderable;
    private ViewRenderable label;
    // create new node upon parent
    public WebNode(NodeParent parent, Context context, String url) {
        this.setParent(parent);
        this.setEnabled(true);

        setupWebView(context, url);
    }

    public WebNode(NodeParent parent, Context context, String url, String name) {
        this.setParent(parent);
        this.setEnabled(true);
        this.name = name;

        setupLabel(context);
        setupWebView(context, url);
    }

    @Override
    public void unExpand() {
        this.setRenderable(label);
    }
    private void lableOnClick(View view) {
        this.setRenderable(renderable);
    }
    // create new node in front of image
    public WebNode(NodeParent parent, Context context, Vector3 position, String url) {
        this(parent, context, url);
        this.setLocalPosition(new Vector3(position.x, position.y, position.z));
    }

    public WebNode(NodeParent parent, Context context, Vector3 position, Pose anchorPose, String url) {
        this(parent, context, url);
        float[] pos = new float[3];
        pos[0] = position.x;
        pos[1] = position.y;
        pos[2] = position.z;
        pos = anchorPose.rotateVector(pos);

        this.setLocalPosition(new Vector3(pos[0], pos[1], pos[2]));
        setupWebView(context, url);
    }

    // create new node in world position
    public WebNode(NodeParent parent, Context context, Pose pose, String url, CameraPose cameraPose) {
        this(parent, context, url);
        this.cameraPose = cameraPose;

        Vector3 pos = new Vector3(pose.tx(), pose.ty(), pose.tz());
        Quaternion rot = new Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw());
        this.setLocalPosition(pos);
        this.setLocalRotation(rot);
//        this.setLocalScale(Vector3.one().scaled(2));
    }

    public void setWebFileChoseListener(WebFileChoseListener webFileChoseListener) {
        this.webChromeClient.setWebFileChoseListener(webFileChoseListener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(Context context, String url) {
        ViewRenderable.builder()
                .setView(context, R.layout.web_view)
                .build()
                .thenAccept(
                        (renderable) -> {
                            this.setRenderable(renderable);
                            renderable.setSizer(new ViewSizer() {
                                @Override
                                public Vector3 getSize(View view) {
                                    return new Vector3(1f, 0.8f, 1f);
                                }
                            });
                            this.renderable = renderable;
                            WebView webView = (WebView) renderable.getView();
                            //支持缩放
                            WebSettings webSettings = webView.getSettings();
//                            webSettings.setUseWideViewPort(true);
//                            webSettings.setLoadWithOverviewMode(true);
                            webSettings.setJavaScriptEnabled(true);
                            webSettings.setDomStorageEnabled(true);
                            webSettings.setAppCacheEnabled(true);
                            webSettings.setAllowFileAccessFromFileURLs(true);
//                            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
//                            webView.setInitialScale(100);
                            webView.setWebViewClient(new WebViewClient() {
                                @Override
                                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//                                    view.loadUrl(request.getUrl().toString());
                                    return false;
                                }
                            });
                            webView.setWebChromeClient(webChromeClient);

                            webView.loadUrl(url);
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });

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
                            textView.setText(this.name);
                            textView.setOnClickListener(this::lableOnClick);
                            renderable.setSizer(view -> new Vector3(0.2f, 0.1f, 1f));
                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load web view.", throwable);
                        });
    }

    public void setCameraPose(CameraPose cameraPose) {
        this.cameraPose = cameraPose;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
        if (cameraPose != null) {
            Vector3 cameraPosition = cameraPose.getTranslation();
            Vector3 cardPosition = this.getWorldPosition();
            Vector3 direction = Vector3.subtract(cameraPosition, cardPosition);
            GeoHelper.getLookRotationFromDirection(direction, Vector3.up());
        }
    }
}
