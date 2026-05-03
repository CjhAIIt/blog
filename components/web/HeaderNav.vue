<template>
  <header class="web-header-nav">
    <a class="web-header-nav__brand" href="/">LK 博客</a>
    <nav>
      <a v-for="item in items" :key="item.href" :href="item.href" :class="{ 'is-active': item.key === active }">
        {{ item.label }}
      </a>
    </nav>
    <slot />
  </header>
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
      { key: 'home', label: '首页', href: '/' },
      { key: 'posts', label: '文章', href: '/posts' },
      { key: 'featured', label: '精选', href: '/posts?sort=featured' },
      { key: 'plans', label: '计划', href: '/plans' },
      { key: 'about', label: '关于', href: '/about' }
    ]
  }
})
</script>
