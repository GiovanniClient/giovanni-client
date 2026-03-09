package rocket.giovanniclient.client.features.updater;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class RatterScannerChecker {
    private static final String API_BASE = "https://api.ratterscanner.com/hash/";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public enum SafetyStatus {
        VERIFIED_SAFE("VERIFIED SAFE"),
        MALICIOUS("MALICIOUS / UNSAFE"),
        UNCHECKED("UNCHECKED / PENDING"),
        ERROR("API ERROR");

        private final String label;
        SafetyStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public static CompletableFuture<SafetyStatus> checkHash(String sha256) {
        if (sha256 == null || sha256.isEmpty()) {
            return CompletableFuture.completedFuture(SafetyStatus.ERROR);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + sha256))
                .header("User-Agent", "GiovanniClient-Updater")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) return SafetyStatus.ERROR;

                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    if (json.has("error")) return SafetyStatus.ERROR;

                    JsonArray results = json.getAsJsonArray("results");
                    if (results == null || results.isEmpty()) return SafetyStatus.UNCHECKED;

                    JsonObject result = results.get(0).getAsJsonObject();
                    boolean safe = result.get("safe").getAsBoolean();
                    boolean automatedSafe = result.get("automated_safe").getAsBoolean();
                    boolean malicious = result.get("malicious").getAsBoolean();

                    if (malicious) return SafetyStatus.MALICIOUS;
                    if (safe || automatedSafe) return SafetyStatus.VERIFIED_SAFE;

                    return SafetyStatus.UNCHECKED;
                }).exceptionally(ex -> SafetyStatus.ERROR);
    }
}