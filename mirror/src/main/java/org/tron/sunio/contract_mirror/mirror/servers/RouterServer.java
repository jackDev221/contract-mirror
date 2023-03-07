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
import org.tron.sunio.contract_mirror.mirror.router.CacheNode;
import org.tron.sunio.contract_mirror.mirror.router.StepInfo;
import org.tron.sunio.contract_mirror.mirror.router.RoutItem;
import org.tron.sunio.contract_mirror.mirror.router.RoutNode;
import org.tron.sunio.contract_mirror.mirror.router.RouterInput;
import org.web3j.utils.Strings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.IS_DEBUG;

@Slf4j
@Service
public class RouterServer {
    private static final String SPLIT = "_";
    private static final String TRX_SYMBOL = "TRX";
    private static final String USDD = "USDD";
    private static final String USDT = "USDT";
    private static final String USDC = "USDC";
    private static final String USDJ = "USDJ";
    private static final String TUSD = "TUSD";
    private static final String POOL_TYPE_V1 = "v1";
    private static final String POOL_TYPE_V2 = "v2";

    private ConcurrentMap<String, RoutNode> routNodeMap = new ConcurrentHashMap<>();
    // 之后考虑用缓存
    private ConcurrentMap<String, List<List<StepInfo>>> cachedPaths = new ConcurrentHashMap<>();

    @Autowired
    private TokenPrice tokenPrice;

    private Map<String, String> baseTokensMap = new HashMap<>();
    private Map<String, String> baseTokenSymbolsMap = new HashMap<>();

    @Autowired
    private RouterConfig routerConfig;

    public List<RoutItem> getRouter(RouterInput routerInput, Map<String, BaseContract> contractMaps) {
        log.info("getRouter:Receive router call:{}", routerInput);
        convertTrxInRouterInput(routerInput);
        RoutNode routNode = routNodeMap.get(routerInput.getFromToken());
        List<RoutItem> res = new ArrayList<>();
        if (!ObjectUtil.isNull(routNode)) {
            List<List<StepInfo>> paths;
            long t0 = System.currentTimeMillis();
            String key = genListPathsKey(routerInput.getFromToken(), routerInput.getToToken(), routerInput.isUseBaseTokens());
            paths = cachedPaths.get(key);
            if (ObjectUtil.isNull(paths)) {
                paths = new ArrayList<>();
                getPathsNoRecurrence(routNode, routerInput.getToToken(), paths, routerConfig.getMaxHops(),
                        routerInput.isUseBaseTokens());
                if (paths.size() != 0) {
                    cachedPaths.put(key, paths);
                }
            }
            long t1 = System.currentTimeMillis();
            log.info("getRouter finish get paths, size:{}, cast:{}", paths.size(), t1 - t0);
            BigDecimal outAmountUnit = new BigDecimal(BigInteger.TEN.pow(routerInput.getToDecimal()));
            BigDecimal inAmountUnit = new BigDecimal(BigInteger.TEN.pow(routerInput.getFromDecimal()));
            for (List<StepInfo> path : paths) {
                RoutItem routItem = getRoutItemByPaths(routerInput, path, contractMaps, inAmountUnit, outAmountUnit);
                if (ObjectUtil.isNull(routItem)) {
                    continue;
                }
                res.add(routItem);
            }
            long t2 = System.currentTimeMillis();
            log.info("getRouter finish calc result, cast {}", t2 - t1);
            res = res.stream().sorted(Comparator.comparing(RoutItem::getAmountV, (s1, s2) -> {
                return (new BigDecimal(s2)).compareTo(new BigDecimal(s1));
            })).collect(Collectors.toList());
            if (res.size() > routerConfig.getMaxResultSize()) {
                res = res.subList(0, routerConfig.getMaxResultSize());
            }
            long t3 = System.currentTimeMillis();
            log.info("getRouter finish sorted result, cast {}", t3 - t2);
        }
        // 数量不够补齐
        for (int i = res.size(); i < routerConfig.getMaxResultSize(); i++) {
            res.add(RoutItem.getNullInstance());
        }
        return res;
    }

    private void convertTrxInRouterInput(RouterInput routerInput) {
        if (ObjectUtil.isNull(routerInput)) {
            return;
        }
        if (Strings.isEmpty(routerInput.getFromToken()) || routerInput.getFromToken().equalsIgnoreCase("null")) {
            routerInput.setFromToken(EMPTY_ADDRESS);
            routerInput.setFromTokenSymbol(TRX_SYMBOL);
        }
        if (Strings.isEmpty(routerInput.getToToken()) || routerInput.getToToken().equalsIgnoreCase("null")) {
            routerInput.setToToken(EMPTY_ADDRESS);
            routerInput.setToTokenSymbol(TRX_SYMBOL);
        }

    }

    public String[] getTokenPrice(String fromTokenAddr, String toTokenAddr, String fromTokenSymbol,
                                  String toTokenSymbol) throws Exception {
        String[] res = new String[2];
        res[0] = this.tokenPrice.price(routerConfig.getPriceUrl(), getTokenPriceInput(fromTokenAddr, fromTokenSymbol));
        res[1] = this.tokenPrice.price(routerConfig.getPriceUrl(), getTokenPriceInput(toTokenAddr, toTokenSymbol));
        return res;
    }

    private String getTokenPriceInput(String tokenAddress, String tokenSymbol) {
        String res = tokenSymbol;
        if (routerConfig.getEnv().equals(RouterConfig.ENV_NILE)) {
            res = tokenAddress;
        }
        if (res.equalsIgnoreCase("null")) {
            res = TRX_SYMBOL;
        }
        return res;
    }

    private RoutItem getRoutItemByPaths(RouterInput routerInput, List<StepInfo> path, Map<String, BaseContract> contractMaps,
                                        BigDecimal inAmountUnit, BigDecimal outAmountUnit) {
        RoutItem routItem = new RoutItem();
        List<String> roadForName = routItem.getRoadForName();
        List<String> roadForAddr = routItem.getRoadForAddr();
        List<String> pool = routItem.getPool();
        roadForName.add(routerInput.getFromTokenSymbol());
        roadForAddr.add(routerInput.getFromToken());
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
            fromToken = toToken;
        }
        BigDecimal dAmount = new BigDecimal(swapResult.amount);
        BigDecimal dIn = new BigDecimal(routerInput.getIn());
        routItem.setAmountV(swapResult.amount);
        String amount = dAmount.divide(outAmountUnit, routerInput.getToDecimal(), RoundingMode.DOWN).toString();
        routItem.setAmount(amount);
        BigDecimal fee = new BigDecimal(swapResult.fee).multiply(new BigDecimal(routerInput.getIn()))
                .divide(inAmountUnit, routerInput.getFromDecimal(), RoundingMode.DOWN);
        routItem.setFee(fee.toString());
        routItem.setInUsd(routerInput.getFromPrice().multiply(dIn).divide(inAmountUnit, RoundingMode.DOWN).toString());
        routItem.setOutUsd(routerInput.getToPrice().multiply(dAmount).divide(outAmountUnit, RoundingMode.DOWN).toString());
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
                int maxCoin = baseStableSwapPool.getCoinsCount() - 1;
                int metaJ = j - maxCoin < 0 ? j : maxCoin;
                long timestamp = System.currentTimeMillis() / 1000;
                if (i + j == -1) {
                    res.fee = preFee + (1 - preFee) * baseStableSwapPool.calcFee(timestamp, j);
                    res.amount = curve.exchange(i, j, amount, BigInteger.ZERO, timestamp);
                } else {
                    if (i - maxCoin < 0 || j - maxCoin < 0) {
                        res.fee = preFee + (1 - preFee) * baseStableSwapPool.calcFee(timestamp, metaJ);
                    } else {
                        res.fee = preFee + (1 - preFee) * baseStableSwapPool.calcBasePoolFee(timestamp, metaJ);
                    }
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
            if (indexes[0] < 0 || indexes[1] < 0) {
                log.error("Curve fail, from:{}, to:{}, contract:{}, amount:{}, wrong input tokens:{} {}",
                        fromAddress, toAddress, baseContract.getAddress(), amount, fromAddress, toAddress);
                res.amount = BigInteger.ZERO;
            } else {
                res.fee = preFee + (1 - preFee) * curve.calcFee(0, indexes[1]);
                res.amount = curve.exchange(indexes[0], indexes[1], amount, BigInteger.ZERO, System.currentTimeMillis() / 1000, data);
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

    private boolean getPathsNoRecurrence(RoutNode routNode, String destToken, List<List<StepInfo>> paths, int maxHops,
                                         boolean isUseBaseTokens) {
        List<CacheNode> cacheNodes = new ArrayList<>();
        cacheNodes.add(new CacheNode(routNode));
        List<StepInfo> path = new ArrayList<>();
        while (cacheNodes.size() > 0) {
            RoutNode node = cacheNodes.get(cacheNodes.size() - 1).getSubNode();
            while (ObjectUtil.isNull(node)) {
                if (path.size() > 0) {
                    path.remove(path.size() - 1);
                }
                cacheNodes.remove(cacheNodes.size() - 1);
                if (cacheNodes.size() > 0) {
                    node = cacheNodes.get(cacheNodes.size() - 1).getSubNode();
                } else {
                    return true;
                }
            }
            boolean isNodeAvailing = (!isPathContainToken(path, node.getContract(), node.getAddress())
                    && isTokenUsable(isUseBaseTokens, node.getAddress(), node.getSymbol(), node.getAddress(), destToken));
            RoutNode nextRoot = this.routNodeMap.get(node.getAddress());
            if (ObjectUtil.isNull(nextRoot) || !isNodeAvailing) {
                continue;
            }
            StepInfo stepInfo = StepInfo.builder()
                    .contract(node.getContract())
                    .tokenAddress(node.getAddress())
                    .tokenName(node.getSymbol())
                    .poolType(node.getPoolType())
                    .build();
            path.add(stepInfo);
            if (node.getAddress().equalsIgnoreCase(destToken)) {
                List<StepInfo> pathCopy = path.stream().collect(Collectors.toList());
                paths.add(pathCopy);
                path.remove(path.size() - 1);
                continue;
            }

            if (path.size() >= maxHops) {
                path.remove(path.size() - 1);
                continue;
            }
            cacheNodes.add(new CacheNode(nextRoot));
        }

        return true;
    }

    private boolean getPaths(RoutNode routNode, String fromToken, String destToken, List<StepInfo> path, List<List<StepInfo>> paths, int maxHops,
                             boolean isUseBaseTokens) {
        if (path.size() > maxHops) {
            return false;
        }
        if (routNode.getAddress().equalsIgnoreCase(destToken)) {
            paths.add(path);
            return true;
        }
        for (RoutNode subNode : routNode.getSubNodes()) {
            if (isPathContainToken(path, subNode.getContract(), subNode.getAddress())
                    || !isTokenUsable(isUseBaseTokens, subNode.getAddress(), subNode.getSymbol(), fromToken, destToken)) {
                continue;
            }
            RoutNode nextRoot = this.routNodeMap.get(subNode.getAddress());
            if (ObjectUtil.isNull(nextRoot)) {
                continue;
            }
            StepInfo stepInfo = StepInfo.builder()
                    .contract(subNode.getContract())
                    .tokenAddress(subNode.getAddress())
                    .tokenName(subNode.getSymbol())
                    .poolType(subNode.getPoolType())
                    .build();
            List<StepInfo> pathCopy = path.stream().collect(Collectors.toList());
            pathCopy.add(stepInfo);
            getPaths(nextRoot, fromToken, destToken, pathCopy, paths, maxHops, isUseBaseTokens);

        }
        return false;
    }

    public void initRoutNodeMap(Map<String, BaseContract> contractMaps) {
        log.info("Start initRoutNodeMap");
        this.initBaseTokensMap();
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
        showRoads();
    }

    private void showRoads() {
        if (IS_DEBUG) {
            for (RoutNode routNode : this.routNodeMap.values()) {
                String start = routNode.getSymbol() + "/" + routNode.getAddress();
                StringBuffer buff = new StringBuffer();

                for (RoutNode subNode : routNode.getSubNodes()) {
                    buff.append(subNode.getSymbol());
                    buff.append("/");
                    buff.append(subNode.getAddress());
                    buff.append(" ");
                }

                System.out.println(start + " ---> " + buff.toString());
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
        String contract = data.getAddress();
        for (int i = 0; i < tokens.size(); i++) {
            if (i == 1) {
                continue;
            }
            for (int j = i + 1; j < tokens.size(); j++) {
                updateRoutNodeMap(tokens.get(i), symbols.get(i), tokens.get(j), symbols.get(j), data.getPoolName(), contract);
                updateRoutNodeMap(tokens.get(j), symbols.get(j), tokens.get(i), symbols.get(i), data.getPoolName(), contract);
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
        updateRoutNodeMap(token0, token0Symbol, token1, token1Symbol, data.getPoolName(), contract);
        updateRoutNodeMap(token1, token1Symbol, token0, token0Symbol, data.getPoolName(), contract);
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
        String contract = data.getAddress();
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                String token0 = data.getCoins()[i];
                String token1 = data.getCoins()[j];
                String token0Symbol = data.getCoinSymbols()[i];
                String token1Symbol = data.getCoinSymbols()[j];
                updateRoutNodeMap(token0, token0Symbol, token1, token1Symbol, data.getPoolName(), contract);
                updateRoutNodeMap(token1, token1Symbol, token0, token0Symbol, data.getPoolName(), contract);
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
        if (Strings.isEmpty(token0Symbol) || Strings.isEmpty(token1Symbol)) {
            log.warn("Some token symbol is empty, info:{}, {}, {}, {}, {}, {}", token0, token0Symbol, token1, token1Symbol, poolType, contract);
            return;
        }
        RoutNode routNode = routNodeMap.getOrDefault(token0, new RoutNode(token0, token0Symbol, "", ""));
        RoutNode subNode = new RoutNode(token1, token1Symbol, contract, poolType);
        routNode.getSubNodes().add(subNode);
        routNodeMap.put(token0, routNode);
    }

    private String genListPathsKey(String token0, String token1, boolean isUseBaseTokens) {
        String index = isUseBaseTokens ? "1" : "0";
        return String.format("%s%s%s%s%s", token0, SPLIT, token1, SPLIT, index);
    }

    private void initBaseTokensMap() {
        if (!Strings.isEmpty(routerConfig.getBaseTokens())) {
            String[] addrs = routerConfig.getBaseTokens().split(",");
            for (String addr : addrs) {
                baseTokensMap.put(addr, addr);
            }
        }

        if (!Strings.isEmpty(routerConfig.getBaseTokenSymbols())) {
            String[] symbols = routerConfig.getBaseTokenSymbols().split(",");
            for (String symbol : symbols) {
                baseTokenSymbolsMap.put(symbol, symbol);
            }
        }

    }

    private boolean isTokenUsable(boolean isUseBaseTokens, String tokenAddr, String tokenSymbol, String fromAddress, String destAddress) {
        if (!isUseBaseTokens || destAddress.equalsIgnoreCase(tokenAddr)) {
            return true;
        }
        return baseTokensMap.containsKey(tokenAddr) || baseTokenSymbolsMap.containsKey(tokenSymbol) || fromAddress.equalsIgnoreCase(tokenAddr);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SwapResult {
        private BigInteger amount;
        private double fee;
    }

}
