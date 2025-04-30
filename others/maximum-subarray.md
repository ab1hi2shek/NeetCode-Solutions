## Problem link
https://neetcode.io/problems/maximum-subarray

```
Given an array of integers nums, find the subarray with the largest sum and return the sum.

A subarray is a contiguous non-empty sequence of elements within an array.
```

```
Input: nums = [2,-3,4,-2,2,1,-1,4]

Output: 8

Input: nums = [-1]

Output: -1


```

## Solution

```java
class Solution {
    public int maxSubArray(int[] nums) {
        
        // Brute force solution. Time: O(N^3)
        //return bruteForce(nums);

        // Brute force optimized solution. Time: O(N^2)
        //return bruteForceOptimized(nums);

        // Greedy algo - came up by myself. Time: O(N)
        //return greedy(nums);

        // Standard Kadane algo. Time: O(N)
        return kadane(nums);
    }

    /**
    Time: O(N^3)
    **/
    private int bruteForce(int[] nums) {
        int n = nums.length;

        int ans = Integer.MIN_VALUE;
        for(int i=0; i<n; i++) {
            for(int j=i; j<n; j++) {
                int sum = 0;
                for(int k=i; k<=j; k++) {
                    sum += nums[k];
                }
                ans = Math.max(ans, sum);
            }
        }
        return ans;
    }

    /**
    Time: O(N^2)
    **/
    private int bruteForceOptimized(int[] nums) {
        int n = nums.length;

        int ans = Integer.MIN_VALUE;
        for(int i=0; i<n; i++) {
            int sum = 0;
            for(int j=i; j<n; j++) {
                sum += nums[j];
                ans = Math.max(ans, sum);
            }
        }
        return ans;
    }

    /**
    I came up with this solution myslef and later realized that it is very
    similar to Kadane algorithm.
    Time: O(n)
    **/
    private int greedy(int[] nums) {
        int n = nums.length;

        int ans = Integer.MIN_VALUE;
        int sum = 0;
        for(int i=0; i<n; i++) {
            if(sum + nums[i] > 0) {
                sum += nums[i];
                ans = Math.max(ans, sum);
            } else {
                sum = 0;
                ans = Math.max(ans, nums[i]);
            }
        }

        return ans;
    }

    /**
    Standard Kadane algorithm.
    Time: O(n)
    **/
    private int kadane(int[] nums) {
        int n = nums.length;

        int ans = Integer.MIN_VALUE;
        int sum = 0;
        for(int i=0; i<n; i++) {
            if(sum < 0) {
                sum = 0;
            }
            sum += nums[i];
            ans = Math.max(ans, sum);
        }

        return ans;
    }
}

```