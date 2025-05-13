# Basic code for transaction filtering and pagination

```java
package others.coinbase;

import java.util.ArrayList;
import java.util.List;

public class TransactionFilterEngine {

    // Transaction class with required fields
    static class Transaction {
        final int time, id, userId, currency, amount;

        Transaction(int time, int id, int userId, int currency, int amount) {
            this.time = time;
            this.id = id;
            this.userId = userId;
            this.currency = currency;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return String.format("Transaction{id=%d, time=%d, userId=%d, currency=%d, amount=%d}", id, time, userId,
                    currency, amount);
        }
    }

    // Filter Abstraction
    interface Filter {
        // Abstract Function to check if transaction matches given the filter.
        boolean matches(Transaction t);
    }

    static class FieldFilter implements Filter {

        enum Operator {
            EQUAL_TO,
            GREATER_THAN,
            LESS_THAN
        }

        private final String field;
        private final Operator operator;
        private final int value;

        FieldFilter(String field, Operator operator, int value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }

        @Override
        public boolean matches(Transaction transaction) {

            int transactionFieldValue = switch (field) {
                case "id" -> transaction.id;
                case "time" -> transaction.time;
                case "userId" -> transaction.userId;
                case "currency" -> transaction.currency;
                case "amount" -> transaction.amount;
                default -> throw new IllegalArgumentException("Unknown field found: " + field);
            };

            return switch (operator) {
                case EQUAL_TO -> transactionFieldValue == value;
                case GREATER_THAN -> transactionFieldValue > value;
                case LESS_THAN -> transactionFieldValue < value;
                default -> throw new IllegalArgumentException("Unknown operator found " + operator.name());
            };
        }
    }

    static class CompositeFilter implements Filter {

        enum Logic {
            AND,
            OR
        }

        private final List<Filter> filters;
        private final Logic logic;

        CompositeFilter(List<Filter> filters, Logic logic) {
            this.filters = filters;
            this.logic = logic;
        }

        @Override
        public boolean matches(Transaction transaction) {
            return switch (logic) {
                case AND -> filters.stream().allMatch(filter -> filter.matches(transaction));
                case OR -> filters.stream().anyMatch(filter -> filter.matches(transaction));
                default -> throw new IllegalArgumentException("Unknown logic operator found " + logic.name());
            };
        }
    }

    /**
     * Function to return the eligible transactions based on filter.
     * 
     * @param transactions
     * @param filter
     * @return
     */
    public static List<Transaction> filterTransactions(List<Transaction> transactions, Filter filter) {
        List<Transaction> result = new ArrayList<>();

        for (Transaction transaction : transactions) {
            if (filter.matches(transaction)) {
                result.add(transaction);
            }
        }
        return result;
    }

    /**
     * Function to return the paginated results using cursor based pagination.
     * 
     * @param transactions
     * @param offset
     * @param limit
     * @return
     */
    public static List<Transaction> offsetPaginate(List<Transaction> transactions, int offset, int limit) {
        if (offset < 0 || limit < 0) {
            throw new IllegalArgumentException("Offset and limit must be non-negative");
        }

        int start = Math.min(offset, transactions.size());
        int end = Math.min(offset + limit, transactions.size());

        return new ArrayList<>(transactions.subList(start, end));
    }

    /**
     * Function to return paginated results using cursor based pagination. It
     * assumes that the transactions are
     * sorted by Id.
     * 
     * @param transactions
     * @param lasSeenId
     * @param limit
     * @return
     */
    public List<Transaction> cursorPaginate(List<Transaction> transactions, int lastSeenId, int limit) {

        if (limit < 0) {
            throw new IllegalArgumentException("Invalid limit");
        }

        List<Transaction> result = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.id > lastSeenId) {
                result.add(t);
                if (result.size() == limit)
                    break;
            }
        }
        return result;

        /*
         * 
         * Optimization Approach
         * If your transactions list is already sorted by id in ascending order, you can
         * use binary search to quickly
         * find the starting point (the first transaction with id > lastSeenId), and
         * then simply take the next limit
         * transactions.
         * 
         * Why This Works
         * 1. Binary search reduces the initial search from O(n) to O(log n).
         * 2. After finding the start index, you can use subList or a simple loop to
         * collect the next limit items,
         * which is efficient.
         */
    }

    // Main function with test cases
    public static void main(String[] args) {
        List<Transaction> transactions = List.of(
                new Transaction(1, 11, 1, 1, 10),
                new Transaction(2, 12, 1, 3, 11),
                new Transaction(3, 13, 2, 1, -10),
                new Transaction(4, 14, 1, 2, 12),
                new Transaction(5, 5, 1, 1, 10),
                new Transaction(6, 6, 1, 1, 13),
                new Transaction(7, 7, 1, 2, -5),
                new Transaction(8, 8, 1, 2, 10),
                new Transaction(9, 9, 1, 1, 10),
                new Transaction(10, 10, 1, 1, 15),
                new Transaction(10, 21, 1, 1, 16),
                new Transaction(11, 22, 1, 2, -3),
                new Transaction(11, 23, 1, 1, 5),
                new Transaction(12, 24, 1, 2, 6),
                new Transaction(13, 25, 1, 1, 10));

        System.out.println("Filter 1: userId == 1");
        Filter userFilter = new FieldFilter("userId", FieldFilter.Operator.EQUAL_TO, 1);
        filterTransactions(transactions, userFilter).forEach(System.out::println);

        System.out.println("\nFilter 2: userId > 1 AND userId < 3 AND time < 10");
        Filter userGt = new FieldFilter("userId", FieldFilter.Operator.GREATER_THAN, 1);
        Filter userLt = new FieldFilter("userId", FieldFilter.Operator.LESS_THAN, 3);
        Filter timeGt = new FieldFilter("time", FieldFilter.Operator.LESS_THAN, 10);
        Filter composite = new CompositeFilter(List.of(userGt, userLt, timeGt), CompositeFilter.Logic.AND);
        filterTransactions(transactions, composite).forEach(System.out::println);
    }
}
```

## A. Real-Time Data Consistency and Edge Cases

### Problem

Offset-based pagination becomes unreliable when data is inserted or deleted between page requests, leading to duplicates or missing items[^5]. Cursor-based pagination is more robust in real-time scenarios, as it uses a stable marker (like `id` or `timestamp`) to fetch the next set of results.

### Solution \& Example

- **Offset-based:**
Not recommended for real-time data. If you must use it, warn about possible inconsistencies.
- **Cursor-based:**
Always use a unique, sequential field (like `id` or `timestamp`) as the cursor. Fetch items "after" the last seen cursor value.

```java
// Efficient cursor-based pagination for real-time data
public static List<Transaction> cursorPaginate(List<Transaction> transactions, int lastSeenId, int limit) {
    // Assumes transactions are sorted by id
    int left = 0, right = transactions.size() - 1, startIdx = transactions.size();
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (transactions.get(mid).id > lastSeenId) {
            startIdx = mid;
            right = mid - 1;
        } else {
            left = mid + 1;
        }
    }
    List<Transaction> result = new ArrayList<>();
    for (int i = startIdx; i < transactions.size() && result.size() < limit; i++) {
        result.add(transactions.get(i));
    }
    return result;
}
```

**Explanation:**

- By always fetching records after the last seen cursor, you avoid duplicates and missing records even if new data is inserted or deleted between requests[^5].

---

## B. Bidirectional and Composite Key Cursor Pagination

### Problem

- Cursor-based pagination is naturally forward-only. Supporting "previous" page navigation and stable ordering when the sort field is not unique (e.g., multiple transactions with the same timestamp) requires enhancements[^6][^8].


### Solution \& Example

- **Bidirectional Pagination:**
Use two parameters: `after` (for next page) and `before` (for previous page).
- **Composite Key Cursor:**
Use both `timestamp` and `id` to uniquely identify the cursor position.

```java
// Composite key cursor pagination (bidirectional)
public static List<Transaction> compositeCursorPaginate(
        List<Transaction> transactions, int lastSeenTime, int lastSeenId, int limit, boolean forward) {
    // Assumes transactions are sorted by (time, id)
    List<Transaction> result = new ArrayList<>();
    if (forward) {
        for (Transaction t : transactions) {
            if (t.time > lastSeenTime || (t.time == lastSeenTime && t.id > lastSeenId)) {
                result.add(t);
                if (result.size() == limit) break;
            }
        }
    } else { // backward
        for (int i = transactions.size() - 1; i >= 0 && result.size() < limit; i--) {
            Transaction t = transactions.get(i);
            if (t.time < lastSeenTime || (t.time == lastSeenTime && t.id < lastSeenId)) {
                result.add(0, t); // insert at front to maintain order
            }
        }
    }
    return result;
}
```

**Explanation:**

- For forward pagination, fetch items after the given `(time, id)`.
- For backward pagination, fetch items before the given `(time, id)`[^6][^8].
- This approach handles duplicate timestamps and supports both directions.

---

## C. Time-Based or Range-Based Pagination

### Problem

You may want to fetch all transactions in a specific time window (e.g., for audit logs), which is especially useful for chronological data[^2].

### Solution \& Example

- Use `start_time` and `end_time` parameters to filter the transactions.
- Combine with cursor-based pagination for efficient scrolling within the window.

```java
// Time-based (range) pagination
public static List<Transaction> timeRangePaginate(
        List<Transaction> transactions, int startTime, int endTime, int lastSeenId, int limit) {
    List<Transaction> result = new ArrayList<>();
    for (Transaction t : transactions) {
        if (t.time >= startTime && t.time <= endTime && t.id > lastSeenId) {
            result.add(t);
            if (result.size() == limit) break;
        }
    }
    return result;
}
```

**Explanation:**

- Fetches transactions within `[startTime, endTime]` and after the last seen id (for cursor-based paging within the window)[^2].

---

## Summary Table

| Scenario | Approach \& Code Example | Key Points |
| :-- | :-- | :-- |
| Real-time consistency | Use cursor-based with unique id (see A) | Avoids duplicates/missing items when data changes during pagination[^5] |
| Bidirectional \& composite | Use (time, id) as cursor, support prev/next (B) | Enables stable paging even with non-unique sort fields, supports backward |
| Time/range-based | Filter by time window, then paginate (C) | Efficient for logs/events, combines well with cursor-based[^2] |


---

## Best Practices

- **Always sort transactions by the cursor fields (e.g., id, time, or both) before paginating.**
- **For real-time data, prefer cursor-based over offset-based pagination for reliability and performance[^5].**
- **For bidirectional or composite key pagination, use both primary and secondary fields (e.g., time + id) as the cursor[^6][^8].**
- **For time-based use cases, combine time filtering with cursor-based pagination for efficient, stable results[^2].**

---

These solutions ensure robust, efficient, and user-friendly pagination even in complex, real-world scenarios.

<div style="text-align: center">⁂</div>

[^1]: https://www.getknit.dev/blog/api-pagination-best-practices

[^2]: https://dev.to/pragativerma18/unlocking-the-power-of-api-pagination-best-practices-and-strategies-4b49

[^3]: https://www.merge.dev/blog/rest-api-pagination

[^4]: https://appwrite.io/blog/post/best-pagination-technique

[^5]: https://www.sitepoint.com/paginating-real-time-data-cursor-based-pagination/

[^6]: https://apryse.com/blog/graphql/implementing-graphql-pagination

[^7]: https://apidog.com/blog/api-pagination-guide/

[^8]: https://www.apollographql.com/docs/ios/pagination/directional-pagers

