package com.decokee.decokeemobile.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressBar extends View {
    private Paint paint;
    private RectF rect;
    private float angle;

    public CircularProgressBar(Context context) {
        super(context);
        init();
    }

    public CircularProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(100);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        rect = new RectF(50, 50, 200, 200);
        angle = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(rect, 270, angle, false, paint);
    }

    public void setProgress(float progress) {
        angle = progress * 360 / 100;
        invalidate();
    }
}

