package org.cloudsim.adaptive.zone.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Random;

import javax.swing.JPanel;

import org.cloudsim.adaptive.zone.core.ResourceZone;
import org.cloudsim.adaptive.zone.core.ZoneManager;

/**
 * Panel for visualizing zone distribution and VM placement
 */
public class ZoneVisualizationPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private ZoneManager zoneManager;
    private Random random = new Random();

    public ZoneVisualizationPanel(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawZoneVisualization(g2d);
    }

    /**
     * Draw zone visualization with VM distributions
     */
    private void drawZoneVisualization(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();

        if (zoneManager == null || width <= 0 || height <= 0) return;

        // Draw title
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Three-Tier Adaptive Zone Model", 20, 30);

        // Zone colors
        Color[] zoneColors = {
            new Color(255, 102, 102), // High-Resource (Red)
            new Color(102, 178, 255), // Medium (Blue)
            new Color(102, 255, 102)  // Fast & Small (Green)
        };

        String[] zoneNames = {
            "High-Resource & Slow Zone",
            "Medium Zone", 
            "Fast & Small VM Zone"
        };

        // Draw zones as circles
        int centerX = width / 2;
        int centerY = height / 2;
        int baseRadius = Math.min(width, height) / 8;

        for (int i = 0; i < Math.min(3, zoneManager.getZones().size()); i++) {
            ResourceZone zone = zoneManager.getZone(i);
            if (zone == null) continue;

            // Calculate zone size based on VM count
            int vmCount = zone.getVMCount();
            int radius = baseRadius + (vmCount * 3);

            // Position zones in triangle formation
            double angle = (i * 2 * Math.PI / 3) - (Math.PI / 2);
            int zoneX = centerX + (int)(150 * Math.cos(angle));
            int zoneY = centerY + (int)(150 * Math.sin(angle));

            // Draw zone circle
            g2d.setColor(zoneColors[i]);
            g2d.fillOval(zoneX - radius, zoneY - radius, radius * 2, radius * 2);

            // Draw zone border
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(zoneX - radius, zoneY - radius, radius * 2, radius * 2);

            // Draw VMs as small circles within zone
            drawVMsInZone(g2d, zoneX, zoneY, radius - 10, vmCount);

            // Draw zone label
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            String label = zoneNames[i] + " (" + vmCount + " VMs)";
            int labelWidth = fm.stringWidth(label);
            g2d.setColor(Color.BLACK);
            g2d.drawString(label, zoneX - labelWidth/2, zoneY + radius + 20);
        }

        // Draw legend
        drawLegend(g2d, zoneColors, zoneNames);

        // Draw performance metrics
        drawPerformanceMetrics(g2d);
    }

    /**
     * Draw VMs as small circles within a zone
     */
    private void drawVMsInZone(Graphics2D g2d, int centerX, int centerY, int zoneRadius, int vmCount) {
        g2d.setColor(Color.DARK_GRAY);
        int vmSize = 4;

        for (int i = 0; i < Math.min(vmCount, 20); i++) { // Limit visual VMs to 20
            double angle = (i * 2 * Math.PI / Math.min(vmCount, 20));
            double distance = random.nextDouble() * (zoneRadius - vmSize);

            int vmX = centerX + (int)(distance * Math.cos(angle));
            int vmY = centerY + (int)(distance * Math.sin(angle));

            g2d.fillOval(vmX - vmSize/2, vmY - vmSize/2, vmSize, vmSize);
        }
    }

    /**
     * Draw legend
     */
    private void drawLegend(Graphics2D g2d, Color[] colors, String[] names) {
        int legendX = 20;
        int legendY = getHeight() - 120;

        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Legend:", legendX, legendY);

        for (int i = 0; i < colors.length; i++) {
            int y = legendY + 20 + (i * 25);

            // Draw color box
            g2d.setColor(colors[i]);
            g2d.fillRect(legendX, y - 10, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(legendX, y - 10, 15, 15);

            // Draw label
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString(names[i], legendX + 25, y + 2);
        }
    }

    /**
     * Draw performance metrics
     */
    private void drawPerformanceMetrics(Graphics2D g2d) {
        int metricsX = getWidth() - 250;
        int metricsY = 60;

        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Zone Metrics:", metricsX, metricsY);

        if (zoneManager != null) {
            for (int i = 0; i < Math.min(3, zoneManager.getZones().size()); i++) {
                ResourceZone zone = zoneManager.getZone(i);
                if (zone != null) {
                    int y = metricsY + 25 + (i * 60);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 11));
                    g2d.drawString("Zone " + i + ":", metricsX, y);
                    g2d.drawString("VMs: " + zone.getVMCount(), metricsX, y + 15);
                    g2d.drawString("Load: " + String.format("%.1f", zone.getCurrentLoad()), metricsX, y + 30);
                }
            }
        }
    }

    /**
     * Update visualization
     */
    public void updateVisualization() {
        repaint();
    }

    /**
     * Reset visualization
     */
    public void reset() {
        repaint();
    }

    public void setZoneManager(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }
}