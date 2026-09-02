package com.slmanju.ceylonads.ad.dto;

// The effective/resolved contact info buyers should use: ad-specific override where present,
// otherwise the seller's account contact. Populated on the public ad detail response only - not
// on search/list responses, which don't need per-ad contact data (see AdMapper).
public record AdContactResponse(
        String name,
        String phoneNumber,
        String whatsappNumber) {
}
