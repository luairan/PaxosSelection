package com.luairan.service.util;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * 产生某个范围内的几个随机数.
 *
 * @author luairan
 */
public class RandomIntPickNumNotSame {

    private final Random random;
    private final int startNum;
    private final int maxNum;
    private Set<Integer> containsSet = new HashSet<Integer>();

    public RandomIntPickNumNotSame(int start, int max) {
        random = new Random();
        startNum = start;
        maxNum = max;
    }

    public RandomIntPickNumNotSame(int max) {
        this(0, max);
    }

    public static void main(String[] args) {
        RandomIntPickNumNotSame ss = new RandomIntPickNumNotSame(1, 10);
        for (int i = 0; i < 6; i++)
            System.out.println(ss.nextInt());
        ss.resetRandom();
        for (int i = 0; i < 6; i++)
            System.out.println(ss.nextInt());
    }

    public int nextInt() {
        int rand = random.nextInt(maxNum - startNum);
        int oldrand = rand;
        while (containsSet.contains(Integer.valueOf(rand + startNum))) {
            rand = (++rand) % (maxNum - startNum + 1);
            if (oldrand == rand) {
                containsSet.clear();
                rand = random.nextInt(maxNum - startNum);
            }
        }
        containsSet.add(rand + startNum);
        return rand + startNum;
    }

    public void resetRandom() {
        containsSet.clear();
    }
}
