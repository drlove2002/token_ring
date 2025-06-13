// Node.java
package com.app;

import java.awt.Point;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Node implements Runnable {
    private static final AtomicInteger idCounter = new AtomicInteger(0);
    private static final List<Node> allNodes = Collections.synchronizedList(new ArrayList<>());
    
    private final int nodeId;
    private volatile NodeState state = NodeState.IDLE;
    private Point position;
    private final Graph graph;
    private volatile boolean running = false;
    private Node nextNode;
    private boolean isRequesting = false;
    private ControlToken controlToken = null;
    
    public enum NodeState {
        IDLE(Color.GREEN), 
        REQUESTING(Color.YELLOW), 
        HAS_TOKEN(Color.BLUE), 
        IN_CS(Color.RED);
        
        private final Color color;
        NodeState(Color color) { this.color = color; }
        public Color getColor() { return color; }
    }

    public Node(Graph graph) {
        this.nodeId = idCounter.getAndIncrement();
        this.graph = graph;
        // Position will be set in recalculatePositions()
        this.position = new Point(0, 0);
        System.out.printf("[Node-%d] Created%n", nodeId);
    }

    public static Node createNode(Graph graph) {
        synchronized (allNodes) {
            Node node = new Node(graph);
            allNodes.add(node);
            rebuildRing();
            
            if (allNodes.size() == 1) {
                node.controlToken = new ControlToken();
                node.state = NodeState.HAS_TOKEN;
                System.out.printf("[Node-%d] Initial control token created%n", node.nodeId);
            }
            return node;
        }
    }

    private static void rebuildRing() {
        if (allNodes.isEmpty()) return;
        
        // Reassign next nodes
        for (int i = 0; i < allNodes.size(); i++) {
            allNodes.get(i).nextNode = allNodes.get((i + 1) % allNodes.size());
        }
        
        recalculatePositions();
    }

    private static void recalculatePositions() {
        int centerX = 450, centerY = 325, radius = 200;
        int nodeCount = allNodes.size();
        
        for (int i = 0; i < nodeCount; i++) {
            double angle = 2 * Math.PI * i / nodeCount;
            allNodes.get(i).position = new Point(
                centerX + (int)(radius * Math.cos(angle)),
                centerY + (int)(radius * Math.sin(angle))
            );
        }
    }

    public void start() {
        if (!running) {
            running = true;
            new Thread(this).start();
            System.out.printf("[Node-%d] Thread started%n", nodeId);
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        Random rand = new Random();
        while (running) {
            try {
                if (state == NodeState.IDLE && rand.nextDouble() < 0.1) {
                    requestCriticalSection();
                }
                
                // Process control token if we have it
                if (controlToken != null && state == NodeState.HAS_TOKEN) {
                    processControlToken();
                }
                
                // Shorter sleep for more responsive visualization
                Thread.sleep(1000 + rand.nextInt(1500));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.printf("[Node-%d] Thread stopped%n", nodeId);
    }

    private void requestCriticalSection() {
        System.out.printf("[Node-%d] Requesting CS%n", nodeId);
        state = NodeState.REQUESTING;
        isRequesting = true;
        
        if (controlToken != null) {
            // We have control token - add directly to queue
            controlToken.addRequest(nodeId);
            System.out.printf("[Node-%d] Added request to control token | %s%n", 
                             nodeId, controlToken);
        } else {
            // Create and send request token
            RequestToken requestToken = new RequestToken(nodeId);
            System.out.printf("[Node-%d] Created request token | %s%n", 
                            nodeId, requestToken);
            nextNode.forwardRequestToken(requestToken);
        }
    }

    public void forwardRequestToken(RequestToken requestToken) {
        System.out.printf("[Node-%d] Received request token | %s%n", 
                         nodeId, requestToken);
        
        if (this.nodeId == requestToken.getOriginNodeId()) {
            // Request token returned to sender after full circle
            System.out.printf("[Node-%d] Request token completed full circle | Creating control token%n", 
                            nodeId);
            
            // Create control token if we don't have one
            if (controlToken == null) {
                controlToken = new ControlToken();
                controlToken.addAllRequests(requestToken.getRequests());
                state = NodeState.HAS_TOKEN;
                System.out.printf("[Node-%d] Created control token | %s%n", 
                                nodeId, controlToken);
            }
            return;
        }
        
        // Animate request token transfer
        graph.animateRequestToken(this, nextNode, requestToken, () -> {
            if (controlToken != null) {
                // We have control token - merge requests
                controlToken.addAllRequests(requestToken.getRequests());
                System.out.printf("[Node-%d] Merged requests into control token | %s%n", 
                                nodeId, controlToken);
            } else if (state == NodeState.IN_CS) {
                // In CS - merge requests
                controlToken.addAllRequests(requestToken.getRequests());
                System.out.printf("[Node-%d] Merged requests into control token | %s%n", 
                                nodeId, controlToken);
            } else {
                // Forward request token to next node
                System.out.printf("[Node-%d] Forwarding request token to Node-%d%n", 
                                nodeId, nextNode.nodeId);
                nextNode.forwardRequestToken(requestToken);
            }
        });
    }

    private void processControlToken() {
        System.out.printf("[Node-%d] Processing control token | %s%n", 
                         nodeId, controlToken);
        
        if (controlToken.hasRequests() && controlToken.peekRequest() == nodeId) {
            // Our turn to enter CS
            enterCriticalSection();
        } else {
            // Pass control token to next node
            passControlToken();
        }
    }

    private void enterCriticalSection() {
        System.out.printf("[Node-%d] ENTERING CS | %s%n", 
                         nodeId, controlToken);
        state = NodeState.IN_CS;
        controlToken.removeRequest();  // Remove ourselves from queue
        
        try {
            // Shorter CS duration for better visualization flow
            Thread.sleep(2000);
        } catch (InterruptedException e) { 
            Thread.currentThread().interrupt();
        } finally {
            exitCriticalSection();
        }
    }
    private void exitCriticalSection() {
        System.out.printf("[Node-%d] EXITING CS | %s%n", 
                         nodeId, controlToken);
        isRequesting = false;
        state = NodeState.IDLE;
        passControlToken();
    }

    private void passControlToken() {
        if (nextNode == null) return;
        
        System.out.printf("[Node-%d] Passing control token to Node-%d | %s%n", 
                         nodeId, nextNode.nodeId, controlToken);
        final ControlToken t = controlToken;
        controlToken = null;
        state = isRequesting ? NodeState.REQUESTING : NodeState.IDLE;
        
        // Animate token transfer and pass token after animation completes
        graph.animateControlToken(this, nextNode, () -> {
            nextNode.receiveControlToken(t);
        });
    }

    public synchronized void receiveControlToken(ControlToken token) {
        System.out.printf("[Node-%d] Received control token | %s%n", 
                         nodeId, token);
        this.controlToken = token;
        state = NodeState.HAS_TOKEN;
    }

    // Getters
    public int getNodeId() { return nodeId; }
    public Color getColor() { return state.getColor(); }
    public Point getPosition() { return position; }
    public boolean hasToken() { return controlToken != null; }
    public NodeState getState() { return state; }
    public static List<Node> getAllNodes() { return Collections.unmodifiableList(allNodes); }
    
    public static Node removeLastNode() {
        synchronized (allNodes) {
            if (allNodes.isEmpty()) return null;
            int lastIndex = allNodes.size() - 1;
            Node removed = allNodes.remove(lastIndex);
            removed.stop();
            
            // Handle token transfer if removed node had token
            if (removed.controlToken != null && !allNodes.isEmpty()) {
                // Transfer token to next node in ring
                Node newHolder = removed.nextNode;
                newHolder.receiveControlToken(removed.controlToken);
                System.out.printf("[Node-%d] Control token transferred to Node-%d | %s%n", 
                                 removed.nodeId, newHolder.nodeId, removed.controlToken);
            }
            
            rebuildRing();
            return removed;
        }
    }
}