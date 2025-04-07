package io.github.dekkerding.examples.state;

public class WarehouseProcessing implements PackageState{

    //Singleton
    private static WarehouseProcessing instance =new WarehouseProcessing();

    private WarehouseProcessing(){}

    public static WarehouseProcessing getInstance(){
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

    }
}