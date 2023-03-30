package org.tron.sunio.contract_mirror.mirror.price;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.base.Joiner;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpGetter {
    private static Logger logger = LoggerFactory.getLogger(HttpGetter.class);

    private static final int TIME_OUT = 10 * 1000;
    private static PoolingHttpClientConnectionManager cm = null;

    static {
        LayeredConnectionSocketFactory sslsf = null;
        try {
            sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault());
        } catch (NoSuchAlgorithmException e) {
            logger.error("SSL Connection failed...");
        }
        Registry<ConnectionSocketFactory> sRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslsf)
                .register("http", new PlainConnectionSocketFactory())
                .build();
        cm = new PoolingHttpClientConnectionManager(sRegistry);
        // max clients
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(20);
    }

    private static CloseableHttpClient getHttpClient() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm).build();
        return httpClient;
    }

    public static JSONArray httpGetArray(String url) {
        JSONArray jsonResult = null;
        CloseableHttpClient httpClient = getHttpClient();

        CloseableHttpResponse response = null;
        try {

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(TIME_OUT).setConnectionRequestTimeout(TIME_OUT)
                    .setSocketTimeout(TIME_OUT).build();
            HttpGet request = new HttpGet(url);
            request.setConfig(requestConfig);
            response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String strResult = EntityUtils.toString(response.getEntity());
                jsonResult = JSONUtil.parseArray(strResult);
                url = URLDecoder.decode(url, "UTF-8");
            } else {
                logger.error("get failed:" + url);
            }
        } catch (IOException e) {
            logger.error("get failed:" + url, e);
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                } catch (IOException e) {
                    logger.error("close response failed", e);
                }
            }
        }
        return jsonResult;
    }

    public static JSONObject httpGet(String url) {
        JSONObject jsonResult = null;
        CloseableHttpClient httpClient = getHttpClient();

        CloseableHttpResponse response = null;
        try {

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(TIME_OUT).setConnectionRequestTimeout(TIME_OUT)
                    .setSocketTimeout(TIME_OUT).build();
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0");
            request.setConfig(requestConfig);
            response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String strResult = EntityUtils.toString(response.getEntity());
                jsonResult = JSONUtil.parseObj(strResult);
                url = URLDecoder.decode(url, "UTF-8");
            } else {
                logger.error("get failed:" + url);
            }
        } catch (IOException e) {
            logger.error("get failed:" + url, e);
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                } catch (IOException e) {
                    logger.error("close response failed", e);
                }
            }
        }
        return jsonResult;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject httpPost(String url, List nameValuePairList, String payLoad,
                                      Map<String, String> headers) {
        JSONObject jsonResult = null;
        CloseableHttpClient httpClient = getHttpClient();

        CloseableHttpResponse response = null;
        try {

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(TIME_OUT).setConnectionRequestTimeout(TIME_OUT)
                    .setSocketTimeout(TIME_OUT).build();
            HttpPost httpPost = new HttpPost(url);
            if (payLoad != null) {
                StringEntity stringEntity = new StringEntity(payLoad, "utf-8");
                httpPost.setEntity(stringEntity);
            } else {
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairList));
            }
            httpPost.setConfig(requestConfig);
            for (String s : headers.keySet()) {
                httpPost.setHeader(s, headers.get(s));
            }

            response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String strResult = EntityUtils.toString(response.getEntity());
                jsonResult = JSONUtil.parseObj(strResult);
                url = URLDecoder.decode(url, "UTF-8");
            } else {
                logger.error("get failed:" + url);
            }
        } catch (IOException e) {
            logger.error("get failed:" + url, e);
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                } catch (IOException e) {
                    logger.error("close response failed", e);
                }
            }
        }
        return jsonResult;
    }

    public static String getNameValuePairStr(List<BasicNameValuePair> nameValuePairs) {
        List<String> rets = new ArrayList<>();
        for (BasicNameValuePair basicNameValuePair : nameValuePairs) {
            rets.add(basicNameValuePair.getName() + "=" + basicNameValuePair.getValue());
        }
        return Joiner.on('&').join(rets);
    }

}
