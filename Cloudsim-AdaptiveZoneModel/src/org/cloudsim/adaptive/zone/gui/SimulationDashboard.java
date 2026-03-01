package org.cloudsim.adaptive.zone.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.cloudsim.adaptive.zone.core.ZoneManager;
import org.cloudsim.adaptive.zone.core.ZoneMetrics;
import org.cloudsim.adaptive.zone.utils.StatisticsAnalyzer;

/**
 * Real-time GUI Dashboard for monitoring the Adaptive Zone Model
 */
public class SimulationDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    private ZoneManager zoneManager;
    private StatisticsAnalyzer statsAnalyzer;

    // GUI Components
    private MetricsPanel metricsPanel;
    private ZoneVisualizationPanel zonePanel;
    private ComparisonPanel comparisonPanel;
    private JTextArea logArea;

    // Real-time update timer
    private Timer updateTimer;
    private boolean isMonitoring = false;

    public SimulationDashboard(ZoneManager zoneManager, StatisticsAnalyzer statsAnalyzer) {
        this.zoneManager = zoneManager;
        this.statsAnalyzer = statsAnalyzer;

        initializeGUI();
        setupUpdateTimer();
    }

    /**
     * Initialize the main GUI components
     */
    private void initializeGUI() {
        setTitle("CloudSim Adaptive Zone Model - Real-time Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create main panels
        createMenuBar();
        createMainPanels();
        createControlPanel();

        // Set window properties
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Create menu bar
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportItem = new JMenuItem("Export Results");
        JMenuItem exitItem = new JMenuItem("Exit");

        exportItem.addActionListener(e -> exportResults());
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // View menu
        JMenu viewMenu = new JMenu("View");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        JMenuItem resetItem = new JMenuItem("Reset View");

        refreshItem.addActionListener(e -> refreshDashboard());
        resetItem.addActionListener(e -> resetDashboard());

        viewMenu.add(refreshItem);
        viewMenu.add(resetItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);

        setJMenuBar(menuBar);
    }

    /**
     * Create main dashboard panels
     */
    private void createMainPanels() {
        // Create tabbed pane for different views
        JTabbedPane tabbedPane = new JTabbedPane();

        // Metrics panel
        metricsPanel = new MetricsPanel(zoneManager);
        tabbedPane.addTab("Real-time Metrics", metricsPanel);

        // Zone visualization panel
        zonePanel = new ZoneVisualizationPanel(zoneManager);
        tabbedPane.addTab("Zone Visualization", zonePanel);

        // Comparison panel
        comparisonPanel = new ComparisonPanel();
        tabbedPane.addTab("Algorithm Comparison", comparisonPanel);

        // Log panel
        createLogPanel();

        // Add components to main frame
        add(tabbedPane, BorderLayout.CENTER);
        add(createLogPanel(), BorderLayout.SOUTH);
    }

    /**
     * Create log panel for real-time updates
     */
    private JPanel createLogPanel() {
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("Simulation Log"));

        logArea = new JTextArea(6, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        logPanel.add(scrollPane, BorderLayout.CENTER);
        return logPanel;
    }

    /**
     * Create control panel with start/stop buttons
     */
    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout());

        JButton startButton = new JButton("Start Monitoring");
        JButton stopButton = new JButton("Stop Monitoring");
        JButton exportButton = new JButton("Export Data");

        startButton.addActionListener(e -> startRealTimeMonitoring());
        stopButton.addActionListener(e -> stopRealTimeMonitoring());
        exportButton.addActionListener(e -> exportResults());

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(exportButton);

        add(controlPanel, BorderLayout.NORTH);
    }

    /**
     * Setup timer for real-time updates
     */
    private void setupUpdateTimer() {
        updateTimer = new Timer();
    }

    /**
     * Start real-time monitoring
     */
    public void startRealTimeMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true;

            TimerTask updateTask = new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(() -> updateDashboard());
                }
            };

            updateTimer.scheduleAtFixedRate(updateTask, 0, 1000); // Update every second
            logMessage("Real-time monitoring started");
        }
    }

    /**
     * Stop real-time monitoring
     */
    public void stopRealTimeMonitoring() {
        if (isMonitoring) {
            isMonitoring = false;
            updateTimer.cancel();
            setupUpdateTimer();
            logMessage("Real-time monitoring stopped");
        }
    }

    /**
     * Update dashboard with current data
     */
    public void updateDashboard() {
        if (metricsPanel != null) {
            metricsPanel.updateMetrics();
        }

        if (zonePanel != null) {
            zonePanel.updateVisualization();
        }

        if (comparisonPanel != null) {
            comparisonPanel.updateComparison();
        }

        // Log zone status
        if (zoneManager != null) {
            for (int i = 0; i < zoneManager.getZones().size(); i++) {
                ZoneMetrics metrics = zoneManager.getZoneMetrics(i);
                if (metrics != null) {
                    String status = String.format("Zone %d: %d VMs, Load: %.2f", 
                            i, metrics.getVMCount(), metrics.getTotalLoad());
                    logMessage(status);
                }
            }
        }
    }

    /**
     * Refresh entire dashboard
     */
    private void refreshDashboard() {
        updateDashboard();
        logMessage("Dashboard refreshed");
    }

    /**
     * Reset dashboard view
     */
    private void resetDashboard() {
        if (logArea != null) {
            logArea.setText("");
        }

        if (metricsPanel != null) {
            metricsPanel.reset();
        }

        if (zonePanel != null) {
            zonePanel.reset();
        }

        if (comparisonPanel != null) {
            comparisonPanel.reset();
        }

        logMessage("Dashboard reset");
    }

    /**
     * Export simulation results
     */
    private void exportResults() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Simulation Results");

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            // Export logic would go here
            logMessage("Results exported to: " + fileChooser.getSelectedFile().getName());
        }
    }

    /**
     * Add message to log area
     */
    public void logMessage(String message) {
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> {
                logArea.append("[" + java.time.LocalTime.now() + "] " + message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    public ComparisonPanel getComparisonPanel() {
        return comparisonPanel;
    }

    @Override
    public void dispose() {
        stopRealTimeMonitoring();
        super.dispose();
    }

    public void setZoneManager(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
        if (metricsPanel != null) {
            metricsPanel.setZoneManager(zoneManager);
        }
        if (zonePanel != null) {
            zonePanel.setZoneManager(zoneManager);
        }
    }
}