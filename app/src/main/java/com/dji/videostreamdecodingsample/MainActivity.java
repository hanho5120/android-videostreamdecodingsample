package com.dji.videostreamdecodingsample;


import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.internal.Objects;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import dji.common.error.DJIError;
import dji.common.error.DJIRemoteControllerError;
import dji.common.error.DJISDKError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.flighthub.model.User;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.sdksharedlib.DJISDKCache;

/** Main activity that displays three choices to user */
public class MainActivity extends Activity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
    private static final String TAG = "MainActivity";
    private static final String LAST_USED_BRIDGE_IP = "bridgeip";
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static boolean isAppStarted = false;


    //추가한 변수 리스트
    TextView stateText;
    Handler mHandler = null;
    Thread mThread = null;
    Button btn_map_widget;
    Button btn_check;
    TextView edit_idText;
    TextView edit_pwText;
    View view_userInfo;
    View view_droneimg;
    View view_background;
    View view_inform;
    boolean checkUsbConnection;


    //값 체크 부분 리스트
    public static boolean bool_onRegister = false;
    public static boolean bool_onProductConnect = false;

    public static StringBuilder LOG = new StringBuilder();

    public static final int SEND_LOG = 0;
    public static final int SEND_VIS = 1;


    private DJISDKManager.SDKManagerCallback registrationCallback = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(DJIError error) {
            isRegistrationInProgress.set(false);
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                //loginAccount();
                DJISDKManager.getInstance().startConnectionToProduct();

                //Toast.makeText(getApplicationContext(), "SDK registration succeeded!", Toast.LENGTH_LONG).show();
                bool_onRegister = true;
                //LOG.append("SDK 등록 성공"+"\n");
                LOG.append("SDK 등록 성공, ");
                //bt_map_widget.setVisibility(View.VISIBLE);
            } else {
                //Toast.makeText(getApplicationContext(),"SDK registration failed, check network and retry!",Toast.LENGTH_LONG).show();
                bool_onRegister = false;
                //LOG.append("SDK 등록 실패"+"\n");
                LOG.append("SDK 등록 실패, ");
            }
        }

        @Override
        public void onProductDisconnect() {
            //Toast.makeText(getApplicationContext(),"product disconnect!",Toast.LENGTH_LONG).show();
            //LOG.append("제품 연결 안 됨"+"\n");
            bool_onProductConnect = false;
            checkUsbConnection = isConnected();
            LOG.append("제품 연결 안 됨, ");
        }

        @Override
        public void onProductConnect(BaseProduct product) {
            //Toast.makeText(getApplicationContext(),"product connect!",Toast.LENGTH_LONG).show();
            //LOG.append("제품 연결 됨"+"\n");
            bool_onProductConnect = true;
            LOG.append("제품 연결 됨, ");
        }

        @Override
        public void onProductChanged(BaseProduct product) {
            LOG.append("onProductChanged, ");
        }

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key,
                                      BaseComponent oldComponent,
                                      BaseComponent newComponent) {
            //Toast.makeText(getApplicationContext(),key.toString() + " changed",Toast.LENGTH_LONG).show();
            //LOG.append("컴포넌트 변경 됨"+"\n");
            LOG.append("컴포넌트 변경 됨, ");
        }

        @Override
        public void onInitProcess(DJISDKInitEvent event, int totalProcess) {
        }

        @Override
        public void onDatabaseDownloadProgress(long current, long total) {
            LOG.append("onDatabaseDownloadProgress, ");
        }
    };

    private void loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        //Toast.makeText(getApplicationContext(),"Login Success!",Toast.LENGTH_LONG).show();
                        //LOG.append("로그인 성공"+"\n");
                        LOG.append("로그인 성공, ");
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        //Toast.makeText(getApplicationContext(),"Login Error!",Toast.LENGTH_LONG).show();
                        //LOG.append("로그인 에러"+"\n");
                        LOG.append("로그인 에러, ");
                    }
                });

    }

    public static boolean isStarted() {
        return isAppStarted;
    }

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE, // Gimbal rotation
            Manifest.permission.INTERNET, // API requests
            Manifest.permission.ACCESS_WIFI_STATE, // WIFI connected products
            Manifest.permission.ACCESS_COARSE_LOCATION, // Maps
            Manifest.permission.ACCESS_NETWORK_STATE, // WIFI connected products
            Manifest.permission.ACCESS_FINE_LOCATION, // Maps
            Manifest.permission.CHANGE_WIFI_STATE, // Changing between WIFI and USB connection

            Manifest.permission.WRITE_EXTERNAL_STORAGE, // Log files
            Manifest.permission.BLUETOOTH, // Bluetooth connected products
            Manifest.permission.BLUETOOTH_ADMIN, // Bluetooth connected products
            Manifest.permission.READ_EXTERNAL_STORAGE, // Log files
            Manifest.permission.READ_PHONE_STATE, // Device UUID accessed upon registration
            Manifest.permission.RECORD_AUDIO, // Speaker accessory,
            Manifest.permission.CAMERA
    };
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private List<String> missingPermission = new ArrayList<>();
    private EditText bridgeModeEditText;

    /*
    @Override
    protected void onStart() {
        super.onStart();
        //checkAndRequestPermissions();//??
        startSDKRegistration();
    }
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkUsbConnection = isConnected();
        setContentView(R.layout.activity_main);  //화면 매칭 -> xml이랑 연결
        isAppStarted = true;
        findViewById(R.id.complete_ui_widgets).setOnClickListener(this);
        findViewById(R.id.bt_customized_ui_widgets).setOnClickListener(this);

        //추가 부분--------------
        btn_map_widget = (Button) findViewById(R.id.btn_map_widget);
        btn_check = (Button) findViewById(R.id.btn_check);

        //로그인정보

        view_userInfo = (View) findViewById(R.id.view_userInfo);
        view_droneimg = (View) findViewById(R.id.view_droneimg);
        view_background = (View) findViewById(R.id.view_background);
        view_inform = (View) findViewById(R.id.view_inform);
        view_background.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                //bool_onRegister=true;
            }
        });
        edit_idText = (TextView) findViewById(R.id.edit_idText);
        edit_pwText = (TextView) findViewById(R.id.edit_pwText);

        findViewById(R.id.btn_map_widget).setOnClickListener(this);


        btn_map_widget.setOnClickListener(new View.OnClickListener(){
        //보내는것
            @Override
            public void onClick(View v) {
                /*
                String droneid = edit_idText.getText().toString();
                String dronepw = edit_pwText.getText().toString();


                intent.putExtra("droneid",droneid);
                intent.putExtra("dronepw",dronepw);

*/
                Userdata.getInstance()._id = edit_idText.getText().toString();
                Userdata.getInstance()._pw = edit_pwText.getText().toString();

                /* DB 대조 */
                ContentValues values = new ContentValues();
                //values.put("login_key", "24d2bd1d-5617-46cd-a49c-e1ecbb69fc4d");
                JsonObject temp = new JsonObject();

                temp.addProperty("login_key", "24d2bd1d-5617-46cd-a49c-e1ecbb69fc4d");
                //NetworkTask networkTask = new NetworkTask("https://101.55.28.64:444/api/get-conference-list", temp);
                NetworkTask networkTask = new NetworkTask(Userdata.getInstance()._server_url+"/api/get-conference-list", temp);
                //LOG.append(Userdata.getInstance()._id.toString() +", "+Userdata.getInstance()._pw.toString()+", ");
                if(Userdata.getInstance()._id.equals("") || Userdata.getInstance()._pw.equals("")) {
                    LOG.append("ID 또는 PW를 확인해주세요, ");
                    Toast.makeText(getApplicationContext(), "ID 또는 PW를 확인해주세요", Toast.LENGTH_LONG).show();
                }
                else
                    networkTask.execute();

                //edit_idText에 들어왔는지 찍어봐
                //Toast.makeText(getApplicationContext(), Userdata.getInstance()._profile_photo.toString(), Toast.LENGTH_LONG).show();

                //Intent intent = new Intent(MainActivity.this, Webrtc1.class);
                //startActivity(intent);
            }
        });

        findViewById(R.id.btn_check).setOnClickListener(this);

        btn_check.setOnClickListener(new View.OnClickListener(){
            //보내는것
            @Override
            public void onClick(View v) {
                DJISDKManager.getInstance().destroy();
                startSDKRegistration();

                LOG.append("버튼 누름");
                //usb
            }
        });



        TextView versionText = (TextView) findViewById(R.id.version);
        versionText.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));

        stateText = (TextView)findViewById(R.id.state);

        Thread mThread = new mThread();
        mThread.setDaemon(true);
        mThread.start();
        mHandler = new Handler();


        //------------------------------

        bridgeModeEditText = (EditText) findViewById(R.id.edittext_bridge_ip);
        bridgeModeEditText.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(LAST_USED_BRIDGE_IP,""));
        bridgeModeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event != null
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event != null && event.isShiftPressed()) {
                        return false;
                    } else {
                        // the user is done typing.
                        handleBridgeIPTextChange();
                    }
                }
                return false; // pass on to other listeners.
            }
        });
        bridgeModeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.toString().contains("\n")) {
                    // the user is done typing.
                    // remove new line characcter
                    final String currentText = bridgeModeEditText.getText().toString();
                    bridgeModeEditText.setText(currentText.substring(0, currentText.indexOf('\n')));
                    handleBridgeIPTextChange();
                }
            }
        });

        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_ATTACHED));
        checkAndRequestPermissions();
    }

    private final BroadcastReceiver mUsbDeviceReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                        DJISDKManager.getInstance().startConnectionToProduct();
                        Toast.makeText(getApplicationContext(),"SDK registration failed, check network and retry!",Toast.LENGTH_LONG).show();

                    }else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                    }
                }
            };


    public boolean isConnected() {
        Intent intent = registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        //Intent intent = registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        return intent.getExtras().getBoolean("connected");
    }

    public class  NetworkTask extends AsyncTask<Void, Void, String>{

        private String url;
        private JsonObject values;

        public NetworkTask(String url, JsonObject values){
            this.url = url;
            this.values = values;
        }

        @Override
        protected String doInBackground(Void... params) {
            String result;  //요청 결과를 저장할 변수
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, values);//얻어오는 코드

            return result;
        }
        @Override
        protected void onPostExecute(String s){
            super.onPostExecute(s);
              if(s==null) {
                  Toast.makeText(getApplicationContext(), "참여할 회의가 없습니다.", Toast.LENGTH_LONG).show();
                  return;
              }
            try {
                JSONObject jsonObject = new JSONObject(s);
                Userdata.getInstance()._code = jsonObject.getInt("code");
                Userdata.getInstance()._message = jsonObject.getString("message");

                JSONObject jsonObject1 = new JSONObject(jsonObject.getString("data"));
                Userdata.getInstance()._room_key = jsonObject1.getString("room_key");
                Userdata.getInstance()._caller_id = jsonObject1.getString("caller_id");
                Userdata.getInstance()._room_id = jsonObject1.getString("room_id");
                Userdata.getInstance()._title = jsonObject1.getString("title");
                Userdata.getInstance()._reservation_yn = jsonObject1.getString("reservation_yn");
                Userdata.getInstance()._reservation_date = jsonObject1.getString("reservation_date");
                Userdata.getInstance()._reservation_time = jsonObject1.getString("reservation_time");
                Userdata.getInstance()._status = jsonObject1.getString("status");
                Userdata.getInstance()._reg_date = jsonObject1.getString("reg_date");
                Userdata.getInstance()._start_date = jsonObject1.getString("start_date");
                Userdata.getInstance()._name = jsonObject1.getString("name");
                //Userdata.getInstance()._profile_photo = jsonObject1.getString("profile_photo");

                if(Userdata.getInstance()._message !="success" || Userdata.getInstance()._room_key != "" ) {
                    Intent intent = new Intent(MainActivity.this, Webrtc1.class);
                    startActivity(intent);
                }
                else{
                    LOG.append("에러 발생! 관리자에게 문의하세요. , ");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEND_LOG:
                    //stateText.setText(Integer.toString(msg.arg1) + msg.obj+"\n");
                    //stateText.setText((Integer) msg.obj);
                    stateText.setText(msg.obj.toString());
                    break;
                case SEND_VIS:
                    //디버그
                    if(bool_onRegister && bool_onProductConnect) {
                    //if(bool_onRegister) {
                        //Intent intent = new Intent(MainActivity.this, Webrtc1.class);
                        //startActivity(intent);
                        //btn_map_widget.setVisibility(View.VISIBLE);
                        view_userInfo.setVisibility(View.VISIBLE);
                        view_droneimg.setVisibility(View.VISIBLE);
                        view_inform.setVisibility(View.INVISIBLE);
                    }
                    else{
                        view_userInfo.setVisibility(View.INVISIBLE);
                        view_droneimg.setVisibility(View.INVISIBLE);
                        view_inform.setVisibility(View.VISIBLE);
                    }
                    break;

                default:
                    break;
            }
        }

    };


    class mThread extends Thread {
        int i = 0;
        @Override
        public void run() {
            super.run();
            while (true) {
                //조건 추가해서 덜 돌게하기
                //버튼 보이게 하기
                Message message_vis = handler.obtainMessage();
                message_vis.what = SEND_VIS;
                handler.sendMessage(message_vis);

                if(!checkUsbConnection){
                    checkUsbConnection = isConnected();
                    if(!bool_onProductConnect && checkUsbConnection) {
                        DJISDKManager.getInstance().destroy();
                        startSDKRegistration();
                    }
                }

                //StringBuilder logmsg = new StringBuilder();

                //로그 메세지
                Message message_log = handler.obtainMessage();
                message_log.what = SEND_LOG;
                //if로 조건하면서 한번에 ~~~~
                //누적 settext
                /*if(bool_onRegister) {
                    logmsg.append("SDK 등록 성공" + "\n");
                }
                else {
                    logmsg.append("SDK 등록 실패, 네트워크 확인 후 재시도 요망" + "\n");
                }*/


                message_log.obj = LOG;
                handler.sendMessage(message_log);

                /*i++;
                Message message = handler.obtainMessage();
                message.what = SEND_TIME;
                // 메시지 내용 설정 (int)
                //String ifRegi = new String(String.valueOf(bool_onRegister));
                //message.obj = ifRegi;
                message.arg1 = i;
                // 메시지 내용 설정 (Object)
                String information = new String("초 째 Thread 동작 중입니다.");
                message.obj = information;

                handler.sendMessage(message);
                */

                try {
                    // 1초 씩 딜레이 부여
                    sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    protected void onDestroy() {
        DJISDKManager.getInstance().destroy();
        isAppStarted = false;
        super.onDestroy();
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
            LOG.append("의심-missingPermission");
        } else {
            //Toast.makeText(getApplicationContext(), "Missing permissions! Will not register SDK to connect to aircraft.", Toast.LENGTH_LONG).show();
            LOG.append("SDK와 드론이 연결되지 않음, ");
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    DJISDKManager.getInstance().registerApp(MainActivity.this, registrationCallback);
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        Class nextActivityClass = null;

        int id = view.getId();
        if (id == R.id.complete_ui_widgets) {
            nextActivityClass = CompleteWidgetActivity.class;
        } else if (id == R.id.bt_customized_ui_widgets) {
            nextActivityClass = CustomizedWidgetsActivity.class;
        }
        else {
        //nextActivityClass = Webrtc1.class;

        /*
        PopupMenu popup = new PopupMenu(this, view);
        popup.setOnMenuItemClickListener(this);
        Menu popupMenu = popup.getMenu();
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.map_select_menu, popupMenu);
        popupMenu.findItem(R.id.here_map).setEnabled(isHereMapsSupported());
        popupMenu.findItem(R.id.google_map).setEnabled(isGoogleMapsSupported(this));
        popup.show();
        return;
        */


        }

        Intent intent = new Intent(this, nextActivityClass);
        startActivity(intent);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        Intent intent = new Intent(this, MapWidgetActivity.class);
        int mapBrand = 0;
        switch (menuItem.getItemId()) {
            case R.id.here_map:
                mapBrand = 0;
                break;
            case R.id.google_map:
                mapBrand = 1;
                break;
            case R.id.amap:
                mapBrand = 2;
                break;
            case R.id.mapbox:
                mapBrand = 3;
                break;
        }
        intent.putExtra(MapWidgetActivity.MAP_PROVIDER, mapBrand);
        startActivity(intent);
        return false;
    }

    public static boolean isHereMapsSupported() {
        String abi;


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            abi = Build.CPU_ABI;
        } else {
            abi = Build.SUPPORTED_ABIS[0];
        }
        DJILog.d(TAG, "abi=" + abi);

        //The possible values are armeabi, armeabi-v7a, arm64-v8a, x86, x86_64, mips, mips64.
        return abi.contains("arm");
    }

    public static boolean isGoogleMapsSupported(Context context) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }

    private void handleBridgeIPTextChange() {
        // the user is done typing.
        final String bridgeIP = bridgeModeEditText.getText().toString();

        if (!TextUtils.isEmpty(bridgeIP)) {
            DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP(bridgeIP);
            //Toast.makeText(getApplicationContext(),"BridgeMode ON!\nIP: " + bridgeIP,Toast.LENGTH_SHORT).show();
            LOG.append("bridge 모드 ON, ");
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString(LAST_USED_BRIDGE_IP,bridgeIP).apply();
        }
    }
}
