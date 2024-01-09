use chrono::NaiveDateTime;
use serde::{de, Deserialize, Deserializer};
use std::collections::HashMap;

fn list_from_key_value<'de, D>(deserializer: D) -> Result<Vec<(String, String)>, D::Error>
where
    D: Deserializer<'de>,
{
    let m: HashMap<String, String> = Deserialize::deserialize(deserializer)?;
    let v = m
        .iter()
        .map(|entry| (entry.0.to_owned(), entry.1.to_owned()))
        .collect();
    Ok(v)
}

fn naive_date_time_from_str<'de, D>(deserializer: D) -> Result<NaiveDateTime, D::Error>
where
    D: Deserializer<'de>,
{
    let s: String = Deserialize::deserialize(deserializer)?;
    NaiveDateTime::parse_from_str(&s, "%Y-%m-%d %H:%M:%S").map_err(de::Error::custom)
}

fn optional_naive_date_time_from_str<'de, D>(
    deserializer: D,
) -> Result<Option<NaiveDateTime>, D::Error>
where
    D: Deserializer<'de>,
{
    let s: Option<String> = Deserialize::deserialize(deserializer)?;
    match s {
        Some(s) => NaiveDateTime::parse_from_str(&s, "%Y-%m-%d %H:%M:%S")
            .map(|result| Some(result))
            .map_err(de::Error::custom),

        None => Ok(None),
    }
}

pub trait TaskBaseDTO: Sized {
    fn name(&self) -> &str;

    fn category(&self) -> &str;
    fn start_time(&self) -> NaiveDateTime;
    fn end_time(&self) -> NaiveDateTime;
    fn info(&self) -> &[(String, String)];
}

#[derive(Deserialize, Debug)]
pub struct SubTaskDTO {
    pub name: String,
    pub category: String,
    #[serde(rename = "startTime", deserialize_with = "naive_date_time_from_str")]
    pub start_time: NaiveDateTime,
    #[serde(rename = "endTime", deserialize_with = "naive_date_time_from_str")]
    pub end_time: NaiveDateTime,
    #[serde(deserialize_with = "list_from_key_value")]
    pub info: Vec<(String, String)>,
}

pub trait TaskDTO: TaskBaseDTO {
    type SubTask: TaskBaseDTO;

    fn executor(&self) -> &str;
    fn order(&self) -> &str;
    fn produce(&self) -> Option<&str>;
    fn material(&self) -> Option<&str>;
    fn resources(&self) -> &[(String, String)];
    fn scheduled_start_time(&self) -> Option<NaiveDateTime>;
    fn scheduled_end_time(&self) -> Option<NaiveDateTime>;
    fn sub_tasks(&self) -> &[Self::SubTask];

    fn group(tasks: &[Self]) -> HashMap<String, Vec<&Self>> {
        let mut task_groups = HashMap::new();
        for i in 0..tasks.len() {
            task_groups
                .entry(String::from(tasks[i].executor()))
                .and_modify(|entry: &mut Vec<_>| entry.push(&tasks[i]))
                .or_insert(vec![&tasks[i]]);
        }
        task_groups
    }
}

#[derive(Deserialize, Debug)]
pub struct NormalTaskDTO {
    pub name: String,
    pub category: String,
    pub executor: String,
    pub order: String,
    pub produce: Option<String>,
    pub material: Option<String>,
    #[serde(deserialize_with = "list_from_key_value")]
    pub resources: Vec<(String, String)>,
    #[serde(
        rename = "scheduledStartTime",
        deserialize_with = "optional_naive_date_time_from_str"
    )]
    pub scheduled_start_time: Option<NaiveDateTime>,
    #[serde(
        rename = "scheduledEndTime",
        deserialize_with = "optional_naive_date_time_from_str"
    )]
    pub scheduled_end_time: Option<NaiveDateTime>,
    #[serde(rename = "startTime", deserialize_with = "naive_date_time_from_str")]
    pub start_time: NaiveDateTime,
    #[serde(rename = "endTime", deserialize_with = "naive_date_time_from_str")]
    pub end_time: NaiveDateTime,
    #[serde(deserialize_with = "list_from_key_value")]
    pub info: Vec<(String, String)>,
    pub sub_tasks: Vec<SubTaskDTO>,
}

impl TaskBaseDTO for SubTaskDTO {
    fn name(&self) -> &str {
        return &self.name;
    }

    fn category(&self) -> &str {
        return &self.category;
    }

    fn start_time(&self) -> NaiveDateTime {
        return self.start_time;
    }

    fn end_time(&self) -> NaiveDateTime {
        return self.end_time;
    }

    fn info(&self) -> &[(String, String)] {
        return &self.info;
    }
}

impl TaskBaseDTO for NormalTaskDTO {
    fn name(&self) -> &str {
        return &self.name;
    }

    fn category(&self) -> &str {
        return &self.category;
    }

    fn start_time(&self) -> NaiveDateTime {
        return self.start_time;
    }

    fn end_time(&self) -> NaiveDateTime {
        return self.end_time;
    }

    fn info(&self) -> &[(String, String)] {
        return &self.info;
    }
}

impl TaskDTO for NormalTaskDTO {
    type SubTask = SubTaskDTO;

    fn executor(&self) -> &str {
        return &self.executor;
    }

    fn order(&self) -> &str {
        return &self.order;
    }

    fn produce(&self) -> Option<&str> {
        return self.produce.as_ref().map(|str| str.as_str());
    }

    fn material(&self) -> Option<&str> {
        return self.produce.as_ref().map(|str| str.as_str());
    }

    fn resources(&self) -> &[(String, String)] {
        return &self.resources;
    }

    fn scheduled_start_time(&self) -> Option<NaiveDateTime> {
        return self.scheduled_start_time.clone();
    }

    fn scheduled_end_time(&self) -> Option<NaiveDateTime> {
        return self.scheduled_end_time.clone();
    }

    fn sub_tasks(&self) -> &[SubTaskDTO] {
        return &self.sub_tasks;
    }
}

#[derive(Deserialize, Debug)]
pub struct ResponseDTO {
    pub tasks: Vec<NormalTaskDTO>,
}
