package array;

public class CountTrailingZeroes {

    // Function to count trailing zeroes in n!
    public static int countTrailingZeroes(int n) {
        int count = 0;

        // Keep dividing n by powers of 5
        for (int i = 5; n / i >= 1; i *= 5) {
            count += n / i;
        }

        return count;
    }

    public static void main(String[] args) {
        int n = 100; // Change this to test other values
        System.out.println("Trailing zeroes in " + n + "! = " + countTrailingZeroes(n));

        int n1 = 250; // Change this to test other values
        System.out.println("Trailing zeroes in " + n1 + "! = " + countTrailingZeroes(n1));
    }

    /**
     * How this works
     * 
     * ------ For n = 100: -------
     * 100 / 5 = 20 → counts how many multiples of 5 there are.
     * 20 / 5 = 4 → counts how many multiples of 25 (5²) there are (each contributes
     * an extra 5).
     * 4 / 5 = 0 → stops here.
     * Total trailing zeroes = 20 + 4 = 24
     * 
     * -------- For n = 250: -----------
     * 250 / 5 = 50 → multiples of 5
     * 50 / 5 = 10 → multiples of 25
     * 10 / 5 = 2 → multiples of 125
     * 2 / 5 = 0 → stop
     * Total trailing zeroes = 50 + 10 + 2 = 62
     */

}
