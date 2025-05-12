package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static io.github.dekkerding.examples.Random.randomList;

@SpringBootTest
public class Merger {


    @Test
    @Monitoring
    public void mergerSort() {
        List<Integer> merger = new ArrayList<>();
        List<List<Integer>> list = randomList(3);
        for (List<Integer> metadataList : list) {
            List<Integer> group = mergerSortCore(metadataList, 0, metadataList.size() - 1);
            System.out.println("排序后的结果: " + group);
            merger.addAll(group);
        }
        System.out.println("全部排序后的结果: " + mergerSortCore(merger, 0, merger.size() - 1));
    }

    public List<Integer> mergerSortCore(List<Integer> resultdataList, int start, int end) {
        List<Integer> list = new ArrayList<>();
        if (end == start) {
            list.add(resultdataList.get(start));
            return list;
        }

        int mid = start + (end - start) / 2; //中位数
        List<Integer> left = mergerSortCore(resultdataList, start, mid);
        List<Integer> right = mergerSortCore(resultdataList, mid + 1, end);

        int l = 0, r = 0, n = 0;

        while (l < left.size() && r < right.size()) {
            list.add(n++, left.get(l) < right.get(r) ? left.get(l++) : right.get(r++));
        }
        while (l < left.size()) {
            list.add(n++, left.get(l++));
        }
        while (r < right.size()) {
            list.add(n++, right.get(r++));
        }
        return list;
    }
}