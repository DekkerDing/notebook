package io.github.dekkerding.examples.template;

/**
 * 改造为未来科技感车辆
 */
public class TechnologyFeeling extends RefittedVehicle{
    /**
     * 初始化 (改装车辆前准备工作)
     */
    @Override
    void initiialize() {
        System.out.println("平整车辆,开孔装电池");
    }

    /**
     * 改装车辆轮胎 (改装项目)
     */
    @Override
    void tyre() {

    }

    /**
     * 改装车辆引擎 (改装项目)
     */
    @Override
    void engine() {
        System.out.println("改油电混合");
    }

    /**
     * 改装车辆外观 (改装项目)
     */
    @Override
    void appearance() {

    }

    /**
     * 改装车辆自动驾驶 (改装项目)
     */
    @Override
    void autoPilot() {
        System.out.println("安装自动驾驶系统");
    }
}
