package com.example.administrator.bluetoothapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    ToggleButton tb_bluetooth;
    ListView lv_bluetooth_devices;
    ProgressBar progress;
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean mScanning;
    // 10秒后停止查找搜索.
    private static final long SCAN_PERIOD = 10000;
    BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ArrayList<BluetoothDevice> mLeDevices;
    BluetoothDevice device;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tb_bluetooth = (ToggleButton) findViewById(R.id.tb_bluetooth);
        lv_bluetooth_devices = (ListView) findViewById(R.id.lv_bluetooth_devices);
        progress= (ProgressBar)findViewById(R.id.progress);
        initActionBar();
        initView();
        initData();
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("蓝牙");
    }

    private void initView() {
        // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "你的手机不支持蓝牙设配", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        mLeDevices=new ArrayList<>();
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        lv_bluetooth_devices.setAdapter(mLeDeviceListAdapter);


        IntentFilter mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);
        mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        // 注册广播接收器，接收并处理搜索结果
        registerReceiver(receiver, mFilter);// 不要忘了之后解除绑定


        tb_bluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 判断蓝牙是否打开，如果没有则打开蓝牙
                    // adapter.enable() 直接打开蓝牙，但是不会弹出提示，以下方式会提示用户是否打开
                    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                        //直接打开系统的蓝牙设置面板
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        //是设备能够被搜索
                        intent.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//                        // 设置蓝牙可见性，最多300秒
//                        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                        startActivityForResult(intent, 0);
                    }else{
                        scanLeDevice(true);
                    }
                    buttonView.setChecked(true);
                } else {
                    scanLeDevice(false);
                    mLeDeviceListAdapter.clear();
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    buttonView.setChecked(false);
                }
            }
        });


        lv_bluetooth_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e("msgParent", "Parent= "+parent);
                Log.e("msgView", "View= "+view);
                Log.e("msgChildView", "ChildView= "+parent.getChildAt(position-parent.getFirstVisiblePosition()));
                device=mLeDeviceListAdapter.getDevice(position);

                try {
                    // 配对
                    Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                    createBondMethod.invoke(device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initData() {

    }

    //开始进行蓝牙设备的搜索
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;
            mBluetoothAdapter.startDiscovery(); //开始搜索
            progress.setVisibility(View.VISIBLE);
        } else {
            mScanning = false;
            mBluetoothAdapter.cancelDiscovery();//停止搜索
            progress.setVisibility(View.GONE);
        }
    }

    ;

    // 创建一个接收ACTION_FOUND广播的BroadcastReceiver
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            progress.setVisibility(View.VISIBLE);
            // 发现设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 从Intent中获取设备对象
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 将设备名称和地址放入array adapter，以便在ListView中显示
                mLeDevices.add(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
            progress.setVisibility(View.GONE);
        }
    };




    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 获得已经搜索到的蓝牙设备
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 搜索到的不是已经绑定的蓝牙设备
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // 防止重复添加
                    if (mLeDevices.indexOf(device) == -1)
                        mLeDevices.add(device);
                    //devicesList.add("未配对 | "+device.getName() + "（"  + device.getAddress()+"）");
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
                // 搜索完成
            } else if (action
                    .equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                progress.setVisibility(View.GONE);
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                // 状态改变的广播
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                if (device.getName().equalsIgnoreCase(name)) {
                    int connectState = device.getBondState();
                    switch (connectState) {
                        case BluetoothDevice.BOND_NONE:  //10
                            Toast.makeText(MainActivity.this, "取消配对："+device.getName(), Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothDevice.BOND_BONDING:  //11
                            Toast.makeText(MainActivity.this, "正在配对："+device.getName(), Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothDevice.BOND_BONDED:   //12
                            Toast.makeText(MainActivity.this, "完成配对："+device.getName(), Toast.LENGTH_SHORT).show();
                            //getBondedDevices();
                            try {
                                // 连接
                               new ConnectTask().execute();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "蓝牙已启用", Toast.LENGTH_SHORT).show();
                    // Initializes list view adapter.
                    scanLeDevice(true);
                } else {
                    Toast.makeText(this, "蓝牙未启用", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private LayoutInflater mInflator;
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText("不知道此设配");
            }
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();
    }



///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //这条是蓝牙串口通用的UUID，不要更改
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = ""; // <==要连接的目标蓝牙设备MAC地址
    private BluetoothSocket btSocket = null;


    //连接蓝牙设备的异步任务
    class ConnectTask extends AsyncTask<String,String,String>
    {
        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();
                Log.e("error", "ON RESUME: BT connection established, data transfer link open.");
            } catch (IOException e) {
                try {
                    btSocket.close();
                    return "Socket 创建失败";
                } catch (IOException e2) {
                    Log .e("error","ON RESUME: Unable to close socket during connection failure", e2);
                    return "Socket 关闭失败";
                }
            }
            //取消搜索
            mBluetoothAdapter.cancelDiscovery();
            return "蓝牙连接正常,Socket 创建成功";
        }

        @Override    //这个方法是在主线程中运行的，所以可以更新界面
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            Toast.makeText(MainActivity.this,result,Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        try {
            if(btSocket!=null)
            {
                btSocket.close();
                btSocket=null;
            }
            this.finish();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
