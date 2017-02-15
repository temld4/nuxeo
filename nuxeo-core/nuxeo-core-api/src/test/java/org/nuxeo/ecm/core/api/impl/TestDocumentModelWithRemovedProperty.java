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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.NoLogCaptureFilterException;

/**
 * @since 9.1
 */
@Features({ LogCaptureFeature.class })
@LogCaptureFeature.FilterOn(logLevel = "ERROR", loggerName = TestDocumentModelWithRemovedProperty.LOGGER_NAME)
public class TestDocumentModelWithRemovedProperty extends NXRuntimeTestCase {

    public static final String LOGGER_NAME = "org.nuxeo.ecm.core.api.model.impl.RemovedProperty";

    private static final String GET_VALUE_LOG = "Property '%s' is marked as removed from '%s' schema, don't use it anymore.\n"
            + "Return null";

    private static final String SET_VALUE_LOG = "Property '%s' is marked as removed from '%s' schema, don't use it anymore.\n"
            + "Do nothing";

    private static final String GET_VALUE_FALLBACK_LOG = "Property '%s' is marked as removed from '%s' schema, don't use it anymore.\n"
            + "Return value from '%s' if not null, from removed property otherwise";

    private static final String SET_VALUE_FALLBACK_LOG = "Property '%s' is marked as removed from '%s' schema, don't use it anymore.\n"
            + "Set value to removed property and to fallback property '%s'";

    private static final String GET_VALUE_PARENT_LOG = "Property '%s' is marked as removed from '%s' schema because property '%s' is marked as removed, don't use it anymore.\n"
            + "Return value from removed property";

    private static final String SET_VALUE_PARENT_LOG = "Property '%s' is marked as removed from '%s' schema because property '%s' is marked as removed, don't use it anymore.\n"
            + "Set value to removed property";

    private static final String GET_VALUE_FALLBACK_PARENT_LOG = "Property '%s' is marked as removed from '%s' schema because property '%s' is marked as removed, don't use it anymore.\n"
            + "Return value from '%s' if not null, from removed property otherwise";

    private static final String SET_VALUE_FALLBACK_PARENT_LOG = "Property '%s' is marked as removed from '%s' schema because property '%s' is marked as removed, don't use it anymore.\n"
            + "Set value to removed property and to fallback property '%s'";

    @Inject
    private LogCaptureFeature.Result logCaptureResult;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-documentmodel-removed-types-contrib.xml");
    }

    // -----------------------------
    // Tests with removed properties
    // -----------------------------

    @Test
    public void testSetRemovedScalarProperty() throws Exception {
        testProperty("scalar", "test scalar");
    }

    @Test
    public void testSetRemovedScalarPropertyValue() throws Exception {
        testPropertyValue("scalar", "test scalar");
    }

    @Test
    public void testSetRemovedScalarProperties() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed", Collections.singletonMap("scalar", "test scalar"));
        assertNull(doc.getProperties("removed").get("scalar"));
    }

    @Test
    public void testSetRemovedComplexProperty() throws Exception {
        testProperty("complexRem", Collections.singletonMap("scalar", "test scalar"));
    }

    @Test
    public void testSetRemovedComplexPropertyValue() throws Exception {
        testPropertyValue("complexRem", (Serializable) Collections.singletonMap("scalar", "test scalar"));
    }

    @Test
    public void testSetRemovedComplexProperties() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed",
                Collections.singletonMap("complexRem", Collections.singletonMap("scalar", "test scalar")));
        assertNull(doc.getProperties("removed").get("complexRem"));
    }

    @Test
    public void testSetScalarOnRemovedComplexProperty() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "complexRem/scalar", "test scalar");
        assertNull(doc.getProperty("removed", "complexRem/scalar"));
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyValue() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:complexRem/scalar", "test scalar");
        assertNull(doc.getPropertyValue("removed:complexRem/scalar"));
    }

    @Test
    public void testSetRemovedScalarOnComplexProperty() throws Exception {
        testProperty("complex/scalar", "test scalar");
    }

    @Test
    public void testSetRemovedScalarOnComplexPropertyValue() throws Exception {
        testPropertyValue("complex/scalar", "test scalar");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetRemovedScalarOnComplexProperties() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed",
                Collections.singletonMap("complex", Collections.singletonMap("scalar", "test scalar")));
        assertNull(((Map<String, Serializable>) doc.getProperties("removed").get("complex")).get("scalar"));
    }

    @Test
    public void testSetComplexPropertyRemovedFromSchemaWithoutRemovedContribution() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // First test: if we try to set a property deleted from schema without contribution still leads to error
        try {
            doc.setProperty("removed", "deleted", null);
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("deleted", pnfe.getPath());
        }
        // Second test: the get
        try {
            doc.getProperty("removed", "deleted");
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("deleted", pnfe.getPath());
        }
        // Third test: set a property deleted from schema whose the last segment has a contribution to Removed
        // Properties, this has to lead to an error (issue faced during development)
        try {
            doc.setProperty("removed", "complexfallback/complexRem", null);
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("complexfallback/complexRem", pnfe.getPath());
        }
        // Fourth test: the get in the same way
        try {
            doc.getProperty("removed", "complexfallback/complexRem");
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("complexfallback/complexRem", pnfe.getPath());
        }
    }

    @Test
    public void testSetComplexPropertyValueRemovedFromSchemaWithoutRemovedContribution() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // First test: if we try to set a property deleted from schema without contribution still leads to error
        try {
            doc.setPropertyValue("removed:deleted", null);
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("removed:deleted", pnfe.getPath());
        }
        // Second test: the get
        try {
            doc.getPropertyValue("removed:deleted");
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("removed:deleted", pnfe.getPath());
        }
        // Third test: set a property deleted from schema whose the last segment has a contribution to Removed
        // Properties, this has to lead to an error (issue faced during development)
        try {
            doc.setPropertyValue("removed:complexfallback/complexRem", null);
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("removed:complexfallback/complexRem", pnfe.getPath());
        }
        // Fourth test: the get in the same way
        try {
            doc.getPropertyValue("removed:complexfallback/complexRem");
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("removed:complexfallback/complexRem", pnfe.getPath());
        }
    }

    // -------------------------------------------
    // Tests with removed properties with fallback
    // -------------------------------------------

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnScalar() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "scalar2scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("removed", "scalar2scalar"));
        assertEquals("test scalar", doc.getProperty("removed", "scalarfallback"));
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnScalar() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:scalar2scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("removed:scalar2scalar"));
        assertEquals("test scalar", doc.getPropertyValue("removed:scalarfallback"));
    }

    @Test
    public void testSetRemovedScalarPropertiesWithFallbackOnScalar() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed", Collections.singletonMap("scalar2scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperties("removed").get("scalarfallback").toString());
    }

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "scalar2complex", "test scalar");
        assertEquals("test scalar", doc.getProperty("removed", "scalar2complex"));
        assertEquals("test scalar", doc.getProperty("removed", "complexfallback/scalar"));
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:scalar2complex", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("removed:scalar2complex"));
        assertEquals("test scalar", doc.getPropertyValue("removed:complexfallback/scalar"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetRemovedScalarPropertiesWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed", Collections.singletonMap("scalar2complex", "test scalar"));
        assertEquals("test scalar",
                ((Map<String, Serializable>) doc.getProperties("removed").get("complexfallback")).get("scalar")
                                                                                                 .toString());
    }

    @Test
    public void testSetRemovedComplexPropertyWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "complex2complex", Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperty("removed", "complex2complex/scalar"));
        assertEquals("test scalar", doc.getProperty("removed", "complexfallback/scalar"));
    }

    @Test
    public void testSetRemovedComplexPropertyValueWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:complex2complex",
                (Serializable) Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getPropertyValue("removed:complex2complex/scalar"));
        assertEquals("test scalar", doc.getPropertyValue("removed:complexfallback/scalar"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetRemovedComplexPropertiesWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed",
                Collections.singletonMap("complex2complex", Collections.singletonMap("scalar", "test scalar")));
        assertEquals("test scalar",
                ((Map<String, Serializable>) doc.getProperties("removed").get("complexfallback")).get("scalar")
                                                                                                 .toString());
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "complex2complex/scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("removed", "complex2complex/scalar"));
        assertEquals("test scalar", doc.getProperty("removed", "complexfallback/scalar"));
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyValueWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:complex2complex/scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("removed:complex2complex/scalar"));
        assertEquals("test scalar", doc.getPropertyValue("removed:complexfallback/scalar"));
    }

    @Test
    public void testSetRemovedScalarOnListPropertyWithFallbackInsideList() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // First create a valid object
        doc.setProperty("removed", "list",
                Collections.singletonList(Collections.singletonMap("scalar", "test scalar")));
        // Second try to set the removed property inside the item
        doc.setProperty("removed", "list/0/renamed", "test scalar 2");
        assertEquals("test scalar 2", doc.getProperty("removed", "list/0/scalar"));
        assertEquals("test scalar 2", doc.getProperty("removed", "list/0/renamed"));
    }

    @Test
    public void testSetRemovedScalarOnListPropertyValueWithFallbackInsideList() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // First create a valid object
        doc.setProperty("removed", "list",
                Collections.singletonList(Collections.singletonMap("scalar", "test scalar")));
        // Second try to set the removed property inside the item
        doc.setPropertyValue("removed:list/0/renamed", "test scalar 2");
        assertEquals("test scalar 2", doc.getPropertyValue("removed:list/0/scalar"));
        assertEquals("test scalar 2", doc.getPropertyValue("removed:list/0/renamed"));
    }

    @Test
    public void testSetRemovedScalarOnListPropertiesWithFallbackInsideList() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed", Collections.singletonMap("list",
                Collections.singletonList(Collections.singletonMap("renamed", "test scalar"))));
        assertEquals("test scalar", doc.getPropertyValue("removed:list/0/scalar"));
        assertEquals("test scalar", doc.getPropertyValue("removed:list/0/renamed"));
    }

    // -------------------------------------
    // Tests with removed properties on blob
    // -------------------------------------

    @Test
    @Ignore
    public void testSetRemovedScalarPropertyWithFallbackOnBlob() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("file", "filename", "test filename");
        assertEquals("test filename", doc.getProperty("file", "content/name"));
    }

    @Test
    @Ignore
    public void testSetRemovedScalarPropertyValueWithFallbackOnBlob() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("file:filename", "test filename");
        assertEquals("test filename", doc.getPropertyValue("file:content/name"));
    }

    @Test
    @Ignore
    public void testSetRemovedScalarPropertiesWithFallbackOnBlob() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("file", Collections.singletonMap("filename", "test filename"));
        assertEquals("test filename", ((Blob) doc.getProperties("file").get("content")).getFilename());
    }

    /**
     * @param removedProperty removed property path to test
     * @param value the value to set, depending on property field type
     */
    private void testProperty(String removedProperty, Object value) throws NoLogCaptureFilterException {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", removedProperty, value);
        assertNull(doc.getProperty("removed", removedProperty));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        String path = "/" + removedProperty;
        assertEquals(String.format(GET_VALUE_LOG, path, "removed"), events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_LOG, path, "removed"), events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, path, "removed"), events.get(2).getRenderedMessage());
    }

    /**
     * @param removedProperty removed property path to test
     * @param value the value to set, depending on property field type
     */
    private void testPropertyValue(String removedProperty, Serializable value) throws NoLogCaptureFilterException {
        String xpath = "removed:" + removedProperty;

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(xpath, value);
        assertNull(doc.getPropertyValue(xpath));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        String path = "/" + removedProperty;
        assertEquals(String.format(GET_VALUE_LOG, path, "removed"), events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_LOG, path, "removed"), events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, path, "removed"), events.get(2).getRenderedMessage());
        logCaptureResult.clear();

        // Test also with a xpath without schema
        doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(removedProperty, value);
        assertNull(doc.getPropertyValue(removedProperty));

        logCaptureResult.assertHasEvent();
        events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_LOG, path, "removed"), events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_LOG, path, "removed"), events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_LOG, path, "removed"), events.get(2).getRenderedMessage());

    }

}
