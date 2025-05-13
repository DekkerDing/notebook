package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static io.github.dekkerding.examples.Random.randomList;

@SpringBootTest
public class Count {

    @Test
    @Monitoring
    public void countSort() {
        List<List<Integer>> list = randomList(3);
        for (List<Integer> metadataList : list) {
            countSortCore(metadataList);
            System.out.println(metadataList);
        }
    }

    /**
     * 计数排序核心方法：对传入的整数列表进行非负整数排序
     * 支持负数，并避免因最大值过大导致内存溢出
     *
     * @param resultdataList 待排序的整数列表（原地修改）
     */
    public void countSortCore(List<Integer> resultdataList) {
        // 检查输入是否为空或长度为0，避免空指针异常
        if (resultdataList == null || resultdataList.isEmpty()) {
            return;
        }

        // 找到列表中的最大值和最小值，用于确定计数数组范围
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;

        for (Integer i : resultdataList) {
            max = Math.max(max, i);
            min = Math.min(min, i);
        }

        // 如果数据范围太大，抛出异常防止内存溢出
        if ((long) max - min > Integer.MAX_VALUE / 2) {
            throw new IllegalArgumentException("数据范围过大，不适用于计数排序");
        }

        // 创建计数数组，大小为 max - min + 1，通过偏移量支持负数
        int[] countArray = new int[max - min + 1];

        // 统计每个数字出现的次数
        for (Integer i : resultdataList) {
            countArray[i - min]++;
        }

        // 将排序结果写回原始列表
        int index = 0; // 当前写入位置
        for (int i = 0; i < countArray.length; i++) {
            // 对应的真实数值 = i + min
            int value = i + min;
            // 写入 countArray[i] 次该值
            for (int j = 0; j < countArray[i]; j++) {
                resultdataList.set(index++, value);
            }
        }
    }

}