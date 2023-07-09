<template>
  <v-container fluid style="margin: 0; padding: 0; display: flex; flex-direction: row;">
    <v-text-field style="flex: 1;" v-model="fileName" readonly clearable hide-details label="输入文件" />
    <v-btn class="align-self-center" @click="selectFile()">选择输入文件</v-btn>
  </v-container>
</template>

<script>
import { invoke } from '@tauri-apps/api/tauri';
import { open } from '@tauri-apps/api/dialog';

export default {
  data: () => ({
    fileName: ""
  }),

  methods: {
    async selectFile() {
      this.fileName = await open({
        title: "选择数据文件",
        directory: false,
        multiple: false,
        defaultPath: await invoke('get_current_directory',),
        filters: [{
          name: 'json',
          extensions: ['json']
        }]
      });
      invoke("load_data", { path: this.fileName }).then(
        (data) => this.$emit("fileLoadingSucceeded", data),
        (err) => this.$emit("fileLoadingFailed", err)
      );
    }
  }
}
</script>
