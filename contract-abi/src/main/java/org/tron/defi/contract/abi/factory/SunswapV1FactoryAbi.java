package org.tron.defi.contract.abi.factory;

import lombok.Getter;
import org.tron.defi.contract.abi.*;

import java.util.HashMap;
import java.util.Map;

public class SunswapV1FactoryAbi extends Contract {
    public SunswapV1FactoryAbi(ContractTrigger trigger, String address) {
        super(trigger, address);
    }

    @Override
    public FunctionPrototype getFunction(String signature) {
        return Functions.getBySignature(signature).getPrototype();
    }

    @Override
    public EventPrototype getEvent(String signature) {
        return Events.getBySignature(signature).getPrototype();
    }

    public enum Functions implements IFunction {
        EXCHANGE_TEMPLATE("exchangeTemplate", "", "address"),
        TOKEN_COUNT("tokenCount", "", "uint256"),
        FEE_TOO("feeTo", "", "address"),
        FEE_TO_RATE("feeToRate", "", "uint256"),
        GET_EXCHANGE("getExchange", "address", "address"),
        GET_TOKEN("getToken", "address", "address"),
        GET_TOKEN_WITH_ID("getTokenWithId", "uint256", "address");
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

    public enum Events implements IEvent {
        NEW_EXCHANGE("NewExchange", "address indexed,address indexed"),
        NEW_FEE_TO("NewFeeTo", "address"),
        NEW_FEE_RATE("NewFeeRate", "NewFeeRate(uint256)");
        private static final Map<String, Events> signatureMap = new HashMap<>();

        static {
            for (Events value : values()) {
                signatureMap.put(value.getPrototype().getSignature(), value);
            }
        }

        @Getter
        private final EventPrototype prototype;

        Events(String name, String parameters) {
            prototype = new EventPrototype(name, parameters);
        }

        public static Events getBySignature(String signature) {
            return signatureMap.getOrDefault(signature, null);
        }
    }
}
