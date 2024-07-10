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

import java.util.List;

public class ConfigInfo {
    private String keyCode;

    private Config config;

    public String getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(String keyCode) {
        this.keyCode = keyCode;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public static class Config {
        private String type;

        private Title title;

        private Title alterTitle;

        private String icon;

        private String alterIcon;

        private List<ActionInfo> actions;

        private List<SubActionItem> subActions;

        private List<String> animations;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Title getTitle() {
            return title;
        }

        public void setTitle(Title title) {
            this.title = title;
        }

        public Title getAlterTitle() {
            return alterTitle;
        }

        public void setAlterTitle(Title alterTitle) {
            this.alterTitle = alterTitle;
        }

        public String getAlterIcon() {
            return alterIcon;
        }

        public void setAlterIcon(String alterIcon) {
            this.alterIcon = alterIcon;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public List<ActionInfo> getActions() {
            return actions;
        }

        public void setActions(List<ActionInfo> actions) {
            this.actions = actions;
        }

        public List<SubActionItem> getSubActions() {
            return subActions;
        }

        public void setSubActions(List<SubActionItem> subActions) {
            this.subActions = subActions;
        }

        public List<String> getAnimations() {
            return animations;
        }

        public void setAnimations(List<String> animations) {
            this.animations = animations;
        }

        public static class Title {
            private String text;
            private String pos;
            private int size;
            private String color;
            private String style;
            private String resourceId;
            private boolean display;

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }

            public String getPos() {
                return pos;
            }

            public void setPos(String pos) {
                this.pos = pos;
            }

            public int getSize() {
                return size;
            }

            public void setSize(int size) {
                this.size = size;
            }

            public String getColor() {
                return color;
            }

            public void setColor(String color) {
                this.color = color;
            }

            public String getStyle() {
                return style;
            }

            public void setStyle(String style) {
                this.style = style;
            }

            public String getResourceId() {
                return resourceId;
            }

            public void setResourceId(String resourceId) {
                this.resourceId = resourceId;
            }

            public boolean isDisplay() {
                return display;
            }

            public void setDisplay(boolean display) {
                this.display = display;
            }

            @Override
            public String toString() {
                return "Title{" +
                        "text='" + text + '\'' +
                        ", pos='" + pos + '\'' +
                        ", size=" + size +
                        ", color='" + color + '\'' +
                        ", style='" + style + '\'' +
                        ", resourceId='" + resourceId + '\'' +
                        ", display=" + display +
                        '}';
            }
        }

        public static class SubActionItem {

            private Config config;

            public Config getConfig() {
                return config;
            }

            public void setConfig(Config config) {
                this.config = config;
            }

            @Override
            public String toString() {
                return "SubActionItem{" +
                        "config=" + config +
                        '}';
            }
        }

        public static class ActionInfo {
            private String type;
            private String value;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return "ActionInfo{" +
                        "type='" + type + '\'' +
                        ", value='" + value + '\'' +
                        '}';
            }
        }

        @Override
        public String toString() {
            return "Config{" +
                    "config='" + type + '\'' +
                    ", title=" + title +
                    ", icon='" + icon + '\'' +
                    ", actions=" + actions +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ConfigInfo{" +
                "keyCode='" + keyCode + '\'' +
                ", config=" + config +
                '}';
    }
}
