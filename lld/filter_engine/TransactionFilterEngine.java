package lld.filter_engine;

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
