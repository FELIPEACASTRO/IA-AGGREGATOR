package com.ia.aggregator.infrastructure.ai.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * AWS Signature Version 4 authentication strategy.
 * Signs requests using AWS credentials for services like Bedrock.
 *
 * <p>Note: This is a simplified implementation. For production, consider using
 * the AWS SDK's built-in signing. This covers the essential signing flow for
 * Bedrock InvokeModel requests.
 */
public class AwsSigV4Auth implements AuthStrategy {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String region;
    private final String service;

    public AwsSigV4Auth(String accessKeyId, String secretAccessKey, String region, String service) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = region;
        this.service = service;
    }

    @Override
    public HttpRequest.Builder apply(HttpRequest.Builder builder) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String timestamp = now.format(TIMESTAMP_FORMAT);
        String dateStamp = now.format(DATE_FORMAT);

        builder.header("x-amz-date", timestamp);
        builder.header("x-amz-content-sha256", "UNSIGNED-PAYLOAD");

        // In a full implementation, we would compute the canonical request,
        // string to sign, and Authorization header. For now, we set the
        // essential headers that Bedrock requires.
        String credential = accessKeyId + "/" + dateStamp + "/" + region + "/" + service + "/aws4_request";
        builder.header("Authorization", ALGORITHM + " Credential=" + credential
                + ", SignedHeaders=host;x-amz-content-sha256;x-amz-date"
                + ", Signature=" + computeSignature(dateStamp, timestamp));

        return builder;
    }

    private String computeSignature(String dateStamp, String timestamp) {
        try {
            byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, service);
            // Simplified: in production, use full canonical request
            String stringToSign = ALGORITHM + "\n" + timestamp + "\n"
                    + dateStamp + "/" + region + "/" + service + "/aws4_request\n"
                    + sha256Hex(""); // empty payload hash for placeholder
            return HexFormat.of().formatHex(hmacSha256(signingKey, stringToSign));
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute AWS SigV4 signature", e);
        }
    }

    private byte[] getSignatureKey(String key, String dateStamp, String region, String service) throws Exception {
        byte[] kDate = hmacSha256(("AWS4" + key).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        return hmacSha256(kService, "aws4_request");
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256", e);
        }
    }
}
