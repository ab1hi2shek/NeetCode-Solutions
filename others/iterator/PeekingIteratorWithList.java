package others.iterator;

import java.util.*;

public class PeekingIteratorWithList implements Iterator<Integer> {

    private List<Integer> list;
    private int currIndex;

    public PeekingIteratorWithList(Iterator<Integer> iterator) {
        list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }

        currIndex = 0;
    }

    @Override
    public boolean hasNext() {
        return currIndex < list.size();
    }

    @Override
    public Integer next() {
        return list.get(currIndex++);
    }

    // Returns the next element in the iteration without advancing the iterator.
    public Integer peek() {
        return list.get(currIndex);
    }

    public static void main(String[] args) {

        PeekingIteratorWithList it = new PeekingIteratorWithList(List.of(10, 20, 30, 40).iterator());

        System.out.println(it.peek()); // should return 10, without moving forward
        System.out.println(it.next()); // should also return 10, and now move forward
        System.out.println(it.peek()); // should return 20
        System.out.println(it.peek()); // should return 20
        System.out.println(it.next()); // should return 20
        System.out.println(it.next()); // should return 30
    }

}
