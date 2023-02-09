package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.dao.PSMData;
import org.tron.sunio.contract_mirror.mirror.dao.PSMTotalData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.PSMEvent.EVENT_NAME_BUY_GEM;
import static org.tron.sunio.contract_mirror.event_decode.events.PSMEvent.EVENT_NAME_BUY_GEM_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.PSMEvent.EVENT_NAME_FILE;
import static org.tron.sunio.contract_mirror.event_decode.events.PSMEvent.EVENT_NAME_FILE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.PSMEvent.EVENT_NAME_SELL_GEM;
import static org.tron.sunio.contract_mirror.event_decode.events.PSMEvent.EVENT_NAME_SELL_GEM_BODY;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_GEM_JOIN;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_QUOTA;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TIN;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_TOUT;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_USDD;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_USDDJOIN;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.METHOD_VAT;

@Slf4j
public class PSM extends BaseContract {
    private static final int FILED_INFO_SIZE = 11;
    private static final String FILE_TYPE_TIN = "tin";
    private static final String FILE_TYPE_TOUT = "tout";
    private static final String FILE_TYPE_QUOTA = "quota";
    private PSMData psmData;
    private String polyAddress;
    private PSMTotalData psmTotalData;

    public PSM(ContractType type, String address, String polyAddress, IChainHelper iChainHelper, IContractsHelper iContractsHelper,
               PSMTotalData psmTotalData, Map<String, String> sigMap) {
        super(address, type, iChainHelper, iContractsHelper, sigMap);
        this.polyAddress = polyAddress;
        this.psmTotalData = psmTotalData;
    }

    private PSMData getVarPsmData() {
        if (ObjectUtil.isNull(psmData)) {
            psmData = new PSMData();
            psmData.setAddress(address);
            psmData.setType(type);
            psmData.setAddExchangeContracts(false);
            psmData.setUsing(true);
            psmData.setReady(false);
            psmData.setInfos(new BigInteger[FILED_INFO_SIZE]);
        }
        return psmData;
    }

    @Override
    public boolean initDataFromChain1() {
        PSMData psmData = getVarPsmData();
        String gemJoin = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "gemJoin").toString());
        psmData.setGemJoin(gemJoin);
        String usdd = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "usdd").toString());
        psmData.setUsdd(usdd);
        String usddJoin = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "usddJoin").toString());
        psmData.setUsddJoin(usddJoin);
        String vat = WalletUtil.ethAddressToTron(callContractAddress(ContractMirrorConst.EMPTY_ADDRESS, "vat").toString());
        psmData.setVat(vat);
        BigInteger tin = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "tin");
        psmData.setTin(tin);
        BigInteger tout = callContractU256(ContractMirrorConst.EMPTY_ADDRESS, "tout");
        psmData.setTout(tout);
        loadInfosField(psmData);
        isDirty = true;
        return true;
    }

    private void loadInfosField(PSMData psmData) {
        Address inAddress = new Address(WalletUtil.ethAddressHex(this.getAddress()));
        List<Type> inputParameters = List.of(inAddress);
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        for (int i = 0; i < FILED_INFO_SIZE; i++) {
            outputParameters.add(new TypeReference<Uint256>() {
            });
        }
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(this.address, polyAddress, "getInfo",
                inputParameters, outputParameters
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        if (results.size() < 11) {
            log.error("Get contract:{} type:{} , function:getInfo result len < 11", this.address, this.type);
            return;
        }
        for (int i = 0; i < FILED_INFO_SIZE; i++) {
            psmData.getInfos()[i] = (BigInteger) results.get(i).getValue();
        }
        BigInteger totalUSDD = this.psmTotalData.getTotalUSDD();
        if (totalUSDD.compareTo(psmData.getInfos()[2]) > 0) {
            psmData.getInfos()[2] = totalUSDD;
        } else {
            this.psmTotalData.setTotalUSDD(psmData.getInfos()[2]);
        }
        BigInteger totalChargedUSDD = this.psmTotalData.getTotalChargedUSDD();
        if (totalChargedUSDD.compareTo(psmData.getInfos()[6]) > 0) {
            psmData.getInfos()[6] = totalChargedUSDD;
        } else {
            this.psmTotalData.setTotalChargedUSDD(psmData.getInfos()[6]);
        }
    }

    @Override
    public void updateBaseInfo(boolean isUsing, boolean isReady, boolean isAddExchangeContracts) {
        PSMData psmData = getVarPsmData();
        psmData.setUsing(isUsing);
        psmData.setReady(isReady);
        psmData.setAddExchangeContracts(isAddExchangeContracts);
        isDirty = true;
    }

    @Override
    protected void saveUpdateToCache() {
    }

    @Override
    protected HandleResult handleEvent1(String eventName, String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        HandleResult result;
        switch (eventName) {
            case EVENT_NAME_FILE:
                result = handleEventFile(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_SELL_GEM:
                result = handleEventSellGem(topics, data, handleEventExtraData);
                break;
            case EVENT_NAME_BUY_GEM:
                result = handleEventBuyGem(topics, data, handleEventExtraData);
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
        return (T) getVarPsmData();
    }

    @Override
    public <T> T handleSpecialRequest(String method, String params) throws Exception {
        switch (method) {
            case METHOD_GEM_JOIN:
                return (T) this.getVarPsmData().getGemJoin();
            case METHOD_USDD:
                return (T) this.getVarPsmData().getUsdd();
            case METHOD_USDDJOIN:
                return (T) this.getVarPsmData().getUsddJoin();
            case METHOD_VAT:
                return (T) this.getVarPsmData().getVat();
            case METHOD_TIN:
                return (T) this.getVarPsmData().getTin();
            case METHOD_TOUT:
                return (T) this.getVarPsmData().getTout();
            case METHOD_QUOTA:
                return (T) this.getVarPsmData().getQuota();
        }
        return null;
    }

    private HandleResult handleEventFile(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_FILE, EVENT_NAME_FILE_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventFile fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        PSMData psmData = getVarPsmData();
        Bytes32 whatBytes = (Bytes32) eventValues.getIndexedValues().get(0);
        String what = new String(whatBytes.getValue());
        if (what.equalsIgnoreCase(FILE_TYPE_TIN)) {
            BigInteger tin = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            psmData.setTin(tin);
        } else if (what.equalsIgnoreCase(FILE_TYPE_TOUT)) {
            BigInteger tout = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            psmData.setTout(tout);
        } else if (what.equalsIgnoreCase(FILE_TYPE_QUOTA)) {
            String quota = WalletUtil.hexStringToTron((String) eventValues.getNonIndexedValues().get(0).getValue());
            psmData.setQuota(quota);
        } else {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s handleEventFile cat find what:{}!, unique id :%s",
                    address, type, what, handleEventExtraData.getUniqueId()));
        }
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventSellGem(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_SELL_GEM, EVENT_NAME_SELL_GEM_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventSellGem fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        PSMData psmData = getVarPsmData();
        BigInteger value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();

        BigInteger totalCharged = this.psmTotalData.getTotalChargedUSDD();
        BigInteger total = this.psmTotalData.getTotalUSDD();
        if (totalCharged.compareTo(psmData.getInfos()[6]) > 0) {
            totalCharged = psmData.getInfos()[6];
        }
        if (total.compareTo(psmData.getInfos()[2]) > 0) {
            psmData.getInfos()[2] = total;
        } else {
            this.psmTotalData.setTotalUSDD(psmData.getInfos()[2]);
        }

        totalCharged = totalCharged.add(value);
        psmData.getInfos()[6] = totalCharged;
        psmData.getInfos()[1] = psmData.getInfos()[1].add(value);
        this.psmTotalData.setTotalChargedUSDD(totalCharged);
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }

    private HandleResult handleEventBuyGem(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_BUY_GEM, EVENT_NAME_BUY_GEM_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventSellGem fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        PSMData psmData = getVarPsmData();
        BigInteger value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        if (psmData.getInfos()[8].compareTo(BigInteger.ONE) == 0) {
            psmData.getInfos()[0] = psmData.getInfos()[0].subtract(value);
            psmData.getInfos()[4] = psmData.getInfos()[4].subtract(value);
        }
        isDirty = true;
        return HandleResult.genHandleSuccess();
    }
}
