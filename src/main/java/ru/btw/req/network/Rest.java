package ru.btw.req.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Rest {
    public static HttpResponse<String> get(String url, String cookie) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url));
        if (!cookie.isEmpty()) {
            requestBuilder = requestBuilder.header("Cookie", cookie);
        }
        HttpResponse<String> response = null;
        try {
            response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            System.out.println("response body: " + response.body());
            return response;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse<String> post(String url, String body, String cookie) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (!cookie.isEmpty()) {
            requestBuilder = requestBuilder.header("Cookie", cookie);
        }
        HttpResponse<String> response = null;
        try {
            response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            System.out.println("response body: " + response.body());
            return response;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
