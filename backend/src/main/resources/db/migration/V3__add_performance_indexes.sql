-- DevFlow Performance and High-Frequency Query Indexes

-- Projects indexes
CREATE INDEX idx_projects_created_by_id ON projects(created_by_id);
CREATE INDEX idx_projects_status ON projects(status);

-- Sprints indexes
CREATE INDEX idx_sprints_project_id ON sprints(project_id);
CREATE INDEX idx_sprints_project_status ON sprints(project_id, status);

-- Issues composite indexes for Kanban and sprint boards
CREATE INDEX idx_issue_project_status ON issues(project_id, status);
CREATE INDEX idx_issue_project_sprint ON issues(project_id, sprint_id);

-- Comments chronological ordering index
CREATE INDEX idx_comment_issue_created ON comments(issue_id, created_at);

-- Activities chronological audit indexes
CREATE INDEX idx_activity_issue_created ON activities(issue_id, created_at DESC);
CREATE INDEX idx_activity_created_at ON activities(created_at DESC);
