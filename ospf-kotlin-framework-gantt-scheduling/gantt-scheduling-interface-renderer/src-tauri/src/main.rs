// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod gantt;
mod gantt_dto;
mod task_dto;
mod time_range;

use gantt::*;
use gantt_dto::*;
use std::collections::HashSet;
use std::fs::File;
use std::io::BufReader;
use std::path::Path;
use task_dto::*;
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
    if let Some(data) = load_response::<ResponseDTO>(path.clone()) {
        let tasks = TaskDTO::group(&data.tasks);
        let executors = tasks.keys().map(|executor| executor.as_str()).collect::<HashSet<&str>>();
        let mut link_info = data
            .tasks
            .iter()
            .flat_map(|task| task.resources.iter().map(|resource| resource.0.as_str()))
            .collect::<HashSet<&str>>();
        link_info.insert("订单");

        let lines = executors
            .iter()
            .map(|executor| {
                let empty_tasks = Vec::new();
                let this_tasks = tasks.get(*executor).unwrap_or(&empty_tasks);
                GanttLine::new(executor, this_tasks)
            })
            .collect::<Vec<GanttLine>>();

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
            link_info.iter().map(|str| String::from(*str)).collect(),
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
        .invoke_handler(tauri::generate_handler![
            get_current_directory,
            load_data,
            load_sub_chart_data
        ])
        .setup(|app| {
            // open the web view console in debug mode
            #[cfg(debug_assertions)]
            {
                let window = app.get_window("main").unwrap();
                window.open_devtools();
                window.close_devtools();
            }
            let _ = app.listen_global("renderSubChart", move |event| unsafe {
                CURRENT_SUB_CHART_DATA = Some(String::from(event.payload().unwrap()));
            });
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
