package dcc_ex.ex_toolbox.comms;


import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dcc_ex.ex_toolbox.R;
import dcc_ex.ex_toolbox.threaded_application;
import dcc_ex.ex_toolbox.type.message_type;

class SocketUsb extends Thread {
    InetAddress host_address;
//    Socket clientSocket = null;
//    BufferedReader inputBR = null;
    PrintWriter outputPW = null;
    UsbSerialPort port;

    private volatile boolean endRead = false;           //signals rcvr to terminate
    private volatile boolean socketGood = false;        //indicates socket condition
    private volatile boolean inboundTimeout = false;    //indicates inbound messages are not arriving from WiT
    private boolean firstConnect = false;               //indicates initial socket connection was achieved
    private int connectTimeoutMs = 3000; //connection timeout in milliseconds
    private int socketTimeoutMs = 500; //socket timeout in milliseconds

    private final int MAX_INBOUND_TIMEOUT_RETRIES = 2;
    private int inboundTimeoutRetryCount = 0;           // number of consecutive inbound timeouts
    boolean inboundTimeoutRecovery = false;     // attempting to force WiT to respond

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    static SharedPreferences prefs;
    static threaded_application mainapp;
    static comm_thread commThread;
    Context context;

     SocketUsb(threaded_application mainapp, SharedPreferences prefs, comm_thread commThread, Context myContext) {
        super("socketUsb");

        SocketUsb.prefs = prefs;
        SocketUsb.mainapp = mainapp;
        SocketUsb.commThread = commThread;
        context = myContext;
    }

    public boolean connect() {

        //use local socketOk instead of setting socketGood so that the rcvr doesn't resume until connect() is done
//        boolean socketOk = HaveNetworkConnection();
        boolean socketOk = true;
        endRead = false;

        connectTimeoutMs = Integer.parseInt(prefs.getString("prefConnectTimeoutMs", mainapp.getResources().getString(R.string.prefConnectTimeoutMsDefaultValue)));
        socketTimeoutMs = Integer.parseInt(prefs.getString("prefSocketTimeoutMs", mainapp.getResources().getString(R.string.prefSocketTimeoutMsDefaultValue)));

        //socket
        if (socketOk) {
            try {
                mainapp.isUSB = true;

                Log.d("EX_Toolbox", "SocketUsb.connect(): List USB");
                UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
                Log.d("EX_Toolbox", "SocketUsb.connect(): USB Ports: " + availableDrivers.size());

                Log.d("EX_Toolbox", "SocketUsb.connect(): get available drivers");
                UsbSerialDriver driver = availableDrivers.get(0);
                Log.d("EX_Toolbox", "SocketUsb.connect(): driver(0)" + driver );

                if (availableDrivers.isEmpty()) {
                    throw new IOException("No USB serial devices found");
                }

                UsbDevice device = driver.getDevice();

                if (!manager.hasPermission(device)) {
                    // You need to request permission.
                    // This usually involves a BroadcastReceiver to listen for the user's response.
                    // For a quick test, you can see if it's already granted or if you need to trigger the UI.
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent("com.android.example.USB_PERMISSION"), PendingIntent.FLAG_IMMUTABLE);
                    manager.requestPermission(device, permissionIntent);
                    return false; // Or wait for the broadcast
                }

                UsbDeviceConnection connection = manager.openDevice(driver.getDevice());

                Log.d("EX_Toolbox", "SocketUsb.connect(): driver.getPorts()");
                port = driver.getPorts().get(0); // Most devices have just one port
                Log.d("EX_Toolbox", "SocketUsb.connect(): port.open()");
                port.open(connection);
                Log.d("EX_Toolbox", "SocketUsb.connect(): setParameters()");
                port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                Log.d("EX_Toolbox", "SocketUsb.connect(): ready)");

            } catch (Exception except) {
                if (!firstConnect) {
                    threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppCantConnect,
                            mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address, except.getMessage()), Toast.LENGTH_LONG);
                }
                socketOk = false;
            }
        }

        startReading();

        socketGood = socketOk;
        if (socketOk)
            firstConnect = true;
        return socketOk;
    }

    public void disconnect(boolean shutdown) {
        disconnect(shutdown, false);
    }

    public void disconnect(boolean shutdown, boolean fastShutdown) {
        if (shutdown) {
            endRead = true;
            if (!fastShutdown) {
                for (int i = 0; i < 5 && this.isAlive(); i++) {
                    try {
                        Thread.sleep(connectTimeoutMs);     //  give run() a chance to see endRead and exit
                    } catch (InterruptedException e) {
                        threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorSleepingThread, e.getMessage()), Toast.LENGTH_SHORT);
                    }
                }
            }
        }

        socketGood = false;

        try {
            if (port != null) {
                port.close();
            }
        } catch (IOException e) {
            Log.d("EX_Toolbox", "SocketUsb.disconnect(): Error closing the Socket: " + e.getMessage());
        }
    }

    private void startReading() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[4096];
                StringBuilder str = new StringBuilder();
                while ( (socketGood) && (!endRead) ) {
                    try {
                        String bitOfStr = new String(buffer, 0, port.read(buffer, socketTimeoutMs));
                        str.append(bitOfStr);
                        if (!str.toString().isEmpty()) {
                            if ( (str.toString().contains("<")) && (str.toString().contains(">")) ) {

                                String wholeStr = str.toString();
                                String remainder = wholeStr.substring(wholeStr.indexOf(">")+2);

                                String oneStr = wholeStr.substring(wholeStr.indexOf("<"), wholeStr.indexOf(">") + 1);

                                Log.d("EX_Toolbox", "SocketUsb.read(): whole str »" + str +"«");
                                Log.d("EX_Toolbox", "SocketUsb.read(): one str   »" + oneStr +"«");
                                Log.d("EX_Toolbox", "SocketUsb.read(): remainder »" + remainder +"«\n\n");

                                String[] superCmds = oneStr.split("\n");

                                for (int j = 0; j < superCmds.length; j++) {
                                    String[] cmds = superCmds[j].split("><");
                                    if (cmds.length == 1) { // multiple concatenated commands
                                        comm_thread.processWifiResponse(cmds[0]);
                                    } else {
                                        for (int i = 0; i < cmds.length; i++) {
                                            if ((cmds[i].charAt(0) == '<') && (cmds[i].charAt(cmds[i].length() - 1)) == '>') {
                                                comm_thread.processWifiResponse(cmds[i]);
                                            } else if ((cmds[i].charAt(0) == '<') && (cmds[i].charAt(cmds[i].length() - 1)) != '>') {
                                                comm_thread.processWifiResponse(cmds[i] + ">");
                                            } else if ((cmds[i].charAt(0) != '<') && (cmds[i].charAt(cmds[i].length() - 1)) == '>') {
                                                comm_thread.processWifiResponse("<" + cmds[i]);
                                            } else {
                                                comm_thread.processWifiResponse("<" + cmds[i] + ">");
                                            }
                                        }
                                    }
                                }
                                str.setLength(0);
                                if (!remainder.isEmpty()) str.append(remainder);

                            } else {
                                Log.d("EX_Toolbox", "SocketUsb.read(): partial: »" + str + "«");
                            }

                            comm_thread.heart.restartInboundInterval();
                            clearInboundTimeout();
                        }
                    } catch (IOException e) {
                        // Handle disconnected or error
                        Log.d("EX_Toolbox", "SocketUsb.disconnect(): disconnected or error: " + e.getMessage());
                        break;
                    }
                }

                comm_thread.heart.stopHeartbeat();
                Log.d("EX_Toolbox", "SocketUsb.run(): SocketUsb exit.");
            }
        });
    }

//    private void stopReading() {
//        isRunning = false;
//        // Shut down executor and close sPort
//    }



//    //read the input buffer
//    public void run() {
//        String str;
//        //continue reading until signaled to exit by endRead
//        while (!endRead) {
//            if (socketGood) {        //skip read when the socket is down
//                try {
//                    byte[] buffer = new byte[1024];
//                    str = new String(buffer, 0, port.read(buffer, socketTimeoutMs));
//                    if (!str.isEmpty()) {
//                        comm_thread.heart.restartInboundInterval();
//                        clearInboundTimeout();
//                        comm_thread.processWifiResponse(str);
//                   }
//                } catch (SocketTimeoutException e) {
//                    socketGood = this.SocketCheck();
//                } catch (IOException e) {
//                    if (socketGood) {
//                        Log.d("EX_Toolbox", "SocketUsb.run(): WiT rcvr error.");
//                        socketGood = false;     //input buffer error so force reconnection on next send
//                    }
//                }
//            }
//            if (!socketGood) {
//                SystemClock.sleep(500L);        //don't become compute bound here when the socket is down
//            }
//        }
//        comm_thread.heart.stopHeartbeat();
//        Log.d("EX_Toolbox", "SocketUsb.run(): SocketUsb exit.");
//    }

    @SuppressLint("StringFormatMatches")
    void Send(String msg) {
        boolean reconInProg = false;
        //reconnect socket if needed
        if (!socketGood || inboundTimeout) {
            String status;
            if (mainapp.client_address == null) {
                status = threaded_application.context.getResources().getString(R.string.statusThreadedAppNotConnected);
                Log.d("EX_Toolbox", "SocketUsb.send(): WiT send reconnection attempt.");
            } else if (inboundTimeout) {
                status = threaded_application.context.getResources().getString(R.string.statusThreadedAppNoResponse, mainapp.host_ip, Integer.toString(mainapp.port), comm_thread.heart.getInboundInterval());
                Log.d("EX_Toolbox", "SocketUsb.send(): WiT receive reconnection attempt.");
            } else {
                status = threaded_application.context.getResources().getString(R.string.statusThreadedAppUnableToConnect, mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address);
                Log.d("EX_Toolbox", "SocketUsb.send(): WiT send reconnection attempt.");
            }
            socketGood = false;

            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WIT_CON_RETRY, status);

            //perform the reconnection sequence
            this.disconnect(false);             //clean up socket but do not shut down the receiver
            this.connect();                     //attempt to reestablish connection
            reconInProg = true;
        }

        //try to send the message
        if (socketGood) {
            try {
                byte[] dataBytes = msg.getBytes();
                port.write(dataBytes, socketTimeoutMs);

                comm_thread.heart.restartOutboundInterval();

                // if we get here without an exception then the socket is ok
                if (reconInProg) {
                    String status = "Connected to WiThrottle Server at " + mainapp.host_ip + ":" + mainapp.port;
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WIT_CON_RECONNECT, status);
                    Log.d("EX_Toolbox", "SocketUsb.send(): WiT reconnection successful.");
                    clearInboundTimeout();
                    comm_thread.heart.restartInboundInterval();     //socket is good so restart inbound heartbeat timer
                }
            } catch (Exception e) {
                Log.d("EX_Toolbox", "SocketUsb.send(): WiT xmtr error.");
                socketGood = false;             //output buffer error so force reconnection on next send
            }
        }

        if (!socketGood) {
            mainapp.comm_msg_handler.postDelayed(comm_thread.heart.outboundHeartbeatTimer, 500L);   //try connection again in 0.5 second
        }
    }

    // Attempt to determine if the socket connection is still good.
    // unfortunately isConnected returns true if the Socket was disconnected other than by calling close()
    // so on signal loss it still returns true.
    // Eventually we just try to send and handle the IOException if the socket was disconnected.
//    boolean SocketCheck() {
////        boolean status = clientSocket.isConnected() && !clientSocket.isInputShutdown() && !clientSocket.isOutputShutdown();
//        boolean status = true;
//        if (status)
//            status = HaveNetworkConnection();   // can't trust the socket flags so try something else...
//        return status;
//    }
//
//    // temporary - SocketCheck should determine whether socket connection is good however socket flags sometimes do not get updated
//    // so it doesn't work.  This is better than nothing though?
//    private boolean HaveNetworkConnection() {
//        boolean haveConnectedWifi = false;
//        boolean haveConnectedMobile = false;
//        mainapp.prefAllowMobileData = prefs.getBoolean("prefAllowMobileData", false);
//
//        final ConnectivityManager cm = (ConnectivityManager) mainapp.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
//        for (NetworkInfo ni : netInfo) {
//            if ("WIFI".equalsIgnoreCase(ni.getTypeName()))
//
//                if (!mainapp.prefAllowMobileData) {
//                    // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
//                    if (!mainapp.haveForcedWiFiConnection) {
//
//                        Log.d("EX_Toolbox", "SocketUsb.HaveNetworkConnection(): NetworkRequest.Builder");
//                        NetworkRequest.Builder request = new NetworkRequest.Builder();
//                        request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
//
//                        cm.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {
//                            @Override
//                            public void onAvailable(Network network) {
//                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//                                    ConnectivityManager.setProcessDefaultNetwork(network);
//                                } else {
//                                    cm.bindProcessToNetwork(network);  //API23+
//                                }
//                            }
//                        });
//                        mainapp.haveForcedWiFiConnection = true;
//                    }
//                }
//
//            if (ni.isConnected()) {
//                haveConnectedWifi = true;
//            } else {
//                // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
//                if (mainapp.prefAllowMobileData) {
//                    haveConnectedWifi = true;
//                }
//            }
//            if ("MOBILE".equalsIgnoreCase(ni.getTypeName()))
//                if ((ni.isConnected()) && (mainapp.prefAllowMobileData)) {
//                    haveConnectedMobile = true;
//                }
//        }
//        return haveConnectedWifi || haveConnectedMobile;
//    }

    boolean SocketGood() {
        return this.socketGood;
    }

    void InboundTimeout() {
        if (++inboundTimeoutRetryCount >= MAX_INBOUND_TIMEOUT_RETRIES) {
            Log.d("EX_Toolbox", "SocketUsb.InboundTimeout(): WiT max inbound timeouts");
            inboundTimeout = true;
            inboundTimeoutRetryCount = 0;
            inboundTimeoutRecovery = false;
            // force a 'send' to start the reconnection process
            mainapp.comm_msg_handler.postDelayed(comm_thread.heart.outboundHeartbeatTimer, 200L);
        } else {
            Log.d("EX_Toolbox", "SocketUsb.InboundTimeout(): WiT inbound timeout " +
                    inboundTimeoutRetryCount + " of " + MAX_INBOUND_TIMEOUT_RETRIES);
            // heartbeat should trigger a WiT reply so force that now
            inboundTimeoutRecovery = true;
            mainapp.comm_msg_handler.post(comm_thread.heart.outboundHeartbeatTimer);
        }
    }

    void clearInboundTimeout() {
        inboundTimeout = false;
        inboundTimeoutRecovery = false;
        inboundTimeoutRetryCount = 0;
    }
}
