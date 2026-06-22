<template>
  <el-popover
    placement="bottom-end"
    :width="360"
    trigger="click"
    @show="handleShow"
  >
    <template #reference>
      <span class="bell-trigger">
        <el-badge
          :value="unreadCount"
          :hidden="unreadCount === 0"
          :max="99"
          class="bell-badge"
        >
          <el-icon :size="20"><Bell /></el-icon>
        </el-badge>
      </span>
    </template>

    <div class="notif-panel">
      <div class="notif-header">
        <span class="notif-title">通知</span>
        <el-button
          link
          type="primary"
          size="small"
          :disabled="unreadCount === 0"
          @click="handleMarkAll"
        >
          全部已读
        </el-button>
      </div>

      <div v-loading="loading" class="notif-list">
        <ul v-if="list.length" class="notif-items">
          <li
            v-for="n in list"
            :key="n.id"
            class="notif-item"
            :class="{ unread: !n.read }"
            @click="handleItemClick(n)"
          >
            <div class="notif-item-top">
              <el-tag :type="notificationTypeTagType(n.type)" size="small" effect="light">
                {{ notificationTypeLabel(n.type) }}
              </el-tag>
              <span class="notif-time">{{ formatTime(n.createTime) }}</span>
            </div>
            <div class="notif-content">{{ n.content }}</div>
          </li>
        </ul>
        <el-empty v-else-if="!loading" description="暂无通知" :image-size="60" />
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notification'
import type { NotificationDto } from '@/types'
import {
  formatTime,
  notificationTypeLabel,
  notificationTypeTagType,
} from '@/utils/enums'

const unreadCount = ref(0)
const list = ref<NotificationDto[]>([])
const loading = ref(false)

async function fetchUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res?.count ?? 0
  } catch {
    // Interceptor already showed an error.
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listNotifications({})
    list.value = res ?? []
  } catch {
    // Interceptor already showed an error.
  } finally {
    loading.value = false
  }
}

function handleShow() {
  fetchList()
}

async function handleItemClick(n: NotificationDto) {
  if (n.read) return
  try {
    await markNotificationRead({ id: n.id })
    await Promise.all([fetchList(), fetchUnreadCount()])
  } catch {
    // Interceptor already showed an error.
  }
}

async function handleMarkAll() {
  try {
    await markAllNotificationsRead()
    await Promise.all([fetchList(), fetchUnreadCount()])
  } catch {
    // Interceptor already showed an error.
  }
}

onMounted(fetchUnreadCount)
</script>

<style scoped>
.bell-trigger {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: #fff;
  outline: none;
}

.bell-badge :deep(.el-icon) {
  color: #fff;
}

.notif-panel {
  max-height: 420px;
  display: flex;
  flex-direction: column;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  margin-bottom: 4px;
  border-bottom: 1px solid #ebeef5;
}

.notif-title {
  font-weight: 600;
  color: #303133;
}

.notif-list {
  overflow-y: auto;
  min-height: 80px;
}

.notif-items {
  list-style: none;
  margin: 0;
  padding: 0;
}

.notif-item {
  padding: 10px 8px;
  border-bottom: 1px solid #f0f2f5;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s ease;
}

.notif-item:hover {
  background: #f5f7fa;
}

.notif-item.unread {
  background: #ecf5ff;
}

.notif-item.unread:hover {
  background: #d9ecff;
}

.notif-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.notif-time {
  color: #c0c4cc;
  font-size: 12px;
}

.notif-content {
  color: #303133;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
