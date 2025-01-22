// 文件核心作用：导入App.vue 基于App.vue创建结构渲染index.html
// 导入Vue核心包
import Vue from 'vue'
// 导入 App.vue 根组件 (全局组件)
import App from './App.vue'

// 提示：当前处于什么环境（生产环境／开发环境）
Vue.config.productionTip = true

// 全局注册组件 大驼峰命名
Vue.component('app', App)

new Vue({
  render: h => h(App),
}).$mount('#app')
