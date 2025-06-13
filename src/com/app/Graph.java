// Graph.java
package com.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class Graph extends JPanel {
    private static final long serialVersionUID = 1L;

    
    // Control token animation
    private Point controlTokenPosition;
    private Node animControlFrom, animControlTo;
    private float animControlProgress;
    private Timer controlTokenTimer;
    
    // Request token animation
    private Point requestTokenPosition;
    private Node animRequestFrom, animRequestTo;
    private float animRequestProgress;
    private Timer requestTokenTimer;

    public Graph() {
        setPreferredSize(new Dimension(900, 650));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw ring edges
        drawRingEdges(g2d);
        
        // Draw nodes
        drawNodes(g2d);
        
        // Draw moving control token
        if (controlTokenPosition != null) {
            g2d.setColor(Color.ORANGE);
            g2d.fillOval(controlTokenPosition.x - 10, controlTokenPosition.y - 10, 20, 20);
            g2d.setColor(Color.BLACK);
            g2d.drawString("C", controlTokenPosition.x - 3, controlTokenPosition.y + 5);
        }
        
        // Draw moving request token
        if (requestTokenPosition != null) {
            g2d.setColor(Color.MAGENTA);
            g2d.fillOval(requestTokenPosition.x - 10, requestTokenPosition.y - 10, 20, 20);
            g2d.setColor(Color.BLACK);
            g2d.drawString("R", requestTokenPosition.x - 3, requestTokenPosition.y + 5);
        }
    }

    private void drawRingEdges(Graphics2D g2d) {
        List<Node> nodes = Node.getAllNodes();
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(2));
        
        for (int i = 0; i < nodes.size(); i++) {
            Node from = nodes.get(i);
            Node to = nodes.get((i + 1) % nodes.size());
            g2d.drawLine(
                from.getPosition().x, from.getPosition().y,
                to.getPosition().x, to.getPosition().y
            );
            
            // Draw arrow
            drawArrow(g2d, from.getPosition(), to.getPosition());
        }
    }
    
    private void drawArrow(Graphics2D g2d, Point from, Point to) {
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int arrowSize = 10;
        Point tip = new Point(
            (int)(to.x - 30 * Math.cos(angle)),
            (int)(to.y - 30 * Math.sin(angle))
        );
        
        Point arrow1 = new Point(
            (int)(tip.x - arrowSize * Math.cos(angle - Math.PI/6)),
            (int)(tip.y - arrowSize * Math.sin(angle - Math.PI/6))
        );
        
        Point arrow2 = new Point(
            (int)(tip.x - arrowSize * Math.cos(angle + Math.PI/6)),
            (int)(tip.y - arrowSize * Math.sin(angle + Math.PI/6))
        );
        
        g2d.fillPolygon(
            new int[]{tip.x, arrow1.x, arrow2.x},
            new int[]{tip.y, arrow1.y, arrow2.y},
            3
        );
    }

    private void drawNodes(Graphics2D g2d) {
        for (Node node : Node.getAllNodes()) {
            Point pos = node.getPosition();
            Color color = node.getColor();
            
            // Draw node
            g2d.setColor(color);
            g2d.fillOval(pos.x - 20, pos.y - 20, 40, 40);
            
            // Highlight if in CS
            if (node.getState() == Node.NodeState.IN_CS) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(pos.x - 24, pos.y - 24, 48, 48);
            }
            
            // Outline for token holder
            if (node.hasToken()) {
                g2d.setColor(Color.ORANGE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(pos.x - 22, pos.y - 22, 44, 44);
            }
            
            // Draw node ID
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(node.getNodeId()), pos.x - 5, pos.y + 5);
        }
    }

    public void animateControlToken(Node from, Node to, Runnable onFinish) {
        animControlFrom = from;
        animControlTo = to;
        animControlProgress = 0;
        
        if (controlTokenTimer != null) {
            controlTokenTimer.stop();
        }
        
        controlTokenTimer = new Timer(20, e -> {
            animControlProgress += 0.05;
            if (animControlProgress >= 1) {
                controlTokenTimer.stop();
                controlTokenPosition = null;
                if (onFinish != null) {
                    onFinish.run();
                }
            } else {
                Point fromPos = animControlFrom.getPosition();
                Point toPos = animControlTo.getPosition();
                controlTokenPosition = new Point(
                    (int) (fromPos.x + (toPos.x - fromPos.x) * animControlProgress),
                    (int) (fromPos.y + (toPos.y - fromPos.y) * animControlProgress)
                );
            }
            repaint();
        });
        controlTokenPosition = from.getPosition();
        controlTokenTimer.start();
    }
    
    public void animateRequestToken(Node from, Node to, RequestToken token, Runnable onFinish) {
        animRequestFrom = from;
        animRequestTo = to;
        animRequestProgress = 0;
        
        if (requestTokenTimer != null) {
            requestTokenTimer.stop();
        }
        
        requestTokenTimer = new Timer(20, e -> {
            animRequestProgress += 0.05;
            if (animRequestProgress >= 1) {
                requestTokenTimer.stop();
                requestTokenPosition = null;
                if (onFinish != null) {
                    onFinish.run();
                }
            } else {
                Point fromPos = animRequestFrom.getPosition();
                Point toPos = animRequestTo.getPosition();
                requestTokenPosition = new Point(
                    (int) (fromPos.x + (toPos.x - fromPos.x) * animRequestProgress),
                    (int) (fromPos.y + (toPos.y - fromPos.y) * animRequestProgress)
                );
            }
            repaint();
        });
        requestTokenPosition = from.getPosition();
        requestTokenTimer.start();
    }

    void addNode(ActionEvent e) {
        synchronized (Node.getAllNodes()) {
            if (Node.getAllNodes().size() >= 10) {
                JOptionPane.showMessageDialog(this, "Max 10 nodes allowed!");
                return;
            }
            Node node = Node.createNode(this);
            node.start();
            repaint();
        }
    }

    void removeNode(ActionEvent e) {
        synchronized (Node.getAllNodes()) {
            List<Node> nodes = Node.getAllNodes();
            if (nodes.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No nodes to remove!");
                return;
            }
            Node removed = Node.removeLastNode();
            if (removed != null) {
                repaint();
            }
        }
    }
}