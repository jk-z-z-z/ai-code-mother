<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { deleteApp, listAppByPage, updateApp } from '@/api/appController.ts'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
  },
  {
    title: '封面',
    dataIndex: 'cover',
  },
  {
    title: '生成模式',
    dataIndex: 'codeGenType',
  },
  {
    title: '优先级',
    dataIndex: 'priority',
  },
  {
    title: '创建人',
    dataIndex: ['userVO', 'userName'],
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

const data = ref<API.AppVO[]>([])
const total = ref(0)

const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (t: number) => `共 ${t} 条`,
  }
})

const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const fetchData = async () => {
  const res = await listAppByPage({
    ...searchParams,
  })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取应用列表失败' + res.data.message)
  }
}
const editVisible = ref(false)
const editSubmitting = ref(false)
const editForm = reactive<{
  id?: number
  appName: string
  cover?: string
  priority?: number
}>({
  id: undefined,
  appName: '',
  cover: '',
  priority: undefined,
})

const openEdit = (record: API.AppVO) => {
  editForm.id = record.id
  editForm.appName = record.appName || ''
  editForm.cover = record.cover || ''
  editForm.priority = record.priority ?? undefined
  editVisible.value = true
}

const handleEditOk = async () => {
  if (!editForm.id) {
    return
  }
  if (!editForm.appName.trim()) {
    message.warning('请输入应用名称')
    return
  }
  editSubmitting.value = true
  try {
    const res = await updateApp({
      id: editForm.id,
      appName: editForm.appName,
      cover: editForm.cover,
      priority: editForm.priority,
    })
    if (res.data.code === 0) {
      message.success('保存成功')
      editVisible.value = false
      fetchData()
    } else {
      message.error('保存失败' + res.data.message)
    }
  } finally {
    editSubmitting.value = false
  }
}

const handleEditCancel = () => {
  editVisible.value = false
}

const setFeatured = async (record: API.AppVO) => {
  if (!record.id) {
    return
  }
  const res = await updateApp({
    id: record.id,
    priority: 99,
  })
  if (res.data.code === 0) {
    message.success('已设置为精选应用')
    fetchData()
  } else {
    message.error('设置精选失败' + res.data.message)
  }
}

const doDelete = async (record: API.AppVO) => {
  if (!record.id) {
    return
  }
  const res = await deleteApp({
    id: record.id,
  })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else {
    message.error('删除失败' + res.data.message)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div id="appManagePage">
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="应用名称">
        <a-input v-model:value="searchParams.appName" placeholder="输入应用名称" />
      </a-form-item>
      <a-form-item label="用户ID">
        <a-input-number
          v-model:value="searchParams.userId"
          placeholder="用户ID"
          style="width: 160px"
        />
      </a-form-item>
      <a-form-item label="生成模式">
        <a-select
          v-model:value="searchParams.codeGenType"
          allow-clear
          placeholder="选择模式"
          style="width: 160px"
        >
          <a-select-option value="html">原生 HTML 模式</a-select-option>
          <a-select-option value="multi_file">原生多文件模式</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />

    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      row-key="id"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cover'">
          <a-image
            v-if="record.cover"
            :src="record.cover"
            style="width: 60px; height: 60px; object-fit: cover"
          />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" @click="setFeatured(record)">精选</a-button>
            <a-popconfirm
              title="确定删除该应用吗？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="doDelete(record)"
            >
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
    <a-modal
      v-model:open="editVisible"
      title="编辑应用"
      :confirm-loading="editSubmitting"
      @ok="handleEditOk"
      @cancel="handleEditCancel"
    >
      <a-form :model="editForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
        <a-form-item label="应用名称">
          <a-input v-model:value="editForm.appName" placeholder="请输入应用名称" />
        </a-form-item>
        <a-form-item label="封面地址">
          <div style="display: flex; align-items: center; gap: 16px">
            <a-input v-model:value="editForm.cover" placeholder="请输入封面图片地址" />
            <a-image
              v-if="editForm.cover"
              :src="editForm.cover"
              style="width: 60px; height: 60px; object-fit: cover"
            />
          </div>
        </a-form-item>
        <a-form-item label="优先级">
          <a-input-number
            v-model:value="editForm.priority"
            :min="0"
            :max="999"
            style="width: 160px"
            placeholder="数值越大优先级越高"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
#appManagePage {
  background: #ffffff;
  padding: 16px;
  border-radius: 8px;
}
</style>
