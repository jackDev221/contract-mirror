package org.tron.sunio.contract_mirror.mirror.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.tron.sunio.contract_mirror.mirror.enums.IResponseEnum;

@EqualsAndHashCode(callSuper = false)
@Data
@Getter
public class ResultResponse<T> extends BaseResponse {
    @ApiModelProperty(value = "返回结果")
    private T data;

    @JsonIgnore
    private IResponseEnum responseEnum;

    public void setResponseEnum(IResponseEnum responseEnum) {
        this.responseEnum = responseEnum;
    }


    public void setData(T data) {
        this.data = data;
    }

    public static <T> ResultResponse<T> newInstance() {
        return new ResultResponse<T>();
    }
}
