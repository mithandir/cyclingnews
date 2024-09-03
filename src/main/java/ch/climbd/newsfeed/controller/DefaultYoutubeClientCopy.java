package ch.climbd.newsfeed.controller;

import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeClient;
import io.github.thoroldvix.api.YtApiV3Endpoint;

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
        String videoId = url.split("=")[1];
        String errorMessage = "Request to YouTube failed.";
        String[] headersArray = this.createHeaders(headers);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).headers(headersArray).build();

        HttpResponse response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException var9) {
            IOException e = var9;
            throw new TranscriptRetrievalException(videoId, errorMessage, e);
        } catch (InterruptedException var10) {
            InterruptedException e = var10;
            Thread.currentThread().interrupt();
            throw new TranscriptRetrievalException(videoId, errorMessage, e);
        }

        if (response.statusCode() != 200) {
            throw new TranscriptRetrievalException(videoId, errorMessage + " Status code: " + response.statusCode());
        } else {
            return (String) response.body();
        }
    }

    public String get(YtApiV3Endpoint endpoint, Map<String, String> params) throws TranscriptRetrievalException {
        String paramsString = this.createParamsString(params);
        String errorMessage = String.format("Request to YouTube '%s' endpoint failed.", endpoint);
        HttpRequest.Builder var10000 = HttpRequest.newBuilder();
        String var10001 = endpoint.url();
        HttpRequest request = var10000.uri(URI.create(var10001 + "?" + paramsString)).build();

        HttpResponse response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException var8) {
            IOException e = var8;
            throw new TranscriptRetrievalException(errorMessage, e);
        } catch (InterruptedException var9) {
            InterruptedException e = var9;
            Thread.currentThread().interrupt();
            throw new TranscriptRetrievalException(errorMessage, e);
        }

        if (response.statusCode() != 200) {
            throw new TranscriptRetrievalException(errorMessage + " Status code: " + response.statusCode());
        } else {
            return (String) response.body();
        }
    }

    private String createParamsString(Map<String, String> params) {
        StringBuilder paramString = new StringBuilder();
        Iterator var3 = params.entrySet().iterator();

        while (var3.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry) var3.next();
            String value = this.formatValue(entry.getValue());
            paramString.append(entry.getKey()).append("=").append(value).append("&");
        }

        paramString.deleteCharAt(paramString.length() - 1);
        return paramString.toString();
    }

    private String formatValue(String value) {
        return value.replaceAll(" ", "%20");
    }

    private String[] createHeaders(Map<String, String> headers) {
        String[] headersArray = new String[headers.size() * 2];
        int i = 0;

        Map.Entry entry;
        for (Iterator var4 = headers.entrySet().iterator(); var4.hasNext(); headersArray[i++] = (String) entry.getValue()) {
            entry = (Map.Entry) var4.next();
            headersArray[i++] = (String) entry.getKey();
        }

        return headersArray;
    }
}

