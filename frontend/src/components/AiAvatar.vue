<template>
  <div 
    class="ai-avatar" 
    :class="{ 'is-angry': isAngry, 'is-loading': isLoading }"
    @click="handleClick"
  >
    <!-- 卡通脸 -->
    <div class="face">
      <!-- 眼睛 -->
      <div class="eyes">
        <div class="eye left" :class="{ 'is-blinking': isBlinking, 'is-angry-eye': isAngry }">
          <div class="eyeball"></div>
        </div>
        <div class="eye right" :class="{ 'is-blinking': isBlinking, 'is-angry-eye': isAngry }">
          <div class="eyeball"></div>
        </div>
      </div>
      
      <!-- 嘴巴 -->
      <div class="mouth" :class="{ 'is-angry-mouth': isAngry, 'is-loading-mouth': isLoading }">
        <div v-if="!isAngry && !isLoading" class="smile"></div>
        <div v-else-if="isAngry" class="angry-line"></div>
        <div v-else class="loading-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
      
      <!-- 腮红 -->
      <div v-if="!isAngry && !isLoading" class="blush left-blush"></div>
      <div v-if="!isAngry && !isLoading" class="blush right-blush"></div>
    </div>
    
    <!-- 加载状态光环 -->
    <div v-if="isLoading" class="loading-ring"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  isAngry: {
    type: Boolean,
    default: false
  },
  isLoading: {
    type: Boolean,
    default: false
  }
})

// 眨眼状态
const isBlinking = ref(false)
let blinkInterval = null

onMounted(() => {
  startBlinking()
})

onUnmounted(() => {
  if (blinkInterval) {
    clearInterval(blinkInterval)
  }
})

function startBlinking() {
  setTimeout(() => {
    blink()
  }, 2000)

  blinkInterval = setInterval(() => {
    if (!props.isAngry && !props.isLoading) {
      if (Math.random() > 0.3) {
        blink()
      }
    }
  }, 3000 + Math.random() * 2000)
}

function blink() {
  isBlinking.value = true
  setTimeout(() => {
    isBlinking.value = false
  }, 150)
}

// 点击处理：触发生气动画（由父组件控制）
function handleClick() {
  // 点击事件会冒泡到父组件，由父组件的handleAvatarClick处理
}
</script>

<style lang="scss" scoped>
.ai-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow: 0 1px 4px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
  
  &:hover {
    transform: scale(1.05);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  }

  &.is-angry {
    animation: shakeHead 0.5s ease-in-out;
    background: linear-gradient(135deg, #ff6b6b 0%, #ee5a52 100%);
    box-shadow: 0 2px 8px rgba(255, 107, 107, 0.4);
  }

  &.is-loading {
    animation: pulse 2s ease-in-out infinite;
  }

  .face {
    position: relative;
    width: 18px;
    height: 18px;
  }

  .eyes {
    display: flex;
    justify-content: space-between;
    position: absolute;
    top: 4px;
    left: 50%;
    transform: translateX(-50%);
    width: 10px;
  }

  .eye {
    width: 4px;
    height: 5px;
    background-color: white;
    border-radius: 50%;
    position: relative;
    overflow: hidden;
    transition: all 0.15s ease;

    .eyeball {
      width: 2.5px;
      height: 2.5px;
      background-color: #333;
      border-radius: 50%;
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      transition: all 0.15s ease;
    }

    &.is-blinking {
      height: 1px;
      padding: 0;

      .eyeball {
        opacity: 0;
      }
    }

    &.is-angry-eye {
      height: 5px;
      
      .eyeball {
        width: 3px;
        height: 3px;
        background-color: #ff4444;
        
        &::before,
        &::after {
          content: '';
          position: absolute;
          width: 5px;
          height: 1.5px;
          background-color: #ff4444;
          border-radius: 1px;
        }
        
        &::before {
          top: -3px;
          left: -1.5px;
          transform: rotate(-15deg);
        }
        
        &::after {
          top: -3px;
          right: -1.5px;
          transform: rotate(15deg);
        }
      }
    }
  }

  .mouth {
    position: absolute;
    bottom: 3px;
    left: 50%;
    transform: translateX(-50%);
    width: 7px;
    height: 3px;
    transition: all 0.2s ease;

    .smile {
      width: 7px;
      height: 3px;
      border-bottom: 1.5px solid white;
      border-radius: 0 0 6px 6px;
    }

    .angry-line {
      width: 8px;
      height: 1.5px;
      background-color: white;
      border-radius: 1px;
      transform: rotate(-5deg);
    }

    .loading-dots {
      display: flex;
      gap: 1px;
      justify-content: center;
      align-items: center;
      height: 3px;

      span {
        width: 1.5px;
        height: 1.5px;
        background-color: white;
        border-radius: 50%;
        animation: loadingDotBounce 1.4s infinite ease-in-out both;

        &:nth-child(1) { animation-delay: -0.32s; }
        &:nth-child(2) { animation-delay: -0.16s; }
      }
    }

    &.is-angry-mouth {
      transform: translateX(-50%) rotate(-10deg);
    }

    &.is-loading-mouth {
      transform: translateX(-50%) scale(1.1);
    }
  }

  .blush {
    position: absolute;
    width: 3px;
    height: 2px;
    background-color: rgba(255, 182, 193, 0.6);
    border-radius: 50%;
    top: 9px;

    &.left-blush {
      left: 1px;
    }

    &.right-blush {
      right: 1px;
    }
  }

  .loading-ring {
    position: absolute;
    width: 28px;
    height: 28px;
    border: 1.5px solid transparent;
    border-top: 1.5px solid rgba(255, 255, 255, 0.5);
    border-right: 1.5px solid rgba(255, 255, 255, 0.5);
    border-radius: 50%;
    animation: spin 1.5s linear infinite;
  }
}

@keyframes shakeHead {
  0%, 100% {
    transform: translateX(0) rotate(0deg);
  }
  20% {
    transform: translateX(-3px) rotate(-5deg);
  }
  40% {
    transform: translateX(3px) rotate(5deg);
  }
  60% {
    transform: translateX(-2px) rotate(-3deg);
  }
  80% {
    transform: translateX(2px) rotate(3deg);
  }
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  }
  50% {
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.5), 0 0 0 8px rgba(102, 126, 234, 0.1);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes loadingDotBounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}
</style>
