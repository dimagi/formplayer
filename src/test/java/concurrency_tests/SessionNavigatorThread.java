package concurrency_tests;

import beans.SessionNavigationBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

/**
 * Created by willpride on 4/5/17.
 */
public class SessionNavigatorThread extends Thread {

    TestRequest testRequest;

    public SessionNavigatorThread(TestRequest testRequest) {
        super();
        this.testRequest = testRequest;
    }

    public void run() {
        Random randomGenerator = new Random();
        while (1==1) {
            try {
                String result = sessionNavigate(testRequest);
                testRequest.loadNewResult(result);
                System.out.println(String.format("TestRequest %s passed.", testRequest));
                if (randomGenerator.nextInt(3) == 1) {
                    System.out.println(String.format("Deleting dbs for request %s", testRequest));
                    deleteApplicationDbs(testRequest);
                }
                Thread.sleep(randomGenerator.nextInt(10) * 1000);
            } catch (InterruptedException | IOException e) {
                System.out.println(e);
            } catch (RuntimeException e) {
                System.err.println(String.format("TestRequest %s failed.", testRequest));
                throw e;
            }
        }
    }

    private static void deleteApplicationDbs(TestRequest testRequest) throws IOException {
        makeRequest(testRequest.getAppId(),
                testRequest.getUsername(),
                testRequest.getDomain(),
                testRequest.getSelections(),
                "delete_application_dbs");
    }

    static String sessionNavigate(TestRequest testRequest) throws IOException {
        return makeRequest(testRequest.getAppId(), testRequest.getUsername(), testRequest.getDomain(), testRequest.getSelections(), "navigate_menu");
    }

    private static String makeRequest(String appId, String username, String domain, String[] selections, String url) throws IOException {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setSelections(selections);
        sessionNavigationBean.setAppId(appId);
        sessionNavigationBean.setDomain(domain);
        sessionNavigationBean.setUsername(username);
        return makeRequest(sessionNavigationBean, url);
    }

    static String makeRequest(Object bean, String url) throws IOException {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("http://localhost:8090/" + url + "/");

        HttpEntity e = new StringEntity(new ObjectMapper().writeValueAsString(bean));
        httppost.setEntity(e);
        httppost.setHeader("Cookie", "sessionid="+ "exrjtza7k4q8xpq9xud9ogkg4x96i8df");
        httppost.setHeader("Accept", "application/json");
        httppost.setHeader("Content-type", "application/json");

        //Execute and get the response.
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (InputStream instream = entity.getContent()) {
                return getStringFromInputStream(instream);
            }
        }
        return null;
    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }
}
