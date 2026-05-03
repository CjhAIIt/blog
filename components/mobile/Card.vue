<template>
  <article class="mobile-card" :class="{ 'mobile-card--pinned': article.isPinned }" @click="$emit('open', article)">
    <div class="mobile-card__media" v-if="article.coverUrl">
      <img :src="article.coverUrl" :alt="article.title">
    </div>
    <div class="mobile-card__badges">
      <Tag v-if="article.isFeatured" text="精选" color="gold" />
      <Tag v-if="article.isPinned" text="置顶" color="red" />
    </div>
    <h3>{{ article.title }}</h3>
    <p>{{ article.summary }}</p>
    <footer>
      <span>{{ article.category }}</span>
      <span>{{ article.likeCount || 0 }} 赞</span>
    </footer>
  </article>
</template>

<script setup>
import Tag from './Tag.vue'

defineProps({
  article: {
    type: Object,
    required: true
  }
})

defineEmits(['open'])
</script>
