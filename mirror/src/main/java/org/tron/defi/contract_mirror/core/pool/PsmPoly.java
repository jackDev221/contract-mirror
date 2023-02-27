package org.tron.defi.contract_mirror.core.pool;

import lombok.Data;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.PsmPolyAbi;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public class PsmPoly extends Contract {
    public PsmPoly(String address) {
        super(address);
    }

    @Override
    public String getContractType() {
        return PoolType.PSM_POLY.name();
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(PsmPolyAbi.class, getAddress());
    }

    public PsmInfo getInfo(String address) {
        String ethAddress = AddressConverter.TronBase58ToEthAddress(address);
        List<Type> response = abi.invoke(PsmPolyAbi.Functions.GET_INFO,
                                         Collections.singletonList(ethAddress));
        return new PsmInfo(((DynamicArray<Uint256>) response.get(0)).getValue());
    }

    @Data
    public class PsmInfo {
        private BigInteger amountUsddToGem;
        private BigInteger amountGemToUsdd;
        private BigInteger quotaTotalToUsdd;
        private BigInteger quotaGemToUsdd;
        private BigInteger balanceGem;
        private BigInteger balanceUsdd;
        private BigInteger amountTotalToUsdd;
        private Boolean available;
        private Boolean enableUsddToGemQuota;
        private BigInteger feeToUsdd;
        private BigInteger feeToGem;

        public PsmInfo(List<Uint256> rawInfo) {
            setAmountUsddToGem(rawInfo.get(0).getValue());
            setAmountGemToUsdd(rawInfo.get(1).getValue());
            setQuotaTotalToUsdd(rawInfo.get(2).getValue());
            setQuotaGemToUsdd(rawInfo.get(3).getValue());
            setBalanceGem(rawInfo.get(4).getValue());
            setBalanceUsdd(rawInfo.get(5).getValue());
            setAmountTotalToUsdd(rawInfo.get(6).getValue());
            setAvailable(rawInfo.get(7).getValue().compareTo(BigInteger.ONE) == 0);
            setEnableUsddToGemQuota(rawInfo.get(8).getValue().compareTo(BigInteger.ONE) == 0);
            setFeeToUsdd(rawInfo.get(9).getValue());
            setFeeToGem(rawInfo.get(10).getValue());
        }
    }
}
