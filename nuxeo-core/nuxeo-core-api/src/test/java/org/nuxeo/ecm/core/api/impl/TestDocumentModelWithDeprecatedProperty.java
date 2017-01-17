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
package org.nuxeo.ecm.core.api.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.NoLogCaptureFilterException;

/**
 * @since 9.1
 */
@Features({ LogCaptureFeature.class })
@LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = TestDocumentModelWithDeprecatedProperty.LOGGER_NAME)
public class TestDocumentModelWithDeprecatedProperty extends NXRuntimeTestCase {

    public static final String LOGGER_NAME = "org.nuxeo.ecm.core.api.model.impl.DeprecatedProperty";

    private static final String GET_VALUE_LOG = "Property '%s' is marked as deprecated from '%s' schema, don't use it anymore.\n"
            + "Return value from deprecated property";

    private static final String SET_VALUE_LOG = "Property '%s' is marked as deprecated from '%s' schema, don't use it anymore.\n"
            + "Set value to deprecated property";

    private static final String GET_VALUE_FALLBACK_LOG = "Property '%s' is marked as deprecated from '%s' schema, don't use it anymore.\n"
            + "Return value from '%s' if not null, from deprecated property otherwise";

    private static final String SET_VALUE_FALLBACK_LOG = "Property '%s' is marked as deprecated from '%s' schema, don't use it anymore.\n"
            + "Set value to deprecated property and to fallback property '%s'";

    private static final String GET_VALUE_PARENT_LOG = "Property '%s' is marked as deprecated from '%s' schema because property '%s' is marked as deprecated, don't use it anymore.\n"
            + "Return value from deprecated property";

    private static final String SET_VALUE_PARENT_LOG = "Property '%s' is marked as deprecated from '%s' schema because property '%s' is marked as deprecated, don't use it anymore.\n"
            + "Set value to deprecated property";

    private static final String GET_VALUE_FALLBACK_PARENT_LOG = "Property '%s' is marked as deprecated from '%s' schema because property '%s' is marked as deprecated, don't use it anymore.\n"
            + "Return value from '%s' if not null, from deprecated property otherwise";

    private static final String SET_VALUE_FALLBACK_PARENT_LOG = "Property '%s' is marked as deprecated from '%s' schema because property '%s' is marked as deprecated, don't use it anymore.\n"
            + "Set value to deprecated property and to fallback property '%s'";

    @Inject
    private LogCaptureFeature.Result logCaptureResult;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-documentmodel-deprecated-types-contrib.xml");
    }

    // --------------------------------
    // Tests with deprecated properties
    // --------------------------------

    @Test
    public void testSetDeprecatedScalarProperty() throws Exception {
        testProperty("scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarPropertyValue() throws Exception {
        testPropertyValue("scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarProperties() throws Exception {
        testProperties("scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarArrayProperty() throws Exception {
        testProperty("scalars", new String[] { "test scalar", "test scalar2" });
    }

    @Test
    public void testSetDeprecatedScalarArrayPropertyValue() throws Exception {
        testPropertyValue("scalars", new String[] { "test scalar", "test scalar2" });
    }

    @Test
    public void testSetDeprecatedScalarArrayProperties() throws Exception {
        testProperties("scalars", new String[] { "test scalar", "test scalar2" });
    }

    @Test
    public void testSetDeprecatedComplexProperty() throws Exception {
        testProperty("complexDep", Collections.singletonMap("scalar", "test scalar"));
    }

    @Test
    public void testSetDeprecatedComplexPropertyValue() throws Exception {
        testPropertyValue("complexDep", (Serializable) Collections.singletonMap("scalar", "test scalar"));
    }

    @Test
    public void testSetDeprecatedComplexProperties() throws Exception {
        testProperties("complexDep", Collections.singletonMap("scalar", "test scalar"));
    }

    @Test
    public void testSetScalarOnDeprecatedComplexProperty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "complexDep/scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("deprecated", "complexDep/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_PARENT_LOG, "/complexDep/scalar", "deprecated", "/complexDep"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_PARENT_LOG, "/complexDep/scalar", "deprecated", "/complexDep"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_PARENT_LOG, "/complexDep/scalar", "deprecated", "/complexDep"),
                events.get(2).getRenderedMessage());
    }

    @Test
    public void testSetScalarOnDeprecatedComplexPropertyValue() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:complexDep/scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complexDep/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_PARENT_LOG, "/complexDep/scalar", "deprecated", "/complexDep"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_PARENT_LOG, "/complexDep/scalar", "deprecated", "/complexDep"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_PARENT_LOG, "/complexDep/scalar", "deprecated", "/complexDep"),
                events.get(2).getRenderedMessage());
    }

    @Test
    public void testSetDeprecatedScalarOnComplexProperty() throws Exception {
        testProperty("complex/scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarOnComplexPropertyValue() throws Exception {
        testPropertyValue("complex/scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarOnComplexProperties() throws Exception {
        testProperties("complex/scalar", "test scalar");
    }

    // ----------------------------------------------
    // Tests with deprecated properties with fallback
    // ----------------------------------------------

    @Test
    public void testSetDeprecatedScalarPropertyWithFallbackOnScalar() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "scalar2scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("deprecated", "scalar2scalar"));
        assertEquals("test scalar", doc.getProperty("deprecated", "scalarfallback"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(2).getRenderedMessage());
    }

    @Test
    public void testSetDeprecatedScalarPropertyValueWithFallbackOnScalar() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:scalar2scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:scalar2scalar"));
        assertEquals("test scalar", doc.getPropertyValue("deprecated:scalarfallback"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(2).getRenderedMessage());
    }

    @Test
    public void testSetDeprecatedScalarPropertiesWithFallbackOnScalar() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated", Collections.singletonMap("scalar2scalar", "test scalar"));
        Map<String, Object> properties = doc.getProperties("deprecated");
        assertEquals("test scalar", properties.get("scalar2scalar").toString());
        assertEquals("test scalar", properties.get("scalarfallback").toString());

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 8 logs:
        // - a set is basically one get then one set if value are different
        // - one log for each of deprecated properties as we get a map of whole properties: here 6 deprecated properties
        // note that there's another deprecated property /complex/scalar but as it doesn't exist we don't get and log it
        assertEquals(8, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, "/complexDep", "deprecated"), events.get(2).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, "/scalar", "deprecated"), events.get(3).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, "/scalars", "deprecated"), events.get(4).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(5).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(6).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/complex2complex", "deprecated", "/complexfallback"),
                events.get(7).getRenderedMessage());
    }

    @Test
    public void testSetDeprecatedScalarPropertyWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "scalar2complex", "test scalar");
        assertEquals("test scalar", doc.getProperty("deprecated", "scalar2complex"));
        assertEquals("test scalar", doc.getProperty("deprecated", "complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(2).getRenderedMessage());
    }

    @Test
    public void testSetDeprecatedScalarPropertyValueWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:scalar2complex", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:scalar2complex"));
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetDeprecatedScalarPropertiesWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated", Collections.singletonMap("scalar2complex", "test scalar"));
        Map<String, Object> properties = doc.getProperties("deprecated");
        assertEquals("test scalar", properties.get("scalar2complex").toString());
        assertEquals("test scalar",
                ((Map<String, Serializable>) properties.get("complexfallback")).get("scalar").toString());

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 8 logs:
        // - a set is basically one get then one set if value are different
        // - one log for each of deprecated properties as we get a map of whole properties: here 6 deprecated properties
        // note that there's another deprecated property /complex/scalar but as it doesn't exist we don't get and log it
        assertEquals(8, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, "/complexDep", "deprecated"), events.get(2).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, "/scalar", "deprecated"), events.get(3).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, "/scalars", "deprecated"), events.get(4).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(5).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(6).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/complex2complex", "deprecated", "/complexfallback"),
                events.get(7).getRenderedMessage());
    }

    @Test
    public void testSetDeprecatedComplexPropertyWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "complex2complex", Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperty("deprecated", "complex2complex/scalar"));
        assertEquals("test scalar", doc.getProperty("deprecated", "complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/complex2complex", "deprecated", "/complexfallback"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, "/complex2complex", "deprecated", "/complexfallback"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_PARENT_LOG, "/complex2complex/scalar", "deprecated",
                "/complex2complex", "/complexfallback/scalar"), events.get(2).getRenderedMessage());
    }

    @Test
    public void testSetDeprecatedComplexPropertyValueWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:complex2complex",
                (Serializable) Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complex2complex/scalar"));
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/complex2complex", "deprecated", "/complexfallback"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, "/complex2complex", "deprecated", "/complexfallback"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_PARENT_LOG, "/complex2complex/scalar", "deprecated",
                "/complex2complex", "/complexfallback/scalar"), events.get(2).getRenderedMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetDeprecatedComplexPropertiesWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated",
                Collections.singletonMap("complex2complex", Collections.singletonMap("scalar", "test scalar")));
        Map<String, Object> properties = doc.getProperties("deprecated");
        assertEquals("test scalar",
                ((Map<String, Serializable>) properties.get("complex2complex")).get("scalar").toString());
        assertEquals("test scalar",
                ((Map<String, Serializable>) properties.get("complexfallback")).get("scalar").toString());

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 8 logs:
        // - a set is basically one get then one set if value are different
        // - one log for each of deprecated properties as we get a map of whole properties: here 6 deprecated properties
        // note that there's another deprecated property /complex/scalar but as it doesn't exist we don't get and log it
        assertEquals(8, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/complex2complex", "deprecated", "/complexfallback"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, "/complex2complex", "deprecated", "/complexfallback"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, "/complexDep", "deprecated"), events.get(2).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, "/scalar", "deprecated"), events.get(3).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, "/scalars", "deprecated"), events.get(4).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar"),
                events.get(5).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback"),
                events.get(6).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, "/complex2complex", "deprecated", "/complexfallback"),
                events.get(7).getRenderedMessage());
    }

    @Test
    public void testSetScalarOnDeprecatedComplexPropertyWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "complex2complex/scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("deprecated", "complex2complex/scalar"));
        assertEquals("test scalar", doc.getProperty("deprecated", "complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_PARENT_LOG, "/complex2complex/scalar", "deprecated",
                "/complex2complex", "/complexfallback/scalar"), events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_PARENT_LOG, "/complex2complex/scalar", "deprecated",
                "/complex2complex", "/complexfallback/scalar"), events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_PARENT_LOG, "/complex2complex/scalar", "deprecated",
                "/complex2complex", "/complexfallback/scalar"), events.get(2).getRenderedMessage());
    }

    @Test
    public void testSetScalarOnDeprecatedComplexPropertyValueWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:complex2complex/scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complex2complex/scalar"));
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_FALLBACK_PARENT_LOG, "/complex2complex/scalar", "deprecated",
                "/complex2complex", "/complexfallback/scalar"), events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_FALLBACK_PARENT_LOG, "/complex2complex/scalar", "deprecated",
                "/complex2complex", "/complexfallback/scalar"), events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_PARENT_LOG, "/complex2complex/scalar", "deprecated",
                "/complex2complex", "/complexfallback/scalar"), events.get(2).getRenderedMessage());
    }

    /**
     * @param deprecatedProperty deprecated property path to test
     * @param value the value to set, depending on property field type
     */
    private void testProperty(String deprecatedProperty, Object value) throws NoLogCaptureFilterException {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", deprecatedProperty, value);
        // As property is just deprecated we still store the value
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) doc.getProperty("deprecated", deprecatedProperty));
        } else {
            assertEquals(value, doc.getProperty("deprecated", deprecatedProperty));
        }

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        String path = "/" + deprecatedProperty;
        assertEquals(String.format(GET_VALUE_LOG, path, "deprecated"), events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_LOG, path, "deprecated"), events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, path, "deprecated"), events.get(2).getRenderedMessage());
    }

    /**
     * @param deprecatedProperty deprecated property path to test
     * @param value the value to set, depending on property field type
     */
    private void testPropertyValue(String deprecatedProperty, Serializable value) throws NoLogCaptureFilterException {
        String xpath = "deprecated:" + deprecatedProperty;

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(xpath, value);
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) doc.getPropertyValue(xpath));
        } else {
            assertEquals(value, doc.getPropertyValue(xpath));
        }

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        String path = "/" + deprecatedProperty;
        assertEquals(String.format(GET_VALUE_LOG, path, "deprecated"), events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_LOG, path, "deprecated"), events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, path, "deprecated"), events.get(2).getRenderedMessage());
        logCaptureResult.clear();

        // Test also with a xpath without schema
        doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(deprecatedProperty, value);
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) doc.getProperty(xpath).getValue());
        } else {
            assertEquals(value, doc.getProperty(xpath).getValue());
        }

        logCaptureResult.assertHasEvent();
        events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_LOG, path, "deprecated"), events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_LOG, path, "deprecated"), events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, path, "deprecated"), events.get(2).getRenderedMessage());
    }

    /**
     * @param deprecatedProperty deprecated property path to test
     * @param value the value to set, depending on property field type
     */
    @SuppressWarnings("unchecked")
    private void testProperties(String deprecatedProperty, Object value) throws NoLogCaptureFilterException {
        Map<String, Object> map;
        // We define that deprecated property is the last property in path
        int i = deprecatedProperty.indexOf('/');
        boolean hasParent = i != -1;
        String parent = hasParent ? deprecatedProperty.substring(0, i) : null;
        String child = hasParent ? deprecatedProperty.substring(i + 1) : null;
        if (hasParent) {
            map = Collections.singletonMap(parent, Collections.singletonMap(child, value));
        } else {
            map = Collections.singletonMap(deprecatedProperty, value);
        }

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated", map);
        Object actualValue;
        if (hasParent) {
            actualValue = ((Map<String, Serializable>) doc.getProperties("deprecated").get(parent)).get(child);
        } else {
            actualValue = doc.getProperties("deprecated").get(deprecatedProperty);
        }
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) actualValue);
        } else {
            assertEquals(value, actualValue);
        }

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // if deprecated property is a child of a complex element there's 9 logs, unless 8 logs:
        // - a set is basically one get then one set if value are different
        // - one log for each of deprecated properties as we get a map of whole properties: here 7 or 6 deprecated
        // properties
        // The deprecated property /complex/scalar appears in logs only when we set it, because unless we don't create
        // values in /complex and so no log when retrieving all properties
        assertEquals(hasParent ? 9 : 8, events.size());
        String path = "/" + deprecatedProperty;
        assertEquals(String.format(GET_VALUE_LOG, path, "deprecated"), events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_LOG, path, "deprecated"), events.get(1).getRenderedMessage());
        // Next logs have not same order depending on the test
        List<String> eventMsgs = events.stream()
                                       .skip(2)
                                       .map(LoggingEvent::getRenderedMessage)
                                       .collect(Collectors.toList());
        assertTrue(eventMsgs.remove(String.format(GET_VALUE_LOG, "/complexDep", "deprecated")));
        assertTrue(eventMsgs.remove(String.format(GET_VALUE_LOG, "/scalar", "deprecated")));
        assertTrue(eventMsgs.remove(String.format(GET_VALUE_LOG, "/scalars", "deprecated")));
        assertTrue(eventMsgs.remove(
                String.format(GET_VALUE_FALLBACK_LOG, "/scalar2complex", "deprecated", "/complexfallback/scalar")));
        assertTrue(eventMsgs.remove(
                String.format(GET_VALUE_FALLBACK_LOG, "/scalar2scalar", "deprecated", "/scalarfallback")));
        assertTrue(eventMsgs.remove(
                String.format(GET_VALUE_FALLBACK_LOG, "/complex2complex", "deprecated", "/complexfallback")));
        if (hasParent) {
            assertTrue(eventMsgs.remove(String.format(GET_VALUE_LOG, "/complex/scalar", "deprecated")));
        }
        assertTrue(eventMsgs.isEmpty());
    }

}
