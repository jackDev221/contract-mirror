package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.dao.AssemblePoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.tools.CallContractUtil;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_BASE_COINS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_BASE_LP;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_BASE_POOL;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_COINS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_POOL;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TOKEN;

@Slf4j
public class AssemblePool extends BaseContract {

    /*
     *
     * 看过代码： 4Pool/2Pool 是以 1 + N的形式构成的， 因此子池保持更新就行了。
     *
     * */
    private static final BigInteger FEE_DENOMINATOR = BigInteger.TEN.pow(10);
    private AssemblePoolData poolData;
    private int coinSize;
    private int baseCoinSize;

    public AssemblePool(String address, ContractType type, int coinSize, int baseCoinSize, IChainHelper iChainHelper, IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        super(address, type, iChainHelper, iContractsHelper, sigMap);
        this.coinSize = coinSize;
        this.baseCoinSize = baseCoinSize;
    }


    private AssemblePoolData getVarPoolData() {
        if (ObjectUtil.isNull(poolData)) {
            poolData = new AssemblePoolData();
            poolData.setAddress(address);
            poolData.setType(type);
            poolData.setUsing(true);
            poolData.setReady(false);
            poolData.setAddExchangeContracts(false);
            poolData.setBaseCoins(new String[baseCoinSize]);
            poolData.setBaseCoinNames(new String[baseCoinSize]);
            poolData.setBaseCoinSymbols(new String[baseCoinSize]);
            poolData.setCoins(new String[coinSize]);
            poolData.setCoinNames(new String[coinSize]);
            poolData.setCoinSymbols(new String[coinSize]);
        }
        return poolData;
    }


    private void updateCoinsInfo(int count, String method, String[] coins, String[] names, String[] symbols) {
        for (int i = 0; i < count; i++) {
            String coinAddress = CallContractUtil.getTronAddressWithIndex(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS,
                    address, method, BigInteger.valueOf(i));
            coins[i] = coinAddress;
            if (!coinAddress.equalsIgnoreCase(EMPTY_ADDRESS)) {
                String name = CallContractUtil.getString(iChainHelper, EMPTY_ADDRESS, coinAddress, "name");
                String symbol = CallContractUtil.getString(iChainHelper, EMPTY_ADDRESS, coinAddress, "symbol");
                names[i] = name;
                symbols[i] = symbol;
            }
        }
    }

    @Override
    public boolean initDataFromChain1() {
        AssemblePoolData curve4PoolData = this.getVarPoolData();
        updateCoinsInfo(coinSize, "coins", curve4PoolData.getCoins(), curve4PoolData.getCoinNames(), curve4PoolData.getCoinSymbols());
        updateCoinsInfo(baseCoinSize, "base_coins", curve4PoolData.getBaseCoins(), curve4PoolData.getBaseCoinNames(), curve4PoolData.getBaseCoinSymbols());
        String basePool = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "base_pool");
        curve4PoolData.setBasePool(basePool);
        String baseLp = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "base_lp");
        curve4PoolData.setBaseLp(baseLp);
        String pool = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "pool");
        curve4PoolData.setPool(pool);
        String token = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "token");
        curve4PoolData.setToken(token);
        isDirty = true;
        return true;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        AssemblePoolData poolData = this.getVarPoolData();
        poolData.setUsing(isUsing);
        poolData.setReady(isReady);
        poolData.setAddExchangeContracts(isAddExchangeContracts);
        isDirty = true;
    }

    @Override
    protected void saveUpdateToCache() {
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result = HandleResult.genHandleFailMessage(String.format("Event:%s not handle", handleEventExtraData.getUniqueId()));
        return result;
    }

    @Override
    public <T> T getStatus() {
        return (T) getVarPoolData();
    }

    @Override
    public String getVersion() {
        return "assempool";
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        switch (method) {
            case METHOD_TOKEN:
                return (T) this.getVarPoolData().getToken();
            case METHOD_POOL:
                return (T) this.getVarPoolData().getPool();
            case METHOD_BASE_POOL:
                return (T) this.getVarPoolData().getBasePool();
            case METHOD_BASE_LP:
                return (T) this.getVarPoolData().getBaseLp();
            case METHOD_COINS:
                return handleCallGetCoins(params, true);
            case METHOD_BASE_COINS:
                return handleCallGetCoins(params, false);
        }
        return null;
    }

    public <T> T handleCallGetCoins(String params, boolean isCoins) {
        List<TypeReference<?>> outputParameters = List.of(new TypeReference<Uint256>() {
        });
        List<Type> res = FunctionReturnDecoder.decode(params, Utils.convert(outputParameters));
        if (res.size() == 0) {
            throw new RuntimeException("Decode failed");
        }
        int index = ((BigInteger) res.get(0).getValue()).intValue();
        if (isCoins) {
            return (T) this.getVarPoolData().getCoins()[index];
        }
        return (T) this.getVarPoolData().getBaseCoins()[index];
    }
}
