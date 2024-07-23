package com.decokee.decokeemobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.decokee.decokeemobile.bean.ConfigInfo;
import com.decokee.decokeemobile.bean.KeyEventBean;
import com.decokee.decokeemobile.bean.ResourceInfo;
import com.decokee.decokeemobile.utils.Constants;
import com.decokee.decokeemobile.utils.UpdateCheckUtil;
import com.decokee.decokeemobile.view.ActionItem;
import com.decokee.decokeemobile.view.RotaryButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements View.OnTouchListener, WebSocketClient.WSClientDataListener {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int MSG_HIDE_CONFIG_SETTINGS = 0x01;

    private static final String SERVER_URL_END_FIX = ":20230?sn=";

    private static final String USER_CONFIG_IP = "USER_CONFIG_IP";

    private static final String[] PERMISSIONS_STORAGE = {
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.VIBRATE",
            "android.permission.ACCESS_COARSE_LOCATION"};
    private static final String TAG = MainActivity.class.getSimpleName();

    private List<ActionItem> mRowColActionKeyList = new ArrayList<>();

    private Button mConnectionButton;
    private TextView mProfileInfo;
    private TextView mAppVersionText;

    private EditText mIpConfigText;
    private Button mSaveIpButton;
    private EditText mRowCountText;
    private EditText mColCountText;

    private Button mCleanCacheButton;

    private ImageView mShowConfigButton;
    private Button mCheckUpdateButton;

    private RelativeLayout mConfigContainer;

    private RelativeLayout mKeyConfigItemsContainer;

    private RotaryButton mRotaryButton;

    private final Gson mGSON = new Gson();

    private final ConcurrentHashMap<String, Integer> mKeyLastStateMap = new ConcurrentHashMap<>(6);

    private final ExecutorService mExecutors = Executors.newSingleThreadExecutor();
    private SharedPreferences mPreferences;
    private VibrationEffect mKeyPress;
    private Vibrator mVibrator;
    private float mScreenBrightness;

    private final Handler mHandler = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_HIDE_CONFIG_SETTINGS:

                    if (!WebSocketClient.getInstance(MainActivity.this).isConnected()) return;

                    runOnUiThread(() -> {
                        mConfigContainer.setVisibility(View.GONE);

                        mHandler.postDelayed(() -> {
                            int margin = getResources().getDimensionPixelSize(R.dimen.action_item_margin); // 例如，定义在 dimens.xml 中的间隔大小
                            updateItemSize(mKeyConfigItemsContainer, mMaxRowNum, mMaxColNum, margin);

                            for (ActionItem actionItem : mRowColActionKeyList) {
                                actionItem.checkAndUpdateIconSize();
                            }
                        }, 500);

                    });
                    break;
            }
        }
    };
    private String mUserKeyMatrixSettingIdx;
    private int mMaxColNum;
    private int mMaxRowNum;
    private List<ConfigInfo> mActiveConfigInfos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        verifyStoragePermissions(this);

        mConfigContainer = findViewById(R.id.config_container);

        mKeyConfigItemsContainer = findViewById(R.id.button_items_container);

        mAppVersionText = findViewById(R.id.app_version_text);

        try {
            String currentVersion = getApplicationContext().getPackageManager()
                    .getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
            mAppVersionText.setText("当前版本: " + currentVersion);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mConnectionButton = findViewById(R.id.connection_status);
        mProfileInfo = findViewById(R.id.profile_info_text);

        mIpConfigText = findViewById(R.id.ip_config_input);
        mSaveIpButton = findViewById(R.id.ip_config_save_btn);

        mShowConfigButton = findViewById(R.id.settings_btn);

        mRotaryButton = findViewById(R.id.knob_0_1);

        mSaveIpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPreferences == null) {
                    mPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                }
                SharedPreferences.Editor edit = mPreferences.edit();
                String configedIp = mIpConfigText.getText().toString();
                edit.putString(USER_CONFIG_IP, configedIp);

                String rowCount = mRowCountText.getText().toString();
                String colCount = mColCountText.getText().toString();

                mMaxRowNum = Integer.parseInt(rowCount);
                mMaxColNum = Integer.parseInt(colCount);

                if (mMaxRowNum > 10) {
                    mMaxRowNum = 10;
                } else if (mMaxRowNum < 1) {
                    mMaxRowNum = 1;
                }

                mRowCountText.setText(String.valueOf(mMaxRowNum));

                if (mMaxColNum > 10) {
                    mMaxColNum = 10;
                } else if (mMaxColNum < 1) {
                    mMaxColNum = 1;
                }
                mColCountText.setText(String.valueOf(mMaxColNum));

                edit.putString(Constants.USER_KEY_MATRIX_SETTING, mMaxRowNum + "x" + mMaxColNum);
                edit.apply();

                int margin = getResources().getDimensionPixelSize(R.dimen.action_item_margin); // 例如，定义在 dimens.xml 中的间隔大小

                addActionItemsToRelativeLayout(mKeyConfigItemsContainer, mMaxRowNum, mMaxColNum, margin);

                if (TextUtils.isEmpty(configedIp) || configedIp.equals("ws://") || configedIp.equals("wss://")) {
                    return;
                }


                checkAndConnectServer(configedIp);
            }
        });

        mConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (WebSocketClient.getInstance(MainActivity.this).isConnected()) {
                    WebSocketClient.getInstance(MainActivity.this).disconnectToServer();
                } else {
                    checkAndConnectServer(null);
                }
            }
        });

        mShowConfigButton.setOnClickListener(view -> runOnUiThread(() -> {
            if (mConfigContainer.getVisibility() == View.VISIBLE) {
                mConfigContainer.setVisibility(View.GONE);

                mHandler.postDelayed(() -> {
                    int margin = getResources().getDimensionPixelSize(R.dimen.action_item_margin); // 例如，定义在 dimens.xml 中的间隔大小
                    updateItemSize(mKeyConfigItemsContainer, mMaxRowNum, mMaxColNum, margin);

                    for (ActionItem actionItem : mRowColActionKeyList) {
                        actionItem.checkAndUpdateIconSize();
                    }
                }, 500);
            } else {
                mConfigContainer.setVisibility(View.VISIBLE);
            }
        }));


        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        mScreenBrightness = layoutParams.screenBrightness;

        WebSocketClient.getInstance(MainActivity.this).registerConnectionStatusListener(this);

        runOnUiThread(() -> {
            mConnectionButton.setText("未连接");
        });

        mExecutors.submit(() -> {
            try {
                ResourceManager.getInstance().init(this);
                mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String userConfigIp = mPreferences.getString(USER_CONFIG_IP, "ws://");

                runOnUiThread(() -> {
                    mIpConfigText.setText(userConfigIp);
                });
            } catch (Exception e) {
                Log.e(TAG, "onCreate: ", e);
            }
        });

        mCheckUpdateButton = findViewById(R.id.check_update_btn);
        mCheckUpdateButton.setOnClickListener(view -> mExecutors.submit(() -> UpdateCheckUtil.checkForUpdates(getApplicationContext(), (haveUpdate) -> {
            if (haveUpdate.equals(0)) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "当前已是最新版本", Toast.LENGTH_LONG).show());
                return;
            } else if (haveUpdate.equals(2)) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "正在下载更新，请稍候......", Toast.LENGTH_LONG).show());
                return;
            }

            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "检测到更新，开始下载更新，请稍候......", Toast.LENGTH_LONG).show());

        })));

        mCleanCacheButton = findViewById(R.id.clean_cache_btn);
        mCleanCacheButton.setOnClickListener(view -> {
            mExecutors.submit(() -> {
                WebSocketClient.getInstance(MainActivity.this)
                        .disconnectToServer()
                        .cleanCache();

                runOnUiThread(() -> {
                    for (ActionItem actionItem : mRowColActionKeyList) {
                        actionItem.setActionTitle("");
                        actionItem.setImageResource("", true);
                    }
                    mRotaryButton.setImageResource("");
                });

                ResourceManager.getInstance().cleanAllResource();

                checkAndConnectServer(null);
            });
        });


        mRotaryButton.setOnRotaryButtonListener(new RotaryButton.OnRotaryButtonListener() {
            @Override
            public void onDown() {
                Log.d(TAG, "onDown: ");
                vibrate();
                mExecutors.submit(() -> {
                    KeyEventBean keyEventBean = new KeyEventBean();
                    keyEventBean.setKeyAction(0);
                    keyEventBean.setType("press");
                    keyEventBean.setKeyCode("0,1");
                    WebSocketClient.getInstance(MainActivity.this).sendMsg(mGSON.toJson(keyEventBean));
                });
            }

            @Override
            public void onUp() {
                Log.d(TAG, "onUp: ");
                mExecutors.submit(() -> {
                    KeyEventBean keyEventBean = new KeyEventBean();
                    keyEventBean.setKeyAction(1);
                    keyEventBean.setType("press");
                    keyEventBean.setKeyCode("0,1");
                    WebSocketClient.getInstance(MainActivity.this).sendMsg(mGSON.toJson(keyEventBean));
                });
            }

            @Override
            public void onLeftRotate() {
                Log.d(TAG, "onLeftRotate: ");
                vibrate();
                mExecutors.submit(() -> {
                    KeyEventBean keyEventBean = new KeyEventBean();
                    keyEventBean.setKeyAction(2);
                    keyEventBean.setType("press");
                    keyEventBean.setKeyCode("0,1");
                    WebSocketClient.getInstance(MainActivity.this).sendMsg(mGSON.toJson(keyEventBean));
                });
            }

            @Override
            public void onRightRotate() {
                Log.d(TAG, "onRightRotate: ");
                vibrate();
                mExecutors.submit(() -> {
                    KeyEventBean keyEventBean = new KeyEventBean();
                    keyEventBean.setKeyAction(3);
                    keyEventBean.setType("press");
                    keyEventBean.setKeyCode("0,1");
                    WebSocketClient.getInstance(MainActivity.this).sendMsg(mGSON.toJson(keyEventBean));
                });
            }
        });

        if (mPreferences == null) {
            mPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        }

        mUserKeyMatrixSettingIdx = mPreferences.getString(Constants.USER_KEY_MATRIX_SETTING, "2x3");

        String[] keyMatrixInfo = mUserKeyMatrixSettingIdx.split("x");
        mMaxRowNum = Integer.parseInt(keyMatrixInfo[0]);
        mMaxColNum = Integer.parseInt(keyMatrixInfo[1]);

        mRowCountText = findViewById(R.id.row_count_input);
        mColCountText = findViewById(R.id.col_count_input);

        mRowCountText.setText(String.valueOf(mMaxRowNum));
        mColCountText.setText(String.valueOf(mMaxColNum));


        mHandler.postDelayed(() -> {
            int margin = getResources().getDimensionPixelSize(R.dimen.action_item_margin); // 例如，定义在 dimens.xml 中的间隔大小
            addActionItemsToRelativeLayout(mKeyConfigItemsContainer, mMaxRowNum, mMaxColNum, margin);
        }, 1000);

        mHandler.postDelayed(() -> {

            checkAndConnectServer(null);
        }, 3000);
    }

    private void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        ActionItem actionItemView = (ActionItem) view;
        String keyCode = actionItemView.getKeyCode();

        boolean isKeyUp = motionEvent.getAction() == MotionEvent.ACTION_UP;

        Integer lastState = mKeyLastStateMap.get(keyCode);
        int newState = isKeyUp ? 1 : 0;
        if (lastState == null || !lastState.equals(newState)) {
            mKeyLastStateMap.put(keyCode, newState);
            KeyEventBean keyEventBean = new KeyEventBean();
            keyEventBean.setKeyAction(newState);
            keyEventBean.setType("press");
            keyEventBean.setKeyCode(keyCode);
            vibrate();
            mExecutors.submit(() -> {
                WebSocketClient.getInstance(MainActivity.this).sendMsg(mGSON.toJson(keyEventBean));
            });
        }


        return true;
    }

    @Override
    public void onConnectionStatusChange(boolean connected) {
        runOnUiThread(() -> {
            if (connected) {
                mConnectionButton.setText("已连接");
                mConnectionButton.setBackgroundColor(Color.GREEN);
            } else {
                mHandler.removeMessages(MSG_HIDE_CONFIG_SETTINGS);
                mConfigContainer.setVisibility(View.VISIBLE);

                mConnectionButton.setText("未连接");
                mConnectionButton.setBackgroundColor(Color.GRAY);
            }
        });
    }

    @Override
    public void onProfileChange(String resourceId, int configId, int page, int folder) {
        String newProfileInfo = String.format("ResourceId: %s configIf: %d page: %d folder: %d", resourceId, configId, page, folder);
        Log.d(TAG, "onProfileChange: newProfileInfo: " + newProfileInfo);
        runOnUiThread(() -> {
            mProfileInfo.setText(newProfileInfo);
        });

        mActiveConfigInfos = ResourceManager.getInstance().getConfigInfo(resourceId);
        if (mActiveConfigInfos == null || mActiveConfigInfos.isEmpty()) return;

        for (ConfigInfo configInfo : mActiveConfigInfos) {
            Log.d(TAG, "onProfileChange: Process: " + configInfo);

            ConfigInfo.Config currentConfig = configInfo.getConfig();
            String iconResourceId = currentConfig.getIcon();
            if (currentConfig.getType().equals("pageNo") && currentConfig.getAnimations() != null && !currentConfig.getAnimations().isEmpty()) {
                iconResourceId = currentConfig.getAnimations().get(page - 1);
            }

            ResourceInfo resourceInfo = ResourceManager.getInstance().getResourceInfo(iconResourceId);
            String resourcePath;
            if (resourceInfo != null) {
                resourcePath = resourceInfo.getPath();
            } else {
                resourcePath = "";
            }
            ConfigInfo.Config.Title titleInfo = currentConfig.getTitle();
            String text = titleInfo.getText();
            String pos = titleInfo.getPos();
            String color = titleInfo.getColor();
            boolean display = titleInfo.isDisplay();
            int titleInfoSize = titleInfo.getSize();
            String titleInfoStyle = titleInfo.getStyle();


            String keyCode = configInfo.getKeyCode();

            String[] keyPressInfo = keyCode.split(",");
            if (keyPressInfo.length != 2) continue;

            int rowIdx = Integer.parseInt(keyPressInfo[0]);
            int colIdx = Integer.parseInt(keyPressInfo[1]);

            if (rowIdx == 0) {
                runOnUiThread(() -> mRotaryButton.setImageResource(resourcePath));
                continue;
            }

            if (rowIdx > mMaxRowNum || colIdx > mMaxColNum) {
                continue;
            }

            if (mRowColActionKeyList == null || mRowColActionKeyList.isEmpty()) continue;

            Log.d(TAG, "onProfileChange: Setup View for " + rowIdx + ":" + colIdx + ":" + mMaxColNum + " SetImgResource: " + resourcePath);
            int keyItemIdx = (rowIdx - 1) * mMaxColNum + (colIdx - 1);

            if (keyItemIdx >= mRowColActionKeyList.size()) continue;

            ActionItem keyButton = mRowColActionKeyList.get(keyItemIdx);

            Log.d(TAG, "onProfileChange: Load for Icon ResId: " + iconResourceId + " Path: " + resourcePath);
            runOnUiThread(() -> {
                if (display && keyButton.getVisibility() != View.VISIBLE) {
                    keyButton.setVisibility(View.VISIBLE);
                } else if (!display && keyButton.getVisibility() != View.GONE) {
                    keyButton.setVisibility(View.GONE);
                }
                keyButton.setTextColor(color);
                keyButton.setPosition(pos);
                keyButton.setTextSize(titleInfoSize);
                keyButton.setTextStyle(titleInfoStyle);
                keyButton.setActionTitle(text);
                keyButton.setImageResource(resourcePath, true);
            });
        }
    }

    @Override
    public void onShowAlert(String keyCode, int type) {
        int alertResId;
        switch (type) {
            default:
            case 0:
                alertResId = -1;
                break;
            case 1:
                alertResId = R.drawable.alert;
                break;
            case 2:
                alertResId = R.drawable.alarm;
                break;
            case 3:
                alertResId = R.drawable.checkmark;
                break;
        }

        String[] keyPressInfo = keyCode.split(",");
        if (keyPressInfo.length != 2) return;

        int rowIdx = Integer.parseInt(keyPressInfo[0]);
        int colIdx = Integer.parseInt(keyPressInfo[1]);

        if (rowIdx == 0) {
            runOnUiThread(() -> mRotaryButton.showAlert(alertResId));
            return;
        }

        if (mRowColActionKeyList == null || mRowColActionKeyList.isEmpty()) return;

        int keyItemIdx = (rowIdx - 1) * mMaxColNum + (colIdx - 1);
        if (keyItemIdx >= mRowColActionKeyList.size()) return;
        ActionItem keyButton = mRowColActionKeyList.get(keyItemIdx);
        runOnUiThread(() -> keyButton.showAlert(alertResId));
    }

    @Override
    public void showAnimation(String keyCode, String resourceId, int time) {
        ResourceInfo resourceInfo = ResourceManager.getInstance().getResourceInfo(resourceId);
        String resourcePath;
        if (resourceInfo != null) {
            resourcePath = resourceInfo.getPath();
        } else {
            resourcePath = "";
        }

        String[] keyPressInfo = keyCode.split(",");
        if (keyPressInfo.length != 2) return;

        int rowIdx = Integer.parseInt(keyPressInfo[0]);
        int colIdx = Integer.parseInt(keyPressInfo[1]);

        if (rowIdx == 0) {
            runOnUiThread(() -> mRotaryButton.setImageResource(resourcePath));
            return;
        }

        if (mRowColActionKeyList == null || mRowColActionKeyList.isEmpty()) return;

        int keyItemIdx = (rowIdx - 1) * mMaxColNum + (colIdx - 1);
        if (keyItemIdx >= mRowColActionKeyList.size()) return;

        ActionItem keyButton = mRowColActionKeyList.get(keyItemIdx);
        runOnUiThread(() -> keyButton.setImageResource(resourcePath));
    }

    @Override
    public void onShowProgress(String keyCode, int percent) {
        String[] keyPressInfo = keyCode.split(",");
        if (keyPressInfo.length != 2) return;

        int rowIdx = Integer.parseInt(keyPressInfo[0]);
        int colIdx = Integer.parseInt(keyPressInfo[1]);

        if (rowIdx == 0) return;

        if (mRowColActionKeyList == null || mRowColActionKeyList.isEmpty()) return;

        int keyItemIdx = (rowIdx - 1) * mMaxColNum + (colIdx - 1);
        if (keyItemIdx >= mRowColActionKeyList.size()) return;
        ActionItem keyButton = mRowColActionKeyList.get(keyItemIdx);
        runOnUiThread(() -> keyButton.showProgress(percent));
    }

    @Override
    public void onShowCountDown(String keyCode, long timeout) {
        String[] keyPressInfo = keyCode.split(",");
        if (keyPressInfo.length != 2) return;

        int rowIdx = Integer.parseInt(keyPressInfo[0]);
        int colIdx = Integer.parseInt(keyPressInfo[1]);

        if (rowIdx == 0) return;

        if (mRowColActionKeyList == null || mRowColActionKeyList.isEmpty()) return;

        int keyItemIdx = (rowIdx - 1) * mMaxColNum + (colIdx - 1);
        if (keyItemIdx >= mRowColActionKeyList.size()) return;

        ActionItem keyButton = mRowColActionKeyList.get(keyItemIdx);
        runOnUiThread(() -> keyButton.startCountdown(timeout));
    }

    @Override
    public void onBrightnessChange(int level) {

        switch (level) {
            default:
            case 0:
                mScreenBrightness += 0.25f;
                break;
            case 1:
                mScreenBrightness -= 0.25f;
                break;
            case 2:
                mScreenBrightness = 1.0f;
                break;
            case 3:
                mScreenBrightness = 0.75f;
                break;
            case 4:
                mScreenBrightness = 0.5f;
                break;
            case 5:
                mScreenBrightness = 0.25f;
                break;
            case 6:
                mScreenBrightness = 0.0f;
                break;
        }

        if (mScreenBrightness > 1.0f) {
            mScreenBrightness = 1.0f;
        } else if (mScreenBrightness < 0.0f) {
            mScreenBrightness = 0.0f;
        }

        runOnUiThread(() -> {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = mScreenBrightness;
            getWindow().setAttributes(layoutParams);
        });
    }

    @Override
    public void onStateChange(String keyCode, int state) {
        String[] keyPressInfo = keyCode.split(",");
        if (keyPressInfo.length != 2) return;

        int rowIdx = Integer.parseInt(keyPressInfo[0]);
        int colIdx = Integer.parseInt(keyPressInfo[1]);

        if (rowIdx == 0) return;

        ConfigInfo keyConfigInfo = null;
        for (ConfigInfo configInfo : mActiveConfigInfos) {
            if (configInfo.getKeyCode().equals(keyCode)) {
                keyConfigInfo = configInfo;
                break;
            }
        }

        if (keyConfigInfo == null) return;

        if (mRowColActionKeyList == null || mRowColActionKeyList.isEmpty()) return;

        int keyItemIdx = (rowIdx - 1) * mMaxColNum + (colIdx - 1);
        if (keyItemIdx >= mRowColActionKeyList.size()) return;

        ActionItem keyButton = mRowColActionKeyList.get(keyItemIdx);


        String iconResId;
        if (state == 0) {
            iconResId = keyConfigInfo.getConfig().getIcon();
        } else {
            iconResId = keyConfigInfo.getConfig().getAlterIcon();
        }

        ResourceInfo resourceInfo = ResourceManager.getInstance().getResourceInfo(iconResId);
        String resourcePath;
        if (resourceInfo != null) {
            resourcePath = resourceInfo.getPath();
        } else {
            resourcePath = "";
        }

        runOnUiThread(() -> keyButton.setImageResource(resourcePath));

    }

    private void checkAndConnectServer(String ip) {
        String newIp;
        if (ip == null) {
            newIp = mIpConfigText.getText().toString();
        } else {
            newIp = ip;
        }
        if (TextUtils.isEmpty(newIp)) {
            Toast.makeText(getApplicationContext(), "Invalid IP", Toast.LENGTH_SHORT).show();
        } else {
            mExecutors.submit(() -> {
                String fixedIp = newIp;
                if (WebSocketClient.getInstance(MainActivity.this).isConnected()) {
                    WebSocketClient.getInstance(MainActivity.this).disconnectToServer();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (!newIp.startsWith("ws")) {
                    fixedIp = "ws://" + newIp;
                }
                WebSocketClient.getInstance(MainActivity.this).connectToServer(fixedIp + SERVER_URL_END_FIX + getDeviceUUID());

                mHandler.removeMessages(MSG_HIDE_CONFIG_SETTINGS);
                mHandler.sendEmptyMessageDelayed(MSG_HIDE_CONFIG_SETTINGS, 5000);
            });
        }
    }

    private void vibrate() {
        if (mVibrator == null) {
            mVibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mKeyPress = VibrationEffect.createOneShot(100, 255);
            mVibrator.vibrate(mKeyPress);
        } else {
            mVibrator.vibrate(100);
        }
    }

    private String getDeviceUUID() {
        String uniqueIdentifier;
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId != null) {
            uniqueIdentifier = androidId;
        } else {
            uniqueIdentifier = UUID.randomUUID().toString();
        }
        return uniqueIdentifier;
    }

    private void setActionItemView(ActionItem view, String resourcePath, String text, String pos, String color, boolean display) {
        view.setImageResource(resourcePath);
        if (display && view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        } else if (!display && view.getVisibility() != View.GONE) {
            view.setVisibility(View.GONE);
        }
        view.setTextColor(color);
        view.setActionTitle(text);
        view.setPosition(pos);
    }

    private void updateItemSize(RelativeLayout parentLayout, int rows, int cols, int margin) {
        int screenWidth = parentLayout.getWidth();
        int screenHeight = parentLayout.getHeight();

//        int minParentSideSize = Math.min(screenWidth, screenHeight);
//        int maxRowCol = Math.max(rows, cols);
        int maxSize = Math.min((screenWidth / cols), screenHeight / rows) - margin;

        for (int i = 0; i < parentLayout.getChildCount(); i++) {
            View childView = parentLayout.getChildAt(i);
            ViewGroup.LayoutParams oldLayoutParam = childView.getLayoutParams();
            oldLayoutParam.width = maxSize;
            oldLayoutParam.height = maxSize;

            childView.setLayoutParams(oldLayoutParam);
        }
    }

    private void addActionItemsToRelativeLayout(RelativeLayout parentLayout, int rows, int cols, int margin) {
        parentLayout.removeAllViews();
        mRowColActionKeyList.clear();

        int screenWidth = parentLayout.getWidth();
        int screenHeight = parentLayout.getHeight();

//        int minParentSideSize = Math.min(screenWidth, screenHeight);
//        int maxRowCol = Math.max(rows, cols);
        int maxSize = Math.min((screenWidth / cols), screenHeight / rows) - margin;

        Log.d(TAG, "addActionItemsToRelativeLayout: screenWidth: " + screenWidth + " screenHeight: " + screenHeight + " rows: " + rows + " Col: " + cols + " maxSize: " + maxSize);

        int previousViewId = 0; // 用于跟踪前一个视图的 ID
        for (int row = 0; row < rows; row++) {
            previousViewId = 0; // 每行开始时重置 previousViewId
            for (int col = 0; col < cols; col++) {
                ActionItem actionItem = new ActionItem(this);
                actionItem.setId(View.generateViewId()); // 生成唯一 ID

                actionItem.setKeyCode((row + 1) + "," + (col + 1));

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(maxSize, maxSize);
                params.setMargins(margin, margin, 0, 0);

                if (row == 0) {
                    // 第一行
                    if (col > 0) {
                        // 非第一列
                        params.addRule(RelativeLayout.END_OF, previousViewId);
                    }
                } else {
                    // 非第一行
                    if (col == 0) {
                        // 每行的第一个
                        params.addRule(RelativeLayout.BELOW, parentLayout.getChildAt((row - 1) * cols).getId());
                    } else {
                        // 非第一列
                        params.addRule(RelativeLayout.END_OF, previousViewId);
                        params.addRule(RelativeLayout.BELOW, parentLayout.getChildAt((row - 1) * cols + col).getId());
                    }
                }

                parentLayout.addView(actionItem, params);
                previousViewId = actionItem.getId();

                actionItem.setOnTouchListener(MainActivity.this);

                mRowColActionKeyList.add(actionItem);
            }
        }
    }

}