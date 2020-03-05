package sg.edu.ntu.scse.mdp.g7.Map;

import java.awt.*;

public class MapGrid {

    // Position Variables
    private Point _pos;

    //Exploration variables
    private boolean obstacles;
    private boolean virtualWall;
    private boolean explored;
    private boolean path;
    private boolean wayPoint;
    private boolean moveThru;
    private String sensorName;

    // Image surface variables
    private boolean surfaceLeftTaken;
    private boolean surfaceRightTaken;
    private boolean surfaceDownTaken;
    private boolean surfaceUpTaken;

    // constructor
    public MapGrid(Point pos) {
        this._pos = pos;
        this.explored = false; //map shouldn't be explored at the start
        this.sensorName = null;
        this.surfaceLeftTaken = false;
        this.surfaceRightTaken = false;
        this.surfaceDownTaken = false;
        this.surfaceUpTaken = false;
    }

    //getters and setters
    public Point getPos() {
        return _pos;
    }

    public void setPos(Point pos) {
        this._pos = pos;
    }

    public boolean isObstacles() {
        return obstacles;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public void setObstacles(boolean obstacles) {
        this.obstacles = obstacles;
    }

    public boolean getVirtualWall() {
        return virtualWall;
    }

    public void setVirtualWall(boolean virtualWall) {
        this.virtualWall = virtualWall;
    }

    public boolean getExplored() {
        return explored;
    }

    public void setExplored(boolean explored) {
        this.explored = explored;
    }

    public boolean getPath() {
        return path;
    }

    public boolean isSurfaceLeftTaken() {
        return surfaceLeftTaken;
    }

    public boolean isSurfaceRightTaken() {
        return surfaceRightTaken;
    }

    public boolean isSurfaceDownTaken() {
        return surfaceDownTaken;
    }

    public boolean isSurfaceUpTaken() {
        return surfaceUpTaken;
    }

    public void setSurfaceLeftTaken(boolean surfaceLeftTaken) {
        this.surfaceLeftTaken = surfaceLeftTaken;
    }

    public void setSurfaceRightTaken(boolean surfaceRightTaken) {
        this.surfaceRightTaken = surfaceRightTaken;
    }

    public void setSurfaceDownTaken(boolean surfaceDownTaken) {
        this.surfaceDownTaken = surfaceDownTaken;
    }

    public void setSurfaceUpTaken(boolean surfaceUpTaken) {
        this.surfaceUpTaken = surfaceUpTaken;
    }

    public void setPath(boolean path) {
        this.path = path;
    }

    public boolean getWayPoint() {
        return wayPoint;
    }

    public boolean setWayPoint(boolean wayPoint) {
        if (!obstacles && explored && !virtualWall) {
            this.wayPoint = wayPoint;
            return true;
        }
        return false;
    }

    public boolean getMoveThru() {
        return moveThru;
    }

    public void setMoveThru(boolean moveThru) {
        this.moveThru = moveThru;
    }

    // Grid is movable if it has been explored and it is not an obstacle or virtual wall
    public boolean movableGrid() {
        return explored && !obstacles && !virtualWall;
    }

    @Override
    public String toString() {
        return "Grid [pos=" + _pos + ", explored=" + explored + ", obstacle=" + obstacles + ", virtualWall=" + virtualWall
                + ", wayPoint=" + wayPoint + ", moveThru=" + moveThru + ", path=" + path + "]";
    }

    public boolean isVirtualWall(){
        if (_pos.y == 0 || _pos.x == 0 || _pos.y == MapConstants.MAP_LENGTH-1 || _pos.x == MapConstants.MAP_WIDTH)
            return true;
        return false;
    }
}