<template>
  <el-drawer
    v-model="visible"
    :size="drawerSize"
    direction="rtl"
    :with-header="false"
    class="tc-drawer"
  >
    <div v-if="form" class="tc-detail">
      <!-- Header -->
      <div class="tc-head">
        <div class="tc-head-top">
          <span class="tc-id">TC-{{ form.id }}</span>
          <el-tag :type="TESTCASE_STATUS_TAG_TYPE[form.status]" effect="dark" size="small">
            {{ TESTCASE_STATUS_LABEL[form.status] }}
          </el-tag>
          <el-tag v-if="form.regression" type="warning" effect="plain" size="small">
            回归
          </el-tag>
          <div class="tc-head-spacer" />
          <el-button text :icon="Close" circle @click="visible = false" />
        </div>

        <!-- Title: read or edit -->
        <el-input
          v-if="editing"
          v-model="form.title"
          class="tc-title-input"
          placeholder="用例标题"
          maxlength="200"
          show-word-limit
        />
        <h2 v-else class="tc-title">{{ form.title }}</h2>
      </div>

      <el-divider class="tc-divider" />

      <!-- Quick actions -->
      <div class="tc-actions">
        <div class="tc-action-row">
          <span class="tc-action-label">状态</span>
          <el-select
            v-model="form.status"
            size="small"
            style="width: 130px"
            :disabled="busy"
            @change="handleStatusChange"
          >
            <el-option
              v-for="opt in TESTCASE_STATUS_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
        <div class="tc-action-row">
          <span class="tc-action-label">回归套件</span>
          <el-switch
            v-model="form.regression"
            :loading="busy"
            inline-prompt
            active-text="是"
            inactive-text="否"
            @change="handleRegressionChange"
          />
        </div>
      </div>

      <el-divider class="tc-divider" />

      <!-- Body: read or edit -->
      <div class="tc-body">
        <section class="tc-section">
          <div class="tc-section-title">前置条件</div>
          <el-input
            v-if="editing"
            v-model="form.preconditions"
            type="textarea"
            :rows="3"
            placeholder="无"
          />
          <p v-else class="tc-text">{{ form.preconditions || '—' }}</p>
        </section>

        <section class="tc-section">
          <div class="tc-section-title">操作步骤</div>
          <el-input
            v-if="editing"
            v-model="form.steps"
            type="textarea"
            :rows="5"
            placeholder="无"
          />
          <p v-else class="tc-text">{{ form.steps || '—' }}</p>
        </section>

        <section class="tc-section">
          <div class="tc-section-title">预期结果</div>
          <el-input
            v-if="editing"
            v-model="form.expected"
            type="textarea"
            :rows="3"
            placeholder="无"
          />
          <p v-else class="tc-text">{{ form.expected || '—' }}</p>
        </section>

        <section class="tc-section">
          <div class="tc-section-title">自动化标识</div>
          <el-input
            v-if="editing"
            v-model="form.automationKey"
            placeholder="对应 pytest 用例 fullName,可空"
          />
          <div v-else class="tc-auto">
            <code v-if="form.automationKey" class="tc-auto-key">{{ form.automationKey }}</code>
            <span v-else class="tc-text">未关联</span>
            <el-button
              v-if="form.automationKey"
              text
              size="small"
              :icon="CopyDocument"
              @click="copyAutomationKey"
            >
              复制
            </el-button>
          </div>
        </section>

        <section class="tc-section">
          <div class="tc-section-title">创建时间</div>
          <p class="tc-text">{{ formatTime(form.createTime) }}</p>
        </section>
      </div>
    </div>

    <!-- Footer -->
    <template v-if="form">
      <div class="tc-footer">
        <template v-if="editing">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
        </template>
        <template v-else>
          <el-button type="danger" plain :icon="Delete" :loading="deleting" @click="handleDelete">
            删除
          </el-button>
          <div class="tc-footer-spacer" />
          <el-button type="primary" :icon="EditPen" @click="startEdit">编辑</el-button>
        </template>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, CopyDocument, Delete, EditPen } from '@element-plus/icons-vue'
import {
  deleteTestCase,
  setTestCaseRegression,
  updateTestCase,
  updateTestCaseStatus,
} from '@/api/testcase'
import type { TestCaseDto, TestCaseStatus } from '@/types'
import {
  TESTCASE_STATUS_LABEL,
  TESTCASE_STATUS_TAG_TYPE,
  TESTCASE_STATUS_OPTIONS,
  formatTime,
} from '@/utils/enums'

const props = defineProps<{
  modelValue: boolean
  testCase: TestCaseDto | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  // The case was mutated (status / regression / edit) — parent should replace the row.
  saved: [value: TestCaseDto]
  // The case was deleted — parent should drop the row.
  deleted: [id: number]
}>()

const visible = ref(props.modelValue)
// Editable working copy so we never mutate the parent's row directly.
const form = ref<TestCaseDto | null>(null)
const editing = ref(false)
const saving = ref(false)
const deleting = ref(false)
const busy = ref(false)

const drawerSize = computed(() => (window.innerWidth <= 768 ? '90%' : 460))

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      editing.value = false
      form.value = props.testCase ? { ...props.testCase } : null
    }
  },
)

watch(visible, (val) => {
  if (val !== props.modelValue) emit('update:modelValue', val)
})

function startEdit() {
  editing.value = true
}

function cancelEdit() {
  // Restore from the original prop snapshot.
  form.value = props.testCase ? { ...props.testCase } : null
  editing.value = false
}

async function saveEdit() {
  if (!form.value) return
  if (!form.value.title.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  saving.value = true
  try {
    const updated = await updateTestCase({
      testCaseId: form.value.id,
      title: form.value.title,
      preconditions: form.value.preconditions,
      steps: form.value.steps,
      expected: form.value.expected,
      automationKey: form.value.automationKey,
    })
    ElMessage.success('已保存')
    form.value = { ...updated }
    editing.value = false
    emit('saved', updated)
  } catch {
    // Interceptor already showed an error.
  } finally {
    saving.value = false
  }
}

async function handleStatusChange(status: TestCaseStatus) {
  if (!form.value) return
  busy.value = true
  try {
    const updated = await updateTestCaseStatus({ testCaseId: form.value.id, status })
    ElMessage.success('状态已更新')
    form.value = { ...updated }
    emit('saved', updated)
  } catch {
    // Revert local select on failure.
    if (props.testCase) form.value = { ...props.testCase }
  } finally {
    busy.value = false
  }
}

async function handleRegressionChange(val: string | number | boolean) {
  if (!form.value) return
  busy.value = true
  try {
    const updated = await setTestCaseRegression({
      testCaseId: form.value.id,
      regression: Boolean(val),
    })
    ElMessage.success(updated.regression ? '已加入回归套件' : '已移出回归套件')
    form.value = { ...updated }
    emit('saved', updated)
  } catch {
    if (props.testCase) form.value = { ...props.testCase }
  } finally {
    busy.value = false
  }
}

async function handleDelete() {
  if (!form.value) return
  try {
    await ElMessageBox.confirm('确定删除该用例吗？此操作不可恢复。', '删除用例', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  deleting.value = true
  try {
    const id = form.value.id
    await deleteTestCase({ testCaseId: id })
    ElMessage.success('已删除')
    emit('deleted', id)
    visible.value = false
  } catch {
    // Interceptor already showed an error.
  } finally {
    deleting.value = false
  }
}

async function copyAutomationKey() {
  if (!form.value?.automationKey) return
  try {
    await navigator.clipboard.writeText(form.value.automationKey)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败')
  }
}
</script>

<style scoped>
.tc-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 20px 0;
}

.tc-head-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tc-head-spacer {
  flex: 1;
}

.tc-id {
  font-family: monospace;
  font-weight: 700;
  color: #818cf8;
  letter-spacing: 0.5px;
}

.tc-title {
  margin: 14px 0 0;
  font-size: 18px;
  line-height: 1.5;
  color: #e7e9f3;
}

.tc-title-input {
  margin-top: 14px;
}

.tc-divider {
  margin: 16px 0;
}

.tc-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tc-action-row {
  display: flex;
  align-items: center;
}

.tc-action-label {
  width: 84px;
  color: #9298b4;
  font-size: 13px;
}

.tc-body {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-bottom: 16px;
}

.tc-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #9298b4;
  margin-bottom: 6px;
}

.tc-text {
  margin: 0;
  color: #e7e9f3;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.tc-auto {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tc-auto-key {
  font-family: monospace;
  font-size: 12px;
  color: #22d3ee;
  background: rgba(34, 211, 238, 0.1);
  border: 1px solid rgba(34, 211, 238, 0.25);
  border-radius: 6px;
  padding: 2px 8px;
  word-break: break-all;
}

.tc-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid rgba(130, 140, 200, 0.18);
}

.tc-footer-spacer {
  flex: 1;
}
</style>
