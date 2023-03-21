package org.tron.sunio.contract_mirror.mirror.response;

import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.enums.IResponseEnum;
import org.tron.sunio.contract_mirror.mirror.enums.ResponseEnum;

@Slf4j
public class RestResultGenerator {
    public static <T> ResultResponse<T> genResult(T result) {
        ResultResponse<T> response = ResultResponse.newInstance();
        response.setCode(ResponseEnum.SUCCESS.getCode());
        response.setMessage(ResponseEnum.SUCCESS.getMessage());
        response.setResponseEnum(ResponseEnum.SUCCESS);
        response.setData(result);
        log.info("--------> response:{}", JacksonMapper.toJsonString(response));
        if (log.isDebugEnabled()) {
            log.debug("--------> response:{}", JacksonMapper.toJsonString(response));
        }
        return response;
    }

    public static <T> ResultResponse<T> genErrorResult(IResponseEnum responseEnum) {
        ResultResponse<T> response = ResultResponse.newInstance();
        response.setCode(responseEnum.getCode());
        response.setMessage(responseEnum.getMessage());
        response.setResponseEnum(responseEnum);

        if (log.isDebugEnabled()) {
            log.debug("--------> response:{}", JacksonMapper.toJsonString(response));
        }

        return response;
    }

    public static <T> ResultResponse<T> genErrorWithMessage(String msg) {
        ResultResponse<T> response = ResultResponse.newInstance();
        response.setCode(1);
        response.setMessage(msg);
        if (log.isDebugEnabled()) {
            log.debug("--------> response:{}", JacksonMapper.toJsonString(response));
        }

        return response;
    }

    public static <T> ResultResponse<T> genErrorResult(IResponseEnum responseEnum, String message) {
        ResultResponse<T> response = ResultResponse.newInstance();
        response.setCode(responseEnum.getCode());
        response.setMessage(message);
        response.setResponseEnum(responseEnum);

        if (log.isDebugEnabled()) {
            log.debug("--------> response:{}", JacksonMapper.toJsonString(response));
        }

        return response;
    }
}
