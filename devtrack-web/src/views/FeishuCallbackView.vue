<template>
  <div class="callback-page">
    <el-card class="callback-card" shadow="always">
      <div v-if="status === 'loading'" class="callback-state">
        <el-icon class="is-loading callback-icon"><Loading /></el-icon>
        <p>正在通过飞书登录…</p>
      </div>
      <div v-else class="callback-state">
        <el-icon class="callback-icon error"><CircleCloseFilled /></el-icon>
        <p>{{ message }}</p>
        <el-button type="primary" @click="goLogin">返回登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, CircleCloseFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const status = ref<'loading' | 'error'>('loading')
const message = ref('飞书登录失败')

function goLogin() {
  router.replace('/login')
}

onMounted(async () => {
  const code = route.query.code as string | undefined
  if (!code) {
    status.value = 'error'
    message.value = '未获取到飞书授权 code'
    return
  }
  try {
    await userStore.loginByFeishu(code)
    ElMessage.success('飞书登录成功')
    router.replace('/defects')
  } catch {
    // The axios interceptor already surfaced the server error.
    status.value = 'error'
    message.value = '飞书登录失败,请重试或改用账号密码登录'
  }
})
</script>

<style scoped>
.callback-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #0b0f1a;
  background-image:
    radial-gradient(700px 500px at 20% 10%, rgba(99, 102, 241, 0.3), transparent 60%),
    radial-gradient(700px 500px at 85% 90%, rgba(168, 85, 247, 0.22), transparent 60%);
}

.callback-card {
  width: 360px;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.55), 0 0 0 1px rgba(99, 102, 241, 0.35);
}

.callback-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 24px 0;
  color: #c2c6da;
}

.callback-icon {
  font-size: 40px;
  color: #818cf8;
}

.callback-icon.error {
  color: #f87171;
}
</style>
