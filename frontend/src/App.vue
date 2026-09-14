<template>
  <a-config-provider :locale="locale" :theme="theme">
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

const locale = ref(zhCN)

/**
 * antd 的主题令牌，全部取自 styles/tokens.css 的设计令牌。
 * 不这么做的话，antd 组件会带它自己的蓝色主色和 6px 圆角，
 * 混在这套低饱和绿灰 + 10~12px 圆角里会很突兀。
 */
const theme = ref({
  token: {
    colorPrimary: '#7C9A88',
    colorPrimaryHover: '#5D7A6A',
    colorPrimaryActive: '#3F5B4C',
    colorInfo: '#7C9A88',
    colorText: '#4A4640',
    colorTextSecondary: '#8C877E',
    colorTextPlaceholder: '#A8A29A',
    colorBorder: '#E8E4DC',
    colorBgContainer: '#FFFFFF',
    colorBgElevated: '#FFFFFF',
    colorFillQuaternary: '#F0F4F1',
    borderRadius: 10,
    borderRadiusLG: 12,
    borderRadiusSM: 8,
    controlHeight: 30,
    controlHeightSM: 26,
    fontSize: 12.5,
    fontFamily:
      '"Source Han Sans CN", "Noto Sans SC", "PingFang SC", "Microsoft YaHei", "微软雅黑", "Segoe UI", system-ui, sans-serif',
    boxShadowSecondary: '0 6px 18px rgba(89, 84, 74, 0.10)'
  },
  components: {
    Select: {
      optionSelectedBg: '#DDE5DF',
      optionSelectedColor: '#3F5B4C',
      optionActiveBg: '#F0F4F1',
      selectorBg: '#FFFFFF'
    },
    DatePicker: {
      cellHoverBg: '#F0F4F1',
      cellActiveWithRangeBg: '#DDE5DF',
      timeColumnHeight: 200,
      timeColumnWidth: 48
    }
  }
})
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
  margin: 0;
  padding: 0;
}
</style>
