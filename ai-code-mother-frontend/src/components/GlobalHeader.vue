<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/LoginUser.ts'
import { logout } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { LogoutOutlined } from '@ant-design/icons-vue'
interface MenuItemConfig {
  key: string
  label: string
  path: string
}

const router = useRouter()
const route = useRoute()

const loginUserStore = useLoginUserStore()

const menuItems: MenuItemConfig[] = [{ key: 'home', label: '首页', path: '/' }]

const selectedKeys = computed(() => {
  const current = menuItems.find((item) => item.path === route.path)
  return current ? [current.key] : []
})

const handleMenuClick = (info: { key: string }) => {
  const item = menuItems.find((menuItem) => menuItem.key === info.key)
  if (item && item.path !== route.path) {
    router.push(item.path)
  }
}

const doLogout = async () => {
  const res = await logout()
  if (res.data.code == 0) {
    loginUserStore.setLoginUser({
      userName: '未登陆',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登陆失败' + res.data.message)
  }
}
</script>

<template>
  <div class="global-header">
    <div class="global-header-left">
      <div class="logo-wrapper">
        <img class="logo" src="@/assets/logo.png" alt="logo" />
        <span class="title">零代码应用生成平台</span>
      </div>

      <a-menu
        class="global-header-menu"
        mode="horizontal"
        theme="light"
        :selected-keys="selectedKeys"
        @click="handleMenuClick"
      >
        <a-menu-item v-for="item in menuItems" :key="item.key">
          {{ item.label }}
        </a-menu-item>
      </a-menu>
    </div>
  </div>
  <div v-if="loginUserStore.loginUser.id">
    <a-dropdown>
      <a-space>
        <a-avatar :src="loginUserStore.loginUser.userAvatar" />
        {{ loginUserStore.loginUser.userName ?? '无名' }}
      </a-space>
      <template #overlay>
        <a-menu>
          <a-menu-item @click="doLogout">
            <LogoutOutlined />
            退出登录
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
}

.global-header-left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
  min-width: 0;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo {
  height: 32px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #000000;
  white-space: nowrap;
}

.global-header-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none;
}

.global-header-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 96px;
  padding-left: 16px;
}

@media (max-width: 768px) {
  .global-header {
    padding: 0 16px;
  }

  .title {
    //display: none;
  }
}
</style>
