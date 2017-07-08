package com.kgdsoftware.gopigobt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;

/**
 * Created by hank on 12/7/16.
 */

public class TouchPad extends View {
    private static final String TAG = "SS";

    private Paint touchPaint;
    private Paint vectorPaint;
    private Paint borderPaint;
    private float borderWidth = 10.0f;
    private double width, height;
    private double radius;
    private double pointer[] = new double[2];
    private double origin[] = new double[2];
    private double tmp[] = new double[2];
    private double axis[] = new double[2];
    private Listener listener;

    private String pattern = "####.##";
    private DecimalFormat decimalFormat = new DecimalFormat(pattern);

    public interface Listener {
        public void onUp();
        public void onDown();
        public void onMove(double angle, double dx, double dy, double length);
    }

    public TouchPad(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        touchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        touchPaint.setColor(Color.GREEN);

        vectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vectorPaint.setColor(Color.RED);
        vectorPaint.setStrokeWidth(8.0f);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.BLUE);
        borderPaint.setStrokeWidth(borderWidth);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.v(TAG, "onSizeChanged: " + w + " X " + h);
        width = w;
        height = h;
        if (width < height) {
            radius = width / 2;
        } else {
            radius = height / 2;
        }
        radius -= 10.0f;
        origin[0] = width / 2;
        origin[1] = height / 2;

        pointer[0] = origin[0];
        pointer[1] = origin[1];

        axis[0] = 0;
        axis[1] = -height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int left = (int)borderWidth / 2;
        int right = (int)(width - borderWidth / 2);
        int top = (int)borderWidth / 2;
        int bottom = (int)(height - borderWidth / 2);

        canvas.drawLine(left, top, right, top, borderPaint);
        canvas.drawLine(right, top, right, bottom, borderPaint);
        canvas.drawLine(right, bottom, left, bottom, borderPaint);
        canvas.drawLine(left, bottom, left, top, borderPaint);

        canvas.drawCircle((float)origin[0], (float)origin[1], (float)radius, touchPaint);

        canvas.drawLine((float)origin[0], (float)origin[1], (float)pointer[0], (float)pointer[1], vectorPaint);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        pointer[0] = event.getX();
        pointer[1] = event.getY();
//        Log.v(TAG, "Pointer: "
//                + decimalFormat.format(pointer[0]) + ", "
//                + decimalFormat.format(pointer[1]));
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.v(TAG, "ACTION_DOWN");
                if(listener != null) listener.onDown();
                break;
            case MotionEvent.ACTION_UP:
                Log.v(TAG, "ACTION_UP stop");
                pointer[0] = origin[0];
                pointer[1] = origin[1];
                if(listener != null) listener.onUp();
                break;
            case MotionEvent.ACTION_MOVE:
                tmp[0] = pointer[0] - origin[0];
                tmp[1] = pointer[1] - origin[1];
                double c= angle(tmp, axis);
                double a = Math.acos(c);
                double deg = Math.toDegrees(a);

                // Look at the angle to decide if go forward, backard, left or right.
                if(listener != null) listener.onMove(deg, tmp[0], tmp[1], len(tmp) / radius);
                break;
        }
        invalidate();
        return true;
    }

    private double len(double[] v) {
        double r = (double) Math.sqrt(v[0] * v[0] + v[1] * v[1]);
        return r;
    }
    private double dot(double[] v1, double[] v2) {
        return v1[0] * v2[0] + v1[1] * v2[1];
    }
    private double angle(double[] v1, double[] v2) {
        double d = dot(v1, v2);
        return d / (len(v1) * len(v2));
    }
}
