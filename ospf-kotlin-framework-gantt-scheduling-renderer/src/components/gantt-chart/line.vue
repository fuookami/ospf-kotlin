<template>
  <v-container :style="{ width: width + 'px', height: height + 'em' }" style=" margin: 0; padding: 0; position: relative;">
    <gantt-bar v-for="(item, _) of items" ref="bar" 
      :style="{ left: item.x + 'px', top: item.y + 'em' }" 
      style="position: absolute;"
      @focus="focus"
    />
  </v-container>
</template>

<script>
import GanttBar from "./bar.vue"

const baseLineHeight = 3.5;

function calculateHeights(items) {
  let ends = [0];
  let heights = [];
  for (let i in items) {
    let item = items[i];
    let start = item.startTime;
    let end = item.endTime;

    let flag = false;
    for (let j in ends) {
      if (start >= ends[j]) {
        heights.push(Number(j));
        ends[j] = end;
        flag = true;
        break;
      }
    }

    if (!flag) {
      heights.push(ends.length);
      ends.push(end);
    }
  }
  return heights;
}

export default {
  components: {
    GanttBar
  },

  data: () => ({
    width: 0,
    height: baseLineHeight,
    items: []
  }),

  methods: {
    async init(ganttItems, width, widthPerUnit, linkedKey) {
      this.width = width;
      const heights = calculateHeights(ganttItems);
      this.items = [];
      this.height = .25 + (Math.max.apply(Math, heights) + 1) * baseLineHeight;

      for (let i in ganttItems) {
        const item = ganttItems[i];
        const x = item.startTime * widthPerUnit;
        const y = 0.25 + heights[i] * baseLineHeight;
        this.items.push({
          x: x,
          y: y
        })
      }

      this.$nextTick(function () {
        for (let i in ganttItems) {
          const item = ganttItems[i];
          this.$refs.bar[i].init(item, widthPerUnit, linkedKey);
        }
      });
    },

    async rescale(scale) {
      for (const item of this.items) {
        item.x = item.x * scale;
      }
      for (let i in this.items) {
        this.$refs.bar[i].rescale(scale);
      }
    },

    async focus(linkInfo) {
      this.$emit("focus", linkInfo);
    },

    async setToFocus(linkedKey, linkedInfo) {
      for (let i in this.items) {
        this.$refs.bar[i].setToFocus(linkedKey, linkedInfo);
      }
    }
  }
}
</script>
