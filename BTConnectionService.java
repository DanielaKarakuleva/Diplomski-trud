package com.example.robome;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.wowwee.robome.RoboMeCommands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.logging.Handler;

public class BTConnectionService {
    private static final String TAG = "BluetoothConnectionServ";

    private static final String appName = "robome";


    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");



    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    public BTConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    // This thread runs while listening for incoming connections. It behaves like
    // a server-side client. It runs until a connection is accepted or canceled.
    private class AcceptThread extends Thread {

        // Local server socket
        private  BluetoothServerSocket mmServerSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // create a new listening server socket
            try {

                //checkBTPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT);

                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread: Setting up server using: " + MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOEXception " + e.getMessage());
            }

            mmServerSocket = tmp;

        }

        public void run() {
            Log.d(TAG, "run: AcceptThread Running.");
            BluetoothSocket socket = null;

            while (true) {
                try {
                    Log.d(TAG, "run: RFCOM server socket start.");
                    socket = mmServerSocket.accept();
                    Log.d(TAG, "run: RFCOM server socket accepted connection.");
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: IOEXception " + e.getMessage());
                    break;
                }

                if (socket != null) {
                    connected(socket, mmDevice);
                    /**  try {
                     mmServerSocket.close();}
                     catch (IOException e) {
                     Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());

                     } **/
                    break;
                }
            }
            Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }


    }

    // This thread runs while attempting to make an outgoing connection with a device.
// It runs straight through - the connection either succeeds or fails.
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }



        @SuppressLint("MissingPermission")
        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread.");

            // connect(mmDevice);

            try {

                Log.d(TAG, "ConnectThread: Trying to create InsecureRFcommSocket using UUID");

                //checkBTPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);

            } catch (Exception e) {
                Log.e(TAG, "ConnectThread: Could not create Insecurerfcommsocket" + e.getMessage());
            }
            mmSocket = tmp;
            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
                // Log.d(TAG, "run: Closed Socket.");
            } catch (IOException e) {
                //close the socket
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: CLosed socket.");
                    try {
                        Log.e("","trying fallback...");

                        mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class}).invoke(mmDevice,1);
                        mmSocket.connect();

                        Log.e("","Connected");
                    }
                    catch (Exception e1) {Log.e(TAG, "FAILED AGAIN");}

                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThreadL run: Unable to close connetion in socket" + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: could not connect to UUID " + MY_UUID_INSECURE);
            }

            connected(mmSocket, mmDevice);


        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close() of mmSocket in ConnectThread failed. " + e.getMessage());
            }
        }
    }



    // start AcceptThread to begin a session in listening (server) mode. Called by the activity onResume()
    public synchronized void start() {
        Log.d(TAG, "start");

        //Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }


    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");

        // initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please Wait...", true);
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConectedThread: starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                mProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    Intent incomingCommandIntent = new Intent("incomingCommand");
                    incomingCommandIntent.putExtra("theCommand", incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingCommandIntent);



                } catch (IOException e) {
                    Log.e(TAG, "write: error reading inputstream" + e.getMessage());
                    break;
                }
            }
        }

        // public void write(RoboMeCommands.RobotCommand command) {
        public void write(byte[] bytes) {

            String text = new String(bytes, Charset.defaultCharset());
            // String bytes = command.toString();
            Log.d(TAG, "write: writing to outputstream " + bytes);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: error writing to outputstream");

            }
        }

        public void cancel() {
            try {
                mmInStream.close();
                mmOutStream.close();
                Thread.sleep(1000);
                mmSocket.close();

            } catch (IOException e) {

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG, "connected: starting.");

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out) {

        ConnectedThread r;
        Log.d(TAG, "write: write called.");

        mConnectedThread.write(out);
    }



}
