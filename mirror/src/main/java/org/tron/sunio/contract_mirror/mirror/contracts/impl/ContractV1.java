package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import org.apache.tomcat.util.bcel.Const;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ContractV1 extends BaseContract {
    private String tokenAddress;
    private String name;
    private String symbol;
    private long decimals;
    private int kLast;
    private BigInteger trxBalance;

    public ContractV1(String address, IChainHelper iChainHelper, String tokenAddress) {
        super(address, ContractType.CONTRACT_V1, iChainHelper);
        this.tokenAddress = tokenAddress;
    }

    @Override
    public boolean initContract() {
        super.initContract();
        name = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "name");
        symbol = callContractString(ContractMirrorConst.EMPTY_ADDRESS, "symbol");
        decimals = callContractU256(ContractMirrorConst.EMPTY_ADDRESS,"decimals").longValue();
        kLast = callContractUint(ContractMirrorConst.EMPTY_ADDRESS,"kLast").intValue();
        trxBalance = getBalance(address);
        return true;
    }

}
