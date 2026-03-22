package com.sintao.friend.controller.question.file;

import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import com.sintao.common.file.domain.OSSResult;
import com.sintao.friend.service.file.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@Tag(name = "C 端文件接口", description = "文件上传等接口")
public class FileController extends BaseController {

    @Autowired
    private IFileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "文件上传", description = "上传文件到 OSS，返回文件访问地址")
    @Parameter(name = "file", description = "待上传的文件", required = true, schema = @Schema(type = "string", format = "binary"))
    @ApiResponse(responseCode = "200", description = "上传成功，返回 OSS 结果")
    @ApiResponse(responseCode = "2000", description = "上传失败")
    public R<OSSResult> upload(@RequestPart("file") MultipartFile file) {
        return R.ok(fileService.upload(file));
    }
}

