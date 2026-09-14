import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ConfigProvider, DatePicker, Select } from 'ant-design-vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import App from './App.vue'
import router from './router'
import 'ant-design-vue/dist/reset.css'
import './styles/global.css'

/*
  只注册实际用到的 antd 组件，不 app.use(Antd) 全量引入。

  原因：全量引入会把整个 antd 打进 bundle（实测 ant-design 块 1405 kB / gzip 415 kB）。
  本地加载不花流量，但这 1.4 MB 的 JS 每次启动都要完整解析一遍，是拖慢首屏的直接来源。
  实测改成按需注册后降到 439 kB / gzip 132 kB（降 69%），省下的时间给了渲染。
  本项目模板里只用到 4 个组件（ConfigProvider / Select / SelectOption / DatePicker）。

  Modal / message 不在这里注册：它们散落在各视图里以具名方式
  `import { Modal, message } from 'ant-design-vue'` 使用（命令式调用），
  具名导入本身就能被 tree-shake，不需要全局挂载。

  dayjs 的 zh-cn 语言包单独引入：ConfigProvider 的 locale 管选择器面板文案，
  而 weekStart / 星期名等由 dayjs 自己的 locale 决定，两者不能互相替代。
*/
dayjs.locale('zh-cn')

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.component('AConfigProvider', ConfigProvider)
app.component('ASelect', Select)
app.component('ASelectOption', Select.Option)
app.component('ADatePicker', DatePicker)

app.mount('#app')
