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
 */
package org.nuxeo.ecm.automation.test.service;

import com.google.inject.Inject;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.io.services.contributor.HeaderDocEvaluationContext;
import org.nuxeo.ecm.automation.io.services.contributor.RestContributor;
import org.nuxeo.ecm.automation.io.services.contributor.RestContributorService;
import org.nuxeo.ecm.automation.io.services.contributor.RestContributorServiceImpl;
import org.nuxeo.ecm.automation.io.services.contributor.RestEvaluationContext;
import org.nuxeo.ecm.automation.jaxrs.io.audit.LogEntryWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.ws.rs.core.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.io", "org.nuxeo.ecm.actions" })
@LocalDeploy("org.nuxeo.ecm.automation.io:testrestcontrib.xml")
public class RestServiceTest {

    private static final String[] NO_SCHEMA = new String[] {};

    @Inject
    RestContributorService rcs;

    @Inject
    CoreSession session;

    @Inject
    JsonFactory factory;

    @Before
    public void doBefore() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "folder1",
                "Folder");
        session.createDocument(doc);

        for (int i = 0; i < 3; i++) {
            doc = session.createDocumentModel("/folder1", "doc" + i, "Note");
            session.createDocument(doc);
        }
    }

    @Test
    public void itCanGetTheRestContributorService() throws Exception {
        assertNotNull(rcs);
    }

    @Test
    public void itCanGetContributorsFromTheService() throws Exception {
        List<RestContributor> cts = rcs.getContributors("test", null);
        assertEquals(1, cts.size());
    }

    @Test
    public void itCanFilterContributorsByCategory() throws Exception {
        List<RestContributor> cts = rcs.getContributors("anothertest", null);
        assertEquals(2, cts.size());
    }

    @Test
    public void itCanWriteToContext() throws Exception {

        // Given some input context (header + doc)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        DocumentModel folder = session.getDocument(new PathRef("/folder1"));
        RestEvaluationContext ec = new HeaderDocEvaluationContext(folder,
                getFakeHeaders());

        // When the service write to the context
        jg.writeStartObject();
        rcs.writeContext(jg, ec);
        jg.writeEndObject();
        jg.flush();

        // Then it is filled with children contributor
        String jsonFolder = out.toString();
        JsonNode node = parseJson(jsonFolder);
        assertEquals("documents",
                node.get("children").get("entity-type").getValueAsText());

    }

    @Test
    public void documentWriterUsesTheRestConributorService() throws Exception {
        // Given a document
        DocumentModel folder = session.getDocument(new PathRef("/folder1"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);

        // When it is written as Json with appropriate headers
        JsonDocumentWriter.writeDocument(jg, folder, NO_SCHEMA,
                new HashMap<String, String>(), getFakeHeaders());
        jg.flush();

        // Then it contains contextParameters with contributor
        JsonNode node = parseJson(out);
        assertNotNull(node.get("contextParameters").get("children"));

        // When it is written as Json with empty headers
        out = new ByteArrayOutputStream();
        jg = getJsonGenerator(out);
        JsonDocumentWriter.writeDocument(jg, folder, NO_SCHEMA,
                new HashMap<String, String>(), null);
        jg.flush();

        // Then it contains contextParameters with contributor
        node = parseJson(out);
        assertNull(node.get("contextParameters").get("children"));

    }

    @Test
    public void itCanContributeWithBreadcrumb() throws Exception {
        // Given a document
        DocumentModel folder = session.getDocument(new PathRef("/folder1/doc0"));

        // When it is written as Json with breadcrumb context category
        String jsonFolder = getDocumentAsJson(folder, "breadcrumb");
        // Then it contains the breadcrumb in contextParameters
        JsonNode node = parseJson(jsonFolder);
        JsonNode breadCrumbEntries = node.get("contextParameters").get(
                "breadcrumb").get("entries");
        assertEquals("/folder1",
                breadCrumbEntries.get(0).get("path").getValueAsText());
        assertEquals("/folder1/doc0",
                breadCrumbEntries.get(1).get("path").getValueAsText());

    }

    @Test
    public void itHasContributorFilteredWithActionFilters() throws Exception {
        // Given a folder and a doc
        DocumentModel folder = session.getDocument(new PathRef("/folder1"));
        DocumentModel note = session.getDocument(new PathRef("/folder1/doc0"));

        // When it is written as Json whith test category
        String jsonFolder = getDocumentAsJson(folder);
        String jsonNote = getDocumentAsJson(note);

        // Then it contains the children in contextParameters if folderish
        JsonNode node = parseJson(jsonFolder);
        JsonNode children = node.get("contextParameters").get("children");
        assertNotNull(children);

        node = parseJson(jsonNote);
        children = node.get("contextParameters").get("children");
        assertNull(children);

    }

    @Test
    public void itCanWriteLogEntry() throws Exception {
        DocumentModel folder = session.getDocument(new PathRef("/folder1"));
        String id = folder.getId();

        LogEntry entry = new LogEntryImpl();
        entry.setEventId("documentModified");
        entry.setDocUUID(id);
        entry.setEventDate(new Date());
        entry.setDocPath("/" + id);
        entry.setRepositoryId("test");
        entry.setCategory("Workflow");
        entry.setComment("comment");
        entry.setDocLifeCycle("deleted");
        entry.setLogDate(new Date());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);

        // When it is written as Json
        LogEntryWriter.writeLogEntry(jg, entry);
        jg.flush();

        // Then it contains
        JsonNode node = parseJson(out);
        assertEquals("Workflow", node.get("category").getTextValue());
    }

    /**
     * Parses a JSON string into a JsonNode
     *
     * @param json
     * @return
     * @throws IOException
     * @throws JsonProcessingException
     *
     */
    private JsonNode parseJson(String json) throws JsonProcessingException,
            IOException {
        ObjectMapper m = new ObjectMapper();
        return m.readTree(json);
    }

    /**
     * @param out
     * @return
     * @throws IOException
     * @throws JsonProcessingException
     *
     */
    private JsonNode parseJson(ByteArrayOutputStream out)
            throws JsonProcessingException, IOException {
        return parseJson(out.toString());
    }

    /**
     * Returns the JSON representation of the document. A category may be passed
     * to have impact on the Rest contributors
     *
     * @param doc
     * @param category
     * @return
     * @throws Exception
     *
     */
    private String getDocumentAsJson(DocumentModel doc, String category)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        // When it is written as Json with appropriate headers
        JsonDocumentWriter.writeDocument(jg, doc, NO_SCHEMA,
                new HashMap<String, String>(), getFakeHeaders(category));
        jg.flush();
        return out.toString();
    }

    /**
     * Returns the JSON representation of the document.
     *
     * @param doc
     * @return
     * @throws Exception
     *
     */
    private String getDocumentAsJson(DocumentModel doc) throws Exception {
        return getDocumentAsJson(doc, null);
    }

    private JsonGenerator getJsonGenerator(OutputStream out) throws IOException {
        return factory.createJsonGenerator(out);
    }

    private HttpHeaders getFakeHeaders() {
        return getFakeHeaders(null);
    }

    private HttpHeaders getFakeHeaders(String category) {
        HttpHeaders headers = mock(HttpHeaders.class);

        when(
                headers.getRequestHeader(JsonDocumentWriter.DOCUMENT_PROPERTIES_HEADER)).thenReturn(
                Arrays.asList(NO_SCHEMA));

        when(
                headers.getRequestHeader(RestContributorServiceImpl.NXCONTENT_CATEGORY_HEADER)).thenReturn(
                Arrays.asList(new String[] { category == null ? "test"
                        : category }));
        return headers;
    }

}
