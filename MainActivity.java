package com.example.robome;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.wowwee.robome.*;
import com.wowwee.robome.RoboMeCommands.*;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.widget.Toast;
//import android.support.v7.app.AppCompatActivity;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements RoboMe.RoboMeListener, PopupMenu.OnMenuItemClickListener, AdapterView.OnItemClickListener {


    private RoboMe robome;
    // Button btnBT;

    BluetoothAdapter mBluetoothAdapter;

    BTConnectionService mBTConnection;
    // static final String PBAP_UUID = "0000112f-0000-1000-8000-00805f9b34fb";

    // private static final UUID MY_UUID_INSECURE = UUID.fromString("d14a9f18-9d72-11ed-a8fc-0242ac120002");

    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    //   ParcelUuid.fromString(PBAP_UUID).getUuid();
    //   UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");


    BluetoothDevice mBTDevice;

    TextView incomingMessage;

    Button btnBT, btnForward, btnBackward, btnLeft, btnRight;
    Button btnSpeed, btnHeadUp, btnHeadDown;
    Button btnEdge, btnChest;
    Button winkLeft, winkRight;
    Button testBtn;
    EditText tekst;
    ImageButton btnR1, btnR2, btnR3, btnR4, btnR5, btnR6;
    boolean chestValue, edgeValue;


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

                Log.d(TAG, "nReceive: " + device.getName() + " Address: " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
                Log.d(TAG, "**********************************************************DISCOVERY FINISHED!!!: ");
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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        robome = new RoboMe(this, this);

        setContentView(R.layout.activity_main);


        btnBT = (Button) findViewById(R.id.btnConnect);
        btnForward = (Button) findViewById(R.id.forward);
        btnBackward = (Button) findViewById(R.id.backward);
        btnLeft = (Button) findViewById(R.id.left);
        btnRight = (Button) findViewById(R.id.right);
        btnEdge = (Button) findViewById(R.id.edge);
        btnChest = (Button) findViewById(R.id.chest);
        btnHeadUp = (Button) findViewById(R.id.headUp);
        btnHeadDown = (Button) findViewById(R.id.headDown);
        btnSpeed = (Button) findViewById(R.id.speed);
        winkLeft = findViewById(R.id.winkLeft);
        winkRight = findViewById(R.id.winkRight);
        btnR1 = findViewById(R.id.buttonR1);
        btnR2 = findViewById(R.id.buttonR2);
        btnR3 = findViewById(R.id.buttonR3);
        btnR4 = findViewById(R.id.buttonR4);
        btnR5 = findViewById(R.id.buttonR5);
        btnR6 = findViewById(R.id.buttonR6);
        //btnR7 = findViewById(R.id.buttonR7);
        //btnR8 = findViewById(R.id.buttonR8);
        //btnR9 = findViewById(R.id.buttonR9);
        //btnR10 = findViewById(R.id.buttonR10);
        testBtn = findViewById(R.id.buttonSend);
        tekst = findViewById(R.id.editText);


        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String command = tekst.getText().toString();
                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset())); }
                catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

        // voice commands send
        btnR1.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerF;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerF != null) return true;
                        mHandlerF = new Handler();
                        mHandlerF.post(mActionF);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerF == null) return true;
                        mHandlerF.removeCallbacks(mActionF);
                        mHandlerF = null;
                        break;
                }
                return false;
            }

            Runnable mActionF = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "replikaeden";
                    // String command = RobotCommand.kRobot_MoveForwardSpeed5.getCommandAsHexString();
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerF.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };
        });

        btnR2.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerF;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerF != null) return true;
                        mHandlerF = new Handler();
                        mHandlerF.post(mActionF);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerF == null) return true;
                        mHandlerF.removeCallbacks(mActionF);
                        mHandlerF = null;
                        break;
                }
                return false;
            }

            Runnable mActionF = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "replikadva";
                    // String command = RobotCommand.kRobot_MoveForwardSpeed5.getCommandAsHexString();
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerF.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };
        });

        btnR3.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerF;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerF != null) return true;
                        mHandlerF = new Handler();
                        mHandlerF.post(mActionF);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerF == null) return true;
                        mHandlerF.removeCallbacks(mActionF);
                        mHandlerF = null;
                        break;
                }
                return false;
            }

            Runnable mActionF = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "replikatri";
                    // String command = RobotCommand.kRobot_MoveForwardSpeed5.getCommandAsHexString();
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerF.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };
        });

        btnR4.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerF;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerF != null) return true;
                        mHandlerF = new Handler();
                        mHandlerF.post(mActionF);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerF == null) return true;
                        mHandlerF.removeCallbacks(mActionF);
                        mHandlerF = null;
                        break;
                }
                return false;
            }

            Runnable mActionF = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "replikachetiri";
                    // String command = RobotCommand.kRobot_MoveForwardSpeed5.getCommandAsHexString();
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerF.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };
        });

        btnR5.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerF;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerF != null) return true;
                        mHandlerF = new Handler();
                        mHandlerF.post(mActionF);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerF == null) return true;
                        mHandlerF.removeCallbacks(mActionF);
                        mHandlerF = null;
                        break;
                }
                return false;
            }

            Runnable mActionF = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "replikapet";
                    // String command = RobotCommand.kRobot_MoveForwardSpeed5.getCommandAsHexString();
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerF.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };
        });

        btnR6.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerF;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerF != null) return true;
                        mHandlerF = new Handler();
                        mHandlerF.post(mActionF);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerF == null) return true;
                        mHandlerF.removeCallbacks(mActionF);
                        mHandlerF = null;
                        break;
                }
                return false;
            }

            Runnable mActionF = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "replikashest";
                    // String command = RobotCommand.kRobot_MoveForwardSpeed5.getCommandAsHexString();
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerF.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };
        });







        chestValue = false;
        edgeValue = false; // edge and chest detection is off in the beginning





        // menu
        btnBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                popup.setOnMenuItemClickListener(MainActivity.this);
                popup.inflate(R.menu.connect_menu);
                popup.show();
            }
        });

        // speed menu
        btnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                popup.setOnMenuItemClickListener(MainActivity.this);
                popup.inflate(R.menu.speed_menu);
                popup.show();
            }
        });

        // movement commands
        btnForward.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerF;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerF != null) return true;
                        mHandlerF = new Handler();
                        mHandlerF.post(mActionF);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerF == null) return true;
                        mHandlerF.removeCallbacks(mActionF);
                        mHandlerF = null;
                        break;
                }
                return false;
            }

            Runnable mActionF = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "forward";
                    // String command = RobotCommand.kRobot_MoveForwardSpeed5.getCommandAsHexString();
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerF.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };

        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = "forward";
                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset())); }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });



        btnBackward.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerB;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerB != null) return true;
                        mHandlerB = new Handler();
                        mHandlerB.post(mActionB);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerB == null) return true;
                        mHandlerB.removeCallbacks(mActionB);
                        mHandlerB = null;
                        break;
                }
                return false;
            }

            Runnable mActionB = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "backward";
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerB.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };

        });

        btnBackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = "backward";
                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        btnLeft.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerL;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerL != null) return true;
                        mHandlerL = new Handler();
                        mHandlerL.post(mActionL);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerL == null) return true;
                        mHandlerL.removeCallbacks(mActionL);
                        mHandlerL = null;
                        break;
                }
                return false;
            }

            Runnable mActionL = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "left";
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerL.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };

        });

        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = "left";
                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                } catch (Exception e) {e.printStackTrace(); }
            }
        });


        btnRight.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerR;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerR != null) return true;
                        mHandlerR = new Handler();
                        mHandlerR.post(mActionR);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerR== null) return true;
                        mHandlerR.removeCallbacks(mActionR);
                        mHandlerR = null;
                        break;
                }
                return false;
            }

            Runnable mActionR = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "right";
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerR.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };

        });

        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = "right";
                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        // head commands
        btnHeadUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = "headup";
                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                } catch (Exception e) {e.printStackTrace(); }
            }
        });

        btnHeadDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = "headdown";
                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                } catch (Exception e) {e.printStackTrace(); }            }
        });

        // chest and edge detection commands
        btnEdge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command;
                if (edgeValue) {
                    command = "edgeoff";
                    edgeValue = false; }
                else {
                    command = "edgeon";
                    edgeValue = true;
                }
                // String command = "edge";

                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                } catch (Exception e) {e.printStackTrace(); }            }
        });

        btnChest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command;
                if (chestValue) {
                    command = "chestoff";
                    chestValue = false; }
                else {
                    command = "cheston";
                    chestValue = true;
                }
                //  String command = "chest";

                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                } catch (Exception e) {e.printStackTrace(); }            }
        });

        // eye commands
        winkLeft.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerF;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerF != null) return true;
                        mHandlerF = new Handler();
                        mHandlerF.post(mActionF);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerF == null) return true;
                        mHandlerF.removeCallbacks(mActionF);
                        mHandlerF = null;
                        break;
                }
                return false;
            }

            Runnable mActionF = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "winkl";
                    // String command = RobotCommand.kRobot_MoveForwardSpeed5.getCommandAsHexString();
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerF.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };
        });

        winkLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = "winkl";
                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                } catch (Exception e) {e.printStackTrace(); }

            }
        });

        winkRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = "winkr";
                try {
                    mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                } catch (Exception e) {e.printStackTrace(); }

            }
        });

        winkRight.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandlerF;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandlerF != null) return true;
                        mHandlerF = new Handler();
                        mHandlerF.post(mActionF);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandlerF == null) return true;
                        mHandlerF.removeCallbacks(mActionF);
                        mHandlerF = null;
                        break;
                }
                return false;
            }

            Runnable mActionF = new Runnable() {
                @Override public void run() {
                    Log.d(TAG,"Performing action...");
                    String command = "winkr";
                    // String command = RobotCommand.kRobot_MoveForwardSpeed5.getCommandAsHexString();
                    try {
                        mBTConnection.write(command.getBytes(Charset.defaultCharset()));
                        mHandlerF.post(this); }
                    catch (Exception e) { e.printStackTrace(); }
                    //mHandler.postDelayed(this, 500);
                }
            };
        });

        robome.setVolume(12);

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

            incomingMessage.setText(text);


        }
    };

    @Override
    public void commandReceived(IncomingRobotCommand incomingRobotCommand) {
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

            case R.id.speed1:
                setSpeed("1");
                return true;

            case R.id.speed2:
                setSpeed("2");
                return true;

            case R.id.speed3:
                setSpeed("3");
                return true;

            case R.id.speed4:
                setSpeed("4");
                return true;

            case R.id.speed5:
                setSpeed("5");
                return true;

            default:
                Log.d(TAG, "LOG return FALSE");

                return false;

        }
    }

    public void setSpeed(String speed) {
        try {
            mBTConnection.write(speed.getBytes(Charset.defaultCharset()));
        } catch (Exception e) {e.printStackTrace(); }
    }

    public void startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    @SuppressLint("MissingPermission")
    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");

        if (device != null) {
            try {
                mBTConnection.startClient(device, uuid);
                Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {e.printStackTrace(); }
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
                Log.d(TAG, "*******************************************ENABLING PERMISSION");
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
            discoverDevicesIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if (!mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            discoverDevicesIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
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

