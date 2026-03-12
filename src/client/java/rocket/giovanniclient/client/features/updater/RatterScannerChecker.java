package rocket.giovanniclient.client.features.updater;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import rocket.giovanniclient.client.util.Utils;

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
        ERROR("API ERROR"),
        OFF("RAT CHECK TURNED OFF");

        private final String label;

        SafetyStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static CompletableFuture<SafetyStatus> checkHash(String sha256) {
        if (sha256 == null || sha256.isEmpty()) {
            Utils.log("RatterScanner: Hash is null or empty");
            return CompletableFuture.completedFuture(SafetyStatus.ERROR);
        }

        Utils.log("RatterScanner: Checking hash: " + sha256.substring(0, 8) + "...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + sha256))
                .header("User-Agent", "GiovanniClient/1.0")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Utils.log("RatterScanner: API response code: " + response.statusCode());

                    if (response.statusCode() != 200) {
                        Utils.log("RatterScanner: Non-200 response: " + response.body());
                        return SafetyStatus.ERROR;
                    }

                    String body = response.body();
                    Utils.log("RatterScanner: Raw API response: " + body);

                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    if (json.has("error")) {
                        Utils.log("RatterScanner: API returned error: " + json.get("error").getAsString());
                        return SafetyStatus.ERROR;
                    }

                    JsonArray results = json.getAsJsonArray("results");
                    if (results == null || results.isEmpty()) {
                        Utils.log("RatterScanner: No results found for hash");
                        return SafetyStatus.UNCHECKED;
                    }

                    // Check ALL results, not just the first one
                    boolean foundMalicious = false;
                    boolean foundSafe = false;

                    for (int i = 0; i < results.size(); i++) {
                        JsonObject result = results.get(i).getAsJsonObject();

                        // Safely get boolean values
                        boolean safe = getBooleanSafe(result, "safe");
                        boolean automatedSafe = getBooleanSafe(result, "automated_safe");
                        boolean malicious = getBooleanSafe(result, "malicious");

                        Utils.log("RatterScanner: Result #" + i + " - safe: " + safe +
                                ", automated_safe: " + automatedSafe + ", malicious: " + malicious);

                        if (malicious) foundMalicious = true;
                        if (safe || automatedSafe) foundSafe = true;
                    }

                    if (foundMalicious) {
                        Utils.log("RatterScanner: Hash flagged as MALICIOUS");
                        return SafetyStatus.MALICIOUS;
                    }

                    if (foundSafe) {
                        Utils.log("RatterScanner: Hash verified as SAFE");
                        return SafetyStatus.VERIFIED_SAFE;
                    }

                    Utils.log("RatterScanner: Hash found but not verified safe");
                    return SafetyStatus.UNCHECKED;
                })
                .exceptionally(ex -> {
                    Utils.log("RatterScanner: Exception during check: " + ex.getMessage());
                    ex.printStackTrace();
                    return SafetyStatus.ERROR;
                });
    }

    // Helper method to safely parse booleans
    private static boolean getBooleanSafe(JsonObject obj, String key) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull() && obj.get(key).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }
}