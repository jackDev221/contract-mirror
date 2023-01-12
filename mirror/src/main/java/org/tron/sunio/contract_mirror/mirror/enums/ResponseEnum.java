package org.tron.sunio.contract_mirror.mirror.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseEnum implements IResponseEnum {
    SUCCESS("success", "success"),
    SERVER_ERROR("server_error", "server error");
    private final String code;
    private final String message;
}
