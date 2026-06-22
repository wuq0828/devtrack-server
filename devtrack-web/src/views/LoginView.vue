<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="login-header">
        <h1 class="login-title">DevTrack</h1>
        <p class="login-subtitle">缺陷管理系统</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login-button"
            :loading="loading"
            @click="handleSubmit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider class="login-divider">或</el-divider>

      <el-button
        class="feishu-button"
        :loading="feishuLoading"
        @click="handleFeishuLogin"
      >
        <el-icon class="feishu-icon"><ChatDotRound /></el-icon>
        使用飞书登录
      </el-button>

      <p class="login-tip">演示账号：admin / admin123</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getFeishuAuthorizeUrl } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const feishuLoading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/defects'
    router.replace(redirect)
  } catch {
    // Error message already surfaced by the axios interceptor.
  } finally {
    loading.value = false
  }
}

async function handleFeishuLogin() {
  feishuLoading.value = true
  try {
    const res = await getFeishuAuthorizeUrl()
    if (res.authorizeUrl) {
      // Real OAuth: jump to Feishu's authorize page; it redirects back to
      // /feishu/callback?code=... which finishes the login.
      window.location.href = res.authorizeUrl
      return
    }
    // Dev mode (backend has no appId): simulate the scan by entering an open_id.
    const { value } = await ElMessageBox.prompt(
      '当前为 dev 模式(后端未配置飞书 appId)。输入任意 open_id 模拟飞书扫码登录：',
      '飞书登录（dev）',
      {
        confirmButtonText: '登录',
        cancelButtonText: '取消',
        inputValue: 'feishu_demo_user',
        inputValidator: (v) => (v && v.trim() ? true : '请输入 open_id'),
      },
    )
    await userStore.loginByFeishu(value.trim())
    ElMessage.success('飞书登录成功')
    const redirect = (route.query.redirect as string) || '/defects'
    router.replace(redirect)
  } catch (e) {
    // ElMessageBox 取消会 reject 'cancel'/'close';真实接口错误由拦截器提示。忽略取消。
    if (e !== 'cancel' && e !== 'close') {
      // no-op: interceptor already surfaced API errors
    }
  } finally {
    feishuLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  /* Deep-space backdrop with neon nebula glows, matching the app theme. */
  background-color: #0b0f1a;
  background-image:
    radial-gradient(700px 500px at 20% 10%, rgba(99, 102, 241, 0.30), transparent 60%),
    radial-gradient(700px 500px at 85% 90%, rgba(168, 85, 247, 0.22), transparent 60%),
    radial-gradient(600px 500px at 60% 50%, rgba(34, 211, 238, 0.12), transparent 65%);
}

.login-card {
  width: 380px;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.55), 0 0 0 1px rgba(99, 102, 241, 0.35);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-title {
  margin: 0;
  font-size: 32px;
  letter-spacing: 1px;
  /* Neon gradient wordmark. */
  background: linear-gradient(92deg, #818cf8, #22d3ee 55%, #a855f7);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
  text-shadow: 0 0 22px rgba(99, 102, 241, 0.45);
}

.login-subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.login-button {
  width: 100%;
}

.login-divider {
  margin: 4px 0 16px;
}

.login-divider :deep(.el-divider__text) {
  color: #9298b4;
  font-size: 12px;
}

.feishu-button {
  width: 100%;
  height: 40px;
  font-size: 14px;
  color: #e7e9f3;
  background: rgba(0, 122, 255, 0.14);
  border: 1px solid rgba(60, 150, 255, 0.45);
  transition: background 0.2s ease, border-color 0.2s ease;
}

.feishu-button:hover {
  background: rgba(0, 122, 255, 0.24);
  border-color: rgba(60, 150, 255, 0.7);
  color: #fff;
}

.feishu-icon {
  margin-right: 6px;
  color: #3c96ff;
  font-size: 16px;
}

.login-tip {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  margin: 4px 0 0;
}
</style>
