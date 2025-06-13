// RequestToken.java
package com.app;

import java.util.Queue;
import java.util.LinkedList;

public class RequestToken {
    private final Queue<Integer> requests = new LinkedList<>();
    private final int originNodeId;

    public RequestToken(int originNodeId) {
        this.originNodeId = originNodeId;
        this.requests.add(originNodeId);
    }

    public void addRequest(int nodeId) {
        if (!requests.contains(nodeId)) {
            requests.add(nodeId);
        }
    }

    public void addAllRequests(Queue<Integer> newRequests) {
        for (int id : newRequests) {
            addRequest(id);
        }
    }

    public int getOriginNodeId() {
        return originNodeId;
    }

    public Queue<Integer> getRequests() {
        return new LinkedList<>(requests);
    }

    public boolean contains(int nodeId) {
        return requests.contains(nodeId);
    }

    @Override
    public String toString() {
        return "RequestToken{origin=" + originNodeId + ", requests=" + requests + "}";
    }
}