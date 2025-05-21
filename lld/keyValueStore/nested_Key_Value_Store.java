package lld.keyValueStore;

import java.util.*;

class nested_Key_Value_Store {

    Map<String, Map<String, TreeMap<Integer, String>>> store;

    nested_Key_Value_Store() {
        store = new HashMap<>();
    }

    // Method to implement the 'put' operation
    public void put(String key, String sub_key, String value, int timestamp) {

        store.putIfAbsent(key, new HashMap<>());
        Map<String, TreeMap<Integer, String>> subKeyMap = store.get(key);
        subKeyMap.putIfAbsent(sub_key, new TreeMap<>());

        subKeyMap.get(sub_key).put(timestamp, value);

    }

    // Method to implement the 'get' operation for a specific timestamp
    public String get(String key, String sub_key, int timestamp) {

        Map<String, TreeMap<Integer, String>> subKeyMap = store.get(key);
        if (subKeyMap == null)
            return "";

        TreeMap<Integer, String> valueMap = subKeyMap.get(sub_key);
        if (valueMap == null)
            return "";

        Integer floor = valueMap.floorKey(timestamp);
        return floor == null ? "" : valueMap.get(floor);
    }

    // Method to implement the 'getLatest' operation for the latest value
    public String getLatest(String key, String sub_key) {

        Map<String, TreeMap<Integer, String>> subKeyMap = store.get(key);
        if (subKeyMap == null)
            return "";

        TreeMap<Integer, String> valueMap = subKeyMap.get(sub_key);
        if (valueMap == null || valueMap.isEmpty())
            return "";

        return valueMap.lastEntry().getValue();
    }

    // Method to implement the 'getAllValues' operation for all values of a key
    public Map<String, String> getAllValues(String key) {

        Map<String, String> result = new HashMap<>();

        if (store.containsKey(key)) {
            for (Map.Entry<String, TreeMap<Integer, String>> entry : store.get(key).entrySet()) {
                result.put(entry.getKey(), entry.getValue().lastEntry().getValue());
            }
        }

        return result;
    }

    // Method to implement the 'deleteKey' operation
    public void deleteKey(String key) {
        store.remove(key);
    }

    // Method to implement the 'deleteSubKey' operation
    public void deleteSubKey(String key, String sub_key) {
        Map<String, TreeMap<Integer, String>> subKeyMap = store.get(key);
        if (subKeyMap != null) {
            subKeyMap.remove(sub_key);
        }

    }

    // Test the functionality of the store with sample operations
    public static void main(String[] args) {
        // Create an instance of the store
        nested_Key_Value_Store store = new nested_Key_Value_Store();

        // Test Case 1: Adding values
        store.put("user1", "email", "alice@example.com", 1);
        store.put("user1", "email", "alice@work.com", 2);
        store.put("user1", "phone", "123-456", 3);
        store.put("user2", "email", "bob@example.com", 1);

        // Test Case 2: Get values at a specific timestamp
        assert store.get("user1", "email", 1).equals("alice@example.com") : "Test failed at get(user1, email, 1)";
        assert store.get("user1", "email", 2).equals("alice@example.com") : "Test failed at get(user1, email, 2)";
        assert store.get("user1", "phone", 3).equals("123-456") : "Test failed at get(user1, phone, 3)";
        assert store.get("user2", "email", 1).equals("bob@example.com") : "Test failed at get(user2, email, 1)";
        assert store.get("user1", "email", 3).equals("alice@work.com") : "Test failed at get(user1, email, 3)";

        // Test Case 3: Get the latest value for a key, sub_key pair
        assert store.getLatest("user1", "email").equals("alice@work.com") : "Test failed at getLatest(user1, email)";
        assert store.getLatest("user1", "phone").equals("123-456") : "Test failed at getLatest(user1, phone)";
        assert store.getLatest("user2", "email").equals("bob@example.com") : "Test failed at getLatest(user2, email)";

        // Test Case 4: Get all values for a key (should return the latest for each
        // sub_key)
        assert store.getAllValues("user1").equals(Map.of("email", "alice@work.com", "phone", "123-456"))
                : "Test failed at getAllValues(user1)";
        assert store.getAllValues("user2").equals(Map.of("email", "bob@example.com"))
                : "Test failed at getAllValues(user2)";

        // Test Case 5: Deleting sub-keys
        store.deleteSubKey("user1", "email");
        assert store.getAllValues("user1").equals(Map.of("phone", "123-456"))
                : "Test failed at deleteSubKey(user1, email)";

        // Test Case 6: Deleting keys
        store.deleteKey("user2");
        assert store.getAllValues("user2").equals(Collections.emptyMap()) : "Test failed at deleteKey(user2)";

        // Test Case 7: Attempting to get deleted key/sub_key
        assert store.get("user2", "email", 1).equals("") : "Test failed at get(user2, email, 1)";
        assert store.get("user1", "email", 2).equals("") : "Test failed at get(user1, email, 2)";

        // Edge Case 1: No matching value at a specific timestamp
        assert store.get("user1", "phone", 2).equals("") : "Test failed at get(user1, phone, 2)"; // phone doesn't exist
                                                                                                  // at timestamp 2

        // Edge Case 2: Trying to get a value for a sub_key that doesn't exist
        assert store.get("user1", "address", 1).equals("") : "Test failed at get(user1, address, 1)"; // address doesn't
                                                                                                      // exist

        // Edge Case 3: Testing with multiple timestamps for the same sub_key
        store.put("user1", "email", "alice@example.com", 4);
        store.put("user1", "email", "alice@home.com", 5);
        assert store.get("user1", "email", 4).equals("alice@example.com") : "Test failed at get(user1, email, 4)";
        assert store.get("user1", "email", 5).equals("alice@home.com") : "Test failed at get(user1, email, 5)";

        // Test Case 8: After deletion, retrieving the latest value (should return null
        // or empty string for deleted keys/sub_keys)
        assert store.getLatest("user2", "email").equals("") : "Test failed at getLatest(user2, email)";
        assert store.getLatest("user1", "email").equals("alice@home.com") : "Test failed at getLatest(user1, email)";

        // If all assertions pass
        System.out.println("All tests passed!");
    }
}