<template>
  <v-app>
    <v-main style="height: 100%; display:flex; flex-direction: column;">
      <file-selector @fileLoadingSucceeded="renderData" @fileLoadingFailed="showMessage" />

      <bin-renderer ref="binRenderer" style="width: 100%; flex: 1;" :style="{ 'visibility': rendererVisibility }" />

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
import BinRenderer from './components/bin-renderer.vue';

export default {
  components: {
    FileSelector,
    BinRenderer
  },

  data: () => ({
    rendererVisibility: "hidden",
    message: "",
    dialog: false,
  }),

  methods: {
    async renderData(data) {
      this.rendererVisibility = "visible";
      this.$refs.binRenderer.renderData(data);
    },

    async showMessage(message) {
      this.message = message;
      this.dialog = true;
    }
  }
}
</script>
