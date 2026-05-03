<template>
  <div class="mobile-shell">
    <header class="mobile-shell__topbar">
      <a class="mobile-shell__brand" href="/">LK</a>
      <slot name="actions" />
    </header>
    <main class="mobile-shell__content">
      <slot />
    </main>
    <BottomNav :active="activeTab" :items="navItems" />
  </div>
</template>

<script setup>
import BottomNav from './BottomNav.vue'

defineProps({
  activeTab: {
    type: String,
    default: 'home'
  },
  navItems: {
    type: Array,
    default: undefined
  }
})
</script>
