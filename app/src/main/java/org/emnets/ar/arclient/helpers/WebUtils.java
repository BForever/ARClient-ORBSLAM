package org.emnets.ar.arclient.helpers;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;

public class WebUtils {
    public static void seleteH5File(Intent data, ValueCallback valueCallback){
        if (valueCallback == null){
            // todo valueCallback 为空的逻辑
            return;
        }
        try {
            Uri[] results = null;
            String dataString = data.getDataString();
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                results = new Uri[clipData.getItemCount()];
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    results[i] = item.getUri();
                }
            }

            if (dataString != null) {
                results = new Uri[]{Uri.parse(dataString)};
                valueCallback.onReceiveValue(results);
            }
        }catch (Exception e){
            e.printStackTrace();
            valueCallback.onReceiveValue(null);
        }
        valueCallback = null;
    }
}
