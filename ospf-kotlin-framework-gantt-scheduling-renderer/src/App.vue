<template>
  <v-app>
    <v-main :style="{ height: height + 'px' }" style="display:flex; flex-direction: column;" v-resize="resized">
      <file-selector ref="fileSelector" @fileLoadingSucceeded="renderData" @fileLoadingFailed="showMessage" />
      <gantt-renderer ref="ganttRenderer" :style="{ 'visibility': rendererVisibility }" style="width: 100%; flex: 1;" />

      <v-dialog v-model="dialog" persistent max-width="30%">
        <v-card>
          <v-card-title class="headline">错误信息</v-card-title>
          <v-card-text v-html="message"></v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn color="green darken-1" text @click="dialog = false">关闭</v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </v-main>
  </v-app>
</template>

<script>
import FileSelector from './components/file-selector.vue';
import GanttRenderer from './components/gantt-renderer.vue';

export default {
  components: {
    FileSelector,
    GanttRenderer
  },

  data: () => ({
    height: 0,
    rendererVisibility: "hidden",
    message: "",
    dialog: false,
  }),

  mounted() {
    window.onresize = function () {
      this.height = window.height;
    }
  },

  methods: {
    async renderData(data) {
      this.rendererVisibility = "visible";
      this.$refs.ganttRenderer.renderData(data);
    },

    async showMessage(message) {
      this.message = message;
      this.dialog = true;
    },

    async resized(event) {
      this.$refs.ganttRenderer.resize(this.$refs.ganttRenderer.$el.offsetWidth, this.$el.offsetHeight - this.$refs.fileSelector.$el.offsetHeight);
    }
  }
}
</script>
