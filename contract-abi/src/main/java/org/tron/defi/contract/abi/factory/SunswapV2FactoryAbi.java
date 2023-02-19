package org.tron.defi.contract.abi.factory;

import lombok.Getter;
import org.tron.defi.contract.abi.*;

import java.util.HashMap;
import java.util.Map;

public class SunswapV2FactoryAbi extends Contract {
    public SunswapV2FactoryAbi(ContractTrigger trigger, String address) {
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
        FEE_TO("feeTo", "", "address"),
        FEE_TO_SETTER("feeToSetter", "", "address"),
        GET_PAIR("getPair", "address,address", "address"),
        ALL_PAIRS("allPairs", "uint256", "address"),
        ALL_PAIRS_LENGTH("allPairsLength", "", "uint256");
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
        PAIR_CREATED("PairCreated", "address indexed,address indexed,address,uint256");
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
