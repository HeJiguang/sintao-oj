package com.sintao.judge.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.sintao.common.core.constants.Constants;
import com.sintao.common.core.constants.JudgeConstants;
import com.sintao.common.core.enums.CodeRunStatus;
import com.sintao.judge.callback.DockerStartResultCallback;
import com.sintao.judge.callback.StatisticsCallback;
import com.sintao.judge.domain.CompileResult;
import com.sintao.judge.domain.SandBoxExecuteResult;
import com.sintao.judge.service.ISandboxService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SandboxServiceImpl implements ISandboxService {

    @Value("${sandbox.docker.host:tcp://localhost:2375}")
    private String dockerHost;

    @Value("${sandbox.docker.image:eclipse-temurin:8-jdk-alpine}")
    private String sandboxImage;

    @Value("${sandbox.limit.memory:100000000}")
    private Long memoryLimit;

    @Value("${sandbox.limit.memory-swap:100000000}")
    private Long memorySwapLimit;

    @Value("${sandbox.limit.cpu:1}")
    private Long cpuLimit;

    @Value("${sandbox.limit.time:5}")
    private Long timeLimit;

    private DockerClient dockerClient;

    private String containerId;

    private String userCodeDir;

    private String userCodeFileName;

    @Override
    public SandBoxExecuteResult exeJavaCode(Long userId, String userCode, List<String> inputList) {
        // 在docker中创建并写入用户代码
        createUserCodeFile(userId, userCode);
        //
        initDockerSanBox();
        //编译代码
        CompileResult compileResult = compileCodeByDocker();
        if (!compileResult.isCompiled()) {
            deleteContainer();
            deleteUserCodeFile();
            return SandBoxExecuteResult.fail(CodeRunStatus.COMPILE_FAILED, compileResult.getExeMessage());
        }
        //执行代码
        return executeJavaCodeByDocker(inputList);
    }


    //创建并返回用户代码的文件
    private void createUserCodeFile(Long userId, String userCode) {
        String examCodeDir = System.getProperty("user.dir") + File.separator + JudgeConstants.EXAM_CODE_DIR;
        if (!FileUtil.exist(examCodeDir)) {
            FileUtil.mkdir(examCodeDir); //创建存放用户代码的目�?        }
        String time = LocalDateTimeUtil.format(LocalDateTime.now(), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        //拼接用户代码文件格式
        userCodeDir = examCodeDir + File.separator + userId + Constants.UNDERLINE_SEPARATOR + time;
        userCodeFileName = userCodeDir + File.separator + JudgeConstants.USER_CODE_JAVA_CLASS_NAME;
        FileUtil.writeString(userCode, userCodeFileName, Constants.UTF8);
    }

    private void initDockerSanBox() {
        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();
        dockerClient = DockerClientBuilder
                .getInstance(clientConfig)
                .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
                .build();
        //拉取镜像
        pullJavaEnvImage();
        //创建容器  限制资源   控制权限
        HostConfig hostConfig = getHostConfig();
        CreateContainerCmd containerCmd = dockerClient
                .createContainerCmd(sandboxImage)
                .withName(JudgeConstants.JAVA_CONTAINER_NAME);
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        //记录容器id
        containerId = createContainerResponse.getId();
        //启动容器
        dockerClient.startContainerCmd(containerId).exec();
    }

    //拉取java执行环境镜像 需要控制只拉取一�?    private void pullJavaEnvImage() {
        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
        List<Image> imageList = listImagesCmd.exec();
        for (Image image : imageList) {
            String[] repoTags = image.getRepoTags();
            if (repoTags != null && repoTags.length > 0 && sandboxImage.equals(repoTags[0])) {
                return;
            }
        }
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(sandboxImage);
        try {
            pullImageCmd.exec(new PullImageResultCallback()).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    //限制资源   控制权限
    private HostConfig getHostConfig() {
        HostConfig hostConfig = new HostConfig();
        //设置挂载目录，指定用户代码路�?        hostConfig.setBinds(new Bind(userCodeDir, new Volume(JudgeConstants.DOCKER_USER_CODE_DIR)));
        //限制docker容器使用资源
        hostConfig.withMemory(memoryLimit);
        hostConfig.withMemorySwap(memorySwapLimit);
        hostConfig.withCpuCount(cpuLimit);
        hostConfig.withNetworkMode("none");  //禁用网络
        hostConfig.withReadonlyRootfs(true); //禁止在root目录写文�?        return hostConfig;
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
        deleteContainer();//删除容器
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

    private void deleteContainer() {
        //执行完成之后删除容器
        dockerClient.stopContainerCmd(containerId).exec();
        dockerClient.removeContainerCmd(containerId).exec();
        //断开和docker连接
        try {
            dockerClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteUserCodeFile() {
        FileUtil.del(userCodeDir);
    }
}

