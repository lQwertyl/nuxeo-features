/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.automation.core.trace;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;

public class Call {

    public Call(OperationContext context, OperationType type,
            InvokableMethod method, Map<String, Object> parms) {
        this.type = type;
        this.variables = new HashMap<String, Object>(context);
        this.method = method;
        this.input = context.getInput();
        this.parameters = parms;
    }

    protected final OperationType type;

    protected final InvokableMethod method;

    protected final Map<String, Object> parameters;

    protected final Map<String, Object> variables;

    protected final List<Trace> nested = new LinkedList<Trace>();

    protected final Object input;

    public OperationType getType() {
        return type;
    }

    public InvokableMethod getMethod() {
        return method;
    }

    public Map<String, Object> getParmeters() {
        return parameters;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Object getInput() {
        return input;
    }

    public List<Trace> getNested() {
        return nested;
    }
}