package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.dekkerding.examples.Random.randomList;

@SpringBootTest
public class Select {

    @Test
    public void selectSort() {
        List<List<Integer>> list = randomList(3);
        List<Integer> mergeList = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            List<Integer> group = list.get(i);
            System.out.println("第 " + (i + 1) + " 组原始数据: " + group);
            selectSortCore(group); // 调用选择排序
            System.out.println("第 " + (i + 1) + " 组排序后数据: " + group);
            mergeList.addAll(group);
        }
        System.out.println("全部排序后的结果: " + mergeList.parallelStream().sorted().collect(Collectors.toList()));
    }

    private static void selectSortCore(List<Integer> list) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            // 假设当前索引为最小值位置
            int minIndex = i;
            for (int j = i + 1; j < n; j++) {
                if (list.get(j) < list.get(minIndex)) {
                    minIndex = j; // 找到更小的值则更新最小值索引
                }
            }
            // 将找到的最小值交换到前面
            Collections.swap(list, i, minIndex);
        }
    }
}