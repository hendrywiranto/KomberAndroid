package com.example.hendry.finalprojectkomber;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ListView listView;
    private Button buttonScan;
    private Button refreshButton;
    private int size = 0;
    private List<ScanResult> results;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter adapter;
    private static DecimalFormat df2 = new DecimalFormat(".##");
    private static final Set<String> targetAP = new HashSet<String>(Arrays.asList(
//            new String[] {"04:4f:4c:0c:a2:bb", "4c:5e:0c:9d:fb:ff", "90:c7:d8:ba:38:b2"} //huawei, ajk, andro
            new String[] {"04:4f:4c:0c:a2:bb", "90:c7:d8:ba:38:b2", "14:dd:a9:3c:88:59"} //huawei, andro, asus
//            new String[] {"04:4f:4c:0c:a2:bb", "90:c7:d8:ba:38:b2", "0a:c5:e1:04:c6:bd"} //huawei, andro, zollav
//            new String[] {"08:3f:bc:c0:aa:52", "4c:5e:0c:9d:fb:ff", "10:fe:ed:9b:93:ac"}
    ));
    Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonScan = (Button) findViewById(R.id.scanBtn);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanWifi();
            }
        });

        refreshButton = (Button) findViewById(R.id.refreshBtn);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshVar();
            }
        });

        listView = (ListView) findViewById(R.id.wifiList);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        scanWifi();
    }

    private void refreshVar() {
        map.clear();
        arrayList.clear();
        adapter.notifyDataSetChanged();
    }

    private void scanWifi() {
        arrayList.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
    }

    private int strengthMeter(int dBm, int freq){
        double FSPL = 27.55;
        double a = 10;
        double b = ((FSPL - (20 * Math.log10((double) freq))-dBm)/20);
        double m = Math.pow(a,b);

        return (int) m;
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            results = wifiManager.getScanResults();
            unregisterReceiver(this);

            for (ScanResult scanResult : results) {
                if (!targetAP.contains(scanResult.BSSID)) continue;
                int distance = strengthMeter(scanResult.level,scanResult.frequency);

                if (map.get(scanResult.BSSID) == null) map.put(scanResult.BSSID, new Vector<Integer>());
                map.get(scanResult.BSSID).add(distance);
            }

            final TextView xy = (TextView) findViewById(R.id.xy);
            RequestQueue queue = Volley.newRequestQueue(context);
            String distanceAP = "";
//            for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
//                distanceAP += "/" + calculateMean(entry.getValue());
//            }
            if (map.get("90:c7:d8:ba:38:b2") != null) distanceAP += "/" + calculateMean(map.get("90:c7:d8:ba:38:b2"));
            if (map.get("04:4f:4c:0c:a2:bb") != null) distanceAP += "/" + calculateMean(map.get("04:4f:4c:0c:a2:bb"));
            if (map.get("14:dd:a9:3c:88:59") != null) distanceAP += "/" + calculateMean(map.get("14:dd:a9:3c:88:59"));
            final String url ="http://10.151.36.172:9999/send" + distanceAP;

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the first 500 characters of the response string.
                            xy.setText("URL: " + url + " Response is: "+ response);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    xy.setText("That didn't work! " + error.toString() + "URL: " + url);
                }
            });

            queue.add(stringRequest);

            WebView mapWebView = (WebView) findViewById(R.id.webview);
            mapWebView.setWebViewClient(new WebViewClient());
            mapWebView.clearCache(true);
            mapWebView.clearHistory();
            mapWebView.getSettings().setJavaScriptEnabled(true);
            mapWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            mapWebView.getSettings().setLoadWithOverviewMode(true);
            mapWebView.getSettings().setUseWideViewPort(true);
            mapWebView.loadUrl(url);

            for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
                if (entry.getValue().size() > 10){
                    map.put( entry.getKey(), removeNoise(entry.getValue()) );
                }
            }

            for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
                arrayList.add(entry.getKey() + System.getProperty("line.separator")
                        + "Hasil scan: " + showAllElement(entry.getValue()) + System.getProperty("line.separator")
                        + "Mean: " + roundTwoDecimals(calculateMean(entry.getValue())) );
                adapter.notifyDataSetChanged();
            }
        }
    };

    private String showAllElement(List<Integer> distanceList){
        String total = "";
        for (int i = 0; i < distanceList.size(); i++) {
            total += distanceList.get(i) + " ";
        }
        return total;
    }

    private double calculateMean(List<Integer> distanceList){
        double total = 0;
        int n = distanceList.size();
        for (int i = 0; i < n; i++) {
            total += distanceList.get(i);
        }
        return total/(double) n;
    }

    private List<Integer> removeNoise(List<Integer> distanceList){
        int n = distanceList.size();
        double mean = calculateMean(distanceList);
        int maxIndex = 0;
        for (int i = 0; i < n; i++) {
            if( Math.abs(distanceList.get(i)-mean) > Math.abs(distanceList.get(maxIndex)-mean) ){
                maxIndex = i;
            }
        }
        distanceList.remove(maxIndex);
        return distanceList;
    }

    private double roundTwoDecimals(double d)
    {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
    }
}
