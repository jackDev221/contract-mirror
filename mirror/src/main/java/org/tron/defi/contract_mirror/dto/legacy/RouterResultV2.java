package org.tron.defi.contract_mirror.dto.legacy;

import lombok.Data;
import org.tron.defi.contract_mirror.core.Contract;
import org.tron.defi.contract_mirror.core.pool.Pool;
import org.tron.defi.contract_mirror.core.token.IToken;
import org.tron.defi.contract_mirror.dao.RouterPath;

import java.util.ArrayList;
import java.util.List;

@Data
public class RouterResultV2 {
    private List<String> roadForAddr;
    private List<String> roadForName;
    private List<String> pool;
    private String inUsd;
    private String outUsd;
    private String amount;
    private String impact;
    private String fee;

    public static RouterResultV2 fromRouterPath(RouterPath path) {
        RouterResultV2 resultV2 = new RouterResultV2();
        resultV2.setAmount(path.getAmountOut().toString());

        int n = path.getSteps().size();
        List<String> roadForAddr = new ArrayList<>(n + 1);
        List<String> roadForName = new ArrayList<>(n + 1);
        List<String> pool = new ArrayList<>(n);

        Contract fromToken = path.getFrom().getToken();
        roadForAddr.add(fromToken.getAddress());
        roadForName.add(((IToken) fromToken).getSymbol());
        for (RouterPath.Step step : path.getSteps()) {
            pool.add(getPoolVersion(step.getEdge().getPool()));
            Contract toToken = step.getEdge().getTo().getToken();
            roadForAddr.add(toToken.getAddress());
            roadForName.add(((IToken) toToken).getSymbol());
        }
        return resultV2;
    }

    private static String getPoolVersion(Pool pool) {
        switch (pool.getType()) {
            case SUNSWAP_V1:
                return "v1";
            case SUNSWAP_V2:
            case WTRX:
                return "v2";
            default:
                return pool.getName();
        }
    }
}
