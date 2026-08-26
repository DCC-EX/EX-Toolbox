package dcc_ex.ex_toolbox.comms;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dcc_ex.ex_toolbox.R;
import dcc_ex.ex_toolbox.threaded_application;
import dcc_ex.ex_toolbox.type.message_type;

class SocketUdp extends Thread {
    public static final String activityName = "SocketUdp";
//    PrintWriter outputPW = null;

    private DatagramSocket udpSocket;

    boolean socketOk = true;
    private boolean isRunning = true;

    private volatile boolean endRead = false;           //signals rcvr to terminate
    private volatile boolean socketGood = false;        //indicates socket condition
    private volatile boolean inboundTimeout = false;    //indicates inbound messages are not arriving from WiT
    private boolean firstConnect = false;               //indicates initial socket connection was achieved
    private int connectTimeoutMs = 3000; //connection timeout in milliseconds
    private int socketTimeoutMs = 500; //socket timeout in milliseconds

    private static final int BUFFER_SIZE = 2048;
    byte[] receiveBuffer = new byte[BUFFER_SIZE];

    private final int MAX_INBOUND_TIMEOUT_RETRIES = 2;
    private int inboundTimeoutRetryCount = 0;           // number of consecutive inbound timeouts
    boolean inboundTimeoutRecovery = false;     // attempting to force WiT to respond

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private final StringBuilder str = new StringBuilder();

    static SharedPreferences prefs;
    static threaded_application mainapp;
    static comm_thread commThread;

     SocketUdp(threaded_application mainapp, SharedPreferences prefs, comm_thread commThread) {
        super("socketUdp");

        SocketUdp.prefs = prefs;
        SocketUdp.mainapp = mainapp;
        SocketUdp.commThread = commThread;
    }

    public boolean connect() {

        //use local socketOk instead of setting socketGood so that the rcvr doesn't resume until connect() is done

        endRead = false;

        connectTimeoutMs = Integer.parseInt(prefs.getString("prefConnectTimeoutMs", mainapp.getResources().getString(R.string.prefConnectTimeoutMsDefaultValue)));
        socketTimeoutMs = Integer.parseInt(prefs.getString("prefSocketTimeoutMs", mainapp.getResources().getString(R.string.prefSocketTimeoutMsDefaultValue)));

        mainapp.isUSB = false;
        mainapp.isUdp = true;

        try {
            // Initialize socket (bound to local port)
            udpSocket = new DatagramSocket(null);
            udpSocket.setReuseAddress(true);
            udpSocket.bind(new InetSocketAddress(mainapp.port));
            receiveBuffer = new byte[BUFFER_SIZE];

            socketOk = true;
            socketGood = true;

        } catch (Exception e) {
            socketOk = false;
            socketGood = false;
            Log.e(threaded_application.applicationName, activityName+": Socket error: ", e);

        }

        //start the SocketUDP thread.
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

        if (socketOk)
            firstConnect = true;
        return socketOk;
    }

    public void disconnect(boolean shutdown) {
        disconnect(shutdown, false);
    }

    public void disconnect(boolean shutdown, boolean fastShutdown) {
        Log.d(threaded_application.applicationName, activityName+": <:> disconnect():");

        if (shutdown) {
            endRead = true;
            if (!fastShutdown) {
                for (int i = 0; i < 5 && this.isAlive(); i++) {
                    try {
                        Thread.sleep(connectTimeoutMs); //  give run() a chance to see endRead and exit
                    } catch (InterruptedException e) {
                        threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorSleepingThread, e.getMessage()), Toast.LENGTH_SHORT);
                    }
                }
            }
        }

        socketGood = false;
        isRunning = false;
        comm_thread.heart.stopHeartbeat();
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }

    public void run() {
        while (isRunning) {
            try {
                // Receive Packet
                Log.d(threaded_application.applicationName, activityName + ": <:> Waiting: ");
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(receivePacket); // Blocks until a packet arrives

                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                Log.d(threaded_application.applicationName, activityName + ": <:> Received: " + receivedMessage);

                clearInboundTimeout();
                processMessage(receivedMessage);
                // Continuous listen loop
            } catch (Exception e) {
                socketOk = false;
                socketGood = false;
                Log.e(threaded_application.applicationName, activityName + ": Socket error: ", e);
            }
        }
    }

    private void processMessage(String message) {
        str.append(message);
        while ( (socketGood) && (!endRead) ) {
            try {
                if (str.toString().isEmpty()) break;
                if ( (str.toString().contains("<")) && (str.toString().contains(">")) ) {

                    String wholeStr = replaceAnglesInQuotes(str.toString());
                    int endIdx = wholeStr.indexOf(">");
                    String oneStr = wholeStr.substring(wholeStr.indexOf("<"), endIdx + 1);
                    String remainder = (endIdx + 2 <= wholeStr.length()) ? wholeStr.substring(endIdx + 2) : "";

                    Log.d(threaded_application.applicationName, activityName+": SocketUdp.read(): whole str »" + wholeStr +"«");
                    Log.d(threaded_application.applicationName, activityName+": SocketUdp.read(): one str   »" + oneStr +"«");
                    Log.d(threaded_application.applicationName, activityName+": SocketUdp.read(): remainder »" + remainder +"«\n\n");

                    if ( oneStr.split("\n")[0].contains(">")) {

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
                    } else { // multi-line response
                        String[] cmds = oneStr.split("><");
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

                    comm_thread.heart.restartInboundInterval();
                    clearInboundTimeout();

                    if (remainder.isEmpty()) break;

                } else {
                    Log.d(threaded_application.applicationName, activityName+": SocketUdp.read(): partial: »" + str + "«");
                    comm_thread.heart.restartInboundInterval();
                    clearInboundTimeout();
                    break;
                }
            } catch (Exception e) {
                // Handle disconnected or error
                Log.d(threaded_application.applicationName, activityName+": SocketUdp.processMessage(): error: " + e.getMessage());
                break;
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    void Send(String msg) {
        boolean reconInProg = false;
        //reconnect socket if needed
        if (!socketGood || inboundTimeout) {
            String status;
            if (mainapp.client_address == null) {
                status = threaded_application.context.getResources().getString(R.string.statusThreadedAppNotConnected);
                Log.d(threaded_application.applicationName, activityName+": SocketUdp.send(): WiT send reconnection attempt.");
            } else if (inboundTimeout) {
                status = threaded_application.context.getResources().getString(R.string.statusThreadedAppNoResponse, mainapp.host_ip, Integer.toString(mainapp.port), comm_thread.heart.getInboundInterval());
                Log.d(threaded_application.applicationName, activityName+": SocketUdp.send(): WiT receive reconnection attempt.");
            } else {
                status = threaded_application.context.getResources().getString(R.string.statusThreadedAppUnableToConnect, mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address);
                Log.d(threaded_application.applicationName, activityName+": SocketUdp.send(): WiT send reconnection attempt.");
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
            if (udpSocket == null || udpSocket.isClosed()) return;

            try {
                new Thread(() -> {
                    try {
                        String message = msg +"\n";
                        byte[] sendData = message.getBytes();
                        InetAddress serverAddress = InetAddress.getByName(mainapp.host_ip);

                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, mainapp.port);
                        udpSocket.send(sendPacket);
                        Log.d(threaded_application.applicationName, activityName+": Sent: " + msg);
                    } catch (Exception e) {
                        Log.e(threaded_application.applicationName, activityName+": Send error: ", e);
                    }
                }).start();

                comm_thread.heart.restartOutboundInterval();

                // if we get here without an exception then the socket is ok
                if (reconInProg) {
                    String status = "Connected to WiThrottle Server at " + mainapp.host_ip + ":" + mainapp.port;
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WIT_CON_RECONNECT, status);
                    Log.d(threaded_application.applicationName, activityName+": SocketUdp.send(): WiT reconnection successful.");
                    clearInboundTimeout();
                    comm_thread.heart.restartInboundInterval();     //socket is good so restart inbound heartbeat timer
                }
            } catch (Exception e) {
                Log.d(threaded_application.applicationName, activityName+": SocketUdp.send(): WiT xmtr error.");
                socketGood = false;             //output buffer error so force reconnection on next send
            }
        }

        if (!socketGood) {
            mainapp.comm_msg_handler.postDelayed(comm_thread.heart.outboundHeartbeatTimer, 500L);   //try connection again in 0.5 second
        }
    }

    boolean SocketGood() {
        return this.socketGood;
    }

    void InboundTimeout() {
        if (++inboundTimeoutRetryCount >= MAX_INBOUND_TIMEOUT_RETRIES) {
            Log.d(threaded_application.applicationName, activityName+": SocketUdp.InboundTimeout(): WiT max inbound timeouts");
            inboundTimeout = true;
            inboundTimeoutRetryCount = 0;
            inboundTimeoutRecovery = false;
            // force a 'send' to start the reconnection process
            mainapp.comm_msg_handler.postDelayed(comm_thread.heart.outboundHeartbeatTimer, 200L);
        } else {
            Log.d(threaded_application.applicationName, activityName+": SocketUdp.InboundTimeout(): WiT inbound timeout " +
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

    public static String replaceAnglesInQuotes(String input) {
        // Matches anything inside double quotes, handling escaped quotes \"
        Pattern pattern = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group();
            // Replace < and > only within the matched quoted string
            String replaced = match.replace("<", "‹").replace(">", "›");
            // Use Matcher.quoteReplacement to safely handle special characters
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replaced));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
