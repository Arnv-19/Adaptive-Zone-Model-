package org.cloudsim.adaptive.zone.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import org.cloudsim.adaptive.zone.core.ZoneManager;
import org.cloudsim.adaptive.zone.core.ZoneMetrics;

/**
 * Panel displaying real-time metrics for each zone
 */
public class MetricsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private ZoneManager zoneManager;
    private List<JProgressBar> cpuBars;
    private List<JProgressBar> ramBars;
    private List<JProgressBar> storageBars;
    private List<JLabel> vmCountLabels;
    private List<JLabel> migrationLabels;

    public MetricsPanel(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
        initializeComponents();
    }

    /**
     * Initialize GUI components
     */
    private void initializeComponents() {
        setLayout(new GridLayout(3, 1, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        cpuBars = new ArrayList<>();
        ramBars = new ArrayList<>();
        storageBars = new ArrayList<>();
        vmCountLabels = new ArrayList<>();
        migrationLabels = new ArrayList<>();

        // Create panels for each zone
        for (int i = 0; i < 3; i++) {
            JPanel zonePanel = createZoneMetricsPanel(i);
            add(zonePanel);
        }
    }

    /**
     * Create metrics panel for a specific zone
     */
    private JPanel createZoneMetricsPanel(int zoneId) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Zone " + zoneId + " - " + getZoneName(zoneId)));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // VM Count
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("VM Count:"), gbc);
        gbc.gridx = 1;
        JLabel vmLabel = new JLabel("0");
        vmCountLabels.add(vmLabel);
        panel.add(vmLabel, gbc);

        // CPU Usage
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Avg CPU:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JProgressBar cpuBar = new JProgressBar(0, 3000);
        cpuBar.setStringPainted(true);
        cpuBars.add(cpuBar);
        panel.add(cpuBar, gbc);

        // RAM Usage
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Avg RAM:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JProgressBar ramBar = new JProgressBar(0, 4000);
        ramBar.setStringPainted(true);
        ramBars.add(ramBar);
        panel.add(ramBar, gbc);

        // Storage Usage
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Avg Storage:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JProgressBar storageBar = new JProgressBar(0, 50000);
        storageBar.setStringPainted(true);
        storageBars.add(storageBar);
        panel.add(storageBar, gbc);

        // Migration Count
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Migrations:"), gbc);
        gbc.gridx = 1;
        JLabel migrationLabel = new JLabel("0");
        migrationLabels.add(migrationLabel);
        panel.add(migrationLabel, gbc);

        return panel;
    }

    /**
     * Update metrics display
     */
    public void updateMetrics() {
        if (zoneManager == null) return;

        for (int i = 0; i < zoneManager.getZones().size() && i < 3; i++) {
            ZoneMetrics metrics = zoneManager.getZoneMetrics(i);
            if (metrics != null) {
                // Update VM count
                vmCountLabels.get(i).setText(String.valueOf(metrics.getVMCount()));

                // Update CPU bar
                int cpuValue = (int) metrics.getAverageCPU();
                cpuBars.get(i).setValue(cpuValue);
                cpuBars.get(i).setString(cpuValue + " MIPS");

                // Update RAM bar
                int ramValue = (int) metrics.getAverageRAM();
                ramBars.get(i).setValue(ramValue);
                ramBars.get(i).setString(ramValue + " MB");

                // Update Storage bar
                int storageValue = (int) metrics.getAverageStorage();
                storageBars.get(i).setValue(storageValue);
                storageBars.get(i).setString(storageValue + " MB");

                // Update migration count
                migrationLabels.get(i).setText(String.valueOf(metrics.getMigrationCount()));
            }
        }

        repaint();
    }

    /**
     * Reset metrics display
     */
    public void reset() {
        for (int i = 0; i < 3; i++) {
            if (i < vmCountLabels.size()) vmCountLabels.get(i).setText("0");
            if (i < cpuBars.size()) {
                cpuBars.get(i).setValue(0);
                cpuBars.get(i).setString("0 MIPS");
            }
            if (i < ramBars.size()) {
                ramBars.get(i).setValue(0);
                ramBars.get(i).setString("0 MB");
            }
            if (i < storageBars.size()) {
                storageBars.get(i).setValue(0);
                storageBars.get(i).setString("0 MB");
            }
            if (i < migrationLabels.size()) migrationLabels.get(i).setText("0");
        }
        repaint();
    }

    /**
     * Get zone name by ID
     */
    private String getZoneName(int zoneId) {
        switch (zoneId) {
            case 0: return "High-Resource & Slow";
            case 1: return "Medium";
            case 2: return "Fast & Small VM";
            default: return "Zone " + zoneId;
        }
    }

    public void setZoneManager(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }
}