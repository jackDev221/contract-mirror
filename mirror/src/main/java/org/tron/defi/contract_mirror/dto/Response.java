package org.tron.defi.contract_mirror.dto;

import lombok.Data;

@Data
public class Response<T> {
    private int code;
    private String message;
    private T data;

    public void setCode(Code code) {
        this.code = code.ordinal();
        message = code.name();
    }

    public enum Code {
        SUCCESS,
        ERROR
    }
}
