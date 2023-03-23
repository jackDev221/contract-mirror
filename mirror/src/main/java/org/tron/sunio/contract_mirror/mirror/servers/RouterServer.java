package org.tron.sunio.contract_mirror.mirror.servers;

import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;
import org.tron.sunio.contract_mirror.mirror.config.RouterConfig;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
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
import org.tron.sunio.contract_mirror.mirror.price.TokenPrice;
import org.tron.sunio.contract_mirror.mirror.router.CacheNode;
import org.tron.sunio.contract_mirror.mirror.router.PathCacheContract;
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

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.CALL_FOR_ROUTER;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;
import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.IS_DEBUG;

@Slf4j
@Service
public class RouterServer {
    private static final String SPLIT = "_";
    private static final String TRX_SYMBOL = "TRX";
    private static final String USDD = "USDD";

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
        log.info("RouterServer receive request:{}", GsonUtil.objectToGson(routerInput));
        long t0 = System.currentTimeMillis();
        convertTrxInRouterInput(routerInput);
        RoutNode routNode = routNodeMap.get(routerInput.getFromToken());
        List<RoutItem> res = new ArrayList<>();
        if (!ObjectUtil.isNull(routNode)) {
            List<List<StepInfo>> paths = null;
//            String key = genListPathsKey(routerInput.getFromToken(), routerInput.getToToken(), routerInput.isUseBaseTokens());
//            paths = cachedPaths.get(key);
            if (ObjectUtil.isNull(paths)) {
                paths = new ArrayList<>();
                getPathsNoRecurrence(routNode, routerInput.getFromToken(), routerInput.getToToken(), paths, routerConfig.getMaxHops(),
                        routerInput.isUseBaseTokens());
//                if (paths.size() != 0) {
//                    cachedPaths.put(key, paths);
//                }
            }
            long t1 = System.currentTimeMillis();
            // test
            log.info("RouterServer finish get paths, size:{}, cast:{}", paths.size(), t1 - t0);
            BigDecimal outAmountUnit = new BigDecimal(BigInteger.TEN.pow(routerInput.getToDecimal()));
            BigDecimal inAmountUnit = new BigDecimal(BigInteger.TEN.pow(routerInput.getFromDecimal()));
            PathCacheContract pathCacheContract = new PathCacheContract();
            for (List<StepInfo> path : paths) {
                pathCacheContract.init(contractMaps);
                RoutItem routItem = getRoutItemByPaths(routerInput, path, pathCacheContract, inAmountUnit, outAmountUnit);
                if (ObjectUtil.isNull(routItem)) {
                    continue;
                }
                res.add(routItem);
            }
            long t2 = System.currentTimeMillis();
            log.info("RouterServer finish calc result, cast {}", t2 - t1);
            res = res.stream().sorted(Comparator.comparing(RoutItem::getAmountV, (s1, s2) -> {
                return (new BigDecimal(s2)).compareTo(new BigDecimal(s1));
            })).collect(Collectors.toList());
            if (res.size() > routerConfig.getMaxResultSize()) {
                res = res.subList(0, routerConfig.getMaxResultSize());
            }
        }
        // 数量不够补齐
        for (int i = res.size(); i < routerConfig.getMaxResultSize(); i++) {
            res.add(RoutItem.getNullInstance());
        }
        long t3 = System.currentTimeMillis();
        log.info("RouterServer response, cast:{}", t3 - t0);
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
        String res = tokenAddress;
        if (routerConfig.getEnv().equals(RouterConfig.ENV_NILE)) {
            res = tokenSymbol;
        }
        if (res.equalsIgnoreCase("null")) {
            res = TRX_SYMBOL;
        }
        return res;
    }

    private RoutItem getRoutItemByPaths(RouterInput routerInput, List<StepInfo> path, PathCacheContract pathCacheContract,
                                        BigDecimal inAmountUnit, BigDecimal outAmountUnit) {
        RoutItem routItem = new RoutItem();
        List<String> roadForName = routItem.getRoadForName();
        List<String> roadForAddr = routItem.getRoadForAddr();
        List<String> pool = routItem.getPool();
        roadForName.add(routerInput.getFromTokenSymbol());
        roadForAddr.add(routerInput.getFromToken());
        SwapResult swapResult = new SwapResult(routerInput.getIn(), 0, BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0));
        String fromToken = routerInput.getFromToken();
        String toToken = "";
        for (int i = 0; i < path.size(); i++) {
            StepInfo info = path.get(i);
            roadForAddr.add(info.getTokenAddress());
            roadForName.add(info.getTokenName());
            pool.add(info.getPoolType());
            toToken = info.getTokenAddress();
            BigInteger inputAmount = swapResult.amount;
            swapToken(fromToken, toToken, swapResult, pathCacheContract.getContract(info.getContract()), pathCacheContract);
            if (swapResult.amount.compareTo(BigInteger.ZERO) == 0) {
                log.error("Calc :from {}, to: {},  contract:{}, input : {}, path:{} return amount is zero", fromToken, toToken, info.getContract(), inputAmount, path);
                return null;
            }
            fromToken = toToken;
        }
        BigDecimal dAmount = new BigDecimal(swapResult.amount);
        BigDecimal dIn = new BigDecimal(routerInput.getIn());
        BigDecimal priceDiff = dIn.divide(dAmount, 36, RoundingMode.UP).multiply(swapResult.impactItem0).subtract(swapResult.impactItem1);
        BigDecimal newPriceWithoutFee = dIn.divide(dAmount, 36, RoundingMode.UP);
        BigDecimal impact = priceDiff.divide(newPriceWithoutFee, 6, RoundingMode.UP).abs();
        routItem.setImpact(impact.toString());
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

    private void swapToken(String fromAddress, String toAddress, SwapResult swapResult, BaseContract baseContract, PathCacheContract pathCacheContract) {
        if (ObjectUtil.isNull(baseContract) || !baseContract.isReady()) {
            swapResult.amount = BigInteger.ZERO;
            return;
        }
        switch (baseContract.getType()) {
            case SWAP_V1:
                swapV1(fromAddress, toAddress, swapResult, baseContract);
                break;
            case SWAP_V2_PAIR:
                swpV2Pair(fromAddress, toAddress, swapResult, baseContract);
                break;
            case CONTRACT_CURVE_2POOL:
            case CONTRACT_CURVE_3POOL:
                curveSwap(fromAddress, toAddress, swapResult, baseContract, pathCacheContract);
                break;
            case CONTRACT_PSM:
                psmSwap(fromAddress, toAddress, swapResult, baseContract);
                break;
            case STABLE_SWAP_POOL:
                stableSwapPoolSwap(fromAddress, toAddress, swapResult, baseContract, pathCacheContract);
                break;
            default:
                break;
        }

    }

    private SwapResult stableSwapPoolSwap(String fromAddress, String toAddress, SwapResult swapResult, BaseContract baseContract, PathCacheContract pathCacheContract) {
        try {
            BaseStableSwapPool baseStableSwapPool = (BaseStableSwapPool) baseContract;
            StableSwapPoolData data = baseStableSwapPool.getVarStableSwapBasePoolData();
//            AbstractCurve curve = baseStableSwapPool.copySelf();
            int i = data.getTokenIndex(fromAddress);
            int j = data.getTokenIndex(toAddress);
            if (i == -2 || j == -2) {
                swapResult.amount = BigInteger.ZERO;
            } else {
                int maxCoin = baseStableSwapPool.getCoinsCount() - 1;
                int metaJ = j - maxCoin < 0 ? j : maxCoin;
                long timestamp = System.currentTimeMillis() / 1000;
                swapResult.impactItem0 = swapResult.impactItem0.multiply(BigDecimal.valueOf(0.9996));
                if (i + j == -1) {
                    i = i == -1 ? 1 : 0;
                    j = j == -1 ? 1 : 0;
                    swapResult.fee = swapResult.fee + (1 - swapResult.fee) * baseStableSwapPool.calcFee(CALL_FOR_ROUTER, timestamp, j, pathCacheContract);
                    int dxDecimals = (int) data.getCoinDecimals()[i];
                    BigInteger dx = BigInteger.TEN.pow(dxDecimals);
                    BigInteger dy = baseStableSwapPool.getDy(CALL_FOR_ROUTER, i, j, dx, timestamp, pathCacheContract);
                    swapResult.impactItem1 = swapResult.impactItem1.multiply(new BigDecimal(dx).divide(new BigDecimal(dy), 36, RoundingMode.UP));
                    swapResult.amount = baseStableSwapPool.exchange(CALL_FOR_ROUTER, i, j, swapResult.amount, BigInteger.ZERO, timestamp, pathCacheContract);
                } else {
                    if (i - maxCoin < 0 || j - maxCoin < 0) {
                        swapResult.fee = swapResult.fee + (1 - swapResult.fee) * baseStableSwapPool.calcFee(CALL_FOR_ROUTER, timestamp, metaJ, pathCacheContract);
                    } else {
                        swapResult.fee = swapResult.fee + (1 - swapResult.fee) * baseStableSwapPool.calcBasePoolFee(CALL_FOR_ROUTER, timestamp, metaJ, pathCacheContract);
                    }

                    swapResult.impactItem0 = swapResult.impactItem0.multiply(BigDecimal.valueOf(0.9996));
                    int dxDecimals;
                    if (i < maxCoin) {
                        dxDecimals = (int) data.getCoinDecimals()[i];
                    } else {
                        dxDecimals = (int) data.getBaseCoinDecimals()[i - maxCoin];
                    }
                    BigInteger dx = BigInteger.TEN.pow(dxDecimals);
                    BigInteger dy = baseStableSwapPool.getDyUnderlying(CALL_FOR_ROUTER, i, j, dx, timestamp, pathCacheContract);
                    swapResult.impactItem1 = swapResult.impactItem1.multiply(new BigDecimal(dx).divide(new BigDecimal(dy), 36, RoundingMode.UP));
                    swapResult.amount = baseStableSwapPool.exchangeUnderlying(CALL_FOR_ROUTER, i, j, swapResult.amount, BigInteger.ZERO, timestamp, pathCacheContract);
                }
            }
        } catch (Exception e) {
            log.error("StableSwapPool fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), swapResult.amount, e);
            swapResult.amount = BigInteger.ZERO;
        }
        return swapResult;
    }

    private void psmSwap(String fromAddress, String toAddress, SwapResult swapResult, BaseContract baseContract) {
        try {
            PSM psm = (PSM) baseContract;
            PSMData data = psm.getPsmData();
            BigDecimal usddDeci = new BigDecimal(BigInteger.TEN.pow(18));
            BigDecimal usdxDeci = new BigDecimal(BigInteger.TEN.pow(data.getTokenDecimal()));
            if (fromAddress.equalsIgnoreCase(data.getUsdd())) {
                swapResult.impactItem0 = swapResult.impactItem0.multiply(BigDecimal.valueOf(1 - psm.calcUSDDToUSDXFee(data.getTout())));
                swapResult.impactItem1 = swapResult.impactItem1.multiply(usddDeci.divide(usdxDeci, 36, RoundingMode.UP));
                swapResult.amount = psm.calcUSDDToUSDX(swapResult.amount, data.getTokenDecimal(), data.getTout())[0];
                swapResult.fee = swapResult.fee + (1 - swapResult.fee) * psm.calcUSDDToUSDXFee(data.getTout());
            } else {
                swapResult.impactItem0 = swapResult.impactItem0.multiply(BigDecimal.valueOf(1 - psm.calcUSDXToUSDDFee(data.getTin())));
                swapResult.impactItem1 = swapResult.impactItem1.multiply(usdxDeci.divide(usddDeci, 36, RoundingMode.UP));
                swapResult.amount = psm.calcUSDXToUSDD(swapResult.amount, data.getTokenDecimal(), data.getTin())[1];
                swapResult.fee = swapResult.fee + (1 - swapResult.fee) * psm.calcUSDXToUSDDFee(data.getTin());
            }

        } catch (Exception e) {
            log.error("PSM fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), swapResult.amount, e);
            swapResult.amount = BigInteger.ZERO;
        }
    }

    private void curveSwap(String fromAddress, String toAddress, SwapResult swapResult, BaseContract baseContract,
                           PathCacheContract pathCacheContract) {
        try {
            CurveBasePool curve = (CurveBasePool) baseContract;
            CurveBasePoolData data = curve.getVarCurveBasePoolData();
            long timestamp = System.currentTimeMillis() / 1000;
            int[] indexes = data.getTokensIndex(fromAddress, toAddress);
            if (indexes[0] < 0 || indexes[1] < 0) {
                log.error("Curve fail, from:{}, to:{}, contract:{}, amount:{}, wrong input tokens:{} {}",
                        fromAddress, toAddress, baseContract.getAddress(), swapResult.amount, fromAddress, toAddress);
                swapResult.amount = BigInteger.ZERO;
            } else {
                swapResult.fee = swapResult.fee + (1 - swapResult.fee) * curve.calcFee(CALL_FOR_ROUTER, 0, indexes[1], pathCacheContract);
                swapResult.impactItem0 = swapResult.impactItem0.multiply(BigDecimal.valueOf(0.9996));
                int dxDecimals = (int) data.getCoinDecimals()[indexes[0]];
                BigInteger dx = BigInteger.TEN.pow(dxDecimals);
                BigInteger dy = curve.getDy(CALL_FOR_ROUTER, indexes[0], indexes[1], dx, timestamp, pathCacheContract);
                swapResult.impactItem1 = swapResult.impactItem1.multiply(new BigDecimal(dx).divide(new BigDecimal(dy), 36, RoundingMode.UP));
                swapResult.amount = curve.exchange(CALL_FOR_ROUTER, indexes[0], indexes[1], swapResult.amount, BigInteger.ZERO, timestamp, data);

            }
        } catch (Exception e) {
            log.error("Curve fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), swapResult.amount, e);
            swapResult.amount = BigInteger.ZERO;
        }
    }

    private void swpV2Pair(String fromAddress, String toAddress, SwapResult swapResult, BaseContract baseContract) {
        swapResult.fee = swapResult.fee + (1 - swapResult.fee) * 0.003;
        swapResult.impactItem0 = swapResult.impactItem0.multiply(BigDecimal.valueOf(0.997));
        try {
            SwapV2Pair swapV2 = (SwapV2Pair) baseContract;
            SwapV2PairData data = swapV2.getSwapV2PairData();
            if (data.getToken0().equals(fromAddress)) {
                swapResult.impactItem1 = swapResult.impactItem1.multiply(new BigDecimal(data.getReserve0()))
                        .divide(new BigDecimal(data.getReserve1()), 36, RoundingMode.UP);
            } else {
                swapResult.impactItem1 = swapResult.impactItem1.multiply(new BigDecimal(data.getReserve1()))
                        .divide(new BigDecimal(data.getReserve0()), 36, RoundingMode.UP);
            }
            swapResult.amount = swapV2.getAmountOut(fromAddress, toAddress, swapResult.amount, data);
        } catch (Exception e) {
            log.error("SwapV2 fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), swapResult.amount, e);
            swapResult.amount = BigInteger.ZERO;
        }
    }

    private void swapV1(String fromAddress, String toAddress, SwapResult swapResult, BaseContract baseContract) {
        swapResult.fee = swapResult.fee + (1 - swapResult.fee) * 0.003;
        swapResult.impactItem0 = swapResult.impactItem0.multiply(BigDecimal.valueOf(0.997));
        try {
            SwapV1 swapV1 = (SwapV1) baseContract;
            SwapV1Data data = swapV1.getSwapV1Data();
            if (fromAddress.equals(EMPTY_ADDRESS)) {
                swapResult.impactItem1 = swapResult.impactItem1.multiply(new BigDecimal(data.getTrxBalance()))
                        .divide(new BigDecimal(data.getTokenBalance()), 36, RoundingMode.UP);
                swapResult.amount = swapV1.trxToTokenInput(swapResult.amount, BigInteger.ZERO, data);
            } else {
                swapResult.impactItem1 = swapResult.impactItem1.multiply(new BigDecimal(data.getTokenBalance()))
                        .divide(new BigDecimal(data.getTrxBalance()), 36, RoundingMode.UP);
                swapResult.amount = swapV1.tokenToTrxInput(swapResult.amount, BigInteger.ZERO, data);
            }
        } catch (Exception e) {
            log.error("SwapV1 fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), swapResult.amount, e);
            swapResult.amount = BigInteger.ZERO;
        }
    }

    private boolean isPathContainToken(List<StepInfo> path, String contract, String token) {
        for (StepInfo stepInfo : path) {
            if (stepInfo.getContract().equals(contract) && stepInfo.getTokenAddress().equals(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean getPathsNoRecurrence(RoutNode routNode, String fromToken, String destToken, List<List<StepInfo>> paths, int maxHops,
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

            boolean isNodeAvailing = isTokenUsable(isUseBaseTokens, node.getAddress(), node.getSymbol(), fromToken, destToken)
                    && !isPathContainToken(path, node.getContract(), node.getAddress());
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
                    initV1((SwapV1) baseContract, true);
                    break;
                case SWAP_V2_PAIR:
                    initV2((SwapV2Pair) baseContract, true);
                    break;
                case CONTRACT_CURVE_2POOL:
                case CONTRACT_CURVE_3POOL:
                    initCurves((CurveBasePool) baseContract);
                    break;
                case CONTRACT_PSM:
                    initPSM((PSM) baseContract);
                    break;
                case STABLE_SWAP_POOL:
                    initStableSwapPool((BaseStableSwapPool) baseContract);
                    break;
            }
        }
        showRoads();
    }

    public void addRoutNodeMap(Map<String, BaseContract> contractMaps, List<String> addContracts) {
        log.info("Start addRoutNodeMap");
        for (String addr : addContracts) {
            BaseContract baseContract = contractMaps.get(addr);
            if (ObjectUtil.isNull(baseContract)) {
                continue;
            }
            switch (baseContract.getType()) {
                case SWAP_V1:
                    initV1((SwapV1) baseContract, false);
                    break;
                case SWAP_V2_PAIR:
                    initV2((SwapV2Pair) baseContract, false);
                    break;
                case CONTRACT_CURVE_2POOL:
                case CONTRACT_CURVE_3POOL:
                    initCurves((CurveBasePool) baseContract);
                    break;
                case CONTRACT_PSM:
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
        for (int j = 1; j < tokens.size(); j++) {
            updateRoutNodeMap(tokens.get(0), symbols.get(0), tokens.get(j), symbols.get(j), data.getPoolName(), contract);
            updateRoutNodeMap(tokens.get(j), symbols.get(j), tokens.get(0), symbols.get(0), data.getPoolName(), contract);
        }
    }

    private void initPSM(PSM psm) {
        PSMData data = psm.getPsmData();
        String token0 = data.getUsdd();
        String token1 = data.getToken();
        String token0Symbol = USDD;
        String token1Symbol = data.getTokenSymbol();
        String contract = data.getAddress();
        updateRoutNodeMap(token0, token0Symbol, token1, token1Symbol, data.getPoolName(), contract);
        updateRoutNodeMap(token1, token1Symbol, token0, token0Symbol, data.getPoolName(), contract);
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

    private void initV1(SwapV1 swapV1, boolean isInit) {
        SwapV1Data data = swapV1.getSwapV1Data();
        if (isInit) {
            if (data.getTokenBalance().compareTo(BigInteger.ZERO) <= 0
                    || data.getTrxBalance().compareTo(BigInteger.ZERO) <= 0) {
                return;
            }
        }
        String token0 = EMPTY_ADDRESS;
        String token1 = data.getTokenAddress();
        String token0Symbol = TRX_SYMBOL;
        String token1Symbol = data.getTokenSymbol();
        String contract = data.getAddress();
        String poolType = swapV1.getVersion();
        updateRoutNodeMap(token0, token0Symbol, token1, token1Symbol, poolType, contract);
        updateRoutNodeMap(token1, token1Symbol, token0, token0Symbol, poolType, contract);
    }

    private void initV2(SwapV2Pair swapV2Pair, boolean isInit) {
        SwapV2PairData data = swapV2Pair.getSwapV2PairData();
        if (isInit) {
            if (data.getReserve0().compareTo(BigInteger.ZERO) <= 0 ||
                    data.getReserve1().compareTo(BigInteger.ZERO) <= 0) {
                return;
            }
        }
        String token0 = data.getToken0();
        String token1 = data.getToken1();
        String token0Symbol = data.getToken0Symbol();
        String token1Symbol = data.getToken1Symbol();
        String contract = data.getAddress();
        String poolType = swapV2Pair.getVersion();
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
        if (destAddress.equalsIgnoreCase(tokenAddr)) {
            // get it
            return true;
        }
        if (fromAddress.equalsIgnoreCase(tokenAddr)) {
            // from address
            return false;
        }
        if (isUseBaseTokens) {
            return baseTokensMap.containsKey(tokenAddr) || baseTokenSymbolsMap.containsKey(tokenSymbol);
        }
        return true;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SwapResult {
        private BigInteger amount;
        private double fee;
        private BigDecimal impactItem0;
        private BigDecimal impactItem1;
    }

}
