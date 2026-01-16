package com.project.Emotiate.agent;

import com.project.Emotiate.config.ApplicationContextHolder;
import com.project.Emotiate.dto.packages.PackageResponseDto;
import com.project.Emotiate.dto.resourceAgt.AvailabilityRequestDto;
import com.project.Emotiate.dto.resourceAgt.AvailabilityResponseDto;
import com.project.Emotiate.dto.resourceAgt.BookingRequestDto;
import com.project.Emotiate.dto.resourceAgt.BookingResponseDto;
import com.project.Emotiate.service.InventoryService;
import com.project.Emotiate.service.PackageService;
import com.project.Emotiate.util.JsonUtil;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class ResourceMgtAgent extends Agent {

    private transient InventoryService inventoryService;
    private transient PackageService packageService;

    @Override
    protected void setup() {

        // Retrieve services via the static Spring context holder
        this.inventoryService = ApplicationContextHolder.getBean(InventoryService.class);
        this.packageService = ApplicationContextHolder.getBean(PackageService.class);

        // Register with the JADE Directory Facilitator
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("inventory-management");
        sd.setName("resource-management");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            log.info("ResourceMgtAgent registered with DF as 'resource-management'");
        } catch (FIPAException e) {
            log.error("ResourceMgtAgent DF registration failed", e);
        }

        // Start listening for QUERY_IF and REQUEST messages
        addBehaviour(new InventoryListenerBehaviour());
        log.info("ResourceMgtAgent started");
    }


    @Override
    // Method to terminate the ResourceMgt Agent
    protected void takeDown() {

        // Deregister from the Directory Facilitator on shutdown
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            log.error("ResourceMgtAgent DF deregistration failed", e);
        }
        log.info("ResourceMgtAgent shutting down");
    }


    // Method to handle incoming messages related to availability queries, booking requests, and package queries
    private class InventoryListenerBehaviour extends CyclicBehaviour {

        private final MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
                ),
                MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF)
        );

        @Override
        public void action() {

            ACLMessage msg = myAgent.receive(mt);

            if (msg == null) {
                block();
                return;
            }

            log.info("ResourceMgtAgent received {} from {}",
                    ACLMessage.getPerformative(msg.getPerformative()),
                    msg.getSender().getLocalName());

            if (msg.getPerformative() == ACLMessage.QUERY_IF) {
                handleAvailabilityQuery(msg);
            } else if (msg.getPerformative() == ACLMessage.REQUEST) {
                handleBookingRequest(msg);
            } else if (msg.getPerformative() == ACLMessage.QUERY_REF) {
                handlePackageQuery(msg);
            }
        }


        // Parse availability query and reply with AvailabilityResponseDto
        private void handleAvailabilityQuery(ACLMessage msg) {

            try {
                // Parse the incoming availability query from message content
                AvailabilityRequestDto request = JsonUtil.fromJson(
                        msg.getContent(), AvailabilityRequestDto.class);

                AvailabilityResponseDto result = inventoryService.checkAvailability(request);

                // Serialize result and send INFORM reply
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(JsonUtil.toJson(result));
                reply.setLanguage("JSON");
                reply.setOntology("hotel-inventory");
                myAgent.send(reply);

                log.info("ResourceMgtAgent sent availability INFORM  available={} package={}",
                        result.isAvailable(), request.getPackageId());

            } catch (Exception e) {
                log.error("ResourceMgtAgent failed to handle QUERY_IF", e);
                sendFailure(msg, "Availability query failed: " + e.getMessage());
            }
        }


        // Parse booking request and reply with BookingResponseDto
        private void handleBookingRequest(ACLMessage msg) {

            try {
                // Parse the booking creation request from message content
                BookingRequestDto request = JsonUtil.fromJson(
                        msg.getContent(), BookingRequestDto.class);

                // Delegate to the inventory service booking confirmation
                BookingResponseDto result = inventoryService.confirmBooking(request);

                // Serialize result and send INFORM reply
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(JsonUtil.toJson(result));
                reply.setLanguage("JSON");
                reply.setOntology("hotel-inventory");
                myAgent.send(reply);

                log.info("ResourceMgtAgent sent booking INFORM  status={} reference={}",
                        result.getStatus(), result.getReference());

            } catch (Exception e) {
                log.error("ResourceMgtAgent failed to handle REQUEST", e);
                sendFailure(msg, "Booking request failed: " + e.getMessage());
            }
        }

        // Parse add-on/guest-count requirements and reply with matching packages
        private void handlePackageQuery(ACLMessage msg) {

            try {
                // Parse the requirements from message content
                Map<?, ?> content = JsonUtil.fromJson(msg.getContent(), Map.class);

                @SuppressWarnings("unchecked")
                List<String> addOns = (List<String>) content.get("addOns");
                Integer guestCount = content.get("guestCount") != null
                        ? ((Number) content.get("guestCount")).intValue()
                        : null;

                // Retrieve matching active packages from the package service
                List<PackageResponseDto> result = packageService.getPackagesByRequirements(addOns, guestCount);

                // Serialize result and send INFORM reply
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(JsonUtil.toJson(result));
                reply.setLanguage("JSON");
                reply.setOntology("hotel-inventory");
                myAgent.send(reply);

                log.info("ResourceMgtAgent sent package INFORM  matched={}", result.size());

            } catch (Exception e) {
                log.error("ResourceMgtAgent failed to handle QUERY_REF", e);
                sendFailure(msg, "Package query failed: " + e.getMessage());
            }
        }


        // Send a FAILURE reply back to the sender
        private void sendFailure(ACLMessage original, String reason) {
            ACLMessage failure = original.createReply();
            failure.setPerformative(ACLMessage.FAILURE);
            failure.setContent(reason);
            myAgent.send(failure);
        }
    }
}