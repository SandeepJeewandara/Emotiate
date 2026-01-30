package com.project.Emotiate.agent;

import com.project.Emotiate.config.ApplicationContextHolder;
import com.project.Emotiate.dto.chat.UserAgentReplyDto;
import com.project.Emotiate.dto.sellerAgt.SellerReplyContentDto;
import com.project.Emotiate.dto.session.AgentMessageDto;
import com.project.Emotiate.service.NegotiationService;
import com.project.Emotiate.util.JsonUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserAgent extends Agent {

    // Session this agent instance is bound to
    private String sessionId;

    // Guest name this agent instance is bound to
    private String guestName;

    // JADE local name of the paired SellerAgent for this session
    private String pairedSellerAgentName;

    private NegotiationService negotiationService;
    private MessageTemplate sellerReplyTemplate;

    @Override
    // Method to set up the User Agent
    protected void setup() {

        // Retrieve constructor arguments supplied by AgentManagerServiceImpl
        Object[] args = getArguments();
        if (args != null && args.length >= 1) {
            this.sessionId = (String) args[0];
        }
        if (args != null && args.length >= 2) {
            this.guestName = (String) args[1];
        }

        // Derive the paired SellerAgent name from the sessionId
        this.pairedSellerAgentName = "SellerAgent-" + sessionId;

        // Build a MessageTemplate that only matches replies from the paired SellerAgent
        this.sellerReplyTemplate = MessageTemplate.and(
                MessageTemplate.MatchConversationId(sessionId),
                MessageTemplate.MatchSender(new AID(pairedSellerAgentName, AID.ISLOCALNAME))
        );

        // Enable the O2A channel
        setEnabledO2ACommunication(true, 0);

        // Obtain NegotiationService from the Spring application context
        this.negotiationService = ApplicationContextHolder.getBean(NegotiationService.class);

        log.info("UserAgent started for session: {} | paired with: {}", sessionId, pairedSellerAgentName);

        // Add the O2A listener behaviour that runs continuously during this session
        addBehaviour(new O2AListenerBehaviour());
        addBehaviour(new SellerReplyListenerBehaviour());
    }


    @Override
    // Method to terminate the User Agent
    protected void takeDown() {

        // Disable the O2A channel cleanly when the agent is killed
        setEnabledO2ACommunication(false, 0);
        log.info("UserAgent terminated for session: {}", sessionId);
    }


    // Method to polls the O2A queue for AgentMessageDto objects pushed by Spring
    private class O2AListenerBehaviour extends CyclicBehaviour{

        @Override
        public void action() {

            // Poll the O2A queue
            AgentMessageDto incomingMessage = (AgentMessageDto) getO2AObject();

            // Validate incoming message
            if (incomingMessage == null) {

                log.warn("User Agent {} received an empty AgentMessageDto", sessionId);
                block();
                return;
            }

            log.info("UserAgent {} received O2A message - round {}", sessionId, incomingMessage.getRound());

            try{

                // Build a CFP (Call For Proposal) directed at the SellerAgent
                ACLMessage cfp = buildCfpMessage(incomingMessage);
                send(cfp);
                log.info("UserAgent {} sent CFP to {}  round {}", sessionId, pairedSellerAgentName, incomingMessage.getRound());

            }catch(Exception e){
                log.error("UserAgent {} failed to dispatch CFP: {}", sessionId, e.getMessage(), e);
            }
        }
    }


    // Builds an ACL CFP message addressed to the SellerAgent with the conversation context
    private ACLMessage buildCfpMessage(AgentMessageDto message) {

        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

        // Address the paired SellerAgent created for this session
        cfp.addReceiver(new AID(pairedSellerAgentName, AID.ISLOCALNAME));

        // Store session ID in the conversation-id field for traceability in logs
        cfp.setConversationId(message.getSessionId());

        // Encode the full AgentMessageDto as JSON in the message content field
        cfp.setContent(JsonUtil.toJson(message));

        cfp.setLanguage("JSON");
        cfp.setOntology("hotel-negotiation");

        return cfp;
    }


    // Method to listen for ACL replies from the paired SellerAgent
    private class SellerReplyListenerBehaviour extends CyclicBehaviour {

        @Override
        public void action() {

            ACLMessage reply = myAgent.receive(sellerReplyTemplate);

            // Validate agent reply
            if (reply == null) {

                log.warn("UserAgent {} received an empty ACLMessage from SellerAgent", sessionId);
                block();
                return;
            }

            log.info("UserAgent {} received ACL reply from {} | performative: {}", sessionId, pairedSellerAgentName, ACLMessage.getPerformative(reply.getPerformative()));
            handleSellerReply(reply);
        }
    }


    // Routes the reply to the appropriate handler based on ACL performative
    private void handleSellerReply(ACLMessage reply) {

        switch (reply.getPerformative()) {

            case ACLMessage.PROPOSE:
                handlePropose(reply);
                break;

            case ACLMessage.REFUSE:
                handleRefuse(reply);
                break;

            case ACLMessage.FAILURE:
                handleFailure(reply);
                break;

            default:
                log.warn("UserAgent {}  unhandled performative {} from {}",
                        sessionId,
                        ACLMessage.getPerformative(reply.getPerformative()),
                        pairedSellerAgentName);
        }
    }


    // Deserializes the PROPOSE content into a Dto
    private void handlePropose(ACLMessage reply) {

        SellerReplyContentDto replyContent;
        try {
            // Use JsonUtil to deserialize the ACL content
            replyContent = JsonUtil.fromJson(reply.getContent(), SellerReplyContentDto.class);

        } catch (RuntimeException ex) {
            log.error("UserAgent {} failed to parse PROPOSE content from {} | payload: {}",
                    sessionId, pairedSellerAgentName, reply.getContent(), ex);
            handleParseFailure();
            return;
        }

        if (replyContent == null) {
            log.error("UserAgent {}  failed to deserialize PROPOSE content from {}", sessionId, pairedSellerAgentName);
            handleParseFailure();
            return;
        }

        // Delegate DB persistence and WebSocket push to the Spring service
        negotiationService.saveAgentReply(
                UserAgentReplyDto.builder()
                        .sessionId(sessionId)
                        .agentReply(replyContent.getAgentReply())
                        .detectedEmotion(replyContent.getDetectedEmotion())
                        .offeredPrice(replyContent.getOfferedPrice())
                        .metadata(replyContent.getMetadata())
                        .bookingComplete(replyContent.getBookingComplete())
                        .build()
        );

        log.info("UserAgent {} persisted PROPOSE reply for user : {} | emotion={} strategy={} ",
                sessionId, guestName, replyContent.getDetectedEmotion(), replyContent.getStrategy());
    }


    // Persists a fallback message when the PROPOSE JSON content could not be parsed
    private void handleParseFailure() {

        String fallback = "I'm sorry, I encountered an issue preparing your offer. Please try again later.";
        negotiationService.saveAgentReply(
                UserAgentReplyDto.builder().sessionId(sessionId).agentReply(fallback).build()
        );
    }


    // Handles a REFUSE reply when SellerAgent declined to make an offer
    private void handleRefuse(ACLMessage reply) {

        log.warn("UserAgent {} received REFUSE from {} | reason: {}", sessionId, pairedSellerAgentName, reply.getContent());
        negotiationService.saveAgentReply(
                UserAgentReplyDto.builder().sessionId(sessionId).agentReply(reply.getContent()).build()
        );
    }


    // Handles a FAILURE reply when an internal error occurred
    private void handleFailure(ACLMessage reply) {

        log.error("UserAgent {} received FAILURE from {} | detail: {}", sessionId, pairedSellerAgentName, reply.getContent());
        String fallback = "I'm sorry, something went wrong while processing your request. Please try again.";
        negotiationService.saveAgentReply(
                UserAgentReplyDto.builder().sessionId(sessionId).agentReply(fallback).build()
        );
    }
}
