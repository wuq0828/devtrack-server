package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.dao.IterationDao;
import com.nx.devtrack.app.dao.WfStateDao;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.app.model.Iteration;
import com.nx.devtrack.app.model.WfState;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.common.dto.BurndownDto;
import com.nx.devtrack.common.dto.IterationReportDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IterationManagerTest {

    private final IterationDao iterationDao = mock(IterationDao.class);
    private final DefectDao defectDao = mock(DefectDao.class);
    private final WfStateDao wfStateDao = mock(WfStateDao.class);
    private final PermissionManager permissionManager = mock(PermissionManager.class);
    private final IterationManager manager =
            new IterationManager(iterationDao, defectDao, wfStateDao, permissionManager);

    private Defect defect(String status, LocalDateTime resolvedAt) {
        Defect d = new Defect();
        d.setStatusCode(status);
        d.setResolvedAt(resolvedAt);
        return d;
    }

    private WfState state(String code, String category) {
        WfState s = new WfState();
        s.setCode(code);
        s.setCategory(category);
        return s;
    }

    @Test
    void burndown_dropsAsDefectsResolve() {
        Iteration it = new Iteration();
        it.setName("S1");
        it.setProjectId(1L);
        it.setStartDate(LocalDate.of(2026, 6, 10));
        it.setEndDate(LocalDate.of(2026, 6, 12)); // 都在 today 之前,actualEnd=endDate,结果确定
        when(iterationDao.findById(1L)).thenReturn(Optional.of(it));
        when(defectDao.findByIterationId(1L)).thenReturn(List.of(
                defect("RESOLVED", LocalDateTime.of(2026, 6, 11, 10, 0)),
                defect("NEW", null)));

        BurndownDto bd = manager.burndown(1L, 99L);

        assertThat(bd.getTotal()).isEqualTo(2);
        assertThat(bd.getPoints()).hasSize(3);
        assertThat(bd.getPoints().get(0).getRemaining()).isEqualTo(2); // 06-10
        assertThat(bd.getPoints().get(1).getRemaining()).isEqualTo(1); // 06-11 一个已解决
        assertThat(bd.getPoints().get(2).getRemaining()).isEqualTo(1); // 06-12
    }

    @Test
    void report_countsByCategory() {
        Iteration it = new Iteration();
        it.setProjectId(1L);
        when(iterationDao.findById(1L)).thenReturn(Optional.of(it));
        when(wfStateDao.findByWorkflowIdOrderBySortOrderAsc(1L)).thenReturn(List.of(
                state("NEW", "INITIAL"), state("IN_PROGRESS", "IN_PROGRESS"), state("CLOSED", "DONE")));
        when(defectDao.findByIterationId(1L)).thenReturn(List.of(
                defect("NEW", null), defect("IN_PROGRESS", null), defect("CLOSED", null)));

        IterationReportDto r = manager.report(1L, 99L);

        assertThat(r.getTotal()).isEqualTo(3);
        assertThat(r.getTodo()).isEqualTo(1);
        assertThat(r.getInProgress()).isEqualTo(1);
        assertThat(r.getDone()).isEqualTo(1);
        assertThat(r.getCompletionRate()).isEqualTo(33.3);
    }
}
