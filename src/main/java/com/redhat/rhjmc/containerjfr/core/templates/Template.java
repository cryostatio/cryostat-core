package com.redhat.rhjmc.containerjfr.core.templates;

import java.util.Objects;

import org.apache.commons.lang.builder.ToStringBuilder;

public class Template {

    private final String name;
    private final String description;
    private final String provider;

    public Template(String name, String description, String provider) {
        this.name = name;
        this.description = description;
        this.provider = provider;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("description", description)
                .append("provider", provider)
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
                && Objects.equals(getProvider(), other.getProvider());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, provider);
    }
}
