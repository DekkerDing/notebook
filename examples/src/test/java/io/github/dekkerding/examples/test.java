package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * 给你一个整数数组 nums ，数组中的元素 互不相同 。返回该数组所有可能的子集（幂集）。
 * <p>
 * 解集 不能 包含重复的子集。你可以按 任意顺序 返回解集。
 * <p>
 * 输入：nums = [1,2,3]
 * 输出：[[],[1],[2],[1,2],[3],[1,3],[2,3],[1,2,3]]
 */
@SpringBootTest
public class test {


    @Test
    @Monitoring
    public void bucketSort() {
        List<Integer> list = new ArrayList();
        list.add(1);
        list.add(2);
        list.add(3);
        bucketSortCore(list);
    }

    public void bucketSortCore(List<Integer> resultdataList) {

        if (resultdataList == null || resultdataList.isEmpty()) {
            return;
        }

        // 计算排列层级
        int n = getCj(resultdataList);
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;

        for (Integer i : resultdataList) {
            Math.max(max, i);
            Math.min(min, i);
        }

        // 防止所有元素相同导致桶数为
        if (max == min) {
            return; // 所有元素相等，无需排序
        }

        // 创建桶列表
        int bucketCount = n;
        List<Integer> buckets = new ArrayList<>(bucketCount);

        // 将数据分配到各个桶中
        for (; ; ) {

        }
    }

    private static int getCj(List<Integer> resultdataList) {
        //初始层级
        int c = 0;

        for (int i = 0; i < resultdataList.size() - 1; i++) {
            for (int j = 0; j < resultdataList.size() - i - 1; j++) {
                c++;
            }
        }
        return c;
    }

//    public Map<String,List<String>> insertSort (List<Score> list){
//        Map map =  new HashMap<String,List<String>>();
//
//        // 分组
//
//        list.stream().collect(Collectors.groupingBy(Score::getClazz)).entrySet().stream().forEach(entry -> {
//            List<String> list_new = entry.getValue().stream().sorted((s1,s2)->s1.getScore().compareTo(s2.getScore())).map(Score::getStudent).collect(Collectors.toList());
//            map.put(entry.getKey(),list_new);
//        });
//
//        // 排序
//
//        // 收集
//
//        return map;
//    }


}