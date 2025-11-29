import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class DeliveryJob {
    private String jobId;
    private String customerName;
    private String deliveryAddress;
    private String goodsDescription;
    private LocalDateTime createdDate;
    private LocalDateTime deliveryDate;
    private String assignedDriverId;
    private double totalAmount;
    private double amountPaid;
    private DeliveryStatus status;

    public enum DeliveryStatus {
        PENDING, CONFIRMED, ASSIGNED, IN_TRANSIT, DELIVERED, CANCELLED
    }

    public DeliveryJob(String jobId, String customerName, String deliveryAddress, 
                      String goodsDescription, double totalAmount, double amountPaid) {
        this.jobId = jobId;
        this.customerName = customerName;
        this.deliveryAddress = deliveryAddress;
        this.goodsDescription = goodsDescription;
        this.totalAmount = totalAmount;
        this.amountPaid = amountPaid;
        this.createdDate = LocalDateTime.now();
        this.status = DeliveryStatus.PENDING;
    }

    // Getters and setters
    public String getJobId() { return jobId; }
    public String getCustomerName() { return customerName; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getGoodsDescription() { return goodsDescription; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public LocalDateTime getDeliveryDate() { return deliveryDate; }
    public String getAssignedDriverId() { return assignedDriverId; }
    public double getTotalAmount() { return totalAmount; }
    public double getAmountPaid() { return amountPaid; }
    public DeliveryStatus getStatus() { return status; }
    public double getBalanceDue() { return totalAmount - amountPaid; }

    public void setDeliveryDate(LocalDateTime deliveryDate) {
        if (deliveryDate != null && !deliveryDate.isBefore(LocalDateTime.now())) {
            this.deliveryDate = deliveryDate;
        }
    }

    public boolean assignDriver(String driverId) {
        if (driverId != null && !driverId.trim().isEmpty()) {
            this.assignedDriverId = driverId.trim();
            this.status = DeliveryStatus.ASSIGNED;
            return true;
        }
        return false;
    }

    public void updateStatus(DeliveryStatus newStatus) {
        if (newStatus != null) {
            this.status = newStatus;
        }
    }

    public boolean isFullyPaid() {
        return amountPaid >= totalAmount;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
        return String.format("%s - %s | %s | %s | $%.2f",
                jobId,
                customerName,
                deliveryDate != null ? deliveryDate.format(formatter) : "Not scheduled",
                status,
                totalAmount);
    }

    public String getDetailedInfo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
        StringBuilder sb = new StringBuilder();
        sb.append("=== Delivery Job Details ===\n");
        sb.append(String.format("Job ID: %s\n", jobId));
        sb.append(String.format("Customer: %s\n", customerName));
        sb.append(String.format("Address: %s\n", deliveryAddress));
        sb.append(String.format("Goods: %s\n", goodsDescription));
        sb.append(String.format("Total Amount: $%.2f\n", totalAmount));
        sb.append(String.format("Amount Paid: $%.2f\n", amountPaid));
        sb.append(String.format("Balance Due: $%.2f\n", getBalanceDue()));
        sb.append(String.format("Status: %s\n", status));
        sb.append(String.format("Assigned Driver: %s\n", 
            assignedDriverId != null ? assignedDriverId : "Not assigned"));
        sb.append(String.format("Created: %s\n", createdDate.format(formatter)));
        sb.append(String.format("Scheduled Delivery: %s\n", 
            deliveryDate != null ? deliveryDate.format(formatter) : "Not scheduled"));
        sb.append("==========================");
        
        return sb.toString();
    }

    // ------------------- Supabase helpers / JSON -------------------
    // Table: delivery_jobs with columns: job_id, customer_name, delivery_address,
    // goods_description, created_date, delivery_date, assigned_driver_id,
    // total_amount, amount_paid, status
    public String toJson() {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"job_id\":\"").append(escapeJson(jobId)).append("\",");
        sb.append("\"customer_name\":\"").append(escapeJson(customerName)).append("\",");
        sb.append("\"delivery_address\":\"").append(escapeJson(deliveryAddress)).append("\",");
        sb.append("\"goods_description\":\"").append(escapeJson(goodsDescription)).append("\",");

        // created_date as offset datetime (include system zone offset)
        String created = "";
        if (createdDate != null) {
            created = OffsetDateTime.of(createdDate, ZoneId.systemDefault().getRules().getOffset(createdDate)).format(dtf);
        }
        sb.append("\"created_date\":").append(created.isEmpty() ? "null" : ("\"" + escapeJson(created) + "\"")).append(",");

        // delivery_date: emit null if not set, otherwise offset datetime string
        if (deliveryDate != null) {
            String delivery = OffsetDateTime.of(deliveryDate, ZoneId.systemDefault().getRules().getOffset(deliveryDate)).format(dtf);
            sb.append("\"delivery_date\":\"").append(escapeJson(delivery)).append("\",");
        } else {
            sb.append("\"delivery_date\":null,");
        }

        // assigned_driver_id: null if not assigned
        if (assignedDriverId != null && !assignedDriverId.isEmpty()) {
            sb.append("\"assigned_driver_id\":\"").append(escapeJson(assignedDriverId)).append("\",");
        } else {
            sb.append("\"assigned_driver_id\":null,");
        }

        sb.append("\"total_amount\":").append(totalAmount).append(",");
        sb.append("\"amount_paid\":").append(amountPaid).append(",");
        sb.append("\"status\":\"").append(escapeJson(status != null ? status.name() : "")).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public boolean saveToSupabase() throws IOException, InterruptedException {
        String body = "[" + toJson() + "]";
        HttpResponse<String> resp = SupabaseClient.postUpsert("delivery_jobs", body, "job_id", null);
        return resp.statusCode() >= 200 && resp.statusCode() < 300;
    }

    public static DeliveryJob fetchFromSupabase(String jobId) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(jobId, StandardCharsets.UTF_8);
        String query = "delivery_jobs?select=*&job_id=eq." + encoded;
        HttpResponse<String> resp = SupabaseClient.get(query, null);
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            String body = resp.body();
            if (body != null && body.trim().startsWith("[") && body.trim().length() > 2) {
                String obj = body.trim();
                obj = obj.substring(1, obj.length()-1).trim();

                // Use raw extractor for numbers/dates (handles quoted or unquoted tokens)
                String customer = extractJsonRaw(obj, "customer_name");
                String address = extractJsonRaw(obj, "delivery_address");
                String goods = extractJsonRaw(obj, "goods_description");

                double total = parseDoubleSafe(extractJsonRaw(obj, "total_amount"));
                double paid  = parseDoubleSafe(extractJsonRaw(obj, "amount_paid"));

                String createdStr = extractJsonRaw(obj, "created_date");
                String deliveryDateStr = extractJsonRaw(obj, "delivery_date");

                DeliveryJob j = new DeliveryJob(jobId, customer, address, goods, total, paid);

                // parse created_date if provided
                if (createdStr != null && !createdStr.isEmpty() && !"null".equalsIgnoreCase(createdStr)) {
                    try {
                        // try offset first
                        OffsetDateTime odt = OffsetDateTime.parse(createdStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        j.createdDate = odt.toLocalDateTime();
                    } catch (DateTimeParseException e1) {
                        try {
                            j.createdDate = LocalDateTime.parse(createdStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (Exception ignored) {}
                    }
                }

                // parse and set delivery date if present
                if (deliveryDateStr != null && !deliveryDateStr.isEmpty() && !"null".equalsIgnoreCase(deliveryDateStr)) {
                    try {
                        // try offset first
                        OffsetDateTime odt = OffsetDateTime.parse(deliveryDateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        j.setDeliveryDate(odt.toLocalDateTime());
                    } catch (DateTimeParseException e1) {
                        try {
                            j.setDeliveryDate(LocalDateTime.parse(deliveryDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        } catch (Exception ignored) {}
                    }
                }

                String assigned = extractJsonRaw(obj, "assigned_driver_id");
                if (assigned != null && !assigned.isEmpty() && !"null".equalsIgnoreCase(assigned)) j.assignDriver(assigned);
                return j;
            }
        }
        return null;
    }

    // Helper: extract raw JSON token value for a key (handles quoted strings, numbers, null)
    private static String extractJsonRaw(String json, String key) {
        String look = "\"" + key + "\"";
        int idx = json.indexOf(look);
        if (idx == -1) return "";
        int colon = json.indexOf(":", idx + look.length());
        if (colon == -1) return "";
        int i = colon + 1;
        // skip whitespace
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

    public boolean deleteFromSupabase() throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(jobId, StandardCharsets.UTF_8);
        String query = "delivery_jobs?job_id=eq." + encoded;
        HttpResponse<String> resp = SupabaseClient.delete(query, null);
        return resp.statusCode() >= 200 && resp.statusCode() < 300;
    }

    private static double parseDoubleSafe(String s) {
        try { 
            if (s == null || s.isEmpty() || "null".equalsIgnoreCase(s)) return 0.0;
            return Double.parseDouble(s);
        } catch (Exception e) { return 0.0; }
    }

    private static String extractJsonString(String json, String key) {
        String look = "\"" + key + "\"";
        int idx = json.indexOf(look);
        if (idx == -1) return "";
        int colon = json.indexOf(":", idx + look.length());
        if (colon == -1) return "";
        int start = json.indexOf("\"", colon);
        if (start == -1) {
            int end = json.indexOf(",", colon);
            if (end == -1) end = json.length();
            return json.substring(colon+1, end).trim().replaceAll("[,\\s\"]", "");
        }
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return "";
        return json.substring(start + 1, end);
    }
}
