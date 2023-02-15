package org.tron.defi.contract_mirror.utils.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.defi.contract_mirror.config.TronConfig;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.sunapi.ErrorCodeEnum;
import org.tron.sunapi.SunNetwork;
import org.tron.sunapi.SunNetworkResponse;
import org.tron.sunapi.request.TriggerConstantContractRequest;
import org.tron.sunapi.response.TransactionResponse;

@Slf4j
@Component
public class ContractTrigger {
    @Autowired
    TronConfig tronConfig;
    @Autowired
    SunNetwork sunNetwork;

    public long getTrxBalance(String address) {
        try {
            SunNetworkResponse<Account> accountSunNetworkResponse = sunNetwork.getMainChainService()
                                                                              .getAccount(address);
            if (accountSunNetworkResponse != null && accountSunNetworkResponse.getData() != null) {
                return accountSunNetworkResponse.getData().getBalance();
            }
        } catch (Throwable e) {
            log.error("trigger error", e);
        }
        return 0;
    }


    public String triggerConstant(String contractAddress, String functionSelector, String param) {
        try {
            TriggerConstantContractRequest constantRequest = new TriggerConstantContractRequest();
            constantRequest.setContractAddrStr(contractAddress);
            constantRequest.setMethodStr(functionSelector);
            constantRequest.setArgsStr(param);
            constantRequest.setHex(false);
            constantRequest.setFeeLimit(tronConfig.getFeeLimit());
            SunNetworkResponse<TransactionResponse> constantResponse
                = sunNetwork.getMainChainService().triggerConstantContract(constantRequest);
            if (constantResponse == null ||
                !constantResponse.getCode().equals(ErrorCodeEnum.SUCCESS.getCode()) ||
                constantResponse.getData() == null ||
                constantResponse.getData().getConstantCode() != code.SUCESS) {
                log.error("trigger constant error, request:{} response:{}",
                          constantRequest,
                          constantResponse.getData());
                throw new RuntimeException();
            }
            return constantResponse.getData().constantResult;
        } catch (Throwable e) {
            log.error("trigger error", e);
            throw e;
        }
    }

    public String triggerConstant(String contractAddress, String functionSelector) {
        return triggerConstant(contractAddress, functionSelector, "");
    }
}
