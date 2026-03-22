rm E:/bitepro/sintao-oj/deploy/test/bitoj-jar/gateway/oj-gateway.jar
rm E:/bitepro/sintao-oj/deploy/test/bitoj-jar/friend/oj-friend.jar
rm E:/bitepro/sintao-oj/deploy/test/bitoj-jar/job/oj-job.jar
rm E:/bitepro/sintao-oj/deploy/test/bitoj-jar/judge/oj-judge.jar
rm E:/bitepro/sintao-oj/deploy/test/bitoj-jar/system/oj-system.jar

copy E:/bitepro/sintao-oj/oj-gateway/target/oj-gateway-1.0-SNAPSHOT.jar E:/bitepro/sintao-oj/deploy/test/bitoj-jar/gateway/oj-gateway.jar
copy E:/bitepro/sintao-oj/oj-modules/oj-judge/target/oj-judge-1.0-SNAPSHOT.jar E:/bitepro/sintao-oj/deploy/test/bitoj-jar/judge/oj-judge.jar
copy E:/bitepro/sintao-oj/oj-modules/oj-friend/target/oj-friend-1.0-SNAPSHOT.jar E:/bitepro/sintao-oj/deploy/test/bitoj-jar/friend/oj-friend.jar
copy E:/bitepro/sintao-oj/oj-modules/oj-job/target/oj-job-1.0-SNAPSHOT.jar E:/bitepro/sintao-oj/deploy/test/bitoj-jar/job/oj-job.jar
copy E:/bitepro/sintao-oj/oj-modules/oj-system/target/oj-system-1.0-SNAPSHOT.jar E:/bitepro/sintao-oj/deploy/test/bitoj-jar/system/oj-system.jar
pause
