use crate::task_dto::TaskDTO;
use chrono::{NaiveDate, NaiveDateTime, ParseError};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug)]
pub struct InjectionMoldingTaskDTO {
    pub equipment_no: String,
    pub batch_no: String,
    pub material_no: String,
    pub material_name: String,
    #[serde(rename = "drawing_no")]
    pub drawing_no_and_material: String,
    #[serde(rename = "mold_no")]
    pub mould_no: Option<String>,
    pub model_type: String,
    #[serde(rename = "manuf_qty")]
    pub manufacture_quantity: u64,
    #[serde(rename = "batch_qty")]
    pub batch_quantity: u64,
    #[serde(rename = "finish_gap_qty")]
    pub finish_gap_quantity: u64,
    #[serde(rename = "finish_qty")]
    pub finish_quantity: u64,
    pub manuf_note: Option<String>,
    pub note: Option<String>,
    #[serde(rename = "pro_mat_no")]
    pub product_material_no: String,
    pub cycle_time: u64,
    #[serde(rename = "start_time")]
    pub start_time_str: String,
    #[serde(rename = "end_time")]
    pub end_time_str: String,
    #[serde(rename = "d_start_date")]
    pub expiration_start_date_str: String,
    #[serde(rename = "d_end_date")]
    pub expiration_end_date_str: String,
    pub plan_order_no: String,
    pub state: String,
    #[serde(rename = "exchange_mould_endTime")]
    pub mould_transform_end_time_str: String,
    #[serde(rename = "exchange_insert_endTime")]
    pub mould_style_transform_end_time_str: String,
    #[serde(rename = "injection_startTime")]
    pub injection_start_time_str: String,
}

impl InjectionMoldingTaskDTO {
    pub fn drawing_no(&self) -> String {
        String::from(self.drawing_no_and_material.split("\\").next().unwrap())
    }

    pub fn start_time(&self) -> Result<NaiveDateTime, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.start_time_str,
            "%Y-%m-%d %H:%M:%S",
        )?)
    }

    pub fn end_time(&self) -> Result<NaiveDateTime, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.end_time_str,
            "%Y-%m-%d %H:%M:%S",
        )?)
    }

    pub fn mould_transform_end_time(&self) -> Result<NaiveDateTime, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.mould_transform_end_time_str,
            "%Y-%m-%d %H:%M:%S",
        )?)
    }

    pub fn mould_style_transform_end_time(&self) -> Result<NaiveDateTime, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.mould_style_transform_end_time_str,
            "%Y-%m-%d %H:%M:%S",
        )?)
    }

    pub fn injection_start_time(&self) -> Result<NaiveDateTime, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.injection_start_time_str,
            "%Y-%m-%d %H:%M:%S",
        )?)
    }

    pub fn expiration_date(&self) -> Result<NaiveDate, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.expiration_start_date_str,
            "%Y-%m-%d %H:%M:%S",
        )?.date())
    }
}

impl TaskDTO for InjectionMoldingTaskDTO {
    fn equipment_no(&self) -> &str {
        &self.equipment_no
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct TestingTaskDTO {
    pub equipment_no: String,
    pub batch_no: String,
    #[serde(rename = "mold_no")]
    pub mould_no: Option<String>,
    pub note: Option<String>,
    #[serde(rename = "d_start_date")]
    pub expiration_date_str: String,
    pub plan_order_no: String,
    pub cycle_time: u64,
    #[serde(rename = "start_time")]
    pub start_time_str: String,
    #[serde(rename = "exchange_mould_endTime")]
    pub mould_transform_end_time_str: String,
    #[serde(rename = "injection_startTime")]
    pub testing_start_time_str: String,
    #[serde(rename = "end_time")]
    pub end_time_str: String,
}

impl TestingTaskDTO {
    pub fn start_time(&self) -> Result<NaiveDateTime, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.start_time_str,
            "%Y-%m-%d %H:%M:%S",
        )?)
    }

    pub fn end_time(&self) -> Result<NaiveDateTime, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.end_time_str,
            "%Y-%m-%d %H:%M:%S",
        )?)
    }

    pub fn testing_start_time(&self) -> Result<NaiveDateTime, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.testing_start_time_str,
            "%Y-%m-%d %H:%M:%S",
        )?)
    }

    pub fn expiration_date(&self) -> Result<NaiveDate, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.expiration_date_str,
            "%Y-%m-%d %H:%M:%S",
        )?.date())
    }
}

impl TaskDTO for TestingTaskDTO {
    fn equipment_no(&self) -> &str {
        &self.equipment_no
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct StopPlanDTO {
    pub equipment_no: String,
    #[serde(rename = "start_time")]
    pub start_time_str: String,
    #[serde(rename = "end_time")]
    pub end_time_str: String,
}

impl StopPlanDTO {
    pub fn start_time(&self) -> Result<NaiveDateTime, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.start_time_str,
            "%Y-%m-%d %H:%M:%S",
        )?)
    }

    pub fn end_time(&self) -> Result<NaiveDateTime, ParseError> {
        Ok(NaiveDateTime::parse_from_str(
            &self.end_time_str,
            "%Y-%m-%d %H:%M:%S",
        )?)
    }
}

impl TaskDTO for StopPlanDTO {
    fn equipment_no(&self) -> &str {
        &self.equipment_no
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct InjectionMoldingResponseDTO {
    pub batch_list: Vec<InjectionMoldingTaskDTO>,
    #[serde(rename = "test_mold_batch")]
    pub testing_list: Vec<TestingTaskDTO>,
    #[serde(rename = "machine_stop_plan")]
    pub stop_plan_list: Vec<StopPlanDTO>,
    pub scheduling_version: Option<String>,
    pub create_time: String,
    pub is_activate: bool,
    pub max_end_time: String,
    pub capacity_rate: f64,
    #[serde(rename = "dalay_rate")]
    pub delay_rate: f64,
    #[serde(rename = "num_exchange_mould")]
    pub mould_transform_times: f64,
    #[serde(rename = "num_exchange_insert")]
    pub mould_style_transform_times: f64,
    #[serde(rename = "num_exchange_task")]
    pub material_transform_times: f64,
    #[serde(rename = "exchange_mould_cost")]
    pub mould_transform_cost: f64,
    #[serde(rename = "exchange_insert_cost")]
    pub mould_style_transform_cost: f64,
    #[serde(rename = "exchange_task_cost")]
    pub material_transform_cost: f64,
}
