package Algorithm;

//Map
import Map.Map;
import Map.MapDirections;
import Map.MapGrid;
import Map.MapConstants;
import Map.MapObjectSurface;

//Network
import Network.NetworkManager;
import Network.NetworkConstants;

//Robot
import Robot.Robot;
import Robot.RoboCmd;
import Robot.RobotConstants;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.logging.Logger;

public class ImageRecognition {

    private static final Logger LOGGER = Logger.getLogger(ImageRecognition.class.getName());
    private static int imagesTakenCounter = 0;

    private boolean simulation;
    private int stepsPerSecond = 30;
    private boolean doingIsland = false;

    private HashSet<String> imageHashSet = new HashSet<String>();

    //Map
    private Map realMap;
    private Map currentMap;

    //Robot
    private Robot robot;
    private Point startPos;
    private int prevLocY;
    private int prevLocX;
    private MapDirections prevDir = MapDirections.UP;

    private ArrayList<RoboCmd> movement = new ArrayList<RoboCmd>();

    private int stepPerSecond;

    //Time
    private int timeLimit;
    private long startTime;
    private long stopTime;
    private double explorationLimit;
    private double exploredPercentage;

    //If it completed one round using right wall hugging
    private boolean backToStart = false;

    //Image
    private int totalImage = 5;

    //tmpGrid
    private MapGrid tmpGrid;

    //network manager
    private static final NetworkManager netMgr = NetworkManager.getInstance();

    //Getters and Setters

    public boolean isSimulation() {
        return simulation;
    }

    public void setSimulation(boolean simulation) {
        this.simulation = simulation;
    }

    public Map getRealMap() {
        return realMap;
    }

    public void setRealMap(Map realMap) {
        this.realMap = realMap;
    }

    public Map getcurrentMap() {
        return currentMap;
    }

    public void setcurrentMap(Map currentMap) {
        this.currentMap = currentMap;
    }

    public Robot getRobot() {
        return robot;
    }

    public void setRobot(Robot robot) {
        this.robot = robot;
    }

    public Point getStartPos() {
        return startPos;
    }

    public void setStartPos(Point startPos) {
        this.startPos = startPos;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public boolean isBackToStart() {
        return backToStart;
    }

    public void setBackToStart(boolean backToStart) {
        this.backToStart = backToStart;
    }

    //Constructor
    public ImageRecognition(boolean simulation, Map currentMap, Map realMap, Robot robot, Point startPos, double explorationLimit, int timeLimit, int stepPerSecond, boolean doingIsland) {
        this.simulation = simulation;
        this.currentMap = currentMap;
        this.realMap = realMap;
        this.robot = robot;
        this.startPos = startPos;
        this.timeLimit = timeLimit;
        this.explorationLimit = explorationLimit;
        this.stepPerSecond = stepPerSecond;
        this.doingIsland = doingIsland;

        this.prevLocX = startPos.x;
        this.prevLocY = startPos.y;

        robot.setExploration(false);
    }

    public void startImageRecognition() throws InterruptedException {
        startTime = System.currentTimeMillis();
        stopTime = startTime + timeLimit;

        exploredPercentage = 0;//currentMap.getPercentageExplored();

        while ((System.currentTimeMillis() < stopTime + 60000) && !backToStart) {
            exploredPercentage = currentMap.getPercentageExplored();

            antiStuck();
            calibrate_every_x_steps();
            rightWallHug(true);
        }

        System.out.println(robot.getSurfTakenMap());
        removeFakeVirtualWall();

        if (doingIsland){
            robot.initSurfaceList(currentMap); // initialize all possible surface and untaken surface.

            while (robot.getNotYetTaken().size() > 0 && (System.currentTimeMillis() < stopTime)) {
                System.out.println(System.currentTimeMillis());
                System.out.println(stopTime);
                System.out.println(timeLimit);
                System.out.println(stopTime - System.currentTimeMillis());
                System.out.println("Printing time");
                imageLoop();
                antiStuck();
            }
            goToPoint(startPos, false);
        }

         System.out.println("not yet taken:" + robot.getNotYetTaken());
         System.out.println("All surfaces:" + robot.getAllPossibleSurfaces(currentMap));
         System.out.println("Image Hash Set:" + imageHashSet);
         System.out.println("END");

    }

    public boolean nextToWall(){
        if (robot.getCurLocation().x == 1 || robot.getCurLocation().x == 13 ||
                robot.getCurLocation().y == 1 || robot.getCurLocation().y == 18)
            return true;
        return false;
    }

    public void antiStuck() throws InterruptedException {
        if (nextToWall()) {
            prevLocX = robot.getCurLocation().x;
            prevLocY = robot.getCurLocation().y;
            if (robot.getCurLocation().x == 1)
                prevDir = MapDirections.DOWN;
            if (robot.getCurLocation().x == 13)
                prevDir = MapDirections.UP;
            if (robot.getCurLocation().y == 1)
                prevDir = MapDirections.RIGHT;
            if (robot.getCurLocation().y == 18)
                prevDir = MapDirections.LEFT;
        }

        if (stuckInLoop()) {
            goToPoint(new Point(prevLocX, prevLocY), false);
            turn(prevDir);
            movement.clear();
        }
    }

    public void calibrate_every_x_steps() throws InterruptedException{

        if(simulation)
            return;

        int tmpx = robot.getCurLocation().x;
        int tmpy = robot.getCurLocation().y;

        if (robot.isObjOnFront(currentMap) && robot.isObjOnRight(currentMap) && (
                (tmpx == 1 && tmpy == 1) || (tmpx == 13 && tmpy == 1) ||(tmpx == 1 && tmpy == 18) || (tmpx == 13 && tmpy == 18)
        )) {
            robot.turn(RoboCmd.RIGHT_TURN, 1);
            robot.sense(currentMap, realMap, Optional.empty());
            align("F");
            robot.turn(RoboCmd.LEFT_TURN, 1);
            robot.sense(currentMap, realMap, Optional.empty());
            robot.setMoveCounter(0);
        }

        if (robot.getMoveCounter() >= 7){
            if (robot.isObjOnFront(currentMap)) {
                align("F");
                robot.setMoveCounter(0);
            }
            if (robot.isObjOnRight(currentMap)) {
                robot.turn(RoboCmd.RIGHT_TURN, 1);
                robot.sense(currentMap, realMap, Optional.empty());
                align("F");
                robot.turn(RoboCmd.LEFT_TURN, 1);
                robot.sense(currentMap, realMap, Optional.empty());
                align("R");
                robot.setMoveCounter(0);
            }
        }
    }

    public boolean stuckInLoop(){
        if (movement.size() >= 4)
            if (movement.get(movement.size() - 1) == RoboCmd.FORWARD &&
                    movement.get(movement.size() - 2) == RoboCmd.RIGHT_TURN &&
                    movement.get(movement.size() - 3) == RoboCmd.FORWARD &&
                    movement.get(movement.size() - 4) == RoboCmd.RIGHT_TURN)
                return true;
        return false;
    }

    public void turn(MapDirections prevDir) throws InterruptedException {
        MapDirections robotDir = robot.getDir();

        if (prevDir.equals(robotDir))
            return;

        if (MapDirections.getClockwise(robotDir).equals(prevDir)) {
            robot.turn(RoboCmd.RIGHT_TURN, 1);
        } else if (MapDirections.getAntiClockwise(robotDir).equals(prevDir)) {
            robot.turn(RoboCmd.LEFT_TURN, 1);

        } else if (MapDirections.getOpposite(robotDir).equals(prevDir)) {
            robot.turn(RoboCmd.LEFT_TURN, 1);
            robot.sense(currentMap, realMap, Optional.empty());
            calibrate();
            robot.turn(RoboCmd.LEFT_TURN, 1);
        }
        robot.sense(currentMap, realMap, Optional.empty());
        calibrate();
    }

    public void calibrate(){
        if (robot.isObjOnFront(currentMap))
            align("F");
        if (robot.isObjOnRight(currentMap))
            align("R");
    }

    public void align(String cmd) {

        System.out.println("aligning " + cmd + " at " + robot.getCurLocation());

        if (!simulation) {
            netMgr.send(cmd, "Ex");
            String msg;
            do {
                msg = netMgr.receive();
            } while (!msg.contains(NetworkConstants.CALI_FIN));
        }
    }

    public void rightWallHug(boolean addToSurfaceTaken) throws InterruptedException{
//        String msg;

        if (movable(MapDirections.getClockwise(robot.getDir())))
        {
            robot.turn(RoboCmd.RIGHT_TURN, stepsPerSecond);
            robot.sense(currentMap, realMap, Optional.empty());
            calibrate();
            movement.add(RoboCmd.RIGHT_TURN);

            if (addToSurfaceTaken)
                takeImage();

            moveForward(addToSurfaceTaken);
        }
        else if (movable(robot.getDir()))
        {
            robot.move(RoboCmd.FORWARD, 1, currentMap, stepsPerSecond);
            robot.sense(currentMap, realMap, Optional.empty());
            calibrate();
            movement.add(RoboCmd.FORWARD);
            robot.setMoveCounter(robot.getMoveCounter()+1);

            if (addToSurfaceTaken)
                takeImage();

            if(robot.getCurLocation().x == 1 &&  robot.getCurLocation().y == 1)
                backToStart = true;
        }
        else if (movable(MapDirections.getAntiClockwise(robot.getDir())))
        {
            robot.turn(RoboCmd.LEFT_TURN, stepsPerSecond);
            robot.sense(currentMap, realMap, Optional.empty());
            calibrate();
            movement.add(RoboCmd.LEFT_TURN);

            if (addToSurfaceTaken)
                takeImage();

        } else
        {
            robot.turn(RoboCmd.LEFT_TURN, stepsPerSecond);
            robot.sense(currentMap, realMap, Optional.empty());
            calibrate();
            movement.add(RoboCmd.LEFT_TURN);

            if (addToSurfaceTaken)
                takeImage();

            robot.turn(RoboCmd.LEFT_TURN, stepsPerSecond);
            robot.sense(currentMap, realMap, Optional.empty());
            calibrate();
            movement.add(RoboCmd.LEFT_TURN);

            if (addToSurfaceTaken)
                takeImage();
        }

    }

    public void moveForward(boolean takingImage) throws InterruptedException {
//        String msg;
        Optional<String> sensorString;
        if (movable(robot.getDir())) {
            sensorString = robot.move(RoboCmd.FORWARD, 1, currentMap, stepsPerSecond);
            robot.sense(currentMap, realMap, sensorString);
            calibrate();
            movement.add(RoboCmd.FORWARD);
            robot.setMoveCounter(robot.getMoveCounter()+1);

            if(takingImage)
                takeImage();

            if (robot.getCurLocation().x == 1 && robot.getCurLocation().y == 1)
                backToStart = true;
        } else {
            sensorString = robot.turn(RoboCmd.LEFT_TURN,1);
            robot.sense(currentMap, realMap, sensorString);
            calibrate();
            movement.add(RoboCmd.LEFT_TURN);
            if(takingImage)
                takeImage();
        }
    }

    public void takeImage() {
        if (!robot.isWallOnRight()) {
            imageRecognitionRight(currentMap);
        }
    }

    public ArrayList<MapObjectSurface> imageRecognitionRight(Map currentMap) {
        int camera_row = robot.getRightCam().getPos().y;
        int camera_col = robot.getRightCam().getPos().x;

        boolean hasObstacleAtCamDir = false;
        boolean takeImageFlag = false;

        int rowInc = 0, colInc = 0;
        int temp_row, temp_col;

        ArrayList<MapObjectSurface> curSurfaceList = new ArrayList<MapObjectSurface>();
        MapObjectSurface tempObstacleSurface;

        MapDirections camDir = robot.getRightCam().getCameraDir();

        switch (camDir) {
            case UP:
                rowInc = 1;
                colInc = 0;
                break;
            case DOWN:
                rowInc = -1;
                colInc = 0;
                break;
            case LEFT:
                colInc = -1;
                rowInc = 0;
                break;
            case RIGHT:
                colInc = 1;
                rowInc = 0;
                break;
        }

        int camRange;

        //check if obstacle is present where the camera is facing. If yes, obtain the range of obstacle from camera.
        for (camRange = RobotConstants.CAM_MIN; camRange <= RobotConstants.CAM_MAX; camRange++) {
            temp_row = camera_row + rowInc * camRange;
            temp_col = camera_col + colInc * camRange;

            if (currentMap.checkValidGrid(temp_row, temp_col)) {
                MapGrid temp_cell = currentMap.getGrid(temp_row, temp_col);
                if (temp_cell.getExplored() && temp_cell.isObstacles()) {
                    hasObstacleAtCamDir = true;
                    break;
                }
            } else {
                break;

            }
        }

        String msg = String.format("I%d|%d|%s", camera_col, camera_row, camDir.toString());
        if (!imageHashSet.contains(msg)) {
            imageHashSet.add(msg);

            // update surfaceTaken
            // R1
            tempObstacleSurface = robot.addToSurfaceTaken("R1", rowInc, colInc);
            if (tempObstacleSurface != null) {
                System.out.println("Object detected by R1");
                curSurfaceList.add(tempObstacleSurface);
            }
            // R2
            tempObstacleSurface = robot.addToSurfaceTaken("R2", rowInc, colInc);
            if (tempObstacleSurface != null) {
                System.out.println("Object detected by R2");
                curSurfaceList.add(tempObstacleSurface);
            }
            // camera
            if (hasObstacleAtCamDir) {
                tempObstacleSurface = robot.internalAddToSurfaceTaken(camera_row, camera_col, rowInc, colInc, camRange);
                if (tempObstacleSurface != null) {
                    System.out.println("Object detected by CAM");
                    curSurfaceList.add(tempObstacleSurface);
                }
            }

            // Check if surface already taken
            if (!simulation && (curSurfaceList.size() > 0)){
//            if ((curSurfaceList.size() > 0)){
                for (int i = 0; i < curSurfaceList.size(); i++) {
                    System.out.println(curSurfaceList.get(i).getPos().x + " " + curSurfaceList.get(i).getPos().y + " " + curSurfaceList.get(i).getSurface());
                    MapGrid temp_cell = currentMap.getGrid(curSurfaceList.get(i).getPos().y, curSurfaceList.get(i).getPos().x);
                    if (!isSurfaceTaken(temp_cell, curSurfaceList.get(i).getSurface())){
                        takeImageFlag = true;
                        updateSurfaceTaken(currentMap, curSurfaceList.get(i));
                    }
                }
            }

            if (takeImageFlag) {
                saveImage();
            }
        }
        return curSurfaceList;
    }

    public void updateSurfaceTaken(Map currentMap, MapObjectSurface tempObstacleSurface){
        if (tempObstacleSurface.getSurface() == MapDirections.RIGHT){
            currentMap.getGrid(tempObstacleSurface.getPos().y, tempObstacleSurface.getPos().x).setSurfaceRightTaken(true);
        }
        if (tempObstacleSurface.getSurface() == MapDirections.LEFT){
            currentMap.getGrid(tempObstacleSurface.getPos().y, tempObstacleSurface.getPos().x).setSurfaceLeftTaken(true);
        }
        if (tempObstacleSurface.getSurface() == MapDirections.UP){
            currentMap.getGrid(tempObstacleSurface.getPos().y, tempObstacleSurface.getPos().x).setSurfaceUpTaken(true);
        }
        if (tempObstacleSurface.getSurface() == MapDirections.DOWN){
            currentMap.getGrid(tempObstacleSurface.getPos().y, tempObstacleSurface.getPos().x).setSurfaceDownTaken(true);
        }
    }

    public boolean isSurfaceTaken(MapGrid mapGrid, MapDirections mapDirections){
        if (mapDirections == MapDirections.UP){
            return mapGrid.isSurfaceUpTaken();
        }
        if (mapDirections == MapDirections.DOWN){
            return mapGrid.isSurfaceDownTaken();
        }
        if (mapDirections == MapDirections.RIGHT){
            return mapGrid.isSurfaceRightTaken();
        }
        if (mapDirections == MapDirections.LEFT){
            return mapGrid.isSurfaceLeftTaken();
        }
        return true;
    }

    public void saveImage() {

        imagesTakenCounter++;
        System.out.println("imagesTakenCounter: " + imagesTakenCounter);


        HashMap<String, Integer> sensorRes = new HashMap<String, Integer>();

        netMgr.send("I", NetworkConstants.EXPLORATION);
        String msg = netMgr.receive();
        MapDirections camDir = robot.getRightCam().getCameraDir();

        int rowInc=0, colInc=0;

        switch (camDir) {
            case UP:
                rowInc = 1;
                colInc = 0;
                break;
            case DOWN:
                rowInc = -1;
                colInc = 0;
                break;
            case LEFT:
                colInc = -1;
                rowInc = 0;
                break;
            case RIGHT:
                colInc = 1;
                rowInc = 0;
                break;
        }

        if (msg.contains(NetworkConstants.IMAGE)) {
            JSONObject camera = new JSONObject(new JSONTokener(msg));

            Point R1_pos = robot.getSensorMap().get("R1").getPos();
            Point robot_pos = robot.getCurLocation();
            Point R2_pos = robot.getSensorMap().get("R2").getPos();

            HashMap<Point, String> imageList = new HashMap<Point, String>();

            if (!camera.get("left").toString().equals("0")) {
                if (currentMap.checkValidGrid(R1_pos.y + rowInc,R1_pos.x + colInc))
                    if (currentMap.getGrid(R1_pos.y + rowInc,R1_pos.x + colInc ).isObstacles())
                        imageList.put(new Point(R1_pos.x + colInc, R1_pos.y + rowInc), camera.get("left").toString());
            }
            if (!camera.get("middle").toString().equals("0"))
                if (currentMap.checkValidGrid(robot_pos.y+(2*rowInc),robot_pos.x+(2*colInc)))
                    if(currentMap.getGrid( robot_pos.y+(2*rowInc),robot_pos.x+(2*colInc)).isObstacles())
                        imageList.put(new Point(robot_pos.x+(2*colInc), robot_pos.y+(2*rowInc)), camera.get("middle").toString());

            if (!camera.get("right").toString().equals("0"))
                if (currentMap.checkValidGrid(R2_pos.y+rowInc, R2_pos.x+colInc))
                    if(currentMap.getGrid(R2_pos.y+rowInc, R2_pos.x+colInc).isObstacles())
                        imageList.put(new Point(R2_pos.x+colInc, R2_pos.y+rowInc), camera.get("right").toString());

            robot.setAllImageList(imageList);
        }
}


    public boolean movable(MapDirections dir) {
        boolean isMovable = false;
        switch (dir) {
            case UP:
                isMovable = currentMap.checkValidMove(robot.getCurLocation().y + 1, robot.getCurLocation().x);
                break;

            case DOWN:
                isMovable = currentMap.checkValidMove(robot.getCurLocation().y - 1, robot.getCurLocation().x);
                break;

            case LEFT:
                isMovable = currentMap.checkValidMove(robot.getCurLocation().y, robot.getCurLocation().x - 1);
                break;

            case RIGHT:
                isMovable = currentMap.checkValidMove(robot.getCurLocation().y, robot.getCurLocation().x + 1);
                break;
        }
        return isMovable;
    }

    public String getArduinoCmd(ArrayList<RoboCmd> commands) {
        StringBuilder cmdBuilder = new StringBuilder();

        for (RoboCmd cmd : commands)
            cmdBuilder.append(RoboCmd.ArduinoCtrl.values()[cmd.ordinal()]);

        String cmds = cmdBuilder.toString();
        return cmds;
    }

    private void imageLoop() throws InterruptedException {
        ArrayList<MapObjectSurface> surfTaken;
        MapObjectSurface nearestObstacle;
        MapGrid nearestCell;
        boolean success;

        nearestObstacle = currentMap.nearestObsSurface(robot.getCurLocation(), robot.getNotYetTaken(), currentMap);

        System.out.println("DEBUG nearestObstacle " + nearestObstacle.toString());
        nearestCell = currentMap.nearestMovable(nearestObstacle);
        System.out.println("DEBUG nearestCell is null:" + (nearestCell == null));

        if (nearestCell != null) {
            System.out.println("DEBUG nearestCell " + nearestCell.toString());

            // go to nearest cell
            success = goToPointForImage(nearestCell.getPos(), nearestObstacle);

            if (success) {
                System.out.println("DEBUG cell pos " + nearestCell.getPos().toString());
                do {
                    surfTaken = imageRecognitionRight(currentMap);
//                    robot.updateNotYetTaken(surfTaken);
                    robot.initSurfaceList(currentMap);
                    rightWallHug(true);
                    antiStuck();
                    calibrate_every_x_steps();
                    calibrate();

                    System.out.println("DEBUG robot pos " + robot.getCurLocation().toString());
                } while (!robot.getCurLocation().equals(nearestCell.getPos()) && !robot.isWallOnRight());
            } else {
                System.out.println("DEBUG in inner else");
                robot.getNotYetTaken().remove(nearestObstacle.toString());
            }

        } else {
            System.out.println("DEBUG in outer else");
            robot.getNotYetTaken().remove(nearestObstacle.toString());
            System.out.println("DEBUG after removing in outer else");
        }
    }

    public void goToPoint(Point location, boolean takingImage) throws InterruptedException {
        FastestPath fp = new FastestPath(currentMap, robot, simulation);
        ArrayList<MapGrid> path = fp.runFloodFill(robot.getCurLocation(), location);

        fp.displayFastestPath(path, true);
        ArrayList<RoboCmd> commands = fp.getPathCommands(path);
        String cmd = getArduinoCmd(commands);

        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);
            Optional<String> sensorString;
            switch (c) {
                case 'W':
                    if (movable(robot.getDir())) {
                        sensorString = robot.move(RoboCmd.FORWARD, 1, currentMap, stepsPerSecond);
                        robot.sense(currentMap, realMap, sensorString);
                    }
                    break;
                case 'S':
                    if (movable(MapDirections.getOpposite(robot.getDir()))) {
                        sensorString = robot.move(RoboCmd.BACKWARD, 1, currentMap, stepsPerSecond);
                        robot.sense(currentMap, realMap, sensorString);
                    }
                    break;
                case 'A':
                    sensorString = robot.turn(RoboCmd.LEFT_TURN, stepsPerSecond);
                    robot.sense(currentMap, realMap, sensorString);
                    break;
                case 'D':
                    sensorString = robot.turn(RoboCmd.RIGHT_TURN, stepsPerSecond);
                    robot.sense(currentMap, realMap, sensorString);
                    break;
            }

            calibrate();
            if (takingImage) {
                takeImage();
            }

//            if(!simulation) {
//                robot.send_android(currentMap, NetworkConstants.MDF);
//                netMgr.receive();
//            }
        }
    }

    private boolean goToPointForImage(Point loc, MapObjectSurface obsSurface) throws InterruptedException {
        ArrayList<MapObjectSurface> surfTaken = new ArrayList<MapObjectSurface>();
        robot.setStatus("Go to point: " + loc.toString());
        LOGGER.info(robot.getStatus());
        ArrayList<RoboCmd> commands = new ArrayList<RoboCmd>();
        ArrayList<MapGrid> path = new ArrayList<MapGrid>();
        FastestPath fp = new FastestPath(currentMap, robot, simulation);
        path = fp.runFloodFill(robot.getCurLocation(), loc);
        if (path == null) {
            return false;
        }

        fp.displayFastestPath(path, true);
        commands = fp.getPathCommands(path);
        System.out.println("Exploration Fastest Commands: " + commands);

        for (RoboCmd c : commands) {
            System.out.println("Command: " + c);
            if ((c == RoboCmd.FORWARD) && !movable(robot.getDir())) {
                System.out.println("Not Executing Forward Not Movable");
                imageLoop();
                goToPointForImage(loc, obsSurface);
                break;
            } else {
                if (((c == RoboCmd.LEFT_TURN && !movable(MapDirections.getAntiClockwise(robot.getDir()))) ||
                        (c == RoboCmd.RIGHT_TURN && !movable(MapDirections.getClockwise(robot.getDir())))) && commands.indexOf(c) == commands.size() - 1)
                    goToPointForImage(loc, obsSurface);
                if (c == RoboCmd.LEFT_TURN || c == RoboCmd.RIGHT_TURN) {
                    robot.turn(c, stepPerSecond);
                    robot.sense(currentMap, realMap, Optional.empty());
                    calibrate();
                } else {
                    robot.move(c, RobotConstants.MOVE_STEPS, currentMap, stepPerSecond);
                    robot.sense(currentMap, realMap, Optional.empty());
                    calibrate();
                }

                if (!simulation) {
//                    robot.send_android(currentMap, NetworkConstants.EXPLORATION);
                    surfTaken = imageRecognitionRight(currentMap);
                }
                robot.updateNotYetTaken(surfTaken);

            }
        }


        // Orient the robot to make its right side hug the wall
        // if right movable

        MapDirections desiredDir = MapDirections.getClockwise(obsSurface.getSurface());
        if (desiredDir == robot.getDir()) {
            return true;
        } else if (desiredDir == MapDirections.getClockwise(robot.getDir())) {
            robot.turn(RoboCmd.RIGHT_TURN, stepPerSecond);
            robot.sense(currentMap, realMap, Optional.empty());
            if (!simulation) {
                surfTaken = imageRecognitionRight(currentMap);
            }
            robot.updateNotYetTaken(surfTaken);
        } else if (desiredDir == MapDirections.getAntiClockwise(robot.getDir())) {
            robot.turn(RoboCmd.LEFT_TURN, stepPerSecond);
            robot.sense(currentMap, realMap, Optional.empty());

            if (!simulation) {
                surfTaken = imageRecognitionRight(currentMap);
            }
            robot.updateNotYetTaken(surfTaken);
        }
        // opposite
        else {
            robot.turn(RoboCmd.LEFT_TURN, stepPerSecond);
            robot.sense(currentMap, realMap, Optional.empty());

            if (!simulation) {
                surfTaken = imageRecognitionRight(currentMap);
            }
            robot.updateNotYetTaken(surfTaken);
            robot.turn(RoboCmd.LEFT_TURN, stepPerSecond);
            robot.sense(currentMap, realMap, Optional.empty());

            if (!simulation) {
                surfTaken = imageRecognitionRight(currentMap);
            }
            robot.updateNotYetTaken(surfTaken);
        }
        return true;
    }

    private void removeFakeVirtualWall(){
        // Init Grids on the map
        for (int row = 0; row < MapConstants.MAP_LENGTH; row++) {
            for (int col = 0; col < MapConstants.MAP_WIDTH; col++) {
                // Init virtual wall
                if (row == 0 || col == 0 || row == MapConstants.MAP_LENGTH - 1 || col == MapConstants.MAP_WIDTH - 1) {
                    LOGGER.info("grid:"+currentMap.getGrid(row,col));
                    currentMap.getGrid(row,col).setVirtualWall(true);
                } else {
                    currentMap.getGrid(row,col).setVirtualWall(false);
                }
            }
        }
    }

}
