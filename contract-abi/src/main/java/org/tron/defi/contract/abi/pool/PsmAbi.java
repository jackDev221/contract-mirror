package org.tron.defi.contract.abi.pool;

import lombok.Getter;
import org.tron.defi.contract.abi.*;

import java.util.HashMap;
import java.util.Map;

public class PsmAbi extends Contract {
    public PsmAbi(ContractTrigger trigger, String address) {
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
        GEM_JOIN("gemJoin", "", "address"),
        USDD("usdd", "", "address"),
        USDD_JOIN("usddJoin", "", "address"),
        VAT("vat", "", "address"),
        TIN("tin", "", "uint256"),
        TOUT("tout", "", "uint256"),
        QUOTA("quota", "", "address"),
        // TODO: move following functions to new class
        GEM("gem", "", "address");
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
        RELY("Rely", "address indexed"),
        DENY("Deny", "address indexed"),
        FILE("File", "bytes32 indexed,uint256"),
        SELL_GEM("SellGem", "address indexed,uint256,uint256"),
        BUY_GEM("BuyGem", "address indexed,uint256,uint256");

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
