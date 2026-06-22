package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.AttachmentManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.AttachmentDto;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.request.DefectIdReq;
import com.nx.devtrack.common.web.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/attachment")
public class AttachmentController {

    private final AttachmentManager attachmentManager;

    @PostMapping("/upload")
    public CommonResponse<AttachmentDto> upload(@RequestParam("file") MultipartFile file,
                                                @RequestParam("defectId") Long defectId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(Errors.PARAM_INVALID.getCode(), "上传文件为空");
        }
        try {
            return CommonResponse.ok(attachmentManager.upload(
                    defectId, file.getOriginalFilename(), file.getBytes(),
                    file.getContentType(), UserContext.getCurrentUserId()));
        } catch (IOException e) {
            throw new BizException(Errors.INTERNAL_ERROR);
        }
    }

    @PostMapping("/list")
    public CommonResponse<List<AttachmentDto>> list(@Valid @RequestBody DefectIdReq req) {
        return CommonResponse.ok(attachmentManager.list(req.getDefectId(), UserContext.getCurrentUserId()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable("id") Long id) {
        AttachmentManager.DownloadFile f = attachmentManager.download(id, UserContext.getCurrentUserId());
        String encoded = URLEncoder.encode(f.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(f.data());
    }
}
