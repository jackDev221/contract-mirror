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

    public enum Events implements IEvent {
        DEPOSIT("Deposit", "address indexed,uint256"),
        WITHDRAW("Withdrawal", "address indexed,uint256");
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

    public enum Functions implements IFunction {
        DEPOSIT("deposit", "address,uint256", ""),
        WITHDRAW("withdraw", "address,uint256", "");
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
