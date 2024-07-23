package com.decokee.decokeemobile.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.decokee.decokeemobile.utils.Constants;

import java.io.File;
import java.io.FileInputStream;

public class RotaryButton extends View {

    private static final String TAG = RotaryButton.class.getSimpleName();

    private static final float INNER_RING_WIDTH = 0.6f;

    private static final int[] IMG_SIZE = { 100, 100 };

    private ImageView mAlertView;

    private float mCenterX, mCenterY;
    private float mRadius;
    private Paint mPaint;
    private Paint mInnerPaint;
    private float mAngle;
    private OnRotaryButtonListener mListener;

    private boolean mIsKeyDown = false;

    private Handler mHandler;


    private String mLastLoadImgPath = "";
    private Bitmap mMainBitmap;

    public RotaryButton(Context context) {
        super(context);
        init();
    }

    public RotaryButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RotaryButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHandler = new Handler(Looper.getMainLooper());

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(10);

        mInnerPaint = new Paint();
        mInnerPaint.setAntiAlias(true);
        mInnerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mInnerPaint.setStrokeWidth(20);
        mAngle = 270;

        mAlertView = new ImageView(getContext());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2.0f;
        mCenterY = h / 2.0f;
        mRadius = Math.min(mCenterX, mCenterY) - mPaint.getStrokeWidth() / 2.0f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint);
        canvas.drawCircle(mCenterX, mCenterY, mRadius * INNER_RING_WIDTH, mInnerPaint);
        canvas.drawLine(mCenterX, mCenterY, mCenterX + (float) Math.cos(Math.toRadians(mAngle)) * mRadius, mCenterY + (float) Math.sin(Math.toRadians(mAngle)) * mRadius, mPaint);

        if (mMainBitmap != null && !mMainBitmap.isRecycled()) {
            float scaleFactor = Math.min((float) getWidth() * INNER_RING_WIDTH * Constants.IMG_DISPLAY_FILL_PERCENT / mMainBitmap.getWidth(), getHeight() * INNER_RING_WIDTH * Constants.IMG_DISPLAY_FILL_PERCENT / mMainBitmap.getHeight());

            // Calculate the scaled width and height
            int scaledWidth = (int) (mMainBitmap.getWidth() * scaleFactor);
            int scaledHeight = (int) (mMainBitmap.getHeight() * scaleFactor);

            // Calculate the offset values to center the scaled bitmap
            int offsetX = (int) mCenterX - scaledWidth / 2;
            int offsetY = (int) mCenterY - scaledHeight / 2;

            // Create a Rect object to define the destination rectangle
            Rect destRect = new Rect(offsetX, offsetY, offsetX + scaledWidth, offsetY + scaledHeight);

            canvas.drawBitmap(mMainBitmap, null, destRect, mPaint);
        }

        if (mAlertView.getVisibility() == View.VISIBLE) {
            mAlertView.layout((int) (mCenterX - getWidth() / 2.0f), (int) (mCenterY - getHeight() / 2.0f), (int) (mCenterX + getWidth() / 2.0f), (int) (mCenterY + getHeight() / 2.0f));
            mAlertView.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - mCenterX;
        float y = event.getY() - mCenterY;
        float distance = (float) Math.sqrt(x * x + y * y);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (distance < mRadius * INNER_RING_WIDTH) {
                mIsKeyDown = true;
                if (mListener != null) {
                    mListener.onDown();
                }
            } else if (distance >= mRadius * INNER_RING_WIDTH && distance <= mRadius * 1.0f) {
                if (x > 0) {
                    if (mListener != null) {
                        mListener.onRightRotate();
                    }
                } else {
                    if (mListener != null) {
                        mListener.onLeftRotate();
                    }
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (distance >= mRadius * INNER_RING_WIDTH && distance <= mRadius * 1.0f) {
                float angle = (float) Math.toDegrees(Math.atan2(y, x));
                float deltaAngle = angle - mAngle;
                if (Math.abs(deltaAngle) >= 5) {
                    mAngle = angle;
                    invalidate();
                    if (deltaAngle > 0) {
                        if (mListener != null) {
                            mListener.onRightRotate();
                        }
                    } else {
                        if (mListener != null) {
                            mListener.onLeftRotate();
                        }
                    }
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            mAngle = 270;
            invalidate();
            if (mIsKeyDown) {
                mIsKeyDown = false;
                if (mListener != null) {
                    mListener.onUp();
                }
            }
        }

        return true;
    }

    public void setOnRotaryButtonListener(OnRotaryButtonListener listener) {
        mListener = listener;
    }


    public void showAlert(int resId) {
        if (resId == -1) {
            hideAlert();
            return;
        }
        mAlertView.setVisibility(VISIBLE);
        mAlertView.setImageResource(resId);
        mHandler.postDelayed(this::hideAlert, 1000);
        invalidate();
    }

    public void hideAlert() {
        mAlertView.setVisibility(GONE);
        invalidate();
    }

    public void setImageResource(String filePath) {
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            if (mMainBitmap != null) {
                mMainBitmap.recycle();
            }
            mMainBitmap = null;
            mLastLoadImgPath = "";
            invalidate();
            return;
        }

        if (mLastLoadImgPath.equals(filePath)) {
            return;
        }
        mLastLoadImgPath = filePath;

        if (mMainBitmap != null) {
            mMainBitmap.recycle();
        }
        mMainBitmap = null;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            mMainBitmap = BitmapFactory.decodeStream(fis);
            Log.d(TAG, "setImageResource: width: " + mMainBitmap.getWidth() + " height: " + mMainBitmap.getHeight());
            Log.d(TAG, "setImageResource: Image width: " + mMainBitmap.getWidth() + " height: " + mMainBitmap.getHeight());

            mMainBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

        } catch (Exception e) {
            e.printStackTrace();
        }
        invalidate();
    }

    public interface OnRotaryButtonListener {
        void onDown();

        void onUp();

        void onLeftRotate();

        void onRightRotate();
    }
}
