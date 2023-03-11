package org.tron.defi.contract_mirror.utils;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class RestClient {
    private final String server;
    private final RestTemplate rest;
    private final HttpHeaders headers;
    private HttpStatus status;

    public RestClient(String server, HttpHeaders headers) {
        this.server = server;
        this.rest = new RestTemplate();
        if (null == headers) {
            this.headers = new HttpHeaders();
            this.headers.add("Content-Type", "application/json");
            this.headers.add("Accept", "*/*");
        } else {
            this.headers = headers;
        }
    }

    public void delete(String uri) {
        HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
        ResponseEntity<String> responseEntity = rest.exchange(server + uri,
                                                              HttpMethod.DELETE,
                                                              requestEntity,
                                                              String.class);
        this.setStatus(responseEntity.getStatusCode());
    }

    public String get(String uri) {
        HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
        ResponseEntity<String> responseEntity = rest.exchange(server + uri,
                                                              HttpMethod.GET,
                                                              requestEntity,
                                                              String.class);
        this.setStatus(responseEntity.getStatusCode());
        return responseEntity.getBody();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String post(String uri, String json) {
        HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
        ResponseEntity<String> responseEntity = rest.exchange(server + uri,
                                                              HttpMethod.POST,
                                                              requestEntity,
                                                              String.class);
        this.setStatus(responseEntity.getStatusCode());
        return responseEntity.getBody();
    }

    public void put(String uri, String json) {
        HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
        ResponseEntity<String> responseEntity = rest.exchange(server + uri,
                                                              HttpMethod.PUT,
                                                              requestEntity,
                                                              String.class);
        this.setStatus(responseEntity.getStatusCode());
    }
}
