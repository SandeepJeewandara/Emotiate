package com.project.Emotiate.agent;


import com.fasterxml.jackson.core.type.TypeReference;
import com.project.Emotiate.config.ApplicationContextHolder;
import com.project.Emotiate.dto.chat.UserAgentReplyDto;
import com.project.Emotiate.dto.packages.PackageResponseDto;
import com.project.Emotiate.dto.resourceAgt.AvailabilityRequestDto;
import com.project.Emotiate.dto.resourceAgt.AvailabilityResponseDto;
import com.project.Emotiate.dto.resourceAgt.BookingRequestDto;
import com.project.Emotiate.dto.resourceAgt.BookingResponseDto;
import com.project.Emotiate.dto.sellerAgt.GetInfoResultDto;
import com.project.Emotiate.dto.sellerAgt.GuestDetailsResultDto;
import com.project.Emotiate.dto.sellerAgt.SellerNegotiationContext;
import com.project.Emotiate.entity.Guest;
import com.project.Emotiate.repository.GuestRepository;
import com.project.Emotiate.dto.sellerAgt.SellerReplyContentDto;
import com.project.Emotiate.dto.session.AgentMessageDto;
import com.project.Emotiate.dto.emotion.EmotionResultDto;
import com.project.Emotiate.dto.sellerAgt.NegotiationReplyResultDto;
import com.project.Emotiate.dto.strategy.NegotiationBounds;
import com.project.Emotiate.dto.strategy.StrategyResultDto;
import com.project.Emotiate.enums.NegotiationPhase;
import com.project.Emotiate.enums.NegotiationStrategy;
import com.project.Emotiate.module.EmotionDetectionModule;
import com.project.Emotiate.module.SellerReplyModule;
import com.project.Emotiate.module.StrategyOptimizationModule;
import com.project.Emotiate.service.NegotiationService;
import com.project.Emotiate.util.JsonUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SellerAgent extends Agent {

    // Session state
    private SellerNegotiationContext ctx;

    // Paired UserAgent name
    private String pairedUserAgentName;

    // CFP template for paired UserAgent
    private MessageTemplate cfpTemplate;

    // Stored CFP from previous state
    private ACLMessage pendingCfp;

    // FSM transition codes
    private static final int STAY = 0;
    private static final int BACK = 2;
    private static final int NEXT = 1;


    // Spring services (transient)
    private transient EmotionDetectionModule emotionDetectionModule;
    private transient StrategyOptimizationModule strategyOptimizationModule;
    private transient SellerReplyModule sellerReplyModule;
    private transient GuestRepository guestRepository;
    private transient NegotiationService negotiationService;


    @Override
    // Initialize agent
    protected void setup() {

        // Read startup args
        String sessionId = null;
        Object[] args = getArguments();
        if (args != null && args.length >= 1) {
            sessionId = (String) args[0];
        }

        // Initialize session context
        this.ctx = SellerNegotiationContext.newSession(sessionId);

        // Build paired UserAgent name
        this.pairedUserAgentName = "UserAgent-" + sessionId;

        // Match CFP for session and sender
        this.cfpTemplate = MessageTemplate.and(
                MessageTemplate.MatchConversationId(sessionId),
                MessageTemplate.MatchSender(new AID(pairedUserAgentName, AID.ISLOCALNAME))
        );

        // Load Spring beans
        this.emotionDetectionModule     = ApplicationContextHolder.getBean(EmotionDetectionModule.class);
        this.strategyOptimizationModule = ApplicationContextHolder.getBean(StrategyOptimizationModule.class);
        this.sellerReplyModule          = ApplicationContextHolder.getBean(SellerReplyModule.class);
        this.guestRepository            = ApplicationContextHolder.getBean(GuestRepository.class);
        this.negotiationService         = ApplicationContextHolder.getBean(NegotiationService.class);

        // Build and register the FSM that drives negotiation phases
        addBehaviour(buildFsm());

        log.info("SellerAgent started for session: {} | paired with: {}", sessionId, pairedUserAgentName);
    }


    @Override
    // Log shutdown
    protected void takeDown() {
        log.info("SellerAgent terminated for session: {} | totalTurns={} bookingComplete={}",
                ctx.getSessionId(), ctx.getTotalTurns(), ctx.isBookingComplete());
    }


    // Builds the FSMBehaviour to handle negotiation phase states and their transitions
    private FSMBehaviour buildFsm() {

        FSMBehaviour fsm = new FSMBehaviour(this);

        // Register phase states
        fsm.registerFirstState(new GetInfoState(),      NegotiationPhase.GET_INFO.name());
        fsm.registerState(new RecommendPackageState(),  NegotiationPhase.RECOMMEND_PACKAGE.name());
        fsm.registerState(new NegotiatingState(),       NegotiationPhase.NEGOTIATING.name());
        fsm.registerState(new BookingState(),           NegotiationPhase.BOOKING.name());
        fsm.registerLastState(new ConfirmationState(),  NegotiationPhase.CONFIRMATION.name());

        // Register state transitions
        fsm.registerDefaultTransition(NegotiationPhase.GET_INFO.name(),          NegotiationPhase.GET_INFO.name());
        fsm.registerTransition(       NegotiationPhase.GET_INFO.name(),          NegotiationPhase.RECOMMEND_PACKAGE.name(), NEXT);

        fsm.registerDefaultTransition(NegotiationPhase.RECOMMEND_PACKAGE.name(), NegotiationPhase.RECOMMEND_PACKAGE.name());
        fsm.registerTransition(       NegotiationPhase.RECOMMEND_PACKAGE.name(), NegotiationPhase.NEGOTIATING.name(), NEXT);
        fsm.registerTransition(       NegotiationPhase.RECOMMEND_PACKAGE.name(), NegotiationPhase.GET_INFO.name(),    BACK);

        fsm.registerDefaultTransition(NegotiationPhase.NEGOTIATING.name(),       NegotiationPhase.NEGOTIATING.name());
        fsm.registerTransition(       NegotiationPhase.NEGOTIATING.name(),       NegotiationPhase.BOOKING.name(),           NEXT);

        fsm.registerDefaultTransition(NegotiationPhase.BOOKING.name(),           NegotiationPhase.BOOKING.name());
        fsm.registerTransition(       NegotiationPhase.BOOKING.name(),           NegotiationPhase.CONFIRMATION.name(),      NEXT);

        return fsm;
    }


    // PHASE 1 : Collect check-in date, check-out date, and guest count from the guest
    private class GetInfoState extends SimpleBehaviour {

        private boolean done = false;
        private int transition = STAY;

        @Override
        public void action() {

            ACLMessage cfp = myAgent.receive(cfpTemplate);

            if (cfp == null) {
                block();
                return;
            }

            // Parse the incoming AgentMessageDto from the CFP content
            AgentMessageDto message = JsonUtil.fromJson(cfp.getContent(), AgentMessageDto.class);

            if (message == null) {
                log.error("SellerAgent {} GET_INFO received null AgentMessageDto", ctx.getSessionId());
                sendFailure(cfp, "Failed to parse incoming message");
                done = true;
                return;
            }

            // Update context with data from the incoming message
            ctx.setTotalTurns(ctx.getTotalTurns() + 1);

            // Set Guest name
            if (ctx.getGuestName() == null) ctx.setGuestName(message.getGuestName());

            log.debug("SellerAgent {} GET_INFO | turn={}", ctx.getSessionId(), ctx.getTotalTurns());

            try {
                // Call LLM with get-info prompt
                GetInfoResultDto result = sellerReplyModule.generateInfoReply(
                        message.getGuestMessage(),
                        message.getConversationHistory(),
                        ctx.getGuestName(),
                        ctx.getCheckInDate(),
                        ctx.getCheckOutDate(),
                        ctx.getGuestCount()
                );

                // Update ctx with any newly extracted values
                if (result.getCheckInDate()  != null) ctx.setCheckInDate(result.getCheckInDate());
                if (result.getCheckOutDate() != null) ctx.setCheckOutDate(result.getCheckOutDate());
                if (result.getGuestCount()   != null) ctx.setGuestCount(result.getGuestCount());
                if (result.getNights() != null) ctx.setNights(result.getNights());

                if (!result.isAllCollected()) {

                    // If still missing details ask for them
                    sendPropose(cfp, result.getReply());
                } else {

                    // Hand cfp to RECOMMEND_PACKAGE
                    log.debug("SellerAgent {} GET_INFO complete | checkIn={} checkOut={} guestCount={} nights={}",
                            ctx.getSessionId(), ctx.getCheckInDate(), ctx.getCheckOutDate(), ctx.getGuestCount(), ctx.getNights());

                    // Move GET_INFO state to RECOMMEND_PACKAGE state
                    pendingCfp = cfp;
                    ctx.setPhase(NegotiationPhase.RECOMMEND_PACKAGE);
                    transition = NEXT;
                }

            } catch (Exception e) {
                log.error("SellerAgent {} GET_INFO failed to generate reply", ctx.getSessionId(), e);
                sendFailure(cfp, "Failed to generate reply: " + e.getMessage());
            }

            // Mark done
            done = true;
        }

        @Override public boolean done() { return done; }
        @Override public int onEnd() { return transition; }
        @Override public void reset() {super.reset(); done = false; transition = STAY;
            
        }
    }


    // PHASE 2 : Query ResourceMgtAgent for packages and present availability to guest
    private class RecommendPackageState extends SimpleBehaviour {

        private boolean done = false;
        private int transition = STAY;

        @Override
        public void action() {

            // Validate CFP from GET_INFO
            if (pendingCfp == null) {
                block();
                return;
            }
            ACLMessage cfp = pendingCfp;
            pendingCfp = null;

            // Increment turn counter
            ctx.setTotalTurns(ctx.getTotalTurns() + 1);

            log.debug("SellerAgent {} RECOMMEND_PACKAGE | checkIn={} checkOut={} guestCount={}",
                    ctx.getSessionId(), ctx.getCheckInDate(), ctx.getCheckOutDate(), ctx.getGuestCount());

            // Find ResourceMgtAgent in DF
            AID resourceMgtAID = findResourceMgtAgent();
            if (resourceMgtAID == null) {
                sendFailure(cfp, "ResourceMgtAgent not available");
                done = true;
                return;
            }

            // Query ResourceMgtAgent for packages matching guestCount
            List<PackageResponseDto> allPackages = queryPackages(resourceMgtAID);
            if (allPackages == null || allPackages.isEmpty()) {
                sendFailure(cfp, "No packages found.");
                done = true;
                return;
            }

            // Filter out available packages for the specified dates
            List<AvailabilityResponseDto> available = new ArrayList<>();
            for (PackageResponseDto pkg : allPackages) {
                AvailabilityResponseDto avail = checkPackageAvailability(resourceMgtAID, pkg.getId());
                if (avail != null) {
                    if (avail.isAvailable()) available.add(avail);
                }
            }
            ctx.setAvailablePackages(available);


            // Send user back to GET_INFO if no packages available
            if (available.isEmpty()) {

                // Clear dates in GET_INFO
                ctx.setCheckInDate(null);
                ctx.setCheckOutDate(null);
                ctx.setPhase(NegotiationPhase.GET_INFO);

                String noAvailReply = sellerReplyModule.generatePackageReply(
                        ctx.getGuestName(),
                        ctx.getCheckInDate(),
                        ctx.getCheckOutDate(),
                        ctx.getGuestCount(),
                        Collections.emptyList()
                );
                sendPropose(cfp, noAvailReply);
                transition = BACK;
                done = true;
                return; 
            }

            // Generate a package reply based on guest details
            String packageReply = sellerReplyModule.generatePackageReply(
                    ctx.getGuestName(),
                    ctx.getCheckInDate(),
                    ctx.getCheckOutDate(),
                    ctx.getGuestCount(),
                    available
            );
            sendPropose(cfp, packageReply, buildPackageMetadata(available));

            // Move RECOMMEND_PACKAGE state to NEGOTIATING state
            ctx.setPhase(NegotiationPhase.NEGOTIATING);
            transition = NEXT;
            done = true;
        }

        @Override public boolean done() { return done; }
        @Override public int onEnd() { return transition; }
        @Override public void reset() { super.reset(); done = false; transition = STAY; }
    }


    // PHASE 3 : Run EDM, SOM pipeline and generate a negotiation reply
    private class NegotiatingState extends SimpleBehaviour {

        private boolean done = false;
        private int transition = STAY;

        @Override
        public void action() {

            // Retrieve CFP from RECOMMEND_PACKAGE
            ACLMessage cfp = myAgent.receive(cfpTemplate);
            if (cfp == null) { block(); return; }

            // Parse incoming message
            AgentMessageDto message = JsonUtil.fromJson(cfp.getContent(), AgentMessageDto.class);
            if (message == null) {
                sendFailure(cfp, "Failed to parse message");
                done = true; return;
            }

            // Increment turn counter
            ctx.setTotalTurns(ctx.getTotalTurns() + 1);

            // Start counting negotiation rounds
            if (!ctx.isNegotiationStarted()) ctx.setNegotiationStarted(true);
            ctx.setNegotiationRound(ctx.getNegotiationRound() + 1);
            ctx.setAbortEligible(ctx.getNegotiationRound() >= 4);

            log.debug("SellerAgent {} NEGOTIATING | round={} turn={}",
                    ctx.getSessionId(), ctx.getNegotiationRound(), ctx.getTotalTurns());

            String guestMessage = message.getGuestMessage();
            List<String> history = message.getConversationHistory();

            // Run Emotion Detectio Module (EDM)
            EmotionResultDto emotion = emotionDetectionModule.detectEmotion(guestMessage);
            log.debug("SellerAgent {} EDM | emotion={} confidence={}",
                    ctx.getSessionId(), emotion.getEmotion(), emotion.getConfidence());

            // Run Strategy Optimization Module (SOM) only when a package has been selected
            StrategyResultDto strategy;
            if (ctx.getSelectedPackage() != null) {
                NegotiationBounds bounds = buildBounds();
                strategy = strategyOptimizationModule.optimize(
                        emotion, ctx.getNegotiationRound(), ctx.getRemainingBudget(), bounds);
                log.debug("SellerAgent {} SOM | strategy={} price={}",
                        ctx.getSessionId(), strategy.getStrategy(), strategy.getNewPrice());

            } else {

                // Default strategy when no package is selected
                strategy = StrategyResultDto.builder()
                        .strategy(NegotiationStrategy.HOLD_FIRM)
                        .tone(com.project.Emotiate.enums.EmotionTone.PROFESSIONAL)
                        .newPrice(0.0)
                        .discountRate(0.0)
                        .boundaryHit(false)
                        .build();
                log.debug("SellerAgent {} SOM skipped — no package selected yet, using HOLD_FIRM",
                        ctx.getSessionId());
            }

            // LLM call for detects package selection and generates reply
            NegotiationReplyResultDto result = sellerReplyModule.generateNegotiatingReply(
                    guestMessage, history, emotion, strategy,
                    ctx.getAvailablePackages(), ctx.getSelectedPackage(),
                    ctx.getCurrentOfferedPrice(), ctx.isAbortEligible());

            if (result.isAbortRequested()) {
                log.info("SellerAgent {} aborting negotiation at round {}", ctx.getSessionId(), ctx.getNegotiationRound());
                abortNegotiation(result.getReply());
                done = true;
                return;
            }

            // If the LLM detected a new (or changed) package selection, update ctx
            boolean isNewSelection = result.getSelectedPackage() != null && ctx.getSelectedPackage() == null;
            boolean isPackageSwitch = result.getSelectedPackage() != null
                    && ctx.getSelectedPackage() != null
                    && !result.getSelectedPackage().equalsIgnoreCase(ctx.getSelectedPackage().getPackageName());
            if (isNewSelection || isPackageSwitch) {
                AvailabilityResponseDto matched = findPackageByName(result.getSelectedPackage());
                if (matched != null) {
                    ctx.setSelectedPackage(matched);
                    ctx.setSelectedPackageId(matched.getPackageId());
                    double upper = matched.getUpperBoundPrice().doubleValue();
                    double lower = matched.getLowerBoundPrice().doubleValue();

                    // Set bound prices
                    ctx.setUpperBound(upper);
                    ctx.setLowerBound(lower);
                    ctx.setBasePrice(upper);

                    // Start negotiations from base price and track remaining discount budget from it
                    ctx.setCurrentOfferedPrice(upper);
                    ctx.setRemainingBudget(buildBounds().maxDiscountRate());

                    // Reset round counter so SOM starts fresh for the new package
                    if (isPackageSwitch) ctx.setNegotiationRound(0);
                    log.debug("SellerAgent {} package {} | name={}",
                            ctx.getSessionId(), isPackageSwitch ? "switched" : "selected", matched.getPackageName());
                }
            }

            // Update price tracking when a package is already selected
            if (ctx.getSelectedPackage() != null) {
                double previousOffer = ctx.getCurrentOfferedPrice() > 0.0
                        ? ctx.getCurrentOfferedPrice()
                        : ctx.getBasePrice();
                double proposedOffer = strategy.getNewPrice() > 0.0
                        ? strategy.getNewPrice()
                        : previousOffer;

                // Control upward price once a lower value was given to the guest
                double finalOffer = Math.min(previousOffer, proposedOffer);
                finalOffer = buildBounds().clamp(finalOffer);

                // Calculate total discount and remaining discount budget
                double cumulativeDiscount = (ctx.getBasePrice() - finalOffer) / ctx.getBasePrice();
                double remainingBudget = Math.max(0.0, buildBounds().maxDiscountRate() - cumulativeDiscount);

                // Store the discount values
                ctx.setCurrentOfferedPrice(finalOffer);
                ctx.setLastDiscountRate(cumulativeDiscount);
                ctx.setBoundaryHit(strategy.isBoundaryHit() || finalOffer < proposedOffer);
                ctx.setRemainingBudget(remainingBudget);
            }

            // Check if a new package was selected or switched
            boolean packageJustChanged = isNewSelection || isPackageSwitch;

            // Ensure at least 2 negotiation rounds before moving to booking
            boolean enoughRounds = ctx.getNegotiationRound() >= 2;

            // Move NEGOTIATING state to BOOKING state
            if (!packageJustChanged && enoughRounds
                    && (strategy.getStrategy() == NegotiationStrategy.CLOSE_DEAL || result.isBookingRequested())) {

                // Save current message for next state
                pendingCfp = cfp;
                ctx.setPhase(NegotiationPhase.BOOKING);
                transition = NEXT;

            } else {

                // Send PROPOSE reply only when staying in NEGOTIATING
                sendPropose(cfp, result.getReply());
            }

            done = true;
        }

        @Override public boolean done() { return done; }
        @Override public int onEnd() { return transition; }
        @Override public void reset() { super.reset(); done = false; transition = STAY; }
    }


    // PHASE 4 : Collect guest details, save Guest entity, confirm booking with ResourceMgtAgent
    private class BookingState extends SimpleBehaviour {

        private boolean done = false;
        private int transition = STAY;

        @Override
        public void action() {

            // Use cfp passed from NegotiatingState (first entry) or wait for the next user message
            ACLMessage cfp;
            if (pendingCfp != null) {
                cfp = pendingCfp;
                pendingCfp = null;
            } else {
                cfp = myAgent.receive(cfpTemplate);
            }
            if (cfp == null) { block(); return; }

            // Convert message JSON into object
            AgentMessageDto message = JsonUtil.fromJson(cfp.getContent(), AgentMessageDto.class);
            if (message == null) {
                sendFailure(cfp, "Failed to parse message");
                done = true;
                return;
            }

            // Increase turn count
            ctx.setTotalTurns(ctx.getTotalTurns() + 1);
            log.debug("SellerAgent {} BOOKING | turn={} guestDetailsCollected={}",
                    ctx.getSessionId(), ctx.getTotalTurns(), ctx.isGuestDetailsCollected());

            // Collect and validate guest contact details
            if (!ctx.isGuestDetailsCollected()) {

                // Check if message contains email or phone number
                boolean hasContactInfo = message.getGuestMessage() != null
                        && (message.getGuestMessage().contains("@")
                                || message.getGuestMessage().replaceAll("\\D", "").length() >= 7);

                // Ask user if no contact details provided yet
                if (!hasContactInfo && ctx.getGuestEmail() == null && ctx.getGuestContactNumber() == null) {
                    sendPropose(cfp, "To finalize your reservation, could you please share your email address and a contact phone number?");
                    done = true;
                    return;
                }

                // Extract guest details using LLM
                GuestDetailsResultDto details = sellerReplyModule.generateGuestDetailsReply(
                        message.getGuestMessage(),
                        message.getConversationHistory(),
                        ctx.getGuestName(),
                        ctx.getGuestEmail(),
                        ctx.getGuestContactNumber()
                );

                // Update guest name and email if found
                if (details.getGuestName()      != null) ctx.setGuestName(details.getGuestName());
                if (details.getEmail()          != null) ctx.setGuestEmail(details.getEmail());

                // Validate phone number
                if (details.getContactNumber() != null) {
                    String digits = details.getContactNumber().replaceAll("\\D", "");

                    // Accept only 10 to 12-digit numbers
                    if (digits.length() >= 10 && digits.length() <= 12) {
                        ctx.setGuestContactNumber(digits);

                    } else {

                        // Ask for number again if invalid
                        log.warn("SellerAgent {} invalid phone '{}' — {} digits, must be 10-12",
                                ctx.getSessionId(), details.getContactNumber(), digits.length());
                        sendPropose(cfp, "That contact number doesn't look right. Please provide a valid phone number (10 to 12 digits).");
                        done = true;
                        return;
                    }
                }

                // If still missing details, ask again
                if (!details.isAllCollected()) {
                    sendPropose(cfp, details.getReply());
                    done = true;
                    return;
                }

                // Save guest details in database
                Guest guest = Guest.builder()
                        .sessionId(ctx.getSessionId())
                        .guestName(ctx.getGuestName())
                        .email(ctx.getGuestEmail())
                        .contactNumber(ctx.getGuestContactNumber())
                        .build();
                guestRepository.save(guest);
                ctx.setGuestDetailsCollected(true);

                log.debug("SellerAgent {} guest saved | name={} email={}",
                        ctx.getSessionId(), guest.getGuestName(), guest.getEmail());
            }

            // Send booking REQUEST to ResourceMgtAgent
            AID resourceMgtAID = findResourceMgtAgent();
            if (resourceMgtAID == null) {
                sendFailure(cfp, "ResourceMgtAgent not available");
                done = true;
                return;
            }

            // Request booking
            BookingResponseDto bookingResult = requestBooking(resourceMgtAID);

            // Handle booking failure
            if (bookingResult == null || !"CONFIRMED".equals(bookingResult.getStatus())) {
                String reason = bookingResult != null ? bookingResult.getMessage() : "Booking request timed out";
                log.warn("SellerAgent {} booking failed | reason={}", ctx.getSessionId(), reason);
                sendPropose(cfp, "I'm sorry, we couldn't complete your booking at this time. " + reason);
                done = true;
                return;
            }

            // Move BOOKING state to CONFIRMATION state
            ctx.setBookingReference(bookingResult.getReference());
            ctx.setBookingComplete(true);
            ctx.setPhase(NegotiationPhase.CONFIRMATION);

            log.debug("SellerAgent {} booking confirmed | reference={}", ctx.getSessionId(), bookingResult.getReference());

            pendingCfp = cfp;
            transition = NEXT;
            done = true;
        }

        @Override public boolean done() { return done; }
        @Override public int onEnd() { return transition; }
        @Override public void reset() { super.reset(); done = false; transition = STAY; }
    }


    // PHASE 5 : Send booking confirmation message
    private class ConfirmationState extends SimpleBehaviour {

        private boolean done = false;

        @Override
        public void action() {

            // Wait if no message from previous state
            if (pendingCfp == null) {
                block();
                return;
            }
            ACLMessage cfp = pendingCfp;
            pendingCfp = null;

            log.debug("SellerAgent {} CONFIRMATION | bookingRef={}", ctx.getSessionId(), ctx.getBookingReference());

            // Calculate final price per night (rounded) and total price for the stay
            double finalPerNight = Math.round(resolveFinalOfferedPrice() / 100.0) * 100.0;
            int nights = ctx.getNights() != null ? ctx.getNights() : 1;
            double finalTotal    = Math.round((finalPerNight * nights) / 100.0) * 100.0;

            // Build confirmation message for the user
            String confirmationReply = String.format(
                    "Your booking is confirmed! " +
                            "The total for your stay is **LKR %.0f**, which works out to **LKR %.0f** per night. " +
                            "Your booking reference is **%s**. " +
                            "Payment details will be sent to your registered email shortly. " +
                            "Please note that if payment is not completed within 6 hours, the booking will be automatically cancelled. " +
                            "We look forward to welcoming you to Emerald Lagoon!",
                    finalTotal, finalPerNight, ctx.getBookingReference()
            );

            // Send final booking confirmation to user
            sendBookingConfirmation(cfp, confirmationReply, finalPerNight, finalTotal, nights);
            done = true;
        }

        @Override
        public boolean done() { return done; }

        @Override
        public void reset() {
            super.reset();
            done = false;
        }
    }


    // Build NegotiationBounds from the currently selected package
    private NegotiationBounds buildBounds() {
        return NegotiationBounds.builder()
                .basePrice(ctx.getBasePrice())
                .upperBound(ctx.getUpperBound())
                .lowerBound(ctx.getLowerBound())
                .build();
    }


    // Look up a package from the available package list
    private AvailabilityResponseDto findPackageByName(String packageName) {
        List<AvailabilityResponseDto> packages = ctx.getAvailablePackages();
        if (packages == null || packageName == null) return null;
        for (AvailabilityResponseDto pkg : packages) {
            if (packageName.equalsIgnoreCase(pkg.getPackageName())) return pkg;
        }
        return null;
    }


    // Looks up ResourceMgtAgent in the DF by service type
    private AID findResourceMgtAgent() {
        try {
            // Create a search template for the required service
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("inventory-management");
            template.addServices(sd);

            // Search DF for matching agents
            DFAgentDescription[] results = DFService.search(this, template);
            if (results.length > 0) return results[0].getName();

            log.error("SellerAgent {} could not find ResourceMgtAgent in DF", ctx.getSessionId());
            return null;

        } catch (FIPAException e) {

            log.error("SellerAgent {} DF search failed", ctx.getSessionId(), e);
            return null;
        }
    }


    // Sends QUERY_REF to ResourceMgtAgent and returns all packages matching guestCount
    private List<PackageResponseDto> queryPackages(AID resourceMgtAID) {
        try {

            // Create QUERY_REF message to request package list
            ACLMessage query = new ACLMessage(ACLMessage.QUERY_REF);
            query.addReceiver(resourceMgtAID);
            query.setConversationId(ctx.getSessionId() + "-pkg");
            query.setContent(String.format("{\"addOns\": null, \"guestCount\": %d}", ctx.getGuestCount()));

            // Set message format and domain
            query.setLanguage("JSON");
            query.setOntology("hotel-inventory");
            send(query);

            // Wait for response with matching conversation ID
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchConversationId(ctx.getSessionId() + "-pkg"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
            );
            ACLMessage reply = blockingReceive(mt, 5000);

            // Handle timeout
            if (reply == null) {
                log.error("SellerAgent {} package query timed out", ctx.getSessionId());
                return null;
            }

            // Convert JSON response into list of PackageResponseDto
            return JsonUtil.fromJson(reply.getContent(), new TypeReference<>() {});

        } catch (Exception e) {
            log.error("SellerAgent {} failed to query packages", ctx.getSessionId(), e);
            return null;
        }
    }


    // Sends QUERY_IF to ResourceMgtAgent and returns availability for a single package on selected dates
    private AvailabilityResponseDto checkPackageAvailability(AID resourceMgtAID, Long packageId) {
        try {

            // Build availability request with package and booking details
            AvailabilityRequestDto request = AvailabilityRequestDto.builder()
                    .packageId(packageId)
                    .checkInDate(ctx.getCheckInDate())
                    .checkOutDate(ctx.getCheckOutDate())
                    .guestCount(ctx.getGuestCount())
                    .build();

            // Create unique conversation ID for this request
            String convId = ctx.getSessionId() + "-avail-" + packageId;

            // Create QUERY_IF message to check availability
            ACLMessage query = new ACLMessage(ACLMessage.QUERY_IF);
            query.addReceiver(resourceMgtAID);
            query.setConversationId(convId);
            query.setContent(JsonUtil.toJson(request));
            query.setLanguage("JSON");
            query.setOntology("hotel-inventory");
            send(query);

            // Wait for response with matching conversation ID
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchConversationId(convId),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
            );
            ACLMessage reply = blockingReceive(mt, 5000);

            // Handle timeout
            if (reply == null) {
                log.error("SellerAgent {} availability check timed out for package {}", ctx.getSessionId(), packageId);
                return null;
            }

            // Convert JSON response into AvailabilityResponseDto
            return JsonUtil.fromJson(reply.getContent(), AvailabilityResponseDto.class);

        } catch (Exception e) {
            log.error("SellerAgent {} availability check failed for package {}", ctx.getSessionId(), packageId, e);
            return null;
        }
    }


    // Sends a REQUEST to ResourceMgtAgent to confirm the booking and returns the response
    private BookingResponseDto requestBooking(AID resourceMgtAID) {
        try {

            // Build booking request with selected package and guest details
            BookingRequestDto request = BookingRequestDto.builder()
                    .packageId(ctx.getSelectedPackageId())
                    .sessionId(ctx.getSessionId())
                    .checkInDate(ctx.getCheckInDate())
                    .checkOutDate(ctx.getCheckOutDate())
                    .offeredPrice(BigDecimal.valueOf(resolveFinalOfferedPrice()))
                    .guestCount(ctx.getGuestCount())
                    .build();

            // Create unique conversation ID for booking
            String convId = ctx.getSessionId() + "-booking";

            // Create REQUEST message to confirm booking
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(resourceMgtAID);
            msg.setConversationId(convId);
            msg.setContent(JsonUtil.toJson(request));
            msg.setLanguage("JSON");
            msg.setOntology("hotel-inventory");
            send(msg);

            // Wait for booking confirmation response
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchConversationId(convId),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
            );
            ACLMessage reply = blockingReceive(mt, 10000);

            // Handle timeout
            if (reply == null) {
                log.error("SellerAgent {} booking request timed out", ctx.getSessionId());
                return null;
            }

            // Convert JSON response into BookingResponseDto
            return JsonUtil.fromJson(reply.getContent(), BookingResponseDto.class);

        } catch (Exception e) {
            log.error("SellerAgent {} failed to request booking", ctx.getSessionId(), e);
            return null;
        }
    }


    // Calculate the negotiated offer price
    private double resolveFinalOfferedPrice() {
        return ctx.getCurrentOfferedPrice() > 0.0 ? ctx.getCurrentOfferedPrice() : ctx.getBasePrice();
    }


    // Sends a PROPOSE reply to the UserAgent with the LLM-generated reply text
    private void sendPropose(ACLMessage cfp, String replyText) {
        sendPropose(cfp, replyText, null);
    }


    // Sends a PROPOSE reply to the UserAgent with optional metadata JSON
    private void sendPropose(ACLMessage cfp, String replyText, String metadata) {

        // Build response object with reply message and negotiation details
        SellerReplyContentDto replyContent = SellerReplyContentDto.builder()
                .agentReply(replyText)
                .offeredPrice(ctx.getCurrentOfferedPrice() > 0.0 ? ctx.getCurrentOfferedPrice() : null)
                .recommendedPackageId(ctx.getSelectedPackageId())
                .metadata(metadata)
                .bookingComplete(false)
                .build();

        // Create reply message based on received CFP
        ACLMessage propose = cfp.createReply();
        propose.setPerformative(ACLMessage.PROPOSE);
        propose.setContent(JsonUtil.toJson(replyContent));
        propose.setLanguage("JSON");
        propose.setOntology("hotel-negotiation");
        send(propose);

        log.debug("SellerAgent {} sent PROPOSE to {} | turn={}", ctx.getSessionId(), pairedUserAgentName, ctx.getTotalTurns());
    }


    // Builds metadata JSON containing available package cards for the UI
    private String buildPackageMetadata(List<AvailabilityResponseDto> availablePackages) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("availablePackages", availablePackages);
        return JsonUtil.toJson(metadata);
    }


    // Sends the final booking confirmation PROPOSE with booking card metadata
    private void sendBookingConfirmation(ACLMessage cfp, String replyText,
                                         double pricePerNight, double totalPrice, int nights) {

        // Build booking details card to send to UI
        Map<String, Object> card = new HashMap<>();
        card.put("reference",    ctx.getBookingReference());
        card.put("packageName",  ctx.getSelectedPackage() != null ? ctx.getSelectedPackage().getPackageName() : null);
        card.put("checkInDate",  ctx.getCheckInDate());
        card.put("checkOutDate", ctx.getCheckOutDate());
        card.put("totalNights",  nights);
        card.put("pricePerNight", pricePerNight);
        card.put("totalPrice",   totalPrice);
        card.put("addOns", ctx.getSelectedPackage() != null ? ctx.getSelectedPackage().getAddOns() : List.of());
        card.put("imageUrl", ctx.getSelectedPackage() != null ? ctx.getSelectedPackage().getImageUrl() : null);
        card.put("guestName",    ctx.getGuestName());

        // Wrap card inside metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bookingConfirmation", card);

        // Build final response object
        SellerReplyContentDto replyContent = SellerReplyContentDto.builder()
                .agentReply(replyText)
                .offeredPrice(pricePerNight)
                .recommendedPackageId(ctx.getSelectedPackageId())
                .bookingReference(ctx.getBookingReference())
                .metadata(JsonUtil.toJson(metadata))
                .bookingComplete(true)
                .build();

        // Create reply message and send
        ACLMessage propose = cfp.createReply();
        propose.setPerformative(ACLMessage.PROPOSE);
        propose.setContent(JsonUtil.toJson(replyContent));
        propose.setLanguage("JSON");
        propose.setOntology("hotel-negotiation");
        send(propose);

        log.debug("SellerAgent {} sent booking CONFIRMATION PROPOSE | ref={}", ctx.getSessionId(), ctx.getBookingReference());
    }


    // Sends a FAILURE reply to the UserAgent with a reason string
    private void sendFailure(ACLMessage cfp, String reason) {

        // Create failure message and send
        ACLMessage failure = cfp.createReply();
        failure.setPerformative(ACLMessage.FAILURE);
        failure.setContent(reason);
        send(failure);

        log.error("SellerAgent {} sent FAILURE | reason: {}", ctx.getSessionId(), reason);
    }


    // Persists the final seller message and aborts the session cleanly.
    private void abortNegotiation(String replyText) {
        negotiationService.saveAgentReply(
                UserAgentReplyDto.builder()
                        .sessionId(ctx.getSessionId())
                        .agentReply(replyText)
                        .offeredPrice(ctx.getCurrentOfferedPrice() > 0.0 ? ctx.getCurrentOfferedPrice() : null)
                        .recommendedPackageId(ctx.getSelectedPackageId())
                        .bookingComplete(false)
                        .build()
        );
        negotiationService.abortSession(ctx.getSessionId());
    }
}
