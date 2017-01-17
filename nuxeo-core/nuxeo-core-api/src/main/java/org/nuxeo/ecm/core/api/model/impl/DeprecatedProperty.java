/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * Property used to declare property as deprecated.
 *
 * @since 9.1
 */
public class DeprecatedProperty extends AbstractProperty {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DeprecatedProperty.class);

    protected final Property deprecated;

    protected final Property fallback;

    public DeprecatedProperty(Property deprecated) {
        this(deprecated, null);
    }

    public DeprecatedProperty(Property deprecated, Property fallback) {
        this(deprecated.getParent(), deprecated, fallback);
    }

    private DeprecatedProperty(Property parent, Property deprecated, Property fallback) {
        super(parent);
        this.deprecated = deprecated;
        this.fallback = fallback;
    }

    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
        StringBuilder msg = newDeprecatedMessage();
        msg.append("Set value to deprecated property");
        if (fallback != null) {
            msg.append(" and to fallback property '").append(fallback.getPath()).append("'");
        }
        if (log.isTraceEnabled()) {
            log.warn(msg, new NuxeoException("debug stack trace"));
        } else {
            log.warn(msg);
        }
        deprecated.setValue(value);
        if (fallback != null) {
            fallback.setValue(value);
        }
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        StringBuilder msg = newDeprecatedMessage();
        if (fallback == null) {
            msg.append("Return value from deprecated property");
        } else {
            msg.append("Return value from '")
               .append(fallback.getPath())
               .append("' if not null, from deprecated property otherwise");
        }
        if (log.isTraceEnabled()) {
            log.warn(msg, new NuxeoException());
        } else {
            log.warn(msg);
        }
        Serializable deprecatedValue = deprecated.getValue();
        if (fallback == null) {
            return deprecatedValue;
        }
        Serializable fallbackValue = fallback.getValue();
        return fallbackValue == null ? deprecatedValue : fallbackValue;
    }

    protected StringBuilder newDeprecatedMessage() {
        StringBuilder builder = new StringBuilder().append("Property '")
                                                   .append(getPath())
                                                   .append("' is marked as deprecated from '")
                                                   .append(getSchema().getName())
                                                   .append("' schema");
        DeprecatedProperty deprecatedParent = getDeprecatedParent();
        if (deprecatedParent != this) {
            builder.append(" because property '")
                   .append(deprecatedParent.getPath())
                   .append("' is marked as deprecated");
        }
        return builder.append(", don't use it anymore.\n");
    }

    protected DeprecatedProperty getDeprecatedParent() {
        DeprecatedProperty property = this;
        while (property.getParent() instanceof DeprecatedProperty) {
            property = (DeprecatedProperty) property.getParent();
        }
        return property;
    }

    @Override
    public boolean isContainer() {
        return deprecated.isContainer();
    }

    @Override
    public String getName() {
        return deprecated.getName();
    }

    @Override
    public Type getType() {
        return deprecated.getType();
    }

    @Override
    public Field getField() {
        return deprecated.getField();
    }

    @Override
    public Property get(String name) throws PropertyNotFoundException {
        return get(prop -> prop.get(name));
    }

    @Override
    public Property get(int index) throws PropertyNotFoundException {
        return get(prop -> prop.get(index));
    }

    private Property get(Function<Property, Property> getter) throws PropertyNotFoundException {
        Property child = getter.apply(deprecated);
        if (child instanceof DeprecatedProperty) {
            return child;
        }
        Property childFallback = fallback == null ? null : getter.apply(fallback);
        return new DeprecatedProperty(this, child, childFallback);
    }

    @Override
    public Collection<Property> getChildren() {
        Collection<Property> children = deprecated.getChildren();
        List<Property> result = new ArrayList<>();
        for (Property child : children) {
            if (child instanceof DeprecatedProperty) {
                result.add(child);
            } else if (fallback == null) {
                result.add(new DeprecatedProperty(this, child));
            } else {
                result.add(new DeprecatedProperty(this, child, fallback.get(child.getName())));
            }
        }
        return result;
    }

    @Override
    public Property addValue(Object value) throws PropertyException {
        Property property = deprecated.addValue(value);
        if (fallback == null) {
            return new DeprecatedProperty(this, property);
        }
        return new DeprecatedProperty(this, property, fallback.addValue(value));
    }

    @Override
    public Property addValue(int index, Object value) throws PropertyException {
        Property property = deprecated.addValue(index, value);
        if (fallback == null) {
            return new DeprecatedProperty(this, property);
        }
        return new DeprecatedProperty(this, property, fallback.addValue(index, value));
    }

    @Override
    public Property addEmpty() throws PropertyException {
        Property property = deprecated.addEmpty();
        if (fallback == null) {
            return new DeprecatedProperty(this, property);
        }
        return new DeprecatedProperty(this, property, fallback.addEmpty());
    }

    @Override
    public void accept(PropertyVisitor visitor, Object arg) throws PropertyException {
        deprecated.accept(visitor, arg);
    }

    @Override
    public boolean isSameAs(Property property) throws PropertyException {
        if (!(property instanceof DeprecatedProperty)) {
            return false;
        }
        DeprecatedProperty rp = (DeprecatedProperty) property;
        return Objects.equals(deprecated, rp.deprecated) && Objects.equals(fallback, rp.fallback);
    }

    @Override
    public Iterator<Property> getDirtyChildren() {
        return deprecated.getDirtyChildren();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getPath().substring(1) + '=' + deprecated + ")";
    }

}
