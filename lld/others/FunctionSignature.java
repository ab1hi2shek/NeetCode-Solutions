package lld.others;

import java.util.*;

class FunctionSignature {
    private String name;
    private List<String> parameterTypes;
    private boolean isVariadic;

    public FunctionSignature(String name, List<String> parameterTypes, boolean isVariadic) {
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.isVariadic = isVariadic;
    }

    public String getName() {
        return name;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public boolean isVariadic() {
        return isVariadic;
    }
}

class FunctionMatcher {

    static class TestCase {
        List<String> query;
        List<String> expected;

        TestCase(List<String> query, List<String> expected) {
            this.query = query;
            this.expected = expected;
        }
    }

    /**
     * Matches the given query against the list of function signatures.
     *
     * @param functions List of FunctionSignature objects.
     * @param query     List of argument types in the query.
     * @return List of function names that match the query.
     */
    public List<String> matchFunctions(List<FunctionSignature> functions, List<String> query) {

        List<String> result = new ArrayList<>();

        for (FunctionSignature fun : functions) {
            List<String> parameterTypes = fun.getParameterTypes();
            int paramCount = parameterTypes.size();
            int queryCount = query.size();

            // Case 1: Exact match for non-variadic functions
            if (!fun.isVariadic() && queryCount == paramCount && query.equals(parameterTypes)) {
                result.add(fun.getName());
            }
            // Case 3: Function with no parameters should only match empty queries
            else if (paramCount == 0 && queryCount == 0) {
                result.add(fun.getName());
            } else if (paramCount == 0) {
                continue;
            }
            // Case 2: Variadic function matching
            else if (fun.isVariadic() && queryCount >= paramCount) {
                List<String> fixedParameters = parameterTypes.subList(0, paramCount);
                List<String> queryFixedPart = query.subList(0, paramCount);

                // Check if fixed parameters match
                if (fixedParameters.equals(queryFixedPart)) {
                    // Check if variadic parameters match the type of the last fixed parameter
                    List<String> queryExtraPart = query.subList(paramCount, queryCount);
                    String lastFixedParamType = parameterTypes.get(paramCount - 1);
                    boolean validExtraParams = queryExtraPart.stream().allMatch(p -> p.equals(lastFixedParamType));

                    if (validExtraParams) {
                        result.add(fun.getName());
                    }
                }
            }
        }

        return result;
    }

    public static void main(String[] args) {
        // Define function signatures
        List<FunctionSignature> functions = Arrays.asList(
                new FunctionSignature("funA", Arrays.asList("int", "bool"), true),
                new FunctionSignature("funB", Arrays.asList("int", "int"), false),
                new FunctionSignature("funC", Arrays.asList("int", "int", "int"), true),
                new FunctionSignature("funD", Arrays.asList("int"), true),
                new FunctionSignature("funE", Collections.emptyList(), false),
                new FunctionSignature("funF", Collections.emptyList(), true));

        // Define test cases
        List<TestCase> testCases = Arrays.asList(
                new TestCase(Arrays.asList("int", "bool"), Arrays.asList("funA")),
                new TestCase(Arrays.asList("int", "int"), Arrays.asList("funB")),
                new TestCase(Arrays.asList("int", "int", "int", "int"), Arrays.asList("funC")),
                new TestCase(Arrays.asList("int", "int", "int", "int", "int"), Arrays.asList("funC")),
                new TestCase(Arrays.asList("int"), Arrays.asList("funD")),
                new TestCase(Arrays.asList("int", "string"), Arrays.asList("funD")),
                new TestCase(Collections.emptyList(), Arrays.asList("funE", "funF")),
                new TestCase(Arrays.asList("int", "bool", "string"), Arrays.asList("funA")),
                new TestCase(Arrays.asList("int", "bool", "string", "float"), Arrays.asList("funA")),
                new TestCase(Arrays.asList("int", "int", "int"), Arrays.asList("funC")),
                new TestCase(Arrays.asList("bool"), Collections.emptyList()),
                new TestCase(Arrays.asList("int", "int", "bool"), Collections.emptyList()));

        // Create an instance of FunctionMatcher
        FunctionMatcher matcher = new FunctionMatcher();

        // Run test cases
        int testNum = 1;
        for (TestCase testCase : testCases) {
            List<String> actual = matcher.matchFunctions(functions, testCase.query);
            List<String> expected = new ArrayList<>(testCase.expected);
            Collections.sort(actual);
            Collections.sort(expected);
            System.out.println("Test " + testNum + " - Query: " + testCase.query);
            System.out.println("Expected: " + expected);
            System.out.println("Actual  : " + actual);

            if (!actual.equals(expected)) {
                System.out.println("❌ Test " + testNum + " FAILED\n");
            } else {
                System.out.println("✅ Test " + testNum + " passed\n");
            }
            testNum++;
        }
    }
}
