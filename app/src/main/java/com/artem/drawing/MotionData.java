package com.artem.drawing;

import android.view.MotionEvent;

/**
 * created by artem on 1/23/16.
 */
public class MotionData {

    public enum MotionType {Up, Down, Move}

    public final long time;
    public final MotionType motion;
    public final float x;
    public final float y;

    public MotionData(long time, float x, float y, MotionType motion) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.motion = motion;
    }

    public String toString() {
        return "MotionData { motion=" + motion + " , x=" + x + ", y=" + y +" }";
    }

}
