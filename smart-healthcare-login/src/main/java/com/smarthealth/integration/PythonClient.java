package com.smarthealth.integration;

import com.smarthealth.util.TokenUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Lightweight HTTP client for calling a Python backend service from Java Servlets.
 */
public class PythonClient {
    private static final String DEFAULT_PYTHON_API_URL = "http://localhost:8000";
    private static final String DEFAULT_STREAMLIT_URL = "http://localhost:8501";

    private final HttpClient httpClient;
    private final String baseUrl;

    public PythonClient() {
        this(resolvePythonApiUrl());
    }

    public PythonClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public static String resolvePythonApiUrl() {
        String env = System.getenv("SMARTHEALTH_PYTHON_API_URL");
        return (env != null && !env.isBlank()) ? env.trim() : DEFAULT_PYTHON_API_URL;
    }

    public static String resolveStreamlitUrl() {
        String env = System.getenv("SMARTHEALTH_STREAMLIT_URL");
        return (env != null && !env.isBlank()) ? env.trim() : DEFAULT_STREAMLIT_URL;
    }

    /**
     * Sends an authenticated POST JSON request to the Python service.
     */
    public PythonResponse postJson(String path, String jsonBody, String signedToken)
            throws IOException, InterruptedException {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + normalizedPath))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-SmartHealth-Token", signedToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new PythonResponse(response.statusCode(), response.body());
    }

    /**
     * Convenience method to request a sample health triage / analysis check from Python.
     */
    public PythonResponse analyzeSymptoms(String symptoms, String signedToken)
            throws IOException, InterruptedException {
        String payload = "{\"symptoms\":\"" + TokenUtil.escapeJson(symptoms) + "\"}";
        return postJson("/analyze", payload, signedToken);
    }

    public record PythonResponse(int statusCode, String body) {
        public boolean isOk() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
