<template>
  <div class="web-shell">
    <HeaderNav :active="activeTab" />
    <main class="web-shell__content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import HeaderNav from './HeaderNav.vue'

defineProps({
  activeTab: {
    type: String,
    default: 'home'
  }
})
</script>
