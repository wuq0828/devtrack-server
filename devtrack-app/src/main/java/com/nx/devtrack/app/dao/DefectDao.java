package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Defect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefectDao extends JpaRepository<Defect, Long>, JpaSpecificationExecutor<Defect> {

    /** 看板 / 统计:按项目取全部(脚手架规模,内存分组) */
    List<Defect> findByProjectId(Long projectId);

    /** 燃尽图:迭代下的缺陷 */
    List<Defect> findByIterationId(Long iterationId);

    /** 项目内最大序号,用于生成 seqInProject(展示编号 PAY-N) */
    Defect findFirstByProjectIdOrderBySeqInProjectDesc(Long projectId);

    /** 自动建单去重:同项目同指纹的未关闭缺陷 */
    Defect findFirstByProjectIdAndExternalRefAndStatusCodeNot(Long projectId, String externalRef, String statusCode);

    /** 迁移去重:同项目同外部 ref 是否已存在 */
    boolean existsByProjectIdAndExternalRef(Long projectId, String externalRef);
}
