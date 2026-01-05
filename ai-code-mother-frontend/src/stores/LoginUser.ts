import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUser } from '@/api/userController.ts'

export const useLoginUserStore = defineStore('loginUser', () => {
  //默认值
  const loginUser = ref<API.LoginUserVo>({
    userName: '未登陆',
  })

  async function fetchLoginUser() {
    const res = await getLoginUser()
    if (res.data.code == 0 && res.data.data) {
      loginUser.value = res.data.data
    }
  }
  function setLoginUser(newLoginUser: API.LoginUserVo) {
    loginUser.value = newLoginUser
  }
  return { loginUser, setLoginUser, fetchLoginUser }
})
