$RunName = "2026-04-07-polling-vs-websocket"
$BootstrapRunName = "2026-04-07-submit-sync-vs-rabbit"

$DefaultBaseUrl = "http://127.0.0.1:19090"
$DefaultSamples = 10
$DefaultPollIntervalMs = 1500
$DefaultPollMaxAttempts = 6
$DefaultTimeoutMs = 15000
$DefaultPauseMs = 250

$SmokeQuestionId = 99992
$SmokeQuestionProgramType = 0
$SmokeQuestionUserCode = "public class Solution { public static int add(int a, int b) { return a + b; } }"
