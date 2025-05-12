package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static io.github.dekkerding.examples.Random.randomList;

@SpringBootTest
public class Modulu {

    @Test
    @Monitoring
    public void moduluSort() {
        List<List<Integer>> list = randomList(3);
        for (List<Integer> metadataList : list) {
            moduluSortCore(metadataList);
            System.out.println(metadataList);
        }
    }

public void moduluSortCore(List<Integer> resultdataList) {
    if (resultdataList == null || resultdataList.isEmpty()) {
        return;
    }

    // 找到最大值并计算位数
    int max = Integer.MIN_VALUE;
    for (Integer num : resultdataList) {
        max = Math.max(max, num);
    }

    int maxDigits = String.valueOf(max).length();

        // 创建10个桶
        List<LinkedList<Integer>> buckets = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            buckets.add(new LinkedList<>());
        }

        // 开始按位排序
        for (int digit = 0; digit < maxDigits; digit++) {

            // 清空桶内容，复用结构
            for (LinkedList<Integer> bucket : buckets) {
                bucket.clear();
            }

            // 分配数据到桶中
            for (Integer num : resultdataList) {
                int digitValue = getDigit(num, digit);
                buckets.get(digitValue).add(num);
            }

            // 回收数据
            int index = 0;
            for (LinkedList<Integer> bucket : buckets) {
                while (!bucket.isEmpty()) {
                    resultdataList.set(index++, bucket.removeFirst());
                }
            }
        }
}

    /**
     * 获取指定位置的数字（从右往左，0 表示个位）
     */
    private int getDigit(int number, int digitPos) {
        return (number / (int) Math.pow(10, digitPos)) % 10;
    }
}