## Problem link
https://neetcode.io/problems/course-schedule-ii

## Solution

### Approach 1: BFS Approach (Kahn's algorithm)
Kahn’s Algorithm is a BFS-based topological sort that uses in-degrees to process nodes with no prerequisites first. It detects cycles and returns a valid linear ordering if possible.
 - Time Complexity: O(V + E)
 - Space Complexity: O(V + E) for the adjacency list, in-degree array, and queue.

```java
class Solution {
    List<List<Integer>> adj = new ArrayList<>();

    public int[] findOrder(int numCourses, int[][] prerequisites) {
        return topologicalKahn(numCourses, prerequisites);
    }

    private int[] topologicalKahn(int numCourses, int[][] prerequisites) {
        int[] indegree = new int[numCourses];

        for(int i=0; i<numCourses; i++) {
            adj.add(new ArrayList<>());
            indegree[i] = 0;
        }

        for(int[] elements: prerequisites) {
            int u = elements[0];
            int v = elements[1];
            adj.get(v).add(u);
            indegree[u]++;
        }

        Queue<Integer> q = new LinkedList<>();
        for(int i=0; i<numCourses; i++) {
            if(indegree[i] == 0) q.offer(i);
        }

        int[] result = new int[numCourses];
        int index = 0;
        while(!q.isEmpty()) {
            int node = q.poll();
            indegree[node]--;
            result[index++] = node;
            for(int neigh: adj.get(node)) {
                indegree[neigh]--;
                if(indegree[neigh] == 0) q.offer(neigh);
            }
        }

        if(index != numCourses) return new int[0];
        return result;
    }
}
```

### Approach 2: DFS Approach
DFS-based approach uses recursion and a color/state array to detect cycles and build a topological order via post-order traversal. It marks nodes as visiting (in recursion stack) and visited to track cycles.
 - Time Complexity: O(V + E)
 - Space Complexity: O(V + E) for the adjacency list and recursion stack.

```java
class Solution {
    List<List<Integer>> adj = new ArrayList<>();

    public int[] findOrder(int numCourses, int[][] prerequisites) {
        return topologicalDFS(numCourses, prerequisites);
    }

    private int[] topologicalDFS(int numCourses, int[][] prerequisites) {

        // 0 = unvisited, 1 = visiting, 2 = visited
        int[] visited = new int[numCourses];
        for(int i=0; i<numCourses; i++) {
            adj.add(new ArrayList<>());
            visited[i] = 0;
        }

        for(int[] elements: prerequisites) {
            int u = elements[0];
            int v = elements[1];
            adj.get(v).add(u);
        }

        Stack<Integer> stck = new Stack<>();
        for(int i=0; i<numCourses; i++) {
            if(!dfs(i, visited, stck)) {
                return new int[0]; // Not possible to print courses.
            }
        }

        // DFS pushes courses in reverse order
        int[] result = new int[numCourses];
        int indx = 0;
        while (!stck.isEmpty()) {
            result[indx++] = stck.pop();
        }
        return result;
    }

    private boolean dfs(int node, int[] visited, Stack<Integer> stck) {

        // if node is being visited (not completely processed), means cycle found.
        if(visited[node] == 1) return false; 
        // Fully visited means we can skip.
        if(visited[node] == 2) return true;

        visited[node] = 1;
        for(int neigh : adj.get(node)) {
            if(!dfs(neigh, visited, stck)) {
                return false;
            }
        }

        visited[node] = 2;
        stck.push(node);
        return true;
    }
}

```