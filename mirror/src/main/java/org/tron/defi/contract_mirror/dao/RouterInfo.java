package org.tron.defi.contract_mirror.dao;

import lombok.Data;

import java.util.List;

@Data
public class RouterInfo {
    private int totalCandidates;
    private List<RouterPath> paths;
}
