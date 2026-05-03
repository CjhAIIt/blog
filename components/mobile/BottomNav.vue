<template>
  <nav class="mobile-bottom-nav" aria-label="底部导航">
    <a v-for="item in items" :key="item.key" :href="item.href" :class="{ 'is-active': item.key === active }">
      <span class="mobile-bottom-nav__icon">{{ item.icon }}</span>
      <span>{{ item.label }}</span>
      <b v-if="item.badge" class="mobile-bottom-nav__badge">{{ item.badge }}</b>
    </a>
  </nav>
</template>

<script setup>
defineProps({
  active: {
    type: String,
    default: 'home'
  },
  items: {
    type: Array,
    default: () => [
      { key: 'home', label: '首页', href: '/', icon: '⌂' },
      { key: 'posts', label: '文章', href: '/posts', icon: '文' },
      { key: 'featured', label: '精选', href: '/posts?sort=featured', icon: '星' },
      { key: 'plans', label: '计划', href: '/plans', icon: '卷' },
      { key: 'space', label: '我的', href: '/space', icon: '人' }
    ]
  }
})
</script>
