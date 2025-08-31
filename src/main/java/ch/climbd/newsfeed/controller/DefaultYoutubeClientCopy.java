package ch.climbd.newsfeed.controller;

import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.Map;

/**
 * Added HTTP proxy configuration for Youtube
 */
public final class DefaultYoutubeClientCopy implements YoutubeClient {
    private final HttpClient httpClient;

    DefaultYoutubeClientCopy() {
        this.httpClient = HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress("192.168.2.5", 3128)))
                .build();
    }

    DefaultYoutubeClientCopy(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String get(String url, Map<String, String> headers) throws TranscriptRetrievalException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

        if (headers != null) {
            Iterator<Map.Entry<String, String>> iterator = headers.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }

        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new TranscriptRetrievalException(url, "GET request failed. Status code: " + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranscriptRetrievalException(url, "GET request failed.", e);
        }
    }

    @Override
    public String post(String url, String json) throws TranscriptRetrievalException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new TranscriptRetrievalException(url, "POST request failed. Status code: " + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranscriptRetrievalException(url, "POST request failed.", e);
        }
    }

    @Override
    public String get(String url) throws TranscriptRetrievalException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new TranscriptRetrievalException(url, "GET request failed. Status code: " + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranscriptRetrievalException(url, "GET request failed.", e);
        }
    }
}

