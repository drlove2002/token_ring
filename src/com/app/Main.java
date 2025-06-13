// Main.java
package com.app;

import javax.swing.*;
import java.awt.*;
import java.io.PrintStream;

public class Main extends JFrame {
    private static final long serialVersionUID = 1L;
	private final Graph graph = new Graph();

    public Main() {
        super("Token Ring Algorithm");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Control panel
        JPanel controls = new JPanel();
        JButton addBtn = new JButton("Add Node");
        JButton removeBtn = new JButton("Remove Node");
        
        addBtn.addActionListener(graph::addNode);
        removeBtn.addActionListener(graph::removeNode);
        
        controls.add(addBtn);
        controls.add(removeBtn);
        
        // Info panel
        JTextArea logs = new JTextArea(10, 30);
        logs.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logs);
        logScroll.setBorder(BorderFactory.createTitledBorder("Logs"));
        
        // Redirect console output to log panel
        System.setOut(new PrintStream(new TextAreaOutputStream(logs)));
        
        add(graph, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);
        add(logScroll, BorderLayout.EAST);
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
    
    // Helper class to redirect console output to JTextArea
    static class TextAreaOutputStream extends java.io.OutputStream {
        private final JTextArea textArea;
        
        public TextAreaOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }
        
        @Override
        public void write(int b) {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }
}