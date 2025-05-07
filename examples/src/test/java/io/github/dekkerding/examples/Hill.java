package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;

import static io.github.dekkerding.examples.Random.randomList;

@SpringBootTest
public class Hill {

    @Test
    public void hillSort() {
        List<List<Integer>> list = randomList(3);
        for (List<Integer> metadataList : list) {
            hillSortCore(metadataList);
        }
    }

    private void hillSortCore(List<Integer> resultdataList) {
        /**
         * 多轮循环：缩减增量
         * 初始条件：增量（gap）值为数组长度除以2
         * 循环条件：增量>0 条件自变：增量自除2
         */
        for (int gap = resultdataList.size() / 2; gap > 0; gap /= 2){
            // 从与增量值大小相等的下标位置开始，向右循环
            for (int i = gap; i < resultdataList.size(); i++){
                // 接下来的循环表示向左找“同组“的元素尝试对比及必要的换位
                //左侧“同组“元素的下标，即被对比元素的下标 因为可能有多个，所以需要循环
                for (int j = i - gap; j >= 0; j -= gap){
                    // 当左侧元素小于或等于当前元素时，跳出当前循环
                    if (resultdataList.get(j) <= resultdataList.get(j + gap)){
                        break;
                    }
                    // 当左侧元素大于当前元素时，将左侧元素与当前元素进行交换
                    Collections.swap(resultdataList, j, j + gap);
                }
            }
        }
        System.out.println("排序后的结果: " + resultdataList);
    }
}