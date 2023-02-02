package org.tron.sunio.contract_mirror.mirror.contracts.factory;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.events.SwapV2PairEvent;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractFactory;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV2Pair;
import org.tron.sunio.contract_mirror.mirror.dao.SwapFactoryV2Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.pool.CMPool;
import org.tron.sunio.contract_mirror.mirror.pool.process.out.BaseProcessOut;
import org.tron.sunio.contract_mirror.mirror.tools.EthUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.SwapV2FactoryEvent.EVENT_NAME_PAIR_CREATED_MINT;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_ALL_PAIRS_LENGTH;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FEE_TO;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_FEE_TO_SETTER;

@Slf4j
public class SwapFactoryV2 extends BaseFactory implements IContractFactory {
    private Map<String, String> v2PairSigMap;
    private SwapFactoryV2Data swapFactoryV2Data;

    public SwapFactoryV2(String address, IChainHelper iChainHelper,
                         Map<String, String> sigMap) {
        super(address, ContractType.SWAP_FACTORY_V2, iChainHelper, sigMap);
        v2PairSigMap = SwapV2PairEvent.getSigMap();
    }

    private SwapFactoryV2Data getVarFactoryV2Data() {
        if (ObjectUtil.isNull(swapFactoryV2Data)) {
            swapFactoryV2Data = new SwapFactoryV2Data();
            swapFactoryV2Data.setReady(false);
            swapFactoryV2Data.setUsing(true);
            swapFactoryV2Data.setAddress(this.address);
            swapFactoryV2Data.setType(this.type);
        }
        return swapFactoryV2Data;
    }

    @Override
    public boolean initDataFromChain1() {
        SwapFactoryV2Data factoryV2Data = this.getVarFactoryV2Data();
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo();
        triggerContractInfo.setContractAddress(this.getAddress());
        triggerContractInfo.setFromAddress(ContractMirrorConst.EMPTY_ADDRESS);
        triggerContractInfo.setMethodName("feeTo");
        List<Type> inputParameters = new ArrayList<>();
        triggerContractInfo.setInputParameters(inputParameters);
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        triggerContractInfo.setOutputParameters(outputParameters);
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() > 0) {
            Address feeAddress = (Address) results.get(0).getValue();
            factoryV2Data.setFeeTo(WalletUtil.ethAddressToTron(feeAddress.toString()));
        }
        triggerContractInfo.setMethodName("feeToSetter()");
        results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() > 0) {
            Address feeToSetterAddress = (Address) results.get(0).getValue();
            factoryV2Data.setFeeToSetter(WalletUtil.ethAddressToTron(feeToSetterAddress.toString()));
        }
        long pairCount = getPairCount().longValue();
        factoryV2Data.setPairCount(pairCount);
        isDirty = true;
        return true;
    }

    @Override
    public BaseFactory getBaseContract() {
        return this;
    }

    private Address getPairWithId(long id) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Uint256(id));
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                ContractMirrorConst.EMPTY_ADDRESS,
                this.getAddress(),
                "allPairs",
                inputParameters,
                outputParameters
        );
        triggerContractInfo.setOutputParameters(outputParameters);
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() > 0) {
            return new Address(EthUtil.addHexPrefix((String) results.get(0).getValue()));
        }
        return Address.DEFAULT;
    }

    @Override
    public List<BaseContract> getListContracts(CMPool cmPool) {
        List<BaseContract> result = new ArrayList<>();
        long pairCount = this.getVarFactoryV2Data().getPairCount();
        List<BaseProcessOut> outs = this.getListContractsBase(cmPool, (int) pairCount);
        for (BaseProcessOut out : outs) {
            String pairAddress = out.getAddress();
            if (pairAddress.equals(EMPTY_ADDRESS)) {
                continue;
            }
            SwapV2Pair swapV2Pair = new SwapV2Pair(
                    pairAddress,
                    this.address,
                    this.iChainHelper,
                    v2PairSigMap

            );
            result.add(swapV2Pair);

        }
        return result;
    }

    @Override
    public List<String> getListContractAddresses() {
        List<String> result = new ArrayList<>();
        long pairCount = getPairCount().longValue();
        for (long i = 0; i < pairCount; i++) {
            Address pairAddress = getPairWithId(i);
            if (pairAddress.equals(Address.DEFAULT)) {
                continue;
            }
            result.add(WalletUtil.ethAddressToTron(pairAddress.toString()));
        }
        return result;
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        SwapFactoryV2Data factoryV2Data = this.getVarFactoryV2Data();
        factoryV2Data.setUsing(isUsing);
        factoryV2Data.setReady(isReady);
        factoryV2Data.setAddExchangeContracts(isAddExchangeContracts);
        isDirty = true;
    }

    @Override
    protected void saveUpdateToCache() {
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result;
        switch (eventName) {
            case EVENT_NAME_PAIR_CREATED_MINT:
                result = handleCreatePair(topics, data);
                break;
            default:
                log.warn("Contract:{} type:{} event:{} not handle", address, type, topics[0]);
                result = HandleResult.genHandleFailMessage(String.format("Event:%s not handle", handleEventExtraData.getUniqueId()));
                break;
        }
        return result;
    }

    @Override
    public <T> T getStatus() {
        return (T) getVarFactoryV2Data();
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        switch (method) {
            case METHOD_FEE_TO:
                return (T) this.getVarFactoryV2Data().getFeeTo();
            case METHOD_FEE_TO_SETTER:
                return (T) this.getVarFactoryV2Data().getFeeToSetter();
            case METHOD_ALL_PAIRS_LENGTH:
                return (T) (Long) this.getVarFactoryV2Data().getPairCount();
        }
        return null;
    }

    @Override
    public String getFactoryState() {
        SwapFactoryV2Data factoryV2Data = this.getVarFactoryV2Data();
        if (ObjectUtil.isNotNull(factoryV2Data)) {
            return String.format("Address:%s, Type: %s, feeTo:%s, feeToSetter:%d", this.address, this.type, factoryV2Data.getFeeTo(),
                    factoryV2Data.getFeeToSetter());
        }
        return String.format("Address:%s, Type: %s not init", this.address, this.type);
    }

    private BigInteger getPairCount() {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                ContractMirrorConst.EMPTY_ADDRESS,
                this.getAddress(),
                "allPairsLength",
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() == 0) {
            log.error("Contract:{}, type: {} call tokenCount failed", address, type);
            return BigInteger.ZERO;
        }
        return (BigInteger) results.get(0).getValue();
    }

    private HandleResult handleCreatePair(String[] _topics, String _data) {
        isAddExchangeContracts = false;
        SwapFactoryV2Data factoryV2Data = this.getVarFactoryV2Data();
        factoryV2Data.setAddExchangeContracts(false);
        factoryV2Data.setPairCount(factoryV2Data.getPairCount() + 1);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }
}
