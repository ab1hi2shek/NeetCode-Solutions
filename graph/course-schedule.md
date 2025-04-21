## Problem link
https://neetcode.io/problems/course-schedule

## Solution

### Approach 1: BFS Approach (Kahn's algorithm)
Kahn’s Algorithm is a BFS-based topological sort that uses in-degrees to process nodes with no prerequisites first. It detects cycles and returns a valid linear ordering if possible.
 - Time Complexity: O(V + E)
 - Space Complexity: O(V + E) for the adjacency list, in-degree array, and queue.

```java
class Solution {
    /**
    Using Kahn's algorithm or BFS based approach.
    Using indegree of nodes. 
    */
    private boolean canFinish(int numCourses, int[][] prerequisites) {
        List<List<Integer>> adj = new ArrayList<>();
        int[] indegree = new int[numCourses];

        for(int i=0; i<numCourses; i++) {
            adj.add(new ArrayList<>());
            indegree[i] = 0;
        }

        for(int[] elements : prerequisites) {
            int u = elements[0];
            int v = elements[1];
            adj.get(v).add(u);
            indegree[u]++;
        }

        Queue<Integer> q = new LinkedList<>();
        for(int i=0; i<numCourses; i++) {
            if(indegree[i] == 0) q.offer(i);
        }

        while(!q.isEmpty()) {
            int node = q.poll();
            indegree[node]--;

            for(int neigh : adj.get(node)) {
                indegree[neigh]--;
                if(indegree[neigh] == 0) q.offer(neigh);
            }
        }

        for(int i=0; i<numCourses; i++) {
            if(indegree[i] > 0) {
                return false;
            }
        }
        return true;
    }
}
```

### Approach 2: DFS Approach
DFS-based approach uses recursion and a color/state array to detect cycles and build a topological order via post-order traversal. It marks nodes as visiting (in recursion stack) and visited to track cycles.
 - Time Complexity: O(V + E)
 - Space Complexity: O(V + E) for the adjacency list and recursion stack.

```java
class Solution {

    /**
    DFS based approach.
    */
    private boolean canFinish(int numCourses, int[][] prerequisites) {
        List<List<Integer>> adj = new ArrayList<>();
        // 0 = unvisited, 1 = visiting, 2 = completed visiting
        int[] visited = new int[numCourses];
        for(int i=0; i<numCourses; i++) {
            adj.add(new ArrayList<>());
            visited[i] = 0;
        }
        for(int[] elements : prerequisites) {
            int u = elements[0];
            int v = elements[1];
            adj.get(v).add(u);
        }

        for(int i=0; i<numCourses; i++) {
            if(!dfs(adj, i, visited)) return false;
        }
        return true;
    }

    private boolean dfs(List<List<Integer>> adj, int node, int[] visited) {
        if(visited[node] == 1) { // cycle detected
            return false;
        }

        if(visited[node] == 2) { //fully processed already.
            return true;
        }

        visited[node] = 1;
        for(int neigh : adj.get(node)) {
            if(!dfs(adj, neigh, visited)) {
                return false;
            }
        }
        visited[node] = 2; // mark as fully processed
        return true;
    }
}
```