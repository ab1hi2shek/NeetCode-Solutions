## 🏦 Level 1: Implement Basic Banking Operations
Objective: Create methods to add an account, withdraw money, and transfer funds between accounts.

Key Considerations:

Data Structures: Use appropriate data structures to store account information.

Concurrency: Ensure thread safety to handle multiple requests simultaneously.

Validation: Implement checks for valid account numbers and sufficient funds for withdrawals.

Sample Implementation:

```java
import java.util.*;

public class BankSystem {
    private Map<String, Double> accounts = new HashMap<>();

    // Create a new account with an initial balance of 0
    public boolean createAccount(String accountId) {
        if (accounts.containsKey(accountId)) return false;
        accounts.put(accountId, 0.0);
        return true;
    }

    // Deposit money into an account
    public boolean deposit(String accountId, double amount) {
        if (amount <= 0 || !accounts.containsKey(accountId)) return false;
        accounts.put(accountId, accounts.get(accountId) + amount);
        return true;
    }

    // Withdraw money from an account
    public boolean withdraw(String accountId, double amount) {
        if (amount <= 0 || !accounts.containsKey(accountId) || accounts.get(accountId) < amount) return false;
        accounts.put(accountId, accounts.get(accountId) - amount);
        return true;
    }

    // Transfer money from one account to another
    public boolean transfer(String fromAccountId, String toAccountId, double amount) {
        if (amount <= 0 || !accounts.containsKey(fromAccountId) || !accounts.containsKey(toAccountId) ||
            accounts.get(fromAccountId) < amount) return false;
        accounts.put(fromAccountId, accounts.get(fromAccountId) - amount);
        accounts.put(toAccountId, accounts.get(toAccountId) + amount);
        return true;
    }
}
```

## 🏦 Level 2: Enhanced Banking System with Transaction Tracking
Enhancements from Level 1:
Transaction Tracking: Each account will maintain a list of its transactions.

Transaction Types: Support for deposit, withdrawal, and transfer transactions.

Transaction Counting: Ability to retrieve the top N accounts based on the number of transactions.

Data Structures:
Account: Stores account balance and a list of transactions.

Transaction: Represents a transaction with details like type, amount, and timestamp.

```java
import java.util.*;

public class BankSystem {
    private Map<String, Account> accounts = new HashMap<>();
    private int transferCount = 0;

    // Create a new account with an initial balance of 0
    public boolean createAccount(String accountId) {
        if (accounts.containsKey(accountId)) return false;
        accounts.put(accountId, new Account(accountId));
        return true;
    }

    // Deposit money into an account
    public boolean deposit(String accountId, double amount) {
        if (amount <= 0 || !accounts.containsKey(accountId)) return false;
        Account account = accounts.get(accountId);
        account.balance += amount;
        account.transactions.add(new Transaction("Deposit", amount));
        return true;
    }

    // Withdraw money from an account
    public boolean withdraw(String accountId, double amount) {
        if (amount <= 0 || !accounts.containsKey(accountId)) return false;
        Account account = accounts.get(accountId);
        if (account.balance < amount) return false;
        account.balance -= amount;
        account.transactions.add(new Transaction("Withdraw", amount));
        return true;
    }

    // Transfer money from one account to another
    public boolean transfer(String fromAccountId, String toAccountId, double amount) {
        if (amount <= 0 || !accounts.containsKey(fromAccountId) || !accounts.containsKey(toAccountId)) return false;
        Account fromAccount = accounts.get(fromAccountId);
        Account toAccount = accounts.get(toAccountId);
        if (fromAccount.balance < amount) return false;
        fromAccount.balance -= amount;
        toAccount.balance += amount;
        fromAccount.transactions.add(new Transaction("Transfer Out", amount));
        toAccount.transactions.add(new Transaction("Transfer In", amount));
        transferCount++;
        return true;
    }

    // Retrieve the top N accounts with the most transactions
    public List<String> getTopNAccounts(int N) {
        List<Account> accountList = new ArrayList<>(accounts.values());
        accountList.sort((a1, a2) -> Integer.compare(a2.transactions.size(), a1.transactions.size()));
        List<String> topAccounts = new ArrayList<>();
        for (int i = 0; i < Math.min(N, accountList.size()); i++) {
            topAccounts.add(accountList.get(i).accountId);
        }
        return topAccounts;
    }

    // Account class representing each account
    private static class Account {
        String accountId;
        double balance;
        List<Transaction> transactions;

        Account(String accountId) {
            this.accountId = accountId;
            this.balance = 0;
            this.transactions = new ArrayList<>();
        }
    }

    // Transaction class representing each transaction
    private static class Transaction {
        String type;
        double amount;
        long timestamp;

        Transaction(String type, double amount) {
            this.type = type;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
```


## 🕒 Level 3: Scheduled Transfers with Timestamp and Expiry
Requirements:
Initiate Transfer:

Input: Source account ID, target account ID, amount, timestamp.

Action: Deduct the amount from the source account and create a pending transfer record.

Output: Return a transfer ID in the format "Transfer<ordinal number>".

Accept Transfer:

Input: Transfer ID, target account ID, current timestamp.

Action: Accept the transfer if the withdrawal from the source account occurred within the last 24 hours.

Output: Return true if successful, false otherwise.

Design Considerations:
Data Structures:

Account: Stores account balance and a list of transactions.

Transaction: Represents a transaction with details like type, amount, timestamp.

Transfer: Represents a pending transfer with details like source account, target account, amount, timestamp, and status.

Concurrency: Ensure thread safety to handle multiple requests simultaneously.

```java
import java.util.*;
import java.util.concurrent.locks.*;

public class BankSystem {
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Transfer> transfers = new HashMap<>();
    private int transferCount = 0;
    private final Lock lock = new ReentrantLock();

    // Create a new account with an initial balance of 0
    public boolean createAccount(String accountId) {
        lock.lock();
        try {
            if (accounts.containsKey(accountId)) return false;
            accounts.put(accountId, new Account(accountId));
            return true;
        } finally {
            lock.unlock();
        }
    }

    // Deposit money into an account
    public boolean deposit(String accountId, double amount) {
        lock.lock();
        try {
            if (amount <= 0 || !accounts.containsKey(accountId)) return false;
            Account account = accounts.get(accountId);
            account.balance += amount;
            account.transactions.add(new Transaction("Deposit", amount));
            return true;
        } finally {
            lock.unlock();
        }
    }

    // Withdraw money from an account
    public boolean withdraw(String accountId, double amount) {
        lock.lock();
        try {
            if (amount <= 0 || !accounts.containsKey(accountId)) return false;
            Account account = accounts.get(accountId);
            if (account.balance < amount) return false;
            account.balance -= amount;
            account.transactions.add(new Transaction("Withdraw", amount));
            return true;
        } finally {
            lock.unlock();
        }
    }

    // Transfer money from one account to another
    public String initiateTransfer(String fromAccountId, String toAccountId, double amount, long timestamp) {
        lock.lock();
        try {
            if (amount <= 0 || !accounts.containsKey(fromAccountId) || !accounts.containsKey(toAccountId)) return null;
            Account fromAccount = accounts.get(fromAccountId);
            if (fromAccount.balance < amount) return null;
            fromAccount.balance -= amount;
            fromAccount.transactions.add(new Transaction("Transfer Out", amount));
            transferCount++;
            String transferId = "Transfer" + transferCount;
            transfers.put(transferId, new Transfer(fromAccountId, toAccountId, amount, timestamp));
            return transferId;
        } finally {
            lock.unlock();
        }
    }

    // Accept a pending transfer
    public boolean acceptTransfer(String transferId, String toAccountId, long currentTimestamp) {
        lock.lock();
        try {
            Transfer transfer = transfers.get(transferId);
            if (transfer == null || !transfer.toAccountId.equals(toAccountId)) return false;
            long timeElapsed = currentTimestamp - transfer.timestamp;
            if (timeElapsed > 24 * 60 * 60 * 1000) return false;
            Account toAccount = accounts.get(toAccountId);
            toAccount.balance += transfer.amount;
            toAccount.transactions.add(new Transaction("Transfer In", transfer.amount));
            transfers.remove(transferId);
            return true;
        } finally {
            lock.unlock();
        }
    }

    // Account class representing each account
    private static class Account {
        String accountId;
        double balance;
        List<Transaction> transactions;

        Account(String accountId) {
            this.accountId = accountId;
            this.balance = 0;
            this.transactions = new ArrayList<>();
        }
    }

    // Transaction class representing each transaction
    private static class Transaction {
        String type;
        double amount;
        long timestamp;

        Transaction(String type, double amount) {
            this.type = type;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // Transfer class representing a pending transfer
    private static class Transfer {
        String fromAccountId;
        String toAccountId;
        double amount;
        long timestamp;

        Transfer(String fromAccountId, String toAccountId, double amount, long timestamp) {
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.amount = amount;
            this.timestamp = timestamp;
        }
    }
}
```

