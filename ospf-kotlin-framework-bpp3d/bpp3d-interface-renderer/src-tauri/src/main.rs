// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod item_dto;
mod renderer_dto;

use item_dto::ResponseDTO;
use renderer_dto::RendererDTO;
use std::fs::File;
use std::io::BufReader;
use std::path::Path;
use tauri::Manager;

static mut CURRENT_DIR: Option<String> = None;

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
fn load_data(path: String) -> Result<RendererDTO, String> {
    if let Some(data) = load_response::<ResponseDTO>(path.clone()) {
        return Ok(RendererDTO::from(&data));
    }
    return Err(String::from("无法识别的数据文件"));
}

fn main() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![get_current_directory, load_data])
        .setup(|app| {
            // open the web view console in debug mode
            #[cfg(debug_assertions)]
            {
                let window = app.get_window("main").unwrap();
                window.open_devtools();
                window.close_devtools();
            }
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
