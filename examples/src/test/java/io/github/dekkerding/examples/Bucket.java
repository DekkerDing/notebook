package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.github.dekkerding.examples.Random.randomList;


@SpringBootTest
public class Bucket{
    @Test
    @ExecutionTime
    public void bucketSort(){
        List<List<Integer>> list = randomList(3);
        for (List<Integer> metadataList : list) {
            bucketSortCore(metadataList);
            System.out.println(metadataList);
        }
    }

    public void bucketSortCore(List<Integer> resultdataList){

        if (resultdataList == null || resultdataList.isEmpty()) {
            return;
        }

        int n = resultdataList.size();
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;

        for (Integer i : resultdataList) {
            Math.max(max, i);
            Math.min(min, i);
        }

        // 防止所有元素相同导致桶数为0
        if (max == min) {
            return; // 所有元素相等，无需排序
        }

        // 创建桶列表
        int bucketCount = n;
        List<List<Integer>> buckets = new ArrayList<>(bucketCount);
        for (int i = 0; i < bucketCount; i++) {
            buckets.add(new ArrayList<>());
        }

        // 将数据分配到各个桶中
        int range = max - min;
        for (Integer num : resultdataList) {
            int index = (int) ((double) (num - min) * bucketCount / range); // 浮点映射避免整除截断误差
            index = Math.max(0, Math.min(index, bucketCount - 1)); // 索引边界保护
            buckets.get(index).add(num);
        }

        // 对每个桶排序并写回原列表
        int sortedIndex = 0;
        for (List<Integer> bucket : buckets) {
            Collections.sort(bucket);
            for (Integer num : bucket) {
                resultdataList.set(sortedIndex++, num);
            }
        }
    }
}