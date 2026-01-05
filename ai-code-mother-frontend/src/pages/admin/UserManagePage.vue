<template>
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <!-- 表格 -->
  </div>

  <a-table
    :columns="columns"
    :data-source="data"
    :pagination="pagination"
    @change="doTableChange"
    row-key="id"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'userAvatar'">
        <a-image :src="record.userAvatar" style="width: 60px; height: 60px; object-fit: cover" />
      </template>
      <template v-if="column.key === 'action'">
        <a-button type="primary" @click="openEdit(record)">编辑</a-button>
        <a-button danger @click="doDelete(record.id)">删除</a-button>
      </template>
    </template>
  </a-table>

  <a-modal
    v-model:open="editVisible"
    title="编辑用户"
    @ok="handleEditOk"
    @cancel="handleEditCancel"
  >
    <a-form :model="editForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="用户名">
        <a-input v-model:value="editForm.userName" />
      </a-form-item>
      <a-form-item label="头像地址">
        <div style="display: flex; align-items: center; gap: 16px">
          <a-input v-model:value="editForm.userAvatar" />
          <a-image
            v-if="editForm.userAvatar"
            :src="editForm.userAvatar"
            style="width: 36px; height: 36px; object-fit: cover"
          />
        </div>
      </a-form-item>
      <a-form-item label="简介">
        <a-input v-model:value="editForm.userProfile" />
      </a-form-item>
      <a-form-item label="角色">
        <a-select v-model:value="editForm.userRole" placeholder="请选择角色">
          <a-select-option value="user">普通用户</a-select-option>
          <a-select-option value="admin">管理员</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteUser, listUserVoByPage, updateUser } from '@/api/userController.ts'
import { message } from 'ant-design-vue'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
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
// 获取数据
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化处理
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const data = ref<API.UserVO[]>([])
const total = ref(0)
const editVisible = ref(false)
const editForm = reactive<API.UserUpdateRequest>({
  id: undefined,
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: '',
})
//搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})
//获取数据
const fetchData = async () => {
  const res = await listUserVoByPage({
    ...searchParams,
  })
  if (res.data.code == 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取数据失败' + res.data.message)
  }
}
const openEdit = (record: API.UserVO) => {
  editForm.id = record.id
  editForm.userName = record.userName ?? ''
  editForm.userAvatar = record.userAvatar ?? ''
  editForm.userProfile = record.userProfile ?? ''
  editForm.userRole = record.userRole ?? ''
  editVisible.value = true
}
const handleEditOk = async () => {
  if (!editForm.id) {
    return
  }
  const res = await updateUser({
    ...editForm,
  })
  if (res.data.code === 0) {
    message.success('更新成功')
    editVisible.value = false
    fetchData()
  } else {
    message.error('更新失败' + res.data.message)
  }
}
const handleEditCancel = () => {
  editVisible.value = false
}
//删除操作
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    //刷新数据
    fetchData()
  } else {
    message.error('删除失败' + res.data.message)
  }
}

onMounted(() => {
  fetchData()
})
</script>
