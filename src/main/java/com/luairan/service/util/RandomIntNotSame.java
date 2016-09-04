package com.luairan.service.util;

import java.util.Random;

/**
 * 产生某个范围内的随机数
 *
 * @author luairan
 */
public class RandomIntNotSame {

    private final Random random;
    private final int startNum;
    private boolean[] innerFlag;

    public RandomIntNotSame(int start, int max) {
        random = new Random();
        startNum = start;
        innerFlag = new boolean[max - start + 1];
    }

    public RandomIntNotSame(int max) {
        this(0, max);
    }

    public static void main(String[] args) {
        RandomIntNotSame s = new RandomIntNotSame(4);
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.println(s.nextInt());
            }
            s.resetRadom();
            System.out.println("-------------");
        }
    }

    public int nextInt() {
        int max = innerFlag.length - 1;
        int rand = random.nextInt(max);
        int oldrand = rand;
        while (innerFlag[rand]) {
            rand = (++rand) % innerFlag.length;
            if (oldrand == rand) {
                innerFlag = new boolean[innerFlag.length];
                rand = random.nextInt(max);
            }
        }
        innerFlag[rand] = true;
        return rand + startNum;
    }

    public void resetRadom() {
        innerFlag = new boolean[innerFlag.length];
    }
}
