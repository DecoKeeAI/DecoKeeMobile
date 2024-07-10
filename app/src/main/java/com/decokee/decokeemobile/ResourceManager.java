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
import android.text.TextUtils;
import android.util.Log;

import com.decokee.decokeemobile.bean.ConfigInfo;
import com.decokee.decokeemobile.bean.ResourceInfo;
import com.decokee.decokeemobile.utils.FileUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager {

    private static final String TAG = ResourceManager.class.getSimpleName();

    private static volatile ResourceManager sInstance;

    private static final ConcurrentHashMap<String, ResourceInfo> RESOURCE_INFO_MAP = new ConcurrentHashMap<>();

    private final Gson mGson = new Gson();
    private String mRootResourceFilePath;
    private String mResourceIdxPath;

    private final Object mResourceRWLock = new Object();

    public ResourceManager() {

    }

    public static ResourceManager getInstance() {
        if (sInstance == null) {
            synchronized (ResourceManager.class) {
                if (sInstance == null) {
                    sInstance = new ResourceManager();
                }
            }
        }
        return sInstance;
    }

    public void init(Context context) {
        mRootResourceFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "Resources";
        mResourceIdxPath = mRootResourceFilePath + File.separator + "ResourceIndex.json";
        String resourceIdxInfo = FileUtils.readFile(mResourceIdxPath);

        if (!TextUtils.isEmpty(resourceIdxInfo)) {
            Type type = new TypeToken<ConcurrentHashMap<String, ResourceInfo>>() {}.getType();
            ConcurrentHashMap<String, ResourceInfo> resourceInfos = mGson.fromJson(resourceIdxInfo, type);
            if (resourceInfos != null && !resourceInfos.isEmpty()) {
                RESOURCE_INFO_MAP.putAll(resourceInfos);
            }
        }

        Log.d(TAG, "ResourceManager: loaded resource index map: " + mGson.toJson(RESOURCE_INFO_MAP));
    }

    public boolean saveResource(String resourceId, String resourceName, String fileName, byte[] resourceData, int sequenceId, long version, String generateTime) throws IOException {
        synchronized (mResourceRWLock) {
            ResourceInfo resourceInfo = RESOURCE_INFO_MAP.get(resourceId);
            boolean isNewConfig = resourceInfo == null;
            Log.d(TAG, "saveResource: for ResourceId: " + resourceId + " isNew: " + isNewConfig);
            File resourceFile = null;
            if (isNewConfig) {
                resourceInfo = new ResourceInfo();
                resourceInfo.setId(resourceId);
                resourceInfo.setName(resourceName);
                resourceInfo.setFileName(fileName);
                String resourceFilePath = mRootResourceFilePath + File.separator + fileName;

                resourceInfo.setPath(resourceFilePath);

                resourceFile = new File(resourceFilePath);
            } else {
                resourceFile = new File(resourceInfo.getPath());
            }

            File parentFile = resourceFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            Log.d(TAG, "saveResource: for ResourceId: " + resourceId + " FileExist: " + resourceFile.exists());
            if (!resourceFile.exists()) {
                resourceFile.createNewFile();
                FileUtils.saveByteToFile(resourceFile.getAbsolutePath(), resourceData);
            } else if (sequenceId == 1 || isNewConfig) {
                FileUtils.saveByteToFile(resourceFile.getAbsolutePath(), resourceData);
            } else {
                FileUtils.appendByteToFile(resourceFile.getAbsolutePath(), resourceData);
            }

            resourceInfo.setVersion(version);

            resourceInfo.setGenerateTime(generateTime);

            RESOURCE_INFO_MAP.put(resourceId, resourceInfo);

            FileUtils.saveToFile(mResourceIdxPath, mGson.toJson(RESOURCE_INFO_MAP));
        }

        return true;
    }

    public boolean deleteResource(String resourceId) {
        synchronized (mResourceRWLock) {
            ResourceInfo resourceInfo = RESOURCE_INFO_MAP.get(resourceId);
            if (resourceInfo == null) {
                return true;
            }

            File resourceFile = new File(resourceInfo.getPath());

            Log.d(TAG, "deleteResource: for " + resourceId + " resourceInfo.getPath(): " + resourceInfo.getPath() + " Exist: " + resourceFile.exists());

            if (resourceFile.exists()) {
                boolean deleteRet = resourceFile.delete();
                Log.d(TAG, "deleteResource: for " + resourceId + " resourceInfo.getPath(): " + resourceInfo.getPath() + " deleteRet: " + deleteRet);
            }

            RESOURCE_INFO_MAP.remove(resourceId);
            FileUtils.saveToFile(mResourceIdxPath, mGson.toJson(RESOURCE_INFO_MAP));
            return true;
        }
    }

    public void cleanAllResource() {
        synchronized (mResourceRWLock) {

            File resourceFolder = new File(mRootResourceFilePath);

            FileUtils.deleteDirectory(resourceFolder);

            if (!resourceFolder.exists()) {
                resourceFolder.mkdirs();
            }

            RESOURCE_INFO_MAP.clear();
            FileUtils.saveToFile(mResourceIdxPath, mGson.toJson(RESOURCE_INFO_MAP));
        }

    }

    public ResourceInfo getResourceInfo(String resourceId) {
        synchronized (mResourceRWLock) {
            if (TextUtils.isEmpty(resourceId) || "null".equals(resourceId)) return null;
        }
        return RESOURCE_INFO_MAP.get(resourceId);
    }

    public List<ConfigInfo> getConfigInfo(String resourceId) {
        synchronized (mResourceRWLock) {
            ResourceInfo resourceInfo = getResourceInfo(resourceId);
            if (resourceInfo == null) {
                return new ArrayList<>();
            }

            String configJson = FileUtils.readFile(resourceInfo.getPath());

            Type type = new TypeToken<ArrayList<ConfigInfo>>() {
            }.getType();
            return mGson.<ArrayList<ConfigInfo>>fromJson(configJson, type);
        }
    }

}
