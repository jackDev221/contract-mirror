package org.tron.sunio.contract_mirror.mirror.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseEnum implements IResponseEnum {
    SUCCESS(0, "SUCCESS"),
    SERVER_ERROR(1, "server error");
    private final int code;
    private final String message;
}
