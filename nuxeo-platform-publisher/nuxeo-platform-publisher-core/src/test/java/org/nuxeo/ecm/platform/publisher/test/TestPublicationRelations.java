/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.test;

import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.jdbc.jdbcDataSource;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.helper.PublicationRelationHelper;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TestPublicationRelations extends SQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestPublicationRelations.class);

    protected DocumentModel doc2Publish;

    public TestPublicationRelations() {
        super();
    }

    public TestPublicationRelations(String name) {
        super(name);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        NuxeoContainer.installNaming();

        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:jena");
        ds.setUser("sa");
        ds.setPassword("");
        NuxeoContainer.addDeepBinding(
                "java:comp/env/jdbc/nxrelations-default-jena", ds);
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseType", "HSQL");
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseTransactionEnabled", "false");

        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.versioning.api");
        deployBundle("org.nuxeo.ecm.platform.versioning");
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployContrib("org.nuxeo.ecm.platform.publisher.test",
                "OSGI-INF/relations-default-jena-contrib.xml");

        deployBundle("org.nuxeo.ecm.platform.publisher.core.contrib");
        deployBundle("org.nuxeo.ecm.platform.publisher.core");

        fireFrameworkStarted();
        openSession();
    }

    @After
    public void tearDown() throws Exception {
        try {
            closeSession();
        } finally {
            if (NuxeoContainer.isInstalled()) {
                NuxeoContainer.uninstall();
            }
            super.tearDown();
        }
    }

    protected void createInitialDocs() throws Exception {

        DocumentModel wsRoot = session.getDocument(new PathRef(
                "default-domain/workspaces"));

        DocumentModel ws = session.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = session.createDocument(ws);

        DocumentModel sectionsRoot = session.getDocument(new PathRef(
                "default-domain/sections"));

        DocumentModel section1 = session.createDocumentModel(
                sectionsRoot.getPathAsString(), "section1", "Section");
        section1.setProperty("dublincore", "title", "section1");
        section1 = session.createDocument(section1);

        DocumentModel section2 = session.createDocumentModel(
                sectionsRoot.getPathAsString(), "section2", "Section");
        section2.setProperty("dublincore", "title", "section2");
        section2 = session.createDocument(section2);

        DocumentModel section11 = session.createDocumentModel(
                section1.getPathAsString(), "section11", "Section");
        section11.setProperty("dublincore", "title", "section11");
        section11 = session.createDocument(section11);

        doc2Publish = session.createDocumentModel(ws.getPathAsString(), "file",
                "File");
        doc2Publish.setProperty("dublincore", "title", "MyDoc");

        Blob blob = new StringBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        blob.setMimeType("text/plain");
        doc2Publish.setProperty("file", "content", blob);

        doc2Publish = session.createDocument(doc2Publish);

        session.save();
    }

    @Test
    public void testPublicationRelation() throws Exception {
        createInitialDocs();

        PublisherService service = Framework.getLocalService(PublisherService.class);
        PublicationTree tree = service.getPublicationTree(
                service.getAvailablePublicationTree().get(0), session, null);
        assertNotNull(tree);

        List<PublicationNode> nodes = tree.getChildrenNodes();
        PublicationNode targetNode = nodes.get(0);
        PublishedDocument pubDoc = tree.publish(doc2Publish, targetNode);
        assertTrue(pubDoc instanceof SimpleCorePublishedDocument);

        DocumentModel proxy = ((SimpleCorePublishedDocument) pubDoc).getProxy();
        assertTrue(PublicationRelationHelper.isPublished(proxy));

        assertEquals(tree.getConfigName(),
                service.getPublicationTreeFor(proxy, session).getConfigName());
    }

}
