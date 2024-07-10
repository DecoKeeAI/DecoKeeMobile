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

public class ResourceDownloadInfo {
    private String type;
    private String resourceId;
    private String name;
    private String fileName;
    private String path;
    private long version;
    private int hasUpdate;
    private String generateTime;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public int getHasUpdate() {
        return hasUpdate;
    }

    public void setHasUpdate(int hasUpdate) {
        this.hasUpdate = hasUpdate;
    }

    public String getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(String generateTime) {
        this.generateTime = generateTime;
    }

    @Override
    public String toString() {
        return "ResourceDownloadInfo{" +
                "type='" + type + '\'' +
                ", id='" + resourceId + '\'' +
                ", name='" + name + '\'' +
                ", fileName='" + fileName + '\'' +
                ", path='" + path + '\'' +
                ", version=" + version +
                ", hasUpdate=" + hasUpdate +
                ", generateTime='" + generateTime + '\'' +
                '}';
    }
}
