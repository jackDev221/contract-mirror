package org.tron.defi.contract_mirror.core.pool;

import lombok.extern.slf4j.Slf4j;
import org.tron.defi.contract.abi.ContractAbi;
import org.tron.defi.contract.abi.pool.PsmAbi;
import org.tron.defi.contract.log.ContractLog;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.token.TRC20;
import org.tron.defi.contract_mirror.core.token.Token;
import org.tron.defi.contract_mirror.dao.KafkaMessage;
import org.tron.defi.contract_mirror.utils.chain.AddressConverter;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;

import java.util.Collections;
import java.util.List;

@Slf4j
public class PsmPool extends Pool {
    public PsmPool(String address) {
        super(address);
        type = PoolType.PSM;
    }

    @Override
    protected ContractAbi loadAbi() {
        return tronContractTrigger.contractAt(PsmAbi.class, getAddress());
    }

    @Override
    public boolean init() {
        try {
            tokens.add(getUsddFromChain());
            tokens.add(getGemFromChain());
            updateName();
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    public Token getUsdd() {
        final int usddId = 0;
        return tokens.get(usddId);
    }

    public Token getGem() {
        final int gemId = 1;
        return tokens.get(gemId);
    }

    private Token getUsddFromChain() {
        List<Type> response = abi.invoke(PsmAbi.Functions.USDD, Collections.emptyList());
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return null != contract
               ? (Token) contract
               : (Token) contractManager.registerContract(new TRC20(tokenAddress));
    }

    private Token getGemFromChain() {
        List<Type> response = abi.invoke(PsmAbi.Functions.GEM_JOIN, Collections.emptyList());
        String gemJoinAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        response = abi.invoke(gemJoinAddress, PsmAbi.Functions.GEM, Collections.emptyList());
        String tokenAddress
            = AddressConverter.EthToTronBase58Address(((Address) response.get(0)).getValue());
        Contract contract = contractManager.getContract(tokenAddress);
        return null != contract
               ? (Token) contract
               : (Token) contractManager.registerContract(new TRC20(tokenAddress));
    }

    @Override
    public void sync() {
        // TODO finish implementation
    }

    @Override
    public void onEvent(KafkaMessage<ContractLog> kafkaMessage) {
        // TODO finish implementation

    }
}
