package org.emnets.ar.arclient.network;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.google.gson.Gson;

import org.emnets.ar.arclient.ARActivity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TargetServer extends AsyncTask<TargetInfo, Void, TargetInfo[]> {
    private final String TAG = "PoseServer";
    private boolean upload = false;
    private long startTime;
    private final String SERVER_ADDRESS;
    private ARActivity activity;


    public TargetServer(ARActivity activity, boolean upload) {
        this.activity = activity;
        this.upload = upload;
        SERVER_ADDRESS = activity.UI_SERVER_ADDRESS + ":5001/pose";
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.i(TAG, "uploading start");
        startTime = SystemClock.uptimeMillis();
    }

    @Override
    protected TargetInfo[] doInBackground(TargetInfo... infos) {
        if (upload) {
            return post(infos[0], SERVER_ADDRESS);
        } else {
            return get(infos[0], SERVER_ADDRESS);
        }

    }

    @Override
    protected void onPostExecute(TargetInfo[] targetInfos) {
        super.onPostExecute(targetInfos);
        if (!upload) {
            activity.targetInfos = targetInfos;
            activity.hasPlacedLabels = false;
            activity.prePlaceLabels();
        }
        Log.i(TAG, "uploading done, elapsed: " + String.valueOf(SystemClock.uptimeMillis() - startTime) + "ms");
    }

    private TargetInfo[] get(TargetInfo infos, String serverAddr) {
        try {
            URL url = new URL(serverAddr);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setDoInput(true);
            c.setRequestMethod("GET");
            c.connect();

            Log.i(TAG, "connected, elapsed: " + String.valueOf(SystemClock.uptimeMillis() - startTime) + "ms");
            TargetInfo[] targetInfos;
            Gson res = new Gson();
            if (c.getResponseCode() < 400) {
                InputStreamReader inputStreamReader = new InputStreamReader(c.getInputStream());

                targetInfos = res.fromJson(inputStreamReader, TargetInfo[].class);
                Log.i(TAG, "response fetched, elapsed: " + String.valueOf(SystemClock.uptimeMillis() - startTime) + "ms");
                Log.i(TAG, "Successfully get pose: " + targetInfos.length + " items");
            } else {
                InputStreamReader inputStreamReader = new InputStreamReader(c.getInputStream());
                TargetInfo targetInfo;
                targetInfo = res.fromJson(inputStreamReader, TargetInfo.class);

                targetInfos = new TargetInfo[1];
                targetInfos[0] = targetInfo;

                Log.e(TAG, "Error posting pose: " + targetInfo.response);
            }
            return targetInfos;
        } catch (IOException e) {
            Log.e(TAG, "Error in POST", e);
            return null;
        }
    }

    private TargetInfo[] post(TargetInfo info, String serverAddr) {
        try {
            URL url = new URL(serverAddr);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setDoInput(true);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.connect();

            Log.i(TAG, "connected, elapsed: " + String.valueOf(SystemClock.uptimeMillis() - startTime) + "ms");
            OutputStream output = c.getOutputStream();

            Gson gson = new Gson();
            output.write(gson.toJson(info, TargetInfo.class).getBytes());
            output.close();

            Log.i(TAG, "json transferred, elapsed: " + String.valueOf(SystemClock.uptimeMillis() - startTime) + "ms");

            TargetInfo[] targetInfos = new TargetInfo[1];
            if (c.getResponseCode() < 400) {
                InputStreamReader inputStreamReader = new InputStreamReader(c.getInputStream());
                targetInfos[0] = gson.fromJson(inputStreamReader, TargetInfo.class);
                Log.i(TAG, "response fetched, elapsed: " + String.valueOf(SystemClock.uptimeMillis() - startTime) + "ms");
                Log.i(TAG, "Successfully posted pose: " + targetInfos[0].response);
            } else {
                InputStreamReader inputStreamReader = new InputStreamReader(c.getInputStream());
                targetInfos[0] = gson.fromJson(inputStreamReader, TargetInfo.class);

                Log.e(TAG, "Error posting pose: " + targetInfos[0].response);
            }
            return targetInfos;
        } catch (IOException e) {
            Log.e(TAG, "Error in POST", e);
            return null;
        }
    }
}
