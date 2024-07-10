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

import java.util.ArrayList;
import java.util.List;

public class ReportDeviceConfigInfo {
    private String type;
    private String appVersion;
    private KeyMatrix keyMatrix = new KeyMatrix();
    private List<KeyConfig> keyConfig = new ArrayList<>();

    public static class KeyMatrix {
        private int row = 0;
        private int col = 0;

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getCol() {
            return col;
        }

        public void setCol(int col) {
            this.col = col;
        }

        @Override
        public String toString() {
            return "KeyMatrix{" +
                    "row=" + row +
                    ", col=" + col +
                    '}';
        }
    }

    public static class KeyConfig {
        private String keyCode;
        private int keyType;

        public String getKeyCode() {
            return keyCode;
        }

        public void setKeyCode(String keyCode) {
            this.keyCode = keyCode;
        }

        public int getKeyType() {
            return keyType;
        }

        public void setKeyType(int keyType) {
            this.keyType = keyType;
        }

        @Override
        public String toString() {
            return "KeyConfig{" +
                    "keyCode='" + keyCode + '\'' +
                    ", keyType=" + keyType +
                    '}';
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public KeyMatrix getKeyMatrix() {
        return keyMatrix;
    }

    public void setKeyMatrix(KeyMatrix keyMatrix) {
        this.keyMatrix = keyMatrix;
    }

    public List<KeyConfig> getKeyConfig() {
        return keyConfig;
    }

    public void setKeyConfig(List<KeyConfig> keyConfig) {
        this.keyConfig = keyConfig;
    }

    @Override
    public String toString() {
        return "ReportDeviceConfigInfo{" +
                "type='" + type + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", keyMatrix=" + keyMatrix +
                ", keyConfig=" + keyConfig +
                '}';
    }
}
