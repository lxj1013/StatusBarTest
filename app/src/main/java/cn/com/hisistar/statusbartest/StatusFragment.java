package cn.com.hisistar.statusbartest;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.EthernetDataTracker;
import android.net.NetworkInfo;
import android.net.ethernet.EthernetManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.app.Fragment;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


/**
 * DATE 2018/12/14 10:41
 * AUTHOR lxj
 * 南无阿弥陀佛
 */
public class StatusFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = StatusFragment.class.getSimpleName();

    private ImageButton phone4GiView;
    private ImageButton wifiView;
    private ImageButton ethernetView;
    private ImageButton bluetoothView;
    private ImageButton hotspotView;
    private StatusReceiver mReceiver;

    private EthernetManager mEthernetManager;

    private WifiManager mWifiManager;

    private BluetoothAdapter mBluetoothAdapter;

    private TextView tempTv;
    private TextView weatherTv;

    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;
    private boolean is4GConnected = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.status_bar, container, false);

        initViews(view);

        registerStatusReceiver();

        initEthernetStatus();
        initBluetoothStatus();
        initWifiAPStatus();
        initPhoneStatus();
//        initWeatherStatus();
        return view;
    }

    @Override
    public void onStart() {


        super.onStart();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
    }


    private void initEthernetStatus() {
        mEthernetManager = (EthernetManager) getActivity().getSystemService("ethernet");
        if (mEthernetManager == null)
            return;
        if (mEthernetManager.getEthernetState() == EthernetManager.ETHERNET_STATE_ENABLED) {
            ethernetView.setVisibility(View.VISIBLE);
            if (mEthernetManager.getEthernetMode().equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)) {
                ethernetView.setImageResource(R.drawable.ethernet_on);
            } else
                ethernetView.setImageResource(R.drawable.ethernet_off);
        } else {
            ethernetView.setVisibility(View.GONE);
        }
    }

    private void initViews(View view) {
        phone4GiView = (ImageButton) view.findViewById(R.id.phone_4g_img);
        wifiView = (ImageButton) view.findViewById(R.id.wifi_img);
        ethernetView = (ImageButton) view.findViewById(R.id.ethernet_img);
        bluetoothView = (ImageButton) view.findViewById(R.id.bluetooth_img);
        hotspotView = (ImageButton) view.findViewById(R.id.hotspot_img);

        tempTv = (TextView) view.findViewById(R.id.temp);
        weatherTv = (TextView) view.findViewById(R.id.weather_txt);

        wifiView.setOnClickListener(this);
        ethernetView.setOnClickListener(this);
        bluetoothView.setOnClickListener(this);
        hotspotView.setOnClickListener(this);
    }


    private void initBluetoothStatus() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
            return;
        int state = mBluetoothAdapter.getState();
        bluetoothView.setVisibility((state == BluetoothAdapter.STATE_ON) ? View.VISIBLE : View.GONE);
        Log.e(TAG, "initViews: mBluetoothAdapter.getState()=" + state);
    }

    private void initWifiAPStatus() {
        mWifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager == null)
            return;
        int state = mWifiManager.getWifiState();
        hotspotView.setVisibility((state == WifiManager.WIFI_AP_STATE_ENABLED) ? View.VISIBLE : View.GONE);
        Log.e(TAG, "initViews: wifiAPState= " + mWifiManager.getWifiApState());
    }

    private void initWeatherStatus() {
        Intent intent = new Intent(getActivity(), WeatherAutoUpdateService.class);
        getActivity().startService(intent);
    }

    private void initPhoneStatus() {

        is4GConnected = is4GAvailable();
        phone4GiView.setVisibility(is4GConnected ? View.VISIBLE : View.GONE);

        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                int level = signalStrength.getLevel();
                if ((level >= 0) && (level <= 4)) {
                    phone4GiView.setVisibility(is4GConnected ? View.VISIBLE : View.GONE);
                    phone4GiView.setImageLevel(level);
                } else {
                    phone4GiView.setVisibility(View.GONE);
                }
                Log.d(TAG, "onSignalStrengthsChanged: level=" + signalStrength.getLevel());
                Log.d(TAG, "onSignalStrengthsChanged signalStrength=" + signalStrength.toString());
            }

            @Override
            public void onServiceStateChanged(ServiceState state) {
                Log.d(TAG, "onServiceStateChanged voiceState=" + state.toString());
            }

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                Log.d(TAG, "onCallStateChanged state=" + state + "  incomingNumber=" + incomingNumber);
            }

            @Override
            public void onDataConnectionStateChanged(int state, int networkType) {
                Log.d(TAG, "onDataConnectionStateChanged: state=" + state
                        + " networkType=" + networkType);
                if ((state == TelephonyManager.DATA_CONNECTED) && (networkType == TelephonyManager.NETWORK_TYPE_LTE)) {
                    is4GConnected = true;
                } else {
                    is4GConnected = false;
                }
            }

            @Override
            public void onDataActivity(int direction) {
                Log.d(TAG, "onDataActivity: direction=" + direction);
            }
        };

        mTelephonyManager = (TelephonyManager) getActivity().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY);
    }

    /**
     * 判断当前网络是否是4G网络
     *
     * @param
     * @return boolean
     */
    public boolean is4GAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            int networkType = telephonyManager.getNetworkType();
            /** Current network is LTE */
            if (networkType == 13) {
                /**此时的网络是4G的*/
                return true;
            }
        }
        return false;
    }


    private void registerStatusReceiver() {
        mReceiver = new StatusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        filter.addAction(WeatherAutoUpdateService.ACTION_REPORT_WEATHER);
        getActivity().registerReceiver(mReceiver, filter);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //添加启动外部应用的Flag，不然会报错
        intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        intent.setClassName("com.android.settings", "com.android.settings.Settings");//包名，要启动fragment所依赖的Activity
        switch (v.getId()) {
            case R.id.wifi_img:
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, "com.android.settings.wifi.WifiSettings");//要启动的fragment
                startActivity(intent);
                Toast.makeText(getActivity(), "wifi", Toast.LENGTH_SHORT).show();
                break;
            case R.id.ethernet_img:
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, "com.android.settings.EthernetSettings");//要启动的fragment
                startActivity(intent);
                Toast.makeText(getActivity(), "ethernet", Toast.LENGTH_SHORT).show();
                break;
            case R.id.bluetooth_img:
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, "com.android.settings.bluetooth.BluetoothSettings");//要启动的fragment
                startActivity(intent);
                Toast.makeText(getActivity(), "bluetooth", Toast.LENGTH_SHORT).show();
                break;
            case R.id.hotspot_img:
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, "com.android.settings.TetherSettings");//要启动的fragment
                startActivity(intent);
                Toast.makeText(getActivity(), "hotspot", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(getActivity(), "error!!!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private class StatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "onReceive: " + intent.getAction());
            String action = intent.getAction();
            if (action == null)
                return;
            int message = -1;
            int rel = -1;
            if (action.equals(EthernetManager.ETHERNET_STATE_CHANGED_ACTION)) {
                message = intent.getIntExtra(EthernetManager.EXTRA_ETHERNET_STATE, rel);

                if (message == EthernetDataTracker.EVENT_DHCP_CONNECT_SUCCESSED || message == EthernetDataTracker.EVENT_STATIC_CONNECT_SUCCESSED) {
                    ethernetView.setImageResource(R.drawable.ethernet_on);
                } else {
                    ethernetView.setImageResource(R.drawable.ethernet_off);
                }

                Log.e(TAG, "onReceive: " + EthernetManager.ETHERNET_STATE_CHANGED_ACTION + " message=" + message);
            }

            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                boolean wifiState = info.isConnected();
                wifiView.setVisibility(wifiState ? View.VISIBLE : View.GONE);
                Log.e(TAG, "onReceive: " + WifiManager.NETWORK_STATE_CHANGED_ACTION + " wifiState=" + wifiState);
            }


            if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                wifiView.setImageLevel(WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 4));
                Log.e(TAG, "onReceive: " + WifiManager.RSSI_CHANGED_ACTION + " rssi=" + wifiInfo.getRssi());
            }

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                bluetoothView.setVisibility(state == BluetoothAdapter.STATE_ON ? View.VISIBLE : View.GONE);
                Log.e(TAG, "onReceive: " + BluetoothAdapter.ACTION_STATE_CHANGED + " state =" + state);
            }

            if (action.equals(WifiManager.WIFI_AP_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED);
                hotspotView.setVisibility(state == WifiManager.WIFI_AP_STATE_ENABLED ? View.VISIBLE : View.GONE);
                Log.e(TAG, "onReceive: " + WifiManager.WIFI_AP_STATE_CHANGED_ACTION + " state=" + state);
            }

            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                is4GConnected = is4GAvailable();
                phone4GiView.setVisibility(is4GConnected ? View.VISIBLE : View.GONE);
            }

            if (action.equals(WeatherAutoUpdateService.ACTION_REPORT_WEATHER)) {
                String weatherInfo = intent.getStringExtra("info");
                Log.e(TAG, "onReceive: weatherInfo=" + weatherInfo);
                String weatherInfos[] = weatherInfo.split(";");
                if (weatherInfos.length == 4) {
                    tempTv.setVisibility(View.VISIBLE);
                    weatherTv.setVisibility(View.VISIBLE);
                    String temp = weatherInfos[1] + "℃";
                    tempTv.setText(temp);
                    int index = Integer.parseInt(weatherInfos[3]);
                    String weather = getResources().getStringArray(R.array.weather)[index];
                    weatherTv.setText(weather);
                } else {
                    tempTv.setVisibility(View.GONE);
                    weatherTv.setVisibility(View.GONE);
                }
            }

        }
    }


}
