package SydTodayPost;


import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.commons.logging.Log;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: neilmao
 * Date: 9/08/2014
 */

public class Spider implements Runnable {

    private static final Log log = LogFactory.getLog(Spider.class);

    private final String HOST = "http://www.sydneytoday.com/";
    private final int INTERVAL = (60 * 60 + 10) * 1000; // every 1 hour plus 10 secs

    private final String SUCCESS_CODE = "恭喜你";
    private final String FAILED_CODE = "距离上次顶贴时间1小时后";

    private HttpClient httpClient;
    private Properties configure;

    private long successfulCount;
    private long failedCount;
    private long unknownCount;

    private boolean active;

    public Spider(Properties properties) {
        configure = properties;
        successfulCount = 0;
        failedCount = 0;
        getHttpClient();
        active = false;
    }

    @Override
    public void run() {
        active = true;
        // init session
        CookieStore cookieStore = new BasicCookieStore();
        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

        // prepare to login
        log.info("Start logging in...");
        String loginLink = HOST + "do/login.php?f";
        Map<String, String> loginParams = new HashMap<String, String>();
        loginParams.put("username", configure.getProperty("username"));
        loginParams.put("password", configure.getProperty("password"));
        loginParams.put("cookietime", "0");
        loginParams.put("step", "2");
        loginParams.put("fromurl", HOST);

        try {
            HttpResponse loginResponse = postRequest(loginLink, loginParams, httpContext);
            if (loginResponse.getStatusLine().getStatusCode() == 302) {
                log.info("Logging successfully.");
            } else {
                log.warn("Incorrect credentials. Program stops.");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //prepare to update thread
        while(active) {
            try {
                doUpdate(httpContext);
            } catch (Exception e) {
                log.error("Posting error.");
                e.printStackTrace();
            }
        }
    }

    private void doUpdate(HttpContext httpContext) throws Exception {
        log.info("Begin posting...");
        String thread = configure.getProperty("thread");
        String paramStr = thread.substring(thread.indexOf('?') + 1);
        paramStr += "&job=update";
        HttpResponse postResponse = getRequest(HOST + "job.php" , paramStr, httpContext);
        try {
            String responseHtml = inputStreamToString(postResponse.getEntity().getContent());
            if (responseHtml.contains(SUCCESS_CODE)) {
                successfulCount++;
                log.info("Post successfully.");
                reportStatus();
                Thread.sleep(INTERVAL);
            } else {
                int delay = Integer.parseInt(configure.getProperty("delay"));
                if (responseHtml.contains(FAILED_CODE)) {
                    failedCount++;
                    log.warn("Post failed, waiting for " + Math.round(delay / 1000 / 60) + " mins.");
                } else {
                    unknownCount++;
                    log.error("Unknown response: " + responseHtml);
                }
                reportStatus();
                Thread.sleep(delay);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reportStatus() {
        log.info("Total attempts: " + (successfulCount + failedCount + unknownCount) +
                ", Success: " + successfulCount + ", Failed: " + failedCount +
                ", Unknown: " + unknownCount);
    }

    private HttpResponse getRequest(String link, String paramStr, HttpContext context) throws IOException {
        HttpGet get = new HttpGet(link + "?" + paramStr);
        return httpClient.execute(get, context);
    }

    private HttpResponse postRequest(String link, Map<String, String> params, HttpContext context) throws IOException {
        HttpPost post = new HttpPost(link);
        List<NameValuePair> nvps = new LinkedList<NameValuePair>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        post.setEntity(new UrlEncodedFormEntity(nvps));
        return httpClient.execute(post, context);
    }

    private String inputStreamToString(InputStream inputStream) throws IOException {
        Reader reader = new InputStreamReader(inputStream, "gb2312");
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String buffer;
        while ((buffer = bufferedReader.readLine()) != null) {
            sb.append(buffer).append("\n");
        }
        return sb.toString();
    }

    private HttpClient getHttpClient() {
        httpClient = HttpClients.createDefault();
        return httpClient;
    }

    public void stop() {
        active = false;
        log.warn("Stopping is scheduled.");
    }
}
