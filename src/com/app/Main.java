package com.app;

import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {
    private static final long serialVersionUID = 1L;
    private Graph graph = new Graph();

    public Main() {
        super("Ring-Based Token Passing Algorithm Visualizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Control panel
        JPanel controlPanel = new JPanel();
        JButton addNodeBtn = new JButton("Add Node");
        JButton removeNodeBtn = new JButton("Remove Node");

        addNodeBtn.addActionListener(graph::addNode);
        removeNodeBtn.addActionListener(graph::removeNode);

        controlPanel.add(addNodeBtn);
        controlPanel.add(removeNodeBtn);

        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Algorithm Info"));
        
        JLabel info1 = new JLabel("Ring-Based Token Passing Algorithm");
        JLabel info2 = new JLabel("• Green: IDLE state");
        JLabel info3 = new JLabel("• Yellow: REQUESTING Critical Section");
        JLabel info4 = new JLabel("• Blue: HAS TOKEN (ready)");
        JLabel info5 = new JLabel("• Red: IN Critical Section");
        JLabel info6 = new JLabel("• Gold outline: Token holder");
        JLabel info7 = new JLabel("• Animated token shows transfers");
        JLabel info8 = new JLabel("Check console for detailed logs");
        
        info1.setFont(info1.getFont().deriveFont(Font.BOLD));
        
        infoPanel.add(info1);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(info2);
        infoPanel.add(info3);
        infoPanel.add(info4);
        infoPanel.add(info5);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(info6);
        infoPanel.add(info7);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(info8);

        // Layout
        add(graph, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        add(infoPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        
        System.out.println("=== Ring Token Algorithm Visualizer Started ===");
        System.out.println("Add nodes to see the token passing algorithm in action!");
        System.out.println("The first node will receive the initial token.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
