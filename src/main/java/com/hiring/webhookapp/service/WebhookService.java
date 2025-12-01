package com.hiring.webhookapp.service;

import com.hiring.webhookapp.model.GenerateWebhookRequest;
import com.hiring.webhookapp.model.GenerateWebhookResponse;
import com.hiring.webhookapp.model.SubmitSolutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    private final RestTemplate restTemplate;
    private final SqlQueryService sqlQueryService;

    @Value("${app.name:John Doe}")
    private String name;

    @Value("${app.regNo:REG12347}")
    private String regNo;

    @Value("${app.email:john@example.com}")
    private String email;

    private static final String GENERATE_WEBHOOK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
    private static final String SUBMIT_WEBHOOK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";

    public WebhookService(RestTemplate restTemplate, SqlQueryService sqlQueryService) {
        this.restTemplate = restTemplate;
        this.sqlQueryService = sqlQueryService;
    }

    public void executeWorkflow() {
        try {
            logger.info("Starting webhook workflow...");

            // Step 1: Generate webhook
            GenerateWebhookResponse webhookResponse = generateWebhook();
            logger.info("Webhook generated successfully");
            logger.info("Webhook URL: {}", webhookResponse.getWebhook());

            // Step 2: Solve SQL problem based on regNo
            String sqlQuery = sqlQueryService.getSqlQuery(regNo);
            logger.info("SQL Query prepared: {}", sqlQuery);

            // Step 3: Submit solution
            submitSolution(webhookResponse.getAccessToken(), sqlQuery);
            logger.info("Solution submitted successfully!");

        } catch (Exception e) {
            logger.error("Error in webhook workflow", e);
        }
    }

    private GenerateWebhookResponse generateWebhook() {
        GenerateWebhookRequest request = new GenerateWebhookRequest(name, regNo, email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GenerateWebhookRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<GenerateWebhookResponse> response = restTemplate.exchange(
                GENERATE_WEBHOOK_URL,
                HttpMethod.POST,
                entity,
                GenerateWebhookResponse.class
        );

        return response.getBody();
    }

    private void submitSolution(String accessToken, String sqlQuery) {
        SubmitSolutionRequest request = new SubmitSolutionRequest(sqlQuery);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);

        HttpEntity<SubmitSolutionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                SUBMIT_WEBHOOK_URL,
                HttpMethod.POST,
                entity,
                String.class
        );

        logger.info("Submit response: {}", response.getBody());
    }
}