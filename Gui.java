import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


public class GUI {

    // make models and status UI components instance fields so helper methods can update them
    private DefaultListModel<DeliveryDriver> driversModel;
    private DefaultTableModel deliveriesModel;
    private JLabel status;

    public GUI(){
        // Initialize GUI components here
        JFrame frame = new JFrame("Delivery System");

        // Models (assign to fields)
        driversModel = new DefaultListModel<>();
        deliveriesModel = new DefaultTableModel(
                new String[]{"ID", "Pickup", "Dropoff", "Driver", "Status"}, 0
        );

        // Controls
        JButton addDriverBtn = new JButton("Add Driver");
        JButton deleteDriverBtn= new JButton("Delete Driver");
        JButton editDriverBtn = new JButton("Edit Driver");
        JButton createJobBtn = new JButton("Create Delivery Job");
        JButton syncBtn = new JButton("Sync From Supabase");

        // JList now holds DeliveryDriver instances; toString() controls display
        JList<DeliveryDriver> driverList = new JList<>(driversModel);
        driverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        driverList.setVisibleRowCount(10);

        JTable deliveriesTable = new JTable(deliveriesModel);
        deliveriesTable.setFillsViewportHeight(true);

        // Top toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.add(addDriverBtn);
        toolbar.add(deleteDriverBtn);
        toolbar.add(editDriverBtn);
        toolbar.add(createJobBtn);
        toolbar.add(syncBtn); // add sync button to toolbar
        toolbar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Split pane: drivers | deliveries
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(driverList),
                new JScrollPane(deliveriesTable));
        split.setResizeWeight(0.25);

        // Status bar
        status = new JLabel("Ready");
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        statusBar.add(status, BorderLayout.WEST);

        // Main frame layout
        frame.setLayout(new BorderLayout());
        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(split, BorderLayout.CENTER);
        frame.add(statusBar, BorderLayout.SOUTH);

        // Frame settings
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(900, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Actions
        addDriverBtn.addActionListener(e -> {
            JPanel form = new JPanel(new GridLayout(0,1,4,4));
            JTextField idField = new JTextField();
            JTextField nameField = new JTextField();
            JTextField contactField = new JTextField();
            JTextField licenseField = new JTextField();
            JTextField vehicleField = new JTextField();
            form.add(new JLabel("Driver ID#:")); form.add(idField);
            form.add(new JLabel("Name:")); form.add(nameField);
            form.add(new JLabel("Phone:")); form.add(contactField);
            form.add(new JLabel("License #:")); form.add(licenseField);
            form.add(new JLabel("Vehicle:")); form.add(vehicleField);

            int result = JOptionPane.showConfirmDialog(frame, form, "Add Driver",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Name is required.", "Validation",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // create DeliveryDriver and add the object to the model (keeps id, contact etc.)
                String driverIdFinal = "DRV" + idField.getText().trim();
                String license = "DL" + licenseField.getText().trim();
                DeliveryDriver d = new DeliveryDriver(driverIdFinal, name, contactField.getText().trim(),
                        licenseField.getText().trim(), vehicleField.getText().trim());
                driversModel.addElement(d);
                status.setText("Added driver: " + name + " (" + driverIdFinal + ")");

                clearDriverFields(idField, nameField, contactField, licenseField, vehicleField); // Clear fields after adding

                // generate password, encrypt it, show plaintext once and push to Supabase
                runAsync(() -> {
                    try {
                        String plaintextPwd = null;
                        try {
                            plaintextPwd = d.generateAndSetPassword(); // generates & sets encryptedPassword
                        } catch (Exception encEx) {
                            // encryption failed; proceed without password
                            System.err.println("Password generation/encryption failed: " + encEx.getMessage());
                        }

                        boolean ok = d.saveToSupabase();
                        final boolean savedOk = ok;
                        final String shownPwd = plaintextPwd;
                        SwingUtilities.invokeLater(() -> {
                            status.setText(savedOk ? "Saved driver to Supabase: " + driverIdFinal
                                    : "Failed saving driver to Supabase");
                            if (shownPwd != null) {
                                // show one-time dialog with plaintext password for admin to copy
                                JOptionPane.showMessageDialog(null,
                                        "Generated password for driver " + name + ":\n\n" + shownPwd +
                                                "\n\nStore it securely and share it with the driver once.",
                                        "Driver Password (one-time)", JOptionPane.INFORMATION_MESSAGE);
                            }
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> status.setText("Error saving to Supabase: " + ex.getMessage()));
                    }
                });
            }
        });
        deleteDriverBtn.addActionListener(e -> {
            DeliveryDriver selected = driverList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(frame, "Please select a driver to delete.", "No selection",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to delete driver " + selected.getName() + " (" + selected.getDriverId() + ")?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                driversModel.removeElement(selected);
                status.setText("Deleted driver: " + selected.getName());

                // Delete from Supabase in background
                runAsync(() -> {
                    try {
                        boolean ok = selected.deleteFromSupabase();
                        SwingUtilities.invokeLater(() -> status.setText(ok ? "Deleted driver from Supabase: " + selected.getDriverId()
                                : "Failed deleting driver from Supabase"));
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> status.setText("Error deleting from Supabase: " + ex.getMessage()));
                    }
                });
            }
        });

        editDriverBtn.addActionListener(e -> {
            DeliveryDriver selected = driverList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(frame, "Please select a driver to edit.", "No selection",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JPanel form = new JPanel(new GridLayout(0,1,4,4));
            JTextField nameField = new JTextField(selected.getName());
            JTextField contactField = new JTextField(selected.getContactNumber());
            JTextField licenseField = new JTextField(selected.getLicenseNumber());
            JTextField vehicleField = new JTextField(selected.getVehicleInfo());
            form.add(new JLabel("Name:")); form.add(nameField);
            form.add(new JLabel("Phone:")); form.add(contactField);
            form.add(new JLabel("License #:")); form.add(licenseField);
            form.add(new JLabel("Vehicle:")); form.add(vehicleField);

            int result = JOptionPane.showConfirmDialog(frame, form, "Edit Driver",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Name is required.", "Validation",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // Update selected driver fields
                selected.setName(name);
                selected.setContactNumber(contactField.getText().trim());
                selected.setLicenseNumber(licenseField.getText().trim());
                selected.setVehicleInfo(vehicleField.getText().trim());
                driverList.repaint(); // refresh display
                status.setText("Updated driver: " + name);

                // Save updates to Supabase in background
                runAsync(() -> {
                    try {
                        boolean ok = selected.saveToSupabase();
                        SwingUtilities.invokeLater(() -> status.setText(ok ? "Updated driver in Supabase: " + selected.getDriverId()
                                : "Failed updating driver in Supabase"));
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> status.setText("Error updating Supabase: " + ex.getMessage()));
                    }
                });
            }
        });

        createJobBtn.addActionListener(e -> {
            if (driversModel.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please add a driver first.", "No drivers",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JPanel form = new JPanel(new GridLayout(0,1,4,4));
            JTextField pickup = new JTextField();
            JTextField dropoff = new JTextField();
            // use JComboBox<DeliveryDriver> so the selected item contains driverId
            JComboBox<DeliveryDriver> driverCombo = new JComboBox<>();
            for (int i = 0; i < driversModel.size(); i++) driverCombo.addItem(driversModel.get(i));
            form.add(new JLabel("Pickup address:")); form.add(pickup);
            form.add(new JLabel("Dropoff address:")); form.add(dropoff);
            form.add(new JLabel("Assign driver:")); form.add(driverCombo);

            int result = JOptionPane.showConfirmDialog(frame, form, "Create Delivery Job",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String p = pickup.getText().trim();
                String d = dropoff.getText().trim();
                DeliveryDriver drvObj = (DeliveryDriver) driverCombo.getSelectedItem();
                String drv = drvObj != null ? (drvObj.getDriverId() + " - " + drvObj.getName()) : "";
                if (p.isEmpty() || d.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Pickup and dropoff are required.", "Validation",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Simple ID generation and add to table
                String id = "JOB-" + (deliveriesModel.getRowCount() + 1);
                deliveriesModel.addRow(new Object[]{id, p, d, drv, "Pending"});
                status.setText("Created job " + id + " (" + (drvObj != null ? drvObj.getName() : "unassigned") + ")");
                clearJobFields(pickup, dropoff); // Clear fields after creating job

                // Create DeliveryJob and push to Supabase in background (use minimal fields)
                DeliveryJob job = new DeliveryJob(id, p /*customerName*/, d /*deliveryAddress*/, /*goods*/"", 0.0, 0.0);
                 // attempt to set a deliveryDate (null here) or leave as not scheduled
                 runAsync(() -> {
                     try {
                         boolean ok = job.saveToSupabase();
                         SwingUtilities.invokeLater(() -> status.setText(ok ? "Saved job to Supabase: " + id
                                 : "Failed saving job to Supabase"));
                     } catch (Exception ex) {
                         SwingUtilities.invokeLater(() -> status.setText("Error saving job to Supabase: " + ex.getMessage()));
                     }
                 });
             }
         });
        // Sync button: delegate to common fetch method
        syncBtn.addActionListener(e -> fetchAllFromSupabase());
        
        // initial fetch on startup (off the EDT)
        runAsync(this::fetchAllFromSupabase);
    }

    // Fetch drivers then jobs from Supabase and populate models.
    private void fetchAllFromSupabase() {
        SwingUtilities.invokeLater(() -> status.setText("Syncing from Supabase..."));
        runAsync(() -> {
            try {
                // 1) Fetch drivers (all columns)
                HttpResponse<String> resp = SupabaseClient.get("delivery_drivers?select=*", null);
                Map<String, DeliveryDriver> driverMap = new HashMap<>();
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    String body = resp.body();
                    List<String> objs = parseJsonArray(body);
                    for (String obj : objs) {
                        String id = extractJsonString(obj, "driver_id");
                        String name = extractJsonString(obj, "name");
                        String contact = extractJsonString(obj, "contact_number");
                        String license = extractJsonString(obj, "license_number");
                        String vehicle = extractJsonString(obj, "vehicle_info");
                        boolean available = "true".equalsIgnoreCase(extractJsonString(obj, "is_available")) || "1".equals(extractJsonString(obj, "is_available"));
                        String realId = (id != null && !id.isEmpty()) ? id : ("DRV-" + System.currentTimeMillis());
                        DeliveryDriver dd = new DeliveryDriver(realId, (name != null && !name.isEmpty()) ? name : realId, contact, license, vehicle);
                        dd.setAvailable(available);
                        driverMap.put(dd.getDriverId(), dd);
                    }
                }

                // Update driversModel on EDT (replace entire list)
                SwingUtilities.invokeLater(() -> {
                    driversModel.clear();
                    for (DeliveryDriver dd : driverMap.values()) driversModel.addElement(dd);
                });

                // 2) Fetch jobs (all columns), then resolve assigned_driver_id -> display
                HttpResponse<String> r2 = SupabaseClient.get("delivery_jobs?select=*", null);
                if (r2.statusCode() >= 200 && r2.statusCode() < 300) {
                    String body2 = r2.body();
                    List<String> jobs = parseJsonArray(body2);
                    SwingUtilities.invokeLater(() -> deliveriesModel.setRowCount(0));
                    for (String o : jobs) {
                        String jobId = extractJsonString(o, "job_id");
                        String pickupAddr = extractJsonString(o, "pickup_address"); // if you have pickup_address column
                        if (pickupAddr == null || pickupAddr.isEmpty()) pickupAddr = extractJsonString(o, "delivery_address"); // fallback
                        String dropoffAddr = extractJsonString(o, "dropoff_address");
                        if (dropoffAddr == null) dropoffAddr = "";
                        String assigned = extractJsonString(o, "assigned_driver_id");
                        String statusToken = extractJsonString(o, "status");
                        String driverDisplay = "";
                        if (assigned != null && !assigned.isEmpty()) {
                            DeliveryDriver dd = driverMap.get(assigned);
                            if (dd != null) driverDisplay = dd.getDriverId() + " - " + dd.getName();
                            else driverDisplay = assigned; // show raw id if not resolved
                        }
                        final String jId = jobId != null ? jobId : ("JOB-" + System.currentTimeMillis());
                        final String p = pickupAddr;
                        final String dr = dropoffAddr;
                        final String disp = driverDisplay;
                        final String st = statusToken;
                        SwingUtilities.invokeLater(() -> deliveriesModel.addRow(new Object[]{jId, p, dr, disp, st}));
                    }
                }

                SwingUtilities.invokeLater(() -> status.setText("Sync complete"));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> status.setText("Sync failed: " + ex.getMessage()));
            }
        });
    }

    // Helper: run a task off the EDT
    private void runAsync(Runnable r) {
        new Thread(r, "gui-net").start();
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
 
     private void clearDriverFields(JTextField idField, JTextField nameField, JTextField contactField, JTextField licenseField, JTextField vehicleField) {
         idField.setText("");
         nameField.setText("");
         contactField.setText("");
         licenseField.setText("");
         vehicleField.setText("");
     }

     private void clearJobFields(JTextField pickup, JTextField dropoff) {
         pickup.setText("");
         dropoff.setText("");
     }

     public static void main(String[] args) {
         // Ensure GUI runs on the EDT
         SwingUtilities.invokeLater(GUI::new);
     }
 }