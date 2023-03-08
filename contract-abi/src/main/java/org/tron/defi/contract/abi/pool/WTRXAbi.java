package org.tron.defi.contract.abi.pool;

import lombok.Getter;
import org.tron.defi.contract.abi.*;

import java.util.HashMap;
import java.util.Map;

public class WTRXAbi extends Contract {
    public WTRXAbi(ContractTrigger trigger, String address) {
        super(trigger, address);
    }

    @Override
    public EventPrototype getEvent(String signature) {
        // TODO: add events if needed
        return null;
    }

    @Override
    public FunctionPrototype getFunction(String signature) {
        try {
            return WTRXAbi.Functions.getBySignature(signature).getPrototype();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public enum Functions implements IFunction {
        DEPOSIT("deposit", "address,uint256", ""),
        WITHDRAW("withdraw", "address,uint256", "");
        private static final Map<String, WTRXAbi.Functions> signatureMap = new HashMap<>();

        static {
            for (WTRXAbi.Functions value : values()) {
                signatureMap.put(value.getPrototype().getRawSignature(), value);
            }
        }

        @Getter
        private final FunctionPrototype prototype;

        Functions(String name, String inputParams, String outputParams) {
            prototype = new FunctionPrototype(name, inputParams, outputParams);
        }

        public static WTRXAbi.Functions getBySignature(String signature) {
            return signatureMap.getOrDefault(signature, null);
        }
    }
}
