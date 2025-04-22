package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
public class ListNode {
    int val;
    ListNode next;

    private ListNode(int val, ListNode next) {
        this.val = val;
        this.next = next;
    }

    @ParameterizedTest
    public static ListNode create() {
        return new ListNode(0, null);
    }

    @ParameterizedTest
    public static ListNode create(int val) {
        return new ListNode(val, null);
    }

    @ParameterizedTest
    public static ListNode create(int val, ListNode next) {
        return new ListNode(val, next);
    }


    // 归并排序方法
    public static ListNode sortList(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode mid = getMid(head);
        ListNode left = head;
        ListNode right = mid.next;
        mid.next = null;

        left = sortList(left);
        right = sortList(right);
        return merge(left, right);
    }

    // 获取链表中点
    private static ListNode getMid(ListNode head) {
        ListNode slow = head;
        ListNode fast = head.next;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    // 合并两个有序链表
    private static ListNode merge(ListNode left, ListNode right) {
        ListNode dummy = ListNode.create();
        ListNode tail = dummy;
        while (left != null && right != null) {
            if (left.val < right.val) {
                tail.next = left;
                left = left.next;
            } else {
                tail.next = right;
                right = right.next;
            }
            tail = tail.next;
        }
        if (left != null) {
            tail.next = left;
        } else {
            tail.next = right;
        }
        return dummy.next;
    }

    // 打印链表方法（用于测试）
    private static void printList(ListNode head) {
        ListNode curr = head;
        while (curr != null) {
            System.out.print(curr.val + " ");
            curr = curr.next;
        }
        System.out.println();
    }

    // 测试场景 1: 普通链表排序
    public static void testSortList_Normal() {
        ListNode head = ListNode.create(3);
        head.next = ListNode.create(2);
        head.next.next = ListNode.create(1);

        head = sortList(head);
        printList(head);

        assertEquals(1, head.val);
        assertEquals(2, head.next.val);
        assertEquals(3, head.next.next.val);
    }

    // 测试场景 2: 空链表
    @Test
    public static void testSortList_Empty() {
        ListNode head = null;
        head = sortList(head);
        printList(head);
        assertNull(head);
    }

    // 测试场景 3: 单节点链表

    public static void testSortList_SingleNode() {
        ListNode head = ListNode.create(42);
        head = sortList(head);
        printList(head);
        assertEquals(42, head.val);
        assertNull(head.next);
    }

    // 测试场景 4: 已排序链表
    public static void testSortList_AlreadySorted() {
        ListNode head = ListNode.create(1);
        head.next = ListNode.create(2);
        head.next.next = ListNode.create(3);

        head = sortList(head);

        printList(head);

        assertEquals(1, head.val);
        assertEquals(2, head.next.val);
        assertEquals(3, head.next.next.val);
    }

    // 测试场景 5: 逆序链表
    public static void testSortList_ReverseSorted() {
        ListNode head = ListNode.create(3);
        head.next = ListNode.create(2);
        head.next.next = ListNode.create(1);

        head = sortList(head);

        printList(head);

        assertEquals(1, head.val);
        assertEquals(2, head.next.val);
        assertEquals(3, head.next.next.val);
    }

    public static void main(String[] args) {
//        testSortList_ReverseSorted();
//        testSortList_AlreadySorted();
//        testSortList_SingleNode();
//        testSortList_Empty();
//        testSortList_Normal();
    }
}