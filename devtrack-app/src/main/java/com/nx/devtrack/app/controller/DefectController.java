package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.CommentManager;
import com.nx.devtrack.app.manager.DefectManager;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.ActivityDto;
import com.nx.devtrack.common.dto.BatchResultDto;
import com.nx.devtrack.common.dto.BoardColumnDto;
import com.nx.devtrack.common.dto.CommentItemDto;
import com.nx.devtrack.common.dto.DefectDetailDto;
import com.nx.devtrack.common.dto.DefectDto;
import com.nx.devtrack.common.dto.StatsDto;
import com.nx.devtrack.common.dto.TagDto;
import com.nx.devtrack.common.request.AddCommentReq;
import com.nx.devtrack.common.request.AssignIterationReq;
import com.nx.devtrack.common.request.BatchTransitionReq;
import com.nx.devtrack.common.request.CreateDefectReq;
import com.nx.devtrack.common.request.DefectIdReq;
import com.nx.devtrack.common.request.ListDefectReq;
import com.nx.devtrack.common.request.ProjectScopeReq;
import com.nx.devtrack.common.request.SetFieldsReq;
import com.nx.devtrack.common.request.SetTagsReq;
import com.nx.devtrack.common.request.SetVersionReq;
import com.nx.devtrack.common.request.TransitionDefectReq;
import com.nx.devtrack.common.web.CommonResponse;
import com.nx.devtrack.common.web.Pagination;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/defect")
public class DefectController {

    private final DefectManager defectManager;
    private final CommentManager commentManager;
    private final com.nx.devtrack.app.manager.TagManager tagManager;

    @PostMapping("/create")
    public CommonResponse<DefectDto> create(@Valid @RequestBody CreateDefectReq req) {
        Defect defect = defectManager.create(req, UserContext.getCurrentUserId());
        return CommonResponse.ok(defectManager.toDto(defect));
    }

    @PostMapping("/list")
    public CommonResponse<Pagination<DefectDto>> list(@RequestBody ListDefectReq req) {
        Page<Defect> page = defectManager.query(req, UserContext.getCurrentUserId());
        List<DefectDto> dtos = page.getContent().stream().map(defectManager::toDto).toList();
        return CommonResponse.ok(Pagination.of(dtos, req.getPn(), req.getPs(), page.getTotalElements()));
    }

    @PostMapping("/transition")
    public CommonResponse<DefectDto> transition(@Valid @RequestBody TransitionDefectReq req) {
        Defect defect = defectManager.transition(
                req.getDefectId(), req.getTransitionCode(), req.getComment(), UserContext.getCurrentUserId());
        return CommonResponse.ok(defectManager.toDto(defect));
    }

    @PostMapping("/detail")
    public CommonResponse<DefectDetailDto> detail(@Valid @RequestBody DefectIdReq req) {
        return CommonResponse.ok(defectManager.detail(req.getDefectId(), UserContext.getCurrentUserId()));
    }

    @PostMapping("/comment")
    public CommonResponse<CommentItemDto> comment(@Valid @RequestBody AddCommentReq req) {
        return CommonResponse.ok(commentManager.addComment(
                req.getDefectId(), req.getContent(), UserContext.getCurrentUserId()));
    }

    @PostMapping("/board")
    public CommonResponse<Map<String, List<BoardColumnDto>>> board(@Valid @RequestBody ProjectScopeReq req) {
        List<BoardColumnDto> columns = defectManager.board(req.getProjectId(), UserContext.getCurrentUserId());
        return CommonResponse.ok(Map.of("columns", columns));
    }

    @PostMapping("/stats")
    public CommonResponse<StatsDto> stats(@Valid @RequestBody ProjectScopeReq req) {
        return CommonResponse.ok(defectManager.stats(req.getProjectId(), UserContext.getCurrentUserId()));
    }

    @PostMapping("/assign-iteration")
    public CommonResponse<DefectDto> assignIteration(@Valid @RequestBody AssignIterationReq req) {
        Defect defect = defectManager.assignIteration(
                req.getDefectId(), req.getIterationId(), UserContext.getCurrentUserId());
        return CommonResponse.ok(defectManager.toDto(defect));
    }

    @PostMapping("/activity")
    public CommonResponse<List<ActivityDto>> activity(@Valid @RequestBody DefectIdReq req) {
        return CommonResponse.ok(defectManager.activity(req.getDefectId(), UserContext.getCurrentUserId()));
    }

    @PostMapping("/batch-transition")
    public CommonResponse<Map<String, List<BatchResultDto>>> batchTransition(@Valid @RequestBody BatchTransitionReq req) {
        List<BatchResultDto> results = defectManager.batchTransition(
                req.getDefectIds(), req.getTransitionCode(), req.getComment(), UserContext.getCurrentUserId());
        return CommonResponse.ok(Map.of("results", results));
    }

    @PostMapping("/set-fields")
    public CommonResponse<DefectDto> setFields(@Valid @RequestBody SetFieldsReq req) {
        Defect defect = defectManager.setCustomFields(req.getDefectId(), req.getFields(), UserContext.getCurrentUserId());
        return CommonResponse.ok(defectManager.toDto(defect));
    }

    @PostMapping("/search")
    public CommonResponse<List<DefectDto>> search(@RequestBody ListDefectReq req) {
        List<DefectDto> list = defectManager.search(req.getKeyword(), req.getProjectId(), UserContext.getCurrentUserId())
                .stream().map(defectManager::toDto).toList();
        return CommonResponse.ok(list);
    }

    @PostMapping("/tags")
    public CommonResponse<List<TagDto>> tags(@Valid @RequestBody DefectIdReq req) {
        return CommonResponse.ok(tagManager.defectTags(req.getDefectId(), UserContext.getCurrentUserId()));
    }

    @PostMapping("/set-tags")
    public CommonResponse<List<TagDto>> setTags(@Valid @RequestBody SetTagsReq req) {
        return CommonResponse.ok(tagManager.setDefectTags(req.getDefectId(), req.getTagIds(), UserContext.getCurrentUserId()));
    }

    @PostMapping("/find-duplicates")
    public CommonResponse<List<DefectDto>> findDuplicates(@RequestBody ListDefectReq req) {
        List<DefectDto> list = defectManager.findDuplicates(req.getKeyword(), req.getProjectId(), UserContext.getCurrentUserId())
                .stream().map(defectManager::toDto).toList();
        return CommonResponse.ok(list);
    }

    @PostMapping("/set-version")
    public CommonResponse<DefectDto> setVersion(@Valid @RequestBody SetVersionReq req) {
        Defect defect = defectManager.setFixVersion(req.getDefectId(), req.getVersionId(), UserContext.getCurrentUserId());
        return CommonResponse.ok(defectManager.toDto(defect));
    }
}
