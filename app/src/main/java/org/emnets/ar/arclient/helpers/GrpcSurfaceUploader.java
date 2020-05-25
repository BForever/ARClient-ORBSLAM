package org.emnets.ar.arclient.helpers;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.google.protobuf.ByteString;

import org.emnets.ar.arclient.ARConnectionServiceGrpc;
import org.emnets.ar.arclient.ImageBlock;
import org.emnets.ar.arclient.Response;

import java.nio.ByteBuffer;

import io.grpc.stub.StreamObserver;


public class GrpcSurfaceUploader implements ImageReader.OnImageAvailableListener {
    final private String TAG = "GrpcSurfaceUploader";
    private ImageReader imageReader;
    private Surface surface;
    private ARConnectionServiceGrpc.ARConnectionServiceStub stub;
    StreamObserver<Response> responseStreamObserver;
    StreamObserver<ImageBlock> imageBlockStreamObserver;

    public GrpcSurfaceUploader(ARConnectionServiceGrpc.ARConnectionServiceStub stub){
        imageReader = ImageReader.newInstance(640,480, ImageFormat.YUV_420_888,2);
        surface = imageReader.getSurface();
        this.stub = stub;

        responseStreamObserver = new StreamObserver<Response>() {
            @Override
            public void onNext(Response value) {
                Log.i("GrpcSurfaceUploader","response status: "+value.getStatus());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        };
    }

    public void onImageAvailable(ImageReader imageReader){
//        Log.e("GrpcSurfaceUploader","onImageAvailable");
        long start = SystemClock.uptimeMillis();
        Image image = imageReader.acquireNextImage();
//        byte[] imageBytes = ImageHelper.YUV_420_888toNV21(image);
        byte[] imageBytes = ImageHelper.ImagetoJpegBytes(image,50);
//        Log.e(TAG,"encode time: "+ (SystemClock.uptimeMillis() - start));
        image.close();
        ImageBlock imageBlock = ImageBlock.newBuilder().setImage(ByteString.copyFrom(imageBytes)).setSize(imageBytes.length).build();
        start = SystemClock.uptimeMillis();
        imageBlockStreamObserver.onNext(imageBlock);
//        Log.e(TAG,"copy and send time: "+ (SystemClock.uptimeMillis() - start));

    }

    public Surface getSurface() {
        return surface;
    }

    public void start(){
        imageBlockStreamObserver = stub.uploadImage(responseStreamObserver);
        imageReader.setOnImageAvailableListener(this,null);
    }

    public void stop(){
        imageReader.setOnImageAvailableListener(null,null);
        imageBlockStreamObserver.onCompleted();
    }
}
