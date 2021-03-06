package com.lorabit.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lorabit
 * @since 16-3-1
 */
public class CustomHttpHelper {

  private static final Logger logger = LoggerFactory.getLogger(CustomHttpHelper.class);

  private static CloseableHttpClient httpClient;

  private static final int DEFAULT_TIMEOUT = 3000;

  public static final ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    RequestConfig defaultRequestConfig = RequestConfig.custom()
        .setSocketTimeout(DEFAULT_TIMEOUT)
        .setConnectTimeout(DEFAULT_TIMEOUT)
        .build();
    httpClient = HttpClients.custom()
        .setDefaultRequestConfig(defaultRequestConfig)
        .build();
    Runtime.getRuntime().addShutdownHook(
        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              if (httpClient != null) {
                httpClient.close();
              }
            } catch (IOException e) {
            }
          }
        }
        ));
  }

  public static <T> T execute(HttpHost httpHost, HttpUriRequest httpUriRequest,
      ResponseHandler<T> handler) throws IOException {
    return httpClient.execute(httpHost, httpUriRequest, handler, new BasicHttpContext());
  }

  public static <T> T execute(HttpUriRequest httpUriRequest,
      ResponseHandler<T> handler) throws IOException {
    return httpClient.execute(httpUriRequest, handler, new BasicHttpContext());
  }

  public static <T> T execute(HttpPost post, Map<String, String> params,
      ResponseHandler<T> handler) throws IOException {
    if (params != null && !params.isEmpty()) {
      List<NameValuePair> valuePairs = new ArrayList<>();
      for (Map.Entry<String, String> param : params.entrySet()) {
        valuePairs.add(new BasicNameValuePair(param.getKey(), param.getValue()));
      }
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(valuePairs, Consts.UTF_8);
      post.setEntity(entity);
    }
    return httpClient.execute(post, handler, new BasicHttpContext());
  }


  public static <T> T execute(HttpPost httpUriRequest, Map<String, File> files,
      Map<String, String> strParams, ResponseHandler<T> handler) throws IOException {
    MultipartEntityBuilder reqBuilder = MultipartEntityBuilder.create();
    if (files != null && !files.isEmpty()) {
      for (Map.Entry<String, File> file : files.entrySet()) {
        reqBuilder.addPart(file.getKey(), new FileBody(file.getValue()));
      }
    }
    if (strParams != null && !strParams.isEmpty()) {
      for (Map.Entry<String, String> str : strParams.entrySet()) {
        reqBuilder.addPart(str.getKey(), new StringBody(str.getValue(), ContentType.TEXT_PLAIN));
      }
    }

    HttpEntity reqEntity = reqBuilder.build();
    httpUriRequest.setEntity(reqEntity);
    return httpClient.execute(httpUriRequest, handler, new BasicHttpContext());
  }

  public static String postImg(Map<String, File> files) throws IOException {

    HttpPost httppost = new HttpPost("http://www.duitang.com/napi/upload/photo/");
    MultipartEntityBuilder reqBuilder = MultipartEntityBuilder.create();
    if (files != null && !files.isEmpty()) {
      for (Map.Entry<String, File> file : files.entrySet()) {
        reqBuilder.addPart(file.getKey(), new FileBody(file.getValue()));
      }
    }
    HttpEntity reqEntity = reqBuilder.build();
    httppost.setEntity(reqEntity);
    httppost.setHeader("Cookie", "__utma=74400135.356992072.1444356525.1457512233.1457791779.276; __utmz=74400135.1457512233.275.98.utmcsr=operate.duitang.com|utmccn=(referral)|utmcmd=referral|utmcct=/buy/; csrftoken=e81cca5ac9f2a97aa5806e69e3ca2c53; sgm=usedtags%3D%25u5BB6%25u5C45%253B%25u8BBE%25u8BA1%253B%25u63D2%25u753B%253B%25u7535%25u5F71%253B%25u65C5%25u884C%253B%25u624B%25u5DE5%253B%25u5973%25u88C5%253B%25u7537%25u88C5%253B%25u914D%25u9970%253B%25u7F8E%25u98DF%253B%25u6444%25u5F71%253B%25u827A%25u672F%253B%257C%253B%25u5C01%25u9762%253B%25u52A8%25u6F2B%253B%25u6000%25u65E7%253B%25u8857%25u62CD%253B%25u5C0F%25u5B69%253B%25u5BA0%25u7269%253B%25u690D%25u7269%253B%25u4EBA%25u7269; sessionid=215c308a-27c1-444a-a220-9f59de0eebec; username=matchDay; _auth_user_id=10249029; js=1; __utmb=74400135.1.10.1457791779; __utmc=74400135; __utmt=1");
    return httpClient.execute(httppost, new basicHandler<String>() {
      @Override
      String handler(HttpResponse response) throws IOException {
        String s = EntityUtils.toString(response.getEntity());
        Map resp = CtxHolder.mapper.readValue(s, Map.class);
        return (String) ((Map) resp.get("data")).get("img_url");
      }
    }, new BasicHttpContext());
  }

  public static void main(String[] args) throws IOException {
    Map<String, File> postImgFile = new HashMap();
    List<String> displayHeaders = Lists.newArrayList("商品名称", "主图", "详情图");
    List<String> headers = Lists.newArrayList("name", "主图", "详情图");
    List<Object> contents = new ArrayList<>();
    File img = new File("/home/hellokitty/桌面/测试");
    for (File invDir : img.listFiles()) {
      Map<String, Object> oneinv = new HashMap();
      oneinv.put("name", invDir.getName());
      for (File picDir : invDir.listFiles()) {
        if ("主图".equals(picDir.getName()) || "详情图".equals(picDir.getName())) {
          List<String> urls = Lists.newArrayList();
          for (File pic : picDir.listFiles()) {
            postImgFile.put("img", pic);
            String url = postImg(postImgFile);
            urls.add(url);
          }
          oneinv.put(picDir.getName(), Joiner.on(",").join(urls));
        }
      }
      contents.add(oneinv);
    }
    ExcelMaker.from(contents, headers)
        .displayHeaders(displayHeaders)
        .resultType(ExcelMaker.ExcelFileType.XLS)
        .create("/home/hellokitty/桌面/imgs.xls");

  }

  abstract static class basicHandler<T> implements ResponseHandler<T> {
    private final int UNSUCCESS_CODE = 300;

    @Override
    public T handleResponse(HttpResponse response) throws IOException {
      if (response.getStatusLine().getStatusCode() >= UNSUCCESS_CODE) {
        int code = response.getStatusLine().getStatusCode();
        String msg = EntityUtils.toString(response.getEntity());
        logger.info("code:" + code + "resp:" + msg);
        throw new HttpResponseException(response.getStatusLine().getStatusCode(),
            "request error. code:" + code + "resp:" + msg);
      }
      return handler(response);
    }

    abstract T handler(HttpResponse response) throws IOException;
  }
}


