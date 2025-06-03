package practice;

import java.util.*;

// Enums
enum Operand {
    EQUAL_TO("="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_OR_EQUAL_TO(">="),
    LESS_THAN_OR_EQUAL_TO("<=");

    private final String value;

    Operand(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

enum CompositeOperand {
    AND("and"),
    OR("or");

    private final String value;

    CompositeOperand(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

enum FieldName {
    ID("id"),
    TIME("time"),
    USER_ID("userId"),
    AMOUNT("amount");

    private final String value;

    FieldName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

// Data classes
class Field {
    final FieldName name;
    final int value;

    Field(FieldName name, int value) {
        this.name = name;
        this.value = value;
    }
}

class Transaction {
    final Field id;
    final Field time;
    final Field userId;
    final Field amount;

    Transaction(Field id, Field time, Field userId, Field amount) {
        this.id = id;
        this.time = time;
        this.userId = userId;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return String.format("Transaction{id=%d, time=%d, userId=%d, amount=%d}",
                id.value, time.value, userId.value, amount.value);
    }
}

// Interfaces
interface FilterEngine {
    boolean isValid(Transaction t);
}

// Filter implementations
class FieldFilterEngine implements FilterEngine {
    private final FieldName fieldName;
    private final int filterValue;
    private final Operand operand;

    FieldFilterEngine(FieldName fieldName, int filterValue, Operand operand) {
        this.fieldName = fieldName;
        this.filterValue = filterValue;
        this.operand = operand;
    }

    @Override
    public boolean isValid(Transaction t) {
        int transactionValue = switch (fieldName) {
            case ID -> t.id.value;
            case TIME -> t.time.value;
            case USER_ID -> t.userId.value;
            case AMOUNT -> t.amount.value;
        };

        return switch (operand) {
            case EQUAL_TO -> transactionValue == filterValue;
            case GREATER_THAN -> transactionValue > filterValue;
            case LESS_THAN -> transactionValue < filterValue;
            case GREATER_THAN_OR_EQUAL_TO -> transactionValue >= filterValue;
            case LESS_THAN_OR_EQUAL_TO -> transactionValue <= filterValue;
        };
    }
}

class CompositeFilterEngine implements FilterEngine {
    private final List<FilterEngine> filterEngines;
    private final CompositeOperand compositeOperand;

    CompositeFilterEngine(List<FilterEngine> filterEngines, CompositeOperand compositeOperand) {
        this.filterEngines = filterEngines;
        this.compositeOperand = compositeOperand;
    }

    @Override
    public boolean isValid(Transaction t) {
        return switch (compositeOperand) {
            case AND -> filterEngines.stream().allMatch(engine -> engine.isValid(t));
            case OR -> filterEngines.stream().anyMatch(engine -> engine.isValid(t));
        };
    }
}

// Main class
public class SearchEngine {

    private final List<Transaction> transactions;

    SearchEngine(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public List<Transaction> search(FilterEngine compositeFilter) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : transactions) {
            if (compositeFilter.isValid(t)) {
                result.add(t);
            }
        }

        return result;
    }

    public static void main(String[] args) {
        List<Transaction> transactions = List.of(
                new Transaction(new Field(FieldName.ID, 1), new Field(FieldName.TIME, 2000),
                        new Field(FieldName.USER_ID, 1), new Field(FieldName.AMOUNT, 1100)),
                new Transaction(new Field(FieldName.ID, 2), new Field(FieldName.TIME, 3000),
                        new Field(FieldName.USER_ID, 1), new Field(FieldName.AMOUNT, 5100)),
                new Transaction(new Field(FieldName.ID, 3), new Field(FieldName.TIME, 4000),
                        new Field(FieldName.USER_ID, 2), new Field(FieldName.AMOUNT, 50)),
                new Transaction(new Field(FieldName.ID, 4), new Field(FieldName.TIME, 5000),
                        new Field(FieldName.USER_ID, 5), new Field(FieldName.AMOUNT, 700)),
                new Transaction(new Field(FieldName.ID, 5), new Field(FieldName.TIME, 6000),
                        new Field(FieldName.USER_ID, 11), new Field(FieldName.AMOUNT, 250)),
                new Transaction(new Field(FieldName.ID, 6), new Field(FieldName.TIME, 7000),
                        new Field(FieldName.USER_ID, 2), new Field(FieldName.AMOUNT, 10)));

        // Create filters directly
        FilterEngine idLessThan5 = new FieldFilterEngine(FieldName.ID, 5, Operand.LESS_THAN);
        FilterEngine amountGreaterThan500 = new FieldFilterEngine(FieldName.AMOUNT, 500, Operand.GREATER_THAN);

        // Combine using AND
        FilterEngine compositeFilter = new CompositeFilterEngine(
                List.of(idLessThan5, amountGreaterThan500),
                CompositeOperand.AND);

        // Call search
        SearchEngine searchEngine = new SearchEngine(transactions);

        // Filter transactions
        List<Transaction> result = searchEngine.search(compositeFilter);

        // Print results
        result.forEach(System.out::println);
    }
}
