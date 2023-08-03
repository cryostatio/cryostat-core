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
package io.cryostat.core.templates;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Template {

    private final String name;
    private final String description;
    private final String provider;
    private final TemplateType type;

    public Template(String name, String description, String provider, TemplateType type) {
        this.name = name;
        this.description = description;
        this.provider = provider;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getProvider() {
        return provider;
    }

    public TemplateType getType() {
        return type;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("description", description)
                .append("provider", provider)
                .append("type", type)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o.getClass() != getClass()) {
            return false;
        }
        Template other = (Template) o;
        return Objects.equals(getName(), other.getName())
                && Objects.equals(getDescription(), other.getDescription())
                && Objects.equals(getProvider(), other.getProvider())
                && Objects.equals(getType(), other.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, provider, type);
    }
}
