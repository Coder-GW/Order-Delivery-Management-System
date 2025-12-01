import java.io.IOException;
import java.net.http.HttpResponse;

public class Feedback {
    private String customerID;
    private String feedbackTitle;
    private String feedbackContent;

    public Feedback(String customerID, String feedbackTitle, String feedbackContent) {
        this.customerID = customerID;
        this.feedbackTitle = feedbackTitle;
        this.feedbackContent = feedbackContent;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"customer_id\":\"").append(escapeJson(customerID)).append("\",");
        sb.append("\"title\":\"").append(escapeJson(feedbackTitle)).append("\",");
        sb.append("\"context\":\"").append(escapeJson(feedbackContent)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public boolean saveToSupabase() throws IOException, InterruptedException {
        String body = "[" + toJson() + "]";
        HttpResponse<String> resp = SupabaseClient.postUpsert("feedback", body, null, null);
        return resp.statusCode() >= 200 && resp.statusCode() < 300;
    }
}
