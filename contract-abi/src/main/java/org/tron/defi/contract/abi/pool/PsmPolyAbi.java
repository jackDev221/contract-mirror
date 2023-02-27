package org.tron.defi.contract.abi.pool;

import lombok.Getter;
import org.tron.defi.contract.abi.*;

import java.util.HashMap;
import java.util.Map;

public class PsmPolyAbi extends Contract {
    public PsmPolyAbi(ContractTrigger trigger, String address) {
        super(trigger, address);
    }

    @Override
    public EventPrototype getEvent(String signature) {
        return null;
    }

    @Override
    public FunctionPrototype getFunction(String signature) {
        return Functions.getBySignature(signature).getPrototype();
    }

    public enum Functions implements IFunction {
        GET_INFO("getInfo", "address", "uint256[]");
        private static final Map<String, Functions> signatureMap = new HashMap<>();

        static {
            for (Functions value : values()) {
                signatureMap.put(value.getPrototype().getRawSignature(), value);
            }
        }

        @Getter
        private final FunctionPrototype prototype;

        Functions(String name, String inputParams, String outputParams) {
            prototype = new FunctionPrototype(name, inputParams, outputParams);
        }

        public static Functions getBySignature(String signature) {
            return signatureMap.getOrDefault(signature, null);
        }
    }
}
