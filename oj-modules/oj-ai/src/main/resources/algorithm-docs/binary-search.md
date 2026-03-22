# 二分查找（Binary Search）

## 概念

二分查找在**有序**序列中每次将待查区间缩小一半，从而在 O(log n) 时间内定位目标或判定不存在。

## 适用场景

- 数组或序列**有序**（或可视为有序）。
- 求满足某条件的**边界**（如第一个 ≥ x 的下标、最后一个 < x 的下标）。
- 答案具有**单调性**时，可将「判定问题」转化为二分（如二分答案）。

## 基本写法（闭区间 [left, right]）

- 循环条件：left <= right。
- 取中点：mid = left + (right - left) / 2，避免溢出。
- 根据 nums[mid] 与 target 的关系缩小 left 或 right。
- 若求「第一个 ≥ target」：若 nums[mid] >= target，则 right = mid - 1，否则 left = mid + 1；结束时 left 为第一个 ≥ target 的下标（若存在）。

## 常见变形

- **第一个等于 target**：判等时令 right = mid - 1，最后检查 left 是否越界且 nums[left]==target。
- **最后一个等于 target**：判等时令 left = mid + 1，最后检查 right 是否合法且 nums[right]==target。
- **二分答案**：在答案区间上二分，对每个 mid 写 check(mid)，根据结果缩小区间。

## 做题建议

先确认有序性，再明确要找的是「下标」还是「值」、是「第一个」还是「最后一个」，统一用闭区间或左闭右开一种写法避免混淆。
