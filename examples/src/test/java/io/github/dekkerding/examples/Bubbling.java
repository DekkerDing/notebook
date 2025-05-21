package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.dekkerding.examples.Random.randomList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class Bubbling {

    // 单向链表节点类
    static class ListNode {
        int val;
        ListNode next;

        ListNode(int val) {
            this.val = val;
            this.next = null;
        }
    }

    // 双向链表节点类
    static class DoublyListNode {
        int val;
        DoublyListNode prev;
        DoublyListNode next;

        DoublyListNode(int val) {
            this.val = val;
            this.prev = null;
            this.next = null;
        }
    }

    // 冒泡排序方法
    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    // 交换 arr[j] 和 arr[j+1]
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
    }

    // 测试场景 1: 普通数组排序
    @Test
    public void testBubbleSort_NormalArray() {
        int[] arr = {64, 34, 25, 12, 22, 11, 90};
        int[] expected = {11, 12, 22, 25, 34, 64, 90};
        bubbleSort(arr);
        assertArrayEquals(expected, arr, "普通数组排序失败");
    }

    // 测试场景 2: 已排序数组
    @Test
    public void testBubbleSort_AlreadySorted() {
        int[] arr = {1, 2, 3, 4, 5, 6, 7};
        int[] expected = {1, 2, 3, 4, 5, 6, 7};
        bubbleSort(arr);
        assertArrayEquals(expected, arr, "已排序数组测试失败");
    }

    // 测试场景 3: 逆序数组
    @Test
    public void testBubbleSort_ReverseSorted() {
        int[] arr = {7, 6, 5, 4, 3, 2, 1};
        int[] expected = {1, 2, 3, 4, 5, 6, 7};
        bubbleSort(arr);
        assertArrayEquals(expected, arr, "逆序数组排序失败");
    }

    // 测试场景 4: 包含重复元素的数组
    @Test
    public void testBubbleSort_DuplicateElements() {
        int[] arr = {5, 3, 8, 3, 1, 5, 8};
        int[] expected = {1, 3, 3, 5, 5, 8, 8};
        bubbleSort(arr);
        assertArrayEquals(expected, arr, "包含重复元素的数组排序失败");
    }

    // 测试场景 5: 空数组
    @Test
    public void testBubbleSort_EmptyArray() {
        int[] arr = {};
        int[] expected = {};
        bubbleSort(arr);
        assertArrayEquals(expected, arr, "空数组测试失败");
    }

    // 测试场景 6: 单元素数组
    @Test
    public void testBubbleSort_SingleElement() {
        int[] arr = {42};
        int[] expected = {42};
        bubbleSort(arr);
        assertArrayEquals(expected, arr, "单元素数组测试失败");
    }

    // 单向链表冒泡排序
    public static ListNode bubbleSortLinkedList(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }

        ListNode dummy = new ListNode(0);
        dummy.next = head;
        boolean swapped;

        do {
            swapped = false;
            ListNode prev = dummy;
            ListNode curr = dummy.next;

            while (curr.next != null) {
                if (curr.val > curr.next.val) {
                    ListNode nextNode = curr.next;
                    prev.next = nextNode;
                    curr.next = nextNode.next;
                    nextNode.next = curr;
                    swapped = true;
                }
                prev = curr;
                curr = curr.next;
            }
        } while (swapped);

        return dummy.next;
    }

    // 双向链表冒泡排序
    public static DoublyListNode bubbleSortDoublyLinkedList(DoublyListNode head) {
        if (head == null || head.next == null) {
            return head;
        }

        DoublyListNode dummy = new DoublyListNode(0);
        dummy.next = head;
        head.prev = dummy;
        boolean swapped;

        do {
            swapped = false;
            DoublyListNode curr = dummy.next;

            while (curr.next != null) {
                if (curr.val > curr.next.val) {
                    DoublyListNode nextNode = curr.next;
                    curr.next = nextNode.next;
                    if (nextNode.next != null) {
                        nextNode.next.prev = curr;
                    }
                    nextNode.prev = curr.prev;
                    curr.prev.next = nextNode;
                    nextNode.next = curr;
                    curr.prev = nextNode;
                    swapped = true;
                } else {
                    curr = curr.next;
                }
            }
        } while (swapped);

        return dummy.next;
    }

    // 队列冒泡排序
    public static Queue<Integer> bubbleSortQueue(Queue<Integer> queue) {
        List<Integer> list = new ArrayList<>(queue);
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (list.get(j) > list.get(j + 1)) {
                    Collections.swap(list, j, j + 1);
                }
            }
        }
        return new LinkedList<>(list);
    }

    // 栈冒泡排序
    public static Stack<Integer> bubbleSortStack(Stack<Integer> stack) {
        List<Integer> list = new ArrayList<>(stack);
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (list.get(j) > list.get(j + 1)) {
                    Collections.swap(list, j, j + 1);
                }
            }
        }
        Stack<Integer> sortedStack = new Stack<>();
        sortedStack.addAll(list);
        return sortedStack;
    }

    // 列表集合冒泡排序
    public static List<Integer> bubbleSortList(List<Integer> list) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (list.get(j) > list.get(j + 1)) {
                    Collections.swap(list, j, j + 1);
                }
            }
        }
        return list;
    }

    // 测试场景 1: 单向链表冒泡排序
    @Test
    public void testBubbleSortLinkedList() {
        ListNode head = new ListNode(3);
        head.next = new ListNode(2);
        head.next.next = new ListNode(1);

        head = bubbleSortLinkedList(head);

        assertEquals(1, head.val);
        assertEquals(2, head.next.val);
        assertEquals(3, head.next.next.val);
    }

    // 测试场景 2: 双向链表冒泡排序
    @Test
    public void testBubbleSortDoublyLinkedList() {
        DoublyListNode head = new DoublyListNode(3);
        head.next = new DoublyListNode(2);
        head.next.prev = head;
        head.next.next = new DoublyListNode(1);
        head.next.next.prev = head.next;

        head = bubbleSortDoublyLinkedList(head);

        assertEquals(1, head.val);
        assertEquals(2, head.next.val);
        assertEquals(3, head.next.next.val);
    }

    // 测试场景 3: 队列冒泡排序
    @Test
    public void testBubbleSortQueue() {
        Queue<Integer> queue = new LinkedList<>(Arrays.asList(3, 2, 1));
        queue = bubbleSortQueue(queue);

        assertEquals(1, queue.poll());
        assertEquals(2, queue.poll());
        assertEquals(3, queue.poll());
    }

    // 测试场景 4: 栈冒泡排序
    @Test
    public void testBubbleSortStack() {
        Stack<Integer> stack = new Stack<>();
        stack.addAll(Arrays.asList(3, 2, 1));
        stack = bubbleSortStack(stack);
        stack.stream().forEach(item -> System.out.println(item));
        //assertEquals(1, stack.pop());
        //assertEquals(2, stack.pop());
        //assertEquals(3, stack.pop());
    }

    // 测试场景 5: 列表集合冒泡排序
    @Test
    public void testBubbleSortList() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        list = bubbleSortList(list);

        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    /**
     *  n2
     */
    @Test
    @Monitoring
    public void bubbleSortRandom() {
        schedule(randomList(3));
    }

    public void schedule(List<List<Integer>> randomList) {

        List<Integer> resultList = new LinkedList<>();

        // 打印生成的随机数集合
        for (List<Integer> group : randomList) {
            int n = group.size();
            boolean flag = true;
            for (int i = 0; i < n - 1; i++) { // 冒泡次数 轮数
                for (int j = 0; j < n - i - 1; j++) {
                    // 冒泡步骤
                    if (group.get(j) > group.get(j + 1)) {
                        flag = false;
                        Collections.swap(group, j, j + 1);
                    }
                }
                if (flag) {
                    break;
                }
            }
            System.out.println("排序后的随机数组: " + group);
            resultList.addAll(group); // 合并所有排序后的列表
        }
        System.out.println("全部排序后的结果: " + resultList.parallelStream().sorted().collect(Collectors.toList()));
    }

    public void swwap(int[] array, int index1, int index2) {
        array[index1] = array[index1] ^ array[index2];
        array[index2] = array[index1] ^ array[index2];
        array[index1] = array[index1] ^ array[index2];
    }

    public int[] randomArray(int n) {
        int[] array = new int[n];
        for (int i = 0; i < n; i++) {
            array[i] = (int) (Math.random() * 100);
        }
        return array;
    }
}