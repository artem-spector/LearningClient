package com.artem.learningclient;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.artem.drawing.DrawingRawData;
import com.artem.drawing.MotionData;


/**
 * created by artem on 1/23/16.
 */
public class DrawingProcessor extends Thread {

    private static final int STROKE_WIDTH = 4;
    private static final int HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
    private static final long DRAW_INTERVAL_MILLIS = 50;

    private SurfaceHolder holder;

    private Paint paint = new Paint();

    private final DrawingRawData data;
    private int nextMotionIdx = 0;

    private Long lastDraw;
    private RectF dirtyRect;

    private Path path = new Path();

    private long lastTime;


    public DrawingProcessor(DrawingRawData data) {
        this.data = data;
        this.lastTime = System.currentTimeMillis();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        while (true) {
            try {
                Thread.sleep(DRAW_INTERVAL_MILLIS);
                drawIfNecessary();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public void setSurface(final SurfaceHolder holder) {
        initSurface(holder);
    }

    public void clearSurface() {
        Log.d("DrawingProcessor", "clearSurface");
        holder = null;
    }

    public boolean onTouch(MotionEvent event) {
        boolean processed;
        long start = System.currentTimeMillis();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                MotionData lastMotion = data.getLastMotion();
                boolean samePlace = lastMotion != null
                        && event.getAction() == MotionEvent.ACTION_MOVE
                        && Math.abs(event.getX() - lastMotion.x) < 1
                        && Math.abs(event.getY() - lastMotion.y) < 1;
                if (!samePlace) {
                    MotionData[] newMotions = toMotionData(event);
                    data.add(newMotions);
                    Log.d("DrawingProcessor", "processing: " + elapsedTime() + "; duration=" + (System.currentTimeMillis() - start) + "; num events=" + newMotions.length + "; occured before " + (SystemClock.uptimeMillis() - event.getEventTime()));
                }
                processed = true;
                break;
            default:
                processed = false;
        }

        return processed;
    }

    public void clear() {
        path.reset();
        data.clear();
        nextMotionIdx = 0;
        lastDraw = null;
    }

    private MotionData[] toMotionData(MotionEvent event) {
        int historySize = event.getHistorySize();
        MotionData[] res = new MotionData[historySize + 1];
        for (int i = 0; i < historySize; i++) {
            res[i] = toMotionData(event.getHistoricalEventTime(i), event.getHistoricalX(i), event.getHistoricalY(i), event.getAction());
        }
        res[historySize] = toMotionData(event.getEventTime(), event.getX(), event.getY(), event.getAction());

        return res;
    }

    private MotionData toMotionData(long time, float x, float y, int action) {
        MotionData.MotionType motion;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                motion = MotionData.MotionType.Down;
                break;
            case MotionEvent.ACTION_MOVE:
                motion = MotionData.MotionType.Move;
                break;
            case MotionEvent.ACTION_UP:
                motion = MotionData.MotionType.Up;
                break;
            default:
                String msg = "unsupported action: " + action;
                Log.e("DrawingSurfaceView", msg);
                throw new RuntimeException(msg);
        }
        return new MotionData(time, x, y, motion);
    }

    private void drawIfNecessary() {
        long now = System.currentTimeMillis();

        // if the data jast was cleared, redraw the whole area
        if (data.isEmpty() && lastDraw == null) {
            draw(new RectF(holder.getSurfaceFrame()));
            lastDraw = now;
            return;
        }

        // don't start drawing right away
        if (lastDraw == null)
            lastDraw = now;

        MotionData[] newMotions = data.getMotions(nextMotionIdx);
        if (newMotions.length == 0) return;

        if (dirtyRect == null)
            dirtyRect = new RectF(newMotions[0].x, newMotions[0].y, newMotions[0].x, newMotions[0].y);
        updateDirtyRectAndBitmap(dirtyRect, newMotions);

        MotionData lastMotion = newMotions[newMotions.length - 1];
        draw(dirtyRect);
        nextMotionIdx += newMotions.length;
        lastDraw = now;
        dirtyRect = new RectF(lastMotion.x, lastMotion.y, lastMotion.x, lastMotion.y);
//        }
    }

    private void updateDirtyRectAndBitmap(RectF dirtyRect, MotionData[] newMotions) {
        for (MotionData event : newMotions) {
            dirtyRect.set(
                    Math.min(dirtyRect.left, event.x),
                    Math.min(dirtyRect.top, event.y),
                    Math.max(dirtyRect.right, event.x),
                    Math.max(dirtyRect.bottom, event.y));

            switch (event.motion) {
                case Down:
                    path.moveTo(event.x, event.y);
                    break;
                case Up:
                case Move:
                    path.lineTo(event.x, event.y);
                    break;
            }
        }
    }

    private void initSurface(SurfaceHolder holder) {
        Log.d("DrawingProcessor", "initSurface");
        this.holder = holder;

        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(STROKE_WIDTH);

        Canvas canvas = holder.lockCanvas();
        Paint background = new Paint();
        background.setColor(Color.WHITE);
        canvas.drawPaint(background);
        holder.unlockCanvasAndPost(canvas);
    }

    private void draw(RectF dirtyRect) {
        long start = System.currentTimeMillis();

        Rect dirty = new Rect(
                (int) (dirtyRect.left - HALF_STROKE_WIDTH),
                (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                (int) (dirtyRect.bottom + HALF_STROKE_WIDTH)
        );
        Canvas canvas = holder.lockCanvas(dirty);

        paint.setColor(Color.WHITE);
        canvas.drawPaint(paint);
        paint.setColor(Color.BLACK);
        canvas.drawPath(path, paint);
        holder.unlockCanvasAndPost(canvas);
        Log.d("DrawingProcessor", "drawing:    " + elapsedTime() + ": duration = " + (System.currentTimeMillis() - start) + "; " + dirty);
    }

    private synchronized long elapsedTime() {
        long now = System.currentTimeMillis();
        long res = now - lastTime;
        lastTime = now;
        return res;
    }
}
