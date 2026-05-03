<template>
  <section class="mobile-filter-panel" :class="{ 'is-open': open }">
    <button type="button" class="mobile-filter-panel__handle" @click="open = !open">
      {{ open ? '收起筛选' : '筛选排序' }}
    </button>
    <div class="mobile-filter-panel__body">
      <label>
        分类
        <select v-model="localCategory" @change="emitChange">
          <option value="latest">全部</option>
          <option v-for="category in categories" :key="category.slug" :value="category.slug">{{ category.displayName }}</option>
        </select>
      </label>
      <label>
        排序
        <select v-model="localSort" @change="emitChange">
          <option value="newest">最新</option>
          <option value="popular">热门</option>
          <option value="featured">精选优先</option>
          <option value="pinned">置顶优先</option>
        </select>
      </label>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
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

const emit = defineEmits(['change'])
const open = ref(false)
const localCategory = ref(props.category)
const localSort = ref(props.sort)

function emitChange() {
  emit('change', { category: localCategory.value, sort: localSort.value })
}
</script>
