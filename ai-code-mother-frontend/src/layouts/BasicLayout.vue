<script setup lang="ts">
import { RouterView } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<template>
  <a-layout class="basic-layout">
    <a-layout-header class="basic-layout-header">
      <GlobalHeader />
    </a-layout-header>

    <a-layout-content class="basic-layout-content">
      <RouterView />
    </a-layout-content>

    <a-layout-footer class="basic-layout-footer">
      <GlobalFooter />
    </a-layout-footer>
  </a-layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
}

.basic-layout-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  padding: 0;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.basic-layout-content {
  margin-top: 64px;
  padding: 24px;
}

.basic-layout-footer {
  text-align: center;
}

@media (max-width: 768px) {
  .basic-layout-content {
    padding: 16px;
  }
}
</style>
