package com.project.Emotiate.config;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class JadeConfig {

    @Value("${jade.platform-name:EmotiatePlatform}")
    private String platformName;

    @Value("${jade.gui:false}")
    private boolean showGui;

    @Bean(destroyMethod = "kill")
    public AgentContainer mainContainer() {

        // Set up the runtime environment
        Runtime rt = Runtime.instance();
        rt.setCloseVM(false);

        // Create a profile for the main container
        ProfileImpl profile = new ProfileImpl();

        // Set the platform name and GUI parameter
        profile.setParameter(Profile.PLATFORM_ID, platformName);
        profile.setParameter(Profile.GUI, String.valueOf(showGui));

        // Enable JADE REST Management Agent for monitoring
        profile.setParameter(Profile.SERVICES,
                "jade.core.replication.MainReplicationService;");

        // Start the main container with the specified profile
        AgentContainer container = rt.createMainContainer(profile);
        log.info("JADE platform '{}' started", platformName);
        return container;
    }
}