package Algorithm;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import Map.*;
import Network.NetworkManager;
import Robot.Robot;
import Robot.RoboCmd;
import Robot.RobotConstants;

import static java.lang.Math.*;

public class FastestPath {

    private static final Logger LOGGER = Logger.getLogger(FastestPath.class.getName());

    private boolean simulation;
    private Map exploredMap;
    private Robot robot;

    private MapDirections robotDir;

    private HashMap<Point, Double> costGMap;
    private HashMap<Point, Double> costMap = new HashMap<Point, Double>();

//    private static final NetworkManager netMgr = NetworkManager.getInstance();

    public FastestPath(Map exploredMap, Robot robot, boolean simulation) {
        this.exploredMap = exploredMap;
        this.robot = robot;
        this.simulation = simulation;
        this.robotDir = robot.getDir();
        initCostMap();
    }

    public void initCostMap() {
        costGMap = new HashMap<Point, Double>();
        for (int row = 0; row < MapConstants.MAP_LENGTH; row ++) {
            for (int col = 0; col < MapConstants.MAP_WIDTH; col ++) {
                MapGrid grid = exploredMap.getGrid(row, col);
                //differentiate between obs and path
                if (grid.movableGrid()) {
                    costGMap.put(grid.getPos(), 0.0);
                }
                else {
                    costGMap.put(grid.getPos(), RobotConstants.INFINITE_COST);
                }
            }
        }
    }

    public void initFloodFillCostMap(Point goal) {
        MapGrid grid;
        ArrayList<MapGrid> gridList = new ArrayList<MapGrid>();
        ArrayList<MapGrid> tmpList = new ArrayList<MapGrid>();
        ArrayList<MapGrid> neighbouringGrids = new ArrayList<MapGrid>();

        for (int row = 0; row < MapConstants.MAP_LENGTH; row++) {
            for (int col = 0; col < MapConstants.MAP_WIDTH; col++) {
                grid = exploredMap.getGrid(row, col);
                costMap.put(grid.getPos(), RobotConstants.INFINITE_COST);
            }
        }

        grid = exploredMap.getGrid(goal.y, goal.x);
        costMap.put(grid.getPos(), 0.0);
        gridList.add(grid);
        tmpList.add(grid);
//        gridList.add(exploredMap.getGrid(goal.y, goal.x));
//        tmpList.add(exploredMap.getGrid(goal.y, goal.x));
        System.out.println("start");

        while(!tmpList.isEmpty()){
            MapGrid tmpGrid = tmpList.get(0);
            tmpList.remove(tmpGrid);
            neighbouringGrids = exploredMap.getNeighbours(tmpGrid);

            for (MapGrid nGrid : neighbouringGrids) {
                if (!gridList.contains(nGrid)){
                    if (nGrid.movableGrid() && canPassThrough(nGrid)) {
                        gridList.add(nGrid);
                        updateCostMap(nGrid);
                        tmpList.add(nGrid);
                    }
                }
            }
        }
        System.out.println();

    }

    public boolean canPassThrough(MapGrid grid) {
        int row = grid.getPos().y;
        int col = grid.getPos().x;

        MapGrid tempGrid;

        for (int i = row - 1; i <= row + 1; i++){
            for (int j = col - 1; j <= col + 1; j++) {
                if (exploredMap.checkValidGrid(i,j)) {
                    tempGrid = exploredMap.getGrid(i, j);
                    if (tempGrid.isObstacles()){
                        System.out.println(tempGrid);
                        return false;
                    }
                } else {
                    return false;
                }


            }
        }

        return true;
    }

    public void updateCostMap(MapGrid grid) {

        Point temp_pt = grid.getPos();

        double cost = costMap.get(grid.getPos());
        double min_cost;

        double gridUpCost = costMap.get(exploredMap.getGrid(temp_pt.y + 1, temp_pt.x).getPos()) + 1;
        double gridDownCost = costMap.get(exploredMap.getGrid(temp_pt.y - 1, temp_pt.x).getPos()) + 1;
        double gridLeftCost = costMap.get(exploredMap.getGrid(temp_pt.y, temp_pt.x - 1).getPos()) + 1;
        double gridRightCost = costMap.get(exploredMap.getGrid(temp_pt.y, temp_pt.x + 1).getPos()) + 1;

        min_cost = min(min(min(gridUpCost,gridDownCost), min(gridLeftCost,gridRightCost)), cost);

        costMap.replace(grid.getPos(), min_cost);

    }

    private MapGrid getMinNeighbourGrid(Point cur) {
        MapGrid grid = null;
        ArrayList<MapGrid> gridList = new ArrayList<MapGrid>();
        double minCost = RobotConstants.INFINITE_COST;

        MapGrid tempGridUp = exploredMap.getGrid(cur.y + 1, cur.x);
        MapGrid tempGridDown = exploredMap.getGrid(cur.y - 1, cur.x);
        MapGrid tempGridLeft = exploredMap.getGrid(cur.y, cur.x - 1);
        MapGrid tempGridRight = exploredMap.getGrid(cur.y, cur.x + 1);

        gridList.add(tempGridDown);
        gridList.add(tempGridUp);
        gridList.add(tempGridLeft);
        gridList.add(tempGridRight);

        for (MapGrid gridTemp : gridList) {
            gridTemp.getPos();

            if(costMap.get(gridTemp.getPos()) < minCost) {
                minCost = costMap.get(gridTemp.getPos());
                if (requiredTurning(cur, gridTemp.getPos())){
                    minCost += 0.5;
                }
                grid = gridTemp;
            }
        }
        updateRobotDir(cur, grid.getPos());
        return grid;
    }

    public boolean requiredTurning(Point initial, Point target){
        MapDirections dir = robotDir;

        switch (dir){
            case UP:
                if (initial.y + 1 == target.y)
                    return false;
            case DOWN:
                if (initial.y - 1 == target.y)
                    return false;
            case LEFT:
                if (initial.x - 1 == target.x)
                    return false;
            case RIGHT:
                if (initial.x + 1 == target.x)
                    return false;
        }

        return true;
    }

    public void updateRobotDir(Point initial, Point target){
        if (target.y - initial.y == 1)
            robotDir = MapDirections.UP;
        else if (target.x - initial.x == 1)
            robotDir = MapDirections.RIGHT;
        else if (target.x - initial.x == -1)
            robotDir = MapDirections.LEFT;
        else if (target.y - initial.y == -1)
            robotDir = MapDirections.DOWN;
    }

    public ArrayList<MapGrid> runFloodFill (Point start, Point goal) {
        initFloodFillCostMap(goal);

        ArrayList<MapGrid> toVisit = new ArrayList<MapGrid>();

        ArrayList<MapGrid> neighbours;
        double newGtemp, curGtemp;

        MapGrid cur = exploredMap.getGrid(start);
        MapDirections curDir = robot.getDir();

        while (!cur.getPos().equals(goal)) {
            cur = getMinNeighbourGrid(cur.getPos());
            toVisit.add(cur);
        }
        return toVisit;
    }

    /**
     * To display the fastest path found on the simulator
     * @param path
     * @param display
     */
    public void displayFastestPath(ArrayList<MapGrid> path, boolean display) {
        MapGrid temp;
        System.out.println("Path:");
        for(int i = 0; i < path.size(); i++) {
            temp = path.get(i);
            //Set the path cells to display as path on the Sim
            exploredMap.getGrid(temp.getPos()).setPath(display);
            System.out.println(exploredMap.getGrid(temp.getPos()).toString());

            //Output Path on console
            if(i != (path.size()-1))
                System.out.print("(" + temp.getPos().y + ", " + temp.getPos().x + ") --> ");
            else
                System.out.print("(" + temp.getPos().y + ", " + temp.getPos().x + ")");
        }
        System.out.println("\n");
    }

    //Returns the movements required to execute the path
    public ArrayList<RoboCmd> getPathCommands(ArrayList<MapGrid> path) throws InterruptedException {
        Robot tempRobot = new Robot(true, true, robot.getCurLocation().y, robot.getCurLocation().x, robot.getDir());
        ArrayList<RoboCmd> moves = new ArrayList<RoboCmd>();

        RoboCmd move;
        MapGrid cell = exploredMap.getGrid(tempRobot.getCurLocation());
        MapGrid newCell;
        MapDirections cellDir;

        //Iterate through the path
        for (int i = 0; i < path.size(); i++) {
            newCell = path.get(i);
            cellDir = exploredMap.getGridDir(cell.getPos(), newCell.getPos());
            // If the TempRobot and cell direction not the same
            if (MapDirections.getOpposite(tempRobot.getDir()) == cellDir) {
                move = RoboCmd.LEFT_TURN; //first move
                tempRobot.turn(move, RobotConstants.STEP_PER_SECOND);
                moves.add(move);
                tempRobot.turn(move, RobotConstants.STEP_PER_SECOND);
                moves.add(move);
                move = RoboCmd.FORWARD; //second move
//                move = RoboCmd.BACKWARD; //second move

            } else if (MapDirections.getClockwise(tempRobot.getDir()) == cellDir) {
                move = RoboCmd.RIGHT_TURN; //first move
                tempRobot.turn(move, RobotConstants.STEP_PER_SECOND);
                moves.add(move); //second move
                move = RoboCmd.FORWARD;
            } else if (MapDirections.getAntiClockwise(tempRobot.getDir()) == cellDir) {
                move = RoboCmd.LEFT_TURN; //first move
                tempRobot.turn(move, RobotConstants.STEP_PER_SECOND);
                moves.add(move);
                move = RoboCmd.FORWARD; //second move
            } else {
                move = RoboCmd.FORWARD;
            }
            tempRobot.move(move, RobotConstants.MOVE_STEPS, exploredMap, RobotConstants.STEP_PER_SECOND);
            moves.add(move);
            cell = newCell;
        }
        System.out.println("Generated Moves: " + moves.toString());
        return moves;
    }
}
