use crate::item_dto::*;
use serde::{Serialize, Deserialize};
use std::collections::{ HashSet, BTreeMap };

#[derive(Serialize, Deserialize, Debug)]
pub struct RendererDTO {
    message: String,
    bins: BTreeMap<String, BinDTO>
}

impl From<&ResponseDTO> for RendererDTO {
    fn from(value: &ResponseDTO) -> Self {
        return crossbeam::scope(|s| {
            let future1 = s.spawn(|_|{
                let mut cargo_types = HashSet::new();
                let mut package_types = HashSet::<&str>::new();
    
                for bin_data in &value.bins {
                    for item_data in &bin_data.items {
                        for this_cargo_type in item_data.name.split(",") {
                            cargo_types.insert(this_cargo_type);
                        }
                        package_types.insert(&item_data.package_type);
                    }
                }
                return (cargo_types, package_types);
            });
    
            let future2 = s.spawn(|_|{
                let mut batch_nos = HashSet::<String>::new();
                let mut bin_types = HashSet::<&str>::new();
    
                for bin_data in &value.bins {
                    batch_nos.insert(bin_data.batch_no.as_ref().unwrap_or(&String::from("")).clone());
                    bin_types.insert(&bin_data.type_code);
                }
    
                return (batch_nos, bin_types);
            });
    
            let (cargo_types, package_types) = future1.join().unwrap();
            let (batch_nos, bin_types) = future2.join().unwrap();

            let volume = value.bins.iter().map(|bin| bin.volume).sum::<f64>();
            let item_amount: usize = value.bins.iter().map(|bin| bin.items.len()).sum();

            RendererDTO {
                message: format!("共 {} 个分柜， {} 个货柜，总体积 {:.3} m3， {} 种货柜类型， {} 种物料， {} 种包装材料， {} 个物料， 剩 {} 个物料", batch_nos.len(), value.bins.len(), volume, bin_types.len(), cargo_types.len(), package_types.len(), item_amount, value.rest_amount.unwrap_or(0)),
                bins: value.bins.iter().map(|bin| (format!("{}/{}", bin.batch_no.as_ref().unwrap_or(&String::from("*")), bin.order), bin.clone())).collect()
            }
        }).unwrap();
    }
}
