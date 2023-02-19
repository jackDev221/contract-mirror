package org.tron.defi.contract.abi.pool;

import lombok.Getter;
import org.tron.defi.contract.abi.*;

import java.util.HashMap;
import java.util.Map;

public class CurveAbi extends Contract {
    public CurveAbi(ContractTrigger trigger, String address) {
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
        COINS("coins", "uint256", "address"),
        BALANCES("balances", "uint256", "uint256"),
        TOKEN("token", "", "address"),
        FEE("fee", "", "uint256"),
        FUTURE_FEE("future_fee", "", "uint256"),
        ADMIN_FEE("admin_fee", "", "uint256"),
        FUTURE_ADMIN_FEE("future_admin_fee", "", "uint256"),
        ADMIN_ACTIONS_DEADLINE("admin_actions_deadline", "", "uint256"),
        FEE_CONVERTER("fee_converter", "", "address"),
        INITIAL_A("initial_A", "", "uint256"),
        INITIAL_A_TIME("initial_A_time", "", "uint256"),
        FUTURE_A("future_A", "", "uint256"),
        FUTURE_A_TIME("future_A_time", "", "uint256"),
        OWNER("owner", "", "address"),
        FUTURE_OWNER("future_owner", "", "address"),
        TRANSFER_OWNERSHIP_DEADLINE("transfer_ownership_deadline", "", "uint256");
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

        public static IFunction getBySignature(String signature) {
            return signatureMap.getOrDefault(signature, null);
        }
    }

    public enum Events implements IEvent {
        TOKEN_EXCHANGE("TokenExchange", "address indexed,int128,uint256,int128,uint256"),
        ADD_LIQUIDITY("AddLiquidity", "address indexed,uint256[],uint256[],uint256,uint256"),
        REMOVE_LIQUIDITY("RemoveLiquidity", "address indexed,uint256[],uint256[],uint256"),
        REMOVE_LIQUIDITY_ONE("RemoveLiquidityOne", "address indexed,uint256,uint256"),
        REMOVE_LIQUIDITY_IMBLANCE("RemoveLiquidityImbalance",
                                  "address indexed,uint256[],uint256[],uint256,uint256"),
        COMMIT_NEW_ADMIN("CommitNewAdmin", "uint256 indexed,address indexed"),
        NEW_ADMIN("NewAdmin", "address indexed"),
        NEW_FEE_CONVERTER("NewFeeConverter", "address indexed"),
        COMMIT_NEW_FEE("CommitNewFee", "uint256 indexed,uint256,uint256"),
        NEW_FEE("NewFee", "uint256,uint256"),
        RAMP_A("RampA", "uint256,uint256,uint256,uint256"),
        STOP_RAMP_A("StopRampA", "uint256,uint256");
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

        public static IEvent getBySignature(String signature) {
            return signatureMap.getOrDefault(signature, null);
        }
    }
}
