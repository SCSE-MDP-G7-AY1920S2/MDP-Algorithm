package Network;

import Algorithm.Exploration;
import Main.SimulatorNew;
import Map.Map;
import Map.MapDescriptor;
import Robot.Robot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

/**
 * Socket client class to connect to RPI
 */
public class NetworkManager {

    private static final Logger LOGGER = Logger.getLogger(NetworkManager.class.getName());

    private String ip;
    private int port;

    public static Socket socket = null;
    private String prevMsg = null;

    private BufferedWriter out;
    private BufferedReader in;
    private int msgCounter = 0;

    private static NetworkManager netMgr = null;

    /**
     * cheat
     */
    Exploration explore;

    Robot robot;

    public void setExplore(Exploration explore){
        this.explore = explore;
    }

    public void setRobot(Robot robot){
        this.robot = robot;
    }

    /**
     * Getter & Setter
     */
    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public NetworkManager(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public static NetworkManager getInstance() {
        if (netMgr == null) {
            netMgr = new NetworkManager(NetworkConstants.IP, NetworkConstants.PORT);
        }
        return netMgr;
    }

    /**
     * Initiate a connection with RPI if there isn't already one
     *
     * @return true if connection established with RPI
     */
    public boolean initConn() {
        if (isConnect()) {
            LOGGER.info("Connection with RPI was established.");
            return true;
        } else {
            try {
                LOGGER.info("Initiating Connection with RPI...");
                socket = new Socket(ip, port);
                out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                LOGGER.info("Connection with RPI established!");
                return true;
            } catch (UnknownHostException e) {
                LOGGER.warning("Connection Failed: UnknownHostException\n" + e.toString());
                return false;
            } catch (IOException e) {
                LOGGER.warning("Connection Failed: IOException\n" + e.toString());
                return false;
            } catch (Exception e) {
                LOGGER.warning("Connection Failed!\n" + e.toString());
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Check if there are existing connection with RPI
     *
     * @return
     */
    public boolean isConnect() {
        return (socket == null) ? false : true;
    }

    /**
     * Close the connection with RPI
     *
     * @return True if there is no more connection with RPI
     */
    public boolean closeConn() {
        LOGGER.info("Closing connection... ");
        if (!isConnect()) {
            LOGGER.warning("No connection with RPI");
            return true;
        } else {
            try {
                socket.close();
                out.close();
                in.close();
                socket = null;
                return true;
            } catch (IOException e) {
                LOGGER.warning("Unable to close connection: IOException\n" + e.toString());
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Sending a String type msg through socket
     *
     * @param msg
     * @return true if the message is sent out successfully
     */
    public boolean send(String msg, String purpose) {
        try {
            LOGGER.log(Level.FINE, "Sending Message...");
            JSONObject cmdJson;
            if (purpose == NetworkConstants.FASTEST_PATH) {
                cmdJson = new JSONObject()
                        .put("from", "Applet")
                        .put("com", purpose)
                        .put("path", msg);
            } else {
                cmdJson = new JSONObject()
                        .put("from", "Applet")
                        .put("com", msg);
            }


            msg = cmdJson.toString();
            msg = ";" + msg;
            out.write(msg);
            out.newLine();
            out.flush();
            msgCounter++;
            LOGGER.info(msgCounter + " Message Sent: " + msg);
            prevMsg = msg;

            System.out.println(msg);

            return true;
        } catch (IOException e) {
            LOGGER.info("Sending Message Failed (IOException)!");
            if (socket.isConnected())
                LOGGER.info("Connection still Established!");
            else {
                while (true) {
                    LOGGER.info("Connection disrupted! Trying to Reconnect!");
                    if (netMgr.initConn()) {
                        break;
                    }
                }
            }
            return netMgr.send(msg, purpose);
        } catch (Exception e) {
            LOGGER.info("Sending Message Failed!");
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Sending a String type msg through socket
     *
     * @param com
     * @return true if the message is sent out successfully
     */
    public boolean send(String com, String expMDF, String objMDF, int[] status, String[][]img) {
        String msg;

        try {
            LOGGER.log(Level.FINE, "Sending Message...");
            JSONObject MDFJson;
            MDFJson = new JSONObject()
                    .put("from", "Applet")
                    .put("com", com)
                    .put("expMDF", expMDF)
                    .put("objMDF", objMDF)
                    .put("pos", status)
                    .put("imgs", img);

            msg = MDFJson.toString();
            msg = ";" + msg;
            out.write(msg);
            out.newLine();
            out.flush();
//            TimeUnit.MILLISECONDS.sleep(100); //need to be removed on actual run

            msgCounter++;
            LOGGER.info(msgCounter + " Message Sent: " + msg);
            prevMsg = msg;

            System.out.println(msg);


            return true;
        } catch (IOException e) {
            LOGGER.info("Sending Message Failed (IOException)!");
            if (socket.isConnected())
                LOGGER.info("Connection still Established!");
            else {
                while (true) {
                    LOGGER.info("Connection disrupted! Trying to Reconnect!");
                    if (netMgr.initConn()) {
                        break;
                    }
                }
            }
            return netMgr.send(com, expMDF, objMDF, status, img);
        } catch (Exception e) {
            LOGGER.info("Sending Message Failed!");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Receive a message which is send through the socket
     *
     * @return the message that is receive
     */
    public String receive() {
        try {
            LOGGER.log(Level.FINE, "Receiving Message...");
            String receivedMsg = in.readLine();
            while (receivedMsg == null || receivedMsg.isEmpty()) {
                receivedMsg = in.readLine();
            }
            LOGGER.info("Received in receive(): " + receivedMsg);


            JSONObject msg = new JSONObject(new JSONTokener(receivedMsg));
            String result = " ";

            if (msg.has("com"))
                result = msg.getString("com");

//            if (result.equals("T")) {
//                System.out.println(msg);
//                System.out.println(result);
//                explore.terminateExp();
//                explore.setTerminate(true);
//
//                if(SimulatorNew.displayTimer != null)
//                    SimulatorNew.displayTimer.stop();
//
//                robot.send_android(explore.getcurrentMap(), NetworkConstants.MDF);
//                netMgr.send("H", "Ex");
//
//                receivedMsg = receive();
//            }

            return receivedMsg;
        } catch (IOException e) {
            LOGGER.info("Receiving Message Failed (IOException)!");
            return receive();
        } catch (Exception e) {
            LOGGER.info("Receiving Message Failed!");
            e.printStackTrace();
        }
        return null;
    }


    public static void main(String[] args) throws InterruptedException {
        String ip = "192.168.7.7";
        int port = 36126;

        Map exploredMap = new Map();
        MapDescriptor MDF = new MapDescriptor();
        MDF.loadRealMap(exploredMap, "defaultMap.txt");
        String data;

        NetworkManager netMgr = new NetworkManager(ip, port);
        netMgr.initConn();


        while (true) {
//            String msg = Command.FORWARD.toString();
            do {
                data = netMgr.receive();
            } while (data == null);
//            data = netMgr.receive();
//            System.out.println("\nReceived: " + data);
            String msg = "AW3,D,W3,D1,";
            if (data.equals("checklist")) {
                netMgr.send(msg, NetworkConstants.EXPLORATION);
            }

//            netMgr.closeConn();
        }

//        JSONObject androidJson = new JSONObject();
//
//        // robot
//        JSONArray robotArray = new JSONArray();
//        JSONObject robotJson = new JSONObject()
//                .put("x", 1+ 1)
//                .put("y", 1 + 1)
//                .put("direction", Direction.LEFT.toString().toLowerCase());
//        robotArray.put(robotJson);
//
//        // map
//        String obstacleString = MDF.generateMDFString2(exploredMap);
//        JSONArray mapArray = new JSONArray();
//        JSONObject mapJson = new JSONObject()
//                .put("explored", MDF.generateMDFString1(exploredMap))
//                .put("obstacle", obstacleString)
//                .put("length", obstacleString.length() * 4);
//        mapArray.put(mapJson);
//
//        androidJson.put("map", mapArray).put("robot", robotArray);
//        netMgr.send(androidJson.toString());
    }

}
