package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.Setter;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.web3j.utils.Strings;

import java.math.BigInteger;
import java.util.Map;

public abstract class AbstractCurve extends BaseContract {
    @Setter
    protected String currentTx;
    @Setter
    protected String currentIndex;

    public AbstractCurve(String address, ContractType type, String version, IChainHelper iChainHelper,
                         IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        super(address, type, version, iChainHelper, iContractsHelper, sigMap);
    }

    public abstract String coins(int i);

    public abstract BigInteger getVirtualPrice(String uniqueId, long timestamp, IContractsHelper iContractsHelper) throws Exception;

    public abstract BigInteger calcTokenAmount(String uniqueId, long timestamp, BigInteger[] amounts, boolean deposit, IContractsHelper iContractsHelper) throws Exception;

    public abstract BigInteger calcWithdrawOneCoin(String uniqueId, long timestamp, BigInteger _token_amount, int i, IContractsHelper iContractsHelper) throws Exception;

    public abstract BigInteger fee();

    public abstract BigInteger adminFee();

    public abstract BigInteger[] rates(String uniqueId, long timestamp, IContractsHelper iContractsHelper);

    public abstract BigInteger getDyUnderlying(String uniqueId, int i, int j, BigInteger dx, long timestamp, IContractsHelper iContractsHelper) throws Exception;

    public abstract BigInteger getDy(String uniqueId, int i, int j, BigInteger dx, long timestamp, IContractsHelper iContractsHelper) throws Exception;

    public abstract BigInteger addLiquidity(String uniqueId, BigInteger[] amounts, BigInteger minMintAmount, long timestamp, IContractsHelper iContractsHelper) throws Exception;

    public abstract BigInteger[] removeLiquidity(String uniqueId, BigInteger _amount, BigInteger[] _minAmounts, long timestamp, IContractsHelper iContractsHelper) throws Exception;

    public abstract BigInteger removeLiquidityImBalance(String uniqueId, BigInteger[] _amounts, BigInteger _minBurnAmount, long timestamp, IContractsHelper iContractsHelper) throws Exception;

    public abstract BigInteger removeLiquidityOneCoin(String uniqueId, BigInteger _token_amount, int i, BigInteger min_amount, long timestamp, IContractsHelper iContractsHelper) throws Exception;

    public abstract BigInteger exchange(String uniqueId, int i, int j, BigInteger dx, BigInteger min_dy, long timestamp, IContractsHelper iContractsHelper) throws Exception;

    public abstract BigInteger exchangeUnderlying(String uniqueId, int i, int j, BigInteger _dx, BigInteger mindy, long timestamp, IContractsHelper iContractsHelper) throws Exception;

    public abstract double calcFee(String uniqueId, long timestamp, int j, IContractsHelper iContractsHelper);

    public abstract double calcBasePoolFee(String uniqueId, long timestamp, int j, IContractsHelper iContractsHelper);

    @Override
    public boolean initDataFromChain1() {
        return false;
    }

    @Override
    protected void saveUpdateToCache() {

    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        return null;
    }

    @Override
    public <T> T getStatus() {
        return null;
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        return null;
    }

    protected AbstractCurve getAbstractCurve(String address, IContractsHelper iContractsHelper) throws Exception {
        BaseContract baseContract = iContractsHelper.getContract(address);
        if (ObjectUtil.isNull(baseContract)) {
            throw new Exception(String.format("Contract %s not found", address));
        }
        if (!(baseContract instanceof AbstractCurve)) {
            throw new Exception(String.format("Contract %s not instanceof AbstractCurve", address));
        }
        return (AbstractCurve) baseContract;
    }
}
