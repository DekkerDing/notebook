package io.github.dekkerding.examples.state;

import com.oracle.deploy.update.UpdateCheck;

public class Acknowledged implements PackageState {

    //Singleton
    private static Acknowledged instance =new Acknowledged();

    private Acknowledged(){}

    public static Acknowledged getInstance(){
        return instance;
    }

    /**
     * 定义了6种状态
     * 1-已下单
     * 2-仓库处理中
     * 3-运输中
     * 4-派送中
     * 5-待取件
     * 6-已签收
     *
     * @param ctx
     */
    @Override
    public void updateState(PackageContext ctx) {
        ctx.setCurrentState(WarehouseProcessing.getInstance());
    }
}