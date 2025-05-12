package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Comparator;
import java.util.List;

import static io.github.dekkerding.examples.Random.randomList;

@SpringBootTest
public class Quick {

    /**
     * 先挑选数组中的某个元素，它将作为所有元素排列大小的分界值作为分界值的数组元素称之为：枢纽元(pivot)
     * 需要将比枢纽元小的元素放在其左侧位置，将比枢纽元大的元素放在其右侧位置
     * 并不关心其左侧区域或右侧区域内的各元素是否有序
     * 将原数组根据枢纽元划分开来的过程称之为：分区 (Partition)
     */
    @Test
    @ExecutionTime
    public void quickSort() {
        List<List<Integer>> list = randomList(3);
        for (List<Integer> metadataList : list) {
            quickSortCore(metadataList,0,metadataList.size()-1);
            System.out.println(metadataList);
        }

    }

    public void quickSortCore(List<Integer> resultdataList, int left,  int right) {
        if(left<right){
            int pivot = resultdataList.get(left); // 获取基准值
            int l = left, r = right;
            while (l < r) {
                // 从右往左找小于 pivot 的数
                while (l < r && resultdataList.get(r) >= pivot) {
                    r--;
                }
                if (l < r) {
                    resultdataList.set(l, resultdataList.get(r));
                    l++;
                }

                // 从左往右找大于等于 pivot 的数
                while (l < r && resultdataList.get(l) < pivot) {
                    l++;
                }
                if (l < r) {
                    resultdataList.set(r, resultdataList.get(l));
                    r--;
                }
            }
            // 把 pivot 放回中间位置
            resultdataList.set(l, pivot);

            // 递归排序左右两部分
            quickSortCore(resultdataList, left, l - 1);  // 排序左半部分
            quickSortCore(resultdataList, l + 1, right); // 排序右半部分
        }
    }

//    public void quickSortCore(List<Integer> resultdataList, int left, int right) {
//        quickSortWithComparator(resultdataList, left, right, Comparator.naturalOrder()); // 默认升序
//    }

    public void quickSortDescending(List<Integer> resultdataList, int left, int right) {
        quickSortWithComparator(resultdataList, left, right, Comparator.reverseOrder()); // 降序
    }

    /**
     * 带比较器的通用快速排序核心方法
     */
    public void quickSortWithComparator(List<Integer> resultdataList, int left, int right, Comparator<Integer> comparator) {
        if (left < right) {
            int pivot = resultdataList.get(left);
            int l = left, r = right;

            System.out.println("进入排序区间 [" + left + ", " + right + "]，基准值：" + pivot);
            System.out.println("初始数组：" + resultdataList);

            while (l < r) {
                // 使用 comparator 控制比较方向
                while (l < r && comparator.compare(resultdataList.get(r), pivot) >= 0) {
                    System.out.println("r-- (" + r + ") -> 值：" + resultdataList.get(r));
                    r--;
                }
                if (l < r) {
                    resultdataList.set(l, resultdataList.get(r));
                    System.out.println("将右指针值 " + resultdataList.get(r) + " 移动到左指针位置 " + l);
                    l++;
                }

                while (l < r && comparator.compare(resultdataList.get(l), pivot) < 0) {
                    System.out.println("l++ (" + l + ") -> 值：" + resultdataList.get(l));
                    l++;
                }
                if (l < r) {
                    resultdataList.set(r, resultdataList.get(l));
                    System.out.println("将左指针值 " + resultdataList.get(l) + " 移动到右指针位置 " + r);
                    r--;
                }
            }

            resultdataList.set(l, pivot);
            System.out.println("放置 pivot 到位置 " + l + "，当前数组：" + resultdataList);

            // 递归排序左右子数组
            System.out.println("递归排序左半部分 [" + left + ", " + (l - 1) + "]");
            quickSortWithComparator(resultdataList, left, l - 1, comparator);
            System.out.println("递归排序右半部分 [" + (l + 1) + ", " + right + "]");
            quickSortWithComparator(resultdataList, l + 1, right, comparator);

            System.out.println("完成排序区间 [" + left + ", " + right + "]，当前数组：" + resultdataList);
        }
    }

}