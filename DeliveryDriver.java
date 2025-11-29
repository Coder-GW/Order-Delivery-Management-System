import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.LocalDate;

public class DeliveryDriver {
    private String driverId;
    private String name;
    private String contactNumber;
    private String licenseNumber;
    private String vehicleInfo;
    private List<DeliveryJob> assignedJobs;
    private boolean isAvailable;

    public DeliveryDriver(String driverId, String name, String contactNumber, 
                         String licenseNumber, String vehicleInfo) {
        if (driverId == null || driverId.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver name cannot be null or empty");
        }
        
        this.driverId = driverId.trim();
        this.name = name.trim();
        this.contactNumber = contactNumber != null ? contactNumber.trim() : "";
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim() : "";
        this.vehicleInfo = vehicleInfo != null ? vehicleInfo.trim() : "";
        this.assignedJobs = new ArrayList<>();
        this.isAvailable = true;
    }

    // Getters
    public String getDriverId() { return driverId; }
    public String getName() { return name; }
    public String getContactNumber() { return contactNumber; }
    public String getLicenseNumber() { return licenseNumber; }
    public String getVehicleInfo() { return vehicleInfo; }
    public boolean isAvailable() { return isAvailable; }
    public List<DeliveryJob> getAssignedJobs() { return new ArrayList<>(assignedJobs); }

    // Setters with validation
    public void setName(String name) { 
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
    }

    public void setContactNumber(String contactNumber) { 
        this.contactNumber = contactNumber != null ? contactNumber.trim() : "";
    }

    public void setLicenseNumber(String licenseNumber) { 
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim() : "";
    }

    public void setVehicleInfo(String vehicleInfo) { 
        this.vehicleInfo = vehicleInfo != null ? vehicleInfo.trim() : "";
    }

    public void setAvailable(boolean available) { 
        this.isAvailable = available; 
    }

    // Business logic methods
    public boolean canAcceptJob(LocalDate deliveryDate) {
        if (!isAvailable) {
            return false;
        }
        
        long jobsOnDate = assignedJobs.stream()
            .filter(job -> job.getDeliveryDate() != null 
                    && job.getDeliveryDate().toLocalDate().equals(deliveryDate)
                    && (job.getStatus() == DeliveryJob.DeliveryStatus.ASSIGNED 
                        || job.getStatus() == DeliveryJob.DeliveryStatus.IN_TRANSIT))
            .count();
            
        return jobsOnDate < 3; // Maximum 3 jobs per day
    }

    public boolean assignJob(DeliveryJob job) {
        if (job == null || !isAvailable) {
            return false;
        }

        if (job.getDeliveryDate() != null && 
            !canAcceptJob(job.getDeliveryDate().toLocalDate())) {
            return false;
        }

        if (job.assignDriver(driverId)) {
            assignedJobs.add(job);
            return true;
        }
        return false;
    }

    // ------------------- Supabase helpers / JSON -------------------
    // Note: ensure Supabase table "delivery_drivers" has columns:
    // driver_id, name, contact_number, license_number, vehicle_info, is_available
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"driver_id\":\"").append(escapeJson(driverId)).append("\",");
        sb.append("\"name\":\"").append(escapeJson(name)).append("\",");
        sb.append("\"contact_number\":\"").append(escapeJson(contactNumber)).append("\",");
        sb.append("\"license_number\":\"").append(escapeJson(licenseNumber)).append("\",");
        sb.append("\"vehicle_info\":\"").append(escapeJson(vehicleInfo)).append("\",");
        sb.append("\"is_available\":").append(isAvailable);
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public boolean saveToSupabase() throws IOException, InterruptedException {
        String body = "[" + toJson() + "]";
        HttpResponse<String> resp = SupabaseClient.postUpsert("delivery_drivers", body, "driver_id", null);
        return resp.statusCode() >= 200 && resp.statusCode() < 300;
    }

    public static DeliveryDriver fetchFromSupabase(String driverId) throws IOException, InterruptedException {
        String encodedId = URLEncoder.encode(driverId, StandardCharsets.UTF_8);
        String query = "delivery_drivers?select=*&driver_id=eq." + encodedId;
        HttpResponse<String> resp = SupabaseClient.get(query, null);
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            String body = resp.body();
            if (body != null && body.trim().startsWith("[") && body.trim().length() > 2) {
                String obj = body.trim();
                obj = obj.substring(1, obj.length()-1).trim();
                String nameVal = extractJsonString(obj, "name");
                String contactVal = extractJsonString(obj, "contact_number");
                String licenseVal = extractJsonString(obj, "license_number");
                String vehicleVal = extractJsonString(obj, "vehicle_info");
                String isAvailableVal = extractJsonString(obj, "is_available");
                DeliveryDriver d = new DeliveryDriver(driverId, nameVal, contactVal, licenseVal, vehicleVal);
                boolean available = "true".equalsIgnoreCase(isAvailableVal) || "1".equals(isAvailableVal);
                d.setAvailable(available);
                return d;
            }
        }
        return null;
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

    public boolean deleteFromSupabase() throws IOException, InterruptedException {
        // Delete any delivery_jobs that reference this driver first to avoid FK constraint violations
        String encodedId = URLEncoder.encode(driverId, StandardCharsets.UTF_8);
        String jobsQuery = "delivery_jobs?assigned_driver_id=eq." + encodedId;
        try {
            HttpResponse<String> respJobs = SupabaseClient.delete(jobsQuery, null);
            // allow deletion to proceed even if no jobs existed; logging is handled by SupabaseClient
        } catch (Exception ignored) {
            // ignore and proceed to attempt driver deletion; SupabaseClient logs errors when DEBUG is enabled
        }

        String query = "delivery_drivers?driver_id=eq." + encodedId;
        HttpResponse<String> resp = SupabaseClient.delete(query, null);
        return resp.statusCode() >= 200 && resp.statusCode() < 300;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryDriver that = (DeliveryDriver) o;
        return driverId.equals(that.driverId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(driverId);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s | %s", 
            name, 
            driverId,
            isAvailable ? "Available" : "Unavailable",
            vehicleInfo);
    }

    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Driver Details ===\n");
        sb.append(String.format("Name: %s\n", name));
        sb.append(String.format("ID: %s\n", driverId));
        sb.append(String.format("Contact: %s\n", contactNumber));
        sb.append(String.format("License: %s\n", licenseNumber));
        sb.append(String.format("Vehicle: %s\n", vehicleInfo));
        sb.append(String.format("Status: %s\n", isAvailable ? "Available" : "Unavailable"));
        sb.append(String.format("Assigned Jobs: %d\n", assignedJobs.size()));
        
        if (!assignedJobs.isEmpty()) {
            sb.append("\n--- Assigned Jobs ---\n");
            for (DeliveryJob job : assignedJobs) {
                sb.append(String.format("- %s | %s\n", 
                    job.getJobId(), 
                    job.getStatus()));
            }
        }
        
        sb.append("======================");
        return sb.toString();
    }
}
