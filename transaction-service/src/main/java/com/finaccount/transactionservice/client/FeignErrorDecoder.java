package com.finaccount.transactionservice.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FeignErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.resolve(response.status());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return switch (status) {
            case BAD_REQUEST -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account service bad request: " + response.reason());
            case NOT_FOUND -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + response.reason());
            default -> new ResponseStatusException(status, "Account service error: " + response.reason());
        };
    }
}