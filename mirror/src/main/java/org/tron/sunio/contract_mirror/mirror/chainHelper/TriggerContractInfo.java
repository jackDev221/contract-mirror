package org.tron.sunio.contract_mirror.mirror.chainHelper;

import cn.hutool.core.util.HexUtil;
import lombok.Data;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.utils.Numeric;

import java.util.List;

@Data
public class TriggerContractInfo {
    private String fromAddress;
    private String contractAddress;
    private String methodName;
    private List<Type> inputParameters;
    private List<TypeReference<?>> outputParameters;


    public TriggerContractInfo() {
    }

    public TriggerContractInfo(String fromAddress, String contractAddress, String methodName, List<Type> inputParameters,
                               List<TypeReference<?>> outputParameters) {
        this.fromAddress = fromAddress;
        this.contractAddress = contractAddress;
        this.methodName = methodName;
        this.inputParameters = inputParameters;
        this.outputParameters = outputParameters;

    }

    public byte[] getCallData() {
        Function function = new Function(methodName, inputParameters, outputParameters);
        return HexUtil.decodeHex(Numeric.cleanHexPrefix(FunctionEncoder.encode(function)));
    }

    public Function getCallFunction() {
        return new Function(methodName, inputParameters, outputParameters);
    }

    @Override
    public String toString() {
        return "TriggerContractInfo{" +
                "fromAddress='" + fromAddress + '\'' +
                ", contractAddress='" + contractAddress + '\'' +
                ", methodName='" + methodName + '\'' +
                ", inputParameters=" + inputParameters +
                ", outputParameters=" + outputParameters +
                '}';
    }
}
