package com.devflow.dto.response;

import com.devflow.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;

@Schema(description = "Consolidated dashboard summary metrics and activity")
public class DashboardSummaryResponse {

    @Schema(description = "Role of the authenticated caller used to scope the dashboard", example = "PROJECT_MANAGER")
    private UserRole userRole;

    @Schema(description = "Aggregated project counts")
    private ProjectSummaryDto projectSummary;

    @Schema(description = "Aggregated issue status and priority counts")
    private IssueSummaryDto issueSummary;

    @Schema(description = "Aggregated issue counts assigned specifically to the caller")
    private AssignedIssueSummaryDto assignedIssueSummary;

    @Schema(description = "Aggregated sprint status counts")
    private SprintSummaryDto sprintSummary;

    @Schema(description = "10 most recent activity events accessible to caller")
    private List<ActivityResponse> recentActivity;

    public DashboardSummaryResponse() {
    }

    public DashboardSummaryResponse(
            UserRole userRole,
            ProjectSummaryDto projectSummary,
            IssueSummaryDto issueSummary,
            AssignedIssueSummaryDto assignedIssueSummary,
            SprintSummaryDto sprintSummary,
            List<ActivityResponse> recentActivity) {
        this.userRole = userRole;
        this.projectSummary = projectSummary;
        this.issueSummary = issueSummary;
        this.assignedIssueSummary = assignedIssueSummary;
        this.sprintSummary = sprintSummary;
        this.recentActivity = recentActivity != null ? recentActivity : Collections.emptyList();
    }

    public static DashboardSummaryResponse empty(UserRole role) {
        return new DashboardSummaryResponse(
                role,
                new ProjectSummaryDto(0, 0, 0, 0),
                new IssueSummaryDto(0, 0, 0, 0, 0, 0, 0, 0, 0),
                new AssignedIssueSummaryDto(0, 0, 0, 0, 0),
                new SprintSummaryDto(0, 0, 0, 0),
                Collections.emptyList()
        );
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public ProjectSummaryDto getProjectSummary() {
        return projectSummary;
    }

    public void setProjectSummary(ProjectSummaryDto projectSummary) {
        this.projectSummary = projectSummary;
    }

    public IssueSummaryDto getIssueSummary() {
        return issueSummary;
    }

    public void setIssueSummary(IssueSummaryDto issueSummary) {
        this.issueSummary = issueSummary;
    }

    public AssignedIssueSummaryDto getAssignedIssueSummary() {
        return assignedIssueSummary;
    }

    public void setAssignedIssueSummary(AssignedIssueSummaryDto assignedIssueSummary) {
        this.assignedIssueSummary = assignedIssueSummary;
    }

    public SprintSummaryDto getSprintSummary() {
        return sprintSummary;
    }

    public void setSprintSummary(SprintSummaryDto sprintSummary) {
        this.sprintSummary = sprintSummary;
    }

    public List<ActivityResponse> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<ActivityResponse> recentActivity) {
        this.recentActivity = recentActivity;
    }

    @Schema(description = "Project metrics by status")
    public static class ProjectSummaryDto {
        private long total;
        private long active;
        private long completed;
        private long archived;

        public ProjectSummaryDto() {
        }

        public ProjectSummaryDto(long total, long active, long completed, long archived) {
            this.total = total;
            this.active = active;
            this.completed = completed;
            this.archived = archived;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getActive() {
            return active;
        }

        public void setActive(long active) {
            this.active = active;
        }

        public long getCompleted() {
            return completed;
        }

        public void setCompleted(long completed) {
            this.completed = completed;
        }

        public long getArchived() {
            return archived;
        }

        public void setArchived(long archived) {
            this.archived = archived;
        }
    }

    @Schema(description = "Issue metrics by status and priority")
    public static class IssueSummaryDto {
        private long total;
        private long todo;
        private long inProgress;
        private long codeReview;
        private long done;
        private long lowPriority;
        private long mediumPriority;
        private long highPriority;
        private long criticalPriority;

        public IssueSummaryDto() {
        }

        public IssueSummaryDto(long total, long todo, long inProgress, long codeReview, long done,
                               long lowPriority, long mediumPriority, long highPriority, long criticalPriority) {
            this.total = total;
            this.todo = todo;
            this.inProgress = inProgress;
            this.codeReview = codeReview;
            this.done = done;
            this.lowPriority = lowPriority;
            this.mediumPriority = mediumPriority;
            this.highPriority = highPriority;
            this.criticalPriority = criticalPriority;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getTodo() {
            return todo;
        }

        public void setTodo(long todo) {
            this.todo = todo;
        }

        public long getInProgress() {
            return inProgress;
        }

        public void setInProgress(long inProgress) {
            this.inProgress = inProgress;
        }

        public long getCodeReview() {
            return codeReview;
        }

        public void setCodeReview(long codeReview) {
            this.codeReview = codeReview;
        }

        public long getDone() {
            return done;
        }

        public void setDone(long done) {
            this.done = done;
        }

        public long getLowPriority() {
            return lowPriority;
        }

        public void setLowPriority(long lowPriority) {
            this.lowPriority = lowPriority;
        }

        public long getMediumPriority() {
            return mediumPriority;
        }

        public void setMediumPriority(long mediumPriority) {
            this.mediumPriority = mediumPriority;
        }

        public long getHighPriority() {
            return highPriority;
        }

        public void setHighPriority(long highPriority) {
            this.highPriority = highPriority;
        }

        public long getCriticalPriority() {
            return criticalPriority;
        }

        public void setCriticalPriority(long criticalPriority) {
            this.criticalPriority = criticalPriority;
        }
    }

    @Schema(description = "Issue metrics assigned directly to caller")
    public static class AssignedIssueSummaryDto {
        private long total;
        private long todo;
        private long inProgress;
        private long codeReview;
        private long done;

        public AssignedIssueSummaryDto() {
        }

        public AssignedIssueSummaryDto(long total, long todo, long inProgress, long codeReview, long done) {
            this.total = total;
            this.todo = todo;
            this.inProgress = inProgress;
            this.codeReview = codeReview;
            this.done = done;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getTodo() {
            return todo;
        }

        public void setTodo(long todo) {
            this.todo = todo;
        }

        public long getInProgress() {
            return inProgress;
        }

        public void setInProgress(long inProgress) {
            this.inProgress = inProgress;
        }

        public long getCodeReview() {
            return codeReview;
        }

        public void setCodeReview(long codeReview) {
            this.codeReview = codeReview;
        }

        public long getDone() {
            return done;
        }

        public void setDone(long done) {
            this.done = done;
        }
    }

    @Schema(description = "Sprint metrics by status")
    public static class SprintSummaryDto {
        private long total;
        private long planned;
        private long active;
        private long completed;

        public SprintSummaryDto() {
        }

        public SprintSummaryDto(long total, long planned, long active, long completed) {
            this.total = total;
            this.planned = planned;
            this.active = active;
            this.completed = completed;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getPlanned() {
            return planned;
        }

        public void setPlanned(long planned) {
            this.planned = planned;
        }

        public long getActive() {
            return active;
        }

        public void setActive(long active) {
            this.active = active;
        }

        public long getCompleted() {
            return completed;
        }

        public void setCompleted(long completed) {
            this.completed = completed;
        }
    }
}
