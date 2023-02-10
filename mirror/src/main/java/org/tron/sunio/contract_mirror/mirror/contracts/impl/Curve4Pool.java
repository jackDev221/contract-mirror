package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.dao.Curve4PoolData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_BASE_COINS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_BASE_LP;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_BASE_POOL;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_COINS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_POOL;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TOKEN;

@Slf4j
public class Curve4Pool extends BaseContract {
    /*
     *
     * 看过代码： 4Pool 是以 1 + N的形式构成的， 因此子池保持更新就行了。
     *
     * */
    private static final int N_COINS = 2;
    private static final int BASE_N_COINS = 3;
    private static final BigInteger FEE_DENOMINATOR = BigInteger.TEN.pow(10);
    private Curve4PoolData curve4PoolData;

    public Curve4Pool(String address, IChainHelper iChainHelper, IContractsHelper iContractsHelper, Map<String, String> sigMap) {
        super(address, ContractType.CONTRACT_CURVE_4POOL, iChainHelper, iContractsHelper, sigMap);
    }

    private Curve4PoolData getVarCurve4PoolData() {
        if (ObjectUtil.isNull(curve4PoolData)) {
            curve4PoolData = new Curve4PoolData();
            curve4PoolData.setAddress(address);
            curve4PoolData.setType(type);
            curve4PoolData.setUsing(true);
            curve4PoolData.setReady(false);
            curve4PoolData.setAddExchangeContracts(false);
            curve4PoolData.setBaseCoins(new String[3]);
            curve4PoolData.setCoins(new String[2]);
        }
        return curve4PoolData;
    }

    private void updateCoinsAndBalance(Curve4PoolData curve4PoolData) {
        for (int i = 0; i < BASE_N_COINS; i++) {
            // update base_coins
            TriggerContractInfo triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address, "base_coins",
                    List.of(new Uint256(i)), List.of(new TypeReference<Address>() {
            }));
            List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "base_coins");
            } else {
                curve4PoolData.updateBaseCoins(i, WalletUtil.hexStringToTron((String) results.get(0).getValue()));
            }

            if (i >= N_COINS) {
                continue;
            }

            // update coins string
            triggerContractInfo = new TriggerContractInfo(ContractMirrorConst.EMPTY_ADDRESS, address,
                    "coins", List.of(new Uint256(i)), List.of(new TypeReference<Address>() {
            })
            );
            results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
            if (results.size() == 0) {
                log.error("Get contract:{} type:{} , function:{} result len is zero", this.address, this.type, "coins");
            } else {
                curve4PoolData.updateCoins(i, WalletUtil.hexStringToTron((String) results.get(0).getValue()));
            }
        }
    }

    @Override
    public boolean initDataFromChain1() {
        Curve4PoolData curve4PoolData = this.getVarCurve4PoolData();
        updateCoinsAndBalance(curve4PoolData);
        String basePool = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "base_pool").toString());
        curve4PoolData.setBasePool(basePool);
        String baseLp = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "base_lp").toString());
        curve4PoolData.setBaseLp(baseLp);
        String pool = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "pool").toString());
        curve4PoolData.setPool(pool);
        String token = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "token").toString());
        curve4PoolData.setToken(token);
        isDirty = true;
        return true;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        Curve4PoolData curve4PoolData = this.getVarCurve4PoolData();
        curve4PoolData.setUsing(isUsing);
        curve4PoolData.setReady(isReady);
        curve4PoolData.setAddExchangeContracts(isAddExchangeContracts);
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
        return (T) getVarCurve4PoolData();
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        switch (method) {
            case METHOD_TOKEN:
                return (T) this.getVarCurve4PoolData().getToken();
            case METHOD_POOL:
                return (T) this.getVarCurve4PoolData().getPool();
            case METHOD_BASE_POOL:
                return (T) this.getVarCurve4PoolData().getBasePool();
            case METHOD_BASE_LP:
                return (T) this.getVarCurve4PoolData().getBaseLp();
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
            return (T) this.getVarCurve4PoolData().getCoins()[index];
        }
        return (T) this.getVarCurve4PoolData().getBaseCoins()[index];
    }
}
