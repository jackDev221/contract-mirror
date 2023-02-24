package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;

import java.math.BigInteger;
import java.util.Map;

public abstract class AbstractCurve extends BaseContract {
    public AbstractCurve(String address, ContractType type, IChainHelper iChainHelper,
                         IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        super(address, type, iChainHelper, iContractsHelper, sigMap);
    }

    public abstract String coins(int i);

    public abstract BigInteger getVirtualPrice(long timestamp) throws Exception;

    public abstract BigInteger calcTokenAmount(long timestamp, BigInteger[] amounts, boolean deposit) throws Exception;

    public abstract BigInteger calcWithdrawOneCoin(long timestamp, BigInteger _token_amount, int i) throws Exception;

    public abstract BigInteger fee();

    public abstract BigInteger adminFee();

    public abstract BigInteger[] rates(long timestamp);

    public abstract BigInteger getDyUnderlying(int i, int j, BigInteger dx, BigInteger dy) throws Exception;

    public abstract BigInteger addLiquidity(BigInteger[] amounts, BigInteger minMintAmount) throws Exception;

    public abstract BigInteger removeLiquidityOneCoin(BigInteger _token_amount, int i, BigInteger min_amount) throws Exception;

    public abstract BigInteger exchange(int i, int j, BigInteger dx, BigInteger min_dy, long timestamp) throws Exception;

    public abstract BigInteger exchangeUnderlying(int i, int j, BigInteger _dx, BigInteger mindy, long timestamp) throws Exception;

    public abstract AbstractCurve copySelf();

    public abstract double calcFee(long timestamp, int j);

    @Override
    public boolean initDataFromChain1() {
        return false;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {

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

}
