package io.github.dekkerding.examples.template;

/**
 * 改造运动类型车辆
 */
public class MotionType extends RefittedVehicle {
    /**
     * 初始化 (改装车辆前准备工作)
     */
    @Override
    void initiialize() {
        System.out.println("平整车辆,开孔装两台涡轮");
    }

    /**
     * 改装车辆轮胎 (改装项目)
     */
    @Override
    void tyre() {
        System.out.println("改轮胎,加宽");
    }

    /**
     * 改装车辆引擎 (改装项目)
     */
    @Override
    void engine() {
        System.out.println("加大发动机排量");
    }

    /**
     * 改装车辆外观 (改装项目)
     */
    @Override
    void appearance() {
        System.out.println("加运动套件");
    }

    /**
     * 改装车辆自动驾驶 (改装项目)
     */
    @Override
    void autoPilot() {
    }
}
