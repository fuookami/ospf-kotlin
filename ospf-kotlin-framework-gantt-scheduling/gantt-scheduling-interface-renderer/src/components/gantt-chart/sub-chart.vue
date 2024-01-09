<template>
  <v-app>
    <v-main :style="{ height: height + 'px' }" style="display:flex; flex-direction: column;" v-resize="resized">
      <gantt-chart ref="ganttChart" :style="{ height: height + 'px' }" />
    </v-main>
  </v-app>
</template>

<script>
import GanttChart from "./chart.vue";
import { invoke } from '@tauri-apps/api/tauri';

export default {
  components: {
    GanttChart
  },

  async mounted() {
    this.data = JSON.parse(await invoke('load_sub_chart_data',),);
    this.$refs.ganttChart.init(this.data.data, this.data.width, this.data.height);
  },

  data: () => ({
    data: null,
    height: 0
  }),

  methods: {
    async resized() {

    }
  }
}
</script>
