/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.service;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.rendition.RenditionException;

/**
 * Service handling Rendition Definitions and actual render based on
 * a Rendition Definition
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
public interface RenditionService {

    /**
     * Returns a {@code List} of registered {@code RenditionDefinition}. The
     * order of the List does not depend on the registering order.
     */
    List<RenditionDefinition> getAvailableRenditionDefinitions();

    /**
     * Render a document based on the given rendition definition name
     * and returns the Rendition document.
     * <p>
     * Only the user launching the render operation has the Read right on the
     * returned document.
     *
     * @param sourceDocument the document to render
     * @param renditionDefinitionName the rendition definition to use
     * @return the {@code DocumentRef} of the newly created Rendition document.
     */
    DocumentRef render(DocumentModel sourceDocument,
            String renditionDefinitionName) throws RenditionException;

}