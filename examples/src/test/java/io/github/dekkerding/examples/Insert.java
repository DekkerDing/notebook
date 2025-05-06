package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static io.github.dekkerding.examples.Random.randomList;

@SpringBootTest
public class Insert {

    @Test
    public void insertSort(int[] arr) {
        List<List<Integer>> list = randomList(3);
        for (List<Integer> metadataList : list) {
            insertSortCore(metadataList);
        }
    }

    private static void insertSortCore(List<Integer> list) {
    }

}