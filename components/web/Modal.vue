<template>
  <Teleport to="body">
    <div v-if="open" class="web-modal" role="dialog" aria-modal="true" @click.self="$emit('close')">
      <article class="web-modal__panel">
        <button type="button" class="web-modal__close" @click="$emit('close')">×</button>
        <div class="web-modal__tags">
          <Tag v-if="article?.isFeatured" text="精选" color="gold" />
          <Tag v-if="article?.isPinned" text="置顶" color="red" />
        </div>
        <h2>{{ article?.title }}</h2>
        <p>{{ article?.summary }}</p>
        <footer>
          <button type="button" @click="$emit('like', article)">点赞</button>
          <button type="button" @click="$emit('comment', article)">评论</button>
        </footer>
      </article>
    </div>
  </Teleport>
</template>

<script setup>
import Tag from './Tag.vue'

defineProps({
  open: Boolean,
  article: {
    type: Object,
    default: null
  }
})

defineEmits(['close', 'like', 'comment'])
</script>
