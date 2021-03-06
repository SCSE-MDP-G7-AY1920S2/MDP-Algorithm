package Robot;

import javafx.scene.paint.Color;

public class RobotConstants {

    public static final double INFINITE_COST = 10000000;
//    public static final int CHECKSTEPS = 18;    // 4

    // targeted coverage
//    public static final int TARGETED_COVERAGE = 97;

    // best exploration timing for leaderboard in seconds
//    public static final int BEST_EXP_TIMING = 1;

    // To be adjusted
    public static final int MOVE_STEPS = 1;
    public static final long WAIT_TIME = 1000;    //Time waiting before retransmitting in milliseconds
    public static final int STEP_PER_SECOND = 30;  // 30; // default large step per second to avoid any delay

    // Sensors default range (In grids)
    public static final int SHORT_IR_MIN = 1;
    public static final int SHORT_IR_MAX = 2;

    public static final int LONG_IR_MIN = 1;
    public static final int LONG_IR_MAX = 5;

    // Camera default range (In grids)
    public static final int CAM_MIN = 1;
    public static final int CAM_MAX = 1;

    //Constants to render Robot
    public static final Color ROBOT_BODY = Color.rgb(59, 89, 152, 1);
    public static final Color ROBOT_OUTLINE = Color.BLACK;
    public static final Color ROBOT_DIRECTION = Color.WHITESMOKE;

}
