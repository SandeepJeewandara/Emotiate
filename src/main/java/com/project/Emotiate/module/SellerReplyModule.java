package com.project.Emotiate.module;


import com.project.Emotiate.dto.emotion.EmotionResultDto;
import com.project.Emotiate.dto.resourceAgt.AvailabilityResponseDto;
import com.project.Emotiate.dto.sellerAgt.GetInfoResultDto;
import com.project.Emotiate.dto.sellerAgt.GuestDetailsResultDto;
import com.project.Emotiate.dto.sellerAgt.NegotiationReplyResultDto;
import com.project.Emotiate.dto.strategy.StrategyResultDto;
import com.project.Emotiate.service.LLMService;
import com.project.Emotiate.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class SellerReplyModule {

    @Value("classpath:prompts/get-info.txt")
    private Resource getInfoPromptResource;

    @Value("classpath:prompts/recommend-package.txt")
    private Resource recommendPackagePromptResource;

    @Value("classpath:prompts/negotiation-reply.txt")
    private Resource negotiationReplyPromptResource;

    @Value("classpath:prompts/booking-guest.txt")
    private Resource bookingGuestPromptResource;

    private final LLMService llmService;

    private String getInfoPrompt;
    private String recommendPackagePrompt;
    private String negotiationPrompt;
    private String bookingGuestPrompt;

    @PostConstruct
    public void init() throws IOException {
        this.getInfoPrompt          = getInfoPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.recommendPackagePrompt = recommendPackagePromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.negotiationPrompt      = negotiationReplyPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.bookingGuestPrompt     = bookingGuestPromptResource.getContentAsString(StandardCharsets.UTF_8);

        log.info("SellerReplyModule prompts loaded successfully");
    }


    // Function to collects check-in, check-out, and guest count from guest message
    public GetInfoResultDto generateInfoReply(String guestMessage,
                                              List<String> conversationHistory,
                                              String guestName,
                                              String collectedCheckIn,
                                              String collectedCheckOut,
                                              Integer collectedGuestCount) {
        try {
            String context = String.format("""
                    {
                      "guestName": "%s",
                      "guestMessage": "%s",
                      "conversationHistory": "%s",
                      "collectedCheckIn": %s,
                      "collectedCheckOut": %s,
                      "collectedGuestCount": %s
                    }
                    """,
                    guestName,
                    guestMessage,
                    formatHistory(conversationHistory).replace("\"", "'"),
                    jsonString(collectedCheckIn),
                    jsonString(collectedCheckOut),
                    collectedGuestCount != null ? collectedGuestCount : "null"
            );

            log.trace("SellerReplyModule generating info reply | guest={} checkIn={} checkOut={} guestCount={}",
                    guestName, collectedCheckIn, collectedCheckOut, collectedGuestCount);

            // Call LLM
            String raw = llmService.complete(getInfoPrompt, context).trim();

            // Convert Results to DTO
            GetInfoResultDto result = JsonUtil.fromJson(raw, GetInfoResultDto.class);

            log.trace("SellerReplyModule info reply generated | allCollected={} checkIn={} checkOut={} guestCount={}",
                    result.isAllCollected(), result.getCheckInDate(), result.getCheckOutDate(), result.getGuestCount());

            return result;

        } catch (Exception e) {
            log.error("SellerReplyModule failed to generate info reply for guest: {}", guestName, e);
            return fallbackInfoReply();
        }
    }


    // Safe fallback when the get-info LLM call or parse fails
    private GetInfoResultDto fallbackInfoReply() {
        return GetInfoResultDto.builder()
                .reply("Thank you! Could you please share your check-in date, check-out date, and number of guests?")
                .checkInDate(null)
                .checkOutDate(null)
                .guestCount(null)
                .allCollected(false)
                .build();
    }


    // Function to presents available packages to the guest based on their preferences
    public String generatePackageReply(String guestName,
                                       String checkIn,
                                       String checkOut,
                                       Integer guestCount,
                                       List<AvailabilityResponseDto> available) {
        try {
            String context = buildPackageContext(guestName, checkIn, checkOut, guestCount, available);

            log.trace("SellerReplyModule generating package reply | guest={} available={}",
                    guestName, available.size());

            // Call LLM
            String reply = llmService.complete(recommendPackagePrompt, context).trim();

            log.trace("SellerReplyModule package reply generated | availableCount={}", available.size());

            return reply;

        } catch (Exception e) {
            log.error("SellerReplyModule failed to generate package reply", e);
            return fallbackReply();
        }
    }


    // Builds the context JSON for the package recommendation fields
    private String buildPackageContext(String guestName,
                                       String checkIn,
                                       String checkOut,
                                       Integer guestCount,
                                       List<AvailabilityResponseDto> available) {
        String packagesJson = available.stream()
                .map(pkg -> String.format(
                        "{\"name\":\"%s\",\"totalNights\":%d,\"addOns\":%s}",
                        pkg.getPackageName(),
                        pkg.getTotalNights(),
                        JsonUtil.toJson(formatAddOns(pkg))
                ))
                .collect(Collectors.joining(",", "[", "]"));

        return String.format("""
                {
                  "guestName": "%s",
                  "checkInDate": "%s",
                  "checkOutDate": "%s",
                  "guestCount": %d,
                  "availablePackages": %s
                }
                """,
                guestName, checkIn, checkOut, guestCount, packagesJson
        );
    }


    //  Function to generate negotiation reply with detected emotion and strategy
    public NegotiationReplyResultDto generateNegotiatingReply(String guestMessage,
                                                              List<String> conversationHistory,
                                                              EmotionResultDto emotion,
                                                              StrategyResultDto strategy,
                                                              List<AvailabilityResponseDto> availablePackages,
                                                              AvailabilityResponseDto selectedPackage,
                                                              double currentOfferedPrice,
                                                              boolean abortEligible) {
        try {
            // Convert available packages into a JSON array
            String pkgArray = availablePackages.stream()
                    .map(pkg -> {

                        // Use upperBoundPrice as the starting price
                        double perNight = roundToNearest100(pkg.getUpperBoundPrice().doubleValue());

                        // Calculate total price
                        double totalPrice = roundToNearest100(perNight * pkg.getTotalNights());
                        return String.format(
                                "{\"name\":\"%s\",\"totalPrice\":%.0f,\"pricePerNight\":%.0f,\"nights\":%d,\"addOns\":%s}",
                                pkg.getPackageName(),
                                totalPrice,
                                perNight,
                                pkg.getTotalNights(),
                                JsonUtil.toJson(formatAddOns(pkg))
                        );
                    })
                    .collect(Collectors.joining(",", "[", "]"));

            // Get selected package name
            String selectedName = selectedPackage != null ? selectedPackage.getPackageName() : null;

            // Determine price per night as negotiation
            double resolvedPerNight = currentOfferedPrice > 0.0
                    ? (strategy.getNewPrice() > 0.0 ? Math.min(strategy.getNewPrice(), currentOfferedPrice) : currentOfferedPrice)
                    : strategy.getNewPrice();

            // Round price per night to 100
            double pricePerNight = roundToNearest100(resolvedPerNight);

            // Calculate total price for the selected package
            double totalPrice    = selectedPackage != null ? roundToNearest100(pricePerNight * selectedPackage.getTotalNights()) : 0;

            // Format add-ons
            String addOns        = selectedPackage != null && selectedPackage.getAddOns() != null
                    ? selectedPackage.getAddOns().stream().map(this::formatAddOn).collect(Collectors.joining(", "))
                    : "none";

            // Get total nights
            int totalNights      = selectedPackage != null ? selectedPackage.getTotalNights() : 0;

            // Check if guest is asking for price justification
            String lowerMessage = guestMessage.toLowerCase();
            boolean justificationAsked = lowerMessage.contains("why")
                    || lowerMessage.contains("expensive")
                    || lowerMessage.contains("justify");

            String context = String.format("""
                    {
                      "guestMessage": "%s",
                      "conversationHistory": "%s",
                      "detectedEmotion": "%s",
                      "tone": "%s",
                      "strategy": "%s",
                      "availablePackages": %s,
                      "selectedPackageName": %s,
                      "offeredPrice": %.0f,
                      "pricePerNight": %.0f,
                      "totalNights": %d,
                      "packageAddOns": "%s",
                      "priceJustificationRequested": %b,
                      "abortEligible": %b
                    }
                    """,
                    guestMessage,
                    formatHistory(conversationHistory).replace("\"", "'"),
                    emotion.getEmotion(),
                    strategy.getTone(),
                    strategy.getStrategy(),
                    pkgArray,
                    jsonString(selectedName),
                    totalPrice,
                    pricePerNight,
                    totalNights,
                    addOns,
                    justificationAsked,
                    abortEligible
            );

            log.trace("SellerReplyModule generating negotiating reply | strategy={} selectedPackage={}",
                    strategy.getStrategy(), selectedName);

            // Call LLM
            String raw = stripJsonFences(llmService.complete(negotiationPrompt, context));

            // Convert Results to DTO
            NegotiationReplyResultDto result = JsonUtil.fromJson(raw, NegotiationReplyResultDto.class);

            log.trace("SellerReplyModule negotiating reply generated | strategy={} selectedPackage={}",
                    strategy.getStrategy(), result.getSelectedPackage());

            return result;

        } catch (Exception e) {
            log.error("SellerReplyModule failed to generate negotiating reply", e);
            return NegotiationReplyResultDto.builder()
                    .selectedPackage(null)
                    .reply(fallbackReply())
                    .abortRequested(false)
                    .build();
        }
    }


    //  Function to Collects and validates guest contact details (email, contact number) for booking
    public GuestDetailsResultDto generateGuestDetailsReply(String guestMessage,
                                                           List<String> conversationHistory,
                                                           String guestName,
                                                           String collectedEmail,
                                                           String collectedContactNumber) {
        try {
            String context = String.format("""
                    {
                      "guestName": "%s",
                      "guestMessage": "%s",
                      "conversationHistory": "%s",
                      "collectedEmail": %s,
                      "collectedContactNumber": %s
                    }
                    """,
                    guestName,
                    guestMessage,
                    formatHistory(conversationHistory).replace("\"", "'"),
                    jsonString(collectedEmail),
                    jsonString(collectedContactNumber)
            );

            log.trace("SellerReplyModule generating guest details reply | guest={} emailCollected={} contactCollected={}",
                    guestName, collectedEmail != null, collectedContactNumber != null);

            // Call LLM
            String raw = stripJsonFences(llmService.complete(bookingGuestPrompt, context));

            // Convert Results to DTO
            GuestDetailsResultDto result = JsonUtil.fromJson(raw, GuestDetailsResultDto.class);

            log.trace("SellerReplyModule guest details reply | allCollected={} email={}",
                    result.isAllCollected(), result.getEmail());

            return result;

        } catch (Exception e) {

            log.error("SellerReplyModule failed to generate guest details reply", e);
            return GuestDetailsResultDto.builder()
                    .reply("To complete your booking, could you please share your email address and contact number?")
                    .allCollected(false)
                    .build();
        }
    }


    // Returns formatted add-ons list for a package
    private List<String> formatAddOns(AvailabilityResponseDto pkg) {
        return pkg.getAddOns() != null
                ? pkg.getAddOns().stream().map(this::formatAddOn).collect(Collectors.toList())
                : Collections.emptyList();
    }


    // Converts Uppercase enum names to "Title Case" for LLM readability
    private String formatAddOn(String addOn) {
        if (addOn == null) return "";
        String[] words = addOn.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }


    // Formats conversation history for the LLM prompt
    private String formatHistory(List<String> history) {
        return (history == null || history.isEmpty()) ? "No prior turns." : String.join("\n", history);
    }


    // Wraps a nullable string as a JSON string
    private String jsonString(String value) {
        return value != null ? "\"" + value + "\"" : "null";
    }


    // Strips Markdown code fences from LLM output
    private String stripJsonFences(String raw) {
        return raw.trim().replaceAll("(?s)```json\\s*", "").replaceAll("```", "").trim();
    }


    // Rounds a value to the nearest 100
    private double roundToNearest100(double value) {
        return Math.round(value / 100.0) * 100.0;
    }


    // Safe fallback reply when the LLM call fails
    private String fallbackReply() {
        return "Thank you for your message. Let me look into the best option for you and get back to you shortly.";
    }
}
