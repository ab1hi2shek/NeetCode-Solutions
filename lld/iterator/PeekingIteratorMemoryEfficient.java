package lld.iterator;

import java.util.*;

public class PeekingIteratorMemoryEfficient implements Iterator<Integer> {

    private Iterator<Integer> iterator;
    private Integer nextElement;
    private boolean hasPeeked;

    public PeekingIteratorMemoryEfficient(Iterator<Integer> iterator) {
        this.iterator = iterator;
        this.hasPeeked = false;
        this.nextElement = null;
    }

    @Override
    public boolean hasNext() {
        return hasPeeked || iterator.hasNext();
    }

    @Override
    public Integer next() {
        if (!hasPeeked) {
            if (iterator.hasNext()) {
                return iterator.next();
            } else {
                throw new NoSuchElementException("next function, no such element");
            }
        }

        Integer val = nextElement;
        hasPeeked = false;
        nextElement = null;
        return val;
    }

    // Returns the next element in the iteration without advancing the iterator.
    public Integer peek() {
        if (!hasPeeked) {
            if (iterator.hasNext()) {
                nextElement = iterator.next();
                hasPeeked = true;
            } else {
                throw new NoSuchElementException("peek function, no such element");
            }
        }

        return nextElement;
    }

    public static void main(String[] args) {

        PeekingIteratorMemoryEfficient it = new PeekingIteratorMemoryEfficient(List.of(10, 20, 30, 40).iterator());

        System.out.println(it.peek()); // should return 10, without moving forward
        System.out.println(it.next()); // should also return 10, and now move forward
        System.out.println(it.peek()); // should return 20
        System.out.println(it.peek()); // should return 20
        System.out.println(it.next()); // should return 20
        System.out.println(it.next()); // should return 30
        System.out.println(it.peek()); // should return 40
        System.out.println(it.peek()); // should return 40
        System.out.println(it.next()); // should return 40

        List<Integer> list = List.of(10);
        PeekingIteratorMemoryEfficient it1 = new PeekingIteratorMemoryEfficient(list.iterator());

        System.out.println(it1.peek()); // Peek 10, caches it, no call to next() yet
        System.out.println(it1.hasNext()); // <-- What does this print?
        while (it1.hasNext()) {
            System.out.println(it1.next()); // <-- What does this print?
        }

    }

}
