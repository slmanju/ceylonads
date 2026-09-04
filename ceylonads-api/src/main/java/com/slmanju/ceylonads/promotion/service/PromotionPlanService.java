package com.slmanju.ceylonads.promotion.service;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.promotion.dto.AdminPromotionPlanRequest;
import com.slmanju.ceylonads.promotion.dto.AdminPromotionPlanUpdateRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionPlanResponse;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import com.slmanju.ceylonads.promotion.mapper.PromotionPlanMapper;
import com.slmanju.ceylonads.promotion.repository.PromotionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PromotionPlanService {

    private final PromotionPlanRepository plans;
    private final PromotionSlotService slotService;
    private final PromotionPlanMapper mapper;

    public PromotionPlanService(PromotionPlanRepository plans, PromotionSlotService slotService, PromotionPlanMapper mapper) {
        this.plans = plans;
        this.slotService = slotService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PromotionPlanResponse> activePlans() {
        return plans.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PromotionPlanResponse> allPlans() {
        return plans.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public PromotionPlanResponse create(AdminPromotionPlanRequest request) {
        if (plans.findByCode(request.code().trim()).isPresent()) {
            throw new BadRequestException("A promotion plan with this code already exists");
        }
        PromotionSlot slot = slotService.requireSlot(request.slotId());
        boolean paymentRequired = request.paymentRequired() == null || request.paymentRequired();
        boolean approvalRequired = request.approvalRequired() == null || request.approvalRequired();
        requireSupportedCombo(paymentRequired, approvalRequired);
        PromotionPlan plan = plans.save(new PromotionPlan(
                request.code().trim(),
                request.name().trim(),
                request.description().trim(),
                slot,
                request.durationDays(),
                request.price(),
                paymentRequired,
                approvalRequired,
                request.displayOrder() == null ? 0 : request.displayOrder()));
        return mapper.toResponse(plan);
    }

    @Transactional
    public PromotionPlanResponse update(Long id, AdminPromotionPlanUpdateRequest request) {
        PromotionPlan plan = requirePlan(id);
        boolean paymentRequired = request.paymentRequired() == null ? plan.isPaymentRequired() : request.paymentRequired();
        boolean approvalRequired = request.approvalRequired() == null ? plan.isApprovalRequired() : request.approvalRequired();
        requireSupportedCombo(paymentRequired, approvalRequired);
        plan.update(
                request.name().trim(),
                request.description().trim(),
                request.price(),
                request.durationDays(),
                paymentRequired,
                approvalRequired,
                request.displayOrder() == null ? plan.getDisplayOrder() : request.displayOrder());
        plan.setActive(request.active());
        return mapper.toResponse(plan);
    }

    // paymentRequired=true with approvalRequired=false has no defined activation path: a paid
    // promotion always needs an admin to approve the payment before it activates.
    private void requireSupportedCombo(boolean paymentRequired, boolean approvalRequired) {
        if (paymentRequired && !approvalRequired) {
            throw new BadRequestException("A plan that requires payment must also require approval");
        }
    }

    @Transactional
    public PromotionPlanResponse setActive(Long id, boolean active) {
        PromotionPlan plan = requirePlan(id);
        plan.setActive(active);
        return mapper.toResponse(plan);
    }

    @Transactional(readOnly = true)
    public PromotionPlan requirePlan(Long id) {
        return plans.findById(id).orElseThrow(() -> new NotFoundException("Promotion plan not found"));
    }

    // --- Channel-scoped admin surface (Tuition admin console) -------------------------------
    // Additive overloads only - every method above stays untouched and keeps serving the
    // cross-channel MAIN_SITE admin UI exactly as before.

    @Transactional(readOnly = true)
    public List<PromotionPlanResponse> allPlans(SourceChannel restrictToChannel) {
        if (restrictToChannel == null) {
            return allPlans();
        }
        return plans.findBySlot_SourceChannelOrderByDisplayOrderAscIdAsc(restrictToChannel).stream()
                .map(mapper::toResponse).toList();
    }

    @Transactional
    public PromotionPlanResponse create(AdminPromotionPlanRequest request, SourceChannel restrictToChannel) {
        if (restrictToChannel != null) {
            PromotionSlot slot = slotService.requireSlot(request.slotId());
            if (slot.getSourceChannel() != restrictToChannel) {
                throw new BadRequestException("Selected placement does not belong to the " + restrictToChannel + " channel");
            }
        }
        return create(request);
    }

    @Transactional
    public PromotionPlanResponse update(Long id, AdminPromotionPlanUpdateRequest request, SourceChannel restrictToChannel) {
        requireChannelMatch(id, restrictToChannel);
        return update(id, request);
    }

    @Transactional
    public PromotionPlanResponse setActive(Long id, boolean active, SourceChannel restrictToChannel) {
        requireChannelMatch(id, restrictToChannel);
        return setActive(id, active);
    }

    // 404s (never leaks that the plan exists in another channel) - same shape as
    // AdService.requireAny/requireOwned.
    private void requireChannelMatch(Long id, SourceChannel restrictToChannel) {
        if (restrictToChannel == null) {
            return;
        }
        if (requirePlan(id).getSlot().getSourceChannel() != restrictToChannel) {
            throw new NotFoundException("Promotion plan not found");
        }
    }
}
