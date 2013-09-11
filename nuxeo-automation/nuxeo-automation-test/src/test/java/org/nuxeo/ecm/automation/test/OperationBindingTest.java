/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 *     vpasquier
 */
package org.nuxeo.ecm.automation.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.trace.Trace;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList;
import org.nuxeo.ecm.automation.server.jaxrs.adapters.BlobAdapter;
import org.nuxeo.ecm.automation.server.jaxrs.adapters.OperationAdapter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @since 5.7.2 - Test the Rest binding to run operations
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@LocalDeploy({ "org.nuxeo.ecm.automation.test:operation-contrib.xml" })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class OperationBindingTest extends BaseTest {

    private static String PARAMS = "{\"params\":{\"one\":\"1\",\"two\": 2}}";

    @Inject
    protected AutomationService automationService;

    @Inject
    protected TracerFactory factory;

    @Override
    @Before
    public void doBefore() throws Exception {
        super.doBefore();
        // Activate trace mode
        if (!factory.getRecordingState()) {
            factory.toggleRecording();
        }
    }

    @Test
    public void itCanRunAnOperationOnaDocument() throws Exception {

        // Given a document and an operation
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i call the REST binding on the document resource
        ClientResponse response = getResponse(RequestType.POSTREQUEST, "id/"
                + note.getId() + "/@" + OperationAdapter.NAME + "/testOp",
                PARAMS);

        // Then the operation is called on the document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // Then the operation is called on all children documents
        Trace trace = factory.getTrace("testOp");
        assertEquals(1, trace.getCalls().size());

        Map parameters = (Map) trace.getCalls().get(0).getVariables().get(
                Constants.VAR_RUNTIME_CHAIN);

        assertEquals("1", parameters.get("one"));
        assertEquals(2, parameters.get("two"));
        assertEquals(note.getId(), ((DocumentModel) trace.getOutput()).getId());
    }

    @Test
    public void itCanRunAChainOnADocument() throws Exception {
        // Given a document and an operation
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i call the REST binding on the document resource
        ClientResponse response = getResponse(RequestType.POSTREQUEST, "id/"
                + note.getId() + "/@" + OperationAdapter.NAME + "/testChain",
                "{}");

        // Then the operation is called twice on the document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Then the operation is called on all children documents
        Trace trace = factory.getTrace("testChain");
        assertEquals(2, trace.getCalls().size());

        Map parameters = trace.getCalls().get(0).getParmeters();

        assertEquals("One", parameters.get("one"));
        assertEquals("2", parameters.get("two"));
        assertEquals(note.getId(), ((DocumentModel) trace.getOutput()).getId());

        parameters = trace.getCalls().get(1).getParmeters();
        assertEquals("4", parameters.get("two"));
        assertEquals("Two", parameters.get("one"));

    }

    @Test
    public void itCanRunAChainOnMutlipleDocuments() throws Exception {
        // Given a folder
        DocumentModel folder = RestServerInit.getFolder(1, session);

        // When i call the REST binding on the children resource

        getResponse(RequestType.POSTREQUEST, "id/" + folder.getId()
                + "/@children/@" + OperationAdapter.NAME + "/testOp", PARAMS);

        // Then the operation is called on all children documents
        Trace trace = factory.getTrace("testOp");
        assertEquals(1, trace.getCalls().size());
        assertEquals(6, ((PaginableDocumentModelList) trace.getOutput()).size());

    }

    @Test
    public void itCanRunAutomationWithBlob() throws Exception {
        // Given a file
        DocumentModel file = RestServerInit.getFile(1, session);

        // When i call the REST binding on the blob resource
        getResponse(RequestType.POSTREQUEST, "id/" + file.getId() + "/@"
                + BlobAdapter.NAME + "/file:content/@" + OperationAdapter.NAME
                + "/testOp", PARAMS);

        // Then the operation is called on a document blob
        Trace trace = factory.getTrace("testOp");
        assertTrue(trace.getOutput() instanceof Blob);
    }

}
