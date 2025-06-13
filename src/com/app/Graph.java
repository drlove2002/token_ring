package com.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class Graph extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    private final Color RING_EDGE_COLOR = new Color(149, 165, 166);
    private final Color TOKEN_COLOR = new Color(255, 215, 0); // Gold
    
    private Point tokenPosition = null;
    private Node tokenAnimationSource = null;
    private Node tokenAnimationTarget = null;
    private Timer animationTimer = null;

    public Graph() {
        setPreferredSize(new Dimension(900, 650));
        setBackground(BACKGROUND_COLOR);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawRingEdges(g2d);
        drawNodes(g2d);
        drawToken(g2d);
        drawLegend(g2d);
        drawTokenStatus(g2d);
    }

    private void drawRingEdges(Graphics2D g2d) {
        List<Node> nodes = Node.getAllNodes();
        if (nodes.size() < 2) return;
        
        g2d.setColor(RING_EDGE_COLOR);
        g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            Node next = nodes.get((i + 1) % nodes.size());
            if (node != null && next != null) {
                Point from = node.getPosition();
                Point to = next.getPosition();
                
                // Draw edge with arrow
                drawDirectedEdge(g2d, from, to);
            }
        }
    }
    
    private void drawDirectedEdge(Graphics2D g2d, Point from, Point to) {
        // Calculate edge endpoints (don't draw over nodes)
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 70) return;
        
        double startX = from.x + (dx / distance) * 35;
        double startY = from.y + (dy / distance) * 35;
        double endX = to.x - (dx / distance) * 35;
        double endY = to.y - (dy / distance) * 35;
        
        // Draw line
        g2d.drawLine((int)startX, (int)startY, (int)endX, (int)endY);
        
        // Draw small arrowhead
        double arrowLength = 10;
        double arrowAngle = Math.PI / 6;
        double angle = Math.atan2(dy, dx);
        
        int arrowX1 = (int)(endX - arrowLength * Math.cos(angle - arrowAngle));
        int arrowY1 = (int)(endY - arrowLength * Math.sin(angle - arrowAngle));
        int arrowX2 = (int)(endX - arrowLength * Math.cos(angle + arrowAngle));
        int arrowY2 = (int)(endY - arrowLength * Math.sin(angle + arrowAngle));
        
        g2d.fillPolygon(
            new int[]{(int)endX, arrowX1, arrowX2},
            new int[]{(int)endY, arrowY1, arrowY2},
            3
        );
    }

    private void drawNodes(Graphics2D g2d) {
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        for (Node node : Node.getAllNodes()) {
            if (node == null) continue;
            Point pos = node.getPosition();
            Color nodeColor = node.getColor();
            
            // Draw shadow
            g2d.setColor(new Color(0, 0, 0, 30));
            g2d.fillOval(pos.x - 32, pos.y - 28, 64, 64);
            
            // Draw node
            g2d.setColor(nodeColor);
            g2d.fillOval(pos.x - 30, pos.y - 30, 60, 60);
            
            // Add highlight if has token
            if (node.hasToken()) {
                g2d.setColor(TOKEN_COLOR);
                g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawOval(pos.x - 33, pos.y - 33, 66, 66);
            }
            
            // Node border
            g2d.setColor(nodeColor.darker());
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(pos.x - 30, pos.y - 30, 60, 60);
            
            // Node ID
            String id = String.valueOf(node.getNodeId());
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(id);
            int textHeight = fm.getAscent();
            
            g2d.setColor(Color.WHITE);
            g2d.drawString(id, pos.x - textWidth / 2, pos.y + textHeight / 4);
        }
    }
    
    private void drawToken(Graphics2D g2d) {
        if (tokenPosition != null) {
            // Draw animated token
            g2d.setColor(TOKEN_COLOR);
            g2d.fillOval(tokenPosition.x - 15, tokenPosition.y - 15, 30, 30);
            g2d.setColor(TOKEN_COLOR.darker());
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(tokenPosition.x - 15, tokenPosition.y - 15, 30, 30);
            
            // Token label
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2d.drawString("T", tokenPosition.x - 4, tokenPosition.y + 4);
        }
    }

    private void drawLegend(Graphics2D g2d) {
        int x = 15, y = 25;
        
        // Background
        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.fillRoundRect(x - 10, y - 15, 220, 140, 10, 10);
        g2d.setColor(new Color(189, 195, 199));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(x - 10, y - 15, 220, 140, 10, 10);
        
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2d.setColor(new Color(44, 62, 80));
        g2d.drawString("Ring Token Algorithm", x, y);
        y += 25;
        
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        // Node states
        for (Node.NodeState state : Node.NodeState.values()) {
            g2d.setColor(state.getColor());
            g2d.fillOval(x, y - 10, 16, 16);
            g2d.setColor(state.getColor().darker());
            g2d.drawOval(x, y - 10, 16, 16);
            
            g2d.setColor(new Color(44, 62, 80));
            String stateName = state.name().replace("_", " ");
            g2d.drawString(stateName, x + 25, y);
            y += 18;
        }
        
        y += 5;
        g2d.setColor(TOKEN_COLOR);
        g2d.fillOval(x, y - 10, 16, 16);
        g2d.setColor(TOKEN_COLOR.darker());
        g2d.drawOval(x, y - 10, 16, 16);
        g2d.setColor(new Color(44, 62, 80));
        g2d.drawString("Token Holder", x + 25, y);
    }
    
    private void drawTokenStatus(Graphics2D g2d) {
        Token token = Node.getSharedToken();
        if (token == null) return;
        
        List<Node> nodes = Node.getAllNodes();
        Node tokenHolder = nodes.stream()
            .filter(Node::hasToken)
            .findFirst()
            .orElse(null);
        
        int x = getWidth() - 220;
        int y = 25;
        
        // Background
        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.fillRoundRect(x - 10, y - 15, 210, 140, 10, 10);
        g2d.setColor(new Color(189, 195, 199));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(x - 10, y - 15, 210, 140, 10, 10);
        
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2d.setColor(new Color(44, 62, 80));
        g2d.drawString("Token Status", x, y);
        y += 25;
        
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2d.drawString("Current Holder: Node-" + token.getCurrentHolder(), x, y);
        y += 18;
        
        // Show actual queue from token holder
        int actualQueueSize = tokenHolder != null ? tokenHolder.getQueueSize() : 0;
        g2d.drawString("Queue Size: " + actualQueueSize, x, y);
        y += 18;
        g2d.drawString("Total Nodes: " + nodes.size(), x, y);
        y += 18;
        
        // Queue contents from actual token holder
        if (tokenHolder != null && tokenHolder.getQueueSize() > 0) {
            g2d.drawString("Queue: " + tokenHolder.getQueueCopy().toString(), x, y);
        } else {
            g2d.drawString("Queue: Empty", x, y);
        }
        y += 18;
        
        // Show requesting nodes
        long requestingCount = nodes.stream()
            .filter(node -> node.getColor().equals(Node.NodeState.REQUESTING.getColor()))
            .count();
        g2d.drawString("Requesting: " + requestingCount + " nodes", x, y);
    }

    // Animation methods
    public void animateTokenTransfer(Node from, Node to) {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        if (from == null || to == null) {
            tokenPosition = null;
            return;
        }
        
        tokenAnimationSource = from;
        tokenAnimationTarget = to;
        tokenPosition = new Point(from.getPosition());
        
        animationTimer = new Timer(50, e -> updateTokenAnimation());
        animationTimer.start();
    }
    
    private void updateTokenAnimation() {
        if (tokenAnimationSource == null || tokenAnimationTarget == null) {
            if (animationTimer != null) animationTimer.stop();
            return;
        }

        Point end = tokenAnimationTarget.getPosition();
        double dx = end.x - tokenPosition.x;
        double dy = end.y - tokenPosition.y;
        double remainingDistance = Math.sqrt(dx * dx + dy * dy);

        if (remainingDistance < 1) {
            tokenPosition = null;
            tokenAnimationSource = null;
            tokenAnimationTarget = null;
            if (animationTimer != null) animationTimer.stop();
        } else {
            double step = Math.min(8.0, remainingDistance / 10);
            double moveX = (dx / remainingDistance) * step;
            double moveY = (dy / remainingDistance) * step;

            tokenPosition.x += (int) moveX;
            tokenPosition.y += (int) moveY;
        }

        repaint();
    }

    // Control methods
    void addNode(ActionEvent e) {
        if (Node.getAllNodes().size() >= 10) {
            JOptionPane.showMessageDialog(
                this,
                "Maximum number of nodes (10) reached for optimal ring visualization!",
                "Node Limit Reached",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        System.out.println("=== Adding new ring node ===");
        Node newNode = Node.createNode(this);
        if (newNode != null) {
            newNode.start();
            repaint();
            System.out.println("Ring Node " + newNode.getNodeId() + " added and started");
        }
    }

    void removeNode(ActionEvent e) {
        List<Node> nodes = Node.getAllNodes();
        if (nodes.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No nodes available to remove!",
                "No Nodes",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        
        System.out.println("=== Removing ring node ===");
        Node removed = Node.removeLastNode();
        if (removed != null) {
            // Clean up any requests for the removed node
            Node.cleanRequestsForNode(removed.getNodeId());
            
            repaint();
            System.out.println("Ring Node " + removed.getNodeId() + " removed");
        }
    }
}