package Robot;

import Map.*;
import Network.NetworkConstants;
import Network.NetworkManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Robot {

    private static final Logger LOGGER = Logger.getLogger(Robot.class.getName());

    //Options
    private boolean simulation;        // simulation vs actual run
    private boolean fastestPath;       // fastest path or exploration
    private boolean reachedEndPoint;
    private boolean imageRecognition = false;
    private boolean isExploration = false;

    //Robot
    private Point curLocation;         // the current location of the robot
    private MapDirections dir;         // the direction the robot is facing (UP, DOWN, LEFT, RIGHT)
    private String status;
    private RoboCmd preMove = RoboCmd.FORWARD;
    private int moveCounter = 0;

    //Sensors
    private ArrayList<String> sensorList;
    private HashMap<String, RobotSensors> sensorMap;
    private HashMap<String, Integer> sensorResult;

    //Camera
    RobotCamera rightCam;

    //surface
    private HashMap<String, MapObjectSurface> allPossibleSurfaces = new HashMap<String, MapObjectSurface>();
    private HashMap<String, MapObjectSurface> notYetTaken = new HashMap<String, MapObjectSurface>();
    private HashMap<String, MapObjectSurface> surfTakenMap = new HashMap<String, MapObjectSurface>();
    private HashMap<Point, String> allImageList = new HashMap<Point, String>();
    private HashMap<Point, String> removeList = new HashMap<Point, String>();

    // MapDescriptor
    private MapDescriptor MDF = new MapDescriptor();

    // for delay in sim
    private long tempStartTime, tempEndTime, tempDiff;

    //Network
    private static final NetworkManager netMgr = NetworkManager.getInstance();

    public int getMoveCounter() {
        return moveCounter;
    }

    public void setMoveCounter(int moveCounter) {
        this.moveCounter = moveCounter;
    }

    public boolean isExploration() {
        return isExploration;
    }

    public void setExploration(boolean exploration) {
        isExploration = exploration;
    }

    public HashMap<Point, String> getAllImageList() {
        return allImageList;
    }


    //surface getter setter

    public void setAllPossibleSurfaces(HashMap<String, MapObjectSurface> allPossibleSurfaces) {
        this.allPossibleSurfaces = allPossibleSurfaces;
    }

    public HashMap<String, MapObjectSurface> getNotYetTaken() {
        return notYetTaken;
    }

    public void setNotYetTaken(HashMap<String, MapObjectSurface> notYetTaken) {
        this.notYetTaken = notYetTaken;
    }

    public HashMap<String, MapObjectSurface> getSurfTakenMap() {
        return surfTakenMap;
    }

    public void setSurfTakenMap(HashMap<String, MapObjectSurface> surfTakenMap) {
        this.surfTakenMap = surfTakenMap;
    }

    /**
     * Getters
     **/

    public boolean getSimulation() {
        return this.simulation;
    }

    public boolean isFastestPath() {
        return this.fastestPath;
    }

    public Point getCurLocation() {
        return this.curLocation;
    }

    public String getStatus() {
        return this.status;
    }

    public MapDirections getDir() {
        return this.dir;
    }

    public boolean isReachedEndPoint() {
        return this.reachedEndPoint;
    }

    public ArrayList<String> getSensorList() {
        return sensorList;
    }

    public HashMap<String, RobotSensors> getSensorMap() {
        return sensorMap;
    }

    public RobotSensors getSensor(String sensorId) {
        return sensorMap.get(sensorId);
    }

    public HashMap<String, Integer> getSensorResult() {
        return sensorResult;
    }

    public boolean isImageRecognition() {
        return imageRecognition;
    }

    public RobotCamera getRightCam() {
        return rightCam;
    }

    public RoboCmd getPreMove() {
        return preMove;
    }

    /**
     * Setters
     **/
    public void setSimulation(boolean simulation) {
        this.simulation = simulation;
    }

    public void setFastestPath(boolean fastestPath) {
        this.fastestPath = fastestPath;
    }

    public void setPos(int row, int col) {
        this.curLocation = new Point(col, row);
    }

    public void setCurLocation(Point curLocation) {
        this.curLocation = curLocation;
    }

    public void setDir(MapDirections dir) {
        this.dir = dir;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setReachedEndPoint(boolean reachedEndPoint) {
        this.reachedEndPoint = reachedEndPoint;
    }

    public void setSensorResult(HashMap<String, Integer> sensorResult) {
        this.sensorResult = sensorResult;
    }

    public void setImageRecognition(boolean img) {
        this.imageRecognition = img;
    }

    public void setPreMove(RoboCmd preMove) {
        this.preMove = preMove;
    }

    public void setRightCam(RobotCamera rightCam) {
        this.rightCam = rightCam;
    }
    /**
     * ROBOT
     */

    /**
     * Constructor to create a Robot Object.
     *
     * @param simulation To check if this is a simulation or an actual run
     * @param fastestPath To check if the robot will be doing exploration or fastest path.
     * @param col the x location (column) the robot
     * @param row the y location (row) of the robot
     * @param dir the direction the robot is facing
     */
    public Robot(boolean simulation, boolean fastestPath, int row, int col, MapDirections dir) {
        this.simulation = simulation;
        this.fastestPath = fastestPath;
        this.reachedEndPoint = false;

        this.curLocation = new Point(col, row);
        this.dir = dir;

        this.sensorList = new ArrayList<String>();
        this.sensorMap = new HashMap<String, RobotSensors>();
        this.sensorResult = new HashMap<String, Integer>();

        initSensors();
        initCamera();

        this.status = String.format("Initialization completed.\n");
    }

    /**
     * Set the Robot and sensor position.
     *
     * @param row the location, Point y, of the robot.
     * @param col the location, Point x, of the robot.
     */
    public void setRobotPosition(int row, int col) {

        int colDiff = col - curLocation.x;
        int rowDiff = row - curLocation.y;

        curLocation.setLocation(col, row);
        setSensorPos(rowDiff, colDiff);
        setCameraPos(rowDiff, colDiff);
    }

    /**
     * Set starting position of the robot with the current exploration status of the map.
     *
     * @param row the location, Point y, of the robot.
     * @param col the location, Point x, of the robot.
     * @param exploredMap the exploration status of the map.
     */
    public void setStartPos(int row, int col, Map exploredMap) {
        setRobotPosition(row, col);
        exploredMap.setAllExplored(false);
        exploredMap.setAllMoveThru(false);

        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                exploredMap.getGrid(r, c).setExplored(true);
                exploredMap.getGrid(r, c).setMoveThru(true);
            }
        }
    }

    /**
     * Robot movement with direction (forward, backward) and steps) and Map updated.
     *  @param cmd         FORWARD or BACKWARD
     * @param steps       number of steps moved by the robot
     * @param currentMap current explored environment of the robot
     * @return Optional raw sensor data.
     */
    public Optional<String> move(RoboCmd cmd, int steps, Map currentMap, int stepsPerSecond) throws InterruptedException {

        tempStartTime = System.currentTimeMillis();
        Optional<String> sensorString = Optional.empty();
        if (!simulation && !fastestPath) {
            String cmdStr = getArduinoCommand(cmd, steps);
            netMgr.send(cmdStr, NetworkConstants.EXPLORATION);
            // CHANGES HERE
            String msg;
            do {
                msg = netMgr.receive();
                LOGGER.info(msg);
            } while (!msg.contains(NetworkConstants.MOVE_FIN));
            sensorString = Optional.of(msg);
        }

        int rowInc = 0, colInc = 0;

        switch (dir) {
            case UP:
                rowInc = 1;
                colInc = 0;
                break;
            case DOWN:
                rowInc = -1;
                colInc = 0;
                break;
            case LEFT:
                rowInc = 0;
                colInc = -1;
                break;
            case RIGHT:
                rowInc = 0;
                colInc = 1;
                break;
        }

        switch (cmd) {
            case FORWARD:
                break;
            case BACKWARD:
                rowInc *= -1;
                colInc *= -1;
                break;
            default:
                status = String.format("Invalid command: %s! No movement executed.\n", cmd.toString());
                LOGGER.warning(status);
                return Optional.empty();
        }

        preMove = cmd;

        int newRow = curLocation.y + rowInc * steps;
        int newCol = curLocation.x + colInc * steps;

        if (currentMap.checkValidMove(newRow, newCol)) {
            status = String.format("%s for %d steps\n", cmd.toString(), steps);
            LOGGER.info(status);
            LOGGER.info("row = " + newRow + ", col = " + newCol);

            // delay for simulation if not the robot will teleport too quickly
            if (simulation) {
                tempEndTime = System.currentTimeMillis();
                tempDiff = RobotConstants.WAIT_TIME / stepsPerSecond * steps - (tempEndTime - tempStartTime);
                if (tempDiff > 0) {
                    TimeUnit.MILLISECONDS.sleep(tempDiff);
                }
            }

            this.setRobotPosition(newRow, newCol);

            if (!fastestPath) {
                for (int i = 0; i < steps; i++) {
                    currentMap.setPassThru(newRow - rowInc * i, newCol - colInc * i);
                }
            }
        }
        return sensorString;
    }

    /**
     * Rotate Robot(LEFT_TURN, RIGHT_TURN)
     * @param cmd the command to turn the robot left or right
     * @return Optional raw sensor data.
     */
    public Optional<String> turn(RoboCmd cmd, int stepsPerSecond) throws InterruptedException {

        tempStartTime = System.currentTimeMillis();
        Optional<String> sensorString = Optional.empty();
        if (!simulation && !fastestPath) {
            String cmdStr = getArduinoCommand(cmd, 1);
            netMgr.send(cmdStr, NetworkConstants.EXPLORATION);
            String msg;
            do {
                msg = netMgr.receive();
                LOGGER.info(msg);
            } while (!msg.contains(NetworkConstants.MOVE_FIN));
            sensorString = Optional.of(msg);
        }

        switch (cmd) {
            case LEFT_TURN:
                dir = MapDirections.getAntiClockwise(dir);
                LOGGER.info("Rotating Robot Left");
                rotateSensors(MapDirections.LEFT);
                rotateCamera(MapDirections.LEFT);
                break;
            case RIGHT_TURN:
                dir = MapDirections.getClockwise(dir);
                LOGGER.info("Rotating Robot Right");
                rotateSensors(MapDirections.RIGHT);
                rotateCamera(MapDirections.RIGHT);
                break;
            default:
                status = "Invalid command! No movement executed.\n";
                LOGGER.warning(status);
                break;
        }
        preMove = cmd;
        status = cmd.toString() + "\n";

        LOGGER.info(status);
        LOGGER.info(curLocation.toString());

        // delay for simulator
        if (simulation) {
            tempEndTime = System.currentTimeMillis();
            tempDiff = RobotConstants.WAIT_TIME / stepsPerSecond - (tempEndTime - tempStartTime);
            if (tempDiff > 0) {
                TimeUnit.MILLISECONDS.sleep(tempDiff);
            }
        }
        return sensorString;
    }

    /**
     * SENSOR
     */

    /**
     * Initialize the Sensor In the following format
     * The robot is represented by a 3 x 3 cell space
     *      ^   ^   ^
     *      SR  SR  SR
     * < LR [X] [X] [X] SR >
     *      [X] [X] [X]
     *      [X] [X] [X] SR >
     */
    private void initSensors() {
        int row = curLocation.y;
        int col = curLocation.x;

        //initialise sensors for robot

        // Front Sensors
        RobotSensors SF1 = new RobotSensors("F1", RobotConstants.SHORT_IR_MIN, RobotConstants.SHORT_IR_MAX, row + 1, col - 1, MapDirections.UP);
        RobotSensors SF2 = new RobotSensors("F2", RobotConstants.SHORT_IR_MIN, RobotConstants.SHORT_IR_MAX, row + 1, col, MapDirections.UP);
        RobotSensors SF3 = new RobotSensors("F3", RobotConstants.SHORT_IR_MIN, RobotConstants.SHORT_IR_MAX, row + 1, col + 1, MapDirections.UP);

        // RIGHT Sensor
        RobotSensors SR1 = new RobotSensors("R1", RobotConstants.SHORT_IR_MIN, RobotConstants.SHORT_IR_MAX, row + 1, col + 1, MapDirections.RIGHT);
        RobotSensors SR2 = new RobotSensors("R2", RobotConstants.SHORT_IR_MIN, RobotConstants.SHORT_IR_MAX, row - 1, col + 1, MapDirections.RIGHT);

        // LEFT Sensor
        RobotSensors LL1 = new RobotSensors("L1", RobotConstants.LONG_IR_MIN, RobotConstants.LONG_IR_MAX, row + 1, col-1, MapDirections.LEFT);

        sensorList.add(SF1.getId());
        sensorList.add(SF2.getId());
        sensorList.add(SF3.getId());
        sensorList.add(SR1.getId());
        sensorList.add(SR2.getId());
        sensorList.add(LL1.getId());

        sensorMap.put(SF1.getId(), SF1);
        sensorMap.put(SF2.getId(), SF2);
        sensorMap.put(SF3.getId(), SF3);
        sensorMap.put(SR1.getId(), SR1);
        sensorMap.put(SR2.getId(), SR2);
        sensorMap.put(LL1.getId(), LL1);

        if (dir != MapDirections.UP) {

            rotateSensors(dir);
        }

        this.status = "Sensor initialized\n";
    }

    /**
     * Rotate the sensor.
     *
     * @param dir the direction the sensor will be rotated towards.
     */
    private void rotateSensors(MapDirections dir) {
        double angle = Math.PI / 2;

        LOGGER.info("Rotating Sensors...");
        switch (dir) {
            case DOWN:
                rotateSensors(MapDirections.RIGHT);
                rotateSensors(MapDirections.RIGHT);
                break;

            case LEFT:
                for (String sensorId : sensorList) {
                    RobotSensors sensor = sensorMap.get(sensorId);
                    sensor.setSensorDir(MapDirections.getAntiClockwise(sensor.getSensorDir()));
                    rotateSensorLocation(sensor, angle);
                }
                break;

            case RIGHT:
                for (String sensorId : sensorList) {
                    RobotSensors sensor = sensorMap.get(sensorId);
                    sensor.setSensorDir(MapDirections.getClockwise(sensor.getSensorDir()));
                    rotateSensorLocation(sensor, angle * -1);
                }
                break;

            default:
                LOGGER.warning("No rotation done. Wrong input direction: " + dir);
                break;
        }
        LOGGER.info("Sensors Rotated");
    }

    /**
     * Update the new location of sensors.
     *
     * @param sensor the sensor object that will be rotated.
     * @param angle the angle the sensor will be rotating towards.
     */
    private void rotateSensorLocation(RobotSensors sensor, double angle) {
        int cur_x = sensor.getPos().x;
        int cur_y = sensor.getPos().y;

        int newY, newX;
        newX = (int) Math.round((Math.cos(angle) * (cur_x - curLocation.x) - Math.sin(angle) * (cur_y - curLocation.y) + curLocation.x));
        newY = (int) Math.round((Math.sin(angle) * (cur_x - curLocation.x) - Math.cos(angle) * (cur_y - curLocation.y) + curLocation.y));
        sensor.setPos(newY, newX);
    }

    /**
     * Calculate and update the sensor position.
     * @param rowDiff the difference in distance of Point y, of the initial location to the new location of the robot.
     * @param colDiff the difference in distance of Point x, of the initial location to the new location of the robot.
     */
    private void setSensorPos(int rowDiff, int colDiff) {
        RobotSensors sensors;
        for (String sname : sensorList) {
            sensors = sensorMap.get(sname);
            sensors.setPos(sensors.getRow() + rowDiff, sensors.getCol() + colDiff);
        }
    }

    /**
     * CAMERA
     */

    /**
     * Initialize the camera In the following format
     * The robot is represented by a 3 x 3 cell space
     *  [X] [X] [X]
     *  [X] [X] [X] Camera >
     *  [X] [X] [X]
     */
    private void initCamera() {
        int row = curLocation.y;
        int col = curLocation.x;

        // Right Camera
        rightCam = new RobotCamera(RobotConstants.CAM_MIN, RobotConstants.CAM_MAX, row, col + 1, MapDirections.RIGHT);

        if (dir != MapDirections.UP) {
            rotateCamera(dir);
        }

        this.status = "Camera initialized\n";
    }

    /**
     * Rotate the camera.
     *
     * @param dir the direction the camera will be rotated towards.
     */
    private void rotateCamera(MapDirections dir) {
        double angle = Math.PI / 2;

        LOGGER.info("Rotating Camera...");

        switch (dir) {
            case DOWN:
                rotateCamera(MapDirections.RIGHT);
                rotateCamera(MapDirections.RIGHT);
                break;

            case LEFT:
                rightCam.setCameraDir(MapDirections.getAntiClockwise(rightCam.getCameraDir()));
                rotateCameraLocation(rightCam, angle);
                break;

            case RIGHT:
                rightCam.setCameraDir(MapDirections.getClockwise(rightCam.getCameraDir()));
                rotateCameraLocation(rightCam, -1*angle);
                break;

            default:
                LOGGER.warning("No rotation done. Wrong input direction: " + dir);
                break;
        }
        LOGGER.info("Camera Rotated");
    }

    /**
     * Update the new location of camera.
     *
     * @param rightCam the camera object that will be rotated.
     * @param angle the angle the sensor will be rotating towards.
     */
    private void rotateCameraLocation(RobotCamera rightCam, double angle) {
        int cur_x = rightCam.getPos().x;
        int cur_y = rightCam.getPos().y;

        int newY, newX;
        newX = (int) Math.round((Math.cos(angle) * (cur_x - curLocation.x) - Math.sin(angle) * (cur_y - curLocation.y) + curLocation.x));
        newY = (int) Math.round((Math.sin(angle) * (cur_x - curLocation.x) - Math.cos(angle) * (cur_y - curLocation.y) + curLocation.y));
        rightCam.setPos(newY, newX);
    }

    /**
     * Calculate and update the camera position.
     * @param rowDiff the difference in distance of Point y, of the initial location to the new location of the robot.
     * @param colDiff the difference in distance of Point x, of the initial location to the new location of the robot.
     */
    private void setCameraPos(int rowDiff, int colDiff) {
        rightCam.setPos(rightCam.getPos().y + rowDiff, rightCam.getPos().x + colDiff);
    }

    /**
     * Robot sensing surrounding obstacles for simulator
     *  @param exploredMap the current Map which is being explored
     * @param realMap the actual Map
     * @param sensorString Optional JSON string containing sensor data
     */
    public void sense(Map exploredMap, Map realMap, Optional<String> sensorString) {

        HashMap<String, Integer> sensorResult = sensorString.map(this::updateSensorResult)
                .orElseGet(() -> updateAllSensorResult(realMap));
        updateMap(exploredMap, realMap, sensorResult);

        if (!simulation && !fastestPath) {
            if (isExploration)
                send_android(exploredMap, NetworkConstants.MDF);
            if (!isExploration)
                send_android(exploredMap, allImageList);
        }
    }

    /**
     * update sensor result from simulation or RPI
     * @param realMap to be pass as a parameter to updateSensorResult.
     * @return SensorResult in hashMap
     */
    public HashMap<String, Integer> updateAllSensorResult(Map realMap) {
        HashMap<String, Integer> sensorResult;

        if (simulation) {
            sensorResult = updateSensorResult(realMap);
        } else {
//            String hardcode = "{\"com\":\"MDF\",\"fl\":-1,\"fm\":-1,\"fr\":-1,\"rt\":-1,\"rb\":-1,\"left\":-1}";
            netMgr.send(getArduinoCommand(RoboCmd.SEND_SENSORS, 0), NetworkConstants.EXPLORATION);
            String msg = netMgr.receive();
            System.out.println("sensor message = " + msg);
            sensorResult = updateSensorResult(msg);
//            sensorResult = updateSensorResult(hardcode);
        }
        return sensorResult;
    }

    /**
     * Getting sensor result from simulator
     *
     * @param realMap
     * @return HashMap<SensorId, ObsBlockDis>
     */
    public HashMap<String, Integer> updateSensorResult(Map realMap) {
        int obsBlock;

        for (String sname : sensorList) {
            obsBlock = sensorMap.get(sname).detect(realMap);
            sensorResult.put(sname, obsBlock);
        }

        return sensorResult;
    }

    /**
     * Getting sensor result from RPI/Arduino
     *
     * @return HashMap<SensorId, ObsBlockDis>
     */
    public HashMap<String, Integer> updateSensorResult(String msg) {

        HashMap<String, Integer> sensorRes = new HashMap<String, Integer>();

        JSONObject sensorResult = new JSONObject(new JSONTokener(msg));
        HashMap<String, Integer> sensorStrings = new HashMap<String, Integer>();
        sensorStrings.put("F1", (int) sensorResult.get("fl"));
        sensorStrings.put("F2", (int) sensorResult.get("fm"));
        sensorStrings.put("F3", (int) sensorResult.get("fr"));

        sensorStrings.put("R1", (int) sensorResult.get("rf"));
        sensorStrings.put("R2", (int) sensorResult.get("rb"));
        sensorStrings.put("L1", (int) sensorResult.get("left"));

        for (String sensorID : sensorStrings.keySet()) {
            int grid = sensorStrings.get(sensorID);
            if (grid >= sensorMap.get(sensorID).getMinRange() && grid <=sensorMap.get(sensorID).getMaxRange()) {
                sensorRes.put(sensorID, grid);
                this.sensorResult.put(sensorID, grid);
            } else {
                sensorRes.put(sensorID, -1);
                this.sensorResult.put(sensorID, -1);
            }
        }
        System.out.println(sensorRes);
        return sensorRes;
    }


    public void setAllImageList(HashMap<Point, String> imageList) {
        this.allImageList.putAll(imageList);
    }

    /**
     * Update the explore map
     * @param exploredMap
     * @param sensorResult
     */
    public void updateMap(Map exploredMap, Map realMap, HashMap<String, Integer> sensorResult) {
        int obsBlock;
        int rowInc = 0, colInc = 0, row, col;

        if (sensorResult == null) {
            LOGGER.warning("Invalid msg. Map not updated");
            return;
        }

        for (String sname : sensorList) {
            RobotSensors sensor = sensorMap.get(sname);
            obsBlock = sensorResult.get(sname);

            // Assign the rowInc and colInc based on sensor Direction
            switch (sensor.getSensorDir()) {
                case UP:
                    rowInc = 1;
                    colInc = 0;
                    break;

                case LEFT:
                    rowInc = 0;
                    colInc = -1;
                    break;

                case RIGHT:
                    rowInc = 0;
                    colInc = 1;
                    break;

                case DOWN:
                    rowInc = -1;
                    colInc = 0;
                    break;
            }

            for (int j = sensor.getMinRange(); j <= sensor.getMaxRange(); j++) {

                row = sensor.getRow() + rowInc * j;
                col = sensor.getCol() + colInc * j;

                // check whether the block is valid otherwise exit (Edge of Map)
                if (exploredMap.checkValidGrid(row, col)) {
                    exploredMap.getGrid(row, col).setExplored(true);
                    if (!exploredMap.getGrid(row, col).getMoveThru() && j == obsBlock) {
                        exploredMap.getGrid(row, col).setObstacles(true);

                        // CHECK IF IT HAS BEEN REMOVE BEFORE
//                        if (removeList.get(new Point(col, row)) != null)
//                            allImageList.put(new Point(col, row), removeList.get(new Point(col, row)));

                        exploredMap.setVirtualWall(exploredMap.getGrid(row, col), true);
                        exploredMap.reinitVirtualWall();

                        if (exploredMap.getGrid(row, col).getSensorName() == null)
                            exploredMap.getGrid(row, col).setSensorName(sname);

                        if (sname.equals("R2") && exploredMap.getGrid(row, col).getSensorName().equals("L1"))
                            exploredMap.getGrid(row, col).setSensorName(sname);
                        else if ((exploredMap.getGrid(row, col).getSensorName().equals("L1") || exploredMap.getGrid(row, col).getSensorName().equals("R2")))
                            exploredMap.getGrid(row, col).setSensorName(sname);
                        else if ((sname.equals("F1") || sname.equals("F2") || sname.equals("F3") || sname.equals("R1"))
                                && (exploredMap.getGrid(row, col).getSensorName().equals("L1") || exploredMap.getGrid(row, col).getSensorName().equals("R2"))
                            )
                            exploredMap.getGrid(row, col).setSensorName(sname);
                            break;

                    } else if (j != obsBlock && exploredMap.getGrid(row, col).isObstacles()) {
                        // j = 5 obsBlock = 4 will not occur, it should've break in previous senario
                        // j = 1,2,3,4 obsBlock = 5 might occur
                        // if is obstacles and was previously set by left sensor, then we can reset it to false

                        if ((
                            (exploredMap.getGrid(row, col).getSensorName().equals("L1") || exploredMap.getGrid(row, col).getSensorName().equals("R2"))
                                || (sname.equals("R1") || sname.equals("F1") || sname.equals("F2") || sname.equals("F3")))
                                || (exploredMap.getGrid(row, col).getSensorName().equals(sname))
                        ) {
                            exploredMap.getGrid(row, col).setObstacles(false);
                            exploredMap.getGrid(row, col).setExplored(true);
                            exploredMap.getGrid(row, col).setSensorName(sname);
                            exploredMap.setVirtualWall(exploredMap.getGrid(row, col), false);
                            exploredMap.reinitVirtualWall();

                            // IF it happens that a phantom block exist, and it detects image, then remove it...
                            // THIS MIGHT HAVE COMPLICATION.. IF ITS NOT PHANTOM BLOCK, THEN IMAGE WONT BE TAKEN A SECOND TIME, CHANCES ARE LOW THOU....
//                            reinitImage(new Point(row, col), exploredMap);
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void initSurfaceList(Map currentMap){
        allPossibleSurfaces = getAllPossibleSurfaces(currentMap);
        notYetTaken = getUntakenSurfaces(currentMap);
    }

    public HashMap<String, MapObjectSurface> getUntakenSurfaces(Map currentMap) {
        HashMap<String, MapObjectSurface> notYetTaken = getAllPossibleSurfaces(currentMap);
        System.out.println(notYetTaken);
        for (String takenSurface : surfTakenMap.keySet())
            if (notYetTaken.containsKey(takenSurface)) {
                notYetTaken.remove(takenSurface);
            } else {
                System.out.println("impossible surface = " + surfTakenMap.get(takenSurface));
                LOGGER.warning("Surface taken not in all possible surfaces. Please check. \n");
            }

        return notYetTaken;
    }



    public void reinitImage(Point pt, Map currentMap) {
        removeList.put(pt, allImageList.get(pt));
        allImageList.remove(pt);

        if (surfTakenMap.containsValue(new MapObjectSurface(pt, MapDirections.UP)))
            surfTakenMap.remove((new MapObjectSurface(pt, MapDirections.UP)).toString());
        if (surfTakenMap.containsValue(new MapObjectSurface(pt, MapDirections.DOWN)))
            surfTakenMap.remove((new MapObjectSurface(pt, MapDirections.DOWN)).toString());
        if (surfTakenMap.containsValue(new MapObjectSurface(pt, MapDirections.LEFT)))
            surfTakenMap.remove((new MapObjectSurface(pt, MapDirections.LEFT)).toString());
        if (surfTakenMap.containsValue(new MapObjectSurface(pt, MapDirections.RIGHT)))
            surfTakenMap.remove((new MapObjectSurface(pt, MapDirections.RIGHT)).toString());

        allPossibleSurfaces = getAllPossibleSurfaces(currentMap);
        notYetTaken = getUntakenSurfaces(currentMap);
    }

    public boolean isStartOrEndPoint(int row, int col) {
        return ((row >= MapConstants.MAP_LENGTH-3 && col >= MapConstants.MAP_WIDTH-3) || (row <= 2 && col <= 2));
    }

    //cam
    public MapObjectSurface addToSurfaceTaken(String sensorName, int rowInc, int colInc) {
        int tempSensorRow, tempSensorCol, tempSensorReading;

        tempSensorReading =  getSensorResult().get(sensorName);

        if (tempSensorReading > 0 && tempSensorReading <= RobotConstants.CAM_MAX) {
            tempSensorRow = getSensorMap().get(sensorName).getRow();
            tempSensorCol = getSensorMap().get(sensorName).getCol();
            MapObjectSurface tempObsSurface = internalAddToSurfaceTaken(tempSensorRow, tempSensorCol, rowInc, colInc, tempSensorReading);
            System.out.println("add to surface taken");
            return tempObsSurface;
        } else {
            return null;
        }

    }

    //cam
    public MapObjectSurface internalAddToSurfaceTaken(int tempRow, int tempCol, int rowInc, int colInc, int incStep) {
        int tempObsRow, tempObsCol;

        MapObjectSurface tempObsSurface;
        MapDirections tempSurface;

        tempObsRow = tempRow + rowInc * incStep;
        tempObsCol = tempCol + colInc * incStep;

        tempSurface = MapDirections.getOpposite(getRightCam().getCameraDir());

        tempObsSurface = new MapObjectSurface(tempObsRow, tempObsCol, tempSurface);
        surfTakenMap.put(tempObsSurface.toString(), tempObsSurface);
        System.out.println(tempObsSurface.toString());

        return tempObsSurface;
    }

    public void updateNotYetTaken(ArrayList<MapObjectSurface> surfTaken) {
        for (MapObjectSurface obsSurface : surfTaken) {
            if (notYetTaken.containsKey(obsSurface.toString())) {
                notYetTaken.remove(obsSurface.toString());
                LOGGER.info("Remove from not yet taken: " + obsSurface);
            }
        }
    }

    public HashMap<String, MapObjectSurface> getAllPossibleSurfaces(Map currentMap) {
        MapGrid tempCell;
        MapGrid temp;
        MapObjectSurface tempObsSurface;
        HashMap<MapDirections, MapGrid> tempNeighbours;
        HashMap<String, MapObjectSurface> allPossibleSurfaces = new HashMap<String, MapObjectSurface>();

        for (int row = 0; row < MapConstants.MAP_LENGTH; row++) {
            for (int col = 0; col < MapConstants.MAP_WIDTH; col++) {
                tempCell = currentMap.getGrid(row, col);

                if (tempCell.isObstacles()) {
                    tempNeighbours = currentMap.getNeighboursMap(tempCell, currentMap);

                    for (MapDirections neighbourDir : tempNeighbours.keySet()) {
                        temp = tempNeighbours.get(neighbourDir);

                        if (!temp.isObstacles()) {
                            tempObsSurface = new MapObjectSurface(tempCell.getPos(), neighbourDir);
                            allPossibleSurfaces.put(tempObsSurface.toString(), tempObsSurface);
                        }
                    }
                }

            }
        }
        return allPossibleSurfaces;
    }

    /**
     * HELPERS
     */
    @Override
    public String toString() {
        String s = String.format("Robot at %s facing %s\n", curLocation.toString(), dir.toString());
        return s;
    }

    /**
     * Convert Robot Command into Arduino Command
     * @param cmd an enum from RoboCmd
     * @param steps number of steps to take
     * @return
     */
    public String getArduinoCommand(RoboCmd cmd, int steps) {
        StringBuilder cmdStr = new StringBuilder();

        cmdStr.append(RoboCmd.ArduinoCtrl.values()[cmd.ordinal()]);

        if (steps > 0 && fastestPath) {
            cmdStr.append(steps);
            cmdStr.append(',');
        }
        return cmdStr.toString();
    }

    /**
     *  Convert Json Object into Point
     * @param jsonMsg a String in json format
     * @return The Start Point
     */
    public Point parseStartPointJson(String jsonMsg) {
        System.out.println(jsonMsg);
        if (jsonMsg.contains(NetworkConstants.START_POINT_KEY)) {
            JSONObject startPointJson = new JSONObject(new JSONTokener(jsonMsg));
            JSONArray start = startPointJson.getJSONArray(NetworkConstants.START_POINT_KEY);
//            System.out.println(start.get(0));
//            System.out.println(start.get(1));
//            System.out.println(start.get(2));
            System.out.printf("start point: x:%d y:%d deg:%d \n" ,start.get(0),start.get(1),start.get(2));

            int x =  (int) start.get(0);
            int y =  (int) start.get(1);
            int deg = (int)  start.get(2);

            Point startPoint = new Point(x, y);

            if (deg == 0) {
                this.dir = MapDirections.UP;
            }else if (deg == 90){
                this.dir = MapDirections.RIGHT;
            }else if (deg == 180){
                this.dir = MapDirections.DOWN;
            }else if (deg == 270){
                this.dir = MapDirections.LEFT;
            }

            return startPoint;
        } else {
            LOGGER.warning("Not a start point msg. Return null.");
            return null;
        }
    }
    /**
     *  Convert Json Object into Point
     * @param jsonMsg a String in json format
     * @return The Start Point
     */
    public MapDirections parseDirection(String jsonMsg) {
            JSONObject startPointJson = new JSONObject(new JSONTokener(jsonMsg));
            JSONArray start = startPointJson.getJSONArray(NetworkConstants.START_POINT_KEY);
            int deg = (int)  start.get(2);

            if (deg == 0) {
                dir = MapDirections.UP;
            }else if (deg == 90){
                dir = MapDirections.RIGHT;
            }else if (deg == 180){
                dir = MapDirections.DOWN;
            }else if (deg == 270){
                dir = MapDirections.LEFT;
            }

            return dir;
    }

    /**
     * Convert Json Object into Point
     * @param jsonMsg a String in json format
     * @return The WayPoint
     */
    public Point parseWayPointJson(String jsonMsg) {
        if (jsonMsg.contains(NetworkConstants.WAY_POINT_KEY)) {
            JSONObject wayPointJson = new JSONObject(new JSONTokener(jsonMsg));
            JSONArray waypoint = wayPointJson.getJSONArray(NetworkConstants.WAY_POINT_KEY);
//            System.out.println(waypoint.get(0));
//            System.out.println(waypoint.get(1));

            System.out.printf("way point: x:%d y:%d \n" ,waypoint.get(0), waypoint.get(1));
            int x = (int) waypoint.get(0);
            int y = (int) waypoint.get(1);

            Point wayPoint = new Point(x, y);

            return wayPoint;

        } else {
            LOGGER.warning("Not a way point msg. Return null.");
            return null;
        }
    }

    public boolean isWallOnLeft() {
        Point L1_pos = sensorMap.get("L1").getPos();
        Point F2_pos = sensorMap.get("F2").getPos();

        if ((L1_pos.x == 0 && F2_pos.x != 0) || (L1_pos.x == MapConstants.MAP_WIDTH - 1 && F2_pos.x != MapConstants.MAP_WIDTH - 1)
                || (L1_pos.y == 0 && F2_pos.y != 0) || (L1_pos.y == MapConstants.MAP_LENGTH - 1 && F2_pos.y != MapConstants.MAP_LENGTH - 1)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Robot is right hugging the wall if the right sensor position is equal to
     * the lowest or highest possible row or col number
     *
     * @return
     */
    public boolean isWallOnRight() {
        Point R1_pos = sensorMap.get("R1").getPos();
        Point R2_pos = sensorMap.get("R2").getPos();

        if ((R1_pos.x == 0 && R2_pos.x == 0)
                || (R1_pos.x == MapConstants.MAP_WIDTH - 1 && R2_pos.x == MapConstants.MAP_WIDTH - 1)
                || (R1_pos.y == 0 && R2_pos.y == 0)
                || (R1_pos.y == MapConstants.MAP_LENGTH - 1 && R2_pos.y == MapConstants.MAP_LENGTH - 1)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isWallOnFront() {
        Point F1_pos = sensorMap.get("F1").getPos();
        Point F2_pos = sensorMap.get("F2").getPos();
        Point F3_pos = sensorMap.get("F3").getPos();

        if ((F1_pos.x == 0 && F2_pos.x == 0 && F3_pos.x == 0)
                || (F1_pos.x == MapConstants.MAP_WIDTH - 1 && F2_pos.x == MapConstants.MAP_WIDTH - 1 && F3_pos.x == MapConstants.MAP_WIDTH - 1)
                || (F1_pos.y == 0 && F2_pos.y == 0 && F3_pos.y == 0)
                || (F1_pos.y == MapConstants.MAP_LENGTH - 1  && F2_pos.y == MapConstants.MAP_LENGTH - 1 && F3_pos.y == MapConstants.MAP_LENGTH - 1)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isObjOnFront(Map currentMap) {
        if(isWallOnFront())
            return true;

        Point F1_pos = sensorMap.get("F1").getPos();
        Point F2_pos = sensorMap.get("F2").getPos();
        Point F3_pos = sensorMap.get("F3").getPos();

        MapGrid F1, F2, F3;

        int F1Y = F1_pos.y;
        int F2Y = F2_pos.y;
        int F3Y = F3_pos.y;

        int F1X = F1_pos.x;
        int F2X = F2_pos.x;
        int F3X = F3_pos.x;

        switch (dir) {
            case UP:
                F1Y += 1;
                F2Y += 1;
                F3Y += 1;
                break;
            case DOWN:
                F1Y -= 1;
                F2Y -= 1;
                F3Y -= 1;
                break;
            case LEFT:
                F1X -= 1;
                F2X -= 1;
                F3X -= 1;
                break;
            case RIGHT:
                F1X += 1;
                F2X += 1;
                F3X += 1;
                break;
        }

        if (currentMap.checkValidGrid(F1Y, F1X) && currentMap.checkValidGrid(F2Y, F2X) && currentMap.checkValidGrid(F3Y, F3X)) {
            F1 = currentMap.getGrid(F1Y, F1X);
            F2 = currentMap.getGrid(F2Y, F2X);
            F3 = currentMap.getGrid(F3Y, F3X);
            if (F1.isObstacles() && F2.isObstacles() && F3.isObstacles()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean isBlockOnFront(Map currentMap) {
        Point F1_pos = sensorMap.get("F1").getPos();
        Point F2_pos = sensorMap.get("F2").getPos();
        Point F3_pos = sensorMap.get("F3").getPos();

        MapGrid F1, F2, F3;

        int F1Y = F1_pos.y;
        int F2Y = F2_pos.y;
        int F3Y = F3_pos.y;

        int F1X = F1_pos.x;
        int F2X = F2_pos.x;
        int F3X = F3_pos.x;

        switch (dir) {
            case UP:
                F1Y += 1;
                F2Y += 1;
                F3Y += 1;
                break;
            case DOWN:
                F1Y -= 1;
                F2Y -= 1;
                F3Y -= 1;
                break;
            case LEFT:
                F1X -= 1;
                F2X -= 1;
                F3X -= 1;
                break;
            case RIGHT:
                F1X += 1;
                F2X += 1;
                F3X += 1;
                break;
        }

        if (currentMap.checkValidGrid(F1Y, F1X) && currentMap.checkValidGrid(F2Y, F2X) && currentMap.checkValidGrid(F3Y, F3X)) {
            F1 = currentMap.getGrid(F1Y, F1X);
            F2 = currentMap.getGrid(F2Y, F2X);
            F3 = currentMap.getGrid(F3Y, F3X);
            if (F1.isObstacles() && F2.isObstacles() && F3.isObstacles()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean isObjOnRight(Map currentMap) {
        if (isWallOnRight())
            return true;

        RobotSensors R1 = sensorMap.get("R1");
        RobotSensors R2 = sensorMap.get("R2");

        Point R1_pos = R1.getPos();
        Point R2_pos = R2.getPos();

        MapDirections sensorDir = R1.getSensorDir();

        MapGrid R1grid, R2grid;

        int R1Y = R1_pos.y;
        int R1X = R1_pos.x;

        int R2Y = R2_pos.y;
        int R2X = R2_pos.x;

        switch (sensorDir){
            case UP:
                R1Y += 1;
                R2Y += 1;
                break;
            case DOWN:
                R1Y -= 1;
                R2Y -= 1;
                break;
            case LEFT:
                R1X -= 1;
                R2X -= 1;
                break;
            case RIGHT:
                R1X += 1;
                R2X += 1;
                break;
        }

        if(currentMap.checkValidGrid(R1Y, R1X) && currentMap.checkValidGrid(R2Y, R2X)) {
            R1grid = currentMap.getGrid(R1Y ,R1X);
            R2grid = currentMap.getGrid(R2Y ,R2X);
            if (R1grid.isObstacles() && R2grid.isObstacles()){
                return true ;
            } else {
                return false;
            }
        }
        return false;
    }

    public JSONArray getRobotArray() {

        JSONArray robotArray = new JSONArray();
        JSONObject robotJson = new JSONObject()
                .put("x", curLocation.x + 1)
                .put("y", curLocation.y + 1)
                .put("direction", dir.toString().toLowerCase());
        robotArray.put(robotJson);
        return robotArray;
    }

    public JSONArray getMapArray(Map exploredMap) {
        String obstacleString = MDF.generateMDFString2(exploredMap);
        JSONArray mapArray = new JSONArray();
        JSONObject mapJson = new JSONObject()
                .put("explored", MDF.generateMDFString1(exploredMap))
                .put("obstacle", obstacleString)
                .put("length", obstacleString.length() * 4);
        mapArray.put(mapJson);
        return mapArray;
    }

    public JSONArray getStatusArray() {
        JSONArray statusArray = new JSONArray();
        JSONObject statusJson = new JSONObject()
                .put("status", status.replaceAll("\\n", ""));
        statusArray.put(statusJson);
        return statusArray;
    }

    /**
     * Send the current robot position/direction and status (if uncomment) to android

    public void send_android(String purpose) {
        JSONObject androidJson = new JSONObject();

        androidJson.put("robot", getRobotArray());
        androidJson.put("status", getStatusArray());
        netMgr.send(androidJson.toString() + "\n", purpose);

    }
     */

    /**
     * Send the current explored map and robot position/direciton, status (if uncomment) to android
     *
     * @param exploredMap
    */
    public void send_android(Map exploredMap, String purpose) {
        String objMDF = MDF.generateMDFString2(exploredMap);
        String expMDF = MDF.generateMDFString1(exploredMap);

        int xPos = curLocation.x;
        int yPos = curLocation.y;
        int degree = 0;

        MapDirections robotDir = dir;

        switch (robotDir){
            case UP:
                degree = 0;
                break;
            case DOWN:
                degree = 180;
                break;
            case LEFT:
                degree = 270;
                break;
            case RIGHT:
                degree = 90;
                break;

        }

        int[] status = {xPos, yPos, degree};
        String[][] imgs = {};

        netMgr.send(NetworkConstants.MDF, expMDF, objMDF, status, imgs);
    }

    public void send_android(Map exploredMap, HashMap<Point, String> imageList) {
        String objMDF = MDF.generateMDFString2(exploredMap);
        String expMDF = MDF.generateMDFString1(exploredMap);

        int xPos = curLocation.x;
        int yPos = curLocation.y;
        int degree = 0;

        MapDirections robotDir = dir;

        switch (robotDir){
            case UP:
                degree = 0;
                break;
            case DOWN:
                degree = 180;
                break;
            case LEFT:
                degree = 270;
                break;
            case RIGHT:
                degree = 90;
                break;

        }

        String[][] imgs = new String[imageList.size()][3];

        int i = 0;
        for (Point imgPoint : imageList.keySet()) {
            imgs[i][0] = Integer.toString(imgPoint.x);
            imgs[i][1] = Integer.toString(imgPoint.y);
            imgs[i][2] = imageList.get(imgPoint);
            i++;
        }

        int[] status = {xPos, yPos, degree};

        netMgr.send(NetworkConstants.MDF, expMDF, objMDF, status, imgs);
    }

}
