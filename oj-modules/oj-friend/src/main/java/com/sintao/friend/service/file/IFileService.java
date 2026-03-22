package com.sintao.friend.service.file;

import com.sintao.common.file.domain.OSSResult;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    OSSResult upload(MultipartFile file);
}

