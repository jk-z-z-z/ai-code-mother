<template>
  <div id="userLoginPage">
    <h1 class="title">用户登录</h1>
    <div class="desc">不写一行代码，让你生成一个完整应用</div>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item
        name="userAccount"
        :rules="[{ required: true, message: 'Please input your username!' }]"
      >
        <a-input v-model:value="formState.userAccount" placeHolder="请输入账号" />
      </a-form-item>

      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: 'Please input your password!' },
          { min: 8, message: '密码长度不能小于8位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>

      <div class="tip">
        没有账号？
        <router-link to="/user/register">立即注册</router-link>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">登陆</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script lang="ts" setup>
import { useLoginUserStore } from '@/stores/LoginUser'
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { login } from '@/api/userController.ts'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()
const handleSubmit = async (values: API.UserLoginRequest) => {
  const res = await login(values)
  if (res.data.code == 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登陆成功')
    router.push({
      path: '/',
      replace: true,
    })
  } else {
    message.error('登陆失败' + res.data.message)
  }
}
</script>

<style scoped>
#userLoginPage {
  max-width: 360px;
  margin: 0 auto;
}

.title {
  text-align: center;
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 20px;
}

.desc {
  text-align: center;
  font-size: 14px;
  color: #999;
  margin-bottom: 20px;
}

.tip {
  text-align: right;
  font-size: 16px;
  color: #999;
  margin-bottom: 20px;
}
</style>
