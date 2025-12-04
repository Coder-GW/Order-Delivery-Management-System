import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ADMINDB {
    // Simple Base64 encoding for demo purposes (not secure for production)
    private static String simpleHash(String password) {
        if (password == null) return null;
        return Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean verifyPassword(String inputPassword, String storedPassword) {
        if (inputPassword == null || storedPassword == null) return false;

        // Try to decode storedPassword as Base64, fall back to plain compare if it's not Base64
        try {
            byte[] decoded = Base64.getDecoder().decode(storedPassword);
            String decodedPassword = new String(decoded, StandardCharsets.UTF_8);
            return inputPassword.equals(decodedPassword);
        } catch (IllegalArgumentException e) {
            // storedPassword was not valid Base64; compare directly
            return inputPassword.equals(storedPassword);
        }
    }

    /**
     * Verify admin credentials via Supabase REST API
     * @param adminId The admin username or email
     * @param password The plain text password to verify
     * @return boolean indicating if credentials are valid
     */
    public static boolean verifyAdminCredentials(String adminId, String password) {
        if (adminId == null || adminId.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
        }

        try {
            // Encode the admin ID for URL safety
            String encodedId = URLEncoder.encode(adminId, StandardCharsets.UTF_8.name());

            // Query the adminlog table for the admin user
            String query = "adminlog?select=password&adminid=eq." + encodedId;
            HttpResponse<String> response = SupabaseClient.get(query, null);

            // Check if the request was successful
            if (response.statusCode() != 200) {
                System.err.println("Error querying admin: HTTP " + response.statusCode());
                return false;
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return false;
            }

            // Simple JSON parsing (assuming response is like [{"password":"hash"}])
            Pattern pattern = Pattern.compile("\"password\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(responseBody);

            if (!matcher.find()) {
                return false;
            }

            String storedPassword = matcher.group(1);
            return verifyPassword(password, storedPassword);

        } catch (Exception e) {
            System.err.println("Error verifying admin credentials: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Add a new admin to the database
     * @param adminId The admin username or email
     * @param password The plain text password
     * @return boolean indicating if the operation was successful
     */
    public static boolean addAdmin(String adminId, String password) {
        if (adminId == null || adminId.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
        }

        try {
            // Prepare the admin data
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Prefer", "return=minimal");

            // Store the password as Base64 "hash" (demonstration only)
            String stored = simpleHash(password);

            String jsonBody = String.format("{\"adminid\":\"%s\",\"password\":\"%s\"}",
                    adminId.replace("\"", "\\\""),
                    stored.replace("\"", "\\\""));

            // Insert the new admin
            HttpResponse<String> response = SupabaseClient.post("adminlog", jsonBody, headers);

            // Check if the insert was successful
            return response.statusCode() >= 200 && response.statusCode() < 300;

        } catch (Exception e) {
            System.err.println("Error adding admin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

