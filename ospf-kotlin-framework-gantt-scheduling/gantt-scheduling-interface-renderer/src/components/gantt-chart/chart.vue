<template>
  <div class="gantt_header" ref="header" :style="{ width: width + 'px' }">
    <div class="gantt_meta_line" ref="metaLine" :style="{ width: ganttWidth + 'px', 'margin-left': maxNameWidth + 'em' }">
      <p v-for="(header, _) in metaHeaders" :style="{ 'min-width': header.width + 'px' }">
        {{ header.name }}
      </p>
    </div>

    <div class="gantt_meta_line" ref="metaAssistantLine"
      :style="{ width: ganttWidth + 'px', 'margin-left': maxNameWidth + 'em' }">
      <p v-for="(header, _) in metaSubHeaders" :style="{ 'min-width': header.width + 'px' }">
        {{ header.name }}
      </p>
    </div>
  </div>

  <div :style="{ width: width + 'px' }" style="padding: 0; display: flex;">
    <div class="gantt_actor" ref="actor" :style="{ height: chartHeight + 'px' }" @mousewheel.prevent>
      <p v-for="(line, _) in lines" 
        :style="{
          width: maxNameWidth + 'em',
          'line-height': line.height,
          display: line.visible
        }"
      >
        {{ line.name }}
      </p>
    </div>
    <div ref="chart" 
      :style="{width: chartWidth + 'px', height: chartHeight + 'px'}"
      style="overflow-x: auto; overflow-y: auto;" @scroll="chartScroll()"
    >
      <div v-for="(line, _) in lines" 
        :style="{
          width: ganttWidth + 'px',
          display: line.visible
        }" style="
          min-height: 3.5em;
          padding: .25em 0 .25em 0;
          border-bottom: 1px dotted #5a5a5a;
      "
      >
        <gantt-line ref="line" @focus="focus"/>
      </div>
    </div>
  </div>
</template>

<script>
import "./chart.css"
import dayjs from "dayjs"
import duration from "dayjs/plugin/duration"
import GanttLine from "./line.vue"

const minWidthPerHour = 32;
const basicScale = [1 / 12, 1 / 8, 1 / 6, 1 / 4, 1 / 3, 1 / 2, 1, 2, 4, 8, 16, 32, 64];

dayjs.extend(duration);

function dump(item, startTime) {
  const subItems = [];
  for (const subItem of item.sub_items) {
    subItems.push({
      name: subItem.name,
      category: subItem.category,
      startTime: dayjs.duration(dayjs(subItem.start_time, "%Y-%m-%d %H:%M:%S").diff(startTime)).asHours(),
      endTime: dayjs.duration(dayjs(subItem.end_time, "%Y-%m-%d %H:%M:%S").diff(startTime)).asHours()
    })
  }
  return {
    name: item.name,
    category: item.category,
    scheduledStartTime: dayjs.duration(dayjs(item.scheduled_start_time, "%Y-%m-%d %H:%M:%S").diff(startTime)).asHours(),
    scheduledEndTime: dayjs.duration(dayjs(item.scheduled_end_time, "%Y-%m-%d %H:%M:%S").diff(startTime)).asHours(),
    startTime: dayjs.duration(dayjs(item.start_time, "%Y-%m-%d %H:%M:%S").diff(startTime)).asHours(),
    endTime: dayjs.duration(dayjs(item.end_time, "%Y-%m-%d %H:%M:%S").diff(startTime)).asHours(),
    info: item.info,
    subItems: subItems
  };
}

function generateHeader(startTime, endTime, widthPerHour, stepHours) {
  let diffHours = dayjs.duration(endTime.diff(startTime)).asHours();
  const startDate = startTime.startOf("day");
  const endDate = endTime.startOf("day");
  let dayHours = startTime.hour();
  const metaSubHeaders = [];
  const metaHeaders = [];
  for (let date = startDate; date <= endDate; date = date.add(1, "day")) {
    let hours = Math.min(diffHours, 24 - dayHours);
    if (hours == 0) {
      break;
    }
    const width = widthPerHour * hours;

    if (hours % stepHours != 0 && dayHours != 0) {
      const thisStepHours = stepHours + hours % stepHours;
      if (hours == thisStepHours) {
        metaSubHeaders.push({
          width: width,
          name: `${dayHours % 24}`
        });
      } else {
        metaSubHeaders.push({
          width: widthPerHour * thisStepHours,
          name: `${dayHours % 24}`
        });
      }
      hours -= thisStepHours;
      diffHours -= thisStepHours;
      dayHours = (dayHours + thisStepHours) % 24;
    }

    for (let j = 0; j < hours;) {
      const thisStepHours = Math.min(hours - j, stepHours);
      metaSubHeaders.push({
        width: widthPerHour * thisStepHours,
        name: `${dayHours % 24}`
      });
      j += thisStepHours;
      dayHours = (dayHours + thisStepHours) % 24
    }
    metaHeaders.push({
      width: width,
      name: `${date.month() + 1}-${date.date()}`
    });
    dayHours = 0;
    diffHours -= hours;
  }
  return [metaSubHeaders, metaHeaders];
}

export default {
  components: {
    GanttLine
  },

  data: () => ({
    width: 0,
    chartWidth: 0,
    chartHeight: 0,
    ganttWidth: 0,
    ganttHeight: 0,
    diffHours: 0,
    lines: [],
    maxNameWidth: 0,
    metaHeaders: [],
    metaSubHeaders: [],
    widthPerHour: minWidthPerHour,
    startTime: null,
    endTime: null,
    stepHours: 1,
    scales: basicScale,
    linkedKey: null,
  }),

  methods: {
    async init(data, width, height) {
      this.width = width;
      this.chartHeight = height - this.$refs.metaLine.offsetHeight - this.$refs.metaAssistantLine.offsetHeight;
      this.ganttHeight = 0;
      this.linkedKey = data.link_info;

      this.lines = [];
      this.maxNameWidth = 0;
      this.metaHeaders = [];
      this.metaSubHeaders = [];

      this.startTime = dayjs(data.start_time, "%Y-%m-%d %H:%M:%S").subtract(1, "hour").startOf("hour");
      this.endTime = dayjs(data.end_time, "%Y-%m-%d %H:%M:%S").add(2, "hour").startOf("hour");
      let diffHours = dayjs.duration(this.endTime.diff(this.startTime)).asHours();

      this.widthPerHour = Math.ceil(Math.max(minWidthPerHour, width / diffHours));
      this.ganttWidth = this.widthPerHour * diffHours;

      const [metaSubHeaders, metaHeaders] = generateHeader(this.startTime, this.endTime, this.widthPerHour, this.stepHours);
      this.metaSubHeaders = metaSubHeaders;
      this.metaHeaders = metaHeaders;

      data.lines.sort(function (lhs, rhs) {
        if (lhs.name < rhs.name) {
          return -1;
        } else if (lhs.name > rhs.name) {
          return 1;
        } else {
          return 0;
        }
      });
      for (const line of data.lines) {
        this.maxNameWidth = Math.max(this.maxNameWidth, line.name.length);
        this.lines.push({
          name: line.name,
          height: 16,
          visible: "visible"
        });
      }
      this.chartWidth = this.width - this.maxNameWidth * 16;

      this.$nextTick(function () {
        for (let i in data.lines) {
          const line = data.lines[i];
          const items = [];
          for (const item of line.items) {
            items.push(dump(item, this.startTime));
          }
          this.$refs.line[i].init(items, this.ganttWidth, this.widthPerHour, data.link_info);
        }
      })

      this.$nextTick(function () {
        for (let i in data.lines) {
          this.lines[i].height = this.$refs.line[i].height;
          this.ganttHeight += this.$refs.line[i].height * 16;
        }
      })
    },

    async chartScroll() {
      let maxScrollX = this.ganttWidth - this.width + this.maxNameWidth * 16;
      let maxScrollY = this.ganttHeight;
      if (this.$refs.chart.scrollLeft > maxScrollX) {
        this.$refs.chart.scrollLeft = maxScrollX;
      }
      if (this.$refs.chart.scrollTop > maxScrollY) {
        this.$refs.chart.scrollTop = maxScrollY;
      }
      this.$refs.header.scrollLeft = this.$refs.chart.scrollLeft;
      this.$refs.actor.scrollTop = this.$refs.chart.scrollTop;
    },

    async resize(width, height) {
      this.width = width;
      this.chartHeight = height - this.$refs.metaLine.offsetHeight - this.$refs.metaAssistantLine.offsetHeight;
      this.chartWidth = this.width - this.maxNameWidth * 16;

      if (this.startTime != null && this.endTime != null) {
        let diffHours = dayjs.duration(this.endTime.diff(this.startTime)).asHours();
        const newWidthPerHour = Math.ceil(Math.max(minWidthPerHour, this.width / diffHours));
        if (newWidthPerHour != this.widthPerHour) {
          const scale = newWidthPerHour / this.widthPerHour;
          this.ganttWidth = this.ganttWidth * scale;
          this.widthPerHour = newWidthPerHour;
          for (let i in this.lines) {
            this.$refs.line[i].rescale(scale);
          }
        }
        const [metaSubHeaders, metaHeaders] = generateHeader(this.startTime, this.endTime, newWidthPerHour, this.stepHours);
        this.metaSubHeaders = metaSubHeaders;
        this.metaHeaders = metaHeaders;
      }
    },

    async rescale(currentScale, newScale) {
      let scale = this.scales[newScale] / this.scales[currentScale];
      const newWidthPerHour = this.widthPerHour * scale;
      const newStepHours = Math.ceil(minWidthPerHour / newWidthPerHour);

      this.ganttWidth = this.ganttWidth * scale;
      this.widthPerHour = newWidthPerHour;
      for (let i in this.lines) {
        this.$refs.line[i].rescale(scale);
      }
      if (newStepHours == this.stepHours) {
        for (const header of this.metaHeaders) {
          header.width = header.width * scale;
        }
        for (const header of this.metaSubHeaders) {
          header.width = header.width * scale;
        }
      } else {
        this.startHours = newStepHours;
        const [metaSubHeaders, metaHeaders] = generateHeader(this.startTime, this.endTime, newWidthPerHour, newStepHours);
        this.metaSubHeaders = metaSubHeaders;
        this.metaHeaders = metaHeaders;
      }
    },

    async focus(linkedInfo) {
      this.$emit("focus", linkedInfo);
      for (let i in this.lines) {
        this.$refs.line[i].setToFocus(this.linkedKey, linkedInfo);
      }
    },

    async setVisibleLines(visibleLines) {
      for (const line of this.lines) {
        if (visibleLines.find((value) => line.name == value)) {
          line.visible = "";
        } else {
          line.visible = "none";
        }
      }
    }
  }
}
</script>
