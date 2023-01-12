package org.tron.sunio.contract_mirror.mirror.response;

import io.swagger.annotations.ApiModelProperty;

public class BaseResponse {
    public static final String SUCCESS = "success";

    @ApiModelProperty(value = "结果信息")
    private String message;

    @ApiModelProperty(value = "结果编码")
    private String code;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean hasError() {
        return !SUCCESS.equals(code);
    }
}
