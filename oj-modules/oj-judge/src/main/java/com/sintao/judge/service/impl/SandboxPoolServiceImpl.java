package com.sintao.judge.service.impl;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.sintao.common.core.constants.Constants;
import com.sintao.common.core.constants.JudgeConstants;
import com.sintao.common.core.enums.CodeRunStatus;
import com.sintao.judge.callback.DockerStartResultCallback;
import com.sintao.judge.callback.StatisticsCallback;
import com.sintao.judge.config.DockerSandBoxPool;
import com.sintao.judge.domain.CompileResult;
import com.sintao.judge.domain.SandBoxExecuteResult;
import com.sintao.judge.service.ISandboxPoolService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.StatsCmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SandboxPoolServiceImpl implements ISandboxPoolService {

    @Autowired
    private DockerSandBoxPool sandBoxPool;

    @Autowired
    private DockerClient dockerClient;

    private String containerId;

    private String userCodeFileName;

    @Value("${sandbox.limit.time:5}")
    private Long timeLimit;

    @Override
    public SandBoxExecuteResult exeJavaCode(Long userId, String userCode, List<String> inputList) {
        containerId = sandBoxPool.getContainer();
        createUserCodeFile(userCode);
        //编译代码
        CompileResult compileResult = compileCodeByDocker();
        if (!compileResult.isCompiled()) {
            sandBoxPool.returnContainer(containerId);
            deleteUserCodeFile();
            return SandBoxExecuteResult.fail(CodeRunStatus.COMPILE_FAILED, compileResult.getExeMessage());
        }
        //执行代码
        return executeJavaCodeByDocker(inputList);
    }

    //创建并返回用户代码的文件
    private void createUserCodeFile(String userCode) {
        String codeDir = sandBoxPool.getCodeDir(containerId);
        log.info("user-pool路径信息：{}", codeDir);
        userCodeFileName = codeDir + File.separator + JudgeConstants.USER_CODE_JAVA_CLASS_NAME;
        //如果文件之前存在，将之前的文件删除掉
        if (FileUtil.exist(userCodeFileName)) {
            FileUtil.del(userCodeFileName);
        }
        FileUtil.writeString(userCode, userCodeFileName, Constants.UTF8);
    }

    //编译
    //的使用docker编译
    private CompileResult compileCodeByDocker() {
        String cmdId = createExecCmd(JudgeConstants.DOCKER_JAVAC_CMD, null, containerId);
        DockerStartResultCallback resultCallback = new DockerStartResultCallback();
        CompileResult compileResult = new CompileResult();
        try {
            dockerClient.execStartCmd(cmdId)
                    .exec(resultCallback)
                    .awaitCompletion();
            if (CodeRunStatus.FAILED.equals(resultCallback.getCodeRunStatus())) {
                compileResult.setCompiled(false);
                compileResult.setExeMessage(resultCallback.getErrorMessage());
            } else {
                compileResult.setCompiled(true);
            }
            return compileResult;
        } catch (InterruptedException e) {
            //此处可以直接抛出 已做统一异常处理  也可再做定制化处�?            throw new RuntimeException(e);
        }
    }

    private SandBoxExecuteResult executeJavaCodeByDocker(List<String> inputList) {
        List<String> outList = new ArrayList<>(); //记录输出结果
        long maxMemory = 0L;  //最大占用内�?        long maxUseTime = 0L; //最大运行时�?        //执行用户代码
        for (String inputArgs : inputList) {
            String cmdId = createExecCmd(JudgeConstants.DOCKER_JAVA_EXEC_CMD, inputArgs, containerId);
            //执行代码
            StopWatch stopWatch = new StopWatch();        //执行代码后开始计�?            //执行情况监控
            StatsCmd statsCmd = dockerClient.statsCmd(containerId); //启动监控
            StatisticsCallback statisticsCallback = statsCmd.exec(new StatisticsCallback());
            stopWatch.start();
            DockerStartResultCallback resultCallback = new DockerStartResultCallback();
            try {
                dockerClient.execStartCmd(cmdId)
                        .exec(resultCallback)
                        .awaitCompletion(timeLimit, TimeUnit.SECONDS);
                if (CodeRunStatus.FAILED.equals(resultCallback.getCodeRunStatus())) {
                    //未通过所有用例返回结�?                    return SandBoxExecuteResult.fail(CodeRunStatus.NOT_ALL_PASSED);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            stopWatch.stop();  //结束时间统计
            statsCmd.close();  //结束docker容器执行统计
            long userTime = stopWatch.getLastTaskTimeMillis(); //执行耗时
            maxUseTime = Math.max(userTime, maxUseTime);       //记录最大的执行用例耗时
            Long memory = statisticsCallback.getMaxMemory();
            if (memory != null) {
                maxMemory = Math.max(maxMemory, statisticsCallback.getMaxMemory()); //记录最大的执行用例占用内存
            }
            outList.add(resultCallback.getMessage().trim());   //记录正确的输出结�?        }
        sandBoxPool.returnContainer(containerId);
        deleteUserCodeFile(); //清理文件

        return getSanBoxResult(inputList, outList, maxMemory, maxUseTime); //封装结果
    }

    private String createExecCmd(String[] javaCmdArr, String inputArgs, String containerId) {
        if (!StrUtil.isEmpty(inputArgs)) {
            //当入参不为空时拼接入�?            String[] inputArray = inputArgs.split(" "); //入参
            javaCmdArr = ArrayUtil.append(JudgeConstants.DOCKER_JAVA_EXEC_CMD, inputArray);
        }
        ExecCreateCmdResponse cmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(javaCmdArr)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .exec();
        return cmdResponse.getId();
    }

    private SandBoxExecuteResult getSanBoxResult(List<String> inputList, List<String> outList,
                                                 long maxMemory, long maxUseTime) {
        if (inputList.size() != outList.size()) {
            //输入用例数量 不等�?输出用例数量  属于执行异常
            return SandBoxExecuteResult.fail(CodeRunStatus.NOT_ALL_PASSED, outList, maxMemory, maxUseTime);
        }
        return SandBoxExecuteResult.success(CodeRunStatus.SUCCEED, outList, maxMemory, maxUseTime);
    }

    private void deleteUserCodeFile() {
        FileUtil.del(userCodeFileName);
    }
}

