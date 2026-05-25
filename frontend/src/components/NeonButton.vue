<template>
  <button class="neon-button" @click="$emit('click')">
    <span class="button-content">
      <el-icon v-if="icon"><component :is="icon" /></el-icon>
      <slot></slot>
    </span>
    <div class="button-glow"></div>
  </button>
</template>

<script setup>
defineProps({
  icon: {
    type: [String, Object],
    default: null
  }
})

defineEmits(['click'])
</script>

<style scoped>
.neon-button {
  position: relative;
  background: linear-gradient(135deg, rgba(0, 242, 254, 0.2), rgba(178, 36, 239, 0.2));
  border: 1px solid var(--neon-blue);
  border-radius: 8px;
  color: var(--neon-blue);
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.button-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.button-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 242, 254, 0.4), transparent);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.neon-button:hover {
  background: linear-gradient(135deg, rgba(0, 242, 254, 0.3), rgba(178, 36, 239, 0.3));
  box-shadow: 
    0 0 20px rgba(0, 242, 254, 0.4),
    0 0 40px rgba(0, 242, 254, 0.2),
    inset 0 0 20px rgba(0, 242, 254, 0.1);
  transform: translateY(-2px);
  color: white;
  border-color: var(--neon-purple);
}

.neon-button:hover .button-glow {
  width: 300px;
  height: 300px;
}

.neon-button:active {
  transform: translateY(0);
}
</style>
