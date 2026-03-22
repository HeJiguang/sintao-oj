package com.sintao.common.file.service;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.ObjectId;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.sintao.common.core.constants.CacheConstants;
import com.sintao.common.core.constants.Constants;
import com.sintao.common.core.enums.ResultCode;
import com.sintao.common.core.utils.ThreadLocalUtil;
import com.sintao.common.file.config.OSSProperties;
import com.sintao.common.file.domain.OSSResult;
import com.sintao.common.redis.service.RedisService;
import com.sintao.common.security.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RefreshScope
public class OSSService {

    @Autowired
    private OSSProperties prop;

    @Autowired
    private OSSClient ossClient;

    @Autowired
    private RedisService redisService;

    @Value("${file.max-time}")
    private int maxTime;

    @Value("${file.test}")
    private boolean test;

    public OSSResult uploadFile(MultipartFile file) throws Exception {
        if (!test) {
            checkUploadCount();
        }
        InputStream inputStream = null;
        try {
            String fileName;
            if (file.getOriginalFilename() != null) {
                fileName = file.getOriginalFilename().toLowerCase();
            } else {
                fileName = "a.png";
            }
            String extName = fileName.substring(fileName.lastIndexOf(".") + 1);
            inputStream = file.getInputStream();
            return upload(extName, inputStream);
        } catch (Exception e) {
            log.error("OSS upload file error", e);
            throw new ServiceException(ResultCode.FAILED_FILE_UPLOAD);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void checkUploadCount() {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        Long times = redisService.getCacheMapValue(CacheConstants.USER_UPLOAD_TIMES_KEY, String.valueOf(userId), Long.class);
        if (times != null && times >= maxTime) {
            throw new ServiceException(ResultCode.FAILED_FILE_UPLOAD_TIME_LIMIT);
        }
        redisService.incrementHashValue(CacheConstants.USER_UPLOAD_TIMES_KEY, String.valueOf(userId), 1);
        if (times == null || times == 0) {
            long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
            redisService.expire(CacheConstants.USER_UPLOAD_TIMES_KEY, seconds, TimeUnit.SECONDS);
        }
    }

    private OSSResult upload(String fileType, InputStream inputStream) {
        // key pattern: file/id.xxx, cannot start with /
        String key = prop.getPathPrefix() + ObjectId.next() + "." + fileType;
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setObjectAcl(CannedAccessControlList.PublicRead);
        PutObjectRequest request = new PutObjectRequest(prop.getBucketName(), key, inputStream, objectMetadata);
        PutObjectResult putObjectResult;
        try {
            putObjectResult = ossClient.putObject(request);
        } catch (Exception e) {
            log.error("OSS put object error: {}", ExceptionUtil.stacktraceToOneLineString(e, 500));
            throw new ServiceException(ResultCode.FAILED_FILE_UPLOAD);
        }
        return assembleOSSResult(key, putObjectResult);
    }

    private OSSResult assembleOSSResult(String key, PutObjectResult putObjectResult) {
        OSSResult ossResult = new OSSResult();
        if (putObjectResult == null || StrUtil.isBlank(putObjectResult.getRequestId())) {
            ossResult.setSuccess(false);
        } else {
            ossResult.setSuccess(true);
            ossResult.setName(FileUtil.getName(key));
        }
        return ossResult;
    }
}

