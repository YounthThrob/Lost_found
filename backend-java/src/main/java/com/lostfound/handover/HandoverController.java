package com.lostfound.handover;

import com.lostfound.account.RequiresAgreement;
import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HandoverController {

    private final HandoverService service;

    public HandoverController(HandoverService service) {
        this.service = service;
    }

    public record HandoverView(String id, String claimId, String proposerId, String timeWindow,
                               String publicPlace, String ownerConfirmedAt,
                               String claimantConfirmedAt, String status, String createdAt) {
        static HandoverView from(Handover h) {
            return new HandoverView(h.id(), h.claimId(), h.proposerId(), h.timeWindow(),
                h.publicPlace(), h.ownerConfirmedAt(), h.claimantConfirmedAt(), h.status(),
                h.createdAt());
        }
    }

    @PostMapping("/handovers")
    @RequiresAgreement
    public Envelope.Response<HandoverView> createHandover(Authentication authentication,
                                                          @RequestBody HandoverService.HandoverCreate request) {
        return Envelope.ok(HandoverView.from(service.create(authentication.getName(), request)));
    }

    @PostMapping("/handovers/{handoverId}/confirm")
    public Envelope.Response<HandoverView> confirmHandover(Authentication authentication,
                                                           @PathVariable String handoverId) {
        return Envelope.ok(HandoverView.from(service.confirm(authentication.getName(), handoverId)));
    }

    @PostMapping("/handovers/{handoverId}/cancel")
    public Envelope.Response<HandoverView> cancelHandover(Authentication authentication,
                                                          @PathVariable String handoverId) {
        return Envelope.ok(HandoverView.from(service.cancel(authentication.getName(), handoverId)));
    }
}
