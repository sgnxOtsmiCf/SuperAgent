package cn.sgnxotsmicf.agentTool.commonTool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PlanningTool {

    private final Map<String, Map<String, Plan>> userPlans = new ConcurrentHashMap<>();
    private final Map<String, String> userCurrentPlanId = new ConcurrentHashMap<>();

    private Map<String, Plan> getUserPlans(String userId) {
        return userPlans.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
    }

    private String getCurrentPlanId(String userId) {
        return userCurrentPlanId.get(userId);
    }

    private void setCurrentPlanId(String userId, String planId) {
        if (planId != null) {
            userCurrentPlanId.put(userId, planId);
        } else {
            userCurrentPlanId.remove(userId);
        }
    }

    private String getUserId(ToolContext toolContext) {
        Object userIdObj = toolContext.getContext().get("userId");
        if (userIdObj == null) {
            throw new IllegalArgumentException("userId not found in ToolContext. " +
                    "Please pass userId via .toolContext(Map.of(\"userId\", \"xxx\")) when calling ChatClient.");
        }
        return userIdObj.toString();
    }

    @Tool(description = "Create a new plan with the specified ID, title, and steps.")
    public String createPlan(
            @ToolParam(description = "Unique identifier for the plan") String planId,
            @ToolParam(description = "Title for the plan") String title,
            @ToolParam(description = "List of plan steps") List<String> steps,
            ToolContext toolContext
    ) {
        String userId = getUserId(toolContext);
        Map<String, Plan> plans = getUserPlans(userId);

        if (planId == null || planId.isEmpty()) {
            return "Error: plan_id is required for command: create";
        }

        if (plans.containsKey(planId)) {
            return "Error: A plan with ID '" + planId + "' already exists. Use 'update' to modify existing plans.";
        }

        if (title == null || title.isEmpty()) {
            return "Error: title is required for command: create";
        }

        if (steps == null || steps.isEmpty()) {
            return "Error: steps must be a non-empty list of strings for command: create";
        }

        List<String> stepStatuses = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            stepStatuses.add("not_started");
        }

        Plan plan = new Plan(planId, title, steps, stepStatuses, new ArrayList<>());
        plans.put(planId, plan);
        setCurrentPlanId(userId, planId);

        return "Plan created successfully with ID: " + planId + "\n\n" + formatPlan(plan);
    }

    @Tool(description = "Update an existing plan with new title or steps.")
    public String updatePlan(
            @ToolParam(description = "Unique identifier for the plan") String planId,
            @ToolParam(description = "Title for the plan") String title,
            @ToolParam(description = "List of plan steps") List<String> steps,
            ToolContext toolContext
    ) {
        String userId = getUserId(toolContext);
        Map<String, Plan> plans = getUserPlans(userId);

        if (planId == null || planId.isEmpty()) {
            return "Error: plan_id is required for command: update";
        }

        if (!plans.containsKey(planId)) {
            return "Error: No plan found with ID: " + planId;
        }

        Plan plan = plans.get(planId);

        if (title != null && !title.isEmpty()) {
            plan.title = title;
        }

        if (steps != null && !steps.isEmpty()) {
            List<String> oldSteps = plan.steps;
            List<String> oldStatuses = plan.stepStatuses;
            List<String> oldNotes = plan.stepNotes;

            List<String> newStatuses = new ArrayList<>();
            List<String> newNotes = new ArrayList<>();

            for (int i = 0; i < steps.size(); i++) {
                if (i < oldSteps.size() && steps.get(i).equals(oldSteps.get(i))) {
                    newStatuses.add(oldStatuses.get(i));
                    newNotes.add(oldNotes.get(i));
                } else {
                    newStatuses.add("not_started");
                    newNotes.add("");
                }
            }

            plan.steps = steps;
            plan.stepStatuses = newStatuses;
            plan.stepNotes = newNotes;
        }

        return "Plan updated successfully: " + planId + "\n\n" + formatPlan(plan);
    }

    @Tool(description = "List all available plans.")
    public String listPlans(ToolContext toolContext) {
        String userId = getUserId(toolContext);
        Map<String, Plan> plans = getUserPlans(userId);

        if (plans.isEmpty()) {
            return "No plans available. Create a plan with 'create' command.";
        }

        StringBuilder output = new StringBuilder("Available plans:\n");
        for (Map.Entry<String, Plan> entry : plans.entrySet()) {
            String planId = entry.getKey();
            Plan plan = entry.getValue();
            String currentMarker = planId.equals(getCurrentPlanId(userId)) ? " (active)" : "";

            int completed = 0;
            for (String status : plan.stepStatuses) {
                if (status.equals("completed")) completed++;
            }

            int total = plan.steps.size();
            String progress = completed + "/" + total + " steps completed";

            output.append("• ").append(planId).append(currentMarker)
                    .append(": ").append(plan.title).append(" - ").append(progress).append("\n");
        }

        return output.toString();
    }

    @Tool(description = "Get details of a specific plan.")
    public String getPlan(@ToolParam(description = "Unique identifier for the plan. If not provided, uses current active plan") String planId,
                          ToolContext toolContext) {
        String userId = getUserId(toolContext);
        Map<String, Plan> plans = getUserPlans(userId);

        if (planId == null || planId.isEmpty()) {
            if (getCurrentPlanId(userId) == null) {
                return "Error: No active plan. Please specify a plan_id or set an active plan.";
            }
            planId = getCurrentPlanId(userId);
        }

        if (!plans.containsKey(planId)) {
            return "Error: No plan found with ID: " + planId;
        }

        return formatPlan(plans.get(planId));
    }

    @Tool(description = "Set a plan as the active plan.")
    public String setActivePlan(@ToolParam(description = "Unique identifier for the plan") String planId,
                                ToolContext toolContext) {
        String userId = getUserId(toolContext);
        Map<String, Plan> plans = getUserPlans(userId);

        if (planId == null || planId.isEmpty()) {
            return "Error: plan_id is required for command: set_active";
        }

        if (!plans.containsKey(planId)) {
            return "Error: No plan found with ID: " + planId;
        }

        setCurrentPlanId(userId, planId);
        return "Plan '" + planId + "' is now the active plan.\n\n" + formatPlan(plans.get(planId));
    }

    @Tool(description = "Mark a step with a specific status and optional notes.")
    public String markStep(
            @ToolParam(description = "Unique identifier for the plan. If not provided, uses current active plan") String planId,
            @ToolParam(description = "Index of the step to update (0-based)") Integer stepIndex,
            @ToolParam(description = "Status to set for a step. Valid statuses: not_started, in_progress, completed, blocked") String stepStatus,
            @ToolParam(description = "Additional notes for a step") String stepNotes,
            ToolContext toolContext
    ) {
        String userId = getUserId(toolContext);
        Map<String, Plan> plans = getUserPlans(userId);

        if (planId == null || planId.isEmpty()) {
            if (getCurrentPlanId(userId) == null) {
                return "Error: No active plan. Please specify a plan_id or set an active plan.";
            }
            planId = getCurrentPlanId(userId);
        }

        if (!plans.containsKey(planId)) {
            return "Error: No plan found with ID: " + planId;
        }

        if (stepIndex == null) {
            return "Error: step_index is required for command: mark_step";
        }

        Plan plan = plans.get(planId);

        if (stepIndex < 0 || stepIndex >= plan.steps.size()) {
            return "Error: Invalid step_index: " + stepIndex + ". Valid indices range from 0 to " + (plan.steps.size() - 1);
        }

        if (stepStatus != null && !stepStatus.isEmpty()) {
            if (!stepStatus.equals("not_started") && !stepStatus.equals("in_progress")
                    && !stepStatus.equals("completed") && !stepStatus.equals("blocked")) {
                return "Error: Invalid step_status: " + stepStatus + ". Valid statuses are: not_started, in_progress, completed, blocked";
            }
            plan.stepStatuses.set(stepIndex, stepStatus);
        }

        if (stepNotes != null && !stepNotes.isEmpty()) {
            plan.stepNotes.set(stepIndex, stepNotes);
        }

        return "Step " + stepIndex + " updated in plan '" + planId + "'.\n\n" + formatPlan(plan);
    }

    @Tool(description = "Delete a plan.")
    public String deletePlan(@ToolParam(description = "Unique identifier for the plan") String planId,
                             ToolContext toolContext) {
        String userId = getUserId(toolContext);
        Map<String, Plan> plans = getUserPlans(userId);

        if (planId == null || planId.isEmpty()) {
            return "Error: plan_id is required for command: delete";
        }

        if (!plans.containsKey(planId)) {
            return "Error: No plan found with ID: " + planId;
        }

        plans.remove(planId);

        if (planId.equals(getCurrentPlanId(userId))) {
            setCurrentPlanId(userId, null);
        }

        return "Plan '" + planId + "' has been deleted.";
    }

    private String formatPlan(Plan plan) {
        StringBuilder output = new StringBuilder();
        output.append("Plan: ").append(plan.title).append(" (ID: ").append(plan.planId).append(")\n");
        output.append("=".repeat(plan.title.length() + 14)).append("\n\n");

        int totalSteps = plan.steps.size();
        int completed = 0;
        int inProgress = 0;
        int blocked = 0;
        int notStarted = 0;

        for (String status : plan.stepStatuses) {
            switch (status) {
                case "completed" -> completed++;
                case "in_progress" -> inProgress++;
                case "blocked" -> blocked++;
                default -> notStarted++;
            }
        }

        output.append("Progress: ").append(completed).append("/").append(totalSteps).append(" steps completed ");
        if (totalSteps > 0) {
            double percentage = ((double) completed / totalSteps) * 100;
            output.append(String.format("(%.1f%%)\n", percentage));
        } else {
            output.append("(0%)\n");
        }

        output.append("Status: ").append(completed).append(" completed, ").append(inProgress)
                .append(" in progress, ").append(blocked).append(" blocked, ").append(notStarted)
                .append(" not started\n\n");
        output.append("Steps:\n");

        for (int i = 0; i < plan.steps.size(); i++) {
            String statusSymbol = switch (plan.stepStatuses.get(i)) {
                case "not_started" -> "[ ]";
                case "in_progress" -> "[→]";
                case "completed" -> "[✓]";
                case "blocked" -> "[!]";
                default -> "[ ]";
            };

            output.append(i).append(". ").append(statusSymbol).append(" ").append(plan.steps.get(i)).append("\n");

            if (!plan.stepNotes.get(i).isEmpty()) {
                output.append("   Notes: ").append(plan.stepNotes.get(i)).append("\n");
            }
        }

        return output.toString();
    }

    public static class Plan {
        public String planId;
        public String title;
        public List<String> steps;
        public List<String> stepStatuses;
        public List<String> stepNotes;

        public Plan(String planId, String title, List<String> steps, List<String> stepStatuses, List<String> stepNotes) {
            this.planId = planId;
            this.title = title;
            this.steps = steps;
            this.stepStatuses = stepStatuses;
            this.stepNotes = stepNotes;
        }
    }
}
