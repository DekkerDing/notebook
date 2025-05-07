package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
public class test {

    /**
     *  给定一个数组 nums，编写一个函数将所有 0 移动到数组的末尾，同时保持非零元素的相对顺序。
     *
     * 请注意 ，必须在不复制数组的情况下原地对数组进行操作。
     *
     *
     *
     * 示例 1:
     *
     * 输入: nums = [0,1,0,3,12]
     * 输出: [1,3,12,0,0]
     * 示例 2:
     *
     * 输入: nums = [0]
     * 输出: [0]
     *
     *
     * 提示:
     *
     * 1 <= nums.length <= 104
     * -231 <= nums[i] <= 231 - 1
     */

    @Test
    public void s1(){

        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(1);
        list.add(0);
        list.add(3);
        list.add(12);
        System.out.println(append(core(list),0));
    }

    public  List<Integer> core(List<Integer> list){
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
        return list.stream().filter(x -> x != 0).collect(Collectors.toList());
    }

    public List<Integer> append (List<Integer> list, int target ){
        list.add(target);
        return list;
    }

    /**
     * 给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出 和为目标值 target  的那 两个 整数，并返回它们的数组下标。
     *
     * 你可以假设每种输入只会对应一个答案，并且你不能使用两次相同的元素。
     *
     * 你可以按任意顺序返回答案。
     *
     *
     *
     * 示例 1：
     *
     * 输入：nums = [2,7,11,15], target = 9
     * 输出：[0,1]
     * 解释：因为 nums[0] + nums[1] == 9 ，返回 [0, 1] 。
     * 示例 2：
     *
     * 输入：nums = [3,2,4], target = 6
     * 输出：[1,2]
     * 示例 3：
     *
     * 输入：nums = [3,3], target = 6
     * 输出：[0,1]
     */

    // 桶排序


}