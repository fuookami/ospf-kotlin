<template>
  <v-container fluid style="margin: 0; padding: 0;">
    <v-container ref="toolbar" fluid class="d-flex flex-row-reverse" style="margin: 0; padding: 0;">
      <v-btn density="compact" icon="mdi-plus" style="margin-right: .5em;" @click="rescale(1)" />
      <v-btn density="compact" icon="mdi-minus" style="margin-right: .5em;" @click="rescale(-1)" />
      <v-select v-model="visibleLines" :items="lines" label="可见行" multiple="true">
        <template v-slot:prepend-item>
          <v-list-item title="选择全部" @click="setAllLineVisible"/>
          <v-list-item title="选择关联行" @click="setLinkedLineVisible"/>
        </template>
      </v-select>
    </v-container>
    <gantt-chart ref="ganttChart" :style="{ height: height + 'px' }" @focus="focus"/>
  </v-container>
</template>

<script>
import GanttChart from "./gantt-chart/chart.vue"
import { WebviewWindow, PhysicalSize } from '@tauri-apps/api/window'

function selectLinkItems(lines, linkedKey, linkedInfo) {
  const ret = [];
  for (const line of lines) {
    const items = [];
    for (const item of line.items) {
      if (item.info.find((value) => value.key == linkedKey).value == linkedInfo) {
        items.push(item);
      }
    }

    if (items.length > 0) {
      ret.push({
        name: line.name,
        category: line.category,
        items: items
      });
    }
  }
  return ret;
}

function selectedLinkedLines(lines, linkedKey, linkedInfo) {
  const ret = [];
  for (const line of lines) {
    if (line.items.find((item) => item.info.find((value) => value.key == linkedKey).value == linkedInfo)) {
      ret.push(line.name);
    }
  }
  return ret;
}

export default {
  components: {
    GanttChart
  },

  data: () => ({
    data: null,
    height: 0,
    minScale: 0,
    maxScale: 0,
    scale: 0,
    lines: [],
    visibleLines: [],
    linkedLines: [],
    currentFocusSubChart: null
  }),

  watch: {
    visibleLines(newVisibleLines) {
      console.log(newVisibleLines);
      this.$refs.ganttChart.setVisibleLines(newVisibleLines);
    }
  },

  methods: {
    async renderData(data) {
      this.data = data;
      this.height = this.$el.offsetHeight - this.$refs.toolbar.$el.offsetHeight;
      this.$refs.ganttChart.init(data, this.$el.offsetWidth, this.height);
      this.minScale = 0 - Math.floor(this.$refs.ganttChart.scales.length / 2);
      this.maxScale = 0 + Math.floor(this.$refs.ganttChart.scales.length / 2);
      this.scale = 0;

      this.lines = [];
      for (const line of data.lines) {
        this.lines.push(line.name);
      }
      this.lines.sort(function (lhs, rhs) {
        if (lhs < rhs) {
          return -1;
        } else if (lhs > rhs) {
          return 1;
        } else {
          return 0;
        }
      });

      this.$nextTick(function () {
        this.visibleLines = this.lines;
      });
    },

    async resize(width, height) {
      if (this.$refs.ganttChart != undefined) {
        this.height = height - this.$refs.toolbar.$el.offsetHeight;
        this.$refs.ganttChart.resize(width, this.height);
      }
    },

    async rescale(diff) {
      let newScale = this.scale + diff;
      if (newScale > this.maxScale) {
        newScale = this.maxScale;
      }
      if (newScale < this.minScale) {
        newScale = this.minScale;
      }
      if (newScale != this.scale) {
        this.$refs.ganttChart.rescale(this.scale - this.minScale, newScale - this.minScale);
      }
      this.scale += diff;
    },

    async focus(linkedInfo) {
      this.linkedLines = selectedLinkedLines(this.data.lines, this.data.link_info, linkedInfo);
      // const lines = selectLinkItems(this.data.lines, this.data.link_info, linkedInfo);
      // const thisData = {
      //   start_time: this.data.start_time,
      //   end_time: this.data.end_time,
      //   lines: lines,
      //   link_info: null
      // };

      // if (this.currentFocusSubChart != null) {
      //   await this.currentFocusSubChart.close();
      //   this.currentFocusSubChart = null;
      // }

      // const webView = new WebviewWindow("subChart", {
      //   url: "/src/components/gantt-chart/sub-chart.html",
      //   width: 1280,
      //   height: 720,
      //   title: `${linkedInfo}`
      // });
      // this.currentFocusSubChart = webView;

      // webView.once('tauri://created', function () {
      //   webView.emit("renderSubChart", {
      //     data: thisData, 
      //     width: 1280, 
      //     height: 720
      //   });
      // });

      // webView.once('tauri://error', function (e) {
      //   console.log(e);
      // });
    },

    async setAllLineVisible() {
      this.visibleLines = this.lines;
    },

    async setLinkedLineVisible() {
      this.visibleLines = this.linkedLines;
    }
  }
}
</script>
