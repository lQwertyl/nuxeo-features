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
package org.nuxeo.ecm.automation.server.jaxrs.adapters;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodec;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.automation.jaxrs.DefaultJsonAdapter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Basic CRUD with a BusinessAdapter
 *
 * @since 5.7.2
 */
@WebAdapter(name = BOAdapter.NAME, type = "BOService", targetType = "Document")
@Produces({ "application/json+nxentity", "application/json" })
public class BOAdapter extends DefaultAdapter {

    public static final String NAME = "bo";

    @GET
    @Path("{adapterName}")

    public Object doGetAdapter(@PathParam("adapterName")
    String adapterName) throws Exception {
        BusinessAdapter adapter = getAdapter(adapterName);
        return new DefaultJsonAdapter(adapter);
    }

    @PUT
    @Path("{adapterName}")
    @Consumes({ "application/json+nxentity", "application/json" })
    public Object doPostAdapter(@PathParam("adapterName")
    String adapterName, BusinessAdapter input) throws Exception {
        ctx.getCoreSession().saveDocument(input.getDocument());

        //TODO : To be removed
        ctx.getCoreSession().save();

        return new DefaultJsonAdapter(input);
    }

    @POST
    @Path("{adapterName}/{docName}")
    public Object doPutAdapter(@PathParam("adapterName")
    String adapterName, @PathParam("docName")
    String docName, BusinessAdapter input) throws Exception {
        DocumentModel document = input.getDocument();

        DocumentObject dobj = (DocumentObject) getTarget();
        DocumentModel parentDoc = dobj.getDocument();

        document.setPathInfo(parentDoc.getPathAsString(), docName);
        CoreSession session = ctx.getCoreSession();
        document = session.createDocument(document);
        session.save();
        BusinessAdapter adapter = document.getAdapter(input.getClass());
        return new DefaultJsonAdapter(adapter);
    }

    private BusinessAdapter getAdapter(String adapterName) {
        ObjectCodecService cs = Framework.getLocalService(ObjectCodecService.class);
        ObjectCodec<?> codec = cs.getCodec(adapterName);
        if (codec != null) {
            DocumentObject dobj = (DocumentObject) getTarget();
            DocumentModel doc = dobj.getDocument();

            return (BusinessAdapter) doc.getAdapter(codec.getJavaType());
        } else {
            throw new WebException(String.format("Unable to find [%s] adapter",
                    adapterName));
        }

    }

}
