package org.emnets.ar.arclient.helpers;

import android.util.Log;

public class CircleQueue {
    static final String TAG = "CircleQueue";
    private final Object[] queue;
    public int size = 0;
    public int front;
    static final boolean verbose = false;

    public CircleQueue(int length) {
        queue = new Object[length];
        if(verbose)Log.v(TAG, "Queue inited, length = " + queue.length);
    }

    public void push(Object obj) {
        synchronized (queue) {
            queue[front++] = obj;
            front = front % queue.length;
            if (size < queue.length) {
                size++;
            }
            if(verbose)Log.v(TAG, "obj added,size=" + size);
        }
    }

    public Object[] getSortedQueue() {

        Object[] res = new Object[queue.length];
        if(verbose)Log.v(TAG, "objs get,size=" + size);
        synchronized (queue) {
            for (int i = 0; i < queue.length; i++) {
                res[i] = queue[(front + i) % queue.length];
                if(verbose)Log.v(TAG, "res[" + i + "] = queue[" + (front + i) % queue.length + "]: " + res[i].toString());
            }
        }
        if(verbose)Log.v(TAG, "return");
        return res;
    }

    public Object getLatest() {

        if (0 == size) {
            return null;
        }
        Object res;
        synchronized (queue) {
            if(verbose)Log.v(TAG, "front=" + front);
            int p = front - 1;
            if(verbose)Log.v(TAG, "p=" + p);
            if (p == -1) {
                p = queue.length - 1;
                if(verbose)Log.v(TAG, "p=" + p);
            }
            res = queue[p];
        }
        return res;
    }

    public boolean full() {
        return size == queue.length;
    }


}
