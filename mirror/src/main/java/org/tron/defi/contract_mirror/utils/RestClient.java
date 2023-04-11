package org.tron.defi.contract_mirror.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class RestClient {
    private final RestTemplate rest;
    private final HttpHeaders headers;

    public RestClient(HttpHeaders headers) {
        this.rest = new RestTemplate();
        if (null == headers) {
            this.headers = new HttpHeaders();
            this.headers.add("Content-Type", "application/json");
            this.headers.add("Accept", "application/json");
        } else {
            this.headers = headers;
        }
    }

    public void delete(String uri) {
        HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
        ResponseEntity<String> responseEntity = rest.exchange(uri,
                                                              HttpMethod.DELETE,
                                                              requestEntity,
                                                              String.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException(responseEntity.getStatusCode().toString());
        }
    }

    public String get(String uri) {
        HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
        ResponseEntity<String> responseEntity = rest.exchange(uri,
                                                              HttpMethod.GET,
                                                              requestEntity,
                                                              String.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            log.error(responseEntity.toString());
            throw new RuntimeException(responseEntity.getStatusCode().toString());
        }
        return responseEntity.getBody();
    }

    public String post(String uri, String json) {
        HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
        ResponseEntity<String> responseEntity = rest.exchange(uri,
                                                              HttpMethod.POST,
                                                              requestEntity,
                                                              String.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException(responseEntity.getStatusCode().toString());
        }
        return responseEntity.getBody();
    }

    public void put(String uri, String json) {
        HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
        ResponseEntity<String> responseEntity = rest.exchange(uri,
                                                              HttpMethod.PUT,
                                                              requestEntity,
                                                              String.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException(responseEntity.getStatusCode().toString());
        }
    }
}
