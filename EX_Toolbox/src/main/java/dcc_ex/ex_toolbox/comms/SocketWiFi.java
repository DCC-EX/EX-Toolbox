package dcc_ex.ex_toolbox.comms;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import dcc_ex.ex_toolbox.R;
import dcc_ex.ex_toolbox.threaded_application;
import dcc_ex.ex_toolbox.type.message_type;

class SocketWiFi extends Thread {
    InetAddress host_address;
    Socket clientSocket = null;
    BufferedReader inputBR = null;
    PrintWriter outputPW = null;
    private volatile boolean endRead = false;           //signals rcvr to terminate
    private volatile boolean socketGood = false;        //indicates socket condition
    private volatile boolean inboundTimeout = false;    //indicates inbound messages are not arriving from WiT
    private boolean firstConnect = false;               //indicates initial socket connection was achieved
    private int connectTimeoutMs = 3000; //connection timeout in milliseconds
    private int socketTimeoutMs = 500; //socket timeout in milliseconds

    private final int MAX_INBOUND_TIMEOUT_RETRIES = 2;
    private int inboundTimeoutRetryCount = 0;           // number of consecutive inbound timeouts
    boolean inboundTimeoutRecovery = false;     // attempting to force WiT to respond

    static SharedPreferences prefs;
    static threaded_application mainapp;
    static comm_thread commThread;

     SocketWiFi(threaded_application mainapp, SharedPreferences prefs, comm_thread commThread) {
        super("socketWiFi");

        SocketWiFi.prefs = prefs;
        SocketWiFi.mainapp = mainapp;
        SocketWiFi.commThread = commThread;
    }

    public boolean connect() {

        //use local socketOk instead of setting socketGood so that the rcvr doesn't resume until connect() is done
        boolean socketOk = HaveNetworkConnection();

        connectTimeoutMs = Integer.parseInt(prefs.getString("prefConnectTimeoutMs", mainapp.getResources().getString(R.string.prefConnectTimeoutMsDefaultValue)));
        socketTimeoutMs = Integer.parseInt(prefs.getString("prefSocketTimeoutMs", mainapp.getResources().getString(R.string.prefSocketTimeoutMsDefaultValue)));

        //validate address
        if (socketOk) {
            try {
                host_address = InetAddress.getByName(mainapp.host_ip);
            } catch (UnknownHostException except) {
//                        show_toast_message("Can't determine IP address of " + host_ip, Toast.LENGTH_LONG);
                threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppCantDetermineIp, mainapp.host_ip), Toast.LENGTH_SHORT);
                socketOk = false;
            }
        }

        //socket
        if (socketOk) {
            try {
                mainapp.isUSB = false;

                //look for someone to answer on specified socket, and set timeout
                Log.d("EX_Toolbox", "SocketWiFi.socketWifi: Opening socket, connectTimeout=" + connectTimeoutMs + " and socketTimeout=" + socketTimeoutMs);
                clientSocket = new Socket();
                InetSocketAddress sa = new InetSocketAddress(mainapp.host_ip, mainapp.port);
                clientSocket.connect(sa, connectTimeoutMs);
                Log.d("EX_Toolbox", "SocketWiFi.socketWifi: Opening socket: Connect successful.");
                clientSocket.setSoTimeout(socketTimeoutMs);
                Log.d("EX_Toolbox", "SocketWiFi.socketWifi: Opening socket: set timeout successful.");
            } catch (Exception except) {
                if (!firstConnect) {
                    threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppCantConnect,
                            mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address, except.getMessage()), Toast.LENGTH_LONG);
                }
                if ((!mainapp.client_type.equals("WIFI")) && (mainapp.prefAllowMobileData)) { //show additional message if using mobile data
                    Log.d("EX_Toolbox", "SocketWiFi.socketWifi: Opening socket: Using mobile network, not WIFI. Check your WiFi settings and Preferences.");
                    threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppNotWIFI, mainapp.client_type), Toast.LENGTH_LONG);
                }
                socketOk = false;
            }
        }

        //rcvr
        if (socketOk) {
            try {
                inputBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException except) {
                threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorInputStream, except.getMessage()), Toast.LENGTH_SHORT);
                socketOk = false;
            }
        }

        //start the socketWifi thread.
        if (socketOk) {
            if (!this.isAlive()) {
                endRead = false;
                try {
                    this.start();
                } catch (IllegalThreadStateException except) {
                    //ignore "already started" errors
                    threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorStartingSocket, except.getMessage()), Toast.LENGTH_SHORT);
                }
            }
        }

        //xmtr
        if (socketOk) {
            try {
                outputPW = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                if (outputPW.checkError()) {
                    socketOk = false;
                }
            } catch (IOException e) {
                threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorCreatingOutputStream, e.getMessage()), Toast.LENGTH_SHORT);
                socketOk = false;
            }
        }
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

        //close socket
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (Exception e) {
                Log.d("EX_Toolbox", "SocketWiFi.socketWifi: Error closing the Socket: " + e.getMessage());
            }
        }
    }

    //read the input buffer
    public void run() {
        String str;
        //continue reading until signaled to exit by endRead
        while (!endRead) {
            if (socketGood) {        //skip read when the socket is down
                try {
                    if ((str = inputBR.readLine()) != null) {
                        if (!str.isEmpty()) {
                            comm_thread.heart.restartInboundInterval();
                            clearInboundTimeout();
                            comm_thread.processWifiResponse(str);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    socketGood = this.SocketCheck();
                } catch (IOException e) {
                    if (socketGood) {
                        Log.d("EX_Toolbox", "SocketWiFi.run(): WiT rcvr error.");
                        socketGood = false;     //input buffer error so force reconnection on next send
                    }
                }
            }
            if (!socketGood) {
                SystemClock.sleep(500L);        //don't become compute bound here when the socket is down
            }
        }
        comm_thread.heart.stopHeartbeat();
        Log.d("EX_Toolbox", "SocketWiFi.run(): socketWifi exit.");
    }

    @SuppressLint("StringFormatMatches")
    void Send(String msg) {
        boolean reconInProg = false;
        //reconnect socket if needed
        if (!socketGood || inboundTimeout) {
            String status;
            if (mainapp.client_address == null) {
                status = threaded_application.context.getResources().getString(R.string.statusThreadedAppNotConnected);
                Log.d("EX_Toolbox", "SocketWiFi.send(): WiT send reconnection attempt.");
            } else if (inboundTimeout) {
                status = threaded_application.context.getResources().getString(R.string.statusThreadedAppNoResponse, mainapp.host_ip, Integer.toString(mainapp.port), comm_thread.heart.getInboundInterval());
                Log.d("EX_Toolbox", "SocketWiFi.send(): WiT receive reconnection attempt.");
            } else {
                status = threaded_application.context.getResources().getString(R.string.statusThreadedAppUnableToConnect, mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address);
                Log.d("EX_Toolbox", "SocketWiFi.send(): WiT send reconnection attempt.");
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
                outputPW.println(msg);
                outputPW.flush();
                comm_thread.heart.restartOutboundInterval();

                // if we get here without an exception then the socket is ok
                if (reconInProg) {
                    String status = "Connected to WiThrottle Server at " + mainapp.host_ip + ":" + mainapp.port;
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WIT_CON_RECONNECT, status);
                    Log.d("EX_Toolbox", "SocketWiFi.send(): WiT reconnection successful.");
                    clearInboundTimeout();
                    comm_thread.heart.restartInboundInterval();     //socket is good so restart inbound heartbeat timer
                }
            } catch (Exception e) {
                Log.d("EX_Toolbox", "SocketWiFi.send(): WiT xmtr error.");
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
    boolean SocketCheck() {
        boolean status = clientSocket.isConnected() && !clientSocket.isInputShutdown() && !clientSocket.isOutputShutdown();
        if (status)
            status = HaveNetworkConnection();   // can't trust the socket flags so try something else...
        return status;
    }

    // temporary - SocketCheck should determine whether socket connection is good however socket flags sometimes do not get updated
    // so it doesn't work.  This is better than nothing though?
    private boolean HaveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        mainapp.prefAllowMobileData = prefs.getBoolean("prefAllowMobileData", false);

        final ConnectivityManager cm = (ConnectivityManager) mainapp.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if ("WIFI".equalsIgnoreCase(ni.getTypeName()))

                if (!mainapp.prefAllowMobileData) {
                    // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
                    if (!mainapp.haveForcedWiFiConnection) {

                        Log.d("EX_Toolbox", "SocketWiFi.HaveNetworkConnection: NetworkRequest.Builder");
                        NetworkRequest.Builder request = new NetworkRequest.Builder();
                        request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

                        cm.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {
                            @Override
                            public void onAvailable(Network network) {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                    ConnectivityManager.setProcessDefaultNetwork(network);
                                } else {
                                    cm.bindProcessToNetwork(network);  //API23+
                                }
                            }
                        });
                        mainapp.haveForcedWiFiConnection = true;
                    }
                }

            if (ni.isConnected()) {
                haveConnectedWifi = true;
            } else {
                // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
                if (mainapp.prefAllowMobileData) {
                    haveConnectedWifi = true;
                }
            }
            if ("MOBILE".equalsIgnoreCase(ni.getTypeName()))
                if ((ni.isConnected()) && (mainapp.prefAllowMobileData)) {
                    haveConnectedMobile = true;
                }
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    boolean SocketGood() {
        return this.socketGood;
    }

    void InboundTimeout() {
        if (++inboundTimeoutRetryCount >= MAX_INBOUND_TIMEOUT_RETRIES) {
            Log.d("EX_Toolbox", "SocketWiFi.InboundTimeout: WiT max inbound timeouts");
            inboundTimeout = true;
            inboundTimeoutRetryCount = 0;
            inboundTimeoutRecovery = false;
            // force a 'send' to start the reconnection process
            mainapp.comm_msg_handler.postDelayed(comm_thread.heart.outboundHeartbeatTimer, 200L);
        } else {
            Log.d("EX_Toolbox", "SocketWiFi.InboundTimeout: WiT inbound timeout " +
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
