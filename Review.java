import java.io.IOException;
import java.net.http.HttpResponse;

public class Review {
    private String customerID;
    private String reviewInfo;
    private Double ratingScore;

    public Review(String customerID, String reviewInfo, Double rating) {
        this.customerID = customerID;
        this.reviewInfo = reviewInfo;
        this.ratingScore = rating;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"customer_id\":\"").append(escapeJson(customerID)).append("\",");
        sb.append("\"rating\":\"").append(escapeJson(String.valueOf(ratingScore))).append("\",");
        sb.append("\"review\":\"").append(escapeJson(reviewInfo)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public boolean saveToSupabase() throws IOException, InterruptedException {
        String body = "[" + toJson() + "]";
        HttpResponse<String> resp = SupabaseClient.postUpsert("review", body, null, null);
        return resp.statusCode() >= 200 && resp.statusCode() < 300;
    }
}
