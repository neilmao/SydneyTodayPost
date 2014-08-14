package SydTodayPost;


import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: neilmao
 * Date: 9/08/2014
 */

public class Spider {

    private final String HOST = "http://www.sydneytoday.com/";

    public Spider() {
    }

    public void execute() {

    }

    public String sendRequest(String link, Map<String, String> params) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(link);
        List<NameValuePair> nvps = new LinkedList<NameValuePair>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        post.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response = httpClient.execute(post);

        return "";
    }
}
