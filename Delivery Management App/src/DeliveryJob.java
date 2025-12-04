import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.net.http.HttpResponse;

public class DeliveryJob {
    private String jobId;
    private String customerName;
    private String deliveryAddress;
    private String goodsDescription;
    private double totalAmount;
    private String assignedDriverId;
    private String status;
    private LocalDate deliveryDate;

    // Constructor matching delivery_jobs table schema (with deliveryDate)
    public DeliveryJob(String jobId, String customerName, String deliveryAddress,
                       String goodsDescription, double totalAmount, LocalDate deliveryDate) {
        this.jobId = jobId;
        this.customerName = customerName;
        this.deliveryAddress = deliveryAddress;
        this.goodsDescription = goodsDescription;
        this.totalAmount = totalAmount;
        this.assignedDriverId = "";
        this.status = "Pending";
        this.deliveryDate = deliveryDate;
    }

    // assignDriver now returns boolean and marks the job as Assigned
    public boolean assignDriver(String driverId) {
        if (driverId == null || driverId.isEmpty()) return false;
        this.assignedDriverId = driverId;
        this.status = "Assigned";
        return true;
    }

    public boolean saveToSupabase() throws Exception {
        // Build JSON matching delivery_jobs table schema
        String datePart = (deliveryDate != null) ? String.format(",\"delivery_date\":\"%s\"", deliveryDate.format(DateTimeFormatter.ISO_DATE)) : "";
        String json = String.format(
                "{\"job_id\":\"%s\",\"customer_name\":\"%s\",\"delivery_address\":\"%s\"," +
                        "\"goods_description\":\"%s\",\"total_amount\":%f,\"assigned_driver_id\":\"%s\",\"status\":\"%s\"%s}",
                escapeJsonString(jobId),
                escapeJsonString(customerName),
                escapeJsonString(deliveryAddress),
                escapeJsonString(goodsDescription),
                totalAmount,
                escapeJsonString(assignedDriverId),
                escapeJsonString(status),
                datePart
        );

        HttpResponse<String> resp = SupabaseClient.postUpsert("delivery_jobs", json, "job_id", null);
        return resp.statusCode() >= 200 && resp.statusCode() < 300;
    }

    public LocalDate getDeliveryDate() { return deliveryDate; }

    // Helper to escape JSON strings
    private String escapeJsonString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // Getters
    public String getJobId() { return jobId; }
    public String getCustomerName() { return customerName; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getGoodsDescription() { return goodsDescription; }
    public double getTotalAmount() { return totalAmount; }
    public String getAssignedDriverId() { return assignedDriverId; }
    public String getStatus() { return status; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public void setGoodsDescription(String goodsDescription) { this.goodsDescription = goodsDescription; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
}
