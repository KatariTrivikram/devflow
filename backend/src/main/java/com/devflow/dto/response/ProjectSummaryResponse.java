package com.devflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Compact project overview summary metrics")
public class ProjectSummaryResponse {

    @Schema(description = "Unique project identifier")
    private UUID projectId;

    @Schema(description = "Project name")
    private String projectName;

    @Schema(description = "Total number of issues in project", example = "24")
    private long totalIssues;

    @Schema(description = "Number of open issues (TODO + IN_PROGRESS + CODE_REVIEW)", example = "20")
    private long openIssues;

    @Schema(description = "Number of TODO issues", example = "8")
    private long todoIssues;

    @Schema(description = "Number of IN_PROGRESS issues", example = "10")
    private long inProgressIssues;

    @Schema(description = "Number of CODE_REVIEW issues", example = "2")
    private long codeReviewIssues;

    @Schema(description = "Number of DONE issues", example = "4")
    private long doneIssues;

    @Schema(description = "Number of members in project", example = "5")
    private long memberCount;

    @Schema(description = "Details of active sprint, or null if no sprint is currently active")
    private ActiveSprintSummaryDto activeSprint;

    public ProjectSummaryResponse() {
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public long getTotalIssues() {
        return totalIssues;
    }

    public void setTotalIssues(long totalIssues) {
        this.totalIssues = totalIssues;
    }

    public long getOpenIssues() {
        return openIssues;
    }

    public void setOpenIssues(long openIssues) {
        this.openIssues = openIssues;
    }

    public long getTodoIssues() {
        return todoIssues;
    }

    public void setTodoIssues(long todoIssues) {
        this.todoIssues = todoIssues;
    }

    public long getInProgressIssues() {
        return inProgressIssues;
    }

    public void setInProgressIssues(long inProgressIssues) {
        this.inProgressIssues = inProgressIssues;
    }

    public long getCodeReviewIssues() {
        return codeReviewIssues;
    }

    public void setCodeReviewIssues(long codeReviewIssues) {
        this.codeReviewIssues = codeReviewIssues;
    }

    public long getDoneIssues() {
        return doneIssues;
    }

    public void setDoneIssues(long doneIssues) {
        this.doneIssues = doneIssues;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(long memberCount) {
        this.memberCount = memberCount;
    }

    public ActiveSprintSummaryDto getActiveSprint() {
        return activeSprint;
    }

    public void setActiveSprint(ActiveSprintSummaryDto activeSprint) {
        this.activeSprint = activeSprint;
    }

    @Schema(description = "Active sprint summary information and calculated issue progress")
    public static class ActiveSprintSummaryDto {
        private UUID id;
        private String name;
        private Instant startDate;
        private Instant endDate;
        private String status;
        private long totalIssues;
        private long completedIssues;
        private long inProgressIssues;
        private long todoIssues;
        private int progressPercentage;

        public ActiveSprintSummaryDto() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Instant getStartDate() {
            return startDate;
        }

        public void setStartDate(Instant startDate) {
            this.startDate = startDate;
        }

        public Instant getEndDate() {
            return endDate;
        }

        public void setEndDate(Instant endDate) {
            this.endDate = endDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getTotalIssues() {
            return totalIssues;
        }

        public void setTotalIssues(long totalIssues) {
            this.totalIssues = totalIssues;
        }

        public long getCompletedIssues() {
            return completedIssues;
        }

        public void setCompletedIssues(long completedIssues) {
            this.completedIssues = completedIssues;
        }

        public long getInProgressIssues() {
            return inProgressIssues;
        }

        public void setInProgressIssues(long inProgressIssues) {
            this.inProgressIssues = inProgressIssues;
        }

        public long getTodoIssues() {
            return todoIssues;
        }

        public void setTodoIssues(long todoIssues) {
            this.todoIssues = todoIssues;
        }

        public int getProgressPercentage() {
            return progressPercentage;
        }

        public void setProgressPercentage(int progressPercentage) {
            this.progressPercentage = progressPercentage;
        }
    }
}
