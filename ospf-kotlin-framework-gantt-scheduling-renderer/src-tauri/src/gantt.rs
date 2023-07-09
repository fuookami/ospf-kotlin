use crate::injection_molding_dto::{InjectionMoldingTaskDTO, StopPlanDTO, TestingTaskDTO};
use crate::time_range::TimeRange;
use chrono::{NaiveDate, NaiveTime};

#[derive(Debug)]
pub struct GanttSubItem {
    pub name: String,
    pub category: String,
    pub time: TimeRange,
}

#[derive(Debug)]
pub struct GanttItem {
    pub name: String,
    pub category: String,
    pub sub_items: Vec<GanttSubItem>,
    pub scheduled_time: TimeRange,
    pub time: TimeRange,
    pub info: Vec<(String, String)>,
}

#[derive(Debug)]
pub struct GanttLine {
    pub name: String,
    pub category: String,
    pub items: Vec<GanttItem>,
}

impl GanttLine {
    pub fn new(
        name: &str,
        tasks: &[&InjectionMoldingTaskDTO],
        testing_tasks: &[&TestingTaskDTO],
        stop_plans: &[&StopPlanDTO],
    ) -> Self {
        let mut items = Vec::new();
        for task in tasks {
            if task.start_time().is_err()
                || task.end_time().is_err()
                || task.mould_transform_end_time().is_err()
                || task.mould_style_transform_end_time().is_err()
                || task.injection_start_time().is_err()
            {
                continue;
            }

            if task.start_time().unwrap() == task.end_time().unwrap() {
                continue;
            }

            let mut sub_items = Vec::new();
            sub_items.push(GanttSubItem {
                name: String::from("换模"),
                category: String::from("Mould Transform"),
                time: TimeRange {
                    start: task.start_time().unwrap(),
                    end: task.mould_transform_end_time().unwrap(),
                },
            });
            sub_items.push(GanttSubItem {
                name: String::from("换款"),
                category: String::from("Mould Style Transform"),
                time: TimeRange {
                    start: task.mould_transform_end_time().unwrap(),
                    end: task.mould_style_transform_end_time().unwrap(),
                },
            });
            sub_items.push(GanttSubItem {
                name: String::from("换料"),
                category: String::from("Material Transform"),
                time: TimeRange {
                    start: task.mould_style_transform_end_time().unwrap(),
                    end: task.injection_start_time().unwrap(),
                },
            });
            sub_items.push(GanttSubItem {
                name: String::from("注塑"),
                category: String::from("Injection"),
                time: TimeRange {
                    start: task.injection_start_time().unwrap(),
                    end: task.end_time().unwrap(),
                },
            });
            items.push(GanttItem {
                name: task.plan_order_no.clone(),
                category: String::from("Normal"),
                sub_items: sub_items,
                scheduled_time: TimeRange {
                    start: task.start_time().unwrap(),
                    end: task.end_time().unwrap(),
                },
                time: TimeRange {
                    start: task.start_time().unwrap(),
                    end: task.end_time().unwrap(),
                },
                info: vec![
                    (String::from("计划单号"), task.plan_order_no.clone()),
                    (String::from("生产批次"), task.batch_no.clone()),
                    (String::from("图号"), task.drawing_no()),
                    (
                        String::from("模具编号"),
                        task.mould_no.as_ref().unwrap_or(&String::new()).clone(),
                    ),
                    (String::from("物料编码"), task.material_no.clone()),
                    (String::from("成品编码"), task.product_material_no.clone()),
                    (
                        String::from("生产数量"),
                        format!("{}", task.manufacture_quantity),
                    ),
                    (
                        String::from("完成差额"),
                        format!("{}", task.finish_gap_quantity),
                    ),
                    (
                        String::from("生产说明"),
                        task.manuf_note
                            .as_ref()
                            .unwrap_or(&String::from(""))
                            .clone(),
                    ),
                    (String::from("加工时长"), format!("{} min", task.cycle_time)),
                    (
                        String::from("任务开始时间"),
                        task.start_time()
                            .unwrap()
                            .format("%Y-%m-%d %H:%M:%S")
                            .to_string(),
                    ),
                    (
                        String::from("任务结束时间"),
                        task.end_time()
                            .unwrap()
                            .format("%Y-%m-%d %H:%M:%S")
                            .to_string(),
                    ),
                    (
                        String::from("交货日期"),
                        task.expiration_date()
                            .unwrap()
                            .format("%Y年%m月%d日")
                            .to_string(),
                    ),
                ],
            })
        }
        for task in testing_tasks {
            if task.start_time().is_err()
                || task.end_time().is_err()
                || task.testing_start_time().is_err()
                || task.expiration_date().is_err()
            {
                continue;
            }

            if task.start_time().unwrap() == task.end_time().unwrap() {
                continue;
            }

            let mut sub_items = Vec::new();
            sub_items.push(GanttSubItem {
                name: String::from("换模"),
                category: String::from("Mould Transform"),
                time: TimeRange {
                    start: task.start_time().unwrap(),
                    end: task.testing_start_time().unwrap(),
                },
            });
            sub_items.push(GanttSubItem {
                name: String::from("试模"),
                category: String::from("Testing"),
                time: TimeRange {
                    start: task.testing_start_time().unwrap(),
                    end: task.end_time().unwrap(),
                },
            });
            items.push(GanttItem {
                name: task.plan_order_no.clone(),
                category: String::from("Testing"),
                sub_items: sub_items,
                scheduled_time: TimeRange {
                    start: task.start_time().unwrap(),
                    end: task.end_time().unwrap(),
                },
                time: TimeRange {
                    start: task.start_time().unwrap(),
                    end: task.end_time().unwrap(),
                },
                info: vec![
                    (String::from("生产批次"), task.batch_no.clone()),
                    (
                        String::from("模具编号"),
                        task.mould_no.as_ref().unwrap_or(&String::new()).clone(),
                    ),
                    (
                        String::from("生产说明"),
                        task.note.as_ref().unwrap_or(&String::new()).clone(),
                    ),
                    (String::from("加工时长"), format!("{} min", task.cycle_time)),
                    (
                        String::from("交货日期"),
                        task.expiration_date()
                            .unwrap()
                            .format("%Y年%m月%d日")
                            .to_string(),
                    ),
                ],
            });
        }
        for task in stop_plans {
            if task.start_time().is_err() || task.end_time().is_err() {
                continue;
            }

            if task.start_time().unwrap() == task.end_time().unwrap() {
                continue;
            }

            items.push(GanttItem {
                name: String::from("停机"),
                category: String::from("Unavailable"),
                sub_items: vec![],
                scheduled_time: TimeRange {
                    start: task.start_time().unwrap(),
                    end: task.end_time().unwrap(),
                },
                time: TimeRange {
                    start: task.start_time().unwrap(),
                    end: task.end_time().unwrap(),
                },
                info: vec![
                    (String::from("事件"), String::from("计划停机")),
                    (
                        String::from("开始时间"),
                        task.start_time()
                            .unwrap()
                            .format("%Y-%m-%d %H:%M:%S")
                            .to_string(),
                    ),
                    (
                        String::from("结束时间"),
                        task.end_time()
                            .unwrap()
                            .format("%Y-%m-%d %H:%M:%S")
                            .to_string(),
                    ),
                ],
            });
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
    pub link_info: Option<String>
}

impl Gantt {
    pub fn new(time: TimeRange, lines: Vec<GanttLine>, link_info: Option<String>) -> Self {
        Self {
            time: time,
            lines: lines,
            link_info: link_info
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
