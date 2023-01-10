package org.tron.sunio.contract_mirror.mirror.contracts.factory;

import cn.hutool.core.util.ObjectUtil;
import org.tron.sunio.contract_mirror.mirror.cache.CacheHandler;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractFactory;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.ContractV1;
import org.tron.sunio.contract_mirror.mirror.dao.ContractFactoryV1Data;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ContractFactoryV1 extends BaseContract implements IContractFactory {

    public ContractFactoryV1(String address, IChainHelper iChainHelper) {
        super(address, ContractType.CONTRACT_FACTORY_V1, iChainHelper);
    }

    @Override
    public BaseContract getBaseContract() {
        return this;
    }

    @Override
    public List<BaseContract> getListContracts() {
        List<BaseContract> result = new ArrayList<>();
        long totalTokens = getTokenCount().longValue();
        for (long i = 0; i < totalTokens; i++) {
            Address tokenAddress = getTokenWithId(i);
            Address contractAddress = getExchange(tokenAddress);
            ContractV1 contractV1 = new ContractV1(WalletUtil.ethAddressToTron(contractAddress.toString()),
                    this.iChainHelper, WalletUtil.ethAddressToTron(tokenAddress.toString()));

            result.add(contractV1);
        }
        return result;
    }

    @Override
    public List<String> getListContractAddresses() {
        List<String> result = new ArrayList<>();
        long totalTokens = getTokenCount().longValue();
        for (long i = 0; i < totalTokens; i++) {
            Address tokenAddress = getTokenWithId(i);
            Address contractAddress = getExchange(tokenAddress);
            result.add(WalletUtil.ethAddressToTron(contractAddress.toString()));
        }
        return result;
    }

    @Override
    public String getFactoryState() {
        ContractFactoryV1Data factoryV1Data = CacheHandler.v1FactoryCache.getIfPresent(this.address);
        if (ObjectUtil.isNotNull(factoryV1Data)) {
            return String.format("Address:%s, Type: %s, feeAddress:%s, feeToRate:%d", this.address, this.type, factoryV1Data.getFeeAddress(),
                    factoryV1Data.getFeeToRate());
        }
        return String.format("Address:%s, Type: %s not init", this.address, this.type);

    }

    private BigInteger getTokenCount() {
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                this.getAddress(),
                ContractMirrorConst.EMPTY_ADDRESS,
                "tokenCount",
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        return (BigInteger) results.get(0).getValue();
    }

    public Address getTokenWithId(long id) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Uint256(id));
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                this.getAddress(),
                ContractMirrorConst.EMPTY_ADDRESS,
                "getTokenWithId",
                inputParameters,
                outputParameters
        );
        triggerContractInfo.setOutputParameters(outputParameters);
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        return (Address) results.get(0).getValue();
    }

    public Address getExchange(Address tokenAddress) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(tokenAddress);
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(
                this.getAddress(),
                ContractMirrorConst.EMPTY_ADDRESS,
                "getExchange",
                inputParameters,
                outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        return (Address) results.get(0).getValue();
    }

    @Override
    public boolean initContract() {
        super.initContract();
        ContractFactoryV1Data factoryV1Data = CacheHandler.v1FactoryCache.getIfPresent(this.address);
        if (ObjectUtil.isNull(factoryV1Data)) {
            factoryV1Data.setReady(false);
            factoryV1Data.setUsing(true);
            factoryV1Data.setAddress(this.address);
            factoryV1Data.setType(this.type);
        }
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo();
        triggerContractInfo.setContractAddress(this.getAddress());
        triggerContractInfo.setFromAddress(ContractMirrorConst.EMPTY_ADDRESS);
        triggerContractInfo.setMethodName("feeTo");
        List<Type> inputParameters = new ArrayList<>();
        triggerContractInfo.setInputParameters(inputParameters);
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Address>() {
        });
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        Address feeAddress = (Address) results.get(0).getValue();
        factoryV1Data.setFeeAddress(WalletUtil.ethAddressToTron(feeAddress.toString()));
        triggerContractInfo.setMethodName("feeToRate");
        outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        BigInteger feeToRate = (BigInteger) results.get(0).getValue();
        factoryV1Data.setFeeToRate(feeToRate.longValue());
        CacheHandler.v1FactoryCache.put(this.address, factoryV1Data);
        return true;
    }

}
