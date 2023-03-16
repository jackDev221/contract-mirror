package org.tron.sunio.contract_mirror.mirror.utils;

import org.junit.jupiter.api.Test;
import org.tron.sunio.contract_mirror.event_decode.logdata.ContractLog;
import org.tron.sunio.contract_mirror.mirror.contracts.events.ContractEventWrap;
import org.tron.sunio.contract_mirror.mirror.contracts.events.IContractEventWrap;
import org.tron.sunio.tronsdk.WalletUtil;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class EventLogUtils {

    public static IContractEventWrap generateContractEvent(String uniqueID, String[] topics, String data) {
        return generateContractEvent(uniqueID, topics, data, System.currentTimeMillis());
    }

    public static IContractEventWrap generateContractEvent(String uniqueID, String[] topics, String data, long timeStamp) {
        ContractLog contractLog = new ContractLog();
        contractLog.setUniqueId(uniqueID);
        contractLog.setTopicList(topics);
        contractLog.setData(data);
        contractLog.setTimeStamp(timeStamp);
        return new ContractEventWrap(contractLog);
    }
    @Test
    public void  decodeInput(){
        /**
         * swapExactTokensForTokens(uint256 amountIn,
         * uint256 amountOutMin,
         * address[] path,
         * string[] poolVersion,
         * uint256[] versionLen, address to, uint256 deadline)
         */

        String params = "0x0000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000000000000000000000000000000000700000000000000000000000000000000000000000000000000000000000000e0000000000000000000000000000000000000000000000000000000000000018000000000000000000000000000000000000000000000000000000000000002c0000000000000000000000000dcfe9e2a5e07bc63b2d1a55e7fca5fe5621b346b00000000000000000000000000000000000000000000000000000000641190010000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000000000000000000000000000094f24e992ca04b49c6f2a2753076ef8938ed4daa000000000000000000000000834295921a488d9d42b4b3021ed1a3c39fb0f03e000000000000000000000000a614f803b6fd780986a42c78ec9c7f77e6ded13c0000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000000000000000000000027631000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000675736a70736d000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000086f6c6433706f6f6c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001";
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Uint256>() {
        });
        outputParameters.add(new TypeReference<Uint256>() {});
        outputParameters.add(new TypeReference<DynamicArray<Address>>(){});
        outputParameters.add(new TypeReference<DynamicArray<Utf8String>>(){});
        outputParameters.add(new TypeReference<DynamicArray<Uint256>>(){});
        outputParameters.add(new TypeReference<Address>() {});
        outputParameters.add(new TypeReference<Uint256>() {});
        List<Type> res;
        res = FunctionReturnDecoder.decode(params, Utils.convert(outputParameters));
        if (res.size() == 0) {
            throw new RuntimeException("Decode failed");
        }
        BigInteger amountIn = (BigInteger) res.get(0).getValue();
        System.out.println("amountIn: "+ amountIn );
        BigInteger amountOutMin = (BigInteger) res.get(1).getValue();
        System.out.println("amountOutMin: "+ amountOutMin );
        List<Address> addresses = ((DynamicArray<Address>) res.get(2)).getValue();
        System.out.print("path[");
        for (Address addr : addresses){
            System.out.print( WalletUtil.hexStringToTron(addr.toString()) + ",");
        }
        System.out.print("]\n");
        List<Utf8String> versions = ((DynamicArray<Utf8String>) res.get(3)).getValue();
        System.out.print("poolVersion[");
        for(Utf8String v: versions){
            System.out.print(v.getValue()+ ",");
        }
        System.out.print("]\n");

        List<Uint256> versionLen = ((DynamicArray<Uint256>) res.get(4)).getValue();
        System.out.print("versionLen[");
        for(Uint256 l: versionLen){

            System.out.print(((BigInteger)l.getValue()).toString() + ",");
        }
        System.out.print("]\n");


    }

}
