package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class Bucket{
    @Test
    @ExecutionTime
    public void bucketSort(){
        bucketSortCore();
    }

    public void bucketSortCore(){
        System.out.println("执行桶排序核心逻辑");
    }
}