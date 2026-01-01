<script setup lang="ts">
import BasicLayout from './layouts/BasicLayout.vue'
import{heathCHeck} from '@/api/healthCheckController.ts'
heathCHeck().then(res=>{console.log(res)})
</script>

<template>
  <BasicLayout />
</template>
