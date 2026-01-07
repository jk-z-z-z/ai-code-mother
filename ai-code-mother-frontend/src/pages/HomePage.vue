<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { addApp, listFeaturedAppByPage, listMyAppByPage } from '@/api/appController.ts'
const router = useRouter()

const creating = ref(false)
const initPrompt = ref('')
const codeGenType = ref<API.AppAddRequest['codeGenType']>('html')

type AppListQuery = {
  pageNum: number
  pageSize: number
}

const myQuery = reactive<AppListQuery>({
  pageNum: 1,
  pageSize: 20,
})

const featuredQuery = reactive<AppListQuery>({
  pageNum: 1,
  pageSize: 20,
})

const myApps = ref<API.AppVO[]>([])
const myTotal = ref(0)
const featuredApps = ref<API.AppVO[]>([])
const featuredTotal = ref(0)

const loadingMy = ref(false)
const loadingFeatured = ref(false)

const mySearchName = ref('')
const featuredSearchName = ref('')

const templatePrompts = [
  '生成一个波普风电商首页',
  '帮我搭建一个企业官网',
  '帮我做一个电商运营后台',
  '生成一个暗黑活跃社区网站',
]

const handleTemplateClick = (text: string) => {
  initPrompt.value = text
}

const handleCreate = async () => {
  const content = initPrompt.value.trim()
  if (!content) {
    message.warning('请先输入一句话需求')
    return
  }
  if (creating.value) {
    return
  }
  creating.value = true
  try {
    const res = await addApp({
      initPrompt: content,
      codeGenType: codeGenType.value,
    })
    if (res.data.code === 0 && res.data.data) {
      const appId = res.data.data
      initPrompt.value = ''
      router.push({
        path: `/app/${appId}/chat`,
        query: {
          autoStart: '1',
        },
      })
    } else {
      message.error('创建应用失败' + res.data.message)
    }
  } finally {
    creating.value = false
  }
}

const fetchMyApps = async () => {
  loadingMy.value = true
  try {
    const res = await listMyAppByPage({
      pageNum: myQuery.pageNum,
      pageSize: myQuery.pageSize,
      appName: mySearchName.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myTotal.value = res.data.data.totalRow || 0
    } else {
      message.error('获取我的应用失败' + res.data.message)
    }
  } finally {
    loadingMy.value = false
  }
}

const fetchFeaturedApps = async () => {
  loadingFeatured.value = true
  try {
    const res = await listFeaturedAppByPage({
      pageNum: featuredQuery.pageNum,
      pageSize: featuredQuery.pageSize,
      appName: featuredSearchName.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredTotal.value = res.data.data.totalRow || 0
    } else {
      message.error('获取精选应用失败' + res.data.message)
    }
  } finally {
    loadingFeatured.value = false
  }
}

const handleMyPageChange = (page: number, pageSize: number) => {
  myQuery.pageNum = page
  myQuery.pageSize = pageSize
  fetchMyApps()
}

const handleFeaturedPageChange = (page: number, pageSize: number) => {
  featuredQuery.pageNum = page
  featuredQuery.pageSize = pageSize
  fetchFeaturedApps()
}

const handleMySearch = () => {
  myQuery.pageNum = 1
  fetchMyApps()
}

const handleFeaturedSearch = () => {
  featuredQuery.pageNum = 1
  fetchFeaturedApps()
}

const goToChat = (app: API.AppVO) => {
  if (!app.id) {
    return
  }
  router.push({
    path: `/app/${app.id}/chat`,
  })
}

const goToPreview = (app: API.AppVO, isFeatured?: boolean) => {
  if (!app.id) {
    return
  }
  router.push({
    path: `/app/${app.id}/chat`,
    query: {
      preview: '1',
      ...(isFeatured ? { featured: '1' } : {}),
    },
  })
}

onMounted(() => {
  fetchMyApps()
  fetchFeaturedApps()
})
</script>

<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-title">一句话 星所想</div>
      <div class="hero-subtitle">与 AI 对话轻松创建应用和网站</div>
      <div class="prompt-card">
        <a-textarea
          v-model:value="initPrompt"
          class="prompt-input"
          :auto-size="{ minRows: 4, maxRows: 6 }"
          placeholder="使用 NoCode 创建一个高效的小工具，帮我计算……"
        />
        <div class="prompt-footer">
          <div class="prompt-footer-left">
            <a-select v-model:value="codeGenType" style="width: 180px">
              <a-select-option value="html">原生 HTML 模式</a-select-option>
              <a-select-option value="multi_file">原生多文件模式</a-select-option>
            </a-select>
          </div>
          <div class="prompt-footer-right">
            <a-button type="primary" shape="round" :loading="creating" @click="handleCreate">
              生成应用
            </a-button>
          </div>
        </div>
      </div>
      <div class="prompt-templates">
        <a-button
          v-for="item in templatePrompts"
          :key="item"
          class="template-button"
          shape="round"
          @click="handleTemplateClick(item)"
        >
          {{ item }}
        </a-button>
      </div>
    </section>

    <section class="apps-section">
      <div class="section-header">
        <div class="section-title">我的作品</div>
        <div class="section-actions">
          <a-input-search
            v-model:value="mySearchName"
            placeholder="按名称搜索我的应用"
            allow-clear
            enter-button="搜索"
            @search="handleMySearch"
          />
        </div>
      </div>
      <a-spin :spinning="loadingMy">
        <a-empty v-if="!myApps.length" description="还没有创建应用，试着在上方输入一句话吧" />
        <a-row v-else :gutter="[24, 24]">
          <a-col v-for="item in myApps" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
            <div class="app-card">
              <div class="app-cover">
                <img v-if="item.cover" :src="item.cover" alt="" />
                <div v-else class="app-cover-placeholder">预览生成中的页面</div>
              </div>
              <div class="app-content">
                <div class="app-name">
                  {{ item.appName || '未命名应用' }}
                </div>
                <div class="app-meta">创建于 {{ item.createTime || '-' }}</div>
              </div>
              <div class="app-primary-actions">
                <a-button type="primary" size="small" shape="round" @click="goToChat(item)">
                  查看对话
                </a-button>
                <a-button size="small" shape="round" @click="goToPreview(item)">
                  查看作品
                </a-button>
              </div>
            </div>
          </a-col>
        </a-row>
        <div v-if="myTotal > myQuery.pageSize" class="section-pagination">
          <a-pagination
            :current="myQuery.pageNum"
            :page-size="myQuery.pageSize"
            :total="myTotal"
            :show-size-changer="false"
            @change="handleMyPageChange"
          />
        </div>
      </a-spin>
    </section>

    <section class="apps-section">
      <div class="section-header">
        <div class="section-title">精选案例</div>
        <div class="section-actions">
          <a-input-search
            v-model:value="featuredSearchName"
            placeholder="按名称搜索精选应用"
            allow-clear
            enter-button="搜索"
            @search="handleFeaturedSearch"
          />
        </div>
      </div>
      <a-spin :spinning="loadingFeatured">
        <a-empty v-if="!featuredApps.length" description="暂无精选应用" />
        <a-row v-else :gutter="[24, 24]">
          <a-col v-for="item in featuredApps" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
            <div class="app-card">
              <div class="app-cover">
                <img v-if="item.cover" :src="item.cover" alt="" />
                <div v-else class="app-cover-placeholder">预览生成中的页面</div>
              </div>
              <div class="app-content">
                <div class="app-name">
                  {{ item.appName || '未命名应用' }}
                </div>
                <div class="app-meta">创建于 {{ item.createTime || '-' }}</div>
              </div>
              <div class="app-primary-actions">
                <a-button
                  type="primary"
                  size="small"
                  shape="round"
                  @click="goToPreview(item, true)"
                >
                  查看作品
                </a-button>
              </div>
            </div>
          </a-col>
        </a-row>
        <div v-if="featuredTotal > featuredQuery.pageSize" class="section-pagination">
          <a-pagination
            :current="featuredQuery.pageNum"
            :page-size="featuredQuery.pageSize"
            :total="featuredTotal"
            :show-size-changer="false"
            @change="handleFeaturedPageChange"
          />
        </div>
      </a-spin>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1120px;
  margin: 0 auto;
}

.hero-section {
  padding: 48px 0 32px;
  text-align: center;
}

.hero-title {
  font-size: 40px;
  font-weight: 700;
  margin-bottom: 12px;
}

.hero-subtitle {
  font-size: 16px;
  color: #666666;
  margin-bottom: 32px;
}

.prompt-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(15, 35, 52, 0.12);
  padding: 24px 24px 16px;
  max-width: 720px;
  margin: 0 auto;
}

.prompt-input {
  border-radius: 16px;
  border: none;
  box-shadow: none;
}

.prompt-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.prompt-footer-left {
  display: flex;
  gap: 8px;
}

.prompt-footer-right {
  display: flex;
  justify-content: flex-end;
}

.prompt-templates {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

.template-button {
  background: rgba(255, 255, 255, 0.8);
}

.apps-section {
  margin-top: 40px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
}

.section-actions {
  min-width: 260px;
}

.app-card {
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(15, 35, 52, 0.08);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.app-cover {
  position: relative;
  padding-top: 56%;
  background: #f5f5f5;
}

.app-cover img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-cover-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999999;
  font-size: 14px;
}

.app-content {
  padding: 12px 16px 4px;
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.app-meta {
  font-size: 12px;
  color: #999999;
}

.app-primary-actions {
  display: flex;
  gap: 8px;
  padding: 8px 16px 4px;
}

.app-actions {
  padding: 8px 16px 12px;
}

.section-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 28px;
  }

  .prompt-card {
    margin: 0 8px;
  }
}
</style>
