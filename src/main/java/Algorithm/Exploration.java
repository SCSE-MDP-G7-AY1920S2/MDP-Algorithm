package Algorithm;

//Map
import Map.Map;
import Map.MapConstants;
import Map.MapDirections;
import Map.MapGrid;
import Network.NetworkConstants;
import Network.NetworkManager;
import Robot.RoboCmd;
import Robot.Robot;

import java.awt.*;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;

//Network
//Robot

public class Exploration {

    private static final Logger LOGGER = Logger.getLogger(Exploration.class.getName());

    private boolean simulation;

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

    //Time
    private int timeLimit;
    private long startTime;
    private long stopTime;

    //exploration status
    private double explorationLimit = 100;
    private double exploredPercentage;

    //If it completed one round using right wall hugging
    private boolean backToStart = false;
    private int backToStartTimes = 0;
    private int obstacleCount = 0;
    private boolean leaveStartPoint = false;

    //network manager
    private static final NetworkManager netMgr = NetworkManager.getInstance();
    private int stepsPerSecond;

    private boolean terminate = false;

    public void setTerminate(boolean terminate) { this.terminate = terminate;}

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

    public double getExplorationLimit() {
        return explorationLimit;
    }

    public void setExplorationLimit(double explorationLimit) {
        this.explorationLimit = explorationLimit;
    }

    public double getExploredPercentage() {
        return exploredPercentage;
    }

    public void setExploredPercentage(double exploredPercentage) {
        this.exploredPercentage = exploredPercentage;
    }

    public boolean isBackToStart() {
        return backToStart;
    }

    public void setBackToStart(boolean backToStart) {
        this.backToStart = backToStart;
    }

    //Constructor
    public Exploration(boolean simulation, Map currentMap, Map realMap, Robot robot, Point startPos, int timeLimit, double explorationLimit, int stepsPerSecond) {
        this.simulation = simulation;
        this.currentMap = currentMap;
        this.realMap = realMap;
        this.robot = robot;
        this.startPos = startPos;
        this.timeLimit = timeLimit;
        this.explorationLimit = explorationLimit;
        this.stepsPerSecond = stepsPerSecond;
        this.prevLocX = startPos.x;
        this.prevLocY = startPos.y;
        robot.setExploration(true);
    }

    public void startExploration() throws InterruptedException {
        startTime = System.currentTimeMillis();
        stopTime = startTime + timeLimit;

        exploredPercentage = currentMap.getPercentageExplored();

        while ((System.currentTimeMillis() < stopTime) &&  backToStartTimes < 1) {
            exploredPercentage = currentMap.getPercentageExplored();

            antiStuck();
            calibrate_every_x_steps();
            rightWallHug();

            // COMMENT HERE
            if (backToStart){
                for (int row = 0; row < MapConstants.MAP_LENGTH; row++) {
                    for (int col = 0; col < MapConstants.MAP_WIDTH; col++) {
                        if (currentMap.getGrid(row, col).isObstacles()) {
                            obstacleCount ++;
                        }
                    }
                }
            }

            if (obstacleCount == 30)
                break;
            else {
                obstacleCount = 0;
                backToStart = false;
            }
            // COMMENT HERE
        }

        removeFakeVirtualWall();

        for (int row = 0; row < MapConstants.MAP_LENGTH; row++) {
            for (int col = 0; col < MapConstants.MAP_WIDTH; col++) {
                if (currentMap.getGrid(row,col).getExplored() == false){
                    currentMap.getGrid(row,col).setExplored(true);
                    currentMap.getGrid(row,col).setObstacles(false);
                }
            }
        }
    }

    public void terminateExp() {
        for (int row = 0; row < MapConstants.MAP_LENGTH; row++) {
            for (int col = 0; col < MapConstants.MAP_WIDTH; col++) {
                if (currentMap.getGrid(row,col).getExplored() == false){
                    currentMap.getGrid(row,col).setExplored(true);
                    currentMap.getGrid(row,col).setObstacles(false);
                }
            }
        }
        robot.send_android(currentMap, NetworkConstants.MDF);
        netMgr.send("H", "Ex");
    }

    public void antiStuck() throws InterruptedException {
        if (nextToWall()) {
            prevLocX = robot.getCurLocation().x;
            prevLocY = robot.getCurLocation().y;
            if (robot.getCurLocation().x == 1)
                prevDir = MapDirections.DOWN;
            else if (robot.getCurLocation().x == 13)
                prevDir = MapDirections.UP;
            else if (robot.getCurLocation().y == 1)
                prevDir = MapDirections.RIGHT;
            else if (robot.getCurLocation().y == 18)
                prevDir = MapDirections.LEFT;
        }

        if (stuckInLoop()) {
            goToPoint(new Point(prevLocX, prevLocY));
            turn(prevDir);
            movement.clear();
        }
    }

    public boolean nextToWall(){
        if (robot.getCurLocation().x == 1 || robot.getCurLocation().x == 13 ||
                robot.getCurLocation().y == 1 || robot.getCurLocation().y == 18)
            return true;
        return false;
    }

    public void calibrate_every_x_steps() throws InterruptedException{

        if(simulation)
            return;

        int tmpx = robot.getCurLocation().x;
        int tmpy = robot.getCurLocation().y;

        Optional<String> sensorString;

        if (robot.isObjOnFront(currentMap) && robot.isObjOnRight(currentMap) && (
                (tmpx == 1 && tmpy == 1) || (tmpx == 13 && tmpy == 1) ||(tmpx == 1 && tmpy == 18) || (tmpx == 13 && tmpy == 18)
        )) {
            sensorString = robot.turn(RoboCmd.RIGHT_TURN, 1);
            robot.sense(currentMap, realMap, sensorString);
            calibrate();
            sensorString = robot.turn(RoboCmd.LEFT_TURN, 1);
            robot.sense(currentMap, realMap, sensorString);
            robot.setMoveCounter(0);
        }

        if (robot.getMoveCounter() >= 15){
            if (robot.isObjOnFront(currentMap)) {
                calibrate();
                robot.setMoveCounter(0);
            }
            if (robot.isObjOnRight(currentMap)) {
                sensorString = robot.turn(RoboCmd.RIGHT_TURN, 1);
                robot.sense(currentMap, realMap, sensorString);
                calibrate();
                sensorString = robot.turn(RoboCmd.LEFT_TURN, 1);
                robot.sense(currentMap, realMap, sensorString);
                calibrate();
                robot.setMoveCounter(0);
            }
        }
    }

//    public void calibrate_every_x_steps() throws InterruptedException{
//        if(simulation)
//            return;
//
//        if (robot.isObjOnFront(currentMap) && robot.isObjOnRight(currentMap)) {
//            align("F");
//            robot.turn(RoboCmd.RIGHT_TURN, 1);
//            robot.sense(currentMap, realMap);
//            align("F");
//            robot.turn(RoboCmd.LEFT_TURN, 1);
//            robot.sense(currentMap, realMap);
//            robot.setMoveCounter(0);
//        }
//
//        if (robot.getMoveCounter() >= 6){
//                if (robot.isObjOnFront(currentMap)) {
//                    align("F");
//                    robot.setMoveCounter(0);
//                }
//                if (robot.isObjOnRight(currentMap)) {
//                    robot.turn(RoboCmd.RIGHT_TURN, 1);
//                    robot.sense(currentMap, realMap);
//                    align("F");
//                    robot.turn(RoboCmd.LEFT_TURN, 1);
//                    robot.sense(currentMap, realMap);
//                    align("R");
//                    robot.setMoveCounter(0);
//                }
//        }
//    }


//    public void stitch() throws InterruptedException{
//        Stack<RoboCmd> stack = new Stack<RoboCmd>();
//        RoboCmd cmd;
//
//        if (requireStitch()) {
//            robot.turn(RoboCmd.LEFT_TURN,1);
//            robot.sense(currentMap, realMap);
//
//            if (movable(robot.getDir())) {
//                stack.push(RoboCmd.LEFT_TURN);
//                while (movable(robot.getDir())) {
//                    stack.push(RoboCmd.FORWARD);
//                    robot.move(RoboCmd.FORWARD, 1, currentMap, 1);
//                    robot.sense(currentMap, realMap);
//                }
//                robot.turn(RoboCmd.RIGHT_TURN,1);
//                robot.sense(currentMap, realMap);
//
//                robot.turn(RoboCmd.RIGHT_TURN,1);
//                robot.sense(currentMap, realMap);
//
//            } else {
//                stack.push(RoboCmd.RIGHT_TURN);
//            }
//
//            while (!stack.isEmpty()) {
//                cmd = stack.pop();
//                if (cmd == RoboCmd.LEFT_TURN || cmd == RoboCmd.RIGHT_TURN){
//                    robot.turn(cmd, 1);
//                    robot.sense(currentMap, realMap);
//
//                } else {
//                    robot.move(cmd, 1,currentMap,1);
//                    robot.sense(currentMap, realMap);
//
//                }
//            }
//        }
//    }

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
        Optional<String> sensorString = Optional.empty();
        if (prevDir.equals(robotDir))
            return;

        if (MapDirections.getClockwise(robotDir).equals(prevDir)) {
            sensorString = robot.turn(RoboCmd.RIGHT_TURN, 1);
        } else if (MapDirections.getAntiClockwise(robotDir).equals(prevDir)) {
            sensorString = robot.turn(RoboCmd.LEFT_TURN, 1);
        } else if (MapDirections.getOpposite(robotDir).equals(prevDir)) {
            sensorString = robot.turn(RoboCmd.LEFT_TURN, 1);
            robot.sense(currentMap, realMap, sensorString);
            calibrate();
            sensorString = robot.turn(RoboCmd.LEFT_TURN, 1);
        }
        robot.sense(currentMap, realMap, sensorString);
        calibrate();
    }

//    public boolean frontMovable() {
//        MapDirections dir = robot.getDir();
//
//        int rowInc = 0, colInc = 0;
//
//        switch (dir){
//            case UP:
//                rowInc = 1;
//                colInc = 0;
//                break;
//            case DOWN:
//                rowInc = -1;
//                colInc = 0;
//                break;
//            case LEFT:
//                rowInc = 0;
//                colInc = -1;
//                break;
//            case RIGHT:
//                rowInc = 0;
//                colInc = 1;
//                break;
//        }
//
//        Point F1_pos = robot.getSensorMap().get("F1").getPos();
//        Point F2_pos = robot.getSensorMap().get("F2").getPos();
//        Point F3_pos = robot.getSensorMap().get("F3").getPos();
//
//        int F1Y = F1_pos.y;
//        int F2Y = F2_pos.y;
//        int F3Y = F3_pos.y;
//
//        int F1X = F1_pos.x;
//        int F2X = F2_pos.x;
//        int F3X = F3_pos.x;
//
////        for (int i = 1 ; i <= RobotConstants.LONG_IR_MAX ; i++){
//            F1Y += rowInc;
//            F2Y += rowInc;
//            F3Y += rowInc;
//
//            F1X += colInc;
//            F2X += colInc;
//            F3X += colInc;
//
//            if (currentMap.checkValidGrid(F1Y, F1X) && currentMap.checkValidGrid(F2Y, F2X) && currentMap.checkValidGrid(F3Y, F3X)) {
//                if ((currentMap.getGrid(F1Y, F1X).isObstacles() || currentMap.getGrid(F2Y, F2X).isObstacles() || currentMap.getGrid(F3Y, F3X).isObstacles())
//                        && currentMap.getGrid(F1Y, F1X).getExplored() && currentMap.getGrid(F2Y, F2X).getExplored() && currentMap.getGrid(F3Y, F3X).getExplored()) {
//                    return true;
//                }
//            }
////        }
//        return false;
//    }

//    public void disableUnnecessaryEntry(){
//        if (frontUnmovable(robot.getDir()) != null) {
//            MapDirections dir = robot.getDir();
//            setVirtualWall(dir, frontUnmovable(robot.getDir()));
//        }
//    }

//    public boolean requireStitch() {
//        RobotSensors L1 = robot.getSensorMap().get("L1");
//        Point L1_pos = L1.getPos();
//
//        MapDirections sDir = L1.getSensorDir();
//
//        int colInc=0, rowInc=0;
//        MapDirections neighbourDir = MapDirections.UP;
//
//        switch (sDir) {
//            case UP:
//                colInc = 0;
//                rowInc = 1;
//                neighbourDir = MapDirections.LEFT;
//                break;
//            case DOWN:
//                colInc = 0;
//                rowInc = -1;
//                neighbourDir = MapDirections.RIGHT;
//                break;
//            case LEFT:
//                colInc = -1;
//                rowInc = 0;
//                neighbourDir = MapDirections.DOWN;
//                break;
//            case RIGHT:
//                colInc = 1;
//                rowInc = 0;
//                neighbourDir = MapDirections.UP;
//                break;
//        }
//
//        MapGrid g = null;
//        int row = L1_pos.y;
//        int col = L1_pos.x;
//        for (int i = 1 ; i <= RobotConstants.LONG_IR_MAX; i++) {
//            row = L1_pos.y + rowInc * i;
//            col = L1_pos.x + colInc * i;
//            if (currentMap.checkValidGrid(row, col)) {
//                if (currentMap.getGrid(row, col).getExplored())
//                    if (currentMap.getGrid(row, col).isObstacles()){
//                        if (currentMap.isStaircase(currentMap.getGrid(row, col))) {
//                            return true;
//                        }
//                    }
//            }
//        }
//        return false;
//    }

//    public MapGrid frontUnmovable(MapDirections dir) {
//
//        if (!(robot.isWallOnRight() || robot.isWallOnLeft()))
//            return null;
//
//        Point F1_pos = robot.getSensorMap().get("F1").getPos();
//        Point F2_pos = robot.getSensorMap().get("F2").getPos();
//        Point F3_pos = robot.getSensorMap().get("F3").getPos();
//
//        int F1Y = F1_pos.y;
//        int F2Y = F2_pos.y;
//        int F3Y = F3_pos.y;
//
//        int F1X = F1_pos.x;
//        int F2X = F2_pos.x;
//        int F3X = F3_pos.x;
//
//        MapGrid g = null;
//        MapGrid canPassGrid = null;
//        switch (dir) {
//            case UP:
//                for (int i= 0; i <= RobotConstants.LONG_IR_MAX ; i++) {
//                    F1Y++;
//                    F2Y++;
//                    F3Y++;
//
//                    if (currentMap.checkValidGrid(F1Y, F1X) && currentMap.checkValidGrid(F2Y, F2X) && currentMap.checkValidGrid(F3Y, F3X)) {
//                        if ((currentMap.getGrid(F1Y, F1X).isObstacles() || currentMap.getGrid(F2Y, F2X).isObstacles() || currentMap.getGrid(F3Y, F3X).isObstacles())
//                                && currentMap.getGrid(F1Y, F1X).getExplored() && currentMap.getGrid(F2Y, F2X).getExplored() && currentMap.getGrid(F3Y, F3X).getExplored()) {
//                            if (robot.isWallOnRight()) {
//                                g = currentMap.getDirNeighbour(currentMap.getGrid(F1Y,F1X), MapDirections.LEFT);
//                                if (g != null)
//                                    canPassGrid = currentMap.getGrid(g.getPos().y-2, g.getPos().x-2);
//                            } else if (robot.isWallOnLeft()) {
//                                g = currentMap.getDirNeighbour(currentMap.getGrid(F1Y,F3X), MapDirections.RIGHT);
//                                if (g != null)
//                                    canPassGrid = currentMap.getGrid(g.getPos().y-2, g.getPos().x+2);
//                            }
//                        }
//                    }
//
//                }
//                if (canPassGrid != null)
//                    if(!currentMap.canPassThrough(canPassGrid))
//                        return canPassGrid;
//                break;
//            case DOWN:
//                for (int i= 0; i <= RobotConstants.LONG_IR_MAX ; i++) {
//                    F1Y--;
//                    F2Y--;
//                    F3Y--;
//
//                    if (currentMap.checkValidGrid(F1Y, F1X) && currentMap.checkValidGrid(F2Y, F2X) && currentMap.checkValidGrid(F3Y, F3X)) {
//                        if ((currentMap.getGrid(F1Y, F1X).isObstacles() || currentMap.getGrid(F2Y, F2X).isObstacles() || currentMap.getGrid(F3Y, F3X).isObstacles())
//                                && currentMap.getGrid(F1Y, F1X).getExplored() && currentMap.getGrid(F2Y, F2X).getExplored() && currentMap.getGrid(F3Y, F3X).getExplored()) {
//                            if (robot.isWallOnRight()) {
//                                g = currentMap.getDirNeighbour(currentMap.getGrid(F1Y,F1X), MapDirections.RIGHT);
//                                if (g != null)
//                                    canPassGrid = currentMap.getGrid(g.getPos().y+2, g.getPos().x+2);
//                            } else if (robot.isWallOnLeft()) {
//                                g = currentMap.getDirNeighbour(currentMap.getGrid(F1Y,F3X), MapDirections.LEFT);
//                                if (g != null)
//                                    canPassGrid = currentMap.getGrid(g.getPos().y+2, g.getPos().x-2);
//                            }
//                        }
//                    }
//
//                }
//                if (canPassGrid != null)
//                    if(!currentMap.canPassThrough(canPassGrid))
//                        return canPassGrid;
//                break;
//            case LEFT:
//                for (int i= 0; i <= RobotConstants.LONG_IR_MAX ; i++) {
//                    F1X--;
//                    F2X--;
//                    F3X--;
//
//                    if (currentMap.checkValidGrid(F1Y, F1X) && currentMap.checkValidGrid(F2Y, F2X) && currentMap.checkValidGrid(F3Y, F3X)) {
//                        if ((currentMap.getGrid(F1Y, F1X).isObstacles() || currentMap.getGrid(F2Y, F2X).isObstacles() || currentMap.getGrid(F3Y, F3X).isObstacles())
//                                && currentMap.getGrid(F1Y, F1X).getExplored() && currentMap.getGrid(F2Y, F2X).getExplored() && currentMap.getGrid(F3Y, F3X).getExplored()) {
//                            if (robot.isWallOnRight()) {
//                                g = currentMap.getDirNeighbour(currentMap.getGrid(F1Y,F1X), MapDirections.DOWN);
//                                if (g != null)
//                                    canPassGrid = currentMap.getGrid(g.getPos().y-2, g.getPos().x+2);
//                            } else if (robot.isWallOnLeft()) {
//                                g = currentMap.getDirNeighbour(currentMap.getGrid(F1Y,F3X), MapDirections.UP);
//                                if (g != null)
//                                    canPassGrid = currentMap.getGrid(g.getPos().y+2, g.getPos().x+2);
//                            }
//                        }
//                    }
//
//                }
//                if (canPassGrid != null)
//                    if(!currentMap.canPassThrough(canPassGrid))
//                        return canPassGrid;
//                break;
//            case RIGHT:
//                for (int i= 0; i <= RobotConstants.LONG_IR_MAX ; i++) {
//                    F1X++;
//                    F2X++;
//                    F3X++;
//
//                    if (currentMap.checkValidGrid(F1Y, F1X) && currentMap.checkValidGrid(F2Y, F2X) && currentMap.checkValidGrid(F3Y, F3X)) {
//                        if ((currentMap.getGrid(F1Y, F1X).isObstacles() || currentMap.getGrid(F2Y, F2X).isObstacles() || currentMap.getGrid(F3Y, F3X).isObstacles())
//                                && currentMap.getGrid(F1Y, F1X).getExplored() && currentMap.getGrid(F2Y, F2X).getExplored() && currentMap.getGrid(F3Y, F3X).getExplored()) {
//                            if (robot.isWallOnRight()) {
//                                g = currentMap.getDirNeighbour(currentMap.getGrid(F1Y,F1X), MapDirections.UP);
//                                if (g != null)
//                                    canPassGrid = currentMap.getGrid(g.getPos().y+2, g.getPos().x-2);
//                            } else if (robot.isWallOnLeft()) {
//                                g = currentMap.getDirNeighbour(currentMap.getGrid(F1Y,F3X), MapDirections.DOWN);
//                                if (g != null)
//                                    canPassGrid = currentMap.getGrid(g.getPos().y-2, g.getPos().x-2);
//                            }
//                        }
//                    }
//
//                }
//                if (canPassGrid != null)
//                    if(!currentMap.canPassThrough(canPassGrid))
//                        return canPassGrid;
//                break;
//        }
//
//        return null;
//    }

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

    public void rightWallHug() throws InterruptedException{
        Optional<String> sensorString;
        if (movable(MapDirections.getClockwise(robot.getDir())))
        {
            sensorString = robot.turn(RoboCmd.RIGHT_TURN, stepsPerSecond);
            robot.sense(currentMap, realMap, sensorString);
            calibrate();
            movement.add(RoboCmd.RIGHT_TURN);
            moveForward();
        }
        else if (movable(robot.getDir()))
        {
            sensorString = robot.move(RoboCmd.FORWARD, 1, currentMap, stepsPerSecond);
            robot.sense(currentMap, realMap, sensorString);
            //sean tries lesser calibrate
            //if (robot.getMoveCounter() % 5 == 0)
            calibrate();
            movement.add(RoboCmd.FORWARD);
            robot.setMoveCounter(robot.getMoveCounter()+1);

            if (robot.getCurLocation().x == 1 && robot.getCurLocation().y == 1 ) {
                backToStart = true;
                backToStartTimes += 1;
            }
        }
        else if (movable(MapDirections.getAntiClockwise(robot.getDir())))
        {
            sensorString = robot.turn(RoboCmd.LEFT_TURN, stepsPerSecond);
            robot.sense(currentMap, realMap, sensorString);
            calibrate();

            movement.add(RoboCmd.LEFT_TURN);

        } else
        {
            sensorString = robot.turn(RoboCmd.LEFT_TURN, stepsPerSecond);
            robot.sense(currentMap, realMap, sensorString);
            calibrate();
            movement.add(RoboCmd.LEFT_TURN);

            sensorString = robot.turn(RoboCmd.LEFT_TURN, stepsPerSecond);
            robot.sense(currentMap, realMap, sensorString);
            calibrate();
            movement.add(RoboCmd.LEFT_TURN);
        }


    }

    public void moveForward() throws InterruptedException {
        Optional<String> sensorString;
        if (movable(robot.getDir())) {
            sensorString = robot.move(RoboCmd.FORWARD, 1, currentMap, stepsPerSecond);
            robot.sense(currentMap, realMap, sensorString);
            calibrate();
            movement.add(RoboCmd.FORWARD);
            robot.setMoveCounter(robot.getMoveCounter()+1);

            if (robot.getCurLocation().x == 1 && robot.getCurLocation().y == 1) {
                backToStart = true;
                backToStartTimes += 1;
            }
        } else {
            sensorString = robot.turn(RoboCmd.LEFT_TURN,1);
            robot.sense(currentMap, realMap, sensorString);
            calibrate();
            movement.add(RoboCmd.LEFT_TURN);
        }
    }

    public void calibrate(){
        if (robot.isWallOnFront()){
            align("F");
        } else if (robot.isBlockOnFront(currentMap)){
            align("f");
        }
        if (robot.isObjOnRight(currentMap))
            align("R");
    }

    public void align(String cmd) {
        System.out.println("aligning " + cmd);
        if (!simulation) {
            netMgr.send(cmd, "Ex");
            String msg;
            do {
                msg = netMgr.receive();
                LOGGER.info(msg);
            } while (!msg.contains(NetworkConstants.CALI_FIN));
            System.out.println("come out of align loop");
        }
    }

    public void goToPoint(Point location) throws InterruptedException {
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
                    sensorString = robot.move(RoboCmd.FORWARD, 1, currentMap, stepsPerSecond);
                    robot.sense(currentMap, realMap, sensorString);
                    break;
                case 'S':
                    sensorString = robot.move(RoboCmd.BACKWARD, 1, currentMap, stepsPerSecond);
                    robot.sense(currentMap, realMap, sensorString);
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

        }
}

    public String getArduinoCmd(ArrayList<RoboCmd> commands) {
        RoboCmd tempCmd;
        int moves = 0;

        RoboCmd curAction = RoboCmd.FORWARD;

        StringBuilder cmdBuilder = new StringBuilder();

        for (RoboCmd cmd : commands)
            cmdBuilder.append(RoboCmd.ArduinoCtrl.values()[cmd.ordinal()]);

        String cmds = cmdBuilder.toString();
        return cmds;
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

//    public void setVirtualWall(MapDirections dir, MapGrid obstacles){
//        Point curLoc = robot.getCurLocation();
//
//        int tmpX = curLoc.x;
//        int tmpY = curLoc.y;
//
//        switch (dir){
//            case UP:
//                if (robot.isWallOnRight()){
//                    tmpX++;
//                    while (obstacles.getPos().x != tmpX) {
//                        currentMap.getGrid(tmpY + 1,tmpX).setVirtualWall(true);
//                        tmpX--;
//                    }
//                } else if (robot.isWallOnLeft()){
//                    tmpX--;
//                    while (obstacles.getPos().x != tmpX) {
//                        currentMap.getGrid(tmpY + 1,tmpX).setVirtualWall(true);
//                        tmpX++;
//                    }
//                }
//                break;
//            case DOWN:
//                if (robot.isWallOnRight()){
//                    tmpX--;
//                    while (obstacles.getPos().x != tmpX) {
//                        currentMap.getGrid(tmpY - 1,tmpX).setVirtualWall(true);
//                        tmpX++;
//                    }
//                } else if (robot.isWallOnLeft()){
//                    tmpX++;
//                    while (obstacles.getPos().x != tmpX) {
//                        currentMap.getGrid(tmpY - 1,tmpX).setVirtualWall(true);
//                        tmpX--;
//                    }
//                }
//                break;
//            case LEFT:
//                if (robot.isWallOnRight()){
//                    tmpY++;
//                    while (obstacles.getPos().y != tmpY) {
//                        currentMap.getGrid(tmpY, tmpX - 1).setVirtualWall(true);
////                        currentMap.getGrid(tmpY, tmpX- 2).setObstacles(true);
//                        tmpY--;
//                    }
//                } else {
//                    tmpY--;
//                    while (obstacles.getPos().y != tmpY) {
//                        currentMap.getGrid(tmpY, tmpX - 1).setVirtualWall(true);
////                        currentMap.getGrid(tmpY, tmpX- 2).setObstacles(true);
//                        tmpY++;
//                    }
//                }
//                break;
//            case RIGHT:
//                if (robot.isWallOnRight()){
//                    tmpY--;
//                    while (obstacles.getPos().y != tmpY) {
//                        currentMap.getGrid(tmpY, tmpX + 1).setVirtualWall(true);
////                        currentMap.getGrid(tmpY, tmpX + 2).setObstacles(true);
//                        tmpY++;
//                    }
//                } else {
//                    tmpY++;
//                    while (obstacles.getPos().y != tmpY) {
//                        currentMap.getGrid(tmpY, tmpX + 1).setVirtualWall(true);
////                        currentMap.getGrid(tmpY, tmpX + 2).setObstacles(true);
//                        tmpY--;
//                    }
//                }
//                break;
//        }
//    }

}
