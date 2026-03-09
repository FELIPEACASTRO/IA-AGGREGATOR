package com.ia.aggregator.presentation.billing;

import com.ia.aggregator.application.billing.port.in.BillingPlanUseCase;
import com.ia.aggregator.application.billing.port.in.BudgetUseCase;
import com.ia.aggregator.application.billing.port.in.SubscriptionUseCase;
import com.ia.aggregator.application.billing.port.in.UsageMeteringUseCase;
import com.ia.aggregator.domain.billing.BudgetAlert;
import com.ia.aggregator.domain.billing.PlanTier;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Billing, subscription, and budget management endpoints.
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final SubscriptionUseCase subscriptionUseCase;
    private final UsageMeteringUseCase usageUseCase;
    private final BudgetUseCase budgetUseCase;
    private final BillingPlanUseCase billingPlanUseCase;

    public BillingController(SubscriptionUseCase subscriptionUseCase,
                              UsageMeteringUseCase usageUseCase,
                              BudgetUseCase budgetUseCase,
                              BillingPlanUseCase billingPlanUseCase) {
        this.subscriptionUseCase = subscriptionUseCase;
        this.usageUseCase = usageUseCase;
        this.budgetUseCase = budgetUseCase;
        this.billingPlanUseCase = billingPlanUseCase;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<BillingPlanUseCase.PlanDto>> getPlans() {
        return ResponseEntity.ok(billingPlanUseCase.getActivePlans());
    }

    @GetMapping("/subscription")
    @RequiresPermission(Permission.ORG_BILLING_READ)
    public ResponseEntity<SubscriptionUseCase.SubscriptionInfo> getSubscription(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(subscriptionUseCase.getSubscription(user.getOrgId()));
    }

    @PostMapping("/subscription")
    @RequiresPermission(Permission.ORG_BILLING_MANAGE)
    public ResponseEntity<SubscriptionUseCase.SubscriptionInfo> subscribe(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> body) {
        PlanTier tier = PlanTier.valueOf(body.get("tier").toUpperCase());
        String paymentMethodId = body.get("paymentMethodId");
        return ResponseEntity.ok(subscriptionUseCase.subscribe(user.getOrgId(), tier, paymentMethodId));
    }

    @PatchMapping("/subscription")
    @RequiresPermission(Permission.ORG_BILLING_MANAGE)
    public ResponseEntity<SubscriptionUseCase.SubscriptionInfo> changePlan(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> body) {
        PlanTier newTier = PlanTier.valueOf(body.get("tier").toUpperCase());
        return ResponseEntity.ok(subscriptionUseCase.changePlan(user.getOrgId(), newTier));
    }

    @DeleteMapping("/subscription")
    @RequiresPermission(Permission.ORG_BILLING_MANAGE)
    public ResponseEntity<Map<String, String>> cancelSubscription(
            @AuthenticationPrincipal AuthenticatedUser user) {
        subscriptionUseCase.cancelSubscription(user.getOrgId());
        return ResponseEntity.ok(Map.of("status", "canceled"));
    }

    @GetMapping("/usage")
    @RequiresPermission(Permission.ORG_BILLING_READ)
    public ResponseEntity<Map<String, UsageMeteringUseCase.UsageSummary>> getUsage(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "30") int days) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        return ResponseEntity.ok(usageUseCase.getUsageSummary(user.getOrgId(), from, Instant.now()));
    }

    @GetMapping("/usage/cost")
    @RequiresPermission(Permission.ORG_BILLING_READ)
    public ResponseEntity<Map<String, Object>> getTotalCost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "30") int days) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        double cost = usageUseCase.getTotalCost(user.getOrgId(), from, Instant.now());
        return ResponseEntity.ok(Map.of("totalCostUsd", cost, "periodDays", days));
    }

    @PostMapping("/budgets")
    @RequiresPermission(Permission.ORG_BILLING_MANAGE)
    public ResponseEntity<BudgetAlert> createBudget(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(budgetUseCase.createBudget(
                user.getOrgId(),
                (String) body.get("name"),
                ((Number) body.get("budgetUsd")).doubleValue(),
                ((Number) body.get("threshold")).doubleValue(),
                BudgetAlert.BudgetAction.valueOf(((String) body.get("action")).toUpperCase())
        ));
    }

    @GetMapping("/budgets")
    @RequiresPermission(Permission.ORG_BILLING_READ)
    public ResponseEntity<List<BudgetAlert>> getBudgets(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(budgetUseCase.getBudgets(user.getOrgId()));
    }
}
