## Problem link
https://neetcode.io/problems/count-paths

```
There is an m x n grid where you are allowed to move either down or to the right at any point in time.

Given the two integers m and n, return the number of possible unique paths that can be taken from the top-left corner of the grid (grid[0][0]) to the bottom-right corner (grid[m - 1][n - 1]).

You may assume the output will fit in a 32-bit integer.
```

## Solution

```
class Solution {
    public int uniquePaths(int m, int n) {
        
        int[][] dp = new int[m][n];
        for(int i=m-1; i>=0; i--) {
            for(int j=n-1; j>=0; j--) {
                if(i == m-1 || j == n-1) {
                    dp[i][j] = 1;
                } else {
                    dp[i][j] = 0;
                    if(i + 1 < m) dp[i][j] += dp[i+1][j];
                    if(j + 1 < n) dp[i][j] += dp[i][j+1];
                }
            }
        }

        return dp[0][0];
    }
}
```