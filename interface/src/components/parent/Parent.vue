<script>
// 子组件
import Son from "@/components/parent/Son.vue";

export default {
  name: 'Parent',
  components: {Son},
  data() {
    return {
      msg: 'Parent To  Hello World'
    }
  },
  methods: {
    handleChildEvent(msg) {
      alert(msg)
    }
  },
  Components: {Son}
}
</script>

<template>
  <!--父组件 对消息进行监听-->
  <Son @childEvent="handleChildEvent" :msg="msg"/>
</template>

<style scoped>

</style>