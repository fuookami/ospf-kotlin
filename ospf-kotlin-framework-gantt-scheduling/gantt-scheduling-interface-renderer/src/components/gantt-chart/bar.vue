<template>
  <v-container style="height: 3em; min-width: 0px; padding: 0;">
    <v-btn class="gantt_item" :class="{
      'normal_item': isNormal,
      'testing_item': isTesting,
      'unavailable_item': isUnavailable,
      'elevation-0': !isFocused,
      'evelation-1': isFocused,
      'focused_item': isFocused
    }" :style="{
      width: width + 'px',
      'max-width': width + 'px',
      height: mainHeight + 'em',
      'margin-bottom': mainMarginBottom + 'em'
    }" style="
      min-width: 0px;
      padding: 0;
    " @click="click()"
    >
      <span :style="{ width: width + 'px' }">
        {{ name }}
      </span>
      <v-tooltip activator="parent" location="top">
        <v-row v-for="(info, _) in infoList" style="margin: .25em 0 .25em 0; width: 20em;">
          <v-col cols="4" style="padding: 0;">{{ info.key }}</v-col>
          <v-col cols="1" style="padding: 0;">：</v-col>
          <v-col cols="7" style="padding: 0;">{{ info.value }}</v-col>
        </v-row>
      </v-tooltip>
      <v-snackbar v-model="snackbar" :timeout="2000">{{ snackbarText }}</v-snackbar>
    </v-btn>
    <v-container :style="{
      width: width + 'px',
      height: subHeight + 'em'
    }" style="
        margin: 0; 
        padding: 0; 
        position: relative; 
        min-width: 0px;
    ">
      <div v-for="(subItem, _) in subItems" class="gantt_item" :style="{
        left: subItem.x + 'px',
        width: subItem.width + 'px',
        'max-width': subItem.width + 'px',
        background: subItem.color
      }" style="
          height: 100%; 
          position: absolute; 
          text-align: center;
      ">
        <span :style="{ width: width + 'px' }" style="position: relative; top: -.75em; font-size: .5em; ">
          {{ subItem.name }}
        </span>
        <v-tooltip activator="parent" location="top">{{ subItem.name }}</v-tooltip>
      </div>
    </v-container>
  </v-container>
</template>

<script>
import "./bar.css"
import Color from 'color';
import * as BarColor from "./bar-color";
import useClipboard from 'vue-clipboard3'

const { toClipboard } = useClipboard()

function needSubBar(subItems) {
  if (subItems.length == 0) {
    return false;
  } else {
    for (const subItem of subItems) {
      if (subItem.startTime != subItem.endTime) {
        return true;
      }
    }
    return false;
  }
}

export default {
  data: () => ({
    name: "",
    infoList: [],
    linkedInfo: null,
    x: 0,
    y: 0,
    width: 0,
    mainHeight: 3,
    mainMarginBottom: 0,
    subHeight: 0,
    subItems: [],
    isNormal: false,
    isTesting: false,
    isUnavailable: false,
    isFocused: false,
    snackbarText: "",
    snackbar: false
  }),

  methods: {
    async init(ganttItem, widthPerUnit, linkedKey) {
      this.name = ganttItem.name;
      this.infoList = ganttItem.info;
      if (linkedKey != null) {
        this.linkedInfo = this.infoList.find((value) => value.key == linkedKey).value;
      }
      const duration = ganttItem.endTime - ganttItem.startTime;
      const width = duration * widthPerUnit;
      this.width = width;

      if (ganttItem.category == "Normal") {
        this.isNormal = true;
      } else if (ganttItem.category == "Testing") {
        this.isTesting = true;
      } else if (ganttItem.category == "Unavailable") {
        this.isUnavailable = true;
      }

      if (needSubBar(ganttItem.subItems)) {
        this.mainHeight = 2;
        this.mainMarginBottom = .25;
        this.subHeight = .75;

        for (let i in ganttItem.subItems) {
          const subItem = ganttItem.subItems[i];
          if (subItem.startTime == subItem.endTime) {
            continue;
          }

          const begin = subItem.startTime - ganttItem.startTime;
          const x = begin * widthPerUnit;
          const duration = subItem.endTime - subItem.startTime;
          const width = duration * widthPerUnit;

          this.subItems.push({
            name: subItem.name,
            x: x,
            width: width,
            color: Color(BarColor.getSubItemColor(subItem.category)).toString()
          })
        }
      }
    },

    async click() {
      try {
        await toClipboard(this.name);
        this.snackbarText = `${this.name} 复制成功`
      } catch(e) {
        this.snackbarText = `${this.name} 复制失败`
      }
      this.snackbar = true;
      if (this.linkedInfo != null) {
        this.$emit("focus", this.linkedInfo);
      }
    },

    async rescale(scale) {
      this.x = this.x * scale;
      this.width = this.width * scale;
      for (const item of this.subItems) {
        item.x = item.x * scale;
        item.width = item.width * scale;
      }
    },

    async setToFocus(linkedKey, linkedInfo) {
      if (this.infoList.find((value) => value.key == linkedKey).value == linkedInfo) {
        this.isFocused = true;
      } else {
        this.isFocused = false;
      }
    }
  }
}
</script>
