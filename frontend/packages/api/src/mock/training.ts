import type { ExamSummary, TrainingSnapshot } from "../contracts";

export const trainingSnapshot: TrainingSnapshot = {
  title: "哈希表与区间题稳定训练",
  direction: "后端开发 / 算法基础",
  level: "Lv.4 稳定提升期",
  streakDays: 12,
  weeklyGoal: "本周完成 8 题，并做 1 次阶段测复盘",
  completionRate: 68,
  strengths: ["哈希表模板切换比较稳", "基础数组题提交节奏较好", "最近复盘执行率明显提升"],
  weaknesses: ["复杂边界 case 还容易漏判", "区间题模板还没有完全固化", "回溯题的状态管理容易写散"],
  tasks: [
    {
      taskId: "task-1",
      title: "完成两数之和并总结“先查后存”模板",
      status: "进行中",
      focus: "补数映射 / 一次遍历",
      difficulty: "Easy"
    },
    {
      taskId: "task-2",
      title: "整理合并区间与插入区间的统一解法",
      status: "待开始",
      focus: "排序 + 扫描",
      difficulty: "Medium"
    },
    {
      taskId: "task-3",
      title: "用一场 60 分钟小测验证回溯题稳定性",
      status: "待开始",
      focus: "搜索树拆解",
      difficulty: "Hard"
    }
  ]
};

export const exams: ExamSummary[] = [
  {
    examId: "exam-sprint-01",
    title: "周末算法冲刺测",
    status: "未开始",
    startTime: "2026-03-30 19:30",
    endTime: "2026-03-30 21:00",
    durationMinutes: 90,
    questionCount: 3
  },
  {
    examId: "exam-checkpoint-02",
    title: "数组与哈希阶段测",
    status: "进行中",
    startTime: "2026-03-29 20:00",
    endTime: "2026-03-29 21:00",
    durationMinutes: 60,
    questionCount: 2
  },
  {
    examId: "exam-review-03",
    title: "回溯专题复盘测",
    status: "已结束",
    startTime: "2026-03-27 19:00",
    endTime: "2026-03-27 20:00",
    durationMinutes: 60,
    questionCount: 2
  }
];
