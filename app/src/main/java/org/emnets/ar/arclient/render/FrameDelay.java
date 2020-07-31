package org.emnets.ar.arclient.render;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageWriter;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;


public class FrameDelay implements ImageReader.OnImageAvailableListener,ImageWriter.OnImageReleasedListener{
    private static final String TAG="FrameDelay";
    private ImageReader imageReader;
    private ImageWriter imageWriter;
    private Handler readHandler, writeHandler;
    private int readerBufferCount = 0;
    private int writerBufferCount = 0;

    public FrameDelay(){
        imageReader = ImageReader.newInstance(640,480,ImageFormat.YUV_420_888,10);
        readHandler = new Handler();
        writeHandler = new Handler();
    }

    public Surface getInputSurface(){
        return imageReader.getSurface();
    }

    public void setOutputSurface(Surface outputSurface){
        imageWriter = ImageWriter.newInstance(outputSurface,10, ImageFormat.YUV_420_888);
        imageReader.setOnImageAvailableListener(this, readHandler);
        imageWriter.setOnImageReleasedListener(this, writeHandler);
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        readerBufferCount++;
        Log.d(TAG,"New frame available,now we have "+ readerBufferCount +" frames in buffer");
        if(readerBufferCount >=8){
            Image image = imageReader.acquireNextImage();
            if(writerBufferCount>3){
                Log.i(TAG,"drop frame");
                image.close();
            }else {
                imageWriter.queueInputImage(image);
                synchronized (this) {
                    writerBufferCount++;
                    Log.d(TAG, "Queue to writer, now " + writerBufferCount);
                }
            }
            readerBufferCount--;
        }
    }

    @Override
    public void onImageReleased(ImageWriter imageWriter) {
        synchronized (this) {
            writerBufferCount--;
            Log.d(TAG, "Released from writer, now " + writerBufferCount);
        }
    }
}
