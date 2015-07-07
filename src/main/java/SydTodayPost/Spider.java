package SydTodayPost;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: neilmao
 * Date: 9/08/2014
 */

public class Spider implements Runnable {

    private static final Log LOG = LogFactory.getLog(Spider.class);

    private final static String HOST = "http://www.sydneytoday.com/";
    private final static long INTERVAL = (60 * 60 + 10) * 1000; // every 1 hour plus 10 secs
    private final static int TIMEOUT = 15 * 1000;
    private final static String SUCCESS_CODE = "恭喜你";
    private final static String FAILED_CODE = "距离上次顶贴时间1小时后";

    private Properties properties;

    private long successfulCount;
    private long failedCount;
    private long unknownCount;

    private boolean active;
    private Status status;
    private long startTimeStamp;
    private long delay;

    public Spider(Properties properties) {
        this.properties = properties;
        successfulCount = 0;
        failedCount = 0;
        active = false;
        status = Status.Wait;
        delay = Long.parseLong(properties.getProperty("delay"));
    }

    public void run() {
        active = true;
        // init session
        CookieStore cookieStore = new BasicCookieStore();
        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

        // Load login page
        LOG.info("Loading logging page...");
        String loginPage = HOST + "user/login?destination=/";
        String loginFormHtml = null;
        try{
            HttpResponse response = getRequest(getHttpClient(), loginPage, "", httpContext);
            loginFormHtml = inputStreamToString(response.getEntity().getContent());
        } catch (IOException e) {
            LOG.warn("Failed to load login page.");
        }

        if (StringUtils.isBlank(loginFormHtml)) {
            LOG.warn("Empty response.");
            return;
        }

        Document rootDoc = Jsoup.parse(loginFormHtml);
        Elements elements = rootDoc.select("#login-form input[name=form_build_id]");
        if (elements.size() == 0) {
            LOG.warn("form_build_id not found.");
            return;
        }

        org.jsoup.nodes.Element element = elements.get(0);
        String formBuildId = element.val();

        // prepare to login
        LOG.info("Start logging in...");
        String loginLink = HOST + "user/login?destination=/";
        Map<String, String> loginParams = new HashMap<String, String>();
        loginParams.put("name", properties.getProperty("username"));
        loginParams.put("pass", properties.getProperty("password"));
        loginParams.put("form_id", "user_login");
        loginParams.put("form_build_id", formBuildId);

        try {
            HttpResponse loginResponse = postRequest(getHttpClient(), loginLink, loginParams, httpContext);
            if (loginResponse.getStatusLine().getStatusCode() == 302) {
                LOG.info("Logging successfully.");
            } else {
                LOG.warn("Incorrect credentials. Program stops.");
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
                LOG.error("Posting error.");
                e.printStackTrace();
            }
        }
        LOG.info("Program stopped.");
    }

    private void doUpdate(HttpContext httpContext) throws Exception {
        LOG.info("Begin posting...");
        String thread = properties.getProperty("thread");
        String id = thread.substring(thread.lastIndexOf('/') + 1);
        HttpResponse getResponse = getRequest(getHttpClient(), HOST + "nodesticky/" + id , "", httpContext);
        try {
            String responseHtml = inputStreamToString(getResponse.getEntity().getContent());
            ObjectMapper objectMapper = new ObjectMapper();

            UpvoteResponse result = objectMapper.readValue(responseHtml, UpvoteResponse.class);

            if (1 == result.getStatus()) {
                successfulCount++;
                LOG.info("Post successfully.");
                startTimeStamp = System.currentTimeMillis();
                status = Status.Wait;
                reportStatus();
                Thread.sleep(INTERVAL);
            } else {
                if (2 == result.getStatus()) {
                    failedCount++;
                    LOG.warn("Post failed, waiting for " + Math.round(delay / 1000 / 60) + " mins.");
                } else {
                    if (-1 == result.getStatus()) {
                        failedCount++;
                        LOG.warn("Need to login first.");
                    }
                }
                startTimeStamp = System.currentTimeMillis();
                status = Status.Delay;
                reportStatus();
                Thread.sleep(delay);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reportStatus() {
        long restTime;
        long now = System.currentTimeMillis();
        if (status == Status.Wait) {
            restTime = INTERVAL - (now - startTimeStamp);
        } else {
            restTime = delay - (now - startTimeStamp);
        }
        LOG.info("Total attempts: " + (successfulCount + failedCount + unknownCount) +
                ", Success: " + successfulCount + ", Failed: " + failedCount +
                ", Unknown: " + unknownCount + "; Will retry in " + Math.round(restTime / 1000 / 60) + " mins.");
    }

    private HttpResponse getRequest(HttpClient httpClient, String link, String paramStr, HttpContext context) throws IOException {
        if (StringUtils.isNotBlank(paramStr))
            link = link + "?" + paramStr;

        HttpGet get = new HttpGet(link);
        return httpClient.execute(get, context);
    }

    private HttpResponse postRequest(HttpClient httpClient, String link, Map<String, String> params, HttpContext context) throws IOException {
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
        RequestConfig requestConfig = RequestConfig.custom().
                setConnectTimeout(TIMEOUT).
                setConnectionRequestTimeout(TIMEOUT).
                setSocketTimeout(TIMEOUT).
                build();
        return HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
    }

    public void stop() {
        active = false;
        LOG.warn("Stopping is scheduled.");
    }
}
