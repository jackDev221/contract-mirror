package org.tron.defi.contract.abi;

import lombok.Getter;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class EventPrototype {
    private static final String HEX_PREFIX = "0x";
    private final String name;
    private final List<TypeReference<Type>> indexedParams = new ArrayList<>();
    private final List<TypeReference<Type>> nonIndexedParams = new ArrayList<>();
    private final String rawSignature;
    private final String signature;

    public EventPrototype(String name, String parameters) {
        this.name = name;
        List<Parameter> parametersList = Arrays.stream(parameters.split(","))
                                               .filter(description -> !description.isEmpty())
                                               .map(description -> new Parameter(description))
                                               .collect(Collectors.toList());
        try {
            for (Parameter parameter : parametersList) {
                if (parameter.isIndexed()) {
                    indexedParams.add(TypeReference.makeTypeReference(parameter.getType(),
                                                                      true,
                                                                      false));
                } else {
                    nonIndexedParams.add(TypeReference.makeTypeReference(parameter.getType()));
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        rawSignature = name +
                       "(" +
                       parametersList.stream()
                                     .map(parameter -> parameter.getType())
                                     .collect(Collectors.joining(",")) +
                       ")";
        signature = EventEncoder.buildEventSignature(rawSignature).substring(HEX_PREFIX.length());
    }

    @Getter
    public class Parameter {
        private final String type;
        private final boolean indexed;

        Parameter(String description) {
            String[] fields = description.split(" ");
            if (fields.length > 2) {
                throw new IllegalArgumentException();
            }
            type = fields[0].strip();
            indexed = fields.length == 2 && fields[1].strip().equals("indexed");
        }
    }
}
