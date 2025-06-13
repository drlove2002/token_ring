// ControlToken.java
package com.app;

import java.util.Queue;
import java.util.LinkedList;

public class ControlToken {
    private final Queue<Integer> queue = new LinkedList<>();

    public void addRequest(int nodeId) {
        if (!queue.contains(nodeId)) {
            queue.add(nodeId);
        }
    }

    public void addAllRequests(Queue<Integer> requests) {
        for (int id : requests) {
            addRequest(id);
        }
    }

    public int removeRequest() {
        return queue.poll();
    }

    public boolean hasRequests() {
        return !queue.isEmpty();
    }

    public int peekRequest() {
        return queue.peek();
    }

    public boolean contains(int nodeId) {
        return queue.contains(nodeId);
    }

    public Queue<Integer> getQueue() {
        return new LinkedList<>(queue);
    }

    @Override
    public String toString() {
        return "ControlToken{queue=" + queue + "}";
    }
}