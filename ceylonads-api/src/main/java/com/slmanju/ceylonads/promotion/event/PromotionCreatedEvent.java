package com.slmanju.ceylonads.promotion.event;

import com.slmanju.ceylonads.promotion.entity.Promotion;

/**
 * Published synchronously, within the same transaction, right after a new {@link Promotion} is
 * saved. Lets Phase 3 payments react (create the matching bank-transfer Payment record) without
 * PromotionService needing to know the payment domain exists - the dependency runs one way,
 * from payment to promotion, same as everywhere else in this codebase.
 */
public record PromotionCreatedEvent(Promotion promotion) {
}
