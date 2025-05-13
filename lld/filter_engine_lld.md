# Explain pagination and why we need it?

Pagination is the process of dividing a large dataset or content into smaller, more manageable chunks called "pages," which can be retrieved or displayed incrementally rather than all at once[^1][^4][^5]. This technique is widely used in APIs, databases, and user interfaces to efficiently handle and present large volumes of data.

## Why We Need Pagination

**1. Improved Performance**

- Retrieving and processing data in small pages significantly reduces response times for API calls and database queries. It minimizes server workload and ensures that clients receive data quickly, even when the underlying dataset is very large[^1][^4][^5][^10].

**2. Reduced Resource Usage**

- Pagination prevents overwhelming the server, network, and client devices by avoiding the transfer of massive datasets in a single request. This leads to lower memory consumption, reduced processing power requirements, and more efficient use of bandwidth[^1][^4][^5].

**3. Enhanced User Experience**

- By presenting data in smaller chunks, users can navigate data incrementally, making it easier to find and view relevant information. This results in faster page loads, smoother scrolling, and more intuitive navigation through lists or feeds[^1][^4][^5].

**4. Scalability and Flexibility**

- Pagination enables systems to scale efficiently as data grows. APIs and applications can handle millions of records without performance degradation, supporting use cases like infinite scroll, large reports, or real-time feeds[^1][^4][^5][^9][^11].

**5. Efficient Data Transfer**

- Only the needed data is sent over the network, which reduces latency and improves the responsiveness of applications and APIs[^1][^4][^5].

**6. Easier Error Handling**

- If a data retrieval error occurs, only the affected page needs to be reloaded, rather than the entire dataset. This makes error recovery faster and more reliable[^1][^4][^5].


## Example

Suppose an e-commerce site has 100,000 products. Instead of loading all products at once, the site uses pagination to load 20 products per page:

- **Page 1:** `GET /products?page=1&limit=20` (returns products 1–20)
- **Page 2:** `GET /products?page=2&limit=20` (returns products 21–40)

This approach ensures the site loads quickly and efficiently, regardless of how many products exist.

---

**In summary:**
Pagination is essential for managing large datasets efficiently. It improves performance, reduces resource usage, enhances user experience, and supports scalability by delivering data in manageable pages rather than overwhelming systems and users with massive data loads[^1][^4][^5][^9][^10][^11].

<div style="text-align: center">⁂</div>

[^1]: https://blog.treblle.com/api-pagination-guide-techniques-benefits-implementation/

[^2]: https://appwrite.io/blog/post/best-pagination-technique

[^3]: https://www.techtarget.com/whatis/definition/pagination

[^4]: https://dev.to/pragativerma18/unlocking-the-power-of-api-pagination-best-practices-and-strategies-4b49

[^5]: https://www.getknit.dev/blog/api-pagination-best-practices

[^6]: https://stackoverflow.com/questions/55744926/offset-pagination-vs-cursor-pagination

[^7]: https://planetscale.com/blog/mysql-pagination

[^8]: https://developer.zendesk.com/documentation/api-basics/pagination/comparing-cursor-pagination-and-offset-pagination/

[^9]: https://www.milanjovanovic.tech/blog/understanding-cursor-pagination-and-why-its-so-fast-deep-dive

[^10]: https://dev.to/jacktt/comparing-limit-offset-and-cursor-pagination-1n81

[^11]: https://www.contentful.com/blog/graphql-pagination-cursor-offset-tutorials/

------------------
------------------

# Cursor based pagination vs Offset based pagination

## Offset-Based Pagination

**Definition:**
Offset-based pagination is a method where you specify how many records to skip (the offset) and how many records to return (the limit) in each request. This approach is straightforward and commonly used in APIs and databases[^1][^2][^5][^9].

**How It Works:**

- The client requests a specific "page" of results by indicating the number of items to skip (offset) and how many items to fetch (limit).
- For example, to get the first page of 10 items, you use `offset=0&limit=10`.
- To get the second page, you use `offset=10&limit=10`, which skips the first 10 items and returns the next 10[^1][^2][^5].

**Example:**
Suppose you want to fetch product reviews, 10 per page, and you want page 3:

```
GET /v1/products/12345/reviews?offset=20&limit=10
```

This request skips the first 20 reviews and returns reviews 21–30[^1].

**Advantages:**

- Simple to implement and understand.
- Works well for static or rarely changing datasets.

**Limitations:**

- Performance can degrade with large datasets, as the database must count and skip many records.
- If records are inserted or deleted between requests, users may see duplicates or miss items (results can "shift")[^7][^9].

---

## Cursor-Based Pagination

**Definition:**
Cursor-based pagination (also known as keyset pagination) uses a unique field (like an `id` or timestamp) as a "cursor" to mark the position in the dataset. Instead of skipping a number of records, you fetch records *after* a specific cursor value[^4][^6][^8].

**How It Works:**

- The client requests the first page with a limit (e.g., `limit=10`).
- The server returns the results along with a cursor (e.g., the last item's `id`).
- For the next page, the client requests items *after* this cursor:

```
GET /v1/products/12345/reviews?after=last_seen_id&limit=10
```

- The process repeats, always using the last seen cursor value[^4][^6][^8].

**Example:**
Suppose you have a list of books with incremental IDs. To fetch the first 10 books:

```
SELECT * FROM books WHERE id > 0 ORDER BY id ASC LIMIT 10;
```

Assume the last book in the result has `id = 1009`. To fetch the next 10:

```
SELECT * FROM books WHERE id > 1009 ORDER BY id ASC LIMIT 10;
```

This ensures you always get the next set of results, even if new records are added or deleted in between[^8].

**Advantages:**

- More efficient for large or frequently changing datasets.
- Avoids issues with missing or duplicated records due to inserts/deletes.
- Better performance, as the database can use indexes instead of counting/skipping rows[^4][^6][^8].

**Limitations:**

- More complex to implement.
- Not as easy to jump to arbitrary pages (e.g., "go to page 5").
- Requires a stable, unique, and sequential field (like an auto-incrementing `id` or timestamp).

---

## Comparison Table

| Feature | Offset-Based Pagination | Cursor-Based Pagination |
| :-- | :-- | :-- |
| Request Parameters | `offset`, `limit` | `cursor` (e.g., `id`), `limit` |
| Use Case | Small/medium, static datasets | Large, dynamic datasets |
| Performance | Slower for large offsets | Fast, even with large datasets |
| Consistency (with updates) | Prone to duplicates/skips | Consistent, avoids duplicates |
| Arbitrary Page Access | Easy (jump to any page) | Hard (must traverse sequentially) |
| Implementation Complexity | Simple | More complex |


---

## Summary

- **Offset-based pagination** is simple and flexible, ideal for small or static datasets, but can be inefficient and inconsistent with large or frequently changing data[^1][^2][^5][^9].
- **Cursor-based pagination** is more robust and performant for large or dynamic datasets, preventing duplicates and skips, but is harder to implement and less flexible for random page access[^4][^6][^8].

<div style="text-align: center">⁂</div>

[^1]: https://www.merge.dev/blog/offset-pagination

[^2]: https://www.contentful.com/blog/graphql-pagination-cursor-offset-tutorials/

[^3]: https://stackoverflow.com/questions/311068/offset-vs-page-number-when-doing-pagination

[^4]: https://www.milanjovanovic.tech/blog/understanding-cursor-pagination-and-why-its-so-fast-deep-dive

[^5]: https://developer.box.com/guides/api-calls/pagination/offset-based/

[^6]: https://www.merge.dev/blog/cursor-pagination

[^7]: https://www.apollographql.com/docs/react/pagination/offset-based

[^8]: https://www.pingcap.com/article/limit-offset-pagination-vs-cursor-pagination-in-mysql/

[^9]: https://www.merge.dev/blog/rest-api-pagination

[^10]: https://slack.engineering/evolving-api-pagination-at-slack/

[^11]: https://strawberry.rocks/docs/guides/pagination/offset-based

[^12]: https://planetscale.com/blog/mysql-pagination

[^13]: https://stackoverflow.com/questions/55744926/offset-pagination-vs-cursor-pagination

[^14]: https://www.youtube.com/watch?v=WUICbOOtAic

[^15]: https://learn.microsoft.com/en-us/ef/core/querying/pagination

[^16]: https://brandur.org/fragments/offset-pagination

[^17]: https://www.prisma.io/docs/orm/prisma-client/queries/pagination

[^18]: https://dev.to/joaofbantunes/pagination-in-an-api-page-number-vs-start-index-115g

[^19]: https://www.reddit.com/r/programming/comments/knlp8a/stop_using_offset_for_pagination_why_its_grossly/

[^20]: https://docs.gitlab.com/development/database/offset_pagination_optimization/

[^21]: https://developer.zendesk.com/documentation/api-basics/pagination/paginating-through-lists-using-cursor-pagination/

[^22]: https://www.elastic.co/docs/reference/elasticsearch/rest-apis/paginate-search-results

[^23]: https://www.youtube.com/watch?v=gfRJBoOuNUA

[^24]: https://stackoverflow.com/questions/18314687/how-to-implement-cursors-for-pagination-in-an-api

[^25]: https://specs.openstack.org/openstack/api-wg/guidelines/pagination_filter_sort.html

[^26]: https://support.safe.com/hc/en-us/articles/25407519816717-Cursor-Based-API-Pagination

[^27]: https://www.contentful.com/blog/graphql-pagination-cursor-offset-tutorials/

[^28]: https://stackoverflow.com/questions/34935809/jquery-if-current-number-on-pagination-is-first-or-last-disable-next-or-previo

[^29]: https://www.getknit.dev/blog/api-pagination-techniques

[^30]: https://www.citusdata.com/blog/2016/03/30/five-ways-to-paginate/

[^31]: https://opensearch.org/docs/latest/search-plugins/searching-data/paginate/

[^32]: https://solr.apache.org/guide/solr/latest/query-guide/pagination-of-results.html

[^33]: https://www.linkedin.com/pulse/which-pagination-approach-should-we-use-offset-based-arghya-majumder

[^34]: https://www.youtube.com/watch?v=zwDIN04lIpc

[^35]: https://planetscale.com/learn/courses/mysql-for-developers/examples/cursor-pagination

[^36]: https://www.apollographql.com/docs/react/pagination/cursor-based

[^37]: https://jsonapi.org/profiles/ethanresnick/cursor-pagination/

[^38]: https://dba.stackexchange.com/questions/328447/cursor-style-pagination-with-timestamp-id-with-a-filter-on-timestamp

------------------
------------------

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

------------------
------------------

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

