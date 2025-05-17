```java
/**
 * Your BSTIterator object will be instantiated and called as such:
 * BSTIterator obj = new BSTIterator(root);
 * int param_1 = obj.next();
 * boolean param_2 = obj.hasNext();
 */

/**
 * Definition for a binary tree node.
 * public class TreeNode {
 *     int val;
 *     TreeNode left;
 *     TreeNode right;
 *     TreeNode() {}
 *     TreeNode(int val) { this.val = val; }
 *     TreeNode(int val, TreeNode left, TreeNode right) {
 *         this.val = val;
 *         this.left = left;
 *         this.right = right;
 *     }
 * }
 */

/**
 * Space complexity = O(N), N = number of nodes
 */ 
class BSTIterator {

    List<Integer> inorder;
    int currentIndex = 0;

    public BSTIterator(TreeNode root) {
        this.inorder = new ArrayList<>();
        inorderTraversal(root);
    }
    
    public int next() {
        return inorder.get(currentIndex++);
    }
    
    public boolean hasNext() {
        return currentIndex < inorder.size();
    }

    private void inorderTraversal(TreeNode root) {
        if(root == null) return;
        inorderTraversal(root.left);
        inorder.add(root.val);
        inorderTraversal(root.right);
    }
}
```

#### Better solution - O(h) -> height of tree space.

```java
class BSTIterator {

    private Deque<TreeNode> stack;

    public BSTIterator(TreeNode root) {
        stack = new ArrayDeque<>();
        pushLeftNodes(root);
    }
    
    public int next() {
        TreeNode node = stack.pop();
        if(node.right != null) {
            pushLeftNodes(node.right);
        }
        return node.val;
    }
    
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    private void pushLeftNodes(TreeNode root) {
        while(root != null) {
            stack.push(root);
            root = root.left;
        }
    }
}
```