package com.nx.devtrack.app.init;

import com.nx.devtrack.app.dao.CustomFieldDefDao;
import com.nx.devtrack.app.dao.RoleDao;
import com.nx.devtrack.app.dao.TagDao;
import com.nx.devtrack.app.dao.UserDao;
import com.nx.devtrack.app.dao.UserRoleDao;
import com.nx.devtrack.app.dao.WfStateDao;
import com.nx.devtrack.app.dao.WfTransitionDao;
import com.nx.devtrack.app.model.CustomFieldDef;
import com.nx.devtrack.app.model.Tag;
import com.nx.devtrack.app.model.Role;
import com.nx.devtrack.app.model.User;
import com.nx.devtrack.app.model.UserRole;
import com.nx.devtrack.app.manager.ApiTokenManager;
import com.nx.devtrack.app.model.WfState;
import com.nx.devtrack.app.model.WfTransition;
import com.nx.devtrack.app.security.Perms;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动时幂等播种演示数据:角色 + 账号(admin/guest/qa/dev) + 默认缺陷工作流。
 * 同时适用于本地 H2(ddl-auto)与生产 MySQL(Flyway 已建表),只补数据不建表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Long DEFAULT_WORKFLOW = 1L;
    private static final Long DEMO_PROJECT = 1L;

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final UserRoleDao userRoleDao;
    private final WfStateDao wfStateDao;
    private final WfTransitionDao wfTransitionDao;
    private final PasswordEncoder passwordEncoder;
    private final ApiTokenManager apiTokenManager;
    private final CustomFieldDefDao customFieldDefDao;
    private final TagDao tagDao;

    @Override
    public void run(String... args) {
        seedRoles();
        seedUsers();
        seedStates();
        seedWorkflow();
        seedCustomFields();
        seedTags();
    }

    private void seedTags() {
        if (!tagDao.findByProjectIdOrderByCreateTimeAsc(DEMO_PROJECT).isEmpty()) {
            return;
        }
        tagDao.saveAll(List.of(
                tag("iOS", "#409EFF"), tag("Android", "#67C23A"),
                tag("线上", "#F56C6C"), tag("性能", "#E6A23C")));
        log.info("[Init] 已播种演示标签: iOS/Android/线上/性能");
    }

    private Tag tag(String name, String color) {
        Tag t = new Tag();
        t.setProjectId(DEMO_PROJECT);
        t.setName(name);
        t.setColor(color);
        return t;
    }

    private void seedCustomFields() {
        if (!customFieldDefDao.findByProjectIdOrderByCreateTimeAsc(DEMO_PROJECT).isEmpty()) {
            return;
        }
        CustomFieldDef rootCause = new CustomFieldDef();
        rootCause.setProjectId(DEMO_PROJECT);
        rootCause.setFieldKey("root_cause");
        rootCause.setLabel("根因");
        rootCause.setFieldType("SELECT");
        rootCause.setOptions("[\"代码缺陷\",\"需求理解偏差\",\"环境问题\",\"第三方依赖\"]");
        rootCause.setRequired(false);

        CustomFieldDef points = new CustomFieldDef();
        points.setProjectId(DEMO_PROJECT);
        points.setFieldKey("story_points");
        points.setLabel("修复工作量");
        points.setFieldType("NUMBER");
        points.setRequired(false);

        customFieldDefDao.saveAll(List.of(rootCause, points));
        log.info("[Init] 已播种演示自定义字段: root_cause(SELECT), story_points(NUMBER)");
    }

    private void seedStates() {
        if (wfStateDao.countByWorkflowId(DEFAULT_WORKFLOW) > 0) {
            return;
        }
        List<WfState> states = new ArrayList<>();
        states.add(state("NEW", "新建", "INITIAL", true, 10));
        states.add(state("CONFIRMED", "已确认", "IN_PROGRESS", false, 20));
        states.add(state("IN_PROGRESS", "处理中", "IN_PROGRESS", false, 30));
        states.add(state("RESOLVED", "已解决", "IN_PROGRESS", false, 40));
        states.add(state("VERIFIED", "已验证", "IN_PROGRESS", false, 50));
        states.add(state("CLOSED", "关闭", "DONE", false, 60));
        states.add(state("REOPENED", "重新打开", "IN_PROGRESS", false, 70));
        states.add(state("REJECTED", "已拒绝", "REJECTED", false, 80));
        wfStateDao.saveAll(states);
        log.info("[Init] 已播种默认工作流状态,共 {} 个", states.size());
    }

    private WfState state(String code, String name, String category, boolean initial, int sort) {
        WfState s = new WfState();
        s.setWorkflowId(DEFAULT_WORKFLOW);
        s.setCode(code);
        s.setName(name);
        s.setCategory(category);
        s.setInitial(initial);
        s.setSortOrder(sort);
        return s;
    }

    private void seedRoles() {
        for (String code : Perms.ALL_ROLES) {
            if (roleDao.findByCode(code) == null) {
                Role r = new Role();
                r.setCode(code);
                r.setName(code);
                r.setScope(Perms.ROLE_SYS_ADMIN.equals(code) ? "GLOBAL" : "PROJECT");
                roleDao.save(r);
            }
        }
        log.info("[Init] 角色就绪: {}", Perms.ALL_ROLES);
    }

    private void seedUsers() {
        // admin:全局 SYS_ADMIN
        User admin = ensureUser("admin", "admin123", "管理员", true);
        assignRole(admin.getId(), "SYS_ADMIN", null);
        // 绑一个演示飞书 open_id,便于演示飞书卡片回调以 admin 身份执行
        if (admin.getFeishuOpenId() == null) {
            admin.setFeishuOpenId("ou_admin_demo");
            userDao.save(admin);
        }
        // 演示账号:在演示项目(id=1)分别授 QA / DEV / GUEST,用于演示 RBAC 差异
        assignRole(ensureUser("qa", "qa123", "测试同学", false).getId(), "QA", DEMO_PROJECT);
        assignRole(ensureUser("dev", "dev123", "开发同学", false).getId(), "DEV", DEMO_PROJECT);
        assignRole(ensureUser("guest", "guest123", "访客", false).getId(), "GUEST", DEMO_PROJECT);
        // 自动化机器账号 + 演示 API Token(库里只存 SHA-256;明文 dvt_live_demo_token 保证旧 pytest 集成不断)
        User bot = ensureUser("allure-bot", "no-login-bot", "自动化机器人", false);
        assignRole(bot.getId(), "SERVICE", DEMO_PROJECT);
        apiTokenManager.ensureToken("dvt_live_demo_token", "demo", bot.getId());
        log.info("[Init] 演示账号就绪: admin/admin123(SYS_ADMIN) qa/qa123(QA) dev/dev123(DEV) guest/guest123(GUEST);API Token: dvt_live_demo_token");
    }

    private User ensureUser(String username, String password, String displayName, boolean isAdmin) {
        User u = userDao.findByUsername(username);
        if (u != null) {
            return u;
        }
        u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password)); // BCrypt 哈希
        u.setDisplayName(displayName);
        u.setAdmin(isAdmin);
        return userDao.save(u);
    }

    private void assignRole(Long userId, String roleCode, Long projectId) {
        Role role = roleDao.findByCode(roleCode);
        if (role == null || userRoleDao.existsByUserIdAndRoleId(userId, role.getId())) {
            return;
        }
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(role.getId());
        ur.setProjectId(projectId);
        userRoleDao.save(ur);
    }

    private void seedWorkflow() {
        if (wfTransitionDao.countByWorkflowId(DEFAULT_WORKFLOW) > 0) {
            return;
        }
        List<WfTransition> rules = new ArrayList<>();
        rules.add(rule("confirm", "确认", "NEW", "CONFIRMED", null, false));
        rules.add(rule("start", "开始处理", "CONFIRMED", "IN_PROGRESS", null, false));
        rules.add(rule("start", "开始处理", "REOPENED", "IN_PROGRESS", null, false));
        rules.add(rule("resolve", "解决", "IN_PROGRESS", "RESOLVED", null, false));
        rules.add(rule("verify", "验证通过", "RESOLVED", "VERIFIED", "QA", false));
        rules.add(rule("close", "关闭", "VERIFIED", "CLOSED", null, false));
        rules.add(rule("reopen", "重新打开", "RESOLVED", "REOPENED", null, true));
        rules.add(rule("reopen", "重新打开", "VERIFIED", "REOPENED", null, true));
        rules.add(rule("reopen", "重新打开", "CLOSED", "REOPENED", null, true));
        rules.add(rule("reject", "拒绝", "*", "REJECTED", null, true));
        wfTransitionDao.saveAll(rules);
        log.info("[Init] 已播种默认缺陷工作流,共 {} 条流转规则", rules.size());
    }

    private WfTransition rule(String code, String name, String from, String to, String role, boolean requireComment) {
        WfTransition t = new WfTransition();
        t.setWorkflowId(DEFAULT_WORKFLOW);
        t.setCode(code);
        t.setName(name);
        t.setFromState(from);
        t.setToState(to);
        t.setRequireRole(role);
        t.setRequireComment(requireComment);
        return t;
    }
}
