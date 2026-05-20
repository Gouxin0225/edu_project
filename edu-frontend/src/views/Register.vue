<template>
  <div class="register-page">
    <el-card shadow="never" class="register-card">
      <div class="brand-area">
        <div>
          <h2>学生自主注册</h2>
          <p>注册后可申请加入正在开课的班级，审核通过后即可跟班学习。</p>
        </div>
        <el-button text @click="router.push('/login')">返回登录</el-button>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px" size="large" @keyup.enter="submit">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号将作为登录账号" clearable maxlength="11" />
        </el-form-item>
        <el-form-item label="所属学校" prop="schoolName">
          <el-input v-model="form.schoolName" placeholder="请输入所属学校" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-30位密码" show-password clearable />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" @click="submit">注册并登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getRoleHome } from '@/router'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  realName: '',
  phone: '',
  schoolName: '',
  password: '',
  confirmPassword: ''
})

const rules: FormRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  schoolName: [{ required: true, message: '请输入所属学校', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度需为6-30位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        value === form.password ? callback() : callback(new Error('两次输入的密码不一致'))
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.register({
      realName: form.realName,
      phone: form.phone,
      schoolName: form.schoolName,
      password: form.password
    })
    ElMessage.success('注册成功')
    router.push(getRoleHome(userStore.userInfo!.role))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-card {
  width: min(520px, 100%);
  background: rgba(12, 20, 40, 0.9);
  border: 1px solid rgba(64, 128, 255, 0.22);
  border-radius: 16px;
}

.brand-area {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

h2 {
  margin: 0 0 8px;
  color: #e2e8f0;
}

p {
  margin: 0;
  color: rgba(226, 232, 240, 0.62);
  line-height: 1.6;
}

.submit-btn {
  width: 100%;
}
</style>
