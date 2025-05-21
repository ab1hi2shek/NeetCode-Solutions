package lld.iterator;

import java.util.*;

public class ZigzagIterator {

    List<List<Integer>> lists;
    Map<Integer, Integer> map = new HashMap<>();
    int currentList;
    int listCount;

    public ZigzagIterator(List<List<Integer>> lists) {
        this.lists = lists;

        currentList = 0;
        listCount = lists.size();

        for (int i = 0; i < listCount; i++) {
            map.put(i, 0);
        }
    }

    public int next() {

        int index = map.get(currentList);
        int result = lists.get(currentList).get(index);

        map.put(currentList, index + 1);
        currentList = (currentList + 1) % listCount;

        return result;

    }

    public boolean hasNext() {
        int start = currentList;
        while (map.get(currentList) == lists.get(currentList).size()) {
            currentList = (currentList + 1) % listCount;
            if (start == currentList)
                return false;
        }

        return true;

    }

    public static void main(String[] args) {
        List<Integer> v1 = List.of(1, 2, 3, 4);
        List<Integer> v2 = List.of(5, 6, 7, 8, 9, 10, 11);
        List<Integer> v3 = List.of(12);
        List<Integer> v4 = List.of(13, 14);

        List<List<Integer>> input = new ArrayList<>();
        input.add(v1);
        input.add(v2);
        input.add(v3);
        input.add(v4);

        ZigzagIterator zigzagIterator = new ZigzagIterator(input);

        while (zigzagIterator.hasNext()) {
            System.out.println(zigzagIterator.next());
        }
    }

}
