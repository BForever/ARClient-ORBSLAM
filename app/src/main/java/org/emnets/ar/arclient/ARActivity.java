package org.emnets.ar.arclient;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.SystemClock;

import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;

import org.emnets.ar.arclient.helpers.FrameUpdater;
import org.emnets.ar.arclient.helpers.GeoHelper;
import org.emnets.ar.arclient.helpers.GetDistanceOf2linesIn3D;
import org.emnets.ar.arclient.network.GrpcSurfaceUploader;
import org.emnets.ar.arclient.network.TargetInfo;
import org.emnets.ar.arclient.network.TargetServer;
import org.emnets.ar.arclient.render.CameraPose;
import org.emnets.ar.arclient.render.LabelNode;
import org.emnets.ar.arclient.render.WebNode;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;


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
    private Button resetButton;
    private Button showInputButton;
    private LinearLayout inputLayout;
    private Button inputButton;
    private EditText editText;
    private static final float VIDEO_HEIGHT_METERS = 20f;
    // Shader
    private Scene scene;
    Camera camera;
    CameraPose cameraPose;
    GetDistanceOf2linesIn3D getDistanceOf2linesIn3D = new GetDistanceOf2linesIn3D();
    Node previewPoint;
    FrameUpdater frameUpdater;

    ModelRenderable mVideoRenderable;
    Node mVideoNode;
    // Labels
    public boolean hasPlacedLabels;
    public TargetInfo[] targetInfos;
    // Internet
    static final public String UI_SERVER_ADDRESS = "http://47.103.3.12";
    static final public String UI_SERVER_PORT = "8000";
    static final public String SLAM_SERVER_ADDRESS = "10.214.149.2";
    static final public int SLAM_SERVER_PORT = 50051;

    // RPC
    ManagedChannel channel;
    ARConnectionServiceGrpc.ARConnectionServiceStub stub;
    GrpcSurfaceUploader grpcSurfaceUploader;
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
        resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(this::resetButtonOnClick);
        startButton.setOnClickListener(this::startButtonOnClick);
        showInputButton = findViewById(R.id.show_input_button);
        showInputButton.setOnClickListener(this::showInputButtonOnClick);
        inputLayout = findViewById(R.id.input_layout);
        inputButton = findViewById(R.id.input_button);
        inputButton.setOnClickListener(this::inputButtonOnClick);
        editText = findViewById(R.id.editText);

        // Shader init
        scene = sceneView.getScene();
//        sceneView.setOnTouchListener(sceneOnTouchListener);
        camera = scene.getCamera();
        cameraPose = new CameraPose(camera);
        camera.setVerticalFovDegrees(52);
        camera.setFarClipPlane(60);
        frameUpdater = new FrameUpdater(cameraPose);
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
        grpcSurfaceUploader.stop();
        super.onDestroy();
    }

    private void showInputButtonOnClick(View view){
        if (inputLayout.getVisibility() == View.INVISIBLE) {
            inputLayout.setVisibility(View.VISIBLE);
            showInputButton.setText(R.string.hide_input_layout);
            inputLayout.requestLayout();
        }else {
            inputLayout.setVisibility(View.INVISIBLE);
            showInputButton.setText(R.string.show_input_layout);
            inputLayout.requestLayout();
        }
    }

    private void resetButtonOnClick(View view){
        stub.requestReset(request, new StreamObserver<Response>() {
            @Override
            public void onNext(Response value) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                Log.v(TAG,"reset requeset sent.");
            }
        });
    }

    private void startButtonOnClick(View view) {
        // Targets Server
        hasPlacedLabels = true;
        TargetServer targetServer = new TargetServer(this, false);
        TargetInfo[] targetInfos = new TargetInfo[1];
        targetInfos[0] = new TargetInfo();
        targetServer.execute(targetInfos);
        // SLAM Server
        if (fetchPose) {
            channel = ManagedChannelBuilder.forAddress(SLAM_SERVER_ADDRESS, SLAM_SERVER_PORT).usePlaintext().enableRetry().maxRetryAttempts(100).build();
            stub = ARConnectionServiceGrpc.newStub(channel);
            request = Request.newBuilder().setName("hello").build();
            Thread thread = new Thread(receiveMatrix);
            thread.start();
            grpcSurfaceUploader = new GrpcSurfaceUploader(stub);
        }

        mVideoNode = new Node();
        mVideoNode.setParent(camera);
        mVideoNode.setLocalPosition(new Vector3(0f, -10f, -20f));

        // Set the scale of the node so that the aspect ratio of the video is correct.
        float videoWidth = 640f;
        float videoHeight = 480f;
        mVideoNode.setLocalScale(
                new Vector3(
                        VIDEO_HEIGHT_METERS * (videoWidth / videoHeight), VIDEO_HEIGHT_METERS, 1.0f));

        ExternalTexture texture = createExternalTexture(mVideoNode);
        Surface preview = texture.getSurface();

        Camera2Manager camera2Manager = new Camera2Manager(this);
        camera2Manager.getCameraFpsBack();
        camera2Manager.prepareCamera(preview, grpcSurfaceUploader.getSurface());
        camera2Manager.openCamera();
        grpcSurfaceUploader.start();
        frameUpdater.start();

        startButton.setText(R.string.stop_button);
//        startLayout.setVisibility(View.INVISIBLE);
//        startLayout.requestLayout();
    }

    private void inputButtonOnClick(View view) {
        String text = editText.getText().toString();
        Log.e("text", text);

        getDistanceOf2linesIn3D.setRay(GeoHelper.cameraToRay(cameraPose));

        if (getDistanceOf2linesIn3D.twoLineSet()) {
            getDistanceOf2linesIn3D.compute();
            Vector3 point = getDistanceOf2linesIn3D.getPonB();
            Log.e(TAG, "point create at " + point.toString());
            Quaternion lookRotation = GeoHelper.getLookRotationFromDirection(getDistanceOf2linesIn3D.getRayB().getDirection().negated(),cameraPose.getUpVector());
            previewPoint = new LabelNode(scene, this, "target", point,lookRotation);
            inputButton.setText(R.string.input_button_first);
            showInputButton.setText(R.string.show_input_layout);
            inputLayout.setVisibility(View.INVISIBLE);
            inputLayout.requestLayout();
        } else {
            inputButton.setText(R.string.input_button_second);
            if (previewPoint != null) {
                scene.removeChild(previewPoint);
            }
        }

//        Node card = new WebNode(anchorNode, this, getOffsetPose(),SERVER_ADDRESS+":"+UI_SERVER_PORT+"/"+text+".html");
//        PoseInfo info = new PoseInfo(getOffsetPose(),text);
//
//        PoseServer poseServer = new PoseServer(this,true);
//        poseServer.execute(info);
    }

    public void prePlaceLabels() {
        if (this.targetInfos == null) {
            return;
        }
        for (TargetInfo info : this.targetInfos) {
            Log.d(TAG, "placing item: " + info.name + " " + info.pose[0] + " " + info.pose[1] + " " + info.pose[2]);
//            new WebNode(anchorNode,this, new Vector3(info.pose[0],info.pose[1],info.pose[2]),SERVER_ADDRESS+":8000/"+info.name+".html");
            new WebNode(scene, this, info.getPose(), UI_SERVER_ADDRESS + ":" + UI_SERVER_PORT + "/" + info.name + ".html", cameraPose);
        }
//        Node card = new LabelNode(anchorNode, this, Pose.makeTranslation(3.6f, 0.2f, 0.2f), "刘汶鑫");
//        Node web = new WebNode(scene, this, new Vector3(0, 0f, 0f), "范宏昌");
//        new LabelNode(camera,this,"-z",new Vector3(0f,0f,-2f));
//        new LabelNode(camera,this,"x",new Vector3(0.1f,0f,-1f));
//        new LabelNode(camera,this,"y",new Vector3(0f,0.1f,-1f));
    }

//    View.OnTouchListener sceneOnTouchListener = new View.OnTouchListener() {
//        @Override
//        public boolean onTouch(View view, MotionEvent motionEvent) {
////            Log.e(TAG,"onTouch");
//            if (inputLayout.getVisibility() == View.INVISIBLE) {
//                inputLayout.setVisibility(View.VISIBLE);
//                inputLayout.requestLayout();
//            }else {
//                inputLayout.setVisibility(View.INVISIBLE);
//                inputLayout.requestLayout();
//            }
//            return false;
//        }
//    };

    // View Matrix Transfer
    Runnable receiveMatrix = new Runnable() {
        @Override
        public void run() {
            while (!stopReceive) {
                StreamObserver<MatrixBlock> streamObserver = new StreamObserver<MatrixBlock>() {
                    @Override
                    public void onNext(MatrixBlock result) {
                        if (result.getUpdate()) {
                            cameraPose.setViewMatrix(result);
//                            camera.setWorldPosition(cameraPose.getTranslation());
//                            camera.setWorldRotation(cameraPose.getRotation());
//                            Log.e(TAG,"t: "+cameraPose.getTranslation());
                        } else {
//                            Log.v("grpc", "pose not updated");
                        }
                    }
                    @Override
                    public void onError(Throwable t) { }
                    @Override
                    public void onCompleted() { }
                };
                stub.getViewMatrix(request, streamObserver);
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
//                                                Log.e("OnFrameAvailable",String.valueOf(SystemClock.uptimeMillis()));
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
}
