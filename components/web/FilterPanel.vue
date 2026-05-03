<template>
  <aside class="web-filter-panel">
    <h3>择文而读</h3>
    <button type="button" :class="{ 'is-active': category === 'latest' }" @click="$emit('change', { category: 'latest', sort })">全部</button>
    <button
      v-for="item in categories"
      :key="item.slug"
      type="button"
      :class="{ 'is-active': category === item.slug }"
      @click="$emit('change', { category: item.slug, sort })"
    >
      {{ item.displayName }}
    </button>
    <h3>排序</h3>
    <select :value="sort" @change="$emit('change', { category, sort: $event.target.value })">
      <option value="newest">最新发布</option>
      <option value="popular">最多点赞</option>
      <option value="featured">精选优先</option>
      <option value="pinned">置顶优先</option>
    </select>
  </aside>
</template>

<script setup>
defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  category: {
    type: String,
    default: 'latest'
  },
  sort: {
    type: String,
    default: 'newest'
  }
})

defineEmits(['change'])
</script>
