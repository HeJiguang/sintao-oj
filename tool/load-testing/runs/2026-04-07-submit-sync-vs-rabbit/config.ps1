$RunName = "2026-04-07-submit-sync-vs-rabbit"
$DefaultBaseUrl = "http://127.0.0.1:19090"
$DefaultVus = 20
$DefaultWarmupDuration = "20s"
$DefaultFormalDuration = "60s"
$DefaultPauseSeconds = 1

$SmokeQuestionId = 99992
$SmokeQuestionTitle = "Smoke Add OK"
$SmokeQuestionDifficulty = 1
$SmokeQuestionTimeLimit = 1000
$SmokeQuestionSpaceLimit = 268435456
$SmokeQuestionContent = "Smoke test question with relaxed memory limit"
$SmokeQuestionCaseJson = "[{`"input`":`"`",`"output`":`"3`"}]"
$SmokeQuestionDefaultCode = "public class Solution { public static int add(int a, int b) { return a + b; } }"
$SmokeQuestionMainFuc = "public static void main(String[] args) { System.out.print(add(1, 2)); }"

$DefaultJdbcUrl = "jdbc:mysql://101.96.200.76:3306/bitoj_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$DefaultJdbcUser = "sinmysql"
$DefaultJdbcPassword = "Mysql8@sin"

$DefaultJwtCode = "123456"
