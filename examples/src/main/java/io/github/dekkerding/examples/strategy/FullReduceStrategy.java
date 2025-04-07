package io.github.dekkerding.examples.strategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FullReduceStrategy implements PromotionStrategy{
    /**
     * 返回1代表 可以参加 满减活动
     * 返回2代表可以参加 N折优惠活动
     * 返回3代表可以参加M元秒杀活动
     *
     * @param skuld
     * @return
     */
    @Override
    public int recommand(String skuld) {
        log.info("满减活动");
        return 1;
    }
}