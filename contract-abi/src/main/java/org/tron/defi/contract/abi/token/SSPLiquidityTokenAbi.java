package org.tron.defi.contract.abi.token;

import lombok.Getter;
import org.tron.defi.contract.abi.*;

import java.util.HashMap;
import java.util.Map;

public class SSPLiquidityTokenAbi extends Contract {
    public SSPLiquidityTokenAbi(ContractTrigger trigger, String address) {
        super(trigger, address);
    }

    @Override
    public EventPrototype getEvent(String signature) {
        // TODO add is needed
        return null;
    }

    @Override
    public FunctionPrototype getFunction(String signature) {
        return Functions.getBySignature(signature).getPrototype();
    }

    public enum Functions implements IFunction {
        TOTAL_SUPPLY("totalSupply", "", "uint256");

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

        static Functions getBySignature(String signature) {
            return signatureMap.getOrDefault(signature, null);
        }
    }
}
