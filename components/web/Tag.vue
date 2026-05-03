<template>
  <span class="web-tag" :class="`web-tag--${color}`">{{ text }}</span>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    required: true
  },
  color: {
    type: String,
    default: 'neutral'
  }
})
</script>
