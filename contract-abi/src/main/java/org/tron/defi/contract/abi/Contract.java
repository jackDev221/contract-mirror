package org.tron.defi.contract.abi;

import org.tron.defi.contract.log.ContractLog;
import org.web3j.abi.DefaultFunctionEncoder;
import org.web3j.abi.DefaultFunctionReturnDecoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;

import java.util.ArrayList;
import java.util.List;

public abstract class Contract implements ContractAbi {
    private final ContractTrigger trigger;
    private final String address;

    public Contract(ContractTrigger trigger, String address) {
        this.trigger = trigger;
        this.address = address;
    }

    @Override
    public List<Type> invoke(IFunction function, List<Object> params) {
        return invoke(address, function, params);
    }


    @Override
    public List<Type> invoke(String address, IFunction function, List<Object> params) {

        FunctionPrototype prototype = getFunction(function.getPrototype().getRawSignature());
        if (null == prototype) {
            throw new RuntimeException("FUNCTION NOT EXISTS " + function.getPrototype().toString());
        }
        try {
            Function f = DefaultFunctionEncoder.makeFunction(prototype.getName(),
                                                             prototype.getInputTypes(),
                                                             params,
                                                             prototype.getOutputTypes());
            String rawResponse = trigger.trigger(address,
                                                 prototype.getRawSignature(),
                                                 DefaultFunctionEncoder.encodeConstructor(f.getInputParameters()));
            if (rawResponse.isEmpty()) {
                throw new RuntimeException();
            }
            return DefaultFunctionReturnDecoder.decode(rawResponse, f.getOutputParameters());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EventValues decodeEvent(ContractLog message) {
        try {
            String[] topics = message.getRawData().getTopics();
            EventPrototype prototype = getEvent(topics[0]);
            if (topics.length - 1 != prototype.getIndexedParams().size()) {
                throw new IllegalArgumentException("TOPIC SIZE NOT MATCH");
            }
            List<Type> indexedValues = new ArrayList<>();
            for (int i = 1; i < topics.length; i++) {
                indexedValues.add(DefaultFunctionReturnDecoder.decodeIndexedValue(topics[i],
                                                                                  prototype.getIndexedParams()
                                                                                           .get(i)));
            }
            List<Type> nonIndexedValues = DefaultFunctionReturnDecoder.decode(message.getRawData()
                                                                                     .getData(),
                                                                              prototype.getNonIndexedParams());
            return new EventValues(indexedValues, nonIndexedValues);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return null;
        }
    }
}
