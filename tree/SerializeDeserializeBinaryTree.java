package tree;

/**
 * This class handles the serialization and deserialization of a binary
 * tree using preorder traversal (root-left-right)
 */
public class SerializeDeserializeBinaryTree {

    /**
     * Tree Node to build a tree.
     */
    private static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(final int val) {
            this.val = val;
            this.left = null;
            this.right = null;
        }
    }

    /**
     * Custom class wrapper to manipulate the index.
     * this is better way of doing int[] index = new int[1]; and then incrememnting
     * like index[0]++;
     */
    private static class Index {
        int value;

        Index(final int value) {
            this.value = value;
        }
    }

    SerializeDeserializeBinaryTree() {

    }

    /**
     * Encodes a tree to a single string.
     * 
     * @param root
     * @return
     */
    public String serialize(TreeNode root) {

        StringBuilder sb = new StringBuilder();
        serializeByPreOrderTraversal(root, sb);

        String result = sb.toString();

        // Remove the last trailing comma.
        return result.substring(0, result.length() - 1);
    }

    /**
     * Decodes your encoded data to tree.
     * 
     * @param data
     * @return
     */
    public TreeNode deserialize(String data) {

        if (data == null || data.isEmpty()) {
            return null;
        }

        String[] nodes = data.split(",");

        // Using index class to preserve the order of indices with initializing as zero.
        Index index = new Index(0);
        TreeNode root = deserializeByPreOrderTraversal(nodes, index);

        return root;
    }

    /**
     * Helper function to traverse tree in preorder fashion and serialize the tree
     * to string.
     * 
     * @param root
     * @param sb
     */
    private void serializeByPreOrderTraversal(TreeNode root, StringBuilder sb) {

        if (root == null) {
            sb.append("null,");
            return;
        }

        sb.append(root.val).append(",");
        serializeByPreOrderTraversal(root.left, sb);
        serializeByPreOrderTraversal(root.right, sb);
    }

    /**
     * helper function to desealize the tree
     * 
     * @param nodes
     * @param counter
     * @return
     */
    private TreeNode deserializeByPreOrderTraversal(String[] nodes, Index index) {

        if (index.value >= nodes.length || nodes[index.value].equals("null")) {
            index.value++; // You must move forward in the array even if you’re hitting a "null".
            return null;
        }

        TreeNode root = new TreeNode(Integer.parseInt(nodes[index.value++]));
        root.left = deserializeByPreOrderTraversal(nodes, index);
        root.right = deserializeByPreOrderTraversal(nodes, index);

        return root;
    }

    public static void main(String[] args) {
        SerializeDeserializeBinaryTree codec = new SerializeDeserializeBinaryTree();

        // Helper to print serialized data
        java.util.function.Function<TreeNode, String> printSerialized = (root) -> {
            String serialized = codec.serialize(root);
            System.out.println("Serialized: " + serialized);
            return serialized;
        };

        // === Test Case 1: Empty Tree ===
        System.out.println("=== Test Case 1: Empty Tree ===");
        TreeNode root1 = null;
        String data1 = printSerialized.apply(root1);
        TreeNode deserialized1 = codec.deserialize(data1);
        System.out.println("Deserialized (should be null): " + deserialized1);

        // === Test Case 2: Single Node Tree ===
        System.out.println("\n=== Test Case 2: Single Node Tree ===");
        TreeNode root2 = new TreeNode(1);
        String data2 = printSerialized.apply(root2);
        TreeNode deserialized2 = codec.deserialize(data2);
        System.out.println("Deserialized Root Value: " + deserialized2.val);

        // === Test Case 3: Left Skewed Tree ===
        System.out.println("\n=== Test Case 3: Left Skewed Tree ===");
        TreeNode root3 = new TreeNode(1);
        root3.left = new TreeNode(2);
        root3.left.left = new TreeNode(3);
        String data3 = printSerialized.apply(root3);
        TreeNode deserialized3 = codec.deserialize(data3);
        System.out.println("Deserialized Root Value: " + deserialized3.val);
        System.out.println("Left Child: " + deserialized3.left.val);
        System.out.println("Left->Left Child: " + deserialized3.left.left.val);

        // === Test Case 4: Right Skewed Tree ===
        System.out.println("\n=== Test Case 4: Right Skewed Tree ===");
        TreeNode root4 = new TreeNode(1);
        root4.right = new TreeNode(2);
        root4.right.right = new TreeNode(3);
        String data4 = printSerialized.apply(root4);
        TreeNode deserialized4 = codec.deserialize(data4);
        System.out.println("Deserialized Root Value: " + deserialized4.val);
        System.out.println("Right Child: " + deserialized4.right.val);
        System.out.println("Right->Right Child: " + deserialized4.right.right.val);

        // === Test Case 5: Complete Binary Tree ===
        System.out.println("\n=== Test Case 5: Complete Binary Tree ===");
        TreeNode root5 = new TreeNode(1);
        root5.left = new TreeNode(2);
        root5.right = new TreeNode(3);
        root5.left.left = new TreeNode(4);
        root5.left.right = new TreeNode(5);
        root5.right.left = new TreeNode(6);
        root5.right.right = new TreeNode(7);
        String data5 = printSerialized.apply(root5);
        TreeNode deserialized5 = codec.deserialize(data5);
        System.out.println("Deserialized Root Value: " + deserialized5.val);
        System.out.println("Left Child: " + deserialized5.left.val + ", Right Child: " + deserialized5.right.val);

        // === Test Case 6: Random Tree ===
        System.out.println("\n=== Test Case 6: Random Tree ===");
        TreeNode root6 = new TreeNode(10);
        root6.left = new TreeNode(5);
        root6.right = new TreeNode(20);
        root6.left.right = new TreeNode(8);
        root6.right.left = new TreeNode(15);
        root6.right.right = new TreeNode(30);
        String data6 = printSerialized.apply(root6);
        TreeNode deserialized6 = codec.deserialize(data6);
        System.out.println("Root: " + deserialized6.val);
        System.out.println("Left->Right: " + deserialized6.left.right.val);
        System.out.println("Right->Left: " + deserialized6.right.left.val);
    }

}
