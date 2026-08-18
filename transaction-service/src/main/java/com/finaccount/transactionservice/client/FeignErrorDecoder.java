package com.finaccount.transactionservice.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FeignErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String s, Response response) {
        return switch (response.status()) {
            default -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, response.reason());
        };
    }
}
