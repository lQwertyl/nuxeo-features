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
import java.util.Map;

import org.nuxeo.ecm.automation.OperationChain;


public class TracerFactory {

    protected Map<String,ChainTraces> traces = new HashMap<String,ChainTraces>();

    protected boolean recording = false;

    protected static class ChainTraces {

        protected OperationChain chain;

        protected Map<Integer,Trace> traces = new HashMap<Integer,Trace>();

        protected ChainTraces(OperationChain chain) {
            this.chain = chain;
        }

        protected String add(Trace trace) {
            final int index = Integer.valueOf(traces.size());
            traces.put(Integer.valueOf(index),trace);
            return formatKey(trace.chain, index);
        }

        protected Trace getTrace(int index) {
            return traces.get(index);
        }

        protected void removeTrace(int index) {
            traces.remove(index);
        }

        protected void clear() {
            traces.clear();
        }

    }

    public Tracer newTracer() {
        return new Tracer(this);
    }

    public String recordTrace(Trace trace) {
        String chainId = trace.chain.getId();
        if (!traces.containsKey(chainId)) {
            traces.put(chainId, new ChainTraces(trace.chain));
        }
        return traces.get(chainId).add(trace);
    }

    public Trace getTrace(OperationChain chain, int index) {
        return traces.get(chain.getId()).getTrace(index);
    }

    public Trace getTrace(String key) {
        String[] parts = key.split(":");
        String chainId = parts[0];
        Integer index =Integer.valueOf(Integer.parseInt(parts[1]));
        return traces.get(chainId).getTrace(index);
    }

    public void clearTrace(OperationChain chain, int index) {
        traces.get(chain).removeTrace(Integer.valueOf(index));
    }

    public void clearTraces(OperationChain chain) {
        traces.remove(chain);
    }

    public void clearTraces() {
        traces.clear();
    }

    protected static String formatKey(OperationChain chain, int index) {
        return String.format("%s:%s", chain.getId(), index);
    }

    public void onTrace(Trace popped) {
        if (!recording) {
            return;
        }
        recordTrace(popped);
    }

    public void setRecording() {
        recording = true;
    }

    public void unsetRecording() {
        recording = false;
    }

    public boolean toggleRecording() {
        boolean last = recording;
        recording = !recording;
        return last;
    }
}