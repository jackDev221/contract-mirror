package org.tron.sunio.contract_mirror.mirror.contracts.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;
import org.tron.sunio.contract_mirror.mirror.chainHelper.IChainHelper;
import org.tron.sunio.contract_mirror.mirror.chainHelper.TriggerContractInfo;
import org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.ContractInfo;
import org.tron.sunio.contract_mirror.mirror.contracts.IContractsHelper;
import org.tron.sunio.contract_mirror.mirror.dao.PSMData;
import org.tron.sunio.contract_mirror.mirror.dao.PSMTotalData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.tools.CallContractUtil;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
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
    private static final int USDD_DECIMAL = 18;
    private static final int USD_DECIMAL = 6;
    private static final int FILED_INFO_SIZE = 11;
    private static final String FILE_TYPE_TIN = "tin";
    private static final String FILE_TYPE_TOUT = "tout";
    private static final String FILE_TYPE_QUOTA = "quota";
    @Setter
    private PSMData psmData;
    private final String polyAddress;
    private String token;
    @Setter
    private PSMTotalData psmTotalData;


    public static PSM genInstance(ContractInfo contractInfo, IChainHelper iChainHelper, IContractsHelper iContractsHelper,
                                  PSMTotalData psmTotalData, Map<String, String> sigMap) {
        PSM.ContractExtraData extraData = PSM.parseToExtraData(contractInfo.getExtra());
        if (ObjectUtil.isNull(extraData)) {
            return null;
        }
        PSM psm = new PSM(contractInfo.getAddress(), contractInfo.getType(), extraData.getVersion(), extraData.getPoly(),
                iChainHelper, iContractsHelper, psmTotalData, sigMap);
        psm.token = extraData.token;
        return psm;
    }

    public PSM(String address, ContractType type, String version, String polyAddress, IChainHelper iChainHelper, IContractsHelper
            iContractsHelper, PSMTotalData psmTotalData, Map<String, String> sigMap) {
        super(address, type, version, iChainHelper, iContractsHelper, sigMap);
        this.polyAddress = polyAddress;
        this.psmTotalData = psmTotalData;
    }

    public PSM(ContractType type, PSMData psmData, PSMTotalData psmTotalData, IChainHelper iChainHelper, IContractsHelper
            iContractsHelper, Map<String, String> sigMap) {
        this(psmData.getAddress(), type, psmData.getVersion(), psmData.getPolyAddress(), iChainHelper, iContractsHelper, psmTotalData, sigMap);
        this.setPsmData(psmData);
        this.setPsmTotalData(psmTotalData);
    }

    @Override
    public BaseContract copySelf() {
        try {
            rLock.lock();
            PSMData psmData = getPsmData();
            PSMTotalData psmTotalData = this.psmTotalData.copySelf();
            PSM psm = new PSM(
                    type,
                    psmData,
                    psmTotalData,
                    iChainHelper,
                    iContractsHelper,
                    sigMap
            );
            psm.setStateInfo(psmData.getStateInfo());
            return psm;
        } finally {
            rLock.unlock();
        }
    }

    private PSMData getVarPsmData() {
        if (ObjectUtil.isNull(psmData)) {
            psmData = new PSMData();
            psmData.setPolyAddress(polyAddress);
            psmData.setVersion(version);
            psmData.setAddress(address);
            psmData.setToken(token);
            psmData.setType(type);
            psmData.setStateInfo(stateInfo);
        }
        return psmData;
    }

    public PSMData getPsmData() {
        return getVarPsmData().copySelf();
    }

    @Override
    public boolean initDataFromChain1() {
        PSMData psmData = getVarPsmData();
        String gemJoin = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "gemJoin");
        psmData.setGemJoin(gemJoin);
        String usdd = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "usdd");
        psmData.setUsdd(usdd);
        String usddJoin = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "usddJoin");
        psmData.setUsddJoin(usddJoin);
        String vat = CallContractUtil.getTronAddress(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "vat");
        psmData.setVat(vat);
        BigInteger tin = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "tin");
        psmData.setTin(tin);
        BigInteger tout = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, address, "tout");
        psmData.setTout(tout);
        int decimals = CallContractUtil.getU256(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, token, "decimals").intValue();
        psmData.setTokenDecimal(decimals);
        String tokenSymbol = CallContractUtil.getString(iChainHelper, ContractMirrorConst.EMPTY_ADDRESS, token, "symbol");
        psmData.setTokenSymbol(tokenSymbol);
        loadInfosField(psmData);
        stateInfo.dirty = true;
        return true;
    }

    @SuppressWarnings("unchecked")
    private void loadInfosField(PSMData psmData) {
        Address inAddress = new Address(WalletUtil.ethAddressHex(this.getAddress()));
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(this.address, polyAddress, "getInfo",
                List.of(inAddress), List.of(new TypeReference<DynamicArray<Uint256>>() {
        })
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        List<Uint256> infoArray = ((DynamicArray<Uint256>) results.get(0)).getValue();
        if (infoArray.size() < FILED_INFO_SIZE) {
            log.error("From:{} poly:{} getInfo size is{}", address, polyAddress, infoArray.size());
        }
        psmData.setMaxReversSwap(infoArray.get(0).getValue());
        psmData.setSwappedUSDD(infoArray.get(1).getValue());
        psmData.setTotalMaxSwapUSDD(infoArray.get(2).getValue());
        psmData.setMaxSwapUSDD(infoArray.get(3).getValue());
        psmData.setUsdBalance(infoArray.get(4).getValue());
        psmData.setUsddBalance(infoArray.get(5).getValue());
        psmData.setTotalSwappedUSDD(infoArray.get(6).getValue());
        psmData.setEnable(infoArray.get(7).getValue().compareTo(BigInteger.ONE) == 0);
        psmData.setReverseLimitEnable(infoArray.get(8).getValue().compareTo(BigInteger.ONE) == 0);
    }

    @SuppressWarnings("unchecked")
    public BigInteger[] getTotalInfos() {
        Address inAddress = new Address(WalletUtil.ethAddressHex(this.getAddress()));
        TriggerContractInfo triggerContractInfo = new TriggerContractInfo(this.address, polyAddress, "getInfo",
                List.of(inAddress), List.of(new TypeReference<DynamicArray<Uint256>>() {
        })
        );
        List<Type> results = this.iChainHelper.triggerConstantContract(triggerContractInfo);
        List<Uint256> infoArray = ((DynamicArray<Uint256>) results.get(0)).getValue();
        return new BigInteger[]{infoArray.get(2).getValue(), infoArray.get(6).getValue()};
    }

    public void updateTotalInfos(BigInteger[] infos) {
        PSMData psmData = getVarPsmData();
        psmData.setTotalMaxSwapUSDD(new BigInteger(infos[0].toString()));
        psmData.setTotalSwappedUSDD(new BigInteger(infos[1].toString()));
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
                result = HandleResult.genHandleUselessMessage(String.format("Event:%s not handle", handleEventExtraData.getUniqueId()));
                break;
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getStatus() {
        return (T) getVarPsmData();
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    @SuppressWarnings("unchecked")
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
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s handleEventFile cat find what:%s!, unique id :%s",
                    address, type, what, handleEventExtraData.getUniqueId()));
        }
        stateInfo.dirty = true;
        return HandleResult.genHandleSuccess();
    }

    // USD---> USDD
    private HandleResult handleEventSellGem(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_SELL_GEM, EVENT_NAME_SELL_GEM_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventSellGem fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        PSMData psmData = getVarPsmData();
        BigInteger sumValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        doSellGem(psmData, sumValue, fee, psmTotalData);
        stateInfo.dirty = true;
        return HandleResult.genHandleSuccess();
    }

    public void doSellGem(PSMData psmData, BigInteger sumValue, BigInteger fee, PSMTotalData psmTotalData) {
        sumValue = covertToUSDDDecimal(sumValue, psmData.getTokenDecimal());
        BigInteger value = sumValue.subtract(fee);
        psmData.setSwappedUSDD(psmData.getSwappedUSDD().add(value));
        psmData.setUsddBalance(psmData.getUsddBalance().subtract(sumValue));
        psmData.setUsdBalance(psmData.getUsdBalance().add(covertToUSDXDecimal(sumValue, psmData.getTokenDecimal())));

        BigInteger totalSwappedUSDD = psmData.getTotalSwappedUSDD();
        if (psmTotalData.isFinishInit()) {
            totalSwappedUSDD = psmTotalData.getTotalSwappedUSDD();
        }
        totalSwappedUSDD = totalSwappedUSDD.add(value);
        psmData.setTotalSwappedUSDD(totalSwappedUSDD);
        psmTotalData.setTotalSwappedUSDD(totalSwappedUSDD);
    }

    //USD-->USDT（反向）
    private HandleResult handleEventBuyGem(String[] topics, String data, HandleEventExtraData handleEventExtraData) {
        EventValues eventValues = getEventValue(EVENT_NAME_BUY_GEM, EVENT_NAME_BUY_GEM_BODY,
                topics, data, handleEventExtraData.getUniqueId());
        if (ObjectUtil.isNull(eventValues)) {
            return HandleResult.genHandleFailMessage(String.format("Contract%s, type:%s decode handleEventSellGem fail!, unique id :%s",
                    address, type, handleEventExtraData.getUniqueId()));
        }
        PSMData psmData = getVarPsmData();
        BigInteger value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        BigInteger fee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        doBuyGem(value, fee, psmData, psmTotalData);
        stateInfo.dirty = true;
        return HandleResult.genHandleSuccess();
    }

    public void doBuyGem(BigInteger value, BigInteger fee, PSMData psmData, PSMTotalData psmTotalData) {
        value = covertToUSDDDecimal(value, psmData.getTokenDecimal());
        BigInteger sumValue = fee.add(value);
        psmData.setSwappedUSDD(psmData.getSwappedUSDD().subtract(value));
        psmData.setUsddBalance(psmData.getUsddBalance().add(sumValue));
        psmData.setUsdBalance(psmData.getUsdBalance().subtract(covertToUSDXDecimal(sumValue, psmData.getTokenDecimal())));

        BigInteger totalSwappedUSDD = psmData.getTotalSwappedUSDD();
        if (psmTotalData.isFinishInit()) {
            totalSwappedUSDD = psmTotalData.getTotalSwappedUSDD();
        }
        totalSwappedUSDD = totalSwappedUSDD.subtract(value);
        psmData.setTotalSwappedUSDD(totalSwappedUSDD);
        psmTotalData.setTotalSwappedUSDD(totalSwappedUSDD);
    }

    private static BigInteger covertToUSDXDecimal(BigInteger value, int tokenDecimal) {
        if (tokenDecimal == USDD_DECIMAL) {
            return value;
        }
        return value.divide(BigInteger.TEN.pow(USDD_DECIMAL - USD_DECIMAL));
    }

    private static BigInteger covertToUSDDDecimal(BigInteger value, int tokenDecimal) {
        if (tokenDecimal == USDD_DECIMAL) {
            return value;
        }
        return value.multiply(BigInteger.TEN.pow(USDD_DECIMAL - USD_DECIMAL));
    }

    public BigInteger[] calcUSDXToUSDD(BigInteger input, int tokenDecimal, BigInteger tin) {
        BigInteger value = input.multiply(BigInteger.TEN.pow(18).subtract(tin)).divide(BigInteger.TEN.pow(18));
        BigInteger fee = covertToUSDDDecimal(value, tokenDecimal).multiply(tin).divide(BigInteger.TEN.pow(18));
        return new BigInteger[]{value, fee};
    }


    /**
     * usd => usdd
     * value1 = info2 - info6
     * value2 = info3 - info1
     * temp = value1 < value2 ? value1 : value2
     * convertibleAmount = temp < info5 ? temp : info5
     * feeTemp = 1 - info9
     * convertibleAmount = convertibleAmount ✖️ feeTemp
     */


    public static BigInteger getConvertibleAmountUSDD(PSMData psmData) {
        BigInteger value1 = psmData.getTotalMaxSwapUSDD().subtract(psmData.getTotalSwappedUSDD());
        BigInteger value2 = psmData.getMaxSwapUSDD().subtract(psmData.getSwappedUSDD());
        BigInteger tmp = value1.min(value2);
        BigInteger convertibleAmount = tmp.min(psmData.getUsddBalance());
        convertibleAmount = convertibleAmount.multiply(BigInteger.TEN.pow(18).subtract(psmData.getTin())).divide(BigInteger.TEN.pow(18));
        return convertibleAmount;
    }

    public BigInteger[] calcUSDDToUSDX(BigInteger input, int tokenDecimal, BigInteger tout) {
        BigDecimal inputD = new BigDecimal(input.multiply(BigInteger.TEN.pow(18)));
        BigInteger feeTemp = BigInteger.TEN.pow(18).add(tout);
        BigInteger value = inputD.divide(new BigDecimal(feeTemp), 0, RoundingMode.DOWN).toBigInteger();
        value = covertToUSDXDecimal(value, tokenDecimal);
        BigInteger fee = value.multiply(BigInteger.TEN.pow(12)).multiply(tout).divide(BigInteger.TEN.pow(18));
        return new BigInteger[]{value, fee};
    }

    public double calcUSDDToUSDXFee(BigInteger tout) {
        return new BigDecimal(tout).divide(new BigDecimal(BigInteger.TEN.pow(18)), 6, RoundingMode.UP).doubleValue();
    }

    public double calcUSDXToUSDDFee(BigInteger tin) {
        return new BigDecimal(tin).divide(new BigDecimal(BigInteger.TEN.pow(18)), 6, RoundingMode.UP).doubleValue();
    }

    /**
     * usdd => usd
     * feeTemp = 1 + info10
     * info8 > 0
     * convertibleAmount = info0 < info4 ? info0 ➗ feeTemp : info4 ➗ feeTemp
     * info8 <=0
     * convertibleAmount = info4 ➗ feeTemp
     */
    public static BigInteger getConvertibleAmountUSDX(PSMData psmData) {
        BigInteger feeTemp = BigInteger.TEN.pow(18).add(psmData.getTout());
        BigInteger convertibleAmount = psmData.getUsdBalance();
        if (psmData.isReverseLimitEnable()) {
            convertibleAmount = convertibleAmount.min(psmData.getMaxReversSwap());
        }
        convertibleAmount = convertibleAmount.multiply(BigInteger.TEN.pow(18));
        convertibleAmount = new BigDecimal(convertibleAmount).divide(new BigDecimal(feeTemp), 0, RoundingMode.DOWN).toBigInteger();
//        convertibleAmount = covertToUSDXDecimal(convertibleAmount, type);
        return convertibleAmount;
    }

    @Data
    public static class ContractExtraData {
        private String poly;
        private String version;
        private String token;
    }

    public static ContractExtraData parseToExtraData(String input) {
        try {
            return GsonUtil.gsonToObject(input, ContractExtraData.class);
        } catch (Exception e) {
            log.error("Parse psm failed, input:{} err:{}", input, e);
        }
        return null;
    }

}
