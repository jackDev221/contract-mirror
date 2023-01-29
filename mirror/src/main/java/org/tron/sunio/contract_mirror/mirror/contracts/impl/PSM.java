package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.dao.PSMData;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.generated.Bytes32;

import java.math.BigInteger;
import java.util.Map;

import static org.tron.sunio.contract_mirror.event_decode.events.PSMEvent.EVENT_NAME_BUY_GEM;
import static org.tron.sunio.contract_mirror.event_decode.events.PSMEvent.EVENT_NAME_FILE;
import static org.tron.sunio.contract_mirror.event_decode.events.PSMEvent.EVENT_NAME_FILE_BODY;
import static org.tron.sunio.contract_mirror.event_decode.events.PSMEvent.EVENT_NAME_SELL_GEM;
import static org.tron.sunio.contract_mirror.mirror.enums.ContractType.CONTRACT_PSM;

@Slf4j
public class PSM extends BaseContract {
    private static final String FILE_TYPE_TIN = "tin";
    private static final String FILE_TYPE_TOUT = "tout";
    private static final String FILE_TYPE_QUOTA = "quota";
    private PSMData psmData;

    public PSM(String address, IChainHelper iChainHelper, Map<String, String> sigMap) {
        super(address, CONTRACT_PSM, iChainHelper, sigMap);
    }

    private PSMData getVarPsmData() {
        if (ObjectUtil.isNull(psmData)) {
            psmData = new PSMData();
            psmData.setAddress(address);
            psmData.setType(type);
            psmData.setAddExchangeContracts(false);
            psmData.setUsing(true);
            psmData.setReady(false);
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
        isDirty = true;
        return true;
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
    public <T> T handleSpecialRequest(String method) {
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
        log.info("handleEventSellGem not implements!");
        return HandleResult.genHandleFailMessage("handleEventSellGem not implements!");
    }

    private HandleResult handleEventBuyGem(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        log.info("handleEventBuyGem not implements!");
        return HandleResult.genHandleFailMessage("handleEventBuyGem not implements!");
    }
}
