package org.tron.defi.contract.abi;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class FunctionPrototype {
    private final String name;
    private final List<String> inputTypes;
    private final List<String> outputTypes;
    private final String rawSignature;

    public FunctionPrototype(String name, String inputParams, String outputParams) {
        this.name = name;
        inputTypes = Arrays.stream(inputParams.split(",")).filter(type -> !type.isBlank())
                           .collect(Collectors.toList());
        outputTypes = Arrays.stream(outputParams.split(",")).filter(type -> !type.isBlank())
                            .collect(Collectors.toList());
        rawSignature = name + "(" + inputParams + ")";
    }
}
