package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class Random {

    @Test
    public static List<List<Integer>> randomList(int groupNumber) {
        // 生成三组随机数集合
        List<List<Integer>> randomList = new ArrayList<>();

        for (int i = 0; i < groupNumber; i++) {
            int setSize = (int) (Math.random() * 5) + 5; // 每组数量在 5~9 之间
            List<Integer> set = new ArrayList<>();
            for (int j = 0; j < setSize; j++) {
                int randomNumber = (int) (Math.random() * 10) + 1; // 1~10 的随机数
                set.add(randomNumber);
            }
            randomList.add(set);
        }
        System.out.println("生成随机数集合: " + randomList);
        return randomList;
    }
}