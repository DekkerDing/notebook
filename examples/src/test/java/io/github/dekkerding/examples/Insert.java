package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;

import static io.github.dekkerding.examples.Random.randomList;

@SpringBootTest
public class Insert {

    @Test
    @Monitoring
    public void insertSort() {
        List<List<Integer>> list = randomList(3);
        for (List<Integer> metadataList : list) {
            insertSortCore(metadataList);
        }
    }

    private void insertSortCore(List<Integer> list) {
        for (int i = 0; i < list.size(); i++) {
            // 判断轮数
            int n = i;
            while (n > 0) {
                if (list.get(n) < list.get(n - 1)) {
                    // 左侧元素比较 当左侧元素n－1更大时，执行换位
                    Collections.swap(list, n, n - 1);
                    n--;
                } else {
                    // 当不满足条件（左侧元素小于或等于当前元素）时 跳出当前循环
                    break;
                }
            }
        }
        System.out.println("排序后的结果: " + list);
    }
}