package com.decokee.decokeemobile.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.decokee.decokeemobile.R;

import java.io.File;
import java.io.FileInputStream;

public class ActionItem extends RelativeLayout {

    private static final String TAG = ActionItem.class.getSimpleName();

    private ImageView mImageView;
    private ImageView mAlertView;
    private TextView mActionTitle;
    private TextView mCountdown;
    private CircularProgressBar mCircularProgressBar;

    private String mLastLoadImgPath = "";
    private Bitmap mBitmap;

    private CountDownTimer mCountDownTimer;

    private Handler mHandler;


    public ActionItem(Context context) {
        super(context);
        init(context);
        mHandler = new Handler(Looper.getMainLooper());
    }

    public ActionItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        mHandler = new Handler(Looper.getMainLooper());
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_action_item, this, true);
        mImageView = (ImageView) findViewById(R.id.action_item_image);
        mActionTitle = (TextView) findViewById(R.id.action_item_name);
        mCountdown = (TextView) findViewById(R.id.action_item_countdown);
        mAlertView = (ImageView) findViewById(R.id.action_alert_image);
        mCircularProgressBar = (CircularProgressBar) findViewById(R.id.progress_circular);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mImageView.getLayoutParams();

        // 获取父布局的宽高
        mImageView.post(() -> {
            int parentWidth = ((View) mImageView.getParent()).getWidth();
            int parentHeight = ((View) mImageView.getParent()).getHeight();

            // 设置ImageView宽高为父布局的80%
            layoutParams.width = (int) (parentWidth * 0.8);
            layoutParams.height = (int) (parentHeight * 0.8);
            mImageView.setLayoutParams(layoutParams);
        });
    }

    public void setActionTitle(String title) {
        mActionTitle.setText(title);
    }

    public void setTextColor(String color) {
        mActionTitle.setTextColor(Color.parseColor(color));
    }

    public void setTextSize(int size) {
        mActionTitle.setTextSize(size);
    }

    public void setTextStyle(String style) {

        int typefaceStyle = 0;

        if (style.contains("bold")) {
            typefaceStyle |= Typeface.BOLD;
        }

        if (style.contains("italic")) {
            typefaceStyle |= Typeface.ITALIC;
        }

        Log.d(TAG, "setTextStyle: Style: " + style + " typefaceStyle: " + typefaceStyle);

        mActionTitle.setTypeface(mActionTitle.getTypeface(), typefaceStyle);

        if (style.contains("underline")) {
            mActionTitle.setPaintFlags(mActionTitle.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }
    }

    public void showAlert(int resId) {
        if (resId == -1) {
            hideAlert();
            return;
        }
        mAlertView.setVisibility(VISIBLE);
        mAlertView.setImageResource(resId);
        mHandler.postDelayed(() -> {
            mAlertView.setVisibility(GONE);
        }, 1000);
    }

    public void hideAlert() {
        mAlertView.setVisibility(GONE);
    }

    public void setImageResource(String filePath) {
        this.setImageResource(filePath, false);
    }

    public void setImageResource(String filePath, boolean forceSet) {
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            mImageView.setImageBitmap(null);
            if (mBitmap != null) {
                mBitmap.recycle();
            }
            mBitmap = null;
            mLastLoadImgPath = "";
            return;
        }

        if (mLastLoadImgPath.equals(filePath) && !forceSet) {
            return;
        }
        mLastLoadImgPath = filePath;

        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mBitmap = null;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            mBitmap = BitmapFactory.decodeStream(fis);
            if (mBitmap == null) {
                Log.d(TAG, "setImageResource: Image is null clear.");
                mImageView.setImageBitmap(null);
            } else {
                Log.d(TAG, "setImageResource: width: " + mBitmap.getWidth() + " height: " + mBitmap.getHeight());
                Log.d(TAG, "setImageResource: Image width: " + mBitmap.getWidth() + " height: " + mBitmap.getHeight());
                mImageView.setImageBitmap(mBitmap);
                mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }

        } catch (Exception e) {
            e.printStackTrace();
            mLastLoadImgPath = "";
        }
    }

    public void startCountdown(long time) {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        if (time == -1) {
            mCountdown.setVisibility(GONE);
            return;
        }

        if (mCountdown.getVisibility() != VISIBLE) {
            mCountdown.setVisibility(VISIBLE);
        }

        long countDownInterval = 1000; // 间隔1秒

        mCountDownTimer = new CountDownTimer(time, countDownInterval) {

            public void onTick(long millisUntilFinished) {
                int secondsRemaining = Math.round(millisUntilFinished / 1000f);
                long minutes = secondsRemaining / 60;
                long seconds = secondsRemaining % 60;
                mCountdown.setText(String.format("%d:%02d", minutes, seconds));
            }

            public void onFinish() {
                mCountdown.setVisibility(GONE);
            }

        };
        mCountDownTimer.start();


        //Implement your countdown logic
    }

    public void setPosition(String position) {
        RelativeLayout.LayoutParams paramsTitle = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if (position.equals("top")) {
            paramsTitle.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        } else if (position.equals("mid")) {
            paramsTitle.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        } else {
            paramsTitle.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        }
        mActionTitle.setLayoutParams(paramsTitle);
        mActionTitle.bringToFront();
    }

    public void showProgress(int progress) {
        mCircularProgressBar.setProgress(progress);
        if (progress < 100) {
            if (mCircularProgressBar.getVisibility() != VISIBLE) {
                mCircularProgressBar.setVisibility(VISIBLE);
            }
        } else {
            mHandler.postDelayed(() -> {
                if (mCircularProgressBar.getVisibility() != GONE) {
                    mCircularProgressBar.setVisibility(GONE);
                }
            }, 1000);
        }
    }

}

