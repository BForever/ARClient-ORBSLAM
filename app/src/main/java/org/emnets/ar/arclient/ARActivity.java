/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.emnets.ar.arclient;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;
import com.pedro.rtspserver.RtspServerCamera2;

import org.emnets.ar.arclient.helpers.RtspConnectionChecker;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 *
 * <p>In this example, we assume all images are static or moving slowly with a large occupation of
 * the screen. If the target is actively moving, we recommend to check
 * ArAugmentedImage_getTrackingMethod() and render only when the tracking method equals to
 * AR_AUGMENTED_IMAGE_TRACKING_METHOD_FULL_TRACKING. See details in <a
 * href="https://developers.google.com/ar/develop/c/augmented-images/">Recognize and Augment
 * Images</a>.
 */
public class ARActivity extends AppCompatActivity {
    private String TAG = "ARActivity";
    // UI
    private SceneView sceneView;
    private LinearLayout startLayout;
    private Button startButton;
    private LinearLayout inputLayout;
    private Button inputButton;
    private EditText editText;
    private static final float VIDEO_HEIGHT_METERS = 40f;
    // Shader
    private Scene scene;
    Camera camera;
    ModelRenderable mVideoRenderable;
    Node mVideoNode;
    // Labels
    public boolean hasPlacedLabels;
    public PoseInfo[] poseInfos;
    // Internet
    static final public String SERVER_ADDRESS = "http://47.103.3.12";
    static final public String UI_SERVER_PORT = "8000";

    // RPC
    ManagedChannel channel;
    ViewTransGrpc.ViewTransBlockingStub stub;
    Request request;
    boolean stopReceive = false;
    // Debug
    private boolean fetchPose = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Window setting
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // UI
        sceneView = findViewById(R.id.sceneview);
        startLayout = findViewById(R.id.start_layout);
        startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(this::startButtonOnClick);
        inputLayout = findViewById(R.id.input_layout);
        inputButton = findViewById(R.id.input_button);
        inputButton.setOnClickListener(this::inputButtonOnClick);
        editText = findViewById(R.id.editText);

        // Shader init
        scene = sceneView.getScene();
        camera = scene.getCamera();
        camera.setWorldPosition(new Vector3(-3.3f, 0.3f, 0.5f));
        camera.setLocalRotation(Quaternion.axisAngle(Vector3.up(), -15.0f));

        // Targets Server
        hasPlacedLabels = true;
        PoseServer poseServer = new PoseServer(this, false);
        PoseInfo[] poseInfos = new PoseInfo[1];
        poseInfos[0] = new PoseInfo();
        poseServer.execute(poseInfos);
        // Pose Server
        if (fetchPose) {
            channel = ManagedChannelBuilder.forAddress("192.168.1.100", 50051).usePlaintext().build();
            stub = ViewTransGrpc.newBlockingStub(channel);
            request = Request.newBuilder().setName("hello").build();
            Thread thread = new Thread(receiveMatrix);
            thread.start();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            sceneView.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void startButtonOnClick(View view) {
        mVideoNode = new Node();
        mVideoNode.setParent(camera);
        mVideoNode.setLocalPosition(new Vector3(0f, -20f, -20f));

        // Set the scale of the node so that the aspect ratio of the video is correct.
        float videoWidth = 640f;
        float videoHeight = 480f;
        mVideoNode.setLocalScale(
                new Vector3(
                        VIDEO_HEIGHT_METERS * (videoWidth / videoHeight), VIDEO_HEIGHT_METERS, 1.0f));

        ExternalTexture texture = createExternalTexture(mVideoNode);


        Surface surface = texture.getSurface();
        createCameraRtspServer(surface);

        startLayout.setVisibility(View.INVISIBLE);
        startLayout.requestLayout();
    }

    private void inputButtonOnClick(View view) {
        String text = editText.getText().toString();
        Log.e("text", text);

//        Node card = new WebNode(anchorNode, this, getOffsetPose(),SERVER_ADDRESS+":"+UI_SERVER_PORT+"/"+text+".html");
//        PoseInfo info = new PoseInfo(getOffsetPose(),text);
//
//        PoseServer poseServer = new PoseServer(this,true);
//        poseServer.execute(info);

        inputLayout.setVisibility(View.INVISIBLE);
        inputLayout.requestLayout();
    }

    public void prePlaceLabels() {
        if (this.poseInfos == null) {
            return;
        }
        for (PoseInfo info : this.poseInfos) {
            Log.d(TAG, "placing item: " + info.name + info.pose[0] + info.pose[1] + info.pose[2]);
//            new WebNode(anchorNode,this, new Vector3(info.pose[0],info.pose[1],info.pose[2]),SERVER_ADDRESS+":8000/"+info.name+".html");
            new WebNode(scene, this, info.getPose(), SERVER_ADDRESS + ":" + UI_SERVER_PORT + "/" + info.name + ".html");
        }
//        Node card = new LabelNode(anchorNode, this, Pose.makeTranslation(3.6f, 0.2f, 0.2f), "刘汶鑫");
//        Node web = new WebNode(scene, this, new Vector3(0, 0f, -3f), "范宏昌");
//        Node label = new LabelNode(scene,this,"fan");
    }

    // View Matrix Transfer
    Runnable receiveMatrix = new Runnable() {
        @Override
        public void run() {
            while (!stopReceive) {
                Matrix result = stub.getViewMatrix(request);
                if (result.getUpdate()) {
//                    float[] temp = {(float) result.getR0C0(), (float) result.getR1C0(), (float) result.getR2C0(), (float) result.getR3C0(),
//                            (float) result.getR0C1(), (float) result.getR1C1(), (float) result.getR2C1(), (float) result.getR3C1(),
//                            (float) result.getR0C2(), (float) result.getR1C2(), (float) result.getR2C2(), (float) result.getR3C2(),
//                            (float) result.getR0C3(), (float) result.getR1C3(), (float) result.getR2C3(), (float) result.getR3C3()};
                    float[] temp = {(float) result.getR0C0(), (float) result.getR0C1(), (float) result.getR0C2(), (float) result.getR0C3(),
                            (float) result.getR1C0(), (float) result.getR1C1(), (float) result.getR1C2(), (float) result.getR1C3(),
                            (float) result.getR2C0(), (float) result.getR2C1(), (float) result.getR2C2(), (float) result.getR2C3(),
                            (float) result.getR3C0(), (float) result.getR3C1(), (float) result.getR3C2(), (float) result.getR3C3()};
                    com.google.ar.sceneform.math.Matrix viewmatrix = new com.google.ar.sceneform.math.Matrix(temp);
                    com.google.ar.sceneform.math.Matrix worldmodematrix = new com.google.ar.sceneform.math.Matrix();
//                    com.google.ar.sceneform.math.Matrix worldmodematrix = viewmatrix;
                    com.google.ar.sceneform.math.Matrix.invert(viewmatrix, worldmodematrix);
                    Vector3 translation = new Vector3();

                    worldmodematrix.decomposeTranslation(translation);
                    Quaternion rotation = new Quaternion();
                    worldmodematrix.extractQuaternion(rotation);
                    camera.setWorldPosition(translation);
                    camera.setWorldRotation(rotation);
//                    camera.setLocalRotation(Quaternion.axisAngle(Vector3.forward(),-90));
                    Log.e("grpc", "sucessfully get pose:" + temp[0] + " " + temp[1] + " " + temp[2] + " " + temp[3] + " " + temp[4]);
                } else {
                    Log.e("grpc", "pose not updated");
                }
            }
        }
    };

    private ExternalTexture createExternalTexture(Node videoNode) {
        ExternalTexture texture = new ExternalTexture();
        texture.getSurfaceTexture().setDefaultBufferSize(640, 480);
        ModelRenderable.builder()
                .setSource(this, R.raw.chroma_key_video)
                .build()
                .thenAccept(
                        renderable -> {
                            mVideoRenderable = renderable;
                            renderable.getMaterial().setExternalTexture("videoTexture", texture);
                            texture
                                    .getSurfaceTexture()
                                    .setOnFrameAvailableListener(
                                            (SurfaceTexture surfaceTexture1) -> {
                                                videoNode.setRenderable(mVideoRenderable);
                                                surfaceTexture1.setOnFrameAvailableListener(null);
                                            });
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load video renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
        return texture;
    }

    void onFps(int fps) {
        Log.e("fps", Integer.toString(fps));
    }

    private void createCameraRtspServer(Surface surface) {
        try {
            ConnectCheckerRtsp connectCheckerRtsp = new RtspConnectionChecker();
            RtspServerCamera2 rtspCamera = new RtspServerCamera2(this, surface, connectCheckerRtsp, 8086);
            //start stream
            rtspCamera.setFpsListener(this::onFps);
            if (rtspCamera.prepareAudio() && rtspCamera.prepareVideo()) {
                rtspCamera.startStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
