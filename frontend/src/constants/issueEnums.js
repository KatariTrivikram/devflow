/**
 * Issue domain enums and human-readable presentation labels
 * matching the Spring Boot backend domain models exactly.
 */

export const ISSUE_TYPE = Object.freeze({
  BUG: 'BUG',
  FEATURE: 'FEATURE',
  TASK: 'TASK',
  IMPROVEMENT: 'IMPROVEMENT',
});

export const ISSUE_TYPE_LABELS = Object.freeze({
  [ISSUE_TYPE.BUG]: 'Bug',
  [ISSUE_TYPE.FEATURE]: 'Feature',
  [ISSUE_TYPE.TASK]: 'Task',
  [ISSUE_TYPE.IMPROVEMENT]: 'Improvement',
});

export const ISSUE_PRIORITY = Object.freeze({
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  CRITICAL: 'CRITICAL',
});

export const ISSUE_PRIORITY_LABELS = Object.freeze({
  [ISSUE_PRIORITY.LOW]: 'Low',
  [ISSUE_PRIORITY.MEDIUM]: 'Medium',
  [ISSUE_PRIORITY.HIGH]: 'High',
  [ISSUE_PRIORITY.CRITICAL]: 'Critical',
});

export const ISSUE_STATUS = Object.freeze({
  TODO: 'TODO',
  IN_PROGRESS: 'IN_PROGRESS',
  CODE_REVIEW: 'CODE_REVIEW',
  DONE: 'DONE',
});

export const ISSUE_STATUS_LABELS = Object.freeze({
  [ISSUE_STATUS.TODO]: 'To Do',
  [ISSUE_STATUS.IN_PROGRESS]: 'In Progress',
  [ISSUE_STATUS.CODE_REVIEW]: 'Code Review',
  [ISSUE_STATUS.DONE]: 'Done',
});
