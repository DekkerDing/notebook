package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class Tree {

    // 辅助类：树节点
    static class TreeNode<T> {
        T value;
        TreeNode<T> parent;
        List<TreeNode<T>> children = new ArrayList<>();

        public TreeNode(T value) {
            this.value = value;
        }

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
        BiDirectionalTreeNode<T> next;
        BiDirectionalTreeNode<T> prev;

        public BiDirectionalTreeNode(T value) {
            super(value);
        }

        /**
         * getAllNodes方法通过广度优先遍历（BFS）收集所有节点，并返回一个包含所有节点的列表
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
        T value;
        ListNode<T> next;

        public ListNode(T value) {
            this.value = value;
        }
    }

    // 辅助方法：构建树结构
    public TreeNode<Integer> buildTree(int rootValue) {
        TreeNode<Integer> root = new TreeNode<>(rootValue);
        root.addChild(new TreeNode<>(4));
        root.addChild(new TreeNode<>(1));
        root.children.get(0).addChild(new TreeNode<>(6));
        root.children.get(1).addChild(new TreeNode<>(2));
        root.children.get(1).children.get(0).addChild(new TreeNode<>(5));
        return root;
    }

    // 辅助方法：构建双向树结构
    public BiDirectionalTreeNode<Integer> buildBiDirectionalTree(int rootValue) {
        BiDirectionalTreeNode<Integer> root = new BiDirectionalTreeNode<>(rootValue);
        BiDirectionalTreeNode<Integer> node4 = new BiDirectionalTreeNode<>(4);
        BiDirectionalTreeNode<Integer> node1 = new BiDirectionalTreeNode<>(1);
        BiDirectionalTreeNode<Integer> node6 = new BiDirectionalTreeNode<>(6);
        BiDirectionalTreeNode<Integer> node2 = new BiDirectionalTreeNode<>(2);
        BiDirectionalTreeNode<Integer> node5 = new BiDirectionalTreeNode<>(5);

        root.addChild(node4);
        root.addChild(node1);
        node4.addChild(node6);
        node1.addChild(node2);
        node2.addChild(node5);

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
        if (values.length == 0) return null;

        ListNode<Integer> head = new ListNode<>(values[0]);
        ListNode<Integer> current = head;

        for (int i = 1; i < values.length; i++) {
            current.next = new ListNode<>(values[i]);
            current = current.next;
        }

        return head;
    }

    // 辅助方法：将栈转换为树
    public TreeNode<Integer> stackToTree(Stack<Integer> stack) {
        if (stack.isEmpty()) return null;

        TreeNode<Integer> root = new TreeNode<>(stack.pop());
        TreeNode<Integer> current = root;

        while (!stack.isEmpty()) {
            TreeNode<Integer> newNode = new TreeNode<>(stack.pop());
            current.addChild(newNode);
            current = newNode;
        }

        return root;
    }

    // 辅助方法：将链表转换为树
    public TreeNode<Integer> linkedListToTree(ListNode<Integer> head) {
        if (head == null) return null;

        TreeNode<Integer> root = new TreeNode<>(head.value);
        TreeNode<Integer> current = root;

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
        if (list.isEmpty()) return null;

        TreeNode<Integer> root = new TreeNode<>(list.get(0));
        TreeNode<Integer> current = root;

        for (int i = 1; i < list.size(); i++) {
            TreeNode<Integer> newNode = new TreeNode<>(list.get(i));
            current.addChild(newNode);
            current = newNode;
        }

        return root;
    }

    // 辅助方法：对树进行快速排序
    public void quickSortTree(TreeNode<Integer> node) {
        if (node == null || node.children.isEmpty()) return;

        List<Integer> values = new ArrayList<>();
        collectValues(node, values);

        quickSort(values);

        TreeNode<Integer> newRoot = buildSortedTree(values);
        node.value = newRoot.value;
        node.children.clear();
        node.children.addAll(newRoot.children);
    }

    private void collectValues(TreeNode<Integer> node, List<Integer> values) {
        if (node == null) return;
        values.add(node.value);
        for (TreeNode<Integer> child : node.children) {
            collectValues(child, values);
        }
    }

    private void quickSort(List<Integer> list) {
        if (list.size() <= 1) return;

        int pivot = list.get(list.size() / 2);
        List<Integer> less = new ArrayList<>();
        List<Integer> equal = new ArrayList<>();
        List<Integer> greater = new ArrayList<>();

        for (int num : list) {
            if (num < pivot) less.add(num);
            else if (num > pivot) greater.add(num);
            else equal.add(num);
        }

        quickSort(less);
        quickSort(greater);

        List<Integer> sorted = new ArrayList<>();
        sorted.addAll(less);
        sorted.addAll(equal);
        sorted.addAll(greater);
        list.clear();
        list.addAll(sorted);
    }

    /**
     * 快速排序优化
     *
     * @param list
     */
    private void randomQuickSort(List<Integer> list) {
        if (list.size() <= 1) return;

        // 随机选择基准值
        int pivotIndex = new Random().nextInt(list.size());
        int pivot = list.get(pivotIndex);

        List<Integer> less = new ArrayList<>();
        List<Integer> equal = new ArrayList<>();
        List<Integer> greater = new ArrayList<>();

        for (int num : list) {
            if (num < pivot) less.add(num);
            else if (num > pivot) greater.add(num);
            else equal.add(num);
        }

        randomQuickSort(less);
        randomQuickSort(greater);

        list.clear();
        list.addAll(less);
        list.addAll(equal);
        list.addAll(greater);
    }

    /**
     * 广度优先排序（BFS
     * 广度优先排序可以按层级对树节点进行排序
     *
     * @param root
     */
    public void bfsSortTree(TreeNode<Integer> root) {
        if (root == null) return;

        List<Integer> values = new ArrayList<>();
        Queue<TreeNode<Integer>> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode<Integer> node = queue.poll();
            values.add(node.value);
            for (TreeNode<Integer> child : node.children) {
                queue.add(child);
            }
        }

        Collections.sort(values);
        rebuildTree(root, values);
    }

    private void rebuildTree(TreeNode<Integer> root, List<Integer> sortedValues) {
        if (root == null || sortedValues.isEmpty()) return;

        Queue<TreeNode<Integer>> queue = new LinkedList<>();
        queue.add(root);

        int index = 0;
        while (!queue.isEmpty()) {
            TreeNode<Integer> node = queue.poll();
            node.value = sortedValues.get(index++);
            for (TreeNode<Integer> child : node.children) {
                queue.add(child);
            }
        }
    }

    /**
     * 深度优先排序（DFS）
     * 深度优先排序可以按前序、中序或后序对树节点进行排序。
     *
     * @param root
     */
    public void dfsSortTree(TreeNode<Integer> root) {
        if (root == null) return;

        List<Integer> values = new ArrayList<>();
        collectValues(root, values);

        Collections.sort(values);
        rebuildTree(root, values);
    }

    /**
     * 堆排序
     * 堆排序可以高效地对树节点值进行排序
     *
     * @param root
     */
    public void heapSortTree(TreeNode<Integer> root) {
        if (root == null) return;

        List<Integer> values = new ArrayList<>();
        collectValues(root, values);

        PriorityQueue<Integer> minHeap = new PriorityQueue<>(values);
        rebuildTree(root, new ArrayList<>(minHeap));
    }

    /**
     * 优化堆的构建过程
     * 背景：堆排序的核心是构建堆（Heapify），传统方法是从最后一个非叶子节点开始逐个调整，时间复杂度为 O(n)
     * 优化：可以使用 Floyd 算法（自底向上构建堆），减少调整次数，进一步优化构建堆的过程
     *
     * @param list
     */
    private void buildHeap(List<Integer> list) {
        int n = list.size();
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(list, n, i);
        }
    }

    /**
     * 减少交换次数
     * 背景：在堆排序中，每次调整堆时都需要交换元素，交换操作的开销较大
     * 优化：可以使用 “延迟交换” 策略，先记录需要交换的元素，最后一次性交换，减少交换次数
     *
     * @param list
     * @param n
     * @param i
     */
    private void heapify(List<Integer> list, int n, int i) {
        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;

        if (left < n && list.get(left) > list.get(largest)) {
            largest = left;
        }
        if (right < n && list.get(right) > list.get(largest)) {
            largest = right;
        }

        if (largest != i) {
            int swap = list.get(i);
            list.set(i, list.get(largest));
            list.set(largest, swap);
            heapify(list, n, largest);
        }
    }

    /**
     * 使用迭代代替递归
     * 背景：堆排序中的 heapify 方法通常使用递归实现，递归调用会带来额外的栈空间开销。
     * 优化：将递归实现改为迭代实现，减少栈空间的使用
     *
     * @param list
     * @param n
     * @param i
     */
    private void heapifyIterative(List<Integer> list, int n, int i) {
        int largest = i;
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;

            if (left < n && list.get(left) > list.get(largest)) {
                largest = left;
            }
            if (right < n && list.get(right) > list.get(largest)) {
                largest = right;
            }

            if (largest == i) break;

            int swap = list.get(i);
            list.set(i, list.get(largest));
            list.set(largest, swap);
            i = largest;
        }
    }

    /**
     * 结合其他排序算法
     * 背景：堆排序在数据量较小时效率不如快速排序或插入排序
     * 优化：在数据量较小时，切换到更高效的排序算法（如插入排序），结合堆排序和插入排序的优点
     *
     * @param list
     */
    private void hybridSort(List<Integer> list) {
        if (list.size() <= 10) {
            insertionSort(list);
        }
    }

    private void insertionSort(List<Integer> list) {
        for (int i = 1; i < list.size(); i++) {
            int key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j) > key) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }


    /**
     * 并行化堆排序
     * 背景：堆排序的调整过程可以并行化，利用多核 CPU 提高性能
     * 优化：使用多线程并行调整堆的不同部分
     *
     * @param list
     */
    private void parallelHeapSort(List<Integer> list) {
        int n = list.size();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = n / 2 - 1; i >= 0; i--) {
            int finalI = i;
            executor.submit(() -> heapify(list, n, finalI));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = n - 1; i > 0; i--) {
            int swap = list.get(0);
            list.set(0, list.get(i));
            list.set(i, swap);
            heapify(list, i, 0);
        }
    }


    /**
     * 自定义排序
     * 根据具体需求定义排序规则，例如按节点深度或其他属性排序
     *
     * @param root
     * @param comparator
     */
    public void customSortTree(TreeNode<Integer> root, Comparator<Integer> comparator) {
        if (root == null) return;

        List<Integer> values = new ArrayList<>();
        collectValues(root, values);

        values.sort(comparator);
        rebuildTree(root, values);
    }

    private TreeNode<Integer> buildSortedTree(List<Integer> sortedValues) {
        TreeNode<Integer> root = new TreeNode<>(sortedValues.get(0));
        TreeNode<Integer> current = root;

        for (int i = 1; i < sortedValues.size(); i++) {
            TreeNode<Integer> newNode = new TreeNode<>(sortedValues.get(i));
            current.addChild(newNode);
            current = newNode;
        }

        return root;
    }

    // 辅助方法：对双向树进行排序

    /**
     * 双向树排序优化
     * 对双向树的排序可以结合快速排序和双向链接的维护
     *
     * @param root
     */
    public void sortBiDirectionalTree(BiDirectionalTreeNode<Integer> root) {
        if (root == null) return;

        List<Integer> values = new ArrayList<>();
        collectValues(root, values);
        Collections.sort(values);

        BiDirectionalTreeNode<Integer> newRoot = buildSortedBiDirectionalTree(values);

        root.value = newRoot.value;
        root.children.clear();
        root.children.addAll(newRoot.children);

        updateBiDirectionalLinks(root, newRoot);
    }

    private void collectValues(BiDirectionalTreeNode<Integer> node, List<Integer> values) {
        values.add(node.value);
        for (TreeNode<Integer> child : node.children) {
            collectValues(child, values);
        }
    }

    private BiDirectionalTreeNode<Integer> buildSortedBiDirectionalTree(List<Integer> sortedValues) {
        BiDirectionalTreeNode<Integer> root = new BiDirectionalTreeNode<>(sortedValues.get(0));
        BiDirectionalTreeNode<Integer> current = root;

        for (int i = 1; i < sortedValues.size(); i++) {
            BiDirectionalTreeNode<Integer> newNode = new BiDirectionalTreeNode<>(sortedValues.get(i));
            current.addChild(newNode);
            current = newNode;
        }

        BiDirectionalTreeNode<Integer> prev = null;
        for (BiDirectionalTreeNode<Integer> node : root.getAllNodes()) {
            if (prev != null) {
                node.prev = prev;
                prev.next = node;
            }
            prev = node;
        }

        return root;
    }

    private void updateBiDirectionalLinks(BiDirectionalTreeNode<Integer> oldTree, BiDirectionalTreeNode<Integer> newTree) {
        // 实际应用中需要更复杂的逻辑来维护双向链接
    }

    // 辅助方法：获取树的所有节点（广度优先）
    public List<TreeNode<Integer>> getAllTreeNodes(TreeNode<Integer> root) {
        List<TreeNode<Integer>> nodes = new ArrayList<>();
        if (root == null) return nodes;

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
        if (root == null) return nodes;

        Queue<BiDirectionalTreeNode<Integer>> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            BiDirectionalTreeNode<Integer> node = queue.poll();
            nodes.add(node);
            for (TreeNode<Integer> child : node.children) {
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
            if (current.prev != null && current.prev.next != current) {
                return false;
            }
            current = current.next;
        }
        return true;
    }

    // **新增方法：控制台打印树结构**
    public void printTree(TreeNode<Integer> root) {
        if (root == null) {
            System.out.println("Empty tree");
            return;
        }

        printTreeHelper(root, "");
    }

    private void printTreeHelper(TreeNode<Integer> node, String prefix) {
        System.out.println(prefix + node.value);

        for (int i = 0; i < node.children.size(); i++) {
            TreeNode<Integer> child = node.children.get(i);
            String childPrefix = prefix + (i < node.children.size() - 1 ? "├── " : "└── ");
            printTreeHelper(child, childPrefix);
        }
    }

    // 单向树方式排序测试
    @Test
    public void testSingleDirectionTreeSort() {
        TreeNode<Integer> root = buildTree(3);
        System.out.println("Original Single Direction Tree:");
        printTree(root);

        quickSortTree(root);

        System.out.println("\nSorted Single Direction Tree:");
        printTree(root);

        assert isTreeSorted(root) : "Single direction tree sort failed";
    }

    // 双向树排序测试
    @Test
    public void testBiDirectionalTreeSort() {
        BiDirectionalTreeNode<Integer> root = buildBiDirectionalTree(3);
        TreeNode<Integer> treeRoot = new TreeNode<>(root.value);
        for (TreeNode<Integer> child : root.children) {
            treeRoot.addChild(new TreeNode<>(child.value));
        }

        System.out.println("Original Bi-Directional Tree:");
        printTree(treeRoot);

        sortBiDirectionalTree(root);

        TreeNode<Integer> sortedTreeRoot = new TreeNode<>(root.value);
        for (TreeNode<Integer> child : root.children) {
            sortedTreeRoot.addChild(new TreeNode<>(child.value));
        }

        System.out.println("\nSorted Bi-Directional Tree:");
        printTree(sortedTreeRoot);

        assert isBiDirectionalTreeSorted(root) : "Bi-directional tree sort failed";
        assert isBiDirectionalLinksCorrect(root) : "Bi-directional links are incorrect";
    }

    // 树节点插入快速排序测试
    @Test
    public void testTreeNodeInsertQuickSort() {
        TreeNode<Integer> root = buildTree(3);
        System.out.println("Original Tree for Quick Sort:");
        printTree(root);

        quickSortTree(root);

        System.out.println("\nTree after Quick Sort:");
        printTree(root);

        assert isTreeSorted(root) : "Tree node insertion quick sort failed";
    }

    // 栈结构转换为树结构测试
    @Test
    public void testStackToTreeConversion() {
        Stack<Integer> stack = new Stack<>();
        Collections.addAll(stack, 5, 3, 8, 1, 4, 6, 2);

        TreeNode<Integer> root = stackToTree(stack);

        System.out.println("Tree converted from Stack:");
        printTree(root);

        quickSortTree(root);

        System.out.println("\nSorted Tree after Conversion from Stack:");
        printTree(root);

        assert root != null : "Converted tree from stack is empty";
        assert root.value == 5 : "Root node value is incorrect";
        assert root.children.size() >= 6 : "Number of child nodes is incorrect";
        assert isTreeSorted(root) : "Sorted tree from stack conversion failed";
    }

    // 链表结构转换为树结构测试
    @Test
    public void testLinkedListToTreeConversion() {
        ListNode<Integer> head = buildLinkedList(5, 3, 8, 1, 4, 6, 2);
        TreeNode<Integer> root = linkedListToTree(head);

        System.out.println("Tree converted from Linked List:");
        printTree(root);

        quickSortTree(root);

        System.out.println("\nSorted Tree after Conversion from Linked List:");
        printTree(root);

        assert root != null : "Converted tree from linked list is empty";
        assert root.value == 5 : "Root node value is incorrect";
        assert root.children.size() >= 6 : "Number of child nodes is incorrect";
        assert isTreeSorted(root) : "Sorted tree from linked list conversion failed";
    }

    // 列表集合结构转换为树结构测试
    @Test
    public void testListToTreeConversion() {
        List<Integer> list = Arrays.asList(5, 3, 8, 1, 4, 6, 2);
        TreeNode<Integer> root = listToTree(list);

        System.out.println("Tree converted from List:");
        printTree(root);

        quickSortTree(root);

        System.out.println("\nSorted Tree after Conversion from List:");
        printTree(root);

        assert root != null : "Converted tree from list is empty";
        assert root.value == 5 : "Root node value is incorrect";
        assert root.children.size() >= 6 : "Number of child nodes is incorrect";
        assert isTreeSorted(root) : "Sorted tree from list conversion failed";
    }

    @Test
    public void testBfsSortTree() {
        TreeNode<Integer> root = buildTree(3);
        System.out.println("Original Tree:");
        printTree(root);

        bfsSortTree(root);

        System.out.println("\nTree after BFS Sort:");
        printTree(root);

        assert isTreeSorted(root) : "BFS sort failed";
    }

    @Test
    public void testDfsSortTree() {
        TreeNode<Integer> root = buildTree(3);
        System.out.println("Original Tree:");
        printTree(root);

        dfsSortTree(root);

        System.out.println("\nTree after DFS Sort:");
        printTree(root);

        assert isTreeSorted(root) : "DFS sort failed";
    }

    @Test
    public void testHeapSortTree() {
        TreeNode<Integer> root = buildTree(3);
        System.out.println("Original Tree:");
        printTree(root);

        heapSortTree(root);

        System.out.println("\nTree after Heap Sort:");
        printTree(root);

        assert isTreeSorted(root) : "Heap sort failed";
    }

    @Test
    public void testParallelHeapSort() {
        List<Integer> list = new ArrayList<>();
        list.add(4);
        list.add(1);
        list.add(6);
        list.add(5);
        list.add(2);
        list.add(3);
        parallelHeapSort(list);
    }
}