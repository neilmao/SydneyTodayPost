package SydTodayPost;


import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: neilmao
 * Date: 9/08/2014
 */

public class Spider {

    private static final Log log = LogFactory.getLog(Spider.class);

    private final String HOST = "http://www.sydneytoday.com/";
    private final int INTERVAL = (60 * 60 + 10) * 1000; // every 1 hour plus 10 secs
    private final int DELAY = 15 * 60 * 1000; // every 15 mins to try if previous action failed

    public Spider() {
    }

    public void execute() {
        // init session
        CookieStore cookieStore = new BasicCookieStore();
        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

        // prepare to login
        log.info("Start logging in...");
        String loginLink = HOST + "do/login.php?f";
        Map<String, String> loginParams = new HashMap<String, String>();
        loginParams.put("username", "aa");
        loginParams.put("password", "bb");
        loginParams.put("cookietime", "0");
        loginParams.put("step", "2");
        loginParams.put("fromurl", HOST);

        try {
            HttpResponse loginResponse = sendRequest(loginLink, loginParams, httpContext);
            if (loginResponse.getStatusLine().getStatusCode() == 302) {
                log.info("Logging successfully.");
            } else {
                log.warn("Incorrect credentials. Program stops.");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //prepare to post

    }

    public HttpResponse sendRequest(String link, Map<String, String> params, HttpContext context) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(link);
        List<NameValuePair> nvps = new LinkedList<NameValuePair>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        post.setEntity(new UrlEncodedFormEntity(nvps));
        return httpClient.execute(post, context);
    }
}
