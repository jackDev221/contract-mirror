package org.tron.sunio.contract_mirror.mirror.price;

import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;

@Slf4j
@Component
public class TokenPrice {

    public String price(String url, String symbol) throws Exception {
        JSONObject obj = HttpGetter.httpGet(url + URLEncoder.encode(symbol, "utf-8") + "&convert=USD");
        JSONObject status = obj.getJSONObject("status");
        String price = "";
        if (status == null) {
            log.error("priceTaskConnect:" + symbol);
            return null;
        }
        String code = status.get("error_code").toString();
        if (code.equals("0")) {
            JSONObject data = obj.getJSONObject("data");
            if (data == null) {
                price = "0";
            } else {
                JSONObject tokenPriceInfo = data.getJSONObject(symbol);
                if (tokenPriceInfo == null) {
                    price = "0";
                } else {
                    JSONObject quote = tokenPriceInfo.getJSONObject("quote");
                    JSONObject USD = quote.getJSONObject("USD");
                    if (USD == null) {
                        price = "0";
                    } else {
                        price = USD.get("price").toString();
                    }
                }
            }
            return price;
        } else {
            log.error("code:" + code);
            log.error("priceTaskCode:" + symbol);
            return null;
        }
    }
}
