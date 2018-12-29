package cn.com.hisistar.statusbartest;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class WeatherAutoUpdateService extends Service {
    private static final String TAG = "WeatherAutoUpdate";
    public static final String ACTION_REPORT_WEATHER = "cn.com.hisistar.action.ReportWeather";
    int count = 0;
    int times = 10;

    public WeatherAutoUpdateService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        updateWeather();
        new Thread() {
            @Override
            public void run() {
                super.run();
                updateWeather();
            }
        }.start();

        updateOnTime(10);

        return super.onStartCommand(intent, flags, startId);
    }

    private void updateOnTime(int time) {

        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
//        int anHour = 8 * 60 * 60 * 1000; // 这是8小时的毫秒数
        int anHour = time * 60 * 1000; // 这是1小时的毫秒数
        Log.e(TAG, "onStartCommand: elapsedRealtime = " + SystemClock.elapsedRealtime());
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, WeatherAutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        manager.cancel(pi);
        manager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
    }

    private void updateWeather() {
        Log.i(TAG, "Loading location infomation ...");
//        String ipJson = getHttpEntity("http://ip-api.com/json");
        String ipJson = getHttpEntity("http://whois.pconline.com.cn/ipJson.jsp?json=true");
        Log.e(TAG, "onHandleIntent: ipjson=" + ipJson);
        if (ipJson == null)
            return;
        String location = getLocationFromJson(ipJson);
        Log.e(TAG, "onHandleIntent: location=" + location);
        if (location == null)
            return;

        Log.i(TAG, "Querying weather infomation ...");
        String url = buildQueryUrl(location);
        Log.e(TAG, "onHandleIntent: url= " + url);
        String weatherJson = getHttpEntity(url);
        if (weatherJson == null)
            return;

        Log.i(TAG, "Report weather infomation ...");
        String weatherInfo = getWeatherInfo(weatherJson);
        count++;
        times = 10;
        Log.e(TAG, "updateWeather: count=" + count);
        Intent intentWeather = new Intent(ACTION_REPORT_WEATHER);
        // info "city;temp;text;code"
        intentWeather.putExtra("info", weatherInfo);
        sendBroadcast(intentWeather);
    }

    private String getHttpEntity(String url) {
        HttpGet getMethod = new HttpGet(url);
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response;
        try {
            response = httpClient.execute(getMethod);
        } catch (IOException e) {
            e.printStackTrace();
            if (times > 0)
                updateOnTime(1);
            times--;
            return null;
        }
        if (response.getStatusLine().getStatusCode() == 200) {
            try {
                return EntityUtils.toString(response.getEntity(), "utf-8");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }


    private String getLocationFromJson(String json) {
        try {
            JSONObject jObject = new JSONObject(json);
//            return jObject.getString("city") + ", " + jObject.getString("countryCode");
            return jObject.getString("city");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String buildQueryUrl(String location) {
        List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("q", "select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\"" + location + "\")"));
        params.add(new BasicNameValuePair("format", "json"));
        String param = URLEncodedUtils.format(params, "UTF-8");
        String baseUrl = "https://query.yahooapis.com/v1/public/yql";
        return baseUrl + "?" + param;
    }

    private String getWeatherInfo(String json) {
        Log.i(TAG, "getWeatherInfo");
        Log.e(TAG, "getWeatherInfo: json=" + json);
        try {
            JSONObject jObjectYahoo = new JSONObject(json);
            JSONObject channelObject = jObjectYahoo
                    .getJSONObject("query")
                    .getJSONObject("results")
                    .getJSONObject("channel");
            JSONObject locationObject = channelObject.getJSONObject("location");
            String text = locationObject.getString("city");
            JSONObject conditionObject = channelObject.getJSONObject("item").getJSONObject("condition");
            // 摄氏度(℃)=（华氏度(℉)-32）÷1.8
            text += ";" + String.valueOf((int) ((conditionObject.getInt("temp") - 32) / 1.8));
            text += ";" + conditionObject.getString("text");
            text += ";" + conditionObject.getString("code");
            Log.i(TAG, "weather info (city;temp;text;code) : " + text);
            return text;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}
