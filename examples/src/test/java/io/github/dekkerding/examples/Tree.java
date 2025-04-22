package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
public class Tree {

    // 辅助类：树节点
    static class TreeNode<T> {
        T value;  // 节点值
        TreeNode<T> parent;  // 父节点
        List<TreeNode<T>> children = new ArrayList<>();  // 子节点列表

        public TreeNode(T value) {
            this.value = value;
        }

        // 添加子节点
        public void addChild(TreeNode<T> child) {
            child.parent = this;
            children.add(child);
        }

        @Override
        public String toString() {
            return "TreeNode{" +
                    "value=" + value +
                    ", children=" + children +
                    '}';
        }
    }

    // 辅助类：双向树节点（继承自树节点）
    static class BiDirectionalTreeNode<T> extends TreeNode<T> {
        BiDirectionalTreeNode<T> next;  // 后继节点
        BiDirectionalTreeNode<T> prev;  // 前驱节点

        public BiDirectionalTreeNode(T value) {
            super(value);
        }

        // 在BiDirectionalTreeNode类中添加以下方法

        /**
         * getAllNodes方法通过广度优先遍历（BFS）收集所有节点
         * 并返回一个包含所有节点的列表
         * 这样在buildSortedBiDirectionalTree方法中就可以正确调用getAllNodes来获取所有节点并设置双向链接
         *
         * @return
         */
        public List<BiDirectionalTreeNode<T>> getAllNodes() {
            List<BiDirectionalTreeNode<T>> nodes = new ArrayList<>();
            Queue<BiDirectionalTreeNode<T>> queue = new LinkedList<>();
            queue.add(this);

            while (!queue.isEmpty()) {
                BiDirectionalTreeNode<T> node = queue.poll();
                nodes.add(node);
                for (TreeNode<T> child : node.children) {
                    queue.add((BiDirectionalTreeNode<T>) child);
                }
            }

            return nodes;
        }

    }

    // 辅助类：链表节点
    static class ListNode<T> {
        T value;  // 节点值
        ListNode<T> next;  // 指向下一个节点

        public ListNode(T value) {
            this.value = value;
        }
    }

    // 辅助方法：构建树结构
    public TreeNode<Integer> buildTree(int rootValue) {
        // 创建根节点
        TreeNode<Integer> root = new TreeNode<>(rootValue);

        // 添加子节点
        root.addChild(new TreeNode<>(4));
        root.addChild(new TreeNode<>(1));

        // 为第一个子节点添加孙节点
        root.children.get(0).addChild(new TreeNode<>(6));

        // 为第二个子节点添加孙节点
        root.children.get(1).addChild(new TreeNode<>(2));

        // 为孙节点添加曾孙节点
        root.children.get(1).children.get(0).addChild(new TreeNode<>(5));

        return root;
    }

    // 辅助方法：构建双向树结构
    public BiDirectionalTreeNode<Integer> buildBiDirectionalTree(int rootValue) {
        // 创建各种节点
        BiDirectionalTreeNode<Integer> root = new BiDirectionalTreeNode<>(rootValue);
        BiDirectionalTreeNode<Integer> node4 = new BiDirectionalTreeNode<>(4);
        BiDirectionalTreeNode<Integer> node1 = new BiDirectionalTreeNode<>(1);
        BiDirectionalTreeNode<Integer> node6 = new BiDirectionalTreeNode<>(6);
        BiDirectionalTreeNode<Integer> node2 = new BiDirectionalTreeNode<>(2);
        BiDirectionalTreeNode<Integer> node5 = new BiDirectionalTreeNode<>(5);

        // 构建树结构
        root.addChild(node4);
        root.addChild(node1);
        node4.addChild(node6);
        node1.addChild(node2);
        node2.addChild(node5);

        // 设置双向链接
        node4.prev = root;
        node4.next = node1;

        node1.prev = root;
        node1.next = node6;

        node6.prev = node4;
        node6.next = node2;

        node2.prev = node1;
        node2.next = node5;

        node5.prev = node2;

        return root;
    }

    // 辅助方法：构建链表
    public ListNode<Integer> buildLinkedList(int... values) {
        if (values.length == 0) return null;  // 如果没有值，返回空链表

        // 创建头节点
        ListNode<Integer> head = new ListNode<>(values[0]);
        ListNode<Integer> current = head;

        // 添加后续节点
        for (int i = 1; i < values.length; i++) {
            current.next = new ListNode<>(values[i]);
            current = current.next;
        }

        return head;
    }

    // 辅助方法：将栈转换为树
    public TreeNode<Integer> stackToTree(Stack<Integer> stack) {
        if (stack.isEmpty()) return null;  // 如果栈为空，返回空树

        // 创建根节点（使用栈顶元素）
        TreeNode<Integer> root = new TreeNode<>(stack.pop());
        TreeNode<Integer> current = root;

        // 依次弹出栈中元素并创建子节点
        while (!stack.isEmpty()) {
            TreeNode<Integer> newNode = new TreeNode<>(stack.pop());
            current.addChild(newNode);
            current = newNode;
        }

        return root;
    }

    // 辅助方法：将链表转换为树
    public TreeNode<Integer> linkedListToTree(ListNode<Integer> head) {
        if (head == null) return null;  // 如果链表为空，返回空树

        // 创建根节点（使用链表头节点值）
        TreeNode<Integer> root = new TreeNode<>(head.value);
        TreeNode<Integer> current = root;

        // 遍历链表并创建树节点
        while (head.next != null) {
            TreeNode<Integer> newNode = new TreeNode<>(head.next.value);
            current.addChild(newNode);
            current = newNode;
            head = head.next;
        }

        return root;
    }

    // 辅助方法：将列表集合转换为树
    public TreeNode<Integer> listToTree(List<Integer> list) {
        if (list.isEmpty()) return null;  // 如果列表为空，返回空树

        // 创建根节点（使用列表第一个元素）
        TreeNode<Integer> root = new TreeNode<>(list.get(0));
        TreeNode<Integer> current = root;

        // 遍历列表并创建树节点
        for (int i = 1; i < list.size(); i++) {
            TreeNode<Integer> newNode = new TreeNode<>(list.get(i));
            current.addChild(newNode);
            current = newNode;
        }

        return root;
    }

    // 辅助方法：对树进行快速排序
    public void quickSortTree(TreeNode<Integer> node) {
        if (node == null || node.children.isEmpty()) return;  // 如果节点为空或没有子节点，直接返回

        // 统计所有节点值
        List<Integer> values = new ArrayList<>();
        collectValues(node, values);

        // 快速排序
        quickSort(values);

        // 重新构建树（这里简化为按排序后的值重新构建）
        TreeNode<Integer> newRoot = buildSortedTree(values);
        node.value = newRoot.value;  // 更新根节点值
        node.children.clear();  // 清空子节点
        node.children.addAll(newRoot.children);  // 添加新的子节点
    }

    // 辅助方法：收集树中所有节点值
    private void collectValues(TreeNode<Integer> node, List<Integer> values) {
        values.add(node.value);  // 添加当前节点值
        for (TreeNode<Integer> child : node.children) {
            collectValues(child, values);  // 递归添加子节点值
        }
    }

    // 辅助方法：对列表进行快速排序
    private void quickSort(List<Integer> list) {
        if (list.size() <= 1) return;  // 如果列表长度小于等于1，直接返回

        int pivot = list.get(list.size() / 2);  // 选择中间元素作为基准
        List<Integer> less = new ArrayList<>();  // 存储小于基准的元素
        List<Integer> equal = new ArrayList<>();  // 存储等于基准的元素
        List<Integer> greater = new ArrayList<>();  // 存储大于基准的元素

        for (int num : list) {
            if (num < pivot) less.add(num);
            else if (num > pivot) greater.add(num);
            else equal.add(num);
        }

        // 递归排序子列表
        quickSort(less);
        quickSort(greater);

        // 合并排序后的列表
        List<Integer> sorted = new ArrayList<>();
        sorted.addAll(less);
        sorted.addAll(equal);
        sorted.addAll(greater);
        list.clear();
        list.addAll(sorted);
    }

    // 辅助方法：根据排序后的值构建树
    private TreeNode<Integer> buildSortedTree(List<Integer> sortedValues) {
        if (sortedValues.isEmpty()) return null;

        // 创建根节点（使用第一个排序值）
        TreeNode<Integer> root = new TreeNode<>(sortedValues.get(0));
        TreeNode<Integer> current = root;

        // 根据排序值依次创建子节点
        for (int i = 1; i < sortedValues.size(); i++) {
            TreeNode<Integer> newNode = new TreeNode<>(sortedValues.get(i));
            current.addChild(newNode);
            current = newNode;
        }

        return root;
    }

    // 辅助方法：对双向树进行排序
    public void sortBiDirectionalTree(BiDirectionalTreeNode<Integer> root) {
        if (root == null) return;  // 如果根节点为空，直接返回

        // 统计所有节点值并排序
        List<Integer> values = new ArrayList<>();
        collectValues(root, values);
        Collections.sort(values);

        // 重新构建双向树（这里简化为按排序后的值重新构建）
        BiDirectionalTreeNode<Integer> newRoot = buildSortedBiDirectionalTree(values);

        // 更新原有树结构
        root.value = newRoot.value;
        root.children.clear();
        root.children.addAll(newRoot.children);

        // 更新双向链接
        updateBiDirectionalLinks(root, newRoot);
    }

    // 辅助方法：收集双向树中所有节点值
    private void collectValues(BiDirectionalTreeNode<Integer> node, List<Integer> values) {
        values.add(node.value);  // 添加当前节点值
        for (TreeNode<Integer> child : node.children) {
            collectValues(child, values);  // 递归添加子节点值
        }
    }

    // 辅助方法：根据排序后的值构建双向树
    private BiDirectionalTreeNode<Integer> buildSortedBiDirectionalTree(List<Integer> sortedValues) {
        if (sortedValues.isEmpty()) return null;

        // 创建根节点（使用第一个排序值）
        BiDirectionalTreeNode<Integer> root = new BiDirectionalTreeNode<>(sortedValues.get(0));
        BiDirectionalTreeNode<Integer> current = root;

        // 根据排序值依次创建子节点
        for (int i = 1; i < sortedValues.size(); i++) {
            BiDirectionalTreeNode<Integer> newNode = new BiDirectionalTreeNode<>(sortedValues.get(i));
            current.addChild(newNode);
            current = newNode;
        }

        // 设置双向链接
        BiDirectionalTreeNode<Integer> prev = null;
        for (BiDirectionalTreeNode<Integer> node : root.getAllNodes()) {
            if (prev != null) {
                node.prev = prev;  // 设置前驱节点
                prev.next = node;  // 设置后继节点
            }
            prev = node;
        }

        return root;
    }

    // 辅助方法：更新双向树的双向链接
    private void updateBiDirectionalLinks(BiDirectionalTreeNode<Integer> oldTree, BiDirectionalTreeNode<Integer> newTree) {
        // 实际应用中需要更复杂的逻辑来维护双向链接
    }

    // 辅助方法：获取树的所有节点（广度优先）
    public List<TreeNode<Integer>> getAllTreeNodes(TreeNode<Integer> root) {
        List<TreeNode<Integer>> nodes = new ArrayList<>();
        if (root == null) return nodes;  // 如果根节点为空，返回空列表

        // 使用队列实现广度优先遍历
        Queue<TreeNode<Integer>> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode<Integer> node = queue.poll();
            nodes.add(node);
            for (TreeNode<Integer> child : node.children) {
                queue.add(child);
            }
        }

        return nodes;
    }

    // 辅助方法：获取双向树的所有节点（广度优先）
    public List<BiDirectionalTreeNode<Integer>> getAllBiDirectionalTreeNodes(BiDirectionalTreeNode<Integer> root) {
        List<BiDirectionalTreeNode<Integer>> nodes = new ArrayList<>();
        if (root == null) return nodes;  // 如果根节点为空，返回空列表

        // 使用队列实现广度优先遍历
        Queue<BiDirectionalTreeNode<Integer>> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            BiDirectionalTreeNode<Integer> node = queue.poll();
            nodes.add(node);
            for (TreeNode child : node.children) {
                queue.add((BiDirectionalTreeNode<Integer>) child);
            }
        }

        return nodes;
    }

    // 辅助方法：获取链表的所有节点
    public List<ListNode<Integer>> getAllLinkedListNodes(ListNode<Integer> head) {
        List<ListNode<Integer>> nodes = new ArrayList<>();
        while (head != null) {
            nodes.add(head);
            head = head.next;
        }
        return nodes;
    }

    // 辅助方法：验证树结构是否正确排序
    public boolean isTreeSorted(TreeNode<Integer> root) {
        List<TreeNode<Integer>> nodes = getAllTreeNodes(root);
        List<Integer> values = new ArrayList<>();
        for (TreeNode<Integer> node : nodes) {
            values.add(node.value);
        }

        // 检查值是否按升序排列
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) < values.get(i - 1)) {
                return false;
            }
        }

        return true;
    }

    // 辅助方法：验证双向树结构是否正确排序
    public boolean isBiDirectionalTreeSorted(BiDirectionalTreeNode<Integer> root) {
        List<BiDirectionalTreeNode<Integer>> nodes = getAllBiDirectionalTreeNodes(root);
        List<Integer> values = new ArrayList<>();
        for (BiDirectionalTreeNode<Integer> node : nodes) {
            values.add(node.value);
        }

        // 检查值是否按升序排列
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) < values.get(i - 1)) {
                return false;
            }
        }

        return true;
    }

    // 辅助方法：验证双向链接是否正确
    public boolean isBiDirectionalLinksCorrect(BiDirectionalTreeNode<Integer> root) {
        BiDirectionalTreeNode<Integer> current = root;
        while (current != null) {
            // 检查双向链接是否一致
            if (current.prev != null && current.prev.next != current) {
                return false;
            }
            current = current.next;
        }
        return true;
    }

    // 单向树方式排序测试
    @Test
    public void testSingleDirectionTreeSort() {
        // 构建测试树
        TreeNode<Integer> root = buildTree(3);

        // 对树进行快速排序
        quickSortTree(root);

        // 验证排序结果
        assert isTreeSorted(root) : "单向树排序失败";
    }

    // 双向树排序测试
    @Test
    public void testBiDirectionalTreeSort() {
        // 构建双向树
        BiDirectionalTreeNode<Integer> root = buildBiDirectionalTree(3);

        // 对双向树进行排序
        sortBiDirectionalTree(root);

        // 验证排序结果
        assert isBiDirectionalTreeSorted(root) : "双向树排序失败";

        // 验证双向链接是否正确
        assert isBiDirectionalLinksCorrect(root) : "双向链接不正确";
    }

    // 树节点插入快速排序测试
    @Test
    public void testTreeNodeInsertQuickSort() {
        // 构建测试树
        TreeNode<Integer> root = buildTree(3);

        // 对树进行快速排序
        quickSortTree(root);

        // 验证排序结果
        assert isTreeSorted(root) : "树节点插入快速排序失败";
    }

    // 栈结构转换为树结构测试
    @Test
    public void testStackToTreeConversion() {
        // 创建测试栈
        Stack<Integer> stack = new Stack<>();
        Collections.addAll(stack, 5, 3, 8, 1, 4, 6, 2);

        // 将栈转换为树
        TreeNode<Integer> root = stackToTree(stack);

        // 验证转换后的树结构
        assert root != null : "转换后树为空";
        assert root.value == 5 : "根节点值不正确";
        assert root.children.size() >= 6 : "子节点数量不正确";

        // 对转换后的树进行排序
        quickSortTree(root);

        // 验证排序结果
        assert isTreeSorted(root) : "转换后的树排序失败";
    }

    // 链表结构转换为树结构测试
    @Test
    public void testLinkedListToTreeConversion() {
        // 创建测试链表
        ListNode<Integer> head = buildLinkedList(5, 3, 8, 1, 4, 6, 2);

        // 将链表转换为树
        TreeNode<Integer> root = linkedListToTree(head);

        // 验证转换后的树结构
        assert root != null : "转换后树为空";
        assert root.value == 5 : "根节点值不正确";
        assert root.children.size() >= 6 : "子节点数量不正确";

        // 对转换后的树进行排序
        quickSortTree(root);

        // 验证排序结果
        assert isTreeSorted(root) : "转换后的树排序失败";
    }

    // 列表集合结构转换为树结构测试
    @Test
    public void testListToTreeConversion() {
        // 创建测试列表
        List<Integer> list = Arrays.asList(5, 3, 8, 1, 4, 6, 2);

        // 将列表转换为树
        TreeNode<Integer> root = listToTree(list);

        // 验证转换后的树结构
        assert root != null : "转换后树为空";
        assert root.value == 5 : "根节点值不正确";
        assert root.children.size() >= 6 : "子节点数量不正确";

        // 对转换后的树进行排序
        quickSortTree(root);

        // 验证排序结果
        assert isTreeSorted(root) : "转换后的树排序失败";
    }
}