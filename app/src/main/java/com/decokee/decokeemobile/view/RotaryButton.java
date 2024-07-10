package com.decokee.decokeemobile.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;

public class RotaryButton extends View {

    private static final String TAG = RotaryButton.class.getSimpleName();

    private static final float INNER_RING_WIDTH = 1.0f;

    private ImageView mImageView;
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
    private Bitmap mBitmap;

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
        mPaint.setStrokeWidth(20);

        mInnerPaint = new Paint();
        mInnerPaint.setAntiAlias(true);
        mInnerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mInnerPaint.setStrokeWidth(20);
        mAngle = 270;

        mImageView = new ImageView(getContext());
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


        mImageView.layout((int) (mCenterX - getWidth() / 2), (int) (mCenterY - getHeight() / 2), (int) (mCenterX + getWidth() / 2), (int) (mCenterY + getHeight() / 2));
        mImageView.draw(canvas);

        if (mAlertView.getVisibility() == View.VISIBLE) {
            mAlertView.layout((int) (mCenterX - getWidth() / 2), (int) (mCenterY - getHeight() / 2), (int) (mCenterX + getWidth() / 2), (int) (mCenterY + getHeight() / 2));
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
            mImageView.setImageBitmap(null);
            if (mBitmap != null) {
                mBitmap.recycle();
            }
            mBitmap = null;
            mLastLoadImgPath = "";
            invalidate();
            return;
        }

        if (mLastLoadImgPath.equals(filePath)) {
            return;
        }
        mLastLoadImgPath = filePath;

        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mBitmap = null;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            mBitmap = BitmapFactory.decodeStream(fis);
            Log.d(TAG, "setImageResource: width: " + mBitmap.getWidth() + " height: " + mBitmap.getHeight());
            Log.d(TAG, "setImageResource: Image width: " + mBitmap.getWidth() + " height: " + mBitmap.getHeight());
            mImageView.setImageBitmap(mBitmap);
            mImageView.setScaleType(ImageView.ScaleType.CENTER);
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
