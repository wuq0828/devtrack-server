<template>
  <el-menu
    class="nav-menu"
    mode="horizontal"
    menu-trigger="click"
    unique-opened
    :default-active="activeIndex"
    router
    background-color="transparent"
    text-color="#ffffffcc"
    active-text-color="#ffffff"
    :ellipsis="false"
  >
    <el-menu-item index="/dashboard">大屏</el-menu-item>
    <el-menu-item index="/defects">列表</el-menu-item>
    <el-menu-item index="/board">看板</el-menu-item>
    <el-menu-item index="/iterations">迭代</el-menu-item>
    <el-menu-item index="/requirements">需求</el-menu-item>

    <el-sub-menu index="quality" popper-class="nav-sub-popper">
      <template #title>测试</template>
      <el-menu-item index="/testcases">用例</el-menu-item>
      <el-menu-item index="/testruns">执行轮次</el-menu-item>
      <el-menu-item index="/coverage">覆盖率</el-menu-item>
      <el-menu-item index="/ai-cases">AI用例</el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="more" popper-class="nav-sub-popper">
      <template #title>更多</template>
      <el-menu-item index="/gantt">甘特</el-menu-item>
      <el-menu-item index="/logs">日志</el-menu-item>
    </el-sub-menu>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
// Highlight the matching top-level / sub-menu item for the current path.
const activeIndex = computed(() => route.path)
</script>

<style scoped>
.nav-menu {
  border-bottom: none !important;
  height: 56px;
}

.nav-menu :deep(.el-menu-item),
.nav-menu :deep(.el-sub-menu__title) {
  height: 56px;
  line-height: 56px;
}

/* Keep submenu trigger text in the same light color as top-level items. */
.nav-menu :deep(.el-sub-menu__title) {
  color: #ffffffcc;
}

.nav-menu :deep(.el-sub-menu.is-active .el-sub-menu__title) {
  color: #ffffff;
}

@media (max-width: 768px) {
  .nav-menu {
    height: auto;
    flex-wrap: wrap;
  }

  .nav-menu :deep(.el-menu-item),
  .nav-menu :deep(.el-sub-menu__title) {
    height: 40px;
    line-height: 40px;
    padding: 0 10px;
  }
}

/* Active top-level item gets a neon underline + glow on the dark glass bar. */
.nav-menu :deep(.el-menu-item.is-active),
.nav-menu :deep(.el-sub-menu.is-active .el-sub-menu__title) {
  color: #fff !important;
  text-shadow: 0 0 16px rgba(99, 102, 241, 0.7);
  border-bottom: 2px solid #6366f1 !important;
}

.nav-menu :deep(.el-menu-item:hover),
.nav-menu :deep(.el-sub-menu__title:hover) {
  background-color: rgba(99, 102, 241, 0.12) !important;
  color: #fff !important;
}
</style>

<!--
  Submenu popup is teleported to <body>, so scoped styles can't reach it.
  The parent el-menu's props (white text, transparent bg) leak into the popup
  as CSS variables. Override item colors directly so the popup reads as a dark
  glass panel consistent with the rest of the theme.
-->
<style>
/* Frosted-glass dropdown panel. */
.nav-sub-popper.el-menu--horizontal .el-menu--popup,
.nav-sub-popper .el-menu--popup {
  background: rgba(26, 33, 54, 0.85) !important;
  backdrop-filter: saturate(150%) blur(18px);
  -webkit-backdrop-filter: saturate(150%) blur(18px);
  border: 1px solid rgba(130, 140, 200, 0.32) !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5) !important;
}

/* Element writes the parent menu's white text as an inline CSS variable on the
   popup <ul>, so a direct property with !important is needed to win. */
.nav-sub-popper .el-menu-item {
  color: #c2c6da !important;
  background-color: transparent !important;
}

.nav-sub-popper .el-menu-item:hover,
.nav-sub-popper .el-menu-item:focus {
  color: #fff !important;
  background-color: rgba(99, 102, 241, 0.18) !important;
}

.nav-sub-popper .el-menu-item.is-active {
  color: #fff !important;
  background-color: rgba(99, 102, 241, 0.22) !important;
  font-weight: 600;
  text-shadow: 0 0 14px rgba(99, 102, 241, 0.6);
}
</style>
