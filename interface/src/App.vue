<template>
  <div id="app">
    <img alt="Vue logo" src="./assets/logo.png">
    <HelloWorld msg="Welcome to Your Vue.js App"/>
  </div>
</template>

<script>

// 导入局部组件
import HelloWorld from './components/HelloWorld.vue'

export default {
  // 导出的是当前组件的配置项
  // 里面可以提供 data methods computed watch 生命周期钩子函数
  name: 'App',

  // 局部注册组件
  components: {
    HelloWorld
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
