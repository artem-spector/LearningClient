package com.artem.drawing;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * created by artem on 1/30/16.
 */
public class DrawingRawData {

    private List<MotionData> motions = new ArrayList<>();

    public synchronized void add(MotionData[] newMotions) {
        motions.addAll(Arrays.asList(newMotions));
        Log.d("RawData", "Added " + newMotions.length + "; total length " + motions.size());
    }

    public synchronized MotionData[] getMotions(int startIdx) {
        int len = motions.size() - startIdx;
        if (len == 0) return new MotionData[0];

        MotionData[] res = new MotionData[len];
        for (int i = 0; i < res.length; i++)
            res[i] = motions.get(startIdx + i);
        Log.d("RawData", "Got from " + startIdx + " length " + len);
        return res;
    }

    public synchronized MotionData getLastMotion() {
        return motions.isEmpty() ? null : motions.get(motions.size() - 1);
    }

    public void clear() {
        motions = new ArrayList<>();
    }

    public boolean isEmpty() {
        return motions.isEmpty();
    }
}
