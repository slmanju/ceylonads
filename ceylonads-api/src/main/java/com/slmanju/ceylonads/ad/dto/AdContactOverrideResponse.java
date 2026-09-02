package com.slmanju.ceylonads.ad.dto;

// The raw ad-specific contact override as stored, each field independently nullable when unset.
// Populated only on responses returned to the ad's own owner (create/update/mine) so Edit can
// tell "no override, showing account fallback" apart from "override happens to match the
// account value" instead of guessing from the resolved AdContactResponse.
public record AdContactOverrideResponse(
        String contactName,
        String phoneNumber,
        String whatsappNumber) {
}
