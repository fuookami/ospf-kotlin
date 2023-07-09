const subItemColors = [
  0x01DDFF,
  0x5EF031,
  0xFF9600,
  0xFFDB01,
  0xF25A26,
  0xB768EB,
  0x18CB5F,
  0x854FFF,
  0xEC4141,
  0x425FFF,
  0x12C4C7,
  0xB82BE0,
  0x33ECB7,
  0xF9D215,
  0x009BEC,
  0x7233FE,
  0xB99AFE,
  0x81E267,
  0x9AD2FF,
  0xC0EA00,
  0xFEE89A,
  0x4E62AB,
  0x9E0142,
  0x469EB4,
  0xD6404E,
  0x87CFA4,
  0xF57547,
  0xCBE99D,
  0xFDB96A,
  0xF5FBB1
];

const subItemMapper = [];

export function getSubItemColor(category) {
  let index = subItemMapper.indexOf(category);
  if (index == -1) {
    index = subItemMapper.length;
    subItemMapper.push(category);
  }
  return subItemColors[index % subItemColors.length];
}
