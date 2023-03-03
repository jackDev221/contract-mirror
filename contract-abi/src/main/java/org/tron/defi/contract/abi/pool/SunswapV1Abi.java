package org.tron.defi.contract.abi.pool;

import lombok.Getter;
import org.tron.defi.contract.abi.*;

import java.util.HashMap;
import java.util.Map;

public class SunswapV1Abi extends Contract {
    public SunswapV1Abi(ContractTrigger trigger, String address) {
        super(trigger, address);
    }

    @Override
    public EventPrototype getEvent(String signature) {
        try {
            return Events.getBySignature(signature).getPrototype();
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public FunctionPrototype getFunction(String signature) {
        try {
            return Functions.getBySignature(signature).getPrototype();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public enum Functions implements IFunction {
        NAME("name", "", "string"),
        SYMBOLS("symbol", "", "string"),
        DECIMALS("decimals", "", "uint256"),
        K_LAST("kLast", "", "uint256"),
        BALANCE("balance", "", "uint256");
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
        TOKEN_PURCHASE("TokenPurchase", "address indexed,uint256 indexed,uint256 indexed"),
        TRX_PURCHASE("TrxPurchase", "address indexed,uint256 indexed,uint256 indexed"),
        TOKEN_TO_TOKEN("TokenToToken", "address indexed,address,address,uint256,uint256"),
        ADD_LIQUIDITY("AddLiquidity", "address indexed,uint256 indexed,uint256 indexed"),
        REMOVE_LIQUIDITY("RemoveLiquidity", "address indexed,uint256 indexed,uint256 indexed"),
        SNAPSHOT("Snapshot", "address indexed,uint256 indexed,uint256 indexed"),
        ADMIN_FEE_MINT("AdminFeeMint", "address indexed,address indexed,uint256"),
        TRANSFER("Transfer", "address,address,uint256");
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
