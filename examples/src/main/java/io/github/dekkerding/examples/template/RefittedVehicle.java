package io.github.dekkerding.examples.template;

/**
 * 改装车 模板
 */
public abstract class RefittedVehicle {

    /**
     * 初始化 (改装车辆前准备工作)
     */
    abstract void initiialize();

    /**
     * 改装车辆轮胎 (改装项目)
     */
    abstract void tyre();

    /**
     * 改装车辆引擎 (改装项目)
     */
    abstract void engine();

    /**
     * 改装车辆外观 (改装项目)
     */
    abstract void appearance();

    /**
     * 改装车辆自动驾驶 (改装项目)
     */
    abstract void autoPilot();

    /**
     * 改装车辆 (模板方法)
     */
    public final void refit() {
        initiialize();
        tyre();
        engine();
        appearance();
        autoPilot();
    }
}
