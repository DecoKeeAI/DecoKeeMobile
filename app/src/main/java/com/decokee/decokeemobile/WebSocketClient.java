/*
 * Copyright 2024 DecoKee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Additional Terms for DecoKee:
 *
 * 1. Communication Protocol Usage
 *    DecoKee is provided subject to a commercial license and subscription
 *    as described in the Terms of Use (http://www.decokee.com/about/terms.html).
 *
 *    The components of this project related to the communication protocol
 *    (including but not limited to protocol specifications, implementation code, etc.)
 *    are restricted from commercial use, as such use would violate the project's usage policies.
 *    There are no restrictions for non-commercial uses.
 *
 *    (a) Evaluation Use
 *        An evaluation license is offered that provides a limited,
 *        evaluation license for internal and non-commercial use.
 *
 *        With a paid-up subscription you can incorporate new releases,
 *        updates and patches for the software into your products.
 *        If you do not have an active subscription, you cannot apply patches
 *        from the software to your products.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.decokee.decokeemobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.decokee.decokeemobile.bean.ConfigInfo;
import com.decokee.decokeemobile.bean.ReportDeviceConfigInfo;
import com.decokee.decokeemobile.bean.ResourceDetailData;
import com.decokee.decokeemobile.bean.ResourceDownloadInfo;
import com.decokee.decokeemobile.bean.ResourceInfo;
import com.decokee.decokeemobile.utils.Constants;
import com.decokee.decokeemobile.utils.FileUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient {

    private static final String TAG = WebSocketClient.class.getSimpleName();

    private static final String DEVICE_ACTIVE_PROFILE = "DEVICE_ACTIVE_PROFILE";

    private static volatile WebSocketClient sInstance;
    private WebSocket mWebSocket;

    private SharedPreferences mPreferences;

    private boolean mIsConnected = false;
    private WSClientDataListener mWSClientDataListener;

    private final ConcurrentHashMap<String, ResourceDownloadInfo> mResourceRequestMap = new ConcurrentHashMap<>();

    private final CopyOnWriteArrayList<String> mSubResourceDownloadList = new CopyOnWriteArrayList<>();

    private String mPendingConfigLoadId = "";

    private ResourceInfo mDeviceActiveProfileInfo = null;

    private final Gson mGSON = new Gson();
    private int mDeviceKeyMatrix;
    private String mAppVersion = "";

    public WebSocketClient(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String deviceActiveProfileInfoJson = mPreferences.getString(DEVICE_ACTIVE_PROFILE, "");
        if (!TextUtils.isEmpty(deviceActiveProfileInfoJson)) {
            mDeviceActiveProfileInfo = mGSON.fromJson(deviceActiveProfileInfoJson, ResourceInfo.class);
        }

        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            mAppVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static WebSocketClient getInstance(Context context) {
        if (sInstance == null) {
            synchronized (WebSocketClient.class) {
                if (sInstance == null) {
                    sInstance = new WebSocketClient(context);
                }
            }
        }
        return sInstance;
    }

    public WebSocketClient cleanCache() {
        SharedPreferences.Editor edit = mPreferences.edit();
        edit.putString(DEVICE_ACTIVE_PROFILE, "");
        edit.apply();

        mDeviceActiveProfileInfo = null;

        mResourceRequestMap.clear();
        mSubResourceDownloadList.clear();
        mPendingConfigLoadId = "";

        return this;
    }


    public void connectToServer(String url) {

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        mDeviceKeyMatrix = mPreferences.getInt(Constants.USER_KEY_MATRIX_SETTING, 0);

        mWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                // 连接成功时的处理
                Log.i(TAG, "onOpen: ");
                mIsConnected = true;
                if (mWSClientDataListener != null) {
                    mWSClientDataListener.onConnectionStatusChange(true);
                }

                String activeProfileId = "";
                long activeProfileVersion = 0;
                String generateTime = "";
                if (mDeviceActiveProfileInfo != null) {
                    activeProfileId = mDeviceActiveProfileInfo.getId();
                    activeProfileVersion = mDeviceActiveProfileInfo.getVersion();
                    generateTime = mDeviceActiveProfileInfo.getGenerateTime() == null ? "" : mDeviceActiveProfileInfo.getGenerateTime();
                }
                sendDeviceKeyMatrix();
                sendDeviceActiveProfile(activeProfileId, activeProfileVersion, generateTime);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                // 接收到消息时的处理
                Log.i(TAG, "onMessage: " + text);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                Log.i(TAG, "onMessage: binary");
                try {
                    Object decodeData = checkAndDecodeData(bytes);
                    if (decodeData == null) {
                        Log.w(TAG, "onMessage: Get invalid msg from server.");
                        return;
                    }

                    if (decodeData instanceof String) {
                        String msg = String.valueOf(decodeData);
                        JSONObject requestMsg = new JSONObject(msg);
                        String type = requestMsg.getString("type");

                        String resourceId = null;
                        if (requestMsg.has("resourceId")) {
                            resourceId = requestMsg.getString("resourceId");
                        }
                        long version = -1;
                        if (requestMsg.has("version")) {
                            version = requestMsg.getInt("version");
                        }
                        String keyCode = "";
                        if (requestMsg.has("keyCode")) {
                            keyCode = requestMsg.getString("keyCode");
                        }

                        switch (type) {
                            case "profile":
                                if (mWSClientDataListener == null) break;

                                ResourceInfo resourceInfo = ResourceManager.getInstance().getResourceInfo(resourceId);

                                String generateTime = null;
                                if (requestMsg.has("generateTime")) {
                                    generateTime = requestMsg.getString("generateTime");
                                }

                                if (resourceInfo == null
                                        || resourceInfo.getVersion() != version
                                        || resourceInfo.getGenerateTime() == null
                                        || !resourceInfo.getGenerateTime().equals(generateTime)) {
                                    sendRequestResource(resourceId, resourceInfo == null ? 0 : resourceInfo.getVersion(), ((resourceInfo == null || resourceInfo.getGenerateTime() == null) ? "" : resourceInfo.getGenerateTime()));

                                    mPendingConfigLoadId = resourceId;
                                    return;
                                }
                                mPendingConfigLoadId = resourceId;
                                checkForAllRelatedResources(resourceId);
                                break;
                            case "resource":
                                ResourceDownloadInfo resourceDownloadInfo = mGSON.fromJson(msg, ResourceDownloadInfo.class);

                                if (resourceDownloadInfo.getHasUpdate() == 0) {
                                    Log.d(TAG, "onMessage: Received no more update for: " + resourceId);
                                    mResourceRequestMap.remove(resourceId);
                                    mSubResourceDownloadList.remove(resourceId);
                                    Log.d(TAG, "onMessage: type resource: " + resourceId + " mResourceRequestMap: " + mResourceRequestMap + " mSubResourceDownloadList: " + mSubResourceDownloadList);

                                    if (mSubResourceDownloadList.isEmpty()) {
                                        if (!"".equals(mPendingConfigLoadId)) {
                                            notifyProfileChanged(mPendingConfigLoadId);
                                        }
                                    }
                                    break;
                                }
                                Log.d(TAG, "onMessage: new resourceDownloadInfo for resourceId: " + resourceId + " hasRequestInQueue: " + mResourceRequestMap.contains(resourceId) + " DownloadRequestInfo: " + resourceDownloadInfo);

                                mResourceRequestMap.put(resourceId, resourceDownloadInfo);
                                break;
                            case "showAlert":
                                if (mWSClientDataListener == null) return;

                                mWSClientDataListener.onShowAlert(keyCode, requestMsg.getInt("alertType"));
                                break;
                            case "showAnimation":
                                if (mWSClientDataListener == null) return;

                                mWSClientDataListener.showAnimation(keyCode, requestMsg.getString("resourceId"),
                                        requestMsg.getInt("time"));
                                break;
                            case "countdown":
                                if (mWSClientDataListener == null) return;

                                mWSClientDataListener.onShowCountDown(keyCode, requestMsg.getInt("time"));
                                break;
                            case "progress":
                                if (mWSClientDataListener == null) return;

                                mWSClientDataListener.onShowProgress(keyCode, requestMsg.getInt("percent"));
                                break;
                            case "brightness":
                                if (mWSClientDataListener == null) return;

                                mWSClientDataListener.onBrightnessChange(requestMsg.getInt("level"));
                                break;
                            case "setState" :
                                if (mWSClientDataListener == null) return;

                                mWSClientDataListener.onStateChange(keyCode, requestMsg.getInt("state"));
                                break;
                        }
                    } else {
                        ResourceDetailData resourceDetailData = (ResourceDetailData) decodeData;

                        String resourceId = resourceDetailData.getResourceId();
                        ResourceDownloadInfo resourceDownloadInfo = mResourceRequestMap.get(resourceId);
                        if (resourceDownloadInfo == null) {
                            return;
                        }

                        Log.d(TAG, "onMessage: decodedData: " + resourceDetailData.getData().length);

                        ResourceInfo currentResourceInfo = ResourceManager.getInstance().getResourceInfo(resourceId);

                        if (currentResourceInfo != null) {
                            Log.d(TAG, "onMessage: currentResourceInfo.getVersion(): " + currentResourceInfo.getVersion() + " resourceDownloadInfo.getVersion(): " + resourceDownloadInfo.getVersion());
                            Log.d(TAG, "onMessage: currentResourceInfo.getGenerateTime(): " + currentResourceInfo.getGenerateTime() + " resourceDownloadInfo.getGenerateTime(): " + resourceDownloadInfo.getGenerateTime());
                        }
                        if (currentResourceInfo != null
                                && (currentResourceInfo.getVersion() != resourceDownloadInfo.getVersion()
                                || !currentResourceInfo.getGenerateTime().equals(resourceDownloadInfo.getGenerateTime()))) {
                            ResourceManager.getInstance().deleteResource(resourceId);
                        }

                        int sequenceId = resourceDetailData.getSequenceId();
                        ResourceManager.getInstance().saveResource(resourceId, resourceDownloadInfo.getName(),
                                resourceDownloadInfo.getFileName(), resourceDetailData.getData(), sequenceId,
                                resourceDownloadInfo.getVersion(), resourceDownloadInfo.getGenerateTime());

                        if (sequenceId == 0) {
                            Log.d(TAG, "onMessage: Received ResourceInfo: " + resourceId);
                            mResourceRequestMap.remove(resourceId);

                            if (mPendingConfigLoadId.equals(resourceId)) {
                                checkForAllRelatedResources(resourceId);
                            } else {
                                mSubResourceDownloadList.remove(resourceId);

                                if (mSubResourceDownloadList.isEmpty()) {
                                    if (!"".equals(mPendingConfigLoadId)) {
                                        notifyProfileChanged(mPendingConfigLoadId);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "onMessage: error", e);
                }

            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                // 关闭连接时的处理
                Log.i(TAG, "onClosing: " + reason);
                mIsConnected = false;
                if (mWSClientDataListener != null) {
                    mWSClientDataListener.onConnectionStatusChange(false);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                // 连接失败时的处理
                Log.i(TAG, "onFailure: ", t);
                mIsConnected = false;
                if (mWSClientDataListener != null) {
                    mWSClientDataListener.onConnectionStatusChange(false);
                }
            }
        });
    }

    public WebSocketClient disconnectToServer() {
        Log.d(TAG, "disconnectToServer: ");
        if (mWebSocket == null) return this;

        mWebSocket.close(1000, "");
        mWebSocket = null;

        return this;
    }

    public boolean sendMsg(String msg) {
        if (mWebSocket == null) {
            Log.w(TAG, "sendMsg: WebSocket not connected");
            return false;
        }

        Log.d(TAG, "sendMsg: " + msg);
        return mWebSocket.send(new ByteString(wrapData(0xA1, msg, 0, "0-0", 0)));
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public void registerConnectionStatusListener(WSClientDataListener listener) {
        this.mWSClientDataListener = listener;
    }

    public void unRegisterConnectionStatusListener(WSClientDataListener listener) {
        this.mWSClientDataListener = null;
    }


    private Object checkAndDecodeData(ByteString byteString) {

        byte[] dataBytes = byteString.toByteArray();

//        Log.d(TAG, "checkAndDecodeData: byteString: " + byteString + " dataBytes.length: " + dataBytes.length + " dataBytes: " + Arrays.toString(dataBytes));
        Log.d(TAG, "checkAndDecodeData: byteString: " + byteString + " dataBytes.length: " + dataBytes.length);

        int protocolType = bytesToInt(new byte[]{(byte) (dataBytes[0])});

        Log.d(TAG, "checkAndDecodeData: protocolType: " + protocolType);

        int dataLength = bytesToInt(new byte[]{dataBytes[1], dataBytes[2]});
        Log.d(TAG, "checkAndDecodeData: dataLength: " + dataLength + " byteToLength1: " + (bytesToInt(new byte[]{dataBytes[1]})) + " byteToLength2: " + (bytesToInt(new byte[]{dataBytes[2]})));

        int sum = 0;
        for (int i = 0; i < dataLength; i++) {
            sum += (dataBytes[3 + i] & 0xFF);
        }
        int resultCheckSum = sum % 0xFF;
        int checkSum = (dataBytes[3 + dataLength] & 0xFF);

        Log.d(TAG, "checkAndDecodeData: dataCheckSum: " + checkSum + " ResultCheckSum: " + resultCheckSum + " originalByte: " + dataBytes[dataBytes.length - 1]);

        if (resultCheckSum != checkSum) {
            Log.w(TAG, "checkAndDecodeData: invalid checksum.");
            return null;
        }

        switch (protocolType & 0xFF) {
            case 0xA1:
                String dataJson = new String(Arrays.copyOfRange(dataBytes, 3, 3 + dataLength));
                Log.d(TAG, "checkAndDecodeData: getDataJson: " + dataJson);
                return dataJson;
            case 0xA2:
                int opCode = bytesToInt(new byte[]{dataBytes[3]});
                if (opCode != 0x01) break;

                int resourceIdPreId = bytesToInt(new byte[]{dataBytes[4]});
                int resourceIdEndId = bytesToInt(new byte[]{dataBytes[5], dataBytes[6], dataBytes[7], dataBytes[8]});

                int sequenceId = bytesToInt(new byte[]{dataBytes[9]});

                ResourceDetailData resourceDetailData = new ResourceDetailData();
                resourceDetailData.setResourceId(resourceIdPreId + "-" + resourceIdEndId);
                resourceDetailData.setSequenceId(sequenceId);

                byte[] data = Arrays.copyOfRange(dataBytes, 10, dataBytes.length - 1);
                resourceDetailData.setData(data);
//                Log.d(TAG, "checkAndDecodeData: get resource " + resourceDetailData.getResourceId() + " SequenceId: " + resourceDetailData.getSequenceId() + " DataLength: " + data.length + " data: " + Arrays.toString(data));
                Log.d(TAG, "checkAndDecodeData: get resource " + resourceDetailData.getResourceId() + " SequenceId: " + resourceDetailData.getSequenceId() + " DataLength: " + data.length);

                return resourceDetailData;
        }
        return null;
    }

    private byte[] wrapData(int type, Object data, int opCode, String resourceId, int sequenceId) {
        byte[] result = null;
        switch (type) {
            case 0xA1:
                String msg = (String) data;
                byte[] dataLengthInfo = intToBytes(msg.length(), 2);
                byte[] dataBytes = msg.getBytes(StandardCharsets.UTF_8);
                int dataLength = dataBytes.length;
                int fullResultLength = 1 + 2 + dataLength + 1;
                result = new byte[fullResultLength];
                result[0] = (byte) 0xA1;
                result[1] = dataLengthInfo[0];
                result[2] = dataLengthInfo[1];

                int sum = 0;
                int resultIdx = 3;
                for (byte dataByte : dataBytes) {
                    sum += (dataByte & 0xFF);
                    result[resultIdx] = dataByte;
                    resultIdx += 1;
                }

//                Log.d(TAG, "wrapData: fullResultLength: " + fullResultLength + " dataLength: " + dataLength + " checkSum: " + (sum % 0xFF) + " data: " + Arrays.toString(dataBytes));
                Log.d(TAG, "wrapData: fullResultLength: " + fullResultLength + " dataLength: " + dataLength + " checkSum: " + (sum % 0xFF));

                result[resultIdx] = (byte) (sum % 0xFF);

                break;
            case 0xA2:
                break;
        }
        return result;
    }

    private int bytesToInt(byte[] bytes) {
        if (bytes.length > 4) {
            return 0xFFFFFFFF;
        }


        int value = 0;
        for (byte aByte : bytes) {
            value = value << 8;
            value = value | (aByte & 0xFF);
        }
        return value;
    }

    private byte[] intToBytes(int value, int length) {
        if (length == -1) {
            if (value <= 0xFF) {
                return new byte[]{(byte) (value & 0xFF)};
            } else if (value <= 0xFFFF) {
                length = 2;
            } else if (value <= 0xFFFFFF) {
                length = 3;
            } else if (value == 0xFFFFFFFF) {
                return new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            } else {
                length = 4;
            }
        }
        int maxIdx = length - 1;
        byte[] byteArray = new byte[length];
        for (int i = 0; i < length; i++) {
            byteArray[i] = (byte) ((value >> ((maxIdx - i) * 8)) & 0xff);
        }

        return byteArray;
    }

    private void checkForAllRelatedResources(String resourceId) {
        ResourceInfo resourceInfo = ResourceManager.getInstance().getResourceInfo(resourceId);

        if (resourceInfo == null) {
            sendRequestResource(resourceId, 0, "");
            return;
        }

        String configInfoJson = FileUtils.readFile(resourceInfo.getPath());

        if (TextUtils.isEmpty(configInfoJson)) {
            sendRequestResource(resourceId, 0, "");
            return;
        }

        Log.d(TAG, "checkForAllRelatedResources: resourceId: " + resourceId + " resourceInfo: " + resourceInfo + " configInfoJson.Length: " + configInfoJson.length());

        Type type = new TypeToken<List<ConfigInfo>>() {
        }.getType();
        List<ConfigInfo> configInfos;
        try {
            configInfos = mGSON.fromJson(configInfoJson, type);
        } catch (Exception e) {
            ResourceManager.getInstance().deleteResource(resourceId);
            sendRequestResource(resourceId, 0, "");
            e.printStackTrace();
            return;
        }

        List<String> needCheckResourceIds = new LinkedList<>();

        for (ConfigInfo configInfo : configInfos) {
            ConfigInfo.Config config = configInfo.getConfig();
            needCheckResourceIds.addAll(getAllResourceIdsInConfig(config));

            List<ConfigInfo.Config.SubActionItem> subActions = config.getSubActions();
            if (subActions != null && subActions.size() > 0) {
                for (ConfigInfo.Config.SubActionItem subAction : subActions) {
                    needCheckResourceIds.addAll(getAllResourceIdsInConfig(subAction.getConfig()));

                }
            }

        }

        if (needCheckResourceIds.isEmpty()) {
            notifyProfileChanged(resourceId);
            return;
        }

        for (String reqResourceId : needCheckResourceIds) {
            ResourceInfo localResourceInfo = ResourceManager.getInstance().getResourceInfo(reqResourceId);
            long version = 0;
            String generateTime = "";
            if (localResourceInfo != null) {
                version = localResourceInfo.getVersion();
                generateTime = localResourceInfo.getGenerateTime();
            }
            if (!mSubResourceDownloadList.contains(reqResourceId)) {
                mSubResourceDownloadList.add(reqResourceId);
            }

            sendRequestResource(reqResourceId, version, generateTime);
        }
    }

    private void notifyProfileChanged(String resourceId) {

        Log.d(TAG, "notifyProfileChanged: ForResourceId: " + resourceId);
        ResourceInfo resourceInfo = ResourceManager.getInstance().getResourceInfo(resourceId);

        if (resourceInfo == null) return;

        String configIdx = resourceInfo.getName();
        String[] configIdxInfos = configIdx.split(",");

        mDeviceActiveProfileInfo = resourceInfo;

        SharedPreferences.Editor edit = mPreferences.edit();
        edit.putString(DEVICE_ACTIVE_PROFILE, mGSON.toJson(mDeviceActiveProfileInfo));
        edit.apply();

        mWSClientDataListener.onProfileChange(resourceId, Integer.parseInt(configIdxInfos[0]),
                Integer.parseInt(configIdxInfos[1]), Integer.parseInt(configIdxInfos[2]));
    }

    private void sendRequestResource(String resourceId, long version, String generateTime) {
        if (resourceId == null || mResourceRequestMap.contains(resourceId)) return;

        ResourceDownloadInfo resourceDownloadInfo = new ResourceDownloadInfo();
        resourceDownloadInfo.setResourceId(resourceId);
        resourceDownloadInfo.setGenerateTime(generateTime);
        resourceDownloadInfo.setVersion(version);
        resourceDownloadInfo.setHasUpdate(1);
        mResourceRequestMap.put(resourceId, resourceDownloadInfo);
        JSONObject reqResourceJsonObj = new JSONObject();
        try {
            reqResourceJsonObj.put("type", "resource");
            reqResourceJsonObj.put("resourceId", resourceId);
            reqResourceJsonObj.put("version", version);
            reqResourceJsonObj.put("generateTime", generateTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendMsg(reqResourceJsonObj.toString());
    }

    private void sendDeviceActiveProfile(String resourceId, long version, String generateTime) {
        Log.d(TAG, "sendDeviceActiveProfile: resourceId: " + resourceId + " version: " + version + " generateTime: " + generateTime);
        JSONObject reportActiveProfileJsonObj = new JSONObject();
        try {
            reportActiveProfileJsonObj.put("type", "reportActiveProfile");
            reportActiveProfileJsonObj.put("resourceId", resourceId);
            reportActiveProfileJsonObj.put("version", version);
            reportActiveProfileJsonObj.put("generateTime", generateTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendMsg(reportActiveProfileJsonObj.toString());
    }

    private void sendDeviceKeyMatrix() {

        String keyMatrixStr = Constants.KEY_MATRIX_LIST[mDeviceKeyMatrix];

        Log.d(TAG, "sendDeviceKeyMatrix: Device KeyMatrix: " + keyMatrixStr);

        String[] keyMatrixInfo = keyMatrixStr.split("x");
        if (keyMatrixInfo.length != 2) {
            Log.w(TAG, "sendDeviceKeyMatrix: Invalid Device KeyMatrix.");
            return;
        }
        int row = Integer.parseInt(keyMatrixInfo[0]);
        int col = Integer.parseInt(keyMatrixInfo[1]);

        ReportDeviceConfigInfo reportDeviceConfigInfo = new ReportDeviceConfigInfo();
        reportDeviceConfigInfo.setType("reportConfig");
        reportDeviceConfigInfo.setAppVersion(mAppVersion);

        ReportDeviceConfigInfo.KeyMatrix keyMatrix = reportDeviceConfigInfo.getKeyMatrix();
        keyMatrix.setRow(row);
        keyMatrix.setCol(col);
        reportDeviceConfigInfo.setKeyMatrix(keyMatrix);

        List<ReportDeviceConfigInfo.KeyConfig> keyConfigList = reportDeviceConfigInfo.getKeyConfig();
        for (int i = 1; i <= row; i++) {
            for (int j = 1; j <= col; j++) {
                ReportDeviceConfigInfo.KeyConfig keyConfig = new ReportDeviceConfigInfo.KeyConfig();
                keyConfig.setKeyCode(i + "," + j);
                keyConfig.setKeyType(2);
                keyConfigList.add(keyConfig);
            }
        }

        ReportDeviceConfigInfo.KeyConfig knobButton = new ReportDeviceConfigInfo.KeyConfig();
        knobButton.setKeyCode("0,1");
        knobButton.setKeyType(4);
        keyConfigList.add(knobButton);
        reportDeviceConfigInfo.setKeyConfig(keyConfigList);

        sendMsg(mGSON.toJson(reportDeviceConfigInfo));
    }

    private List<String> getAllResourceIdsInConfig(ConfigInfo.Config config) {
        List<String> resourceIds = new ArrayList<>();
        String iconResId = config.getIcon();

        if (!TextUtils.isEmpty(iconResId)) {
            resourceIds.add(iconResId);
        }

        String alterIconResId = config.getAlterIcon();
        if (!TextUtils.isEmpty(alterIconResId)) {
            resourceIds.add(alterIconResId);
        }

        List<String> configAnimations = config.getAnimations();
        if (configAnimations != null && !configAnimations.isEmpty()) {
            resourceIds.addAll(configAnimations);
        }
        return resourceIds;
    }

    public interface WSClientDataListener {
        void onConnectionStatusChange(boolean connected);

        void onProfileChange(String resourceId, int configId, int page, int folder);

        void onShowAlert(String keyCode, int type);

        void showAnimation(String keyCode, String resourceId, int time);

        void onShowProgress(String keyCode, int percent);

        void onShowCountDown(String keyCode, long timeout);

        void onBrightnessChange(int level);

        void onStateChange(String keyCode, int state);
    }

}
