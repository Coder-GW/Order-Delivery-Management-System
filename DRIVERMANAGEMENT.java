import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;



public class DRIVERMANAGEMENT {
    // add DEBUG flag
    private static final boolean DEBUG = false;

    // ...existing fields...
    private String driverId;
    private DefaultListModel<String> jobsListModel;
    private DefaultTableModel deliveriesModel;
    private Map<String, Map<String, String>> jobDetails;
    private JLabel status;
    private JButton UpdateStatusBtn;

    public DRIVERMANAGEMENT(String driverId) {
        this.driverId = driverId;
        // Initialize data structures
        jobsListModel = new DefaultListModel<>();
        jobDetails = new HashMap<>();

        // Create main frame
        JFrame frame = new JFrame("Driver Management - Driver ID: " + driverId);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(900, 600));

        // Gradient background panel (match other UIs)
        JPanel mainPanel = new JPanel(new BorderLayout(10,10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(44, 62, 80),
                        0, getHeight(), new Color(52, 73, 94));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        // Left panel for job list (unchanged)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Jobs"));
        JList<String> jobsList = new JList<>(jobsListModel);
        jobsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobsList.setCellRenderer(new JobListCellRenderer());
        JScrollPane listScrollPane = new JScrollPane(jobsList);
        listScrollPane.setPreferredSize(new Dimension(200, 0));
        listScrollPane.setBorder(new javax.swing.border.LineBorder(Color.LIGHT_GRAY,1));
        leftPanel.add(listScrollPane, BorderLayout.CENTER);

        // Right panel for details (unchanged)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Job Details"));
        String[] columnNames = {"Field", "Value"};
        deliveriesModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable detailsTable = new JTable(deliveriesModel);
        detailsTable.setFillsViewportHeight(true);
        detailsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        detailsTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        JScrollPane detailsScroll = new JScrollPane(detailsTable);
        detailsScroll.setBorder(new javax.swing.border.LineBorder(Color.LIGHT_GRAY,1));
        rightPanel.add(detailsScroll, BorderLayout.CENTER);

        // Toolbar with styled buttons (use helper)
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setOpaque(false);
        JButton backBtn = createStyledButton("Back", new Color(231,76,60));
        JButton updateStatusBtn = createStyledButton("Update Status", new Color(52,152,219));
        JButton syncBtn = createStyledButton("Sync Jobs", new Color(52,152,219));
        toolbar.add(updateStatusBtn);
        toolbar.add(syncBtn);
        toolbar.add(backBtn);

        // Split pane and status bar
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.3);
        splitPane.setBorder(null);

        status = new JLabel("Ready");
        status.setForeground(Color.WHITE);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setOpaque(false);
        statusBar.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));
        statusBar.add(status, BorderLayout.WEST);

        // Assemble and show
        mainPanel.add(toolbar, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        frame.add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Add selection listener to update details when a job is selected
        jobsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedJob = jobsList.getSelectedValue();
                if (selectedJob != null) {
                    updateJobDetails(selectedJob.split(" - ")[0]);
                }
            }
        });

        // Update status button action (unchanged logic)
        updateStatusBtn.addActionListener(e -> {
            String selected = jobsList.getSelectedValue();
            if (selected != null) {
                String jobId = selected.split(" - ")[0];
                String currentStatus = jobDetails.get(jobId).get("status");
                String newStatus = JOptionPane.showInputDialog(frame,
                        "Update status for Job " + jobId + ":", currentStatus);
                if (newStatus != null && !newStatus.trim().isEmpty() && !newStatus.equals(currentStatus)) {
                    updateJobStatus(jobId, newStatus.trim());
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a job to update.",
                        "No Job Selected", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Sync button action
        syncBtn.addActionListener(e -> fetchAllFromSupabase());

        // Back -> open driver login and close this window
        backBtn.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                try { new DRIVERLOG(); } catch (Throwable t) { /* ignore */ }
            });
            SwingUtilities.invokeLater(() -> {
                Window w = SwingUtilities.getWindowAncestor(toolbar);
                if (w != null) w.dispose();
            });
        });

        // initial fetch
        runAsync(this::fetchAllFromSupabase);
    }
    private void updateJobStatus(String jobId, String newStatus) {
        runAsync(() -> {
            try {
                // Update local data first for better UX
                Map<String, String> job = jobDetails.get(jobId);
                if (job != null) {
                    job.put("status", newStatus);
                    SwingUtilities.invokeLater(() -> {
                        // Update the list display
                        updateJobsList();
                        // Update the details if this job is currently selected
                        updateJobDetails(jobId);
                        status.setText("Updated job " + jobId + " to status: " + newStatus);
                    });
                }

                // Then update in Supabase
                HttpClient client = HttpClient.newHttpClient();
                String apiUrl = "https://your-supabase-url/rest/v1/delivery_jobs?job_id=eq." + jobId;
                String requestBody = String.format("{\"status\":\"%s\"}", newStatus);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("apikey", "your-supabase-api-key")
                    .header("Authorization", "Bearer your-supabase-api-key")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 204) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(null,
                                    "Failed to update job status. HTTP Status: " + response.statusCode(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null,
                        "Error updating job status: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }
    private void fetchAllFromSupabase() {
        SwingUtilities.invokeLater(() -> status.setText("Syncing jobs from Supabase..."));
        runAsync(() -> {
            try {
                // Fetch jobs for this driver
                HttpResponse<String> response = SupabaseClient.get(
                    "delivery_jobs?select=*&assigned_driver_id=eq." + driverId, null);

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String body = response.body();
                    List<String> jobs = parseJsonArray(body);

                    // Clear existing data
                    jobDetails.clear();

                    // Parse job data
                    for (String jobData : jobs) {
                        String jobId = extractJsonString(jobData, "job_id");
                        // If job_id is missing, skip the record (do not fabricate)
                        if (jobId == null || jobId.isEmpty()) {
                            if (DEBUG) System.out.println("Skipping job record without job_id: " + jobData);
                            continue;
                        }

                        Map<String, String> job = new HashMap<>();
                        job.put("job_id", jobId);
                        job.put("customer_name", extractJsonString(jobData, "customer_name"));
                        job.put("delivery_address", extractJsonString(jobData, "delivery_address"));
                        job.put("goods_description", extractJsonString(jobData, "goods_description"));
                        job.put("status", extractJsonString(jobData, "status"));
                        job.put("assigned_driver_id", extractJsonString(jobData, "assigned_driver_id"));
                        job.put("created_at", extractJsonString(jobData, "created_at"));

                        jobDetails.put(jobId, job);
                    }

                    // Update UI on the EDT
                    SwingUtilities.invokeLater(this::updateJobsList);
                    status.setText("Synced " + jobDetails.size() + " jobs");
                } else {
                    SwingUtilities.invokeLater(() ->
                        status.setText("Failed to fetch jobs: HTTP " + response.statusCode()));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    status.setText("Error: " + ex.getMessage()));
                ex.printStackTrace();
            }
        });
    }

    private void updateJobsList() {
        jobsListModel.clear();

        // Sort jobs by status and then by ID
        List<Map.Entry<String, Map<String, String>>> sortedJobs = new ArrayList<>(jobDetails.entrySet());
        sortedJobs.sort((a, b) -> {
            int statusCompare = a.getValue().get("status").compareTo(b.getValue().get("status"));
            if (statusCompare != 0) return statusCompare;
            return a.getKey().compareTo(b.getKey());
        });

        // Add jobs to the list model with status
        for (Map.Entry<String, Map<String, String>> entry : sortedJobs) {
            String jobId = entry.getKey();
            String status = entry.getValue().get("status");
            if (status == null) status = "unknown";
            jobsListModel.addElement(String.format("%s - %s", jobId, status));
        }
    }

    private void updateJobDetails(String jobId) {
        deliveriesModel.setRowCount(0); // Clear existing details

        Map<String, String> job = jobDetails.get(jobId);
        if (job == null) return;

        // Add all job details to the table
        addDetailRow("Job ID", job.get("job_id"));
        addDetailRow("Status", job.get("status"));
        addDetailRow("Customer", job.get("customer_name"));
        addDetailRow("Delivery Address", job.get("delivery_address"));
        addDetailRow("Goods Description", job.get("goods_description"));
        addDetailRow("Assigned Driver", job.get("assigned_driver_id"));
        addDetailRow("Created At", job.get("created_at"));
    }

    private void addDetailRow(String field, String value) {
        if (value == null || value.trim().isEmpty()) return;
        deliveriesModel.addRow(new Object[]{field, value});
    }

    // Helper: run a task off the EDT
    private void runAsync(Runnable r) {
        new Thread(r, "driver-mgmt-async").start();
    }

    // Styled button helper to match other UIs
    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { btn.setBackground(color.darker()); }
            public void mouseExited(MouseEvent evt)  { btn.setBackground(color); }
        });
        return btn;
    }

    // Custom cell renderer for job list to show status with colors
    private static class JobListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

            if (value != null) {
                String text = value.toString();

                // Set text color based on status
                if (text.contains("completed")) {
                    label.setForeground(new Color(0, 128, 0)); // Dark green
                } else if (text.contains("in progress") || text.contains("in_progress")) {
                    label.setForeground(new Color(0, 0, 200)); // Blue
                } else if (text.contains("pending")) {
                    label.setForeground(new Color(200, 100, 0)); // Orange
                } else if (text.contains("failed") || text.contains("cancelled")) {
                    label.setForeground(Color.RED);
                }

                // Make the text bold for better visibility
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }

            return label;
        }
    }

    // Very small JSON helpers (assumes Supabase returns an array of flat objects)
    private List<String> parseJsonArray(String json) {
        List<String> out = new ArrayList<>();
        if (json == null) return out;
        String s = json.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length()-1);
        // split by "},{" but keep braces
        int idx = 0;
        StringBuilder cur = new StringBuilder();
        int brace = 0;
        while (idx < s.length()) {
            char c = s.charAt(idx++);
            if (c == '{') brace++;
            if (c == '}') brace--;
            cur.append(c);
            if (brace == 0 && cur.toString().trim().length() > 0) {
                String token = cur.toString().trim();
                if (!token.isEmpty()) out.add(token);
                cur = new StringBuilder();
            }
        }
        return out;
    }

    // extract string value for a key inside a single object text (not full JSON parser)
    private String extractJsonString(String json, String key) {
        if (json == null) return "";
        String look = "\"" + key + "\"";
        int idx = json.indexOf(look);
        if (idx == -1) return "";
        int colon = json.indexOf(":", idx + look.length());
        if (colon == -1) return "";
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return "";
        char c = json.charAt(i);
        if (c == '"') {
            int start = i + 1;
            int end = json.indexOf('"', start);
            if (end == -1) return json.substring(start).trim();
            return json.substring(start, end);
        } else {
            int end = i;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(i, end).trim();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DRIVERMANAGEMENT(args.length > 0 ? args[0] : ""));
    }
}
