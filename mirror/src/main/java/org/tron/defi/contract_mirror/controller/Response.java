package org.tron.defi.contract_mirror.controller;

import lombok.Data;

@Data
public class Response {
    private Code code;
    private String Content;

    public enum Code {
        SUCCESS,
        ERROR
    }
}
