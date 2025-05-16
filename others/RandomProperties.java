package others;

import java.util.*;

import others.Property.PropertyValue;
import others.Property.Rarity;

/**
 * Class to generate random properties given a list of properties.
 */
class Property {

    enum Rarity {
        COMMON,
        UNCOMMON,
        RARE
    }

    static class PropertyValue {
        private final String name;
        private final Rarity rarity;

        PropertyValue(String name, Rarity rarity) {
            this.name = name;
            this.rarity = rarity;
        }

        public String getName() {
            return this.name;
        }

        public Rarity getRarity() {
            return this.rarity;
        }
    }

    private final String name;
    private final List<PropertyValue> values;

    Property(String name, List<PropertyValue> values) {
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return this.name;
    }

    public List<PropertyValue> getValues() {
        return this.values;
    }
}

public class RandomProperties {

    private final List<Property> properties;
    private final Random random = new Random();

    RandomProperties(List<Property> properties) {
        this.properties = properties;
    }

    static class RandomOutput {
        private final String propertyName;
        private final String valueName;

        RandomOutput(String propertyName, String valueName) {
            this.propertyName = propertyName;
            this.valueName = valueName;
        }

        public String getPropertyName() {
            return this.propertyName;
        }

        public String getValueName() {
            return this.valueName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            RandomOutput that = (RandomOutput) o;
            return Objects.equals(propertyName, that.propertyName) &&
                    Objects.equals(valueName, that.valueName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(propertyName, valueName);
        }
    }

    /**
     * Generates random property from the given property list.
     * 
     * @param numberOfProperties
     * @return
     */
    public List<List<RandomOutput>> generateRandomProperty(int numberOfProperties) {

        if (numberOfProperties <= 0) {
            throw new IllegalArgumentException("numberOfProperties must be greater than zero");
        }

        List<List<RandomOutput>> output = new ArrayList<>();
        for (int i = 0; i < numberOfProperties; i++) {
            List<RandomOutput> randomEntry = generateARandomEntry();
            output.add(randomEntry);
        }

        return output;
    }

    /**
     * Generates random unique property
     * 
     * @param numberOfProperties
     * @return
     */
    public List<List<RandomOutput>> generateUniqueRandomProperty(int numberOfProperties) {
        if (numberOfProperties <= 0) {
            throw new IllegalArgumentException("numberOfProperties must be greater than zero");
        }

        int maxCombinations = 1;
        for (Property property : properties) {
            maxCombinations = maxCombinations * property.getValues().size();
        }
        int target = Math.min(maxCombinations, numberOfProperties);

        List<List<RandomOutput>> output = new ArrayList<>();
        Set<List<RandomOutput>> uniqueEntries = new HashSet<>();
        while (target > 0) {
            List<RandomOutput> randomEntry = generateARandomEntry();

            if (!uniqueEntries.contains(randomEntry)) {
                output.add(randomEntry);
                target--;
                uniqueEntries.add(randomEntry);
            }
        }
        return output;
    }

    private List<RandomOutput> generateARandomEntry() {
        List<RandomOutput> randomEntry = new ArrayList<>();
        for (Property property : properties) {
            List<PropertyValue> values = property.getValues();

            if (values == null || values.isEmpty()) {
                continue;
            }

            int randomIndex = random.nextInt(values.size());
            String randomValue = values.get(randomIndex).getName();

            RandomOutput randomOutput = new RandomOutput(property.getName(), randomValue);
            randomEntry.add(randomOutput);
        }

        return randomEntry;
    }

    public static void main(String[] args) {

        // Tree Length property
        PropertyValue shortValue = new PropertyValue("short", Rarity.COMMON);
        PropertyValue mediumValue = new PropertyValue("medium", Rarity.COMMON);
        PropertyValue tallValue = new PropertyValue("tall", Rarity.RARE);
        Property lengthProperties = new Property("length", List.of(shortValue, mediumValue, tallValue));

        // Tree Leaf color property
        PropertyValue greenLeaf = new PropertyValue("green", Rarity.COMMON);
        PropertyValue brownLeaf = new PropertyValue("brown", Rarity.UNCOMMON);
        PropertyValue yellowLeaf = new PropertyValue("yellow", Rarity.RARE);
        PropertyValue redLeaf = new PropertyValue("red", Rarity.COMMON);
        Property leafColorProperties = new Property("leaf_color",
                List.of(greenLeaf, brownLeaf, yellowLeaf, redLeaf));

        // Tree bark texture property
        PropertyValue smoothBark = new PropertyValue("smooth", Rarity.COMMON);
        PropertyValue roughBark = new PropertyValue("rough", Rarity.UNCOMMON);
        PropertyValue crackedBark = new PropertyValue("cracked", Rarity.RARE);
        Property barkTextureProperties = new Property("bark_texture",
                List.of(smoothBark, roughBark, crackedBark));

        RandomProperties randomProperties = new RandomProperties(
                List.of(lengthProperties, leafColorProperties, barkTextureProperties));

        // Call random properties generation
        List<List<RandomOutput>> randomOutput = randomProperties.generateRandomProperty(10);
        printBeautifully(randomOutput);

        // Call unique random properties generation
        List<List<RandomOutput>> randomUniqueOutput = randomProperties.generateUniqueRandomProperty(10);
        printBeautifully(randomUniqueOutput);

    }

    public static void printBeautifully(List<List<RandomOutput>> randomOutput) {
        for (List<RandomOutput> row : randomOutput) {
            StringBuilder sb = new StringBuilder();
            for (RandomOutput output : row) {
                sb.append(output.getPropertyName()).append("->").append(output.getValueName()).append(" | ");
            }
            if (!row.isEmpty()) {
                sb.setLength(sb.length() - 3); // Remove last " | "
            }
            System.out.println(sb);
        }

        System.out.println("---------------------");
    }

}
