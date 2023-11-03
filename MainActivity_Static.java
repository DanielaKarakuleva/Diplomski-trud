package com.example.robome;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.wowwee.robome.IRStatus;
import com.wowwee.robome.RoboMe;
import com.wowwee.robome.RoboMeCommands;
import com.wowwee.robome.SensorStatus;

import java.util.ArrayList;
import java.util.UUID;
import java.util.Locale;

//import android.support.v7.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity implements RoboMe.RoboMeListener, PopupMenu.OnMenuItemClickListener, AdapterView.OnItemClickListener {


    private RoboMe robome;
    // Button btnBT;

    BluetoothAdapter mBluetoothAdapter;

    BTConnectionService mBTConnection;
    ProgressBar mouth;
    IRStatus irStatus;
    SensorStatus sensorStatus;
    ImageView eyeLeft;
    ImageView eyeRight;
    ImageView eyebrowLeft, eyebrowRight;
    ObjectAnimator downAnimatorL, downAnimatorR;
    ObjectAnimator upAnimatorL, upAnimatorR;
    AnimatorSet animatorSet;
    MediaPlayer wink;
    TextToSpeech ttsGenerator;
    //static final String PBAP_UUID = "0000112f-0000-1000-8000-00805f9b34fb";



    //private static final UUID MY_UUID_INSECURE = UUID.fromString("d14a9f18-9d72-11ed-a8fc-0242ac120002");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");



    // ParcelUuid.fromString(PBAP_UUID).getUuid();
    //   UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    //  UUID.fromString("12d09990-96ae-11ed-87cd-0800200c9a66");


    BluetoothDevice mBTDevice;

    TextView incomingMessage;

    Button btnBT;

    int speed;


    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    private ListView lvNewDevices;
    private TextView tvTitle;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private Button btnBack;


    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    // BroadcastReceiver for Discoverability changes
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, mBluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability disabled. Still able to receive connections");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability disabled.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };

    // Broadcast receiver for listing devices that are not paired.
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        //  @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                checkBTPermission(context, Manifest.permission.BLUETOOTH_CONNECT);

                Log.d(TAG, "********************************************************** onReceive: " + device.getName() + " Address: " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                checkBTPermission(context, Manifest.permission.BLUETOOTH_CONNECT);


                // case 1: already bonded
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED");
                    mBTDevice = mDevice;
                }
                // case 2: creating a bond
                if (mDevice.getBondState()==BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING");
                }
                // case 3: breaking a bond
                if (mDevice.getBondState()==BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE");
                }

            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
    }



    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        robome = new RoboMe(this, this);
        speed = 5;

        wink = MediaPlayer.create(this,R.raw.wink);
        setContentView(R.layout.activity_main);
        mouth = findViewById(R.id.Mouth);
        eyeLeft = findViewById(R.id.eyeLeft);
        eyeRight = findViewById(R.id.eyeRight);

        ttsGenerator = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i != TextToSpeech.ERROR)
                {
                    ttsGenerator.setLanguage(new Locale("mk"));
                }
            }
        });


        btnBT = (Button)findViewById(R.id.btnConnect);
        btnBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d(TAG, "LOG 1");
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                Log.d(TAG, "LOG 2");

                popup.setOnMenuItemClickListener(MainActivity.this);
                Log.d(TAG, "LOG 3");

                popup.inflate(R.menu.connect_menu);
                Log.d(TAG, "LOG 4");

                popup.show();

            }
        });

        robome.setVolume(12);
        robome.sendCommand(RoboMeCommands.RobotCommand.kRobot_ChestDetectOff);
        robome.sendCommand(RoboMeCommands.RobotCommand.kRobot_EdgeDetectOff);


        mBTDevices = new ArrayList<>();
        //mBTConnection = new BTConnectionService(this);


        // Broadcast when band state changes (pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);


        //lvNewDevices.setOnItemClickListener(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingCommand"));

        //  System.out.println(robome.getVolume());
        // System.out.println(robome.isRoboMeConnected());
        // System.out.println(robome.isHeadsetPluggedIn());
        //robome.startListening();

    }


    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theCommand");


            // speed change
            if (text.contains("1"))
            {
                String speed1 = "Брзина на движење - еден. Ова е мојата најголема брзина на движење.";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(speed1, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }

            if (text.contains("2"))
            {
                String speed2 = "Брзина на движење - два. Но можам да се движам побрзо и поспоро.";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(speed2, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }

            if (text.contains("3"))
            {
                String speed3 = "Брзина на движење - три. Но можам да се движам побрзо и поспоро.";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(speed3, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }

            if (text.contains("4"))
            {
                String speed4 = "Брзина на движење - четири. Но можам да се движам побрзо и поспоро.";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(speed4, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }

            if (text.contains("5"))
            {
                speed = 5;
                String speed5 = "Брзина на движење - пет. Ова е моето најспоро движење";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(speed5, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }


            // movement
            if (text.contains("forward")) {
                if (!robome.isSendingCommand())
                    robome.sendCommand(RoboMeCommands.RobotCommand.valueOf("kRobot_MoveForwardSpeed" + speed));
            }
            if (text.contains("left")) {
                if (!robome.isSendingCommand())
                    robome.sendCommand(RoboMeCommands.RobotCommand.valueOf("kRobot_TurnLeftSpeed" + speed));
            }
            if (text.contains("right")) {
                if (!robome.isSendingCommand())
                    robome.sendCommand(RoboMeCommands.RobotCommand.valueOf("kRobot_TurnRightSpeed" + speed));
            }

            if (text.contains("backward")) {
                if (!robome.isSendingCommand())
                    robome.sendCommand(RoboMeCommands.RobotCommand.valueOf("kRobot_MoveBackwardSpeed" + speed));
            }

            // head tilts
            if (text.contains("headup")) {
                if (!robome.isSendingCommand())
                    robome.sendCommand(RoboMeCommands.RobotCommand.kRobot_HeadTiltAllUp);
            }

            if (text.contains("headdown")) {
                if (!robome.isSendingCommand())
                    robome.sendCommand(RoboMeCommands.RobotCommand.kRobot_HeadTiltAllDown);
            }

            // chest and edge detection

            if (text.contains("chestoff")) {
                robome.sendCommand(RoboMeCommands.RobotCommand.kRobot_ChestDetectOff);
                String replika2 = "Сензор за препреки деактивиран.";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(replika2, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }
            if (text.contains("cheston")) {
                robome.sendCommand(RoboMeCommands.RobotCommand.kRobot_ChestDetectOn);
                String replika2 = "Сензор за препреки активиран";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(replika2, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });

            }
            if (text.contains("edgeoff")) {
                robome.sendCommand(RoboMeCommands.RobotCommand.kRobot_EdgeDetectOff);
                String replika2 = "Сензор за рабови - деактивиран";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(replika2, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }
            if (text.contains("edgeon")) {
                robome.sendCommand(RoboMeCommands.RobotCommand.kRobot_EdgeDetectOn);
                String replika2 = "Сензор за рабови - активиран.";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(replika2, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }



            //speaking commands

            if(text.contains("replikaeden"))
            {
                String replika1 = "Здраво на сите. Дозволете ми да се претставам. Јас сум Роби, комерцијален робот развиен од студентите од смерот КХИЕ.";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(replika1, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });

            }

            if(text.contains("replikadva"))
            {
                String replika2 = "Дали сакате да дознаете нешто повеќе за мене?";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(replika2, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }

            if(text.contains("replikatri"))
            {
                String replika3 = "Супер, ова е мојата приказна се надевам ќе ви се допанде.";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(replika3, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }

            if(text.contains("replikachetiri"))
            {
                String replika4 = "Јас постојам поради ентузијастичниот тим од студенти кои внесоа живот во мене. Долго време седев речиси заборавен во институтот за електроника. Изминатава есен тоа се промени.";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(replika4, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }

            if(text.contains("replikapet"))
            {
                String replika5 = "После многу труд, најпрво научив да мрдам со својата глава, а потоа да се движам со најразлични брзини. Благодарение на моите вградени сензори можам да детектирам препреки и рабови за да не се повредам. Дали сакате да ги погледнете моите способности?";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(replika5, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }

            if(text.contains("replikashest"))
            {
                String replika6 = "Неодамна почнав да зборувам и на македонски! Беше навистина напорно, но никогаш не се откажав, додека не ги кажав своите први зборови. Нестрпелив сум да дознаам за мојот понатамошен развој. Останувам во рацете на љубопитните студенти, инженери, коишто ќе се грижат за мене и ќе ме дооспособуваат. ";
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(replika6, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });
            }

            if(text.startsWith("@"))
            {
                String command = text.substring(1);
                mouth.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGenerator.speak(command, TextToSpeech.QUEUE_FLUSH, null, "speech");
                }
                ttsGenerator.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    private  int start, end;
                    private  long startTime;

                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        if(s.equals("speech"))
                        {
                            mouth.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(String s) {

                    }

                    @Override
                    public  void  onRangeStart(String s, int start, int end, int frame)
                    {
                        this.start = start;
                        this.end = end;
                        this.startTime = System.currentTimeMillis();
                        mouth.setIndeterminate(false);
                        mouth.setMax(end - start);
                        mouth.setProgress(0);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        float speechRate = sp.getFloat("speech_rate0", 1.0f);
                        int duration = (int) ((end - start) / speechRate) * 1000;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mouth, "progress", 0, end - start);
                        animation.setDuration(duration);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }
                });

            }



            // eyes commands
            if(text.contains("winkl"))
            {
                wink.start();
                eyeLeft.animate().alpha(0.0f).setDuration(250).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        eyeLeft.setVisibility((View.INVISIBLE));
                        eyeLeft.animate().alpha(1.0f).setDuration(250).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                eyeLeft.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }

            if(text.contains("winkr"))
            {
                wink.start();
                eyeRight.animate().alpha(0.0f).setDuration(250).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        eyeRight.setVisibility((View.INVISIBLE));
                        eyeRight.animate().alpha(1.0f).setDuration(250).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                eyeRight.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }






            /**
             if (text.contains("chest")) {
             IRStatus status = IncomingRobotCommand.kRobotIncoming_IRStatus.readIRStatus();
             Log.d(TAG, "CHEST = " + status.chestDetection);
             Log.d(TAG, "EDGE = " + status.edgeDetection);

             if (status.chestDetection)
             robome.sendCommand(RobotCommand.kRobot_ChestDetectOff);
             else
             robome.sendCommand(RobotCommand.kRobot_ChestDetectOn);
             }
             if (text.contains("edge")) {
             IRStatus status = IncomingRobotCommand.kRobotIncoming_IRStatus.readIRStatus();
             if (status.edgeDetection)
             robome.sendCommand(RobotCommand.kRobot_EdgeDetectOff);
             else
             robome.sendCommand(RobotCommand.kRobot_EdgeDetectOn);
             }
             **/

            // robome.stopSending();

        }


    };




    @Override
    public void commandReceived(RoboMeCommands.IncomingRobotCommand incomingRobotCommand) {
        Log.d(TAG, "CommandReceived: command number " + incomingRobotCommand.getCommand());

    }

    @Override
    public void roboMeConnected() {
        Log.d(TAG, "RoboMe connected.");
    }

    @Override
    public void roboMeDisconnected() {
        Log.d(TAG, "RoboMe disconnected.");

    }

    @Override
    public void headsetPluggedIn() {
        Log.d(TAG, "RoboMe headset plugged in.");

    }

    @Override
    public void headsetUnplugged() {
        Log.d(TAG, "RoboMe headset unplugged.");

    }

    @Override
    public void volumeChanged(float v) {
        Log.d(TAG, "Volume changed to " + v);

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Log.d(TAG, "MENUITEMCLICK");
        switch (item.getItemId()) {

            case R.id.btnONOFF:
                enableDisableBT();
                return true;

            case R.id.btnDiscoverable:
                Log.d(TAG, "Making device discoverable for 300 seconds");
                mBTConnection = new BTConnectionService(this);

                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                checkBTPermission(this,Manifest.permission.BLUETOOTH_ADVERTISE);
                startActivity(discoverableIntent);

                IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                registerReceiver(mBroadcastReceiver2, intentFilter);
                return true;
            case R.id.btnDiscover:

                createNewDialog();

                return true;

            case R.id.btnStartConnection:
                startConnection();
                return true;

            default:
                Log.d(TAG, "connect context menu error");
                return false;

        }
    }

    public void startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");

        if (device != null) {
            try {
                mBTConnection.startClient(device, uuid);
            }
            catch (Exception e) { e.printStackTrace(); }
        }

    }


    //  @SuppressLint("MissingPermission")
    private void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "This device does not have Bluetooth");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            checkBTPermission(this, Manifest.permission.BLUETOOTH_CONNECT);
            startActivity(enableBTIntent);
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);

        }

    }

    private void checkBTPermission(Context context, String permission) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            int version = Build.VERSION_CODES.S;
            if (permission==Manifest.permission.ACCESS_COARSE_LOCATION || permission==Manifest.permission.ACCESS_FINE_LOCATION)
                version = Build.VERSION_CODES.LOLLIPOP;

            if (Build.VERSION.SDK_INT >= version) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, 2);
                return;
            }
        }
    }


    public void createNewDialog() {
        dialogBuilder = new AlertDialog.Builder(this);
        final View popupView = getLayoutInflater().inflate(R.layout.popupdialog, null);

        lvNewDevices = (ListView) popupView.findViewById(R.id.lvNewDevices);
        lvNewDevices.setOnItemClickListener(this);

        tvTitle = (TextView)popupView.findViewById(R.id.tvTitle);

        btnBack = (Button) popupView.findViewById(R.id.btnBack);

        dialogBuilder.setView(popupView);
        dialog = dialogBuilder.create();
        dialog.show();

        Log.d(TAG, "Looking for unpaired devices ...");

        checkBTPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        checkBTPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "Canceling discovery...");

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if (!mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }


        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        checkBTPermission(this, Manifest.permission.BLUETOOTH_SCAN);
        mBluetoothAdapter.cancelDiscovery();

        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = "+deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = "+deviceAddress);

        // creating bond - REQUIRES API 17+
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with "+ deviceName);
            mBTDevices.get(i).createBond();

            mBTDevice = mBTDevices.get(i);
            mBTConnection = new BTConnectionService(this);
            Toast.makeText(this, "Paired", Toast.LENGTH_SHORT).show();


        }



    }
}

