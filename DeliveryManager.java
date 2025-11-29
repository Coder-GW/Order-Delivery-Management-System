import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class DeliveryManager {
    private List<DeliveryJob> deliveryJobs;
    private List<DeliveryDriver> drivers;
    private static final int MAX_JOBS_PER_DRIVER_PER_DAY = 3;

    public DeliveryManager() {
        this.deliveryJobs = new ArrayList<>();
        this.drivers = new ArrayList<>();
    }

    // Job Management
    public boolean createDeliveryJob(DeliveryJob job) {
        if (job == null || isDuplicateJob(job)) {
            return false;
        }
        return deliveryJobs.add(job);
    }

    private boolean isDuplicateJob(DeliveryJob newJob) {
        return deliveryJobs.stream().anyMatch(job -> 
            job.getCustomerName().equalsIgnoreCase(newJob.getCustomerName()) &&
            job.getDeliveryAddress().equalsIgnoreCase(newJob.getDeliveryAddress()) &&
            (job.getDeliveryDate() != null && job.getDeliveryDate().toLocalDate()
                .equals(newJob.getDeliveryDate() != null ? 
                    newJob.getDeliveryDate().toLocalDate() : null))
        );
    }

    public boolean updateJobStatus(String jobId, DeliveryJob.DeliveryStatus newStatus) {
        return findJobById(jobId)
            .map(job -> {
                job.updateStatus(newStatus);
                return true;
            })
            .orElse(false);
    }

    public Optional<DeliveryJob> findJobById(String jobId) {
        return deliveryJobs.stream()
            .filter(job -> job.getJobId().equals(jobId))
            .findFirst();
    }

    // Driver Management
    public boolean registerDriver(DeliveryDriver driver) {
        if (driver == null || driverExists(driver.getDriverId())) {
            return false;
        }
        return drivers.add(driver);
    }

    private boolean driverExists(String driverId) {
        return drivers.stream()
            .anyMatch(d -> d.getDriverId().equals(driverId));
    }

    // Assignment Logic
    public boolean assignJobToDriver(String jobId, String driverId) {
        Optional<DeliveryJob> jobOpt = findJobById(jobId);
        Optional<DeliveryDriver> driverOpt = findDriverById(driverId);

        if (!jobOpt.isPresent() || !driverOpt.isPresent()) {
            return false;
        }

        DeliveryJob job = jobOpt.get();
        DeliveryDriver driver = driverOpt.get();

        if (job.getDeliveryDate() != null && 
            !driver.canAcceptJob(job.getDeliveryDate().toLocalDate())) {
            return false; // Driver has reached daily job limit
        }

        return driver.assignJob(job);
    }

    public Optional<DeliveryDriver> findAvailableDriver(LocalDate deliveryDate) {
        return drivers.stream()
            .filter(DeliveryDriver::isAvailable)
            .filter(driver -> {
                long jobsOnDate = driver.getAssignedJobs().stream()
                    .filter(job -> job.getDeliveryDate() != null && 
                                 job.getDeliveryDate().toLocalDate().equals(deliveryDate))
                    .count();
                return jobsOnDate < MAX_JOBS_PER_DRIVER_PER_DAY;
            })
            .findFirst();
    }

    // Notification System
    public void notifyDriver(DeliveryDriver driver, String message) {
        System.out.println("Sending notification to driver " + driver.getName() + 
                          " (" + driver.getContactNumber() + "): " + message);
    }

    public void notifyCustomer(String customerName, String contactInfo, String message) {
        System.out.println("Sending notification to customer " + customerName + 
                          " (" + contactInfo + "): " + message);
    }

    // Reporting
    public List<DeliveryJob> getPendingDeliveries() {
        return deliveryJobs.stream()
            .filter(job -> job.getStatus() != DeliveryJob.DeliveryStatus.DELIVERED &&
                          job.getStatus() != DeliveryJob.DeliveryStatus.CANCELLED)
            .sorted(Comparator.comparing(job -> job.getDeliveryDate() != null ? 
                    job.getDeliveryDate() : LocalDateTime.MAX))
            .collect(Collectors.toList());
    }

    public List<DeliveryJob> getDriverSchedule(String driverId, LocalDate date) {
        return findDriverById(driverId)
            .map(driver -> driver.getAssignedJobs().stream()
                .filter(job -> job.getDeliveryDate() != null && 
                             job.getDeliveryDate().toLocalDate().equals(date))
                .sorted(Comparator.comparing(DeliveryJob::getDeliveryDate))
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    // Helper methods
    public Optional<DeliveryDriver> findDriverById(String driverId) {
        return drivers.stream()
            .filter(driver -> driver.getDriverId().equals(driverId))
            .findFirst();
    }

    public List<DeliveryDriver> getAvailableDrivers() {
        return drivers.stream()
            .filter(DeliveryDriver::isAvailable)
            .collect(Collectors.toList());
    }

    public List<DeliveryJob> getAllJobs() {
        return new ArrayList<>(deliveryJobs);
    }

    public List<DeliveryDriver> getAllDrivers() {
        return new ArrayList<>(drivers);
    }

    // Notification templates
    public void sendJobAssignmentNotification(DeliveryJob job, DeliveryDriver driver) {
        String message = String.format(
            "New delivery job assigned!\n" +
            "Job ID: %s\n" +
            "Customer: %s\n" +
            "Address: %s\n" +
            "Items: %s\n" +
            "Scheduled: %s\n" +
            "Please acknowledge receipt of this assignment.",
            job.getJobId(),
            job.getCustomerName(),
            job.getDeliveryAddress(),
            job.getGoodsDescription(),
            job.getDeliveryDate() != null ? 
                job.getDeliveryDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")) : 
                "Not scheduled yet"
        );
        notifyDriver(driver, message);
    }

    public void sendDeliveryUpdateToCustomer(DeliveryJob job, String updateMessage) {
        String customerContact = "[Customer Contact Info]";
        String message = String.format(
            "Delivery Update for Order %s\n" +
            "Status: %s\n" +
            "%s\n" +
            "Thank you for choosing our service!",
            job.getJobId(),
            job.getStatus(),
            updateMessage
        );
        notifyCustomer(job.getCustomerName(), customerContact, message);
    }
}
