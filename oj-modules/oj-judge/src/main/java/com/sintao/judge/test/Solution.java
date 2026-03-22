package com.sintao.judge.test;

import lombok.extern.slf4j.Slf4j;

import java.util.Stack;

public class Solution {

    public static int twoSum(int var0, int var1) {
        return var0 + var1;
    }

    public static int twoDiff(int var0, int var1) {
        return var0 - var1;
    }

    public static boolean isValid(String str) {
        Stack<Character> stack = new Stack<>();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else if (c == ')' || c == ']' || c == '}') {
                if (stack.isEmpty()) {
                    return false;
                }
                char topChar = stack.pop();
                if ((c == ')' && topChar != '(') ||
                        (c == ']' && topChar != '[') ||
                        (c == '}' && topChar != '{')) {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }

    public static boolean isPrime(int num) {
        if (num <= 1) {
            return false;
        }
        if (num == 2) {
            return true;
        }
        if (num % 2 == 0) {
            return false;
        }
        int sqrtNum = (int) Math.sqrt(num);
        for (int i = 3; i <= sqrtNum; i += 2) {
            if (num % i == 0) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {

    }
}

/**
 * 娴嬭瘯鏁版嵁
 * {
 *   "questionId": 1794933791345602562,
 *   "programType": "0",
 *   "userCode": "public class Solution { \n\n public static int twoSum(int var0, int var1) { \n\n return var0 + var1;\n}\n }"
 * }
 *
 */
