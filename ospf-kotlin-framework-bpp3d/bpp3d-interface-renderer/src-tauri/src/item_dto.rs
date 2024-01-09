use serde::{Serialize, Deserialize};
use std::collections::{ HashMap };

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ItemDTO {
    pub name: String,
    #[serde(rename = "packageType")]
    pub package_type: String,
    pub warehouse: String,
    pub width: f64,
    pub height: f64,
    pub depth: f64,
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub weight: f64,
    #[serde(rename = "loadingOrder")]
    pub loading_order: usize,
    pub info: HashMap<String, String>
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct BinDTO {
    #[serde(rename = "batchNo")]
    pub batch_no: Option<String>,
    pub order: usize,
    #[serde(rename = "typeCode")]
    pub type_code: String,
    pub width: f64,
    pub height: f64,
    pub depth: f64,
    #[serde(rename = "loadingRate")]
    pub loading_rate: f64,
    pub weight: f64,
    pub volume: f64,
    pub items: Vec<ItemDTO>
}

#[derive(Deserialize, Debug, Clone)]
pub struct ResponseDTO {
    #[serde(rename = "restAmount")]
    pub rest_amount: Option<u64>,
    pub bins: Vec<BinDTO>
}
