use chrono::NaiveDateTime;

#[derive(Debug)]
pub struct TimeRange {
    pub start: NaiveDateTime,
    pub end: NaiveDateTime,
}
