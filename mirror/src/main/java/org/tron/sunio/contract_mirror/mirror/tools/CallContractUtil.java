package org.tron.sunio.contract_mirror.mirror.tools;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;

@Slf4j
public class CallContractUtil {

    public static Type callContract(IChainHelper iChainHelper, TriggerContractInfo triggerContractInfo) {
        List<Type> results = iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("From:{} Contract:{} method:{} result size is zero", triggerContractInfo.getFromAddress(),
                    triggerContractInfo.getContractAddress(), triggerContractInfo.getMethodName());
            return null;
        }
        return results.get(0);
    }

    @SuppressWarnings("unchecked")
    public static String getString(IChainHelper iChainHelper, String from, String contract, String method) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(from, contract, method,
                Collections.EMPTY_LIST, List.of(new TypeReference<Utf8String>() {
        }));
        Type res = callContract(iChainHelper, triggerContractInfo);
        if (ObjectUtil.isNull(res)) {
            return "";
        }
        return res.getValue().toString();
    }

    @SuppressWarnings("unchecked")
    public static BigInteger getU256(IChainHelper iChainHelper, String from, String contract, String method) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(from, contract, method, Collections.EMPTY_LIST,
                List.of(new TypeReference<Uint256>() {
                })
        );
        Type res = callContract(iChainHelper, triggerContractInfo);
        if (ObjectUtil.isNull(res)) {
            return BigInteger.ZERO;
        }
        return (BigInteger) res.getValue();
    }

    public static BigInteger getU256WithIndex(IChainHelper iChainHelper, String from, String contract, String method, BigInteger index) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(from, contract, method, List.of(new Uint256(index)),
                List.of(new TypeReference<Uint256>() {
                })
        );
        Type res = callContract(iChainHelper, triggerContractInfo);
        if (ObjectUtil.isNull(res)) {
            return BigInteger.ZERO;
        }
        return (BigInteger) res.getValue();
    }

    @SuppressWarnings("unchecked")
    public static Address getAddress(IChainHelper iChainHelper, String from, String contract, String method) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(from, contract, method, Collections.EMPTY_LIST,
                List.of(new TypeReference<Address>() {
                })
        );
        Type res = callContract(iChainHelper, triggerContractInfo);
        if (ObjectUtil.isNull(res)) {
            return Address.DEFAULT;
        }
        return new Address(EthUtil.addHexPrefix((String) res.getValue()));
    }

    @SuppressWarnings("unchecked")
    public static String getTronAddress(IChainHelper iChainHelper, String from, String contract, String method) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(from, contract, method, Collections.EMPTY_LIST,
                List.of(new TypeReference<Address>() {
                })
        );
        Type res = callContract(iChainHelper, triggerContractInfo);
        if (ObjectUtil.isNull(res)) {
            return EMPTY_ADDRESS;
        }
        return WalletUtil.hexStringToTron((String) res.getValue());
    }

    public static String getTronAddressWithIndex(IChainHelper iChainHelper, String from, String contract, String method, BigInteger index) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(from, contract, method, List.of(new Uint256(index)),
                List.of(new TypeReference<Address>() {
                })
        );
        Type res = callContract(iChainHelper, triggerContractInfo);
        if (ObjectUtil.isNull(res)) {
            return EMPTY_ADDRESS;
        }
        return WalletUtil.hexStringToTron((String) res.getValue());
    }

    public static BigInteger tokenBalance(IChainHelper iChainHelper, String from, String contract) {
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(from, contract, "balanceOf",
                List.of(new Address(WalletUtil.ethAddressHex(from))), List.of(new TypeReference<Uint256>() {
        }));
        Type res = callContract(iChainHelper, triggerContractInfo);
        if (ObjectUtil.isNull(res)) {
            return BigInteger.ZERO;
        }
        return (BigInteger) res.getValue();
    }
}
