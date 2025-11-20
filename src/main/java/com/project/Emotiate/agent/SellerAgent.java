package com.project.Emotiate.agent;

import com.project.Emotiate.dto.sellerAgt.SellerReplyContentDto;
import com.project.Emotiate.util.JsonUtil;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class SellerAgent extends Agent {

    private String sessionId;
    private MessageTemplate sessionMessageTemplate;

    @Override
    protected void setup() {

        Object[] args = getArguments();
        if (args != null && args.length >= 1) {
            this.sessionId = (String) args[0];
        }

        this.sessionMessageTemplate = MessageTemplate.MatchConversationId(sessionId);

        log.info("SellerAgent started for session: {}", sessionId);
        addBehaviour(new CfpListenerBehaviour());
    }

    private class CfpListenerBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage cfp = myAgent.receive(sessionMessageTemplate);

            if (cfp == null) {
                block();
                return;
            }

            log.info("SellerAgent {} received {} from {}", sessionId, ACLMessage.getPerformative(cfp.getPerformative()), cfp.getSender().getLocalName());

            Map incomingMessage = null;
            try {
                incomingMessage = JsonUtil.fromJson(cfp.getContent(), Map.class);
            } catch (RuntimeException ex) {
                log.warn("SellerAgent {} failed to parse incoming message content", sessionId, ex);
            }

            Object guestMessage = incomingMessage == null ? null : incomingMessage.get("guestMessage");
            String guestText = (guestMessage == null || guestMessage.toString().isBlank())
                    ? "your request"
                    : guestMessage.toString();

            SellerReplyContentDto replyContent = SellerReplyContentDto.builder()
                    .agentReply("Temporary seller response: I received your message (" + guestText + ").")
                    .detectedEmotion("neutral")
                    .strategy("temporary-fallback")
                    .offeredPrice(199.0)
                    .metadata(null)
                    .bookingComplete(false)
                    .build();

            ACLMessage propose = cfp.createReply();
            propose.setPerformative(ACLMessage.PROPOSE);
            propose.setContent(JsonUtil.toJson(replyContent));
            propose.setLanguage("JSON");
            propose.setOntology("hotel-negotiation");

            send(propose);
            log.info("SellerAgent {} sent temporary PROPOSE reply", sessionId);
        }
    }
}
