// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod gantt;
mod gantt_data;
mod injection_molding_dto;
mod task_dto;
mod time_range;

use gantt::{Gantt, GanttLine};
use gantt_data::GanttDTO;
use injection_molding_dto::InjectionMoldingResponseDTO;
use std::collections::HashSet;
use std::fs::File;
use std::io::BufReader;
use std::path::Path;
use task_dto::group;
use tauri::Manager;
use time_range::TimeRange;

static mut CURRENT_DIR: Option<String> = None;
static mut CURRENT_SUB_CHART_DATA: Option<String> = None;

#[cfg(debug_assertions)]
#[tauri::command]
fn get_current_directory() -> String {
    return unsafe {
        if CURRENT_DIR.is_none() {
            CURRENT_DIR = Some(String::from(
                std::env::current_dir()
                    .unwrap()
                    .parent()
                    .unwrap()
                    .to_str()
                    .unwrap(),
            ));
        }
        CURRENT_DIR.as_ref().unwrap().clone()
    };
}

#[cfg(not(debug_assertions))]
#[tauri::command]
fn get_current_directory() -> String {
    return unsafe {
        if CURRENT_DIR.is_none() {
            CURRENT_DIR = Some(String::from(
                std::env::current_dir().unwrap().to_str().unwrap(),
            ));
        }
        CURRENT_DIR.as_ref().unwrap().clone()
    };
}

fn load_response<T: serde::de::DeserializeOwned>(path: String) -> Option<T> {
    unsafe {
        let parent_path = Path::new(&path).parent().unwrap();
        CURRENT_DIR = Some(String::from(parent_path.to_str().unwrap()));
    }

    let file = File::open(path).unwrap();
    let reader = BufReader::new(file);
    return match serde_json::from_reader(reader) {
        Ok(data) => Some(data),
        Err(err) => {
            println!("Message from Rust: {}", err);
            return None;
        }
    };
}

#[tauri::command]
fn load_data(path: String) -> Result<GanttDTO, String> {
    if let Some(data) = load_response::<InjectionMoldingResponseDTO>(path.clone()) {
        let tasks = group(&data.batch_list);
        let testing_tasks: std::collections::HashMap<
            &str,
            Vec<&injection_molding_dto::TestingTaskDTO>,
        > = group(&data.testing_list);
        let stop_plans = group(&data.stop_plan_list);

        let mut equipments = HashSet::new();
        tasks.keys().into_iter().for_each(|equipment| {
            equipments.insert(*equipment);
        });
        testing_tasks.keys().into_iter().for_each(|equipment| {
            equipments.insert(*equipment);
        });
        stop_plans.keys().into_iter().for_each(|equipment| {
            equipments.insert(*equipment);
        });

        let mut lines = Vec::new();
        for equipment in equipments {
            let empty_tasks = Vec::new();
            let this_tasks = tasks.get(equipment).unwrap_or(&empty_tasks);
            let empty_testing_tasks = Vec::new();
            let this_testing_tasks = testing_tasks.get(equipment).unwrap_or(&empty_testing_tasks);
            let empty_stop_plans = Vec::new();
            let this_stop_plans = stop_plans.get(equipment).unwrap_or(&empty_stop_plans);
            lines.push(GanttLine::new(
                equipment,
                this_tasks,
                this_testing_tasks,
                this_stop_plans,
            ));
        }

        let start_time = lines
            .iter()
            .map(|line| {
                line.items
                    .iter()
                    .min_by(|lhs, rhs| lhs.time.start.cmp(&rhs.time.start))
                    .unwrap()
                    .time
                    .start
            })
            .min()
            .unwrap();
        let end_time = lines
            .iter()
            .map(|line| {
                line.items
                    .iter()
                    .max_by(|lhs, rhs| lhs.time.end.cmp(&rhs.time.end))
                    .unwrap()
                    .time
                    .end
            })
            .max()
            .unwrap();
        let gantt = Gantt::new(
            TimeRange {
                start: start_time,
                end: end_time,
            },
            lines,
            Some(String::from("模具编号"))
        );
        return Ok(GanttDTO::from(&gantt));
    }
    return Err(String::from("无法识别的数据文件"));
}

#[tauri::command]
fn load_sub_chart_data() -> Result<String, String> {
    unsafe {
        return Ok(CURRENT_SUB_CHART_DATA.as_ref().unwrap().clone());
    }
}

fn main() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![get_current_directory, load_data, load_sub_chart_data])
        .setup(|app| {
            // open the web view console in debug mode
            #[cfg(debug_assertions)]
            {
                let window = app.get_window("main").unwrap();
                window.open_devtools();
                window.close_devtools();
            }
            let _ = app.listen_global("renderSubChart", move |event| {
                unsafe {
                    CURRENT_SUB_CHART_DATA = Some(String::from(event.payload().unwrap()));
                }
            });
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
