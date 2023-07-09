use std::collections::HashMap;

pub trait TaskDTO {
    fn equipment_no(&self) -> &str;
}

pub fn group<T: TaskDTO>(tasks: &[T]) -> HashMap<&str, Vec<&T>> {
    let mut task_groups = HashMap::new();
    for i in 0..tasks.len() {
        task_groups
            .entry(tasks[i].equipment_no().clone())
            .and_modify(|entry: &mut Vec<_>| entry.push(&tasks[i]))
            .or_insert(vec![&tasks[i]]);
    }
    task_groups
}
