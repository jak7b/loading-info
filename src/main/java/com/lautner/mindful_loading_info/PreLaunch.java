package com.lautner.mindful_loading_info;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;
import java.util.Optional;

import static javax.swing.UIManager.*;

public class PreLaunch implements PreLaunchEntrypoint {
    static Optional<JFrame> frame = Optional.empty();
    private static final Logger LOGGER = LoggerFactory.getLogger("loading-window");
    private Timer memoryUpdateTimer;
    private boolean minecraftWindowVisible = false;

    private static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    @Override
    public void onPreLaunch() {
        if (isMac()) {
            LOGGER.warn("Cannot open loading window on MacOS due to limitations regarding Java AWT.");
        } else {
            try {
                JLabel memoryInfoLabel = new JLabel();
                this.createAndShowUI(memoryInfoLabel);
                this.startMemoryUpdateTimer(memoryInfoLabel);
            } catch (Exception e) {
                LOGGER.error("Unable to show loading screen.", e);
            }
        }
    }

    private void createAndShowUI(JLabel memoryInfoLabel) throws Exception {
        setLookAndFeel(getSystemLookAndFeelClassName());

        JFrame loadingFrame = new JFrame("Minecraft");
        loadingFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        loadingFrame.setResizable(false);

        URL iconUrl = getClass().getResource("/assets/loading-icon/icon.png");
        if (iconUrl == null) {
            throw new Exception("Icon URL is null");
        }

        ImageIcon icon = new ImageIcon(iconUrl);
        loadingFrame.setIconImage(icon.getImage());

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(256, 40));

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setLayout(new BorderLayout());

        JLabel loadingLabel = new JLabel("Loading Mindful Optimized...");
        mainPanel.add(loadingLabel, BorderLayout.NORTH);
        mainPanel.add(progressBar, BorderLayout.CENTER);
        mainPanel.add(memoryInfoLabel, BorderLayout.SOUTH);

        JTextArea fabricLoadingStateTextArea = new JTextArea();
        fabricLoadingStateTextArea.setEditable(false);
        mainPanel.add(new JScrollPane(fabricLoadingStateTextArea), BorderLayout.EAST);

        loadingFrame.setContentPane(mainPanel);
        loadingFrame.pack();
        loadingFrame.setLocationRelativeTo(null);
        loadingFrame.setVisible(true);
        frame = Optional.of(loadingFrame);

        updateMemoryInfoLabel(memoryInfoLabel);

        loadingFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                minecraftWindowVisible = true;
            }
        });

        new Thread(() -> {
            for (int i = 0; i <= 100; i++) {
                try {
                    Thread.sleep(50);
                    progressBar.setValue(i);
                } catch (InterruptedException ex) {
                    LOGGER.error("Thread sleep interrupted", ex);
                }
            }

            while (!minecraftWindowVisible) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    LOGGER.error("Thread sleep interrupted", ex);
                }
            }

            SwingUtilities.invokeLater(loadingFrame::dispose);
        }).start();
    }

    private void updateMemoryInfoLabel(JLabel memoryInfoLabel) {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long usedMemory = totalMemory - Runtime.getRuntime().freeMemory();
        String memoryText = String.format("Memory: %dMB/%dMB", usedMemory / 1048576L, maxMemory / 1048576L);
        Color aquaColor = new Color(0, 153, 255);
        memoryInfoLabel.setForeground(aquaColor);
        memoryInfoLabel.setText(memoryText);
    }

    private void startMemoryUpdateTimer(JLabel memoryInfoLabel) {
        this.memoryUpdateTimer = new Timer(1000, e -> this.updateMemoryInfoLabel(memoryInfoLabel));
        this.memoryUpdateTimer.start();
    }

    public void onShutdown() {
        if (this.memoryUpdateTimer != null && this.memoryUpdateTimer.isRunning()) {
            this.memoryUpdateTimer.stop();
        }
    }
}