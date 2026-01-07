<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getAppById, getMyAppById, updateApp, updateMyApp } from '@/api/appController.ts'
import { useLoginUserStore } from '@/stores/LoginUser.ts'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appId = computed(() => String(route.params.id ?? ''))
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

const loading = ref(false)
const submitting = ref(false)

const app = ref<API.AppVO>()

const form = reactive<{
  appName: string
  cover?: string
  priority?: number
}>({
  appName: '',
  cover: '',
  priority: undefined,
})

const resetFormFromApp = () => {
  if (!app.value) {
    return
  }
  form.appName = app.value.appName || ''
  form.cover = app.value.cover || ''
  form.priority = app.value.priority ?? undefined
}

const fetchApp = async () => {
  if (!appId.value) {
    message.error('应用信息缺失')
    return
  }
  loading.value = true
  try {
    if (isAdmin.value) {
      const res = await getAppById({ id: appId.value as unknown as number })
      if (res.data.code === 0 && res.data.data) {
        app.value = res.data.data
        resetFormFromApp()
      } else {
        message.error('获取应用失败' + res.data.message)
      }
    } else {
      const res = await getMyAppById({ id: appId.value as unknown as number })
      if (res.data.code === 0 && res.data.data) {
        app.value = res.data.data
        resetFormFromApp()
      } else {
        message.error('获取应用失败' + res.data.message)
      }
    }
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!appId.value) {
    message.error('应用信息缺失')
    return
  }
  if (!form.appName.trim()) {
    message.warning('请输入应用名称')
    return
  }
  submitting.value = true
  try {
    if (isAdmin.value) {
      const res = await updateApp({
        id: appId.value as unknown as number,
        appName: form.appName,
        cover: form.cover,
        priority: form.priority,
      })
      if (res.data.code === 0) {
        message.success('保存成功')
        if (app.value) {
          app.value.appName = form.appName
          app.value.cover = form.cover
          app.value.priority = form.priority
        }
      } else {
        message.error('保存失败' + res.data.message)
      }
    } else {
      const res = await updateMyApp({
        id: appId.value as unknown as number,
        appName: form.appName,
        cover: form.cover,
      })
      if (res.data.code === 0) {
        message.success('保存成功')
        if (app.value) {
          app.value.appName = form.appName
          app.value.cover = form.cover
        }
      } else {
        message.error('保存失败' + res.data.message)
      }
    }
  } finally {
    submitting.value = false
  }
}

const handleReset = () => {
  resetFormFromApp()
}

const goToChat = () => {
  if (!appId.value) {
    message.error('应用信息缺失')
    return
  }
  router.push(`/app/${appId.value}/chat`)
}

const goBack = () => {
  if (appId.value) {
    router.push(`/app/${appId.value}/chat`)
  } else {
    router.back()
  }
}

const deployedUrl = computed(() => {
  if (!app.value?.deployKey) {
    return ''
  }
  const origin = window.location.origin.replace(/\/+$/, '')
  return `${origin}/${app.value.deployKey}`
})

const openDeployedApp = () => {
  if (!deployedUrl.value) {
    return
  }
  window.open(deployedUrl.value, '_blank')
}

onMounted(() => {
  fetchApp()
})
</script>

<template>
  <div class="app-edit-page">
    <a-spin :spinning="loading">
      <div class="page-header">
        <a-button type="link" @click="goBack">返回</a-button>
        <div class="page-title">编辑应用信息</div>
      </div>

      <a-card title="基本信息" class="section-card">
        <a-form
          :model="form"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 20 }"
          @finish="handleSubmit"
        >
          <a-form-item
            label="应用名称"
            name="appName"
            :rules="[{ required: true, message: '请输入应用名称' }]"
          >
            <a-input v-model:value="form.appName" placeholder="请输入应用名称" />
          </a-form-item>

          <a-form-item label="应用封面">
            <div class="cover-row">
              <a-input v-model:value="form.cover" placeholder="请输入封面图片地址" />
              <a-image v-if="form.cover" :src="form.cover" class="cover-preview" />
            </div>
            <div class="form-hint">支持图片链接，建议尺寸：400x300</div>
          </a-form-item>

          <a-form-item v-if="isAdmin" label="优先级">
            <a-input-number
              v-model:value="form.priority"
              :min="0"
              :max="999"
              style="width: 160px"
              placeholder="数值越大优先级越高"
            />
            <div class="form-hint">设置为 99 表示精选应用</div>
          </a-form-item>

          <a-form-item label="初始提示词">
            <a-textarea
              :value="app?.initPrompt"
              disabled
              placeholder="初始提示词不可修改"
              auto-size
            />
            <div class="form-hint">初始提示词不可修改</div>
          </a-form-item>

          <a-form-item label="生成类型">
            <a-input :value="app?.codeGenType" disabled placeholder="生成类型不可修改" />
            <div class="form-hint">生成类型不可修改</div>
          </a-form-item>

          <a-form-item label="部署密钥">
            <a-input :value="app?.deployKey" disabled placeholder="部署密钥不可修改" />
            <div class="form-hint">部署密钥不可修改</div>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 20, offset: 4 }">
            <a-button type="primary" html-type="submit" :loading="submitting"> 保存修改 </a-button>
            <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
            <a-button style="margin-left: 8px" @click="goToChat">进入对话</a-button>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card title="应用信息" class="section-card">
        <a-row :gutter="[24, 16]">
          <a-col :span="12">
            <div class="info-item">
              <span class="info-label">应用 ID：</span>
              <span class="info-value">{{ app?.id ?? '-' }}</span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="info-item">
              <span class="info-label">创建者：</span>
              <span class="info-value">
                {{ app?.userVO?.userName || app?.userVO?.userAccount || '-' }}
              </span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="info-item">
              <span class="info-label">创建时间：</span>
              <span class="info-value">{{ app?.createTime || '-' }}</span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="info-item">
              <span class="info-label">更新时间：</span>
              <span class="info-value">
                {{ app?.updateTime || '未部署' }}
              </span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="info-item">
              <span class="info-label">部署时间：</span>
              <span class="info-value">
                {{ app?.deployedTime || '未部署' }}
              </span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="info-item">
              <span class="info-label">访问链接：</span>
              <template v-if="deployedUrl">
                <a-button type="link" @click="openDeployedApp">查看预览</a-button>
              </template>
              <span v-else class="info-value">未部署</span>
            </div>
          </a-col>
        </a-row>
      </a-card>
    </a-spin>
  </div>
</template>

<style scoped>
.app-edit-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 0;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
}

.section-card + .section-card {
  margin-top: 24px;
}

.cover-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cover-preview {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.form-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #999999;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.info-label {
  width: 80px;
  color: #666666;
}

.info-value {
  color: #333333;
}
</style>
