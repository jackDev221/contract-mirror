# parse_logs

解析旧版本路由服务日志，组装访问请求，同时访问新旧服务，对比统计结果。

## build
```
cargo build --release
```
可执行文件在 `./targe/release/prase_logs`

## config
修改配置文件`config.json`

```

{
  "oldUrl": "", // 旧版服务Url
  "newUrl": "", // 新版服务Url
  "logFilePath": "", // 旧版本日志路径
  "compareResPath": "", // 输出对比路径
  "compareResDetailPath": "", //输出对比diff路径 
  "useBaseTokens": "true", //是否使用baseToken
  "maxCount": 100, //最大请求次数。
  "restoreInput": false, // 是否重新存储旧版本服务器日志，搭配请求去重，加快下次测试测试
  "restoreInputPath": "", // 重新存储旧版本服务器日志目录 
  "rmDuplicate":true, // 请求是否去重，基于交换币对唯一性
  "tokenPairOfLargePaths": "" //  新版放回路径数大于50的币对存放目录
}

```

##执行

```
./parse_logs ./config.json

```


