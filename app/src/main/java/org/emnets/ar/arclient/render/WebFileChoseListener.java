package org.emnets.ar.arclient.render;

import android.content.Intent;
import android.provider.MediaStore;
import android.webkit.ValueCallback;

public interface WebFileChoseListener {
    void getFile(ValueCallback valueCallback);
}
