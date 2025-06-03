package practice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rarity enum
 */
enum Rarity {
    COMMON(5),
    IN_BETWEEN(3),
    RARE(1);

    final int value;

    Rarity(int value) {
        this.value = value;
    }
}

/**
 * Property class to hold a property raity and name.
 */
class Property {
    final String name;
    final Rarity rarity;

    Property(String name, Rarity rarity) {
        this.name = name;
        this.rarity = rarity;
    }
}

/**
 * Paginated result classes.
 */
class PaginatedResult {
    final List<List<String>> properties;

    PaginatedResult(final List<List<String>> properties) {
        this.properties = properties;
    }
}

class CursorBasedPaginatedResult extends PaginatedResult {

    CursorBasedPaginatedResult(List<List<String>> properties) {
        super(properties);
    }
}

class PageBasedPaginatedResult extends PaginatedResult {

    final boolean hasNextPage;

    PageBasedPaginatedResult(List<List<String>> properties, boolean hasNextPage) {
        super(properties);
        this.hasNextPage = hasNextPage;
    }
}

/**
 * Main class to generate random properties.
 */
public class RandomProperties {

    final List<List<Property>> properties;
    final Random random;
    List<List<String>> pageBasedLists;

    RandomProperties(List<List<Property>> properties) {
        this.properties = properties;
        random = new Random();

        // Generate pageBasedLists.
        pageBasedLists = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            List<String> propertyRow = generateARandomEntry();
            pageBasedLists.add(propertyRow);
        }

    }

    /**
     * Function to return random properties.
     * 
     * @param outputLength
     * @return
     */
    public List<List<String>> generateRandomProperty(int outputLength) {

        List<List<String>> resultProperties = new ArrayList<>();

        for (int i = 0; i < outputLength; i++) {
            List<String> propertyRow = generateARandomEntry();
            resultProperties.add(propertyRow);
        }

        return resultProperties;
    }

    /**
     * Function to return random unique properties.
     * 
     * @param outputLength
     * @return
     */
    public List<List<String>> generateRandomUniqueProperty(int outputLength) {

        validateInput(outputLength);
        List<List<String>> resultProperties = new ArrayList<>();

        while (resultProperties.size() < outputLength) {
            List<String> propertyRow = generateARandomEntry();
            boolean isUnique = checkIfPropertyRowIsUnique(resultProperties, propertyRow);

            if (isUnique) {
                resultProperties.add(propertyRow);
            }
        }

        return resultProperties;
    }

    /**
     * Function to return random properies while accounting rarity factor.
     * 
     * @param outputLength
     * @return
     */
    public List<List<String>> generateRandomPropertyWithRarity(int outputLength) {

        List<List<String>> resultProperties = new ArrayList<>();
        for (int i = 0; i < outputLength; i++) {
            List<String> propertyRow = generateARandomEntryWithWeights();
            resultProperties.add(propertyRow);
        }

        return resultProperties;
    }

    /**
     * Function to return cursor based paginated results.
     * 
     * @param offset
     * @param limit
     * @return
     */
    public CursorBasedPaginatedResult generateRandomPropertyWithCursorBasedPagination(int offset, int limit) {

        // can not implement it as there is no Id like attribute in the input.
        // throw new UnsupportedOperationException("Cursor-based pagination not
        // implemented yet.");
        List<List<String>> resultProperties = new ArrayList<>();
        return new CursorBasedPaginatedResult(resultProperties);
    }

    /**
     * Function to return page based paginated result of random properties.
     * 
     * @param pageNumber
     * @param pageSize
     * @return
     */
    public PageBasedPaginatedResult generateRandomPropertyWithPageBasedPagination(int pageNumber, int pageSize) {

        if (pageNumber < 1) {
            throw new IllegalArgumentException("page number should be greater than Zero");
        }

        int offset = (pageNumber - 1) * pageSize;
        List<List<String>> resultProperties = new ArrayList<>();
        boolean hasNextPage = true;

        int index = offset;
        while (index < offset + pageSize) {
            if (index >= pageBasedLists.size()) {
                hasNextPage = false;
                break;
            }

            resultProperties.add(pageBasedLists.get(index));
            index++;
        }
        return new PageBasedPaginatedResult(resultProperties, hasNextPage);
    }

    private List<String> generateARandomEntry() {
        List<String> randomEntry = new ArrayList<>();
        for (List<Property> property : properties) {
            if (property == null || property.isEmpty()) {
                continue;
            }

            int randomIndex = random.nextInt(property.size());
            String randomValue = property.get(randomIndex).name;

            randomEntry.add(randomValue);
        }
        return randomEntry;
    }

    private void validateInput(int requestedNumberOfProperties) {
        int maxCombination = 1;
        for (List<Property> property : properties) {
            maxCombination *= property.size();
        }

        if (requestedNumberOfProperties > maxCombination) {
            throw new IllegalArgumentException(requestedNumberOfProperties + " is greater than " + maxCombination);
        }
    }

    private boolean checkIfPropertyRowIsUnique(List<List<String>> allProperties, List<String> propertyRow) {

        for (List<String> property : allProperties) {
            if (property.equals(propertyRow)) {
                return false; // Found a duplicate
            }
        }
        return true; // No duplicates found
    }

    private List<String> generateARandomEntryWithWeights() {

        List<String> randomList = new ArrayList<>();
        for (List<Property> property : properties) {

            // Get total sum of rarity weights.
            int weightsSum = 0;
            for (Property item : property) {
                weightsSum += item.rarity.value;
            }

            int randomIndex = random.nextInt(weightsSum);

            // Algo to get random element on weights.
            int randomElement = 0;
            for (Property item : property) {
                randomElement += item.rarity.value;

                if (randomElement > randomIndex) {
                    randomList.add(item.name);
                    break;
                }
            }

        }
        return randomList;
    }

    public static void main(String[] args) {

        // Tree Length property
        Property shortValue = new Property("short", Rarity.COMMON);
        Property mediumValue = new Property("medium", Rarity.COMMON);
        Property tallValue = new Property("tall", Rarity.RARE);
        List<Property> lengthProperties = List.of(shortValue, mediumValue, tallValue);

        // Tree Leaf color property
        Property greenLeaf = new Property("green", Rarity.COMMON);
        Property brownLeaf = new Property("brown", Rarity.IN_BETWEEN);
        Property yellowLeaf = new Property("yellow", Rarity.RARE);
        Property redLeaf = new Property("red", Rarity.COMMON);
        List<Property> leafColorProperties = List.of(greenLeaf, brownLeaf, yellowLeaf, redLeaf);

        // Tree bark texture property
        Property smoothBark = new Property("smooth", Rarity.COMMON);
        Property roughBark = new Property("rough", Rarity.IN_BETWEEN);
        Property crackedBark = new Property("cracked", Rarity.RARE);
        List<Property> barkTextureProperties = List.of(smoothBark, roughBark, crackedBark);

        List<List<Property>> input = List.of(lengthProperties, leafColorProperties, barkTextureProperties);

        RandomProperties randomProperties = new RandomProperties(input);

        // Get random properties
        List<List<String>> randomProperty = randomProperties.generateRandomProperty(3);
        System.out.println("---- Printing Random Property Output ----");
        randomProperty.forEach(t -> System.out.println(t));

        // Get random unique properties
        List<List<String>> randomUniqueProperty = randomProperties.generateRandomUniqueProperty(2);
        System.out.println("---- Printing Random Unique Property Output ----");
        randomUniqueProperty.forEach(t -> System.out.println(t));

        // Get random properties with rarity
        List<List<String>> randomPropertyWithRarity = randomProperties.generateRandomPropertyWithRarity(50);
        System.out.println("---- Printing Random Property Output With Rarity ----");
        randomPropertyWithRarity.forEach(t -> System.out.println(t));

        // Get random properties with pagination cursor based
        System.out.println("---- Printing Random Property Output With cursor based pagination ----");
        CursorBasedPaginatedResult cursorBasedPaginatedResult = randomProperties
                .generateRandomPropertyWithCursorBasedPagination(1, 1);

        // Get random properties with pagination page based
        System.out.println("---- Printing Random Property Output With page based pagination ----");
        int pageNumber = 1;
        boolean hasNextPage = true;
        do {
            System.out.println("---- Printing page " + pageNumber + " ----");
            PageBasedPaginatedResult paginatedProperties = randomProperties
                    .generateRandomPropertyWithPageBasedPagination(pageNumber++, 12);
            // Process the result
            paginatedProperties.properties.forEach(System.out::println);
            hasNextPage = paginatedProperties.hasNextPage;

        } while (hasNextPage);
    }

}
