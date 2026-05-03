<template>
  <div class="mobile-empty-state">
    <strong>{{ title }}</strong>
    <p>{{ copy.mobile.empty }}</p>
  </div>
</template>

<script setup>
import copy from '../../data/copy.json'

defineProps({
  title: {
    type: String,
    default: '空山新雨'
  }
})
</script>
