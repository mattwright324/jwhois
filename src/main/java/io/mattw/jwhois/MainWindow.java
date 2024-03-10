package io.mattw.jwhois;

import com.formdev.flatlaf.FlatLaf;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

public class MainWindow extends JFrame {

    private static final Logger logger = LogManager.getLogger();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextArea output, debug;
    private final JTextField addressInput, whoisServerInput;
    private final JButton search, clear;

    private boolean searchRunning = false;

    public MainWindow() throws Exception {
        logger.info("MainWindow()");

        var icon = ImageIO.read(Objects.requireNonNull(getClass().getResource("/icon.png")));

        setIconImage(icon);
        setTitle("WHOIS Lookup");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new MigLayout("fill"));

        var centerX = (Toolkit.getDefaultToolkit().getScreenSize().width - getSize().width) / 2;
        var centerY = (Toolkit.getDefaultToolkit().getScreenSize().height - getSize().height) / 2;
        setLocation(centerX, centerY);

        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#0094FF"));

        addressInput = new JTextField("google.com");
        addressInput.setColumns(9999);

        whoisServerInput = new JTextField("whois.iana.org");
        whoisServerInput.setColumns(9999);

        search = new JButton("Search");
        search.addActionListener((e) -> new Thread(this::doSearch).start());

        clear = new JButton("Clear");
        clear.addActionListener((e) -> new Thread(this::clear).start());

        var topPanel = new JPanel(new MigLayout("fillx"));
        add(topPanel, "dock north");
        topPanel.add(addressInput, "growx");
        topPanel.add(whoisServerInput, "growx");
        topPanel.add(search);
        topPanel.add(clear);

        output = new JTextArea("Results output here");
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        output.setEditable(false);

        debug = new JTextArea("");
        debug.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        debug.setEditable(false);

        var split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.9);
        add(split, "grow");
        split.setTopComponent(new JScrollPane(output));
        split.setBottomComponent(new JScrollPane(debug));

        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
                debugMessage("App started");

                split.setDividerLocation(0.65);
            }

            @Override
            public void windowClosing(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });
    }

    private void debugMessage(String message) {
        logger.debug(message);
        SwingUtilities.invokeLater(() ->
            debug.append(String.format("%s > %s\n",
                    formatter.format(LocalTime.now()),
                    message)));
    }

    private void enabled(boolean enabled, Component... components) {
        SwingUtilities.invokeLater(() -> {
            for (Component c : components) {
                c.setEnabled(enabled);
            }
        });
    }

    private void clear() {
        SwingUtilities.invokeLater(() -> {
            output.setText("Results output here");
            debug.setText("");
            debugMessage("Cleared");
        });
    }

    private void doSearch() {
        if (searchRunning) {
            return;
        }
        searchRunning = true;

        var address = addressInput.getText();
        var whoisServer = whoisServerInput.getText();
        enabled(false, addressInput, whoisServerInput, search, clear);

        debugMessage(String.format("Lookup [%s, %s]", address, whoisServer));

        try {
            var didForward = false;
            do {
                didForward = false;
                try (var socket = new Socket(whoisServer, 43);
                     var writer = new PrintWriter(socket.getOutputStream(), true);
                     var isr = new InputStreamReader(socket.getInputStream());
                     var reader = new BufferedReader(isr)) {

                    // Send request to whois server
                    var verisignPrefix = whoisServer.equalsIgnoreCase("whois.verisign-grs.com") ? "=" : "";
                    writer.println(verisignPrefix + address);

                    // Get lookup response
                    var body = IOUtils.toString(reader);
                    logger.debug("Response [{}, {}] Body={}", address, whoisServer, body);
                    SwingUtilities.invokeLater(() -> {
                        output.setText(body);
                        output.setSelectionStart(0);
                        output.setSelectionEnd(0);
                    });

                    // Check if the response forwards to another whois server
                    var lines = body.split("\\r?\\n", -1);
                    for (var line : lines) {
                        // Ignore comment lines
                        if (line.startsWith("%") || line.startsWith("#")) {
                            continue;
                        }

                        var split = line.trim().toLowerCase().split(":[ ]+");
                        if (split.length != 2 || !isRefererLine(split[0])) {
                            continue;
                        }
                        if (whoisServer.equalsIgnoreCase(split[1]) && !whoisServer.startsWith("http")) {
                            continue;
                        }

                        whoisServer = split[1];
                        didForward = true;

                        debugMessage(String.format("Forward [%s, %s]", address, whoisServer));
                    }
                }
            } while (didForward);

            Thread.sleep(500);
        } catch (Exception e) {
            debugMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
            logger.error(e);
        } finally {
            searchRunning = false;
            enabled(true, addressInput, whoisServerInput, search, clear);
            debugMessage("Done");
        }
    }

    public boolean isRefererLine(String split0) {
        split0 = split0.toLowerCase().trim();
        return split0.startsWith("whois") || // general/iana.org
                split0.equalsIgnoreCase("registrar whois server"); // google.com
    }

}
