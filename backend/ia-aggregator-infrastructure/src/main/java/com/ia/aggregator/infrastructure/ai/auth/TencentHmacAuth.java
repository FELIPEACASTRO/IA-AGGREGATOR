package com.ia.aggregator.infrastructure.ai.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Tencent Cloud TC3-HMAC-SHA256 authentication strategy.
 * Signs requests using the Tencent Cloud API v3 signature scheme.
 *
 * <p>Used by: Tencent Hunyuan LLM API.
 *
 * <p>Thread-safe: each apply() call computes a fresh signature.
 */
public class TencentHmacAuth implements AuthStrategy {

    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final String SERVICE = "hunyuan";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final String secretId;
    private final String secretKey;

    public TencentHmacAuth(String secretId, String secretKey) {
        this.secretId = secretId;
        this.secretKey = secretKey;
    }

    @Override
    public HttpRequest.Builder apply(HttpRequest.Builder builder) {
        Instant now = Instant.now();
        long timestamp = now.getEpochSecond();
        String date = DATE_FMT.format(now);

        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String hashedPayload = sha256Hex(""); // Payload hash computed separately by caller

        String canonicalRequest = "POST\n/\n\n"
                + "content-type:application/json\n"
                + "host:hunyuan.tencentcloudapi.com\n\n"
                + "content-type;host\n"
                + hashedPayload;

        String stringToSign = ALGORITHM + "\n"
                + timestamp + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

        byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = HexFormat.of().formatHex(hmacSha256(secretSigning, stringToSign));

        String authorization = ALGORITHM + " "
                + "Credential=" + secretId + "/" + credentialScope + ", "
                + "SignedHeaders=content-type;host, "
                + "Signature=" + signature;

        return builder
                .header("Authorization", authorization)
                .header("X-TC-Timestamp", String.valueOf(timestamp))
                .header("X-TC-Version", "2023-09-01")
                .header("X-TC-Action", "ChatCompletions");
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }

    private static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 computation failed", e);
        }
    }
}
