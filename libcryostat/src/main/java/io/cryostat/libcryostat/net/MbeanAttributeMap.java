/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.libcryostat.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MbeanAttributeMap {

    private String mBeanName;
    private List<MBeanAttribute> attributes;

    public MbeanAttributeMap(String name, List<MBeanAttribute> attrs) {
        this.mBeanName = name;
        this.attributes = new ArrayList<MBeanAttribute>(attrs);
    }

    // Default Constructor for ObjectMapper deserialization
    public MbeanAttributeMap() {
        this("", Collections.emptyList());
    }

    public String getMbeanName() {
        return mBeanName;
    }

    public List<MBeanAttribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public void setMbeanName(String mbeanName) {
        this.mBeanName = mbeanName;
    }

    public void setAttributes(List<MBeanAttribute> attributes) {
        this.attributes = new ArrayList<MBeanAttribute>(attributes);
    }

    public static class MBeanAttribute {

        private String name;
        private String type;
        private String description;
        private String parentBean;
        private boolean isReadable;
        private boolean isWritable;

        public MBeanAttribute(
                String name,
                String type,
                String description,
                String parentBean,
                boolean isReadable,
                boolean isWritable) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.parentBean = parentBean;
            this.isReadable = isReadable;
            this.isWritable = isWritable;
        }

        // Default constructor for ObjectMapper deserialization
        public MBeanAttribute() {
            this("", "", "", "", false, false);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getParentBean() {
            return parentBean;
        }

        public void setParentBean(String parentBean) {
            this.parentBean = parentBean;
        }

        public boolean isReadable() {
            return isReadable;
        }

        public void setIsReadable(boolean isReadable) {
            this.isReadable = isReadable;
        }

        public boolean isWritable() {
            return isWritable;
        }

        public void setIsWritable(boolean isWritable) {
            this.isWritable = isWritable;
        }
    }
}
