package lld.iterator;

import java.util.*;

interface CustomIterator {
    int next();

    boolean hasNext();
}

public class IteratorImpl {

    public static class ZigzagIterator implements CustomIterator {

        private final List<List<Integer>> vectors;
        // int[0] = vector index, int[1] = position in that vector
        Queue<int[]> queue;

        private ZigzagIterator(List<List<Integer>> vectors) {
            this.vectors = vectors;
            this.queue = new LinkedList<>();
            for (int i = 0; i < vectors.size(); i++) {
                if (vectors.get(i).size() > 0) {
                    queue.offer(new int[] { i, 0 });
                }
            }
        }

        @Override
        public int next() {
            int[] current = queue.poll();
            int nextIndex = current[1] + 1;
            if (nextIndex < vectors.get(current[0]).size()) {
                queue.offer(new int[] { current[0], nextIndex });
            }

            return vectors.get(current[0]).get(current[1]);
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }
    }

    public static class RangeIterator implements CustomIterator {

        private final List<Integer> vector;
        private final int end;
        private final int step;
        private int current;

        private RangeIterator(List<Integer> vector, int start, int end, int step) {

            if (start < 0 || end < 0 || start >= vector.size() || end > vector.size()) {
                throw new IllegalArgumentException("Not valid start = " + start + " or end =" + end);
            }

            if (step == 0) {
                throw new IllegalArgumentException("Step cannot be zero.");
            }

            this.vector = vector;
            this.end = end;
            this.step = step;
            this.current = start;
        }

        @Override
        public int next() {
            int result = vector.get(current);
            current += step;
            return result;
        }

        @Override
        public boolean hasNext() {
            if (step >= 0) {
                return current <= Math.min(end, vector.size() - 1);
            } else {
                return current >= Math.max(0, end);
            }
        }

    }

    public static class SimpleIterator implements CustomIterator {
        private final List<Integer> vector;
        private int current;

        private SimpleIterator(List<Integer> vector) {

            this.vector = vector;
            this.current = 0;
        }

        @Override
        public int next() {
            return vector.get(current++);
        }

        @Override
        public boolean hasNext() {
            return current < vector.size();
        }
    }

    public static class CustomZigzagIterator implements CustomIterator {

        private final Queue<CustomIterator> queue;

        private CustomZigzagIterator(List<CustomIterator> iterators) {
            this.queue = new LinkedList<>();
            for (CustomIterator iterator : iterators) {
                enqueueIfHasNext(iterator);
            }
        }

        @Override
        public int next() {
            CustomIterator customIterator = queue.poll();
            int val = customIterator.next();
            enqueueIfHasNext(customIterator);

            return val;
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        private void enqueueIfHasNext(CustomIterator iterator) {
            if (iterator.hasNext()) {
                queue.offer(iterator);
            }
        }
    }

    public static void main(String[] args) {
        List<Integer> v1 = List.of(1, 2, 3, 4);
        List<Integer> v2 = List.of(5, 6, 7, 8, 9, 10, 11);
        List<Integer> v3 = List.of(12);
        List<Integer> v4 = List.of(13, 14);

        List<List<Integer>> input = List.of(v1, v2, v3, v4);

        System.out.println("Zignzag Iterator --------------------\n");

        ZigzagIterator zigzagIterator = new ZigzagIterator(input);
        while (zigzagIterator.hasNext()) {
            System.out.print(zigzagIterator.next() + " ");
        }

        System.out.println("\nrangeIterator --------------------\n");
        RangeIterator rangeIterator = new RangeIterator(v1, 0, v1.size() - 1, 2);
        while (rangeIterator.hasNext()) {
            System.out.print(rangeIterator.next() + " ");
        }

        System.out.println("\nrangeIterator1 --------------------\n");
        RangeIterator rangeIterator1 = new RangeIterator(v2, 0, 0, 3);
        while (rangeIterator1.hasNext()) {
            System.out.print(rangeIterator1.next() + " ");
        }

        System.out.println("\nrangeIterator2 --------------------\n");
        RangeIterator rangeIterator2 = new RangeIterator(v2, v2.size() - 1, 0, -2);
        while (rangeIterator2.hasNext()) {
            System.out.print(rangeIterator2.next() + " ");
        }

        System.out.println("\nsimpleIterator --------------------\n");
        SimpleIterator simpleIterator = new SimpleIterator(v1);
        while (simpleIterator.hasNext()) {
            System.out.print(simpleIterator.next() + " ");
        }

        System.out.println("\nsimpleIterator1 --------------------\n");
        SimpleIterator simpleIterator1 = new SimpleIterator(v2);
        while (simpleIterator1.hasNext()) {
            System.out.print(simpleIterator1.next() + " ");
        }

        System.out.println("\nCustomZigzagIterator with RangeIterator and SimpleIterator --------------------\n");

        CustomIterator it1 = new SimpleIterator(List.of(1, 2, 3));
        CustomIterator it2 = new RangeIterator(List.of(10, 11, 12, 13, 14, 15), 0, 5, 2); // Yields 10, 12, 14
        CustomIterator it3 = new SimpleIterator(List.of(100, 200));

        List<CustomIterator> iterators = List.of(it1, it2, it3);
        CustomZigzagIterator customZigzag = new CustomZigzagIterator(iterators);

        while (customZigzag.hasNext()) {
            System.out.print(customZigzag.next() + " ");
        }

    }

}
