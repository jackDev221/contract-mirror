import TronWeb from 'tronweb';
import config from './config.js';
const { mainnet, nile } = config;
import utils from 'web3-utils';
const { toBN } = utils;
import https from 'https';
import { dec } from '@terra-money/terra.js';

async function initTronWeb(config) {
    const privateKey = process.env.PRIVATE_KEY_NILE;
    if (tronWeb == null) {
      tronWeb = await new TronWeb({
        fullHost: config.rpcURL,
        headers: { 'TRON-PRO-API-KEY': '6afeff82-44da-4fe8-9303-cc568365794b' },
        privateKey: privateKey,
        timeout: 3000000,
        userFeePercentage: 100,
        feeLimit: 10000 * 1e6,
      });
      // vote = await tronWeb.contract().at(config.voteAddress);
      // minter = await tronWeb.contract().at(config.minterAddress);
    }
}
const amountIn = '1000000';
const E18 = toBN('1000000000000000000');
let tronWeb = null;
// async function getBackendData(){

let tokenlist = {
    'SUNOLD':'TWrZRHY9aKQZcyjpovdH6qeCEyYZrRQDZt', //18
    'BTC':'TG9XJ75ZWcUw69W8xViEJZQ365fRupGkFP',//8
    'WBTC':'TN3W4H6rK2ce4vX9YnFQHwKENnHjoxb3m9',//8
    'ETH':'TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93',//18
    'WBTT':'TLELxLrgD3dq6kqS4x6dEGJ7xNFMbzK95U', //8
    'WTRX':'TYsbWxNnyTgsZaTFaue9hqpxkU3Fkco94a', //6
    'JST':'TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3', //18
    'WIN':'TNDSHKGBmgRx9mDYA9CnxPx55nu672yQw2', // 6
    'DICE':'TLVu3Pzaep38SGvgCxUe1cUk2SNM6fyR4e',//6 --no
    'LIVE':'TLwpNV2gVkVk1g7ejZJ2hqzsE7RZvDuRSb',//6
    'USDT':'TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf',//6
    'USDJ':'TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL', //18
    'HT':'TGfVzt44kg6ZJ4fUqpHzJy3Jb37YMf8pMH', //18
    'BTCST':'TVwaw3yiAF84schPgjp81a6y6F4Lb4V8m4',//17  -无交易对
    'TUSD':'TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop',//18
    'sSUN':'TDqjTkZ63yHB19w2n7vPm2qAkLHwn9fKKk',//18
    'USDC':'TWMCMCoJPqCGw5RR7eChF2HoY3a9B8eYA3',//6
    'QUINCE':'TT8vc3zKCmGCUryYWLtFvu5PqAyYoZ3KMh',//6
    'USDD':'TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK'//18
}
const tokenArray = [
  'TWrZRHY9aKQZcyjpovdH6qeCEyYZrRQDZt', //18  'SUNOLD':
    'TG9XJ75ZWcUw69W8xViEJZQ365fRupGkFP',//8 'BTC':
    //'TN3W4H6rK2ce4vX9YnFQHwKENnHjoxb3m9',//8 'WBTC':
    'TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93',//18 'ETH':
    'TLELxLrgD3dq6kqS4x6dEGJ7xNFMbzK95U', //8 'WBTT':
    'TYsbWxNnyTgsZaTFaue9hqpxkU3Fkco94a', //6 'WTRX':
   'TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3', //18  'JST':
    'TNDSHKGBmgRx9mDYA9CnxPx55nu672yQw2', // 6 'WIN':
  // 'TLVu3Pzaep38SGvgCxUe1cUk2SNM6fyR4e',//6 --no  'DICE':
   'TLwpNV2gVkVk1g7ejZJ2hqzsE7RZvDuRSb',//6  'LIVE':
    'TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf',//6 'USDT':
    'TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL', //18 'USDJ':
    'TGfVzt44kg6ZJ4fUqpHzJy3Jb37YMf8pMH', //18 'HT':
   //'TVwaw3yiAF84schPgjp81a6y6F4Lb4V8m4',//17  -无交易对  'BTCST':
   'TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop',//18  'TUSD':
   'TDqjTkZ63yHB19w2n7vPm2qAkLHwn9fKKk',//18  'sSUN':
    'TWMCMCoJPqCGw5RR7eChF2HoY3a9B8eYA3',//6 'USDC':
   //'TT8vc3zKCmGCUryYWLtFvu5PqAyYoZ3KMh',//6  'QUINCE':
    'TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK'//18 'USDD':
];
const tokenName = [
    'SUNOLD',
    'BTC',
    //'WBTC',
    'ETH',
    'WBTT',
    'WTRX',
    'JST',
    'WIN',
    //'DICE',
    'LIVE',
    'USDT',
    'USDJ',
    'HT',
    //'BTCST',
    'TUSD',
    'sSUN',
    'USDC',
    //'QUINCE',
    'USDD'
]
let url = null;
//const url = "https://sunio-test-router.endjgfsv.link/routingInV2?fromToken="+"USDT"+"&fromTokenAddr="+tokenlist.USDT+"&toToken="+"HT"+"&toTokenAddr="+tokenlist.HT+"&inAmount=1000000&fromDecimal=6&toDecimal=18&useBaseTokens=true";
      
let path =null;
let named = null;
let pool = null;
let srcToken = null;
let targetToken = null;

var count = 0;
var correctcount = 0;
let GetJsonDataAsync=(url)=>{
    return new Promise((resolve, reject) => {
        https.get(url, (response) => {
            let data = '';
            //数据正在接收中...
            response.on('data', (chunk) => {
                data += chunk;
            });
            //数据接收完成
            response.on('end', () => {
                //console.log(JSON.parse(data));
                resolve(data);//数据接收完成
            });

        }).on("error", (error) => {
            console.log("Error: " + error.message);
            reject(new Error(error.message));
        });
    });
};

let getPoolVersion = (version = []) => {
    if (!version.length) {
      return version;
    }
    let result = [version[0]];
    version.forEach(item => {
      if (item !== result[result.length - 1]) {
        result.push(item);
      }
    });
    return result;
  };

let getPathList = (version = []) => { // versionlen
    if (version.length === 1) {
      return [1];
    }
    let result = [];
    let count = 0;
    version.forEach((item, index) => {
      if (index === 0) {
        count += 1;
      } else {
        if (item === version[index - 1]) {
          count += 1;
          if (index === version.length - 1) {
            result.push(count);
          }
        } else {
          result.push(count);
          count = 1;
          if (index === version.length - 1) {
            result.push(count);
          }
        }
      }
    });
    return result;
  }; 

const loadData = async (url) => {
    
    let promise = new Promise(async (resolve, reject) => {
        https.get(url, function (res, error) {
            
            let body = '';

            res.on('data', function (data) {
                body += data;
            });

            res.on('end', function () {
                //console.log(body);
                resolve(body);
            });

            res.on('error', function (e) {
                console.log(e.message);
                reject(e);
            });
        });

    })
    let res = await promise;
    return res;
}

function sleep(time){
    var timeStamp = new Date().getTime();
    var endTime = timeStamp + time;
    while(true){
    if (new Date().getTime() > endTime){
     return;
    } 
    }
   }
function getAmountIn(decimal){
    if (decimal == toBN(6)){
        return "100000"
    }else if (decimal == toBN(8)){
        return "10000000"
    }else{
        return "100000000000000000"
    }
}
function getDecimalNumber(decimal){
    return toBN(10).pow(decimal)
}
function getAmountOut(amount,decimal){
    var p = amount.indexOf(".");
    var amountWithoutPoint = amount.replace(".","");
    var l = amountWithoutPoint.length - p;
    return toBN(amountWithoutPoint).mul(toBN(10).pow(toBN(decimal))).div(toBN(10).pow(toBN(l)))
}
const now = Math.floor(Date.now() / 1000);
let res = null;
await initTronWeb(nile);
let router =null;
const max_num = toBN(2).pow(toBN(256)).sub(toBN(1));
router = await tronWeb.contract().at('TLCinYBDnF9n31s9ss1cJjiAF6N1akF11c'); 

for(var y = 3; y < tokenArray.length;y++){
    for( var x = 0; x < tokenArray.length;x++){
        if (x == y) {
            continue;
        }else {
            const fromToken = tokenName[y];
            const fromTokenAddr = tokenArray[y];
            srcToken = await tronWeb.contract().at(fromTokenAddr);
            const fromTokenDecimal = await srcToken.decimals().call();

            const toToken = tokenName[x];
            const toTokenAddr = tokenArray[x];
            targetToken = await tronWeb.contract().at(toTokenAddr);
            const toTokenDecimal = await targetToken.decimals().call();
            await srcToken.approve('TLCinYBDnF9n31s9ss1cJjiAF6N1akF11c',max_num.toString()).send();
            const url = "https://sunio-test-router.endjgfsv.link/routingInV2?fromToken=" + fromToken +"&fromTokenAddr=" + fromTokenAddr + "&toToken=" + toToken + "&toTokenAddr=" +toTokenAddr+ "&inAmount="+ getAmountIn(fromTokenDecimal)+"&fromDecimal=" + fromTokenDecimal.toString() + "&toDecimal=" + toTokenDecimal.toString() + "&useBaseTokens=true";

            console.log(url);

            for(var i = 0;i<3 ;i++ ){
            
                res = await loadData(url);

                var resp = JSON.parse(res)
                if (res.error != null){
                    break;
                }
                console.log("-----------------------------------------")
                path = resp.data[i].roadForAddr;
                named = resp.data[i].roadForName;
                pool = resp.data[i].pool;
                if (path == null){
                    break;
                }
                //console.log(res)
                //console.log(resp)
                console.log("path:",named.toString());
                const amountOut = getAmountOut(resp.data[i].amount.toString(),toTokenDecimal);
                console.log("amountOut: ",amountOut.toString());
                            
             
                // console.log(path)
                // console.log(pool)
                console.log(getPoolVersion(pool))
                console.log(getPathList(pool))
                const pathlis = getPathList(pool);
                pathlis[0] +=1;
                console.log(pathlis);
                const txhash = await router.swapExactTokensForTokens(
                    getAmountIn(fromTokenDecimal),
                    amountOut.mul(toBN(995).div(toBN(1000))).toString(),
                    path,
                    getPoolVersion(pool),
                    pathlis,
                    'TF5MekHgFz6neU7zTpX4h2tha5miPDUj3z',
                    Math.floor(Date.now() / 1000)+600
                ).send({feeLimit: 10000 * 1e6,shouldPollResponse: true});
                sleep(3000)
                //console.log(txhash)
                // console.log(getPoolVersion(pool));
                // console.log(getPathList(pool));
                console.log("RealAmount:", txhash[0][txhash[0].length-1].toString())
                const realAmount = txhash[0][txhash[0].length-1];
                if( amountOut.toString() == realAmount.toString()){
                    console.log(fromToken + " to " + toToken + " path " + i + " 's reasult is correct!")
                    correctcount +=1;
                }
                count +=1;
                console.log("count :",count);
                console.log("correctcount: ",correctcount);
                console.log("correct Rate : ",correctcount*100/count);
            }
        }
    }
}



