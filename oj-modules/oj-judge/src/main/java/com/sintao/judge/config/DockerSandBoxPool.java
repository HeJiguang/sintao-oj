package com.sintao.judge.config;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import com.sintao.common.core.constants.JudgeConstants;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Volume;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class DockerSandBoxPool {

    private DockerClient dockerClient;

    private String sandboxImage;

    private String volumeDir;

    private Long memoryLimit;

    private Long memorySwapLimit;

    private Long cpuLimit;

    private int poolSize;

    private String containerNamePrefix;

    private BlockingQueue<String> containerQueue;

    private Map<String, String> containerNameMap;

    public DockerSandBoxPool(DockerClient dockerClient,
                             String sandboxImage,
                             String volumeDir, Long memoryLimit,
                             Long memorySwapLimit, Long cpuLimit,
                             int poolSize, String containerNamePrefix) {
        this.dockerClient = dockerClient;
        this.sandboxImage = sandboxImage;
        this.volumeDir = volumeDir;
        this.memoryLimit = memoryLimit;
        this.memorySwapLimit = memorySwapLimit;
        this.cpuLimit = cpuLimit;
        this.poolSize = poolSize;
        this.containerQueue = new ArrayBlockingQueue<>(poolSize);
        this.containerNamePrefix = containerNamePrefix;
        this.containerNameMap = new HashMap<>();
    }

    public void initDockerPool() {  // 初始化容器池
        log.info("------ 创建容器开始 ------");
            createContainer(containerNamePrefix + "-" + i);
        }
        log.info("------ 创建容器结束 ------");
    }

    public String getContainer() {
        try {
            return containerQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void returnContainer(String containerId) {
        containerQueue.add(containerId);
    }

    private void createContainer(String containerName) {
        List<Container> containerList = dockerClient.listContainersCmd().withShowAll(true).exec();
        if (!CollectionUtil.isEmpty(containerList)) {
            String names = JudgeConstants.JAVA_CONTAINER_PREFIX + containerName;
            for (Container container : containerList) {
                String[] containerNames = container.getNames();
                if (containerNames != null && containerNames.length > 0 && names.equals(containerNames[0])) {
                    if ("created".equals(container.getState()) || "exited".equals(container.getState())) {
                        // 启动容器
                        dockerClient.startContainerCmd(container.getId()).exec();
                    }
                    containerQueue.add(container.getId());
                    containerNameMap.put(container.getId(), containerName);
                    return;
                }
            }
        }

        // 拉取镜像
        pullJavaEnvImage();
        // 创建容器，限制资源和权限
        HostConfig hostConfig = getHostConfig(containerName);
        CreateContainerCmd containerCmd = dockerClient
                .createContainerCmd(sandboxImage)
                .withName(containerName);
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        // 记录容器 ID
        String containerId = createContainerResponse.getId();
        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        containerQueue.add(containerId);
        containerNameMap.put(containerId, containerName);
    }

    private void pullJavaEnvImage() {
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

    // 限制资源和控制权限
    private HostConfig getHostConfig(String containerName) {
        HostConfig hostConfig = new HostConfig();
        // 设置挂载目录，指定用户代码路径
        String userCodeDir = createContainerDir(containerName);
        //闄愬埗docker瀹瑰櫒浣跨敤璧勬簮
        // 限制 Docker 容器使用资源
        hostConfig.withMemorySwap(memorySwapLimit);
        hostConfig.withCpuCount(cpuLimit);
        hostConfig.withNetworkMode("none");  // 禁用网络
        hostConfig.withReadonlyRootfs(true); // 禁止在 root 目录写文件
        return hostConfig;

    public String getCodeDir(String containerId) {
        String containerName = containerNameMap.get(containerId);
        log.info("containerName锛歿}", containerName);
        return System.getProperty("user.dir") + File.separator + JudgeConstants.CODE_DIR_POOL + File.separator + containerName;
    }

    // 为每个容器创建指定挂载目录
    private String createContainerDir(String containerName) {
        // 一级目录，存放所有容器的挂载目录
        if (!FileUtil.exist(codeDir)) {
            FileUtil.mkdir(codeDir);
        }
        return codeDir + File.separator + containerName;
    }
}

