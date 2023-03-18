import fetch from 'node-fetch';
// import { initTronWeb } from './tron.mjs';
import config from './config.js';
const { mainnet, nile } = config;
import utils from 'web3-utils';
const { toBN, toWei } = utils;

const cfg = mainnet;
// const tronWeb = await initTronWeb(cfg);

let tokenList = new Map();
tokenList.set('TRX', {address:'T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb', decimals: 6});
tokenList.set('SUN', {address:'TD3et9gS2pYz46ZC2mkCfYcKQGNwrnBLef', decimals: 18});
tokenList.set('SUNOLD', {address: 'TKkeiboTkxXKJpbmVFbv4a8ov5rAfRDMf9', decimals: 18});
tokenList.set('BTC', {address: 'TN3W4H6rK2ce4vX9YnFQHwKENnHjoxb3m9', decimals: 8});
tokenList.set('WBTC', {address:'TXpw8XeWYeTUd4quDskoUqeQPowRh4jY65', decimals: 8});
tokenList.set('ETH', {address:'THb4CqiFdwNHsWsQCs4JhzwjMWys4aqCbF', decimals:18});
tokenList.set('WETH', {address:'TXWkP3jLBqRGojUih1ShzNyDaN5Csnebok',decimals:18});
tokenList.set('WBTT', {address:'TKfjV9RNKJJCqPvBtK8L7Knykh7DNWvnYt', decimals:6});
tokenList.set('WTRX', {address:'TNUC9Qb1rRpS5CbWLmNMxXBjyFoydXjWFR',decimals:6});
tokenList.set('JST', {address:'TCFLL5dx5ZJdKnWuesXxi1VPwjLVmWZZy9', decimals:18});
tokenList.set('WIN', {address:'TLa2f6VPqDgRE67v1736s7bJ8Ray5wYjU7',decimals:6});
tokenList.set('DICE', {address:'TKttnV3FSY1iEoAwB4N52WK2DxdV94KpSd',decimals:6});
tokenList.set('LIVE', {address:'TVgAYofpQku5G4zenXnvxhbZxpzzrk8WVK',decimals:6});
tokenList.set('LTC', {address:'TR3DLthpnDdCGabhVDbD3VMsiJoCXY3bZd',decimals:8});
tokenList.set('HT', {address:'TDyvndWuvX5xTBwHPYJi7J3Yq8pq8yh62h',decimals:18});
tokenList.set('USDT', {address:'TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t',decimals:6});
tokenList.set('USDJ', {address:'TMwFHYXLJaRUPeW6421aqXL4ZEzPRFGkGT',decimals:18});
tokenList.set('TUSD', {address:'TUpMhErZL2fhh4sVNULAbNKLokS4GjC1F4',decimals:18});
tokenList.set('USDC', {address:'TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8',decimals:6});
tokenList.set('USDD', {address:'TPYmHEhy5n8TCEfYGqW2rPxsghSfzghPDn',decimals:18});

let paths = new Set();
let total = 0;
let success = 0;
let maxTime = 0;
let minTime = 999999999999;
let totalTime = 0;

async function diffPair(from, to) {
  let amountIn = toBN(2).mul(toBN(10).pow(toBN(from.decimals))).toString();
  let url = `http://${cfg.routerServer}/swap/router?fromToken=${from.address}&toToken=${to.address}&amountIn=${amountIn}`;
  console.log(url);
  try {
    total = total + 1;
    let t0 = Date.now();
    let response = await fetch(url);
    let t1 = Date.now();
    let t = t1 - t0;
    maxTime = Math.max(maxTime, t);
    minTime = Math.min(minTime, t);
    totalTime += t;
    response = await response.json();
    console.log(response);
    if (response.code != 0) {
      console.log(response);
      return 0;
    }
    success = success + 1;
    return 0;
  } catch (e) {
    console.log(`error ${e}, ${url}`);
    return 0;
  }
}

for (let [name0, token0] of tokenList) {
  for (let [name1, token1] of tokenList) {
    if (name0 == name1) {
      continue;
    }
    let count = 0;
    do {
      count = await diffPair(token0, token1);
    } while (count > 0);
  }
}
console.log(`total=${total} success=${success} maxTime=${maxTime} minTime=${minTime} avgTime=`, totalTime/total);
