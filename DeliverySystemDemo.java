import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

public class DeliverySystemDemo {
    public static void main(String[] args) {
        // Initialize the delivery manager
        DeliveryManager deliveryManager = new DeliveryManager();

        // Register some drivers
        DeliveryDriver driver1 = new DeliveryDriver(
            "DRV001", 
            "John Doe", 
            "876-555-1234", 
            "DL12345678", 
            "Toyota Hiace - ABC 1234"
        );

        DeliveryDriver driver2 = new DeliveryDriver(
            "DRV002", 
            "Jane Smith", 
            "876-555-5678", 
            "DL87654321", 
            "Nissan NV200 - XYZ 5678"
        );

        deliveryManager.registerDriver(driver1);
        deliveryManager.registerDriver(driver2);

        // Create some delivery jobs
        DeliveryJob job1 = new DeliveryJob(
            "ORD-1001", 
            "Robert Johnson",
            "123 Main St, Kingston",
            "10x 2x4 Lumber, 5x 50kg Cement Bags",
            25000.00,
            10000.00
        );
        job1.setDeliveryDate(LocalDateTime.of(2023, Month.DECEMBER, 15, 10, 0));

        DeliveryJob job2 = new DeliveryJob(
            "ORD-1002",
            "Sarah Williams",
            "456 Oak Ave, Portmore",
            "2x Toilets, 1x Sink, 1x Shower Set",
            45000.00,
            45000.00
        );
        job2.setDeliveryDate(LocalDateTime.of(2023, Month.DECEMBER, 15, 13, 30));

        // Add jobs to the system
        deliveryManager.createDeliveryJob(job1);
        deliveryManager.createDeliveryJob(job2);

        // Display available drivers
        System.out.println("=== Available Drivers ===");
        deliveryManager.getAvailableDrivers().forEach(System.out::println);

        // Assign jobs to drivers
        System.out.println("\n=== Assigning Jobs ===");
        boolean job1Assigned = deliveryManager.assignJobToDriver("ORD-1001", "DRV001");
        boolean job2Assigned = deliveryManager.assignJobToDriver("ORD-1002", "DRV002");
        
        System.out.println("Job 1 assigned: " + job1Assigned);
        System.out.println("Job 2 assigned: " + job2Assigned);

        // Send notifications
        System.out.println("\n=== Sending Notifications ===");
        if (job1Assigned) {
            deliveryManager.sendJobAssignmentNotification(job1, driver1);
        }
        if (job2Assigned) {
            deliveryManager.sendJobAssignmentNotification(job2, driver2);
        }

        // Display driver schedules
        System.out.println("\n=== Driver Schedules ===");
        System.out.println("\n" + driver1.getName() + "'s Schedule:");
        deliveryManager.getDriverSchedule("DRV001", LocalDate.of(2023, Month.DECEMBER, 15))
            .forEach(job -> System.out.println("- " + job));

        System.out.println("\n" + driver2.getName() + "'s Schedule:");
        deliveryManager.getDriverSchedule("DRV002", LocalDate.of(2023, Month.DECEMBER, 15))
            .forEach(job -> System.out.println("- " + job));

        // Update job status
        System.out.println("\n=== Updating Job Status ===");
        deliveryManager.updateJobStatus("ORD-1001", DeliveryJob.DeliveryStatus.IN_TRANSIT);
        deliveryManager.sendDeliveryUpdateToCustomer(job1, "Your delivery is on the way and will arrive shortly.");

        // Complete a delivery
        System.out.println("\n=== Completing a Delivery ===");
        deliveryManager.updateJobStatus("ORD-1001", DeliveryJob.DeliveryStatus.DELIVERED);
        deliveryManager.sendDeliveryUpdateToCustomer(
            job1, 
            "Your delivery has been successfully completed. Thank you for your business!"
        );

        // Display final job status
        System.out.println("\n=== Final Job Status ===");
        System.out.println("ORD-1001 Status: " + job1.getStatus());
        System.out.println("ORD-1002 Status: " + job2.getStatus());
        
        // Show driver details
        System.out.println("\n=== Driver Details ===");
        System.out.println(driver1.getDetailedInfo());

        // --- Supabase Integration Test ---
        System.out.println("\n=== Supabase Integration Test ===");
        try {
            // Test DeliveryDriver save, fetch, delete
            System.out.println("Saving driver1 to Supabase...");
            boolean saved1 = driver1.saveToSupabase();
            System.out.println("Saved: " + saved1);

            System.out.println("Fetching driver1 from Supabase...");
            DeliveryDriver fetchedDriver1 = DeliveryDriver.fetchFromSupabase(driver1.getDriverId());
            System.out.println("Fetched: " + (fetchedDriver1 != null ? fetchedDriver1.getDetailedInfo() : "null"));

            //System.out.println("Deleting driver1 from Supabase...");
            //boolean deleted1 = driver1.deleteFromSupabase();
            //System.out.println("Deleted: " + deleted1);
            
            System.out.println("Saving driver2 to Supabase...");
            boolean saved2 = driver2.saveToSupabase();
            System.out.println("Saved: " + saved2);

            System.out.println("Fetching driver2 from Supabase...");
            DeliveryDriver fetchedDriver2 = DeliveryDriver.fetchFromSupabase(driver2.getDriverId());
            System.out.println("Fetched: " + (fetchedDriver2 != null ? fetchedDriver2.getDetailedInfo() : "null"));

            // Test DeliveryJob save, fetch
            System.out.println("Saving job1 to Supabase...");
            boolean jobSaved1 = job1.saveToSupabase();
            System.out.println("Saved: " + jobSaved1);

            System.out.println("Fetching job1 from Supabase...");
            DeliveryJob fetchedJob1 = DeliveryJob.fetchFromSupabase(job1.getJobId());
            System.out.println("Fetched: " + (fetchedJob1 != null ? fetchedJob1.getDetailedInfo() : "null"));

            System.out.println("Saving job2 to Supabase...");
            boolean jobSaved2 = job2.saveToSupabase();
            System.out.println("Saved: " + jobSaved2);

            System.out.println("Fetching job2 from Supabase...");
            DeliveryJob fetchedJob2 = DeliveryJob.fetchFromSupabase(job2.getJobId());
            System.out.println("Fetched: " + (fetchedJob2 != null ? fetchedJob2.getDetailedInfo() : "null"));

        } catch (Exception e) {
            System.out.println("Supabase test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
