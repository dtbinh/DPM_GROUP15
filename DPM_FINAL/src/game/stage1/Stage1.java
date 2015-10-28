package game.stage1;

import robot.Robot;

public class Stage1 {
    private Robot robot;

    public Stage1(Robot robot){
        this.robot = robot;
    }

    public void start(){
        robot.localize(); //First thing is to localize...

    }

}
