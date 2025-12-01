import java.io.IOException;
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

/*
 Simple Supabase REST helper using Java 11+ HttpClient.
 Expects SUPABASE_URL and SUPABASE_KEY in environment variables.
 If they are not present, this class will attempt to read a .env file
 from the current working directory (or a sensible project path) and
 use values found there.
*/
public class SupabaseClient {
    // values will be initialized in static block (attempt env, then .env)
    private static final String SUPABASE_URL;
    private static final String SUPABASE_KEY;
    private static final boolean DEBUG;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    static {
        String url = System.getenv("SUPABASE_URL");
        String key = System.getenv("SUPABASE_KEY");
        String debugEnv = System.getenv("SUPABASE_DEBUG");

        Map<String, String> envFromFile = new HashMap<>();
        boolean needLoadFile = (url == null || url.trim().isEmpty()) || (key == null || key.trim().isEmpty()) || (debugEnv == null);
        if (needLoadFile) {
            try {
                envFromFile = loadDotEnv();
                if ((url == null || url.trim().isEmpty()) && envFromFile.containsKey("SUPABASE_URL")) {
                    url = envFromFile.get("SUPABASE_URL");
                }
                if ((key == null || key.trim().isEmpty()) && envFromFile.containsKey("SUPABASE_KEY")) {
                    key = envFromFile.get("SUPABASE_KEY");
                }
                if (debugEnv == null && envFromFile.containsKey("SUPABASE_DEBUG")) {
                    debugEnv = envFromFile.get("SUPABASE_DEBUG");
                }
            } catch (Exception e) {
                // ignore — we'll handle missing config in ensureConfigured
            }
        }

        SUPABASE_URL = url != null ? url.trim() : "";
        SUPABASE_KEY = key != null ? key.trim() : "";
        DEBUG = parseBoolean(debugEnv);
    }

    private static boolean parseBoolean(String s) {
        if (s == null) return false;
        s = s.trim().toLowerCase();
        return "1".equals(s) || "true".equals(s) || "yes".equals(s) || "on".equals(s);
    }

    private static void ensureConfigured() {
        if (SUPABASE_URL.isEmpty() || SUPABASE_KEY.isEmpty()) {
            throw new IllegalStateException("SUPABASE_URL and SUPABASE_KEY must be set in the environment");
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
    private static Map<String, String> loadDotEnv() {
        Map<String, String> map = new HashMap<>();
        // candidates: current working dir, and a likely project path
        Path cwd = Paths.get(System.getProperty("user.dir"), ".env");
        Path projectEnv = Paths.get(".env");
        Path[] candidates = new Path[] { cwd, projectEnv };

        for (Path p : candidates) {
            try {
                if (Files.exists(p) && Files.isRegularFile(p)) {
                    List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                    for (String raw : lines) {
                        if (raw == null) continue;
                        String line = raw.trim();
                        if (line.isEmpty()) continue;
                        if (line.startsWith("#") || line.startsWith("//")) continue;
                        // optional "export " prefix
                        if (line.startsWith("export ")) {
                            line = line.substring(7).trim();
                        }
                        int eq = line.indexOf('=');
                        if (eq <= 0) continue;
                        String k = line.substring(0, eq).trim();
                        String v = line.substring(eq + 1).trim();
                        // remove surrounding quotes if present
                        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                            v = v.substring(1, v.length()-1);
                        }
                        if (!k.isEmpty()) {
                            map.put(k, v);
                        }
                    }
                    // stop after first found file
                    if (!map.isEmpty()) return map;
                }
            } catch (Exception ignored) {
            }
        }
        return map;
    }
}
