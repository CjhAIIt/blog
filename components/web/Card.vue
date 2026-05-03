<template>
  <article class="web-card" :class="{ 'web-card--pinned': article.isPinned }">
    <a class="web-card__media" :href="`/posts/${article.id}`" v-if="article.coverUrl">
      <img :src="article.coverUrl" :alt="article.title">
    </a>
    <div class="web-card__body">
      <div class="web-card__badges">
        <Tag v-if="article.isFeatured" text="精选" color="gold" />
        <Tag v-if="article.isPinned" text="置顶" color="red" />
      </div>
      <h3><a :href="`/posts/${article.id}`">{{ article.title }}</a></h3>
      <p>{{ article.summary }}</p>
      <footer>
        <span>{{ article.category }}</span>
        <a :href="`/posts/${article.id}`">{{ copy.web.readButton }}</a>
      </footer>
    </div>
  </article>
</template>

<script setup>
import copy from '../../data/copy.json'
import Tag from './Tag.vue'

defineProps({
  article: {
    type: Object,
    required: true
  }
})
</script>
