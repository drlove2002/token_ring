package com.app;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

public class Token {
    private volatile int currentHolder;
    private final Queue<Integer> displayQueue; // For visualization only
    private long tokenId;
    private static long tokenCounter = 0;
    
    public Token(int initialHolder) {
        this.currentHolder = initialHolder;
        this.displayQueue = new ConcurrentLinkedQueue<>();
        this.tokenId = tokenCounter++;
        System.out.println("[Token-" + tokenId + "] Created with initial holder: Node-" + initialHolder);
    }
    
    public synchronized int getCurrentHolder() {
        return currentHolder;
    }
    
    public synchronized void setCurrentHolder(int nodeId) {
        this.currentHolder = nodeId;
        System.out.println("[Token-" + tokenId + "] Token holder changed to Node-" + nodeId);
    }
    
    // For display purposes only - actual queue management is in Node
    public synchronized void addRequest(int requesterId) {
        if (!displayQueue.contains(requesterId) && requesterId != currentHolder) {
            displayQueue.offer(requesterId);
            System.out.println("[Token-" + tokenId + "] Display queue updated - Added Node-" + requesterId + 
                ". Queue size: " + displayQueue.size());
        }
    }
    
    public synchronized void removeRequest() {
        if (!displayQueue.isEmpty()) {
            Integer removed = displayQueue.poll();
            System.out.println("[Token-" + tokenId + "] Display queue updated - Removed Node-" + removed + 
                ". Queue size: " + displayQueue.size());
        }
    }
    
    public synchronized void removeRequest(int nodeId) {
        displayQueue.remove(nodeId);
        System.out.println("[Token-" + tokenId + "] Display queue updated - Removed Node-" + nodeId + 
            ". Queue size: " + displayQueue.size());
    }
    
    public synchronized Integer getNextRequester() {
        return displayQueue.peek();
    }
    
    public synchronized boolean hasRequests() {
        return !displayQueue.isEmpty();
    }
    
    public synchronized int getQueueSize() {
        return displayQueue.size();
    }
    
    public synchronized Queue<Integer> getQueueCopy() {
        return new ConcurrentLinkedQueue<>(displayQueue);
    }
    
    public synchronized void clearQueue() {
        displayQueue.clear();
    }
    
    public long getTokenId() {
        return tokenId;
    }
    
    @Override
    public String toString() {
        return String.format("Token-%d (Holder: %d, Queue: %s)", 
            tokenId, currentHolder, displayQueue.toString());
    }
}