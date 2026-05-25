<template>
  <div class="cyber-loader">
    <div class="loader-ring"></div>
    <div class="loader-ring"></div>
    <div class="loader-ring"></div>
    <div class="loader-text">{{ text }}</div>
  </div>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default: '加载中...'
  }
})
</script>

<style scoped>
.cyber-loader {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.loader-ring {
  position: absolute;
  border-radius: 50%;
  border: 2px solid transparent;
  animation: rotate 2s linear infinite;
}

.loader-ring:nth-child(1) {
  width: 80px;
  height: 80px;
  border-top-color: var(--neon-blue);
  border-bottom-color: var(--neon-blue);
  animation-duration: 2s;
}

.loader-ring:nth-child(2) {
  width: 60px;
  height: 60px;
  border-left-color: var(--neon-purple);
  border-right-color: var(--neon-purple);
  animation-duration: 1.5s;
  animation-direction: reverse;
}

.loader-ring:nth-child(3) {
  width: 40px;
  height: 40px;
  border-top-color: var(--neon-pink);
  border-bottom-color: var(--neon-pink);
  animation-duration: 1s;
}

.loader-text {
  margin-top: 100px;
  color: var(--neon-blue);
  font-size: 14px;
  letter-spacing: 2px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}
</style>
