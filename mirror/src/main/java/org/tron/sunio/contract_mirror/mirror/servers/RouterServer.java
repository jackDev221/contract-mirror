package org.tron.sunio.contract_mirror.mirror.servers;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.sunio.contract_mirror.mirror.config.RouterConfig;
import org.tron.sunio.contract_mirror.mirror.contracts.BaseContract;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.CurveBasePool;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.PSM;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV1;
import org.tron.sunio.contract_mirror.mirror.contracts.impl.SwapV2Pair;
import org.tron.sunio.contract_mirror.mirror.dao.CurveBasePoolData;
import org.tron.sunio.contract_mirror.mirror.dao.PSMData;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV1Data;
import org.tron.sunio.contract_mirror.mirror.dao.SwapV2PairData;
import org.tron.sunio.contract_mirror.mirror.enums.ContractType;
import org.tron.sunio.contract_mirror.mirror.router.PathInfo;
import org.tron.sunio.contract_mirror.mirror.router.RoutItem;
import org.tron.sunio.contract_mirror.mirror.router.RoutNode;
import org.tron.sunio.contract_mirror.mirror.router.RouterInput;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.tron.sunio.contract_mirror.mirror.consts.ContractMirrorConst.EMPTY_ADDRESS;

@Slf4j
@Service
public class RouterServer {
    private static final String SPLIT = "_";
    private static final String TRX = "trx";
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
    private ConcurrentMap<String, List<List<PathInfo>>> cachedPaths = new ConcurrentHashMap<>();


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
        List<List<PathInfo>> paths;

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
        for (List<PathInfo> item : paths) {
            RoutItem routItem = getRoutItemByPaths(routerInput, item, contractMaps);
            res.add(routItem);
        }
        return res;
    }

    private RoutItem getRoutItemByPaths(RouterInput routerInput, List<PathInfo> pathInfoList, Map<String, BaseContract> contractMaps) {
        RoutItem routItem = new RoutItem();
        List<String> roadForName = routItem.getRoadForName();
        List<String> roadForAddr = routItem.getRoadForAddr();
        List<String> pool = routItem.getPool();
        BigInteger amount = BigInteger.ZERO;
        String fromToken = routerInput.getFromToken();
        String toToken = "";
        for (int i = 0; i < pathInfoList.size(); i++) {
            PathInfo info = pathInfoList.get(i);
            roadForAddr.add(info.getTokenAddress());
            roadForName.add(info.getTokenName());
            pool.add(info.getPoolType());
            toToken = info.getTokenAddress();
            amount = swapToken(fromToken, toToken, amount, contractMaps.get(info.getContract()));
            if (amount.compareTo(BigInteger.ZERO) == 0) {
                log.error("Cal:from {}, to: {},  contract:{}, amount is zero", fromToken, toToken, info.getContract());
                return null;
            }
        }
        routItem.setAmount(amount);
        return routItem;
    }


    private BigInteger swapToken(String fromAddress, String toAddress, BigInteger amount, BaseContract baseContract) {
        BigInteger res = BigInteger.ZERO;
        if (ObjectUtil.isNull(baseContract)) {
            return res;
        }
        switch (baseContract.getType()) {
            case SWAP_V1:
                res = swapV1(fromAddress, toAddress, amount, baseContract);
            default:
                break;
        }
        return res;
    }

    private BigInteger swapV1(String fromAddress, String toAddress, BigInteger amount, BaseContract baseContract) {
        BigInteger res;
        try {
            SwapV1 swapV1 = (SwapV1) baseContract;
            if (fromAddress.equals(EMPTY_ADDRESS)) {
                res = swapV1.trxToTokenInput(amount, BigInteger.ZERO, swapV1.getSwapV1Data());
            } else {
                res = swapV1.tokenToTrxInput(amount, BigInteger.ZERO, swapV1.getSwapV1Data());
            }
        } catch (Exception e) {
            log.error("SwapV1 fail, from:{}, to:{}, contract:{}, amount:{}, err:{}",
                    fromAddress, toAddress, baseContract.getAddress(), amount, e);
            res = BigInteger.ZERO;
        }
        return res;
    }


    private boolean getPaths(RoutNode routNode, String destToken, List<PathInfo> path, List<List<PathInfo>> paths, int maxHops) {
        if (path.size() > maxHops) {
            return false;
        }
        if (routNode.getAddress().equalsIgnoreCase(destToken)) {
            paths.add(path);
            return true;
        }
        for (RoutNode subNode : routNode.getSubNodes()) {
            PathInfo pathInfo = PathInfo.builder()
                    .contract(subNode.getContract())
                    .tokenAddress(subNode.getAddress())
                    .tokenName(subNode.getName())
                    .poolType(subNode.getPoolType())
                    .build();
            List<PathInfo> pathCopy = List.copyOf(path);
            pathCopy.add(pathInfo);
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
            }
        }
    }

    private void initPSM(PSM psm) {
        PSMData data = psm.getPsmData();
        String[] tokenInfo = getPSMTokenInfo(psm.getContractType());
        String token0 = data.getUsdd();
        String token1 = tokenInfo[1];
        String token0Name = USDD;
        String token1Name = tokenInfo[0];
        String contract = data.getAddress();
        updateRoutNodeMap(token0, token0Name, token1, token1Name, POOL_TYPE_PSM, contract);
        updateRoutNodeMap(token1, token1Name, token0, token0Name, POOL_TYPE_PSM, contract);
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
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                String token0 = data.getCoins()[i];
                String token1 = data.getCoins()[j];
                String token0Name = data.getCoinNames()[i];
                String token1Name = data.getCoinNames()[j];
                String contract = data.getAddress();
                updateRoutNodeMap(token0, token0Name, token1, token1Name, poolType, contract);
                updateRoutNodeMap(token1, token1Name, token0, token0Name, poolType, contract);
            }
        }
    }

    private void initV1(SwapV1 swapV1) {
        SwapV1Data data = swapV1.getSwapV1Data();
        String token0 = EMPTY_ADDRESS;
        String token1 = data.getTokenAddress();
        String token0Name = TRX;
        String token1Name = data.getTokenSymbol();
        String contract = data.getAddress();
        String poolType = POOL_TYPE_V1;
        updateRoutNodeMap(token0, token0Name, token1, token1Name, poolType, contract);
        updateRoutNodeMap(token1, token1Name, token0, token0Name, poolType, contract);
    }

    private void initV2(SwapV2Pair swapV2Pair) {
        SwapV2PairData data = swapV2Pair.getSwapV2PairData();
        String token0 = data.getToken0();
        String token1 = data.getToken1();
        String token0Name = data.getToken0Symbol();
        String token1Name = data.getToken1Symbol();
        String contract = data.getAddress();
        String poolType = POOL_TYPE_V2;
        updateRoutNodeMap(token0, token0Name, token1, token1Name, poolType, contract);
        updateRoutNodeMap(token1, token1Name, token0, token0Name, poolType, contract);
    }

    private void updateRoutNodeMap(String token0, String token0Name, String token1, String token1Name, String poolType,
                                   String contract) {
        RoutNode routNode = routNodeMap.getOrDefault(token0, new RoutNode(token0, token0Name, "", ""));
        RoutNode subNode = new RoutNode(token1, token1Name, contract, poolType);
        routNode.getSubNodes().add(subNode);
        routNodeMap.put(token0, routNode);
    }

    private String genListPathsKey(String token0, String token1) {
        return String.format("%s%s%s", token0, SPLIT, token1);
    }

}
