import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import javax.swing.JOptionPane;

/*
 Simple Supabase REST helper using Java 11+ HttpClient.
 Expects SUPABASE_URL and SUPABASE_KEY in environment variables.
 If they are not present, this class will attempt to read a .env file
 from the current working directory (or a sensible project path) and
 use values found there.
*/
public class SupabaseClient {
    // values will be initialized in static block (attempt env, then .env)
    private static final Map<String, String> env=loadDotEnv();
    private static final String SUPABASE_URL = env.get("SUPABASE_URL");
    private static final String SUPABASE_KEY = env.get("SUPABASE_KEY");
    private static final boolean DEBUG = Boolean.parseBoolean(env.get("DEBUG"));

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();



    private static boolean parseBoolean(String s) {
        if (s == null) return false;
        s = s.trim().toLowerCase();
        return "1".equals(s) || "true".equals(s) || "yes".equals(s) || "on".equals(s);
    }

    private static void ensureConfigured() {
        if (SUPABASE_URL.isEmpty() || SUPABASE_KEY.isEmpty()) {
            throw new IllegalStateException("SUPABASE_URL and SUPABASE_KEY must be set in the environment or .env");
        }
        String lower = SUPABASE_URL.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new IllegalArgumentException("SUPABASE_URL appears invalid (missing http/https). Current value: " + SUPABASE_URL);
        }
    }

    // Public helper called by UI startup to show a friendly message if config is invalid
    public static void ensureConfiguredOrShowUI() {
        try {
            ensureConfigured();
        } catch (RuntimeException ex) {
            String msg = "Supabase configuration error:\n" + ex.getMessage()
                    + "\n\nPlease fix the .env file or environment variables.\nFile: c:\\Users\\Miguel\\OneDrive\\Desktop\\SOFTWARE ENGINEERING PROJECT PROPOSAL\\.env";
            JOptionPane.showMessageDialog(null, msg, "Supabase Configuration", JOptionPane.ERROR_MESSAGE);
            // do not rethrow — caller (UI) can continue; any network calls will still fail with clear logs
        }
    }

    private static HttpRequest.Builder baseBuilder(String pathWithQuery) {
        ensureConfigured();
        String base = SUPABASE_URL.endsWith("/") ? SUPABASE_URL.substring(0, SUPABASE_URL.length() - 1) : SUPABASE_URL;
        String url = base + "/rest/v1/" + pathWithQuery;
        return HttpRequest.newBuilder(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    // central send wrapper to log request/response when DEBUG or on error
    private static HttpResponse<String> sendWithLogging(HttpRequest req, String requestBody) throws IOException, InterruptedException {
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (DEBUG || resp.statusCode() >= 300) {
            System.out.println("=== Supabase Request Log ===");
            System.out.println("Method: " + req.method());
            System.out.println("URL   : " + req.uri());
            if (requestBody != null && !requestBody.isEmpty()) {
                System.out.println("Body  : " + requestBody);
            }
            System.out.println("Status: " + resp.statusCode());
            String body = resp.body();
            System.out.println("Resp  : " + (body != null ? body : "<empty>"));
            System.out.println("===========================\n");

            // Helpful hint when PostgREST reports missing table in schema cache (PGRST205)
            if (body != null && body.contains("PGRST205")) {
                System.out.println("Supabase Helper: One or more tables were not found (PGRST205).");
                System.out.println("Run the SQL in: c:\\Users\\Miguel\\OneDrive\\Desktop\\SOFTWARE ENGINEERING PROJECT PROPOSAL\\supabase_table_setup.sql");
                System.out.println("Open your Supabase dashboard → SQL editor → paste and run that file, then re-test.");
            }
        }
        return resp;
    }

    public static HttpResponse<String> get(String pathWithQuery, Map<String, String> extraHeaders) throws IOException, InterruptedException {
        HttpRequest.Builder b = baseBuilder(pathWithQuery).GET();
        if (extraHeaders != null) extraHeaders.forEach(b::header);
        HttpRequest req = b.build();
        return sendWithLogging(req, null);
    }

    public static HttpResponse<String> post(String path, String body, Map<String, String> extraHeaders) throws IOException, InterruptedException {
        HttpRequest.Builder b = baseBuilder(path).POST(HttpRequest.BodyPublishers.ofString(body));
        if (extraHeaders != null) extraHeaders.forEach(b::header);
        HttpRequest req = b.build();
        return sendWithLogging(req, body);
    }

    public static HttpResponse<String> patch(String pathWithQuery, String body, Map<String, String> extraHeaders) throws IOException, InterruptedException {
        HttpRequest.Builder b = baseBuilder(pathWithQuery).method("PATCH", HttpRequest.BodyPublishers.ofString(body));
        if (extraHeaders != null) extraHeaders.forEach(b::header);
        HttpRequest req = b.build();
        return sendWithLogging(req, body);
    }

    public static HttpResponse<String> delete(String pathWithQuery, Map<String, String> extraHeaders) throws IOException, InterruptedException {
        HttpRequest.Builder b = baseBuilder(pathWithQuery).DELETE();
        if (extraHeaders != null) extraHeaders.forEach(b::header);
        HttpRequest req = b.build();
        return sendWithLogging(req, null);
    }

    // Convenience: POST with on_conflict for upsert behavior (table?on_conflict=col1,col2)
    public static HttpResponse<String> postUpsert(String table, String body, String onConflictColumns, Map<String, String> extraHeaders) throws IOException, InterruptedException {
        String path = table;
        if (onConflictColumns != null && !onConflictColumns.isEmpty()) {
            path += "?on_conflict=" + URLEncoder.encode(onConflictColumns, StandardCharsets.UTF_8);
        }
        Map<String, String> headers = extraHeaders != null ? new HashMap<>(extraHeaders) : new HashMap<>();
        // Ask PostgREST/Supabase to merge duplicates and return representation
        headers.put("Prefer", "resolution=merge-duplicates,return=representation");
        return post(path, body, headers);
    }

    // --- Helper: load .env file from working dir or fallback project path ---
    private static Map<String, String> loadDotEnv(){
        System.out.println("=== Load Dot Env ===");
        var props = new Properties();
        try(InputStream is = DeliveryJob.class.getResourceAsStream("resources/.env")){
            if(is == null){
                System.out.println("Cannot load properties file .env");
                return null;
            }
            props.load(is);
        }
        catch(IOException e){
            System.out.println("Cannot load properties file .env");
            return null;
        }
        var result = new HashMap<String, String>();
        result.put("SUPABASE_KEY", props.getProperty("SUPABASE_KEY"));
        result.put("SUPABASE_URL", props.getProperty("SUPABASE_URL"));
        result.put("SUPABASE_DEBUG", "False");
        System.out.println("=== Load Dot Env ===");
        return result;
    }

}
