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
import java.util.*;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Cursor;
import javax.swing.border.LineBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;


public class INHOUSEMANAGEMENT {

    // add DEBUG flag for optional logging
    private static final boolean DEBUG = false;

    // make models and status UI components instance fields so helper methods can update them
    private DefaultListModel<DeliveryDriver> driversModel;
    private DefaultTableModel deliveriesModel;
    private JLabel status;
    private JButton createJobBtn; // ensure we use instance field
    private HashMap<String, DeliveryDriver> driversMap;

    public INHOUSEMANAGEMENT(){
        // Initialize GUI components here
        JFrame frame = new JFrame("Delivery System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(900, 600));

        // Gradient background panel (make look like ADMINMANAGEMENT)
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

        // Models (assign to fields)
        driversModel = new DefaultListModel<>();
        deliveriesModel = new DefaultTableModel(
                new String[]{"ID", "ITEMS", "ADDRESS", "DRIVER", "TOTAL", "STATUS"}, 0
        );

        // Controls — use styled buttons and assign instance field (no shadow)
        JButton backBtn = createStyledButton("Back", new Color(231, 76, 60));
        this.createJobBtn = createStyledButton("Create Delivery Job", new Color(52, 152, 219));
        JButton syncBtn = createStyledButton("Sync From Supabase", new Color(52, 152, 219));

        // JList now holds DeliveryDriver instances; toString() controls display
        JList<DeliveryDriver> driverList = new JList<>(driversModel);
        driverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        driverList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        driverList.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
        driverList.setBackground(Color.WHITE);

        JTable deliveriesTable = new JTable(deliveriesModel);
        deliveriesTable.setFillsViewportHeight(true);
        deliveriesTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        deliveriesTable.setRowHeight(24);

        JScrollPane driversScroll = new JScrollPane(driverList);
        driversScroll.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
        JScrollPane deliveriesScroll = new JScrollPane(deliveriesTable);
        deliveriesScroll.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));

        // Top toolbar (transparent to show gradient)
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        toolbar.add(backBtn);
        toolbar.add(this.createJobBtn);
        toolbar.add(syncBtn);

        // Split pane: drivers | deliveries
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, driversScroll, deliveriesScroll);
        split.setResizeWeight(0.25);
        split.setBorder(null);

        // Status bar (white text like ADMINMANAGEMENT)
        status = new JLabel("Ready");
        status.setForeground(Color.WHITE);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setOpaque(false);
        statusBar.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        statusBar.add(status, BorderLayout.WEST);

        // Assemble main panel
        mainPanel.add(toolbar, BorderLayout.NORTH);
        mainPanel.add(split, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Actions
        createJobBtn.addActionListener(e -> {
            status.setText("Loading orders and drivers...");
            runAsync(() -> {
                try {
                    fetchAllFromSupabase();
                    var drivers = new ArrayList<DeliveryDriver>();
                    // fetch orders (ensure we include customer_id)
                    HttpResponse<String> ordersResp = SupabaseClient.get("orders?select=order_id,customer_id,created_at,items,order_total,status", null);
                    List<Order> orders = new ArrayList<>();
                    if (ordersResp.statusCode() >= 200 && ordersResp.statusCode() < 300) {
                        var orderObjs = parseJsonArray(ordersResp.body());
                        for (var o : orderObjs) {
                            String oid = extractJsonString(o, "order_id");
                            String customerId = extractJsonString(o, "customer_id");
                            String createdAt = extractJsonString(o, "created_at");
                            String items = extractJsonString(o, "items");
                            String total = extractJsonString(o, "order_total");
                            String statusField = extractJsonString(o, "status");
                            if (oid == null || oid.isEmpty()) continue;
                            Order order = new Order(oid, customerId, createdAt, items, total, statusField);
                            orders.add(order);
                        }
                    }

                    // fetch drivers (unchanged)
                    updateDriversMap();
                    var drvObjs = driversMap.values();
                    for (var d : drvObjs) {
                        if (findNumJobs(d.getDriverId()) >= 3) continue;
                        drivers.add(d);
                    }


                    // show UI on EDT to let user pick
                    SwingUtilities.invokeLater(() -> {
                        status.setText("Select order and driver...");
                        if (orders.isEmpty()) {
                            JOptionPane.showMessageDialog(null, "No orders available.", "No orders", JOptionPane.INFORMATION_MESSAGE);
                            status.setText("No orders to create job from.");
                            return;
                        }
                        if (drivers.isEmpty()) {
                            JOptionPane.showMessageDialog(null, "No drivers available.", "No drivers", JOptionPane.INFORMATION_MESSAGE);
                            status.setText("No drivers available.");
                            return;
                        }

                        JComboBox<INHOUSEMANAGEMENT.Order> orderCombo = new JComboBox<>();
                        for (INHOUSEMANAGEMENT.Order ord : orders) orderCombo.addItem(ord);

                        JComboBox<DeliveryDriver> driverCombo = new JComboBox<>();
                        for (DeliveryDriver drv : drivers) driverCombo.addItem(drv);

                        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
                        form.add(new JLabel("Choose Order (items / total / created):"));
                        form.add(orderCombo);
                        form.add(new JLabel("Assign Driver:"));
                        form.add(driverCombo);

                        int res = JOptionPane.showConfirmDialog(null, form, "Create Delivery Job",
                                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                        if (res != JOptionPane.OK_OPTION) {
                            status.setText("Create job cancelled.");
                            return;
                        }

                        INHOUSEMANAGEMENT.Order selOrder = (INHOUSEMANAGEMENT.Order) orderCombo.getSelectedItem();
                        DeliveryDriver selDriver = (DeliveryDriver) driverCombo.getSelectedItem();
                        if (selOrder == null || selDriver == null) {
                            JOptionPane.showMessageDialog(null, "Both order and driver must be selected.",
                                    "Validation", JOptionPane.WARNING_MESSAGE);
                            status.setText("Selection invalid.");
                            return;
                        }

                        // Resolve customer info (name + address) and create/save job off the EDT
                        status.setText("Resolving customer info...");
                        runAsync(() -> {

                            Map<String, String> custInfo = new HashMap<>();
                            custInfo.put("name", "");
                            custInfo.put("address", "");
                            if (selOrder.customerId != null && !selOrder.customerId.isEmpty()) {
                                custInfo = fetchCustomerInfo(selOrder.customerId);
                            }
                            final String customerName = custInfo.get("name");
                            final String deliveryAddress = custInfo.get("address");
                            final String itemsDesc = selOrder.items != null ? selOrder.items : "";
                            final String totalAmount = selOrder.total != null ? selOrder.total : "0";
                            final String jobId = "JOB" + selOrder.id;

                            // Update UI with new job row on EDT
                            SwingUtilities.invokeLater(() -> {
                                deliveriesModel.addRow(new Object[]{jobId, itemsDesc, deliveryAddress, selDriver.getDriverId() + " - " + selDriver.getName(), totalAmount, "Pending"});
                                status.setText("Created job " + jobId + " (customer: " + customerName + ")");
                            });

                            // Persist job to Supabase (include delivery_date)
                            try {
                                LocalDate ddate = LocalDate.now();
                                DeliveryJob job = new DeliveryJob(jobId, customerName, deliveryAddress, itemsDesc,
                                        Double.parseDouble(totalAmount.isEmpty() ? "0" : totalAmount), ddate);
                                boolean assigned = job.assignDriver(selDriver.getDriverId());
                                // save (upsert) to Supabase
                                boolean ok = job.saveToSupabase();
                                SwingUtilities.invokeLater(() -> status.setText(ok ? "Saved job to Supabase: " + jobId
                                        : "Failed saving job to Supabase: " + jobId));
                            } catch (Exception ex) {
                                SwingUtilities.invokeLater(() -> status.setText("Error saving job: " + ex.getMessage()));
                            }
                        });
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> status.setText("Error loading data: " + ex.getMessage()));
                }
            });

        });
        // Sync button: delegate to common fetch method
        syncBtn.addActionListener(e -> fetchAllFromSupabase());
        // Back button -> open Staff login and close this window
        backBtn.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                try { new STAFFLOG(); } catch (Throwable t) { /* ignore */ }
            });
            SwingUtilities.invokeLater(() -> {
                Window w = SwingUtilities.getWindowAncestor(toolbar);
                if (w != null) w.dispose();
            });
        });

        // initial fetch on startup (off the EDT)
        runAsync(this::fetchAllFromSupabase);
    }
    private int findNumJobs(String id) throws IOException, InterruptedException {
        if (id == null || id.isEmpty()) {
            return -1;
        }
        return Math.toIntExact(fetchJobs().stream().filter(j -> Objects.equals(j.d_id, id)).count());


    }


    // helper: fetch customer street,city,parish AND customer name (first+last), return both
    private Map<String, String> fetchCustomerInfo(String customerId) {
        Map<String, String> result = new HashMap<>();
        if (customerId == null || customerId.isBlank()) {
            System.out.println("Customer ID is null or empty — skipping request.");
            return result;
        }
        result.put("name", "");
        result.put("address", "");
        try {
            String q = "customers?select=first_name,last_name,street,city,parish&id=eq." + URLEncoder.encode(customerId, StandardCharsets.UTF_8);
            HttpResponse<String> resp = SupabaseClient.get(q, null);
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                var objs = parseJsonArray(resp.body());
                if (!objs.isEmpty()) {
                    var obj = objs.get(0);
                    String firstName = extractJsonString(obj, "first_name");
                    String lastName = extractJsonString(obj, "last_name");
                    String street = extractJsonString(obj, "street");
                    String city = extractJsonString(obj, "city");
                    String parish = extractJsonString(obj, "parish");

                    // Combine name
                    StringBuilder nameSb = new StringBuilder();
                    if (firstName != null && !firstName.isEmpty()) nameSb.append(firstName.trim());
                    if (lastName != null && !lastName.isEmpty()) {
                        if (!nameSb.isEmpty()) nameSb.append(" ");
                        nameSb.append(lastName.trim());
                    }
                    result.put("name", nameSb.toString());

                    // Combine address
                    StringBuilder addrSb = new StringBuilder();
                    if (street != null && !street.isEmpty()) addrSb.append(street.trim());
                    if (city != null && !city.isEmpty()) {
                        if (addrSb.length() > 0) addrSb.append(", ");
                        addrSb.append(city.trim());
                    }
                    if (parish != null && !parish.isEmpty()) {
                        if (addrSb.length() > 0) addrSb.append(", ");
                        addrSb.append(parish.trim());
                    }
                    result.put("address", addrSb.toString());
                }
            }
        } catch (Exception ex) {
            if (DEBUG) System.out.println("fetchCustomerInfo error: " + ex.getMessage());
        }
        return result;
    }

    // Replace the Order class with schema-aligned fields
    private record Order(String id, String customerId, String createdAt, String items, String total, String status) {
        @Override
        public String toString() {
            String t = (total != null && !total.isEmpty()) ? ("$" + total) : "(no total)";
            String c = (createdAt != null && !createdAt.isEmpty()) ? createdAt : "(no date)";
            String it = (items != null && !items.isEmpty()) ? items : "(no items)";
            String st = (status != null && !status.isEmpty()) ? status : "(unknown)";
            return id + " — " + t + " — " + c + " — " + st + " — " + (it.length() > 60 ? it.substring(0,60) + "…" : it);
        }
    }

    private void updateDriversMap() throws IOException, InterruptedException {
        driversMap = new HashMap<>();
        HttpResponse<String> resp = SupabaseClient.get("delivery_drivers?select=*", null);
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            String body = resp.body();
            var objs = parseJsonArray(body);
            for (var obj : objs) {
                System.out.println(obj.toString());
                String id = extractJsonString(obj, "driver_id");
                String name = extractJsonString(obj, "name");
                String contact = extractJsonString(obj, "contact_number");
                String license = extractJsonString(obj, "license_number");
                String vehicle = extractJsonString(obj, "vehicle_info");
                boolean available = "true".equalsIgnoreCase(extractJsonString(obj, "is_available")) || "1".equals(extractJsonString(obj, "is_available"));
                // If driver_id is missing or empty, skip this record — do not fabricate a driver id.
                if (id == null || id.isEmpty()) {
                    // optional: log to console for debugging
                    if (DEBUG) System.out.println("Skipping driver record without driver_id: " + obj);
                    continue;
                }
                System.out.println(id);
                DeliveryDriver dd = new DeliveryDriver(id, (name != null && !name.isEmpty()) ? name : id, contact, license, vehicle);
                dd.setAvailable(available);
                driversMap.put(dd.getDriverId(), dd);
            }
        }
    }

    // Fetch drivers then jobs from Supabase and populate models.
    private void fetchAllFromSupabase() {
        SwingUtilities.invokeLater(() -> status.setText("Syncing from Supabase..."));
        runAsync(() -> {
            try {
                // 1) Fetch drivers (all columns)
                updateDriversMap();

                // Update driversModel on EDT (replace entire list)
                SwingUtilities.invokeLater(() -> {
                    driversModel.clear();
                    for (DeliveryDriver dd : driversMap.values()) driversModel.addElement(dd);
                });

                // 2) Fetch jobs (all columns), then resolve assigned_driver_id -> display
                var jobs = fetchJobs();
                SwingUtilities.invokeLater(() -> {
                    deliveriesModel.setRowCount(0);
                    for (var job: jobs){
                        deliveriesModel.addRow(new Object[]{job.id, job.goods, job.address, job.display, job.total, job.status});
                    }
                });


                SwingUtilities.invokeLater(() -> status.setText("Sync complete"));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> status.setText("Error syncing: " + ex.getMessage()));
            }
        });
    }
    private record FetchedJob(
            String id,
            String goods,
            String address,
            String display,
            String d_id,
            String total,
            String status

    ){};

    private List<FetchedJob> fetchJobs() throws IOException, InterruptedException {
        HttpResponse<String> r2 = SupabaseClient.get("delivery_jobs?select=*", null);
        ArrayList<FetchedJob> results = new ArrayList<>();
        if (r2.statusCode() >= 200 && r2.statusCode() < 300) {
            String body2 = r2.body();
            var jobs = parseJsonArray(body2);
            System.out.println(body2);
            for (var o : jobs) {
                String jobId = extractJsonString(o, "job_id");
                String goodsDesc = extractJsonString(o, "goods_description");
                String deliveryAddr = extractJsonString(o, "delivery_address");
                String assigned = extractJsonString(o, "assigned_driver_id");
                String totalAmount = extractJsonString(o, "total_amount");
                String statusToken = extractJsonString(o, "status");

                System.out.println("Job Delivery Id: " + jobId);
                System.out.println(assigned);
                String driverDisplay = driversMap.get(assigned).toFriendlyString();
                System.out.println(driverDisplay);

                final String jId = jobId != null ? jobId : ("JOB-" + System.currentTimeMillis());
                final String goods = goodsDesc != null ? goodsDesc : "";
                final String addr = deliveryAddr != null ? deliveryAddr : "";
                final String disp = driverDisplay;
                final String total = String.format("%.2f",Float.parseFloat(totalAmount != null ? totalAmount : "0"));
                final String st = statusToken != null ? statusToken : "Unknown";
                var j = new FetchedJob(jId, goods, addr, disp, assigned, total, st);
                results.add(j);

            }
        }
        return results;
    }

    // Helper: run a task off the EDT
    private void runAsync(Runnable r) {
        new Thread(r, "gui-net").start();
    }

    // Very small JSON helpers (assumes Supabase returns an array of flat objects)
    private ArrayList<JSONObject> parseJsonArray(String jsonString) {
        var array = new JSONArray(jsonString);
        var list = new ArrayList<JSONObject>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getJSONObject(i));
        }
        return list;
    }

    // extract string value for a key inside a single object text (not full JSON parser)
    // extract string value for a key inside a single object text (not full JSON parser)
    private String extractJsonString(JSONObject json, String key) {
        return json.get(key).toString();
    }

    // Add styled button helper to match INHOUSEMANAGEMENT look
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

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    public static void main(String[] args) {
        // Ensure GUI runs on the EDT
        SwingUtilities.invokeLater(INHOUSEMANAGEMENT::new);
    }
}