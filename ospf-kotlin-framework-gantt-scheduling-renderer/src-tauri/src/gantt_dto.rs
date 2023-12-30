use crate::gantt::*;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug)]
pub struct GanttSubItemDTO {
    pub name: String,
    pub category: String,
    pub start_time: String,
    pub end_time: String,
}

impl From<&GanttSubItem> for GanttSubItemDTO {
    fn from(value: &GanttSubItem) -> Self {
        Self {
            name: value.name.clone(),
            category: value.category.clone(),
            start_time: value.time.start.format("%Y-%m-%d %H:%M:%S").to_string(),
            end_time: value.time.end.format("%Y-%m-%d %H:%M:%S").to_string(),
        }
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct GanttItemInfoDTO {
    pub key: String,
    pub value: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct GanttItemDTO {
    pub name: String,
    pub category: String,
    pub sub_items: Vec<GanttSubItemDTO>,
    pub scheduled_start_time: String,
    pub scheduled_end_time: String,
    pub start_time: String,
    pub end_time: String,
    pub produces: Vec<GanttItemInfoDTO>,
    pub resources: Vec<GanttItemInfoDTO>,
    pub info: Vec<GanttItemInfoDTO>,
}

impl From<&GanttItem> for GanttItemDTO {
    fn from(value: &GanttItem) -> Self {
        Self {
            name: value.name.clone(),
            category: value.category.clone(),
            sub_items: value
                .sub_items
                .iter()
                .map(|sub_item| GanttSubItemDTO::from(sub_item))
                .collect(),
            scheduled_start_time: value
                .scheduled_time
                .start
                .format("%Y-%m-%d %H:%M:%S")
                .to_string(),
            scheduled_end_time: value
                .scheduled_time
                .end
                .format("%Y-%m-%d %H:%M:%S")
                .to_string(),
            start_time: value.time.start.format("%Y-%m-%d %H:%M:%S").to_string(),
            end_time: value.time.end.format("%Y-%m-%d %H:%M:%S").to_string(),
            produces: value
                .produces
                .iter()
                .map(|(key, value)| GanttItemInfoDTO {
                    key: key.clone(),
                    value: value.clone(),
                })
                .collect(),
            resources: value
                .resources
                .iter()
                .map(|(key, value)| GanttItemInfoDTO {
                    key: key.clone(),
                    value: value.clone(),
                })
                .collect(),
            info: value
                .info
                .iter()
                .map(|(key, value)| GanttItemInfoDTO {
                    key: key.clone(),
                    value: value.clone(),
                })
                .collect(),
        }
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct GanttLineDTO {
    pub name: String,
    pub category: String,
    pub items: Vec<GanttItemDTO>,
}

impl From<&GanttLine> for GanttLineDTO {
    fn from(value: &GanttLine) -> Self {
        Self {
            name: value.name.clone(),
            category: value.category.clone(),
            items: value
                .items
                .iter()
                .map(|item| GanttItemDTO::from(item))
                .collect(),
        }
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct GanttDTO {
    pub start_time: String,
    pub end_time: String,
    pub link_info: Vec<String>,
    pub lines: Vec<GanttLineDTO>,
}

impl From<&Gantt> for GanttDTO {
    fn from(value: &Gantt) -> Self {
        Self {
            start_time: value.time.start.format("%Y-%m-%d %H:%M:%S").to_string(),
            end_time: value.time.end.format("%Y-%m-%d %H:%M:%S").to_string(),
            link_info: value.link_info.clone(),
            lines: value
                .lines
                .iter()
                .map(|line| GanttLineDTO::from(line))
                .collect(),
        }
    }
}
