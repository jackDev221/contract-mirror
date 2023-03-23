import fetch from 'node-fetch';
import { initTronWeb } from './tron.mjs';
import config from './config.js';
const { mainnet, nile } = config;
import utils from 'web3-utils';
const { toBN, toWei } = utils;

const cfg = nile;
const tronWeb = await initTronWeb(cfg);
const router = await tronWeb.contract().at(cfg.smartRouter);
const receiver = 'TF5MekHgFz6neU7zTpX4h2tha5miPDUj3z';

let tokenList = new Map();
tokenList.set('TRX', {
  address: 'T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb',
  decimals: 6,
});
tokenList.set('SUN', {
  address: 'TDqjTkZ63yHB19w2n7vPm2qAkLHwn9fKKk',
  decimals: 18,
});
tokenList.set('SUNOLD', {
  address: 'TWrZRHY9aKQZcyjpovdH6qeCEyYZrRQDZt',
  decimals: 18,
});
tokenList.set('BTC', {
  address: 'TG9XJ75ZWcUw69W8xViEJZQ365fRupGkFP',
  decimals: 8,
});
tokenList.set('ETH', {
  address: 'TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93',
  decimals: 18,
});
tokenList.set('WTRX', {
  address: 'TYsbWxNnyTgsZaTFaue9hqpxkU3Fkco94a',
  decimals: 6,
});
tokenList.set('JST', {
  address: 'TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3',
  decimals: 18,
});
tokenList.set('WIN', {
  address: 'TNDSHKGBmgRx9mDYA9CnxPx55nu672yQw2',
  decimals: 6,
});
tokenList.set('DICE', {
  address: 'TLVu3Pzaep38SGvgCxUe1cUk2SNM6fyR4e',
  decimals: 6,
});
tokenList.set('LIVE', {
  address: 'TLwpNV2gVkVk1g7ejZJ2hqzsE7RZvDuRSb',
  decimals: 6,
});
tokenList.set('HT', {
  address: 'TGfVzt44kg6ZJ4fUqpHzJy3Jb37YMf8pMH',
  decimals: 18,
});
tokenList.set('USDT', {
  address: 'TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf',
  decimals: 6,
});
tokenList.set('USDJ', {
  address: 'TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL',
  decimals: 18,
});
tokenList.set('TUSD', {
  address: 'TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop',
  decimals: 18,
});
tokenList.set('USDC', {
  address: 'TWMCMCoJPqCGw5RR7eChF2HoY3a9B8eYA3',
  decimals: 6,
});
tokenList.set('USDD', {
  address: 'TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK',
  decimals: 18,
});

let paths = new Map();
let total = 0;
let success = 0;
let fail = 0;
let diff = 0;
let t = 0;
let maxTime = 0;
let minTime = 999999999999;
let totalTime = 0;

function record() {
  total += 1;
  totalTime += t;
  maxTime = Math.max(maxTime, t);
  minTime = Math.min(minTime, t);
  console.log(
    `total=${total} success=${success} fail=${fail} diff=${diff} maxTime=${maxTime} minTime=${minTime} avgTime=`,
    totalTime / total,
  );
}

function pathKey(path) {
  let key = path.roadForName.join('');
  key += path.pool.join('');
  return key;
}

function decimalToBN(value, decimals) {
  let negative = value.substring(0, 1) === '-';
  if (negative) {
    value = value.substring(1);
  }
  let num;
  let exp = toBN(decimals);
  let factor;
  let comps = value.split('E');
  if (comps.length > 2) {
    throw `invalid value ${value} ${decimals}`;
  } else if (comps.length == 2) {
    exp = exp.add(toBN(comps[1]));
  }
  factor = toBN(10).pow(exp.abs());
  comps = comps[0].split('.');
  if (comps.length > 2 || (comps.length == 2 && comps[1].length > decimals)) {
    throw `invalid value ${value} ${decimals}`;
  } else if (comps.length == 1) {
    let n = toBN(comps[0]);
    if (exp.isNeg()) {
      let r = n.divmod(factor);
      if (!r.mod.isZero()) {
        throw `invalid value ${value} ${decimals}`;
      }
      num = r.div;
    } else {
      num = n.mul(factor);
    }
  } else {
    let n = toBN(comps[0]);
    let fraction = toBN(comps[1]);
    if (exp.isNeg()) {
      let r = n.divmod(factor);
      if (!r.mod.isZero() || !fraction.isZero()) {
        throw `invalid value ${value} ${decimals}`;
      }
      num = r.div;
    } else {
      let r = toBN(fraction)
        .mul(factor)
        .divmod(toBN(10).pow(toBN(fraction.length)));
      if (!r.mod.isZero()) {
        throw `invalid value ${value} ${decimals}`;
      }
      num = r.div.add(n.mul(factor));
    }
  }
  return negative ? num.neg() : num;
}

async function diffPair(fromName, from, toName, to) {
  // 0.2
  let amountIn = toBN(2)
    .mul(toBN(10).pow(toBN(from.decimals - 1)))
    .toString();
  let url = `http://${cfg.routerServer}/swap/routingInV2?fromToken=${fromName}&fromTokenAddr=${from.address}&toToken=${toName}&toTokenAddr=${to.address}&inAmount=${amountIn}&fromDecimal=${from.decimals}&toDecimal=${to.decimals}`;
  console.log(url);
  let t0 = Date.now();
  let response = await fetch(url);
  let t1 = Date.now();
  t = t1 - t0;
  response = await response.json();
  if (response.code != 0 || response.data.length == 0) {
    console.log(response);
    return 0;
  }
  let path = null;
  let key = null;
  let error = undefined;
  for (let i = 0; i < response.data.length; i++) {
    path = response.data[i];
    key = pathKey(path);
    error = paths.get(key);
    if (error === 0) {
      path = null;
      continue;
    }
    break;
  }
  if (path === null) {
    return 0;
  }
  let versions = [];
  let versionLens = [];
  for (let i = 0; i < path.pool.length; i++) {
    let pool = path.pool[i];
    if (versions.length == 0 || versions[versions.length - 1] != pool) {
      versions.push(pool);
      versionLens.push(i == 0 ? 2 : 1);
    } else {
      versionLens[versionLens.length - 1] += 1;
    }
  }
  let amountsOut;
  let now = Math.floor(Date.now() / 1000);
  try {
    if (from === tokenList.get('TRX')) {
      amountsOut = await router
        .swapExactETHForTokens(
          amountIn.toString(),
          1,
          JSON.parse(JSON.stringify(path.roadForAddr)),
          versions,
          versionLens,
          receiver,
          now + 6000,
        )
        .send({
          feeLimit: 1000 * 1e6,
          callValue: amountIn.toString(),
          shouldPollResponse: true,
        });
    } else {
      amountsOut = await router
        .swapExactTokensForTokens(
          amountIn.toString(),
          1,
          JSON.parse(JSON.stringify(path.roadForAddr)),
          versions,
          versionLens,
          receiver,
          now + 6000,
        )
        .send({
          feeLimit: 1000 * 1e6,
          shouldPollResponse: true,
        });
    }
  } catch (e) {
    console.log(JSON.stringify(path));
    console.log(e);
    error = error === undefined ? 1 : error + 1;
    paths.set(key, error);
    if (error === 3) {
      fail += 1;
      record();
      return 0;
    }
    return 1; // retry
  }
  amountsOut = amountsOut.amountsOut;
  let amountOut = null;
  if (amountsOut.length > 0) {
    amountOut = amountsOut[amountsOut.length - 1].toString();
  }
  let expectOut = decimalToBN(path.amount, to.decimals).toString();
  if (amountOut === null || amountOut != expectOut) {
    error = error === undefined ? 1 : error + 1;
    paths.set(key, error);
    console.log(JSON.stringify(path));
    console.log(`diff ${error} ${amountsOut} != ${expectOut}`);
    if (error === 3) {
      success += 1;
      diff += 1;
      record();
      return 0;
    }
    return 1; // retry
  }
  paths.set(key, 0);
  success += 1;
  record();
  return 1;
}

for (let [name0, token0] of tokenList) {
  for (let [name1, token1] of tokenList) {
    if (name0 === name1) {
      continue;
    }
    let count = 0;
    do {
      count = await diffPair(name0, token0, name1, token1);
    } while (count > 0);
  }
}
