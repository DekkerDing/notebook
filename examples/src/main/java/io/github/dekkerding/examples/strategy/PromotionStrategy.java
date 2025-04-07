package io.github.dekkerding.examples.strategy;

public interface PromotionStrategy {
    /**
     *  返回1代表 可以参加 满减活动
     *  返回2代表可以参加 N折优惠活动
     *  返回3代表可以参加M元秒杀活动
     * @param skuld
     * @return
     */
    int recommand(String skuld);
}