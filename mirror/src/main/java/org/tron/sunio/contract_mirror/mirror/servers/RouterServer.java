package org.tron.sunio.contract_mirror.mirror.servers;

import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.sunio.contract_mirror.mirror.config.RouterConfig;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.AbstractCurve;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.BaseStableSwapPool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.CurveBasePool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.PSM;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV1;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV2Pair;
import org.tron.sunio.contract_mirror.mirror.dao.CurveBasePoolData;
import org.tron.sunio.contract_mirror.mirror.dao.PSMData;
import org.tron.sunio.contract_mirror.mirror.dao.StableSwapPoolData;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.price.TokenPrice;
import org.tron.sunio.contract_mirror.mirror.router.StepInfo;
import org.tron.sunio.contract_mirror.mirror.router.RoutItem;
import org.tron.sunio.contract_mirror.mirror.router.RoutNode;
import org.tron.sunio.contract_mirror.mirror.router.RouterInput;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;

@Slf4j
@Service
public class RouterServer {
    private static final String SPLIT = "_";
    private static final String TRX_SYMBOL = "TRX";
    private static final String USDD = "usdd";
    private static final String USDT = "usdt";
    private static final String USDC = "usdc";
    private static final String USDJ = "usdj";
    private static final String TUSD = "tusd";
    private static final String POOL_TYPE_V1 = "v1";
    private static final String POOL_TYPE_V2 = "v2";
    private static final String POOL_TYPE_2_POOL = "2pool";
    private static final String POOL_TYPE_3_POOL = "3pool";
    private static final String POOL_TYPE_4_POOL = "4pool";
    private static final String POOL_TYPE_PSM = "psm";

    private ConcurrentMap<String, RoutNode> routNodeMap = new ConcurrentHashMap<>();
    // 之后考虑用缓存
    private ConcurrentMap<String, List<List<StepInfo>>> cachedPaths = new ConcurrentHashMap<>();

    @Autowired
    private TokenPrice tokenPrice;

    @Autowired
    private RouterConfig routerConfig;

    public List<RoutItem> getRouter(RouterInput routerInput, Map<String, BaseContract> contractMaps) {
        RoutNode routNode = routNodeMap.get(routerInput.getFromToken());
        if (ObjectUtil.isNull(routNode)) {
            initRoutNodeMap(contractMaps);
            routNode = routNodeMap.get(routerInput.getFromToken());
        }
        if (ObjectUtil.isNull(routNode)) {
            return null;
        }
        List<List<StepInfo>> paths;

        String key = genListPathsKey(routerInput.getFromToken(), routerInput.getToToken());
        paths = cachedPaths.get(key);
        if (ObjectUtil.isNull(paths)) {
            paths = new ArrayList<>();
            getPaths(routNode, routerInput.getToToken(), new ArrayList<>(), paths, routerConfig.getMaxHops());
            if (paths.size() != 0) {
                cachedPaths.put(key, paths);
            }
        }
        if (paths.size() == 0) {
            return null;
        }
        List<RoutItem> res = new ArrayList<>();
        for (List<StepInfo> path : paths) {
            RoutItem routItem = getRoutItemByPaths(routerInput, path, contractMaps);
            res.add(routItem);
        }
        res = res.stream().sorted(Comparator.comparing(RoutItem::getAmount, (s1, s2) -> {
            return (new BigDecimal(s2)).compareTo(new BigDecimal(s1));
        })).collect(Collectors.toList());

        return res;
    }

    public String[] getTokenPrice(String fromTokenAddr, String toTokenAddr, String fromTokenSymbol,
                                  String toTokenSymbol) throws Exception {
        String[] res = new String[2];
        res[0] = this.tokenPrice.price(routerConfig.getPriceUrl(), getTokenPriceInput(fromTokenAddr, fromTokenSymbol));
        res[1] = this.tokenPrice.price(routerConfig.getPriceUrl(), getTokenPriceInput(toTokenAddr, toTokenSymbol));
        return res;
    }

    private String getTokenPriceInput(String tokenAddress, String tokenSymbol) {
        String res = tokenAddress;
        if (routerConfig.getEnv().equals(RouterConfig.ENV_NILE)) {
            res = tokenSymbol;
        }
        if (res.equalsIgnoreCase("null")) {
            res = TRX_SYMBOL;
        }
        return res;
    }

    private RoutItem getRoutItemByPaths(RouterInput routerInput, List<StepInfo> path, Map<String, BaseContract> contractMaps) {
        RoutItem routItem = new RoutItem();
        List<String> roadForName = routItem.getRoadForName();
        List<String> roadForAddr = routItem.getRoadForAddr();
        List<String> pool = routItem.getPool();
        SwapResult swapResult = new SwapResult(routerInput.getIn(), 0);
        String fromToken = routerInput.getFromToken();
        String toToken = "";
        for (int i = 0; i < path.size(); i++) {
            StepInfo info = path.get(i);
            roadForAddr.add(info.getTokenAddress());
            roadForName.add(info.getTokenName());
            pool.add(info.getPoolType());
            toToken = info.getTokenAddress();
            swapResult = swapToken(fromToken, toToken, swapResult.amount, swapResult.fee, contractMaps.get(info.getContract()));
            if (swapResult.amount.compareTo(BigInteger.ZERO) == 0) {
                log.error("Cal:from {}, to: {},  contract:{}, amount is zero", fromToken, toToken, info.getContract());
                return null;
            }
        }
        routItem.setAmount(swapResult.amount);
        routItem.setFee(swapResult.fee);
        routItem.setInUsd(routerInput.getFromPrice());
        routItem.setOutUsd(routerInput.getToPrice());
        return routItem;
    }

    private SwapResult swapToken(String fromAddress, String toAddress, BigInteger amount, double preFee, BaseContract baseContract) {
        SwapResult swapResult = new SwapResult();
        if (ObjectUtil.isNull(baseContract)) {
            return null;
        }
        switch (baseContract.getType()) {
            case SWAP_V1:
                swapResult = swapV1(fromAddress, toAddress, amount, preFee, baseContract);
                break;
            case SWAP_V2_PAIR:
                swapResult = swpV2Pair(fromAddress, toAddress, amount, preFee, baseContract);
                break;
            case CONTRACT_CURVE_2POOL:
            case CONTRACT_CURVE_3POOL:
                swapResult = curveSwap(fromAddress, toAddress, amount, preFee, baseContract);
                break;
            case CONTRACT_PSM_USDT:
            case CONTRACT_PSM_USDC:
            case CONTRACT_PSM_USDJ:
            case CONTRACT_PSM_TUSD:
                swapResult = psmSwap(fromAddress, toAddress, amount, preFee, baseContract);
                break;
            case STABLE_SWAP_POOL:
                swapResult = stableSwapPoolSwap(fromAddress, toAddress, amount, preFee, baseContract);
                break;
            default:
                break;
        }
        return swapResult;
    }

    private SwapResult stableSwapPoolSwap(String fromAddress, String toAddress, BigInteger amount, double preFee, BaseContract baseContract) {
        SwapResult res = new SwapResult();
        try {
            BaseStableSwapPool baseStableSwapPool = (BaseStableSwapPool) baseContract;
            StableSwapPoolData data = baseStableSwapPool.getCurveBasePoolData();
            AbstractCurve curve = baseStableSwapPool.copySelf();
            int i = data.getTokenIndex(fromAddress);
            int j = data.getTokenIndex(toAddress);
            if (i == -2 || j == -2) {
                res.amount = BigInteger.ZERO;
            } else {
                long timestamp = System.currentTimeMillis() / 1000;
                res.fee = preFee + (1 - preFee) * baseStableSwapPool.calcFee(timestamp, j);
                if (i + j == -1) {
                    res.amount = curve.exchange(i, j, amount, BigInteger.ZERO, timestamp);
                } else {
                    res.amount = curve.exchangeUnderlying(i, j, amount, BigInteger.ZERO, timestamp);
                }
            }
        } catch (Exception e) {
            log.error("StableSwapPool fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), amount, e);
            res.amount = BigInteger.ZERO;
        }
        return res;
    }

    private SwapResult psmSwap(String fromAddress, String toAddress, BigInteger amount, double preFee, BaseContract baseContract) {
        SwapResult res = new SwapResult();
        try {
            PSM psm = (PSM) baseContract;
            PSMData data = psm.getPsmData();
            if (fromAddress.equalsIgnoreCase(data.getUsdd())) {
                res.amount = psm.calcUSDDToUSDX(amount, data.getType(), data.getTout())[0];
                res.fee = preFee + (1 - preFee) * psm.calcUSDDToUSDXFee(data.getTout());
            } else {
                res.amount = psm.calcUSDXToUSDD(amount, data.getType(), data.getTin())[1];
                res.fee = preFee + (1 - preFee) * psm.calcUSDXToUSDDFee(data.getTin());
            }

        } catch (Exception e) {
            log.error("PSM fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), amount, e);
            res.amount = BigInteger.ZERO;
        }
        return res;
    }

    private SwapResult curveSwap(String fromAddress, String toAddress, BigInteger amount, double preFee, BaseContract baseContract) {
        SwapResult res = new SwapResult();
        try {
            CurveBasePool curve = (CurveBasePool) ((AbstractCurve) baseContract).copySelf();

            CurveBasePoolData data = curve.getCurveBasePoolData();
            int[] indexes = data.getTokensIndex(fromAddress, toAddress);
            if (indexes[0] <= 0 || indexes[1] < 0) {
                log.error("Curve fail, from:{}, to:{}, contract:{}, amount:{}, wrong input tokens:{} {}",
                        fromAddress, toAddress, baseContract.getAddress(), amount, fromAddress, toAddress);
                res.amount = BigInteger.ZERO;
            } else {
                res.fee = preFee + (1 - preFee) * curve.calcFee(0, indexes[1]);
                res.amount = curve.exchange(indexes[0], indexes[1], amount, BigInteger.ZERO, data);
            }
        } catch (Exception e) {
            log.error("Curve fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), amount, e);
            res.amount = BigInteger.ZERO;
        }
        return res;
    }

    private SwapResult swpV2Pair(String fromAddress, String toAddress, BigInteger amount, double preFee, BaseContract baseContract) {
        SwapResult res = new SwapResult();
        res.fee = preFee + (1 - preFee) * 0.003;
        try {
            SwapV2Pair swapV2 = (SwapV2Pair) baseContract;
            res.amount = swapV2.getAmountOut(fromAddress, toAddress, amount, swapV2.getSwapV2PairData());
        } catch (Exception e) {
            log.error("SwapV2 fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), amount, e);
            res.amount = BigInteger.ZERO;
        }
        return res;
    }

    private SwapResult swapV1(String fromAddress, String toAddress, BigInteger amount, double preFee, BaseContract baseContract) {
        SwapResult res = new SwapResult();
        res.fee = preFee + (1 - preFee) * 0.003;
        try {
            SwapV1 swapV1 = (SwapV1) baseContract;
            if (fromAddress.equals(EMPTY_ADDRESS)) {
                res.amount = swapV1.trxToTokenInput(amount, BigInteger.ZERO, swapV1.getSwapV1Data());
            } else {
                res.amount = swapV1.tokenToTrxInput(amount, BigInteger.ZERO, swapV1.getSwapV1Data());
            }
        } catch (Exception e) {
            log.error("SwapV1 fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), amount, e);
            res.amount = BigInteger.ZERO;
        }
        return res;
    }

    private boolean isPathContainToken(List<StepInfo> path, String contract, String token) {
        for (StepInfo stepInfo : path) {
            if (stepInfo.getContract().equals(contract) && stepInfo.getTokenAddress().equals(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean getPaths(RoutNode routNode, String destToken, List<StepInfo> path, List<List<StepInfo>> paths, int maxHops) {
        if (path.size() > maxHops) {
            return false;
        }
        if (routNode.getAddress().equalsIgnoreCase(destToken)) {
            paths.add(path);
            return true;
        }
        for (RoutNode subNode : routNode.getSubNodes()) {
            if (isPathContainToken(path, subNode.getContract(), subNode.getAddress())) {
                continue;
            }
            StepInfo stepInfo = StepInfo.builder()
                    .contract(subNode.getContract())
                    .tokenAddress(subNode.getAddress())
                    .tokenName(subNode.getSymbol())
                    .poolType(subNode.getPoolType())
                    .build();
            List<StepInfo> pathCopy = List.copyOf(path);
            pathCopy.add(stepInfo);
            getPaths(subNode, destToken, pathCopy, paths, maxHops);

        }
        return false;
    }

    public void initRoutNodeMap(Map<String, BaseContract> contractMaps) {
        for (BaseContract baseContract : contractMaps.values()) {
            switch (baseContract.getType()) {
                case SWAP_V1:
                    initV1((SwapV1) baseContract);
                    break;
                case SWAP_V2_PAIR:
                    initV2((SwapV2Pair) baseContract);
                    break;
                case CONTRACT_CURVE_2POOL:
                case CONTRACT_CURVE_3POOL:
                    initCurves((CurveBasePool) baseContract);
                    break;
                case CONTRACT_PSM_USDT:
                case CONTRACT_PSM_USDC:
                case CONTRACT_PSM_USDJ:
                case CONTRACT_PSM_TUSD:
                    initPSM((PSM) baseContract);
                    break;
                case STABLE_SWAP_POOL:
                    initStableSwapPool((BaseStableSwapPool) baseContract);
                    break;
            }
        }
    }

    private void initStableSwapPool(BaseStableSwapPool baseStableSwapPool) {
        StableSwapPoolData data = baseStableSwapPool.getCurveBasePoolData();
        List<String> tokens = new ArrayList<>();
        List<String> symbols = new ArrayList<>();
        tokens.addAll(Arrays.asList(data.getCoins()));
        tokens.addAll(Arrays.asList(data.getBaseCoins()));
        symbols.addAll(Arrays.asList(data.getCoinSymbols()));
        symbols.addAll(Arrays.asList(data.getBaseCoinSymbols()));
        String poolType = (tokens.size() - 1) == 2 ? POOL_TYPE_2_POOL : POOL_TYPE_3_POOL;
        String contract = data.getAddress();
        for (int i = 0; i < tokens.size(); i++) {
            if (i == 1) {
                continue;
            }
            for (int j = i + 1; j < tokens.size(); j++) {
                updateRoutNodeMap(tokens.get(i), symbols.get(i), tokens.get(j), symbols.get(j), poolType, contract);
                updateRoutNodeMap(tokens.get(j), symbols.get(j), tokens.get(i), symbols.get(i), poolType, contract);
            }
        }
    }

    private void initPSM(PSM psm) {
        PSMData data = psm.getPsmData();
        String[] tokenInfo = getPSMTokenInfo(psm.getType());
        String token0 = data.getUsdd();
        String token1 = tokenInfo[1];
        String token0Symbol = USDD;
        String token1Symbol = tokenInfo[0];
        String contract = data.getAddress();
        updateRoutNodeMap(token0, token0Symbol, token1, token1Symbol, POOL_TYPE_PSM, contract);
        updateRoutNodeMap(token1, token1Symbol, token0, token0Symbol, POOL_TYPE_PSM, contract);
    }

    private String[] getPSMTokenInfo(ContractType contractType) {
        String[] res;
        switch (contractType) {
            case CONTRACT_PSM_USDT:
                res = new String[]{USDT, routerConfig.getUsdt()};
                break;
            case CONTRACT_PSM_USDC:
                res = new String[]{USDC, routerConfig.getUsdc()};
                break;
            case CONTRACT_PSM_USDJ:
                res = new String[]{USDJ, routerConfig.getUsdj()};
                break;
            case CONTRACT_PSM_TUSD:
                res = new String[]{TUSD, routerConfig.getTusd()};
                break;
            default:
                res = null;
        }
        return res;
    }

    private void initCurves(CurveBasePool curve) {
        CurveBasePoolData data = curve.getCurveBasePoolData();
        int count = data.getCoins().length;
        String poolType = count == 2 ? POOL_TYPE_2_POOL : POOL_TYPE_3_POOL;
        String contract = data.getAddress();
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                String token0 = data.getCoins()[i];
                String token1 = data.getCoins()[j];
                String token0Symbol = data.getCoinNames()[i];
                String token1Symbol = data.getCoinNames()[j];
                updateRoutNodeMap(token0, token0Symbol, token1, token1Symbol, poolType, contract);
                updateRoutNodeMap(token1, token1Symbol, token0, token0Symbol, poolType, contract);
            }
        }
    }

    private void initV1(SwapV1 swapV1) {
        SwapV1Data data = swapV1.getSwapV1Data();
        String token0 = EMPTY_ADDRESS;
        String token1 = data.getTokenAddress();
        String token0Symbol = TRX_SYMBOL;
        String token1Symbol = data.getTokenSymbol();
        String contract = data.getAddress();
        String poolType = POOL_TYPE_V1;
        updateRoutNodeMap(token0, token0Symbol, token1, token1Symbol, poolType, contract);
        updateRoutNodeMap(token1, token1Symbol, token0, token0Symbol, poolType, contract);
    }

    private void initV2(SwapV2Pair swapV2Pair) {
        SwapV2PairData data = swapV2Pair.getSwapV2PairData();
        String token0 = data.getToken0();
        String token1 = data.getToken1();
        String token0Symbol = data.getToken0Symbol();
        String token1Symbol = data.getToken1Symbol();
        String contract = data.getAddress();
        String poolType = POOL_TYPE_V2;
        updateRoutNodeMap(token0, token0Symbol, token1, token1Symbol, poolType, contract);
        updateRoutNodeMap(token1, token1Symbol, token0, token0Symbol, poolType, contract);
    }

    private void updateRoutNodeMap(String token0, String token0Symbol, String token1, String token1Symbol, String poolType,
                                   String contract) {
        RoutNode routNode = routNodeMap.getOrDefault(token0, new RoutNode(token0, token0Symbol, "", ""));
        RoutNode subNode = new RoutNode(token1, token1Symbol, contract, poolType);
        routNode.getSubNodes().add(subNode);
        routNodeMap.put(token0, routNode);
    }

    private String genListPathsKey(String token0, String token1) {
        return String.format("%s%s%s", token0, SPLIT, token1);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SwapResult {
        private BigInteger amount;
        private double fee;
    }

}
