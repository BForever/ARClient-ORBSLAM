package org.emnets.ar.arclient;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.SystemClock;

import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.HitTestResult;
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
import org.emnets.ar.arclient.helpers.WebUtils;
import org.emnets.ar.arclient.network.GrpcSurfaceUploader;
import org.emnets.ar.arclient.network.TargetInfo;
import org.emnets.ar.arclient.network.TargetServer;
import org.emnets.ar.arclient.render.AirNode;
import org.emnets.ar.arclient.render.BookNode;
import org.emnets.ar.arclient.render.CameraPose;
import org.emnets.ar.arclient.render.DoorNode;
import org.emnets.ar.arclient.render.Expandable;
import org.emnets.ar.arclient.render.FlowerNode;
import org.emnets.ar.arclient.render.FrameDelay;
import org.emnets.ar.arclient.render.LabelNode;
import org.emnets.ar.arclient.render.ProjectNode;
import org.emnets.ar.arclient.render.VolNode;
import org.emnets.ar.arclient.render.WaterNode;
import org.emnets.ar.arclient.render.WebFileChoseListener;
import org.emnets.ar.arclient.render.WebNode;

import java.util.ArrayList;
import java.util.List;

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
public class ARActivity extends AppCompatActivity implements WebFileChoseListener {
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
    public GestureDetector gestureDetector;
    private static final float VIDEO_HEIGHT_METERS = 20f;

    public LinearLayout pageUpdateLayout;
    public EditText pageUpdateNum;
    public Button pageUpdateLayoutButton;
    // Shader
    private Scene scene;
    Camera camera;
    CameraPose cameraPose;
    GetDistanceOf2linesIn3D getDistanceOf2linesIn3D = new GetDistanceOf2linesIn3D();
    Node previewPoint;
    FrameUpdater frameUpdater;
    FrameDelay frameDelay;
    private List<Expandable> expandableList;

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
    public ARConnectionServiceGrpc.ARConnectionServiceStub stub;
    public ARConnectionServiceGrpc.ARConnectionServiceBlockingStub bstub;
    GrpcSurfaceUploader grpcSurfaceUploader;
    public Request request;
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

        expandableList = new ArrayList<Expandable>();
        pageUpdateLayout = findViewById(R.id.page_update_layout);
        pageUpdateNum = findViewById(R.id.page_update_num);
        pageUpdateLayoutButton = findViewById(R.id.page_update_layout_button);
        gestureDetector =
                new GestureDetector(
                        this,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onDoubleTap(MotionEvent e) {
                                taggleInputLayout(e);
                                return true;
                            }

                            @Override
                            public boolean onSingleTapConfirmed(MotionEvent e) {
                                for(Expandable expandable : expandableList){
                                    expandable.unExpand();
                                }
                                return true;
                            }

                            //                            @Override
//                            public boolean onDoubleTapEvent(MotionEvent e) {
//                                taggleInputLayout(e);
//                                return true;
//                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });
        // Shader init
        scene = sceneView.getScene();
        scene.setOnTouchListener((HitTestResult hitTestResult, MotionEvent event) -> {
            gestureDetector.onTouchEvent(event);
            return true;

        });
//        sceneView.setOnTouchListener(sceneOnTouchListener);
        camera = scene.getCamera();
        cameraPose = new CameraPose(camera);
        camera.setVerticalFovDegrees(52);
        camera.setFarClipPlane(60);
        frameUpdater = new FrameUpdater(cameraPose);
    }
    public void taggleInputLayout(MotionEvent event){
        if(inputLayout.getVisibility()==View.INVISIBLE) {
            inputLayout.setVisibility(View.VISIBLE);
        }else {
            inputLayout.setVisibility(View.INVISIBLE);
        }
        inputLayout.requestLayout();
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
        frameUpdater.running = false;
        super.onDestroy();
    }

    private void showInputButtonOnClick(View view) {
        if (inputLayout.getVisibility() == View.INVISIBLE) {
            inputLayout.setVisibility(View.VISIBLE);
            showInputButton.setText(R.string.hide_input_layout);
            inputLayout.requestLayout();
        } else {
            inputLayout.setVisibility(View.INVISIBLE);
            showInputButton.setText(R.string.show_input_layout);
            inputLayout.requestLayout();
        }
    }

    private void resetButtonOnClick(View view) {
        stub.requestReset(request, new StreamObserver<Response>() {
            @Override
            public void onNext(Response value) { }
            @Override
            public void onError(Throwable t) { }

            @Override
            public void onCompleted() {
                Log.v(TAG, "reset requeset sent.");
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
            bstub = ARConnectionServiceGrpc.newBlockingStub(channel);
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

        frameDelay = new FrameDelay();
        frameDelay.setOutputSurface(preview);
        Surface delayedSurface = frameDelay.getInputSurface();


        Camera2Manager camera2Manager = new Camera2Manager(this);
        camera2Manager.getCameraFpsBack();
//        camera2Manager.prepareCamera(preview, grpcSurfaceUploader.getSurface());
        camera2Manager.prepareCamera(delayedSurface, grpcSurfaceUploader.getSurface());
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
            Quaternion lookRotation = GeoHelper.getLookRotationFromDirection(getDistanceOf2linesIn3D.getRayB().getDirection(), cameraPose.getUpVector());
            switch (text) {
                case "door":
                    previewPoint = new DoorNode(scene, this);
                    expandableList.add((DoorNode)previewPoint);
                    break;
                case "air":
                    previewPoint = new AirNode(scene, this);
                    expandableList.add((AirNode)previewPoint);
                    break;
                case "vol":
                    previewPoint = new VolNode(scene, this);
                    expandableList.add((VolNode)previewPoint);
                    break;
                case "project":
                    previewPoint = new ProjectNode(scene, this);
                    expandableList.add((ProjectNode)previewPoint);
                    break;
                case "flower":
                    previewPoint = new FlowerNode(scene, this);
                    expandableList.add((FlowerNode)previewPoint);
                    break;
                case "printer":
                    previewPoint = new WebNode(scene, this,"http://10.214.149.2:8080");
                    break;
                case "water":
                    previewPoint = new WaterNode(scene, this);
                    expandableList.add((WaterNode)previewPoint);
                    break;
                case "book":
                    previewPoint = new BookNode(scene, this);
                    expandableList.add((BookNode)previewPoint);
                    break;
                default:
                    previewPoint = new WebNode(scene, this,"http://192.168.31.232:8080/#/render?obj="+text,text);
                    expandableList.add((WebNode)previewPoint);
//                    previewPoint = new LabelNode(scene, this, text);
                    break;
            }
            previewPoint.setWorldPosition(point);
            previewPoint.setWorldRotation(lookRotation);
            inputButton.setText(R.string.input_button_first);

            // Upload
            TargetInfo info = new TargetInfo(previewPoint.getLocalPosition(),previewPoint.getLocalRotation(), text);
            TargetServer targetServer = new TargetServer(this, true);
            targetServer.execute(info);

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

    private ValueCallback valueCallback;
    @Override
    public void getFile(ValueCallback valueCallback) {
        this.valueCallback = valueCallback;
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Downloads.EXTERNAL_CONTENT_URI);
        intent.setDataAndType(MediaStore.Downloads.EXTERNAL_CONTENT_URI, "*/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                WebUtils.seleteH5File(data, valueCallback);
            }else {
                valueCallback.onReceiveValue(null);
                valueCallback = null;
            }
        }
    }

    public void prePlaceLabels() {
        if (this.targetInfos == null) {
            return;
        }
        for (TargetInfo info : this.targetInfos) {
            Log.d(TAG, "placing item: " + info.name + " " + info.pose[0] + " " + info.pose[1] + " " + info.pose[2]);
            Node node;
            switch (info.name) {
                case "door":
                    node = new DoorNode(scene, this);
                    expandableList.add((DoorNode)node);
                    break;
                case "air":
                    node = new AirNode(scene, this);
                    expandableList.add((AirNode)node);
                    break;
                case "vol":
                    node = new VolNode(scene, this);
                    expandableList.add((VolNode)node);
                    break;
                case "project":
                    node = new ProjectNode(scene, this);
                    expandableList.add((ProjectNode)node);
                    break;
                case "flower":
                    node = new FlowerNode(scene, this);
                    expandableList.add((FlowerNode)node);
                    break;
                case "printer":
//                    node = new WebNode(scene, this,"http://192.168.31.232:8080/");
                    node = new WebNode(scene, this,"http://10.214.149.2:8080");
                    break;
                case "water":
                    node = new WaterNode(scene, this);
                    expandableList.add((WaterNode)node);
                    break;
                case "book":
                    node = new BookNode(scene, this);
                    expandableList.add((BookNode)node);
                    break;
                default:
                    node = new WebNode(scene, this,"http://192.168.31.232:8080/#/render?obj="+info.name,info.name);
                    expandableList.add((WebNode)node);
//                    node = new LabelNode(scene, this, info.name);
                    break;
            }
            Vector3 pos = new Vector3(info.pose[0],info.pose[1],info.pose[2]);
            node.setLocalPosition(pos);
            Log.e(TAG,"node "+info.name+" at "+pos);
            node.setLocalRotation(new Quaternion(info.pose[3],info.pose[4],info.pose[5],info.pose[6]));
        }
//        WebNode node = new WebNode(scene, this, "http://10.214.149.2:8080");
//        WebNode node = new WebNode(scene, this, "http://10.192.4.35:3000");
//        node.setWebFileChoseListener(this);
//        node.setWorldPosition(new Vector3(0f, 0f, 0f));
//        node.setCameraPose(cameraPose);


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
                SystemClock.sleep(3);
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
                    public void onError(Throwable t) {
                    }

                    @Override
                    public void onCompleted() {
                    }
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
