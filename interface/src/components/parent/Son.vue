<script>
export default {
  // 父传子
  props:{
    msg:{
      type:String,
      default:'默认值'
    }
  }
}
</script>

<template>
  <div>{{msg}}</div>
  <!-- 子传父 -->
  <button @click="$emit('childEvent','有更新事件')">更新</button>
</template>

<style scoped>

</style>