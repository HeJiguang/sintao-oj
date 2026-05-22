$RunName = "2026-04-07-judge-standalone-vs-pool"
$DefaultBaseUrl = if ($env:OJ_JUDGE_BASE_URL) { $env:OJ_JUDGE_BASE_URL } else { "http://127.0.0.1:9204" }
$DefaultVus = 10
$DefaultWarmupDuration = "15s"
$DefaultFormalDuration = "45s"
$DefaultPauseSeconds = 0

$JudgeUserId = 99999
$JudgeQuestionId = 99992
$JudgeProgramType = 0
$JudgeDifficulty = 1
$JudgeTimeLimit = 1000
$JudgeSpaceLimit = 268435456
$JudgeUserCode = "public class Solution { public static int add(int a, int b) { return a + b; } public static void main(String[] args) { System.out.print(add(1, 2)); } }"
$JudgeInputJson = '[""]'
$JudgeOutputJson = '["3"]'
