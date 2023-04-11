package org.tron.defi.contract_mirror.utils.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.defi.contract.abi.Contract;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.ContractTrigger;
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
public class TronContractTrigger implements ContractTrigger {
    @Autowired
    TronConfig tronConfig;
    @Autowired
    SunNetwork sunNetwork;

    @Override
    public ContractAbi contractAt(Class<? extends Contract> abi, String address) {
        try {
            return abi.getConstructor(ContractTrigger.class, String.class)
                      .newInstance(this, address);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long balance(String address) {
        try {
            SunNetworkResponse<Account> accountSunNetworkResponse = sunNetwork.getMainChainService()
                                                                              .getAccount(address);
            if (accountSunNetworkResponse != null && accountSunNetworkResponse.getData() != null) {
                return accountSunNetworkResponse.getData().getBalance();
            }
            throw new IllegalArgumentException("NO RESPONSE");
        } catch (Exception e) {
            log.error("trigger error {}", e);
            throw e;
        }
    }

    @Override
    public String trigger(String contractAddress, String functionSelector) {
        return trigger(contractAddress, functionSelector, "");
    }

    @Override
    public String trigger(String contractAddress, String functionSelector, String param) {
        try {
            TriggerConstantContractRequest constantRequest = new TriggerConstantContractRequest();
            constantRequest.setContractAddrStr(contractAddress);
            constantRequest.setMethodStr(functionSelector);
            constantRequest.setArgsStr(param);
            constantRequest.setHex(true);
            constantRequest.setFeeLimit(tronConfig.getFeeLimit());
            SunNetworkResponse<TransactionResponse> constantResponse
                = sunNetwork.getMainChainService().triggerConstantContract(constantRequest);
            // TODO: handle with error code
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
        } catch (Exception e) {
            log.error("trigger error {}", e);
            throw e;
        }
    }
}
