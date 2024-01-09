<template>
  <div style="width: 100%; height: 100%; display:flex; flex-direction: column;">
    <p class="transition-swing text-body-1" v-html="renderMessage" />
    <div class="d-flex flex-row">
      <v-select style="max-width: 15em;" v-model="selectedBin" label="选择货柜" :items="bins" no-data-text="没有货柜"
        hide-details />
      <p class="transition-swing text-body-2 align-self-end" v-html="selectedBinMessage" />
    </div>
    <v-row style="height: 100%; width: 100%; position: relative;">
      <v-card class="mx-auto" max-width="25em" :style="{ 'visibility': selectedItemInfoVisibility }"
        style="position: absolute; z-index: 1000; top: 1em; left: 1em; ">
        <v-card-text>
          <p v-html='`批次：${selectedItemLotNo}`' />
          <p style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">     
            物料编码：{{ selectedItemBomNo }}
            <v-tooltip activator="parent" location="top" max-width="500px">{{ selectedItemBomNo }}</v-tooltip>
          </p>
          <p style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
            物料名称：{{ selectedItemName }}
            <v-tooltip activator="parent" location="top" max-width="500px">{{ selectedItemName }}</v-tooltip>
          </p>
          <p v-html='`物料字首：${selectedItemPrefix}`' />
          <p v-html='`包装类型：${selectedItemPackageType}`' />
          <p v-html='`包装尺寸：${selectedItemSize}`' />
          <p v-html='`装载位置：${selectedItemPosition}`' />
          <p v-html='`装载顺序：${selectedItemLoadingOrder}`' />
          <p v-html='`箱数：${selectedItemAmount}`' />
          <p v-html='`重量：${selectedItemWeight}kg`' />
          <p v-html='`所在仓库：${selectedItemWarehouse}`' />
        </v-card-text>
      </v-card>

      <v-col cols="9" id="renderer" height="100%" />
      <v-col id="tabContainer" cols="3" height="100%">
        <v-tabs id="tabList" v-model="tab" color="deep-purple-accent-4" align-tabs="center">
          <v-tab :value="0">货物统计</v-tab>
          <v-tab :value="1">批次统计</v-tab>
          <v-tab :value="2">装柜步骤</v-tab>
        </v-tabs>

        <div :style="{ 'visibility': tabVisibility[0], 'height': tabHeight, 'width': tabWidth }"
          style="position: absolute; overflow-y: auto;">
        </div>

        <div :style="{ 'visibility': tabVisibility[1], 'height': tabHeight, 'width': tabWidth }"
          style="position: absolute; overflow-y: auto;">
          <v-table density="compact" :style="{ 'width': tabWidth }">
            <thead>
              <tr>
                <th class="text-center">批次</th>
                <th class="text-center">货物</th>
                <th class="text-center">数量</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="counter in lotCounter" :order='`${counter.order}`'>
                <td class="text-center">{{ counter.lot }}</td>
                <td class="text-center">{{ counter.model }}</td>
                <td class="text-center">{{ counter.amount }}</td>
              </tr>
            </tbody>
          </v-table>
        </div>

        <div :style="{ 'visibility': tabVisibility[2], 'height': tabHeight, 'width': tabWidth }"
          style="position: absolute; overflow: auto;">
          <v-table id="loadingStepTable" density="compact" style="table-layout: fixed;">
            <thead>
              <tr>
                <th class="text-center" style="width: 2em; padding: 0;">步骤</th>
                <th class="text-center" style="width: 2em; padding: 0;">数量</th>
                <th class="text-center" :style="{ 'width': loadingStepNameWidth }">汇总信息</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="step in loadingStep" :order='`${step.order}`' style="background-color: '#00000050;">
                <td class="text-center" style="width: 2em;">{{ step.order }}</td>
                <td class="text-center" style="width: 2em;">{{ step.amount }}</td>
                <td :style="{ 'width': loadingStepNameWidth }"
                  style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ step.name }}</td>
              </tr>
            </tbody>
          </v-table>
        </div>
      </v-col>
    </v-row>
  </div>
</template>

<script>
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

const itemColors = [
  '#01DDFF',
  '#5EF031',
  '#FF9600',
  '#FFDB01',
  '#F25A26',
  '#B768EB',
  '#18CB5F',
  '#854FFF',
  '#EC4141',
  '#425FFF',
  '#12C4C7',
  '#B82BE0',
  '#33ECB7',
  '#F9D215',
  '#009BEC',
  '#7233FE',
  '#B99AFE',
  '#81E267',
  '#9AD2FF',
  '#C0EA00',
  '#FEE89A',
  '#4E62AB',
  '#9E0142',
  '#469EB4',
  '#D6404E',
  '#87CFA4',
  '#F57547',
  '#CBE99D',
  '#FDB96A',
  '#F5FBB1'
];

function getBinKeys(bins) {
  let binKeys = [];
  for (const binKey in bins) {
    binKeys.push(binKey);
  }
  return binKeys;
}

function getItemType(itemData) {
  return `${itemData.itemPrefix}-${itemData.bomNo}-${itemData.packageType}-${itemData.warehouse}-${itemData.width.toFixed(0)}*${itemData.height.toFixed(0)}*${itemData.depth.toFixed(0)}-${itemData.weight.toFixed(2)}`;
}

function getItemsInfo(itemsData = []) {
  var counter = 0;
  const items = {};
  for (const itemData of itemsData) {
    const type = getItemType(itemData);
    if (type in items) {
      items[type].amount += 1;
    } else {
      items[getItemType(itemData)] = {
        amount: 1,
        order: counter
      };
      ++counter;
    }
  }
  return items;
}

function getLotCounters(itemsData = []) {
  let lotCounter = {};
  for (const itemData of itemsData) {
    if (itemData.lotNo in lotCounter) {
      // todo
      lotCounter[itemData.lotNo].amount += 1;
    } else {
      lotCounter[itemData.lotNo] = {
        lot: itemData.lotNo,
        model: "",
        amount: 1
      }
    }
  }

  let ret = [];
  for (const lotNo in lotCounter) {
    let counter = lotCounter[lotNo];
    ret.push({
      order: 0,
      lot: counter.lot,
      model: counter.model,
      amount: counter.amount
    })
  }
  ret.sort((lhs, rhs) => lhs.amount > rhs.amount);
  return ret;
}

function getLoadingSteps(itemsData = []) {
  var maxStep = 0;
  for (const itemData of itemsData) {
    if (itemData.loadingOrder > maxStep) {
      maxStep = itemData.loadingOrder;
    }
  }

  const steps = [];
  for (var i = 0; i <= maxStep; i++) {
    const names = [];
    for (const itemData of itemsData) {
      if (itemData.loadingOrder == i) {
        names.push(itemData.name);
      }
    }
    steps.push({
      order: i,
      amount: names.length,
      name: Object.entries(names.reduce((counter, currentValue) => {
        if (currentValue in counter) {
          counter[currentValue]++;
        } else {
          counter[currentValue] = 1;
        }
        return counter;
      }, {})).map((val, index) => {
        return `${val[0]} * ${val[1]}`
      }).join(";")
    });
  }
  return steps;
}

function createItems(itemsData = [], binData, items) {
  const cubes = new Array();
  for (const itemData of itemsData) {
    const color = new THREE.Color(itemColors[items[getItemType(itemData)].order % itemColors.length]);
    const darkenColor = darken(color, 0.33);
    const geometry = new THREE.BoxGeometry(itemData.width, itemData.height, itemData.depth);
    const material = new THREE.MeshBasicMaterial({
      color: color,
      // opacity: 0.7, 
      transparent: true,
      depthWrite: false
    });
    const cube = new THREE.Mesh(geometry, material);
    const edges = new THREE.EdgesGeometry(geometry);
    const line = new THREE.Line(edges, new THREE.LineBasicMaterial({
      color: darkenColor,
      linewidth: 1
    }));
    cube.add(line);
    cube.position.set(
      itemData.x + itemData.width / 2 - binData.width / 2,
      itemData.y + itemData.height / 2 - binData.height / 2,
      itemData.z + itemData.depth / 2 - binData.depth / 2
    );
    cube.item = itemData;
    cubes.push(cube);
  }
  return cubes;
}

function createBinLines(bin) {
  const axesMaterial = new THREE.LineBasicMaterial({
    color: new THREE.Color('#3B65AC'),
    linewidth: 5
  });
  const scaleMaterial = new THREE.LineBasicMaterial({
    color: new THREE.Color('#3B65AC'),
    linewidth: 10
  });
  const lineLen = 200;
  const gap = 500;
  const scale = 1000;

  const lines = new Array();

  // x axes
  lines.push(new THREE.Line(new THREE.BufferGeometry().setFromPoints([
    new THREE.Vector3(-bin.width / 2, -bin.height / 2, bin.depth / 2 + gap),
    new THREE.Vector3(bin.width / 2, -bin.height / 2, bin.depth / 2 + gap)
  ]), axesMaterial));

  for (let x = 0; ; x = Math.min(x + scale, bin.width)) {
    lines.push(new THREE.Line(new THREE.BufferGeometry().setFromPoints([
      new THREE.Vector3(x - bin.width / 2, -bin.height / 2, bin.depth / 2 + gap - lineLen / 2),
      new THREE.Vector3(x - bin.width / 2, -bin.height / 2, bin.depth / 2 + gap + lineLen / 2)
    ]), scaleMaterial));

    if (x == bin.width) {
      break;
    }
  }

  // y axes
  lines.push(new THREE.Line(new THREE.BufferGeometry().setFromPoints([
    new THREE.Vector3(bin.width / 2 + gap, -bin.height / 2, -bin.depth / 2),
    new THREE.Vector3(bin.width / 2 + gap, bin.height / 2, -bin.depth / 2)
  ]), axesMaterial));

  for (let y = 0; ; y = Math.min(y + scale, bin.height)) {
    lines.push(new THREE.Line(new THREE.BufferGeometry().setFromPoints([
      new THREE.Vector3(bin.width / 2 + gap - lineLen / 2, y - bin.height / 2, -bin.depth / 2),
      new THREE.Vector3(bin.width / 2 + gap + lineLen / 2, y - bin.height / 2, -bin.depth / 2),
    ]), scaleMaterial));

    if (y == bin.height) {
      break;
    }
  }

  // z axes
  lines.push(new THREE.Line(new THREE.BufferGeometry().setFromPoints([
    new THREE.Vector3(bin.width / 2 + gap, -bin.height / 2, -bin.depth / 2),
    new THREE.Vector3(bin.width / 2 + gap, -bin.height / 2, bin.depth / 2)
  ]), axesMaterial));

  for (let z = 0; ; z = Math.min(z + scale, bin.depth)) {
    lines.push(new THREE.Line(new THREE.BufferGeometry().setFromPoints([
      new THREE.Vector3(bin.width / 2 + gap - lineLen / 2, -bin.height / 2, z - bin.depth / 2),
      new THREE.Vector3(bin.width / 2 + gap + lineLen / 2, -bin.height / 2, z - bin.depth / 2),
    ]), scaleMaterial));

    if (z == bin.depth) {
      break;
    }
  }

  return lines;
}

function createCamera(scene, window, bin) {
  const width = window.offsetWidth;
  const height = window.offsetHeight;

  const camera = new THREE.PerspectiveCamera(
    50,
    width / height,
    20,
    bin.depth * 10
  );
  camera.position.x = bin.width * 1.5;
  camera.position.y = bin.height * 1.5;
  camera.position.z = bin.depth * 1.25;
  camera.lookAt(scene.position);
  return camera;
}

function lighten(color, offset) {
  const hsl = { h: 0, s: 0, l: 0 };
  color.getHSL(hsl);
  return color.clone().offsetHSL(0, 0, hsl.l * offset);
}

function darken(color, offset) {
  const hsl = { h: 0, s: 0, l: 0 };
  color.getHSL(hsl);
  return color.clone().offsetHSL(0, 0, -hsl.l * offset);
}

export default {
  data: () => ({
    binRenderVisibility: "hidden",
    renderMessage: "",
    bins: [],
    binData: {},
    selectedBin: "",
    selectedBinMessage: "",

    tab: null,
    items: {},
    lotCounter: [],
    loadingStep: [],
    selectedItemInfoVisibility: "hidden",
    selectedItemLotNo: "",
    selectedItemBomNo: "",
    selectedItemPrefix: "",
    selectedItemName: "",
    selectedItemPackageType: "",
    selectedItemSize: "",
    selectedItemPosition: "",
    selectedItemLoadingOrder: "",
    selectedItemAmount: "",
    selectedItemWeight: "",
    selectedItemWarehouse: "",
    tabVisibility: ["hidden", "hidden", "hidden"],
    tabHeight: '500px',
    tabWidth: '500px',
    loadingStepNameWidth: '160px'
  }),

  watch: {
    selectedBin(binKey) {
      if (binKey != undefined) {
        const bin = this.binData[binKey];
        this.selectedBinMessage = `装载率：${(bin.loadingRate * 100.0).toFixed(2)}%  体积：${bin.volume.toFixed(2)}m3  重量：${bin.weight.toFixed(2)}吨`;
        this.binRenderVisibility = "visible";
        this.renderBinData(bin);
      }
    },

    tab(newTab) {
      if (newTab == null || newTab >= this.tabVisibility.length) {
        for (var i = 0; i != this.tabVisibility.length; i++) {
          this.tabVisibility[i] = "hidden";
        }
      } else {
        for (var i = 0; i != this.tabVisibility.length; i++) {
          if (i == newTab) {
            this.tabVisibility[i] = "visible";
          } else {
            this.tabVisibility[i] = "hidden";
          }
        }
      }
    }
  },

  methods: {
    originColor(item) {
      return itemColors[this.items[getItemType(item)].order % itemColors.length];
    },

    async renderData(data) {
      this.binRenderVisibility = "hidden";
      this.renderMessage = data.message;
      this.bins = getBinKeys(data.bins);
      this.binData = data.bins;
      this.selectedBin = undefined;
    },

    async renderBinData(binData) {
      this.tabHeight = `${document.getElementById("tabContainer").offsetHeight - document.getElementById("tabList").offsetHeight}px`;
      this.tabWidth = `${document.getElementById("tabContainer").offsetWidth}px`;
      this.loadingStepNameWidth = `${this.tabWidth - 32}px`;
      this.selectedItemInfoVisibility = "hidden";

      const window = document.getElementById("renderer");
      window.innerHTML = '';

      const renderer = new THREE.WebGLRenderer();
      const scene = new THREE.Scene();
      scene.background = new THREE.Color('#e7e7e7');

      this.items = getItemsInfo(binData.items);
      this.lotCounter = getLotCounters(binData.items);
      this.loadingStep = getLoadingSteps(binData.items);
      for (const item of createItems(binData.items, binData, this.items)) {
        scene.add(item);
      }
      for (const line of createBinLines(binData)) {
        scene.add(line);
      }

      const light = new THREE.AmbientLight(new THREE.Color('#999999'))
      const directionalLight = new THREE.DirectionalLight(new THREE.Color('#ffffff'), 1.0)
      directionalLight.position.set(scene.position)
      scene.add(light)
      scene.add(directionalLight)

      renderer.setSize(window.offsetWidth, window.offsetHeight);
      const camera = createCamera(scene, window, binData);
      renderer.render(scene, camera);
      window.append(renderer.domElement);

      window.addEventListener("resize", (event) => {
        if (this.renderer != null) {
          const window = document.getElementById("renderer");
          this.renderer.setSize(window.offsetWidth, window.offsetHeight);
          this.camera.aspect = window.offsetWidth / window.offsetHeight;
          this.camera.updateProjectionMatrix();
        }
      });

      const control = new OrbitControls(camera, renderer.domElement);
      control.saveState();
      function animate() {
        requestAnimationFrame(animate);
        renderer.render(scene, camera);
      };

      window.addEventListener("dblclick", (event) => {
        event.preventDefault();
        const raycaster = new THREE.Raycaster();
        const mouse = new THREE.Vector2();
        mouse.x = (event.offsetX / window.offsetWidth) * 2 - 1;
        mouse.y = -(event.offsetY / window.offsetHeight) * 2 + 1;
        raycaster.setFromCamera(mouse, camera);
        const intersects = raycaster.intersectObjects(scene.children);
        if (intersects.length != 0 && intersects[0].object instanceof THREE.Mesh) {
          const selectedItem = intersects[0].object;
          const type = getItemType(selectedItem.item);

          selectedItem.material.opacity = 1.0;
          selectedItem.material.color = lighten(new THREE.Color(this.originColor(selectedItem.item)), 0.2);
          for (const obj of scene.children) {
            if (obj instanceof THREE.Mesh) {
              for (const line of obj.children) {
                line.material.visible = true;
              }
              if (getItemType(obj.item) == type && selectedItem != obj) {
                obj.material.opacity = 0.9;
                obj.material.color = new THREE.Color(this.originColor(obj.item));
              } else if (selectedItem != obj) {
                obj.material.opacity = 0.3;
                obj.material.color = new THREE.Color(this.originColor(obj.item));
              }
            }
          }

          this.selectedItemInfoVisibility = "visible";
          this.selectedItemLotNo = selectedItem.item.lotNo;
          this.selectedItemBomNo = selectedItem.item.bomNo;
          this.selectedItemPrefix = selectedItem.item.itemPrefix;
          this.selectedItemName = selectedItem.item.name;
          this.selectedItemPackageType = selectedItem.item.packageType;
          this.selectedItemSize = `${selectedItem.item.depth.toFixed(0)}*${selectedItem.item.width.toFixed(0)}*${selectedItem.item.height.toFixed(0)}`;
          this.selectedItemPosition = `${selectedItem.item.x.toFixed(0)},${selectedItem.item.y.toFixed(0)},${selectedItem.item.z.toFixed(0)}`;
          this.selectedItemLoadingOrder = `${selectedItem.item.loadingOrder}`;
          this.selectedItemAmount = `${this.items[type].amount}`;
          this.selectedItemWeight = `${selectedItem.item.weight.toFixed(2)}`;
          this.selectedItemWarehouse = selectedItem.item.warehouse;
        } else {
          control.reset();
          for (const obj of scene.children) {
            if (obj instanceof THREE.Mesh) {
              for (const line of obj.children) {
                line.material.visible = true;
              }
              obj.material.opacity = 1.0;
              obj.material.color = new THREE.Color(this.originColor(obj.item));
            }
          }

          this.selectedItemInfoVisibility = "hidden";
        }
      });

      document.getElementById("loadingStepTable").addEventListener("click", (event) => {
        var target = event.target;
        if (target.nodeName == "TD") {
          target = target.parentNode;
        }
        const selectedOrder = target.getAttribute("order");
        for (const obj of scene.children) {
          if (obj instanceof THREE.Mesh) {
            if (obj.item.loadingOrder <= selectedOrder) {
              for (const line of obj.children) {
                line.material.visible = true;
              }
              obj.material.opacity = obj.item.loadingOrder == selectedOrder ? 1.0 : 0.3;
              obj.material.color = new THREE.Color(this.originColor(obj.item));
            } else {
              for (const line of obj.children) {
                line.material.visible = false;
              }
              obj.material.opacity = 0.0;
              obj.material.color = new THREE.Color(this.originColor(obj.item));
            }
          }
        }
      });

      animate();
    }
  }
}
</script>
