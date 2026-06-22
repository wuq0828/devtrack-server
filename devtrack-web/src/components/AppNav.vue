<template>
  <el-menu
    class="nav-menu"
    mode="horizontal"
    menu-trigger="click"
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
</style>

<!--
  Submenu popup is teleported to <body>, so scoped styles can't reach it.
  The parent el-menu's props (white text, transparent bg) leak into the popup
  as CSS variables, making items white-on-white. Reset those variables to the
  light-theme defaults on the popper so every state stays readable.
-->
<style>
/* Element writes the parent menu's white text as an inline CSS variable on the
   popup <ul>, so resetting the variable loses to inline. Override the item
   color/background directly with !important — a direct property beats
   `color: var(--el-menu-text-color)` regardless of the variable's value. */
.nav-sub-popper .el-menu-item {
  color: #303133 !important;
  background-color: #fff;
}

.nav-sub-popper .el-menu-item:hover,
.nav-sub-popper .el-menu-item:focus {
  color: #409eff !important;
  background-color: #ecf5ff !important;
}

.nav-sub-popper .el-menu-item.is-active {
  color: #409eff !important;
  background-color: #ecf5ff !important;
  font-weight: 600;
}
</style>
