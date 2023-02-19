package org.tron.defi.contract.abi.token;

import lombok.Getter;
import org.tron.defi.contract.abi.*;

import java.util.HashMap;
import java.util.Map;

public class TRC20Abi extends Contract {
    public TRC20Abi(ContractTrigger trigger, String address) {
        super(trigger, address);
    }

    @Override
    public FunctionPrototype getFunction(String signature) {
        return Functions.getBySignature(signature).getPrototype();
    }

    @Override
    public EventPrototype getEvent(String signature) {
        // TODO add is needed
        return null;
    }

    public enum Functions implements IFunction {
        SYMBOL("symbol", "", "string"),
        DECIMALS("decimals", "", "uint256");
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
