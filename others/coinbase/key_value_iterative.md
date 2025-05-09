Question 1: Basic Key-Value Store (Get, Set, Delete, Update)

```java
import java.util.*;

public class KeyValueStore {
    private Map<String, String> store;

    public KeyValueStore() {
        this.store = new HashMap<>();
    }

    public void set(String key, String value) {
        store.put(key, value);
    }

    public String get(String key) {
        return store.getOrDefault(key, null);
    }

    public void delete(String key) {
        store.remove(key);
    }

    public void update(String key, String value) {
        if (store.containsKey(key)) {
            store.put(key, value);
        }
    }
}
```

✅ Question 2: Add prefixSearch and containsSearch
We’ll still use a basic HashMap, but introduce a TreeSet for sorted prefix search or iterate over keys if time is tight.

```java
import java.util.*;

public class KeyValueStoreV2 {
    private Map<String, String> store;

    public KeyValueStoreV2() {
        this.store = new HashMap<>();
    }

    public void set(String key, String value) {
        store.put(key, value);
    }

    public String get(String key) {
        return store.getOrDefault(key, null);
    }

    public void delete(String key) {
        store.remove(key);
    }

    public void update(String key, String value) {
        if (store.containsKey(key)) {
            store.put(key, value);
        }
    }

    public List<String> prefixSearch(String prefix) {
        List<String> result = new ArrayList<>();
        for (String key : store.keySet()) {
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }
        return result;
    }

    public List<String> containsSearch(String substring) {
        List<String> result = new ArrayList<>();
        for (String key : store.keySet()) {
            if (key.contains(substring)) {
                result.add(key);
            }
        }
        return result;
    }
}
```

✅ Question 3: Add TTL Support with Timestamp Awareness
For TTL, we'll introduce a new data class (Entry) and track the TTL and insertion time. Assume currentTimestamp is passed to each operation.

```java
import java.util.*;

public class KeyValueStoreV3 {
    private static class Entry {
        String value;
        Long expiryTime; // null means no TTL

        Entry(String value, Long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        boolean isExpired(long currentTime) {
            return expiryTime != null && currentTime >= expiryTime;
        }
    }

    private Map<String, Entry> store;

    public KeyValueStoreV3() {
        this.store = new HashMap<>();
    }

    public void set(String key, String value, Long ttlSeconds, long currentTime) {
        Long expiryTime = ttlSeconds != null ? currentTime + ttlSeconds : null;
        store.put(key, new Entry(value, expiryTime));
    }

    public String get(String key, long currentTime) {
        Entry entry = store.get(key);
        if (entry == null || entry.isExpired(currentTime)) {
            return null;
        }
        return entry.value;
    }

    public void delete(String key) {
        store.remove(key);
    }

    public void update(String key, String value, long currentTime) {
        Entry entry = store.get(key);
        if (entry != null && !entry.isExpired(currentTime)) {
            entry.value = value;
        }
    }

    public List<String> prefixSearch(String prefix, long currentTime) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Entry> e : store.entrySet()) {
            if (e.getKey().startsWith(prefix) && !e.getValue().isExpired(currentTime)) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    public List<String> containsSearch(String substring, long currentTime) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Entry> e : store.entrySet()) {
            if (e.getKey().contains(substring) && !e.getValue().isExpired(currentTime)) {
                result.add(e.getKey());
            }
        }
        return result;
    }
}
```

✅ Part 4: Add undo and redo functions.

For the undo and redo functionality, we need to maintain the state of operations so that we can revert or reapply them as needed. To achieve this, we'll use two stacks: one for undo and another for redo.

Design Considerations:
Undo Stack: Keeps the history of operations to be undone.

Redo Stack: Keeps operations that can be reapplied after an undo.

Each operation (set, delete, update) needs to store the previous state to allow undo, and similarly store the undone state for redo.

Here’s how you can extend the KeyValueStore with undo/redo functionality:

```java
public class KeyValueStoreV4 {
    private static class Entry {
        String value;
        Long expiryTime; // null means no TTL

        Entry(String value, Long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        boolean isExpired(long currentTime) {
            return expiryTime != null && currentTime >= expiryTime;
        }

        // For cloning the entry
        Entry clone() {
            return new Entry(this.value, this.expiryTime);
        }
    }

    private Map<String, Entry> store;
    private Stack<Command> undoStack;
    private Stack<Command> redoStack;

    public KeyValueStoreV4() {
        this.store = new HashMap<>();
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
    }

    // Command interface for undo/redo
    private interface Command {
        void undo();
        void redo();
    }

    private class SetCommand implements Command {
        String key;
        Entry oldEntry;
        Entry newEntry;

        SetCommand(String key, Entry oldEntry, Entry newEntry) {
            this.key = key;
            this.oldEntry = oldEntry;
            this.newEntry = newEntry;
        }

        public void undo() {
            store.put(key, oldEntry);
        }

        public void redo() {
            store.put(key, newEntry);
        }
    }

    private class DeleteCommand implements Command {
        String key;
        Entry oldEntry;

        DeleteCommand(String key, Entry oldEntry) {
            this.key = key;
            this.oldEntry = oldEntry;
        }

        public void undo() {
            store.put(key, oldEntry);
        }

        public void redo() {
            store.remove(key);
        }
    }

    private class UpdateCommand implements Command {
        String key;
        Entry oldEntry;
        Entry newEntry;

        UpdateCommand(String key, Entry oldEntry, Entry newEntry) {
            this.key = key;
            this.oldEntry = oldEntry;
            this.newEntry = newEntry;
        }

        public void undo() {
            store.put(key, oldEntry);
        }

        public void redo() {
            store.put(key, newEntry);
        }
    }

    public void set(String key, String value, Long ttlSeconds, long currentTime) {
        Entry oldEntry = store.containsKey(key) ? store.get(key).clone() : null;
        Long expiryTime = ttlSeconds != null ? currentTime + ttlSeconds : null;
        Entry newEntry = new Entry(value, expiryTime);
        store.put(key, newEntry);

        undoStack.push(new SetCommand(key, oldEntry, newEntry));
        redoStack.clear(); // Clear redo stack after new operation
    }

    public String get(String key, long currentTime) {
        Entry entry = store.get(key);
        if (entry == null || entry.isExpired(currentTime)) {
            return null;
        }
        return entry.value;
    }

    public void delete(String key) {
        Entry oldEntry = store.get(key) != null ? store.get(key).clone() : null;
        store.remove(key);

        undoStack.push(new DeleteCommand(key, oldEntry));
        redoStack.clear();
    }

    public void update(String key, String value, long currentTime) {
        Entry oldEntry = store.containsKey(key) ? store.get(key).clone() : null;
        Entry newEntry = new Entry(value, null); // Assuming no TTL update for simplicity
        store.put(key, newEntry);

        undoStack.push(new UpdateCommand(key, oldEntry, newEntry));
        redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.redo();
            undoStack.push(command);
        }
    }

    // Optional: Function to clear undo/redo history
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    // A helper function to print the state of the store (for testing purposes)
    public void printState() {
        System.out.println("Current store state: " + store);
    }
}

```
**Key Changes and Operations:**

Command Pattern: A simple design pattern used for undo/redo functionality.

Each operation (set, delete, update) is wrapped in a command object that can perform the undo and redo actions.

Undo Stack: After each operation (set, delete, update), the previous state (old entry) is stored in the undo stack.

Redo Stack: If an operation is undone, the corresponding redo operation is pushed to the redo stack.

Undo: Pops a command from the undo stack and reverts the state to the previous state.

Redo: Pops a command from the redo stack and reapplies the operation.