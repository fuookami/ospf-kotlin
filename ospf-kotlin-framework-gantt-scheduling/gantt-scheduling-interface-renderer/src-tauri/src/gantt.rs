use crate::task_dto::*;
use crate::time_range::TimeRange;
use chrono::{NaiveDate, NaiveTime};

#[derive(Debug)]
pub struct GanttSubItem {
    pub name: String,
    pub category: String,
    pub time: TimeRange,
}

impl<T: TaskBaseDTO> From<&T> for GanttSubItem {
    fn from(task: &T) -> Self {
        return Self {
            name: String::from(task.name()),
            category: String::from(task.category()),
            time: TimeRange {
                start: task.start_time(),
                end: task.end_time(),
            },
        };
    }
}

#[derive(Debug)]
pub struct GanttItem {
    pub name: String,
    pub category: String,
    pub sub_items: Vec<GanttSubItem>,
    pub scheduled_time: TimeRange,
    pub time: TimeRange,
    pub produces: Vec<(String, String)>,
    pub resources: Vec<(String, String)>,
    pub info: Vec<(String, String)>,
}

impl<T: TaskDTO> From<&T> for GanttItem {
    fn from(task: &T) -> Self {
        let mut produces = Vec::new();
        produces.push((String::from("订单"), String::from(task.order())));
        if let Some(produce) = task.produce() {
            produces.push((String::from("工序"), String::from(produce)));
        }
        if let Some(material) = task.material() {
            produces.push((String::from("原料"), String::from(material)));
        }

        return Self {
            name: String::from(task.name()),
            category: String::from(task.category()),
            sub_items: task
                .sub_tasks()
                .iter()
                .map(|sub_task| GanttSubItem::from(sub_task))
                .collect(),
            scheduled_time: match (task.scheduled_start_time(), task.scheduled_end_time()) {
                (Some(scheduled_start_time), Some(scheduled_end_time)) => TimeRange {
                    start: scheduled_start_time,
                    end: scheduled_end_time,
                },

                _ => TimeRange {
                    start: task.start_time(),
                    end: task.end_time(),
                },
            },
            time: TimeRange {
                start: task.start_time(),
                end: task.end_time(),
            },
            produces: produces,
            resources: task
                .resources()
                .iter()
                .map(|resource| resource.clone())
                .collect(),
            info: task.info().iter().map(|info| info.clone()).collect(),
        };
    }
}

#[derive(Debug)]
pub struct GanttLine {
    pub name: String,
    pub category: String,
    pub items: Vec<GanttItem>,
}

impl GanttLine {
    pub fn new(name: &str, tasks: &[&NormalTaskDTO]) -> Self {
        let mut items = Vec::new();
        for task in tasks {
            if task.start_time() == task.end_time() {
                continue;
            }
            items.push(GanttItem::from(*task));
        }
        items.sort_by(|lhs, rhs| lhs.time.start.cmp(&rhs.time.start));
        Self {
            name: String::from(name),
            category: String::from("Normal"),
            items: items,
        }
    }
}

pub struct Gantt {
    pub time: TimeRange,
    pub lines: Vec<GanttLine>,
    pub link_info: Vec<String>,
}

impl Gantt {
    pub fn new(time: TimeRange, lines: Vec<GanttLine>, link_info: Vec<String>) -> Self {
        Self {
            time: time,
            lines: lines,
            link_info: link_info,
        }
    }

    pub fn start_date(&self) -> NaiveDate {
        self.time.start.date()
    }

    pub fn end_date(&self) -> NaiveDate {
        self.time.end.date()
    }

    pub fn start_day_time(&self) -> NaiveTime {
        self.time.start.time()
    }

    pub fn end_day_time(&self) -> NaiveTime {
        self.time.start.time()
    }
}
