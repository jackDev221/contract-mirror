import fetch from 'node-fetch';
// import { initTronWeb } from './tron.mjs';
import config from './config.js';
const { mainnet, nile } = config;
// import utils from 'web3-utils';
// const { toBN, toWei } = utils;

const cfg = mainnet;
// const tronWeb = await initTronWeb(cfg);

let tokenList = new Map();
tokenList.set('TRX', '9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb');
tokenList.set('USDC', 'TWMCMCoJPqCGw5RR7eChF2HoY3a9B8eYA3');
tokenList.set('SUN', 'TDqjTkZ63yHB19w2n7vPm2qAkLHwn9fKKk');
tokenList.set('DICE', 'TLVu3Pzaep38SGvgCxUe1cUk2SNM6fyR4e');
tokenList.set('LIVE', 'TLwpNV2gVkVk1g7ejZJ2hqzsE7RZvDuRSb');
tokenList.set('QUINCE', 'TT8vc3zKCmGCUryYWLtFvu5PqAyYoZ3KMh');
tokenList.set('SUNOLD', 'TWrZRHY9aKQZcyjpovdH6qeCEyYZrRQDZt');
tokenList.set('BTC', 'TG9XJ75ZWcUw69W8xViEJZQ365fRupGkFP');
tokenList.set('WBTC', 'TN3W4H6rK2ce4vX9YnFQHwKENnHjoxb3m9');
tokenList.set('ETH', 'TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93');
tokenList.set('WBTT', 'TLELxLrgD3dq6kqS4x6dEGJ7xNFMbzK95U');
tokenList.set('WTRX', 'TYsbWxNnyTgsZaTFaue9hqpxkU3Fkco94a');
tokenList.set('JST', 'TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3');
tokenList.set('WIN', 'TNDSHKGBmgRx9mDYA9CnxPx55nu672yQw2');
tokenList.set('USDT', 'TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf');
tokenList.set('USDJ', 'TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL');
tokenList.set('TUSD', 'TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop');
tokenList.set('HT', 'TGfVzt44kg6ZJ4fUqpHzJy3Jb37YMf8pMH');
tokenList.set('USDDPSM', 'TWvPn9LWmNd1DMtt52yKHhNJazi7sDfcUq');
tokenList.set('USDD', 'TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK');

let paths = new Set();
let total = 0;
let success = 0;

async function diffPair(from, to) {
  let url = `http://${cfg.routerServer}/swap/router?fromToken=${from}&toToken=${to}&amountIn=1000000`;
  console.log(url);
  try {
    total = total + 1;
    let response = await fetch(url);
    response = await response.json();
    if (response[code] != 0) {
      console.log(response);
      return 0;
    }
    console.log(response);
    success = success + 1;
    return 0;
  } catch (e) {
    console.log(`error ${e}, ${url}`);
    return 0;
  }
}

for (let [name0, address0] of tokenList) {
  for (let [name1, address1] of tokenList) {
    if (address0 == address1) {
      continue;
    }
    let count = 0;
    do {
      count = await diffPair(address0, address1);
    } while (count > 0);
  }
}
