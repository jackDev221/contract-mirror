package org.tron.defi.contract.abi.pool;

import lombok.Getter;
import org.tron.defi.contract.abi.*;

import java.util.HashMap;
import java.util.Map;

public class SunswapV2Abi extends Contract {
    public SunswapV2Abi(ContractTrigger trigger, String address) {
        super(trigger, address);
    }

    @Override
    public FunctionPrototype getFunction(String name) {
        return Functions.getBySignature(name).getPrototype();
    }

    @Override
    public EventPrototype getEvent(String signature) {
        return Events.getBySignature(signature).getPrototype();
    }

    public enum Functions implements IFunction {
        MINIMUM_LIQUIDITY("MINIMUM_LIQUIDITY", "", "uint256"),
        SELECTOR("SELECTOR", "", "bytes4"),
        FACTORY("factory", "", "address"),
        TOKEN0("token0", "", "address"),
        TOKEN1("token1", "", "address"),
        PRICE0_CUMULATIVE_LAST("price0CumulativeLast", "", "uint256"),
        PRICE1_CUMULATIVE_LAST("price1CumulativeLast", "", "uint256"),
        K_LAST("kLast", "", "uint256"),
        GET_RESERVERS("getReserves", "", "uint112,uint112,uint32");
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
        MINT("Mint", "address indexed,uint256,uint256"),
        BURN("Burn", "address indexed,uint256,uint256,address indexed"),
        SWAP("Swap", "address indexed,uint256,uint256,uint256,uint256,address indexed"),
        SYNC("Sync", "uint112,uint112");
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
