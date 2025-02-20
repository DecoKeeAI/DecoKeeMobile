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
package com.decokee.decokeemobile.bean;

public class ResourceInfo {
    private String mId;
    private String mName;
    private String fileName;
    private String mPath;
    private long mVersion;
    private String mGenerateTime;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public long getVersion() {
        return mVersion;
    }

    public void setVersion(long version) {
        mVersion = version;
    }

    public String getGenerateTime() {
        return mGenerateTime;
    }

    public void setGenerateTime(String generateTime) {
        mGenerateTime = generateTime;
    }

    @Override
    public String toString() {
        return "ResourceInfo{" +
                "mId='" + mId + '\'' +
                ", mName='" + mName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", mPath='" + mPath + '\'' +
                ", mVersion=" + mVersion +
                ", mGenerateTime='" + mGenerateTime + '\'' +
                '}';
    }
}
