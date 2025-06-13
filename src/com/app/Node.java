package com.app;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

public class Node extends Thread {
    public enum NodeState {
        IDLE(new Color(46, 204, 113)),           // Green - not requesting
        REQUESTING(new Color(241, 196, 15)),     // Yellow - sent request
        HAS_TOKEN(new Color(52, 152, 219)),      // Blue - has token but not in CS
        IN_CS(new Color(231, 76, 60));           // Red - in critical section
        
        private final Color color;
        NodeState(Color color) { this.color = color; }
        public Color getColor() { return color; }
    }

    private static final AtomicInteger idCounter = new AtomicInteger(0);
    private static final List<Node> allNodes = Collections.synchronizedList(new ArrayList<>());
    private static volatile Graph graph = null;
    private static volatile Token sharedToken = null;

    private final int nodeId;
    private final Point position;
    private final AtomicReference<NodeState> currentState = new AtomicReference<>(NodeState.IDLE);
    private volatile Node nextNode;
    private volatile boolean hasToken = false;
    private volatile boolean wantsCS = false;
    
    // Add queue for token holder to manage requests
    private final Queue<Integer> requestQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private Node(Point position) {
        super("Node-" + idCounter.get());
        this.nodeId = idCounter.getAndIncrement();
        this.position = position;
        log("Node created at position " + position);
    }

    private void log(String message) {
        System.out.println(String.format("[Node-%d] %s (State: %s)", 
            nodeId, message, currentState.get()));
    }

    // Static factory methods
    public static synchronized Node createNode(Graph graph) {
        if (Node.graph == null) { 
            Node.graph = graph; 
        }
        
        Point position = calculateRingPosition(allNodes.size());
        Node node = new Node(position);
        allNodes.add(node);
        
        // Update ring connections
        updateRingConnections();
        
        // Initialize token with first node
        if (allNodes.size() == 1) {
            sharedToken = new Token(node.getNodeId());
            node.receiveToken();
        }
        
        return node;
    }
    
    public static synchronized Node removeLastNode() {
        if (allNodes.isEmpty()) return null;
        
        Node removed = allNodes.remove(allNodes.size() - 1);
        removed.interrupt();
        
        // If removed node had token, pass it to next node
        if (removed.hasToken && sharedToken != null && !allNodes.isEmpty()) {
            Node nextInLine = allNodes.get(0); // Give to first available node
            nextInLine.receiveToken();
        }
        
        // Update ring connections
        updateRingConnections();
        
        if (graph != null) {
            graph.repaint();
        }
        
        return removed;
    }
    
    public static synchronized void cleanRequestsForNode(int nodeId) {
        for (Node node : allNodes) {
            node.requestQueue.remove(nodeId);
        }
        if (sharedToken != null) {
            sharedToken.removeRequest(nodeId);
        }
    }
    
    private static Point calculateRingPosition(int nodeIndex) {
        int centerX = 450;
        int centerY = 325; 
        int radius = 200;
        
        if (allNodes.size() == 0) {
            return new Point(centerX, centerY - radius);
        }
        
        double angle = (2 * Math.PI * nodeIndex) / Math.max(1, allNodes.size() + 1);
        int x = (int) (centerX + radius * Math.cos(angle - Math.PI/2));
        int y = (int) (centerY + radius * Math.sin(angle - Math.PI/2));
        
        return new Point(x, y);
    }
    
    private static synchronized void updateRingConnections() {
        if (allNodes.size() <= 1) return;
        
        for (int i = 0; i < allNodes.size(); i++) {
            Node current = allNodes.get(i);
            Node next = allNodes.get((i + 1) % allNodes.size());
            current.nextNode = next;
        }
        
        // Recalculate positions for better ring distribution
        for (int i = 0; i < allNodes.size(); i++) {
            Point newPos = calculateRingPosition(i);
            allNodes.get(i).position.setLocation(newPos);
        }
    }

    // Getters
    public static synchronized List<Node> getAllNodes() {
        return new ArrayList<>(allNodes);
    }
    
    public static Token getSharedToken() {
        return sharedToken;
    }
    
    public int getNodeId() { return nodeId; }
    public Point getPosition() { return new Point(position); }
    public Color getColor() { return currentState.get().getColor(); }
    public Node getNextNode() { return nextNode; }
    public boolean hasToken() { return hasToken; }
    public int getQueueSize() { return requestQueue.size(); }
    public Queue<Integer> getQueueCopy() { return new java.util.concurrent.ConcurrentLinkedQueue<>(requestQueue); }

    // Token request propagation
    public void requestCriticalSection() {
        if (currentState.get() != NodeState.IDLE) {
            return; // Already requesting or has token
        }
        
        wantsCS = true;
        currentState.set(NodeState.REQUESTING);
        log("Requesting Critical Section - sending token request");
        
        if (hasToken) {
            // We already have the token, enter CS immediately
            log("Already have token, entering CS immediately");
            enterCriticalSection();
        } else {
            // Send token request around the ring
            sendTokenRequest(nodeId);
        }
        
        repaintGraph();
    }
    
    // Send token request around the ring
    private void sendTokenRequest(int requesterId) {
        if (nextNode != null) {
            nextNode.receiveTokenRequest(requesterId);
        }
    }
    // Receive and handle token requests
    public void receiveTokenRequest(int requesterId) {
        Node current = this;
        int totalNodes = allNodes.size(); // Get current node count
        int maxSteps = totalNodes * 2;   // Max 2 full ring rotations
        int steps = 0;
        boolean delivered = false;

        while (steps < maxSteps) {
            steps++;
            
            if (current.hasToken) {
                // Token holder processing
                if (requesterId != current.nodeId && !current.requestQueue.contains(requesterId)) {
                    current.requestQueue.offer(requesterId);
                    current.log("Token holder received request from Node-" + requesterId + 
                        ". Queue size: " + current.requestQueue.size());
                    if (sharedToken != null) {
                        sharedToken.addRequest(requesterId);
                    }
                }
                delivered = true;
                break;
            }
            
            // Move to next node in ring
            current = current.nextNode;
            if (current == null) break;
        }

        if (!delivered) {
            log("WARNING: Request for Node-" + requesterId + " not delivered after " + maxSteps + " steps");
        }
    }


    // Token handling
    public synchronized void receiveToken() {
        hasToken = true;
        if (sharedToken != null) {
            sharedToken.setCurrentHolder(nodeId);
        }
        log("Received token. Current state: " + currentState.get());
        
        if (wantsCS) {
            log("Wants CS - entering Critical Section immediately");
            enterCriticalSection();
        } else {
            currentState.set(NodeState.HAS_TOKEN);
            log("Has token but not requesting CS. Queue size: " + requestQueue.size());
            repaintGraph();
            
            // Check if there are pending requests
            if (!requestQueue.isEmpty()) {
                log("Found pending requests, will pass token");
                // Small delay to show the HAS_TOKEN state, then pass
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        if (!isInterrupted() && hasToken && !wantsCS && currentState.get() == NodeState.HAS_TOKEN) {
                            passTokenToNext();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "TokenCheck-" + nodeId).start();
            } else {
                // No pending requests, pass token after delay
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        if (!isInterrupted() && hasToken && !wantsCS && currentState.get() == NodeState.HAS_TOKEN) {
                            passTokenToNext();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "TokenPass-" + nodeId).start();
            }
        }
    }
    
    private void enterCriticalSection() {
        currentState.set(NodeState.IN_CS);
        wantsCS = false;
        log("ENTERED Critical Section");
        repaintGraph();
        
        // Schedule CS exit
        new Thread(() -> {
            try {
                Thread.sleep(3000 + new Random().nextInt(2000)); // 3-5 seconds
                if (!isInterrupted()) {
                    exitCriticalSection();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "CS-Timer-" + nodeId).start();
    }
    
    private void exitCriticalSection() {
        log("EXITING Critical Section. Queue size: " + requestQueue.size());
        currentState.set(NodeState.HAS_TOKEN);
        repaintGraph();
        
        // Always pass token after exiting CS
        passTokenToNext();
    }
    
    // Proper token passing logic
    private synchronized void passTokenToNext() {
        if (!hasToken) {
            log("Cannot pass token - don't have it");
            return;
        }
        
        // Assign nextRequesterId once (making it effectively final)
        Integer nextRequesterId = requestQueue.isEmpty() ? null : requestQueue.peek();
        Node targetNode = null;
        
        if (nextRequesterId != null) {
            targetNode = allNodes.stream()
                .filter(node -> node.getNodeId() == nextRequesterId)
                .findFirst()
                .orElse(null);
            if (targetNode != null) {
                requestQueue.poll(); // Remove from queue
                log("Passing token to requester Node-" + nextRequesterId + ". Remaining queue: " + requestQueue.size());
            }
        }
        
        if (targetNode == null) {
            targetNode = nextNode;
            log("No pending requests, passing token to next node: " + (targetNode != null ? targetNode.getNodeId() : "none"));
        }
        
        if (targetNode != null) {
            final Node finalTarget = targetNode;
            
            hasToken = false;
            currentState.set(NodeState.IDLE);
            
            if (nextRequesterId != null && sharedToken != null) {
                sharedToken.removeRequest(); // Remove from display queue
            }
            
            printQueueStatus();
            
            if (graph != null) {
                graph.animateTokenTransfer(this, finalTarget);
            }
            
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    if (!isInterrupted()) {
                        finalTarget.receiveToken();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "TokenTransfer-" + nodeId).start();
        } else {
            log("No valid target node to pass token to");
        }
        
        repaintGraph();
    }
    
    private void printQueueStatus() {
        System.out.println("=== QUEUE STATUS ===");
        System.out.println("Token passed from Node-" + nodeId);
        System.out.println("Remaining requests in queue: " + requestQueue.size());
        if (!requestQueue.isEmpty()) {
            System.out.println("Queue contents: " + requestQueue);
        }
        System.out.println("==================");
    }

    @Override
    public void run() {
        Random rand = new Random();
        log("Node thread started");
        
        while (!isInterrupted()) {
            try {
                // Variable wait time
                int waitTime = 8000 + rand.nextInt(7000); // 8-15 seconds
                Thread.sleep(waitTime);
                
                // 40% chance to request CS when idle (increased for more activity)
                if (currentState.get() == NodeState.IDLE && rand.nextDouble() < 0.40 && !isInterrupted()) {
                    requestCriticalSection();
                }
                
            } catch (InterruptedException e) {
                log("Node thread interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        log("Node thread terminated");
    }
    
    private void repaintGraph() {
        if (graph != null) {
            SwingUtilities.invokeLater(() -> graph.repaint());
        }
    }
}