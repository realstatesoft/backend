package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.dto.agent.CreateAgentReviewRequest;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentReview;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.AgentReviewRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentReviewControllerTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;
    
    @Autowired private UserRepository userRepository;
    @Autowired private AgentProfileRepository agentProfileRepository;
    @Autowired private AgentReviewRepository reviewRepository;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;

    private User reviewer;
    private User agentUser;
    private AgentProfile agentProfile;
    private String reviewerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        reviewer = userRepository.save(User.builder().name("Reviewer").email("rev@test.com").passwordHash("hash").role(UserRole.USER).build());
        agentUser = userRepository.save(User.builder().name("Agent").email("agent@test.com").passwordHash("hash").role(UserRole.AGENT).build());
        agentProfile = agentProfileRepository.save(AgentProfile.builder().user(agentUser).build());
        reviewerToken = "Bearer " + jwtService.generateToken(reviewer);
    }

    @Test
    void createReview_withoutJwt_returns401() throws Exception {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(5, "This is a great agent, very professional.", null);
        mockMvc.perform(post("/agents/{id}/reviews", agentProfile.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReview_validJwt_returns201() throws Exception {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, "Good service overall, would recommend.", null);
        mockMvc.perform(post("/agents/{id}/reviews", agentProfile.getId())
                .header("Authorization", reviewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(4));
    }

    @Test
    void createReview_secondTimeSameUser_returns409() throws Exception {
        AgentReview review = new AgentReview();
        review.setAgent(agentProfile);
        review.setUser(reviewer);
        review.setRating(5);
        review.setComment("Initial excellent review for this agent.");
        reviewRepository.save(review);
        
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, "Another good review attempt.", null);
        mockMvc.perform(post("/agents/{id}/reviews", agentProfile.getId())
                .header("Authorization", reviewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void getReviews_withoutJwt_returns200() throws Exception {
        mockMvc.perform(get("/agents/{id}/reviews", agentProfile.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void updateReview_withAnotherUserJwt_returns403() throws Exception {
        AgentReview review = new AgentReview();
        review.setAgent(agentProfile);
        review.setUser(reviewer);
        review.setRating(5);
        review.setComment("Original review content that is long enough.");
        reviewRepository.save(review);
        
        User otherUser = userRepository.save(User.builder().name("Other").email("other@test.com").passwordHash("hash").role(UserRole.USER).build());
        String otherToken = "Bearer " + jwtService.generateToken(otherUser);
        
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(3, "Changed review content by someone else.", null);
        mockMvc.perform(patch("/agents/{id}/reviews/{rid}", agentProfile.getId(), review.getId())
                .header("Authorization", otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReview_withAdminJwt_returns204() throws Exception {
        AgentReview review = new AgentReview();
        review.setAgent(agentProfile);
        review.setUser(reviewer);
        review.setRating(5);
        review.setComment("Review to be deleted by an administrator.");
        reviewRepository.save(review);
        
        User adminUser = userRepository.save(User.builder().name("Admin").email("admin@test.com").passwordHash("hash").role(UserRole.ADMIN).build());
        String adminToken = "Bearer " + jwtService.generateToken(adminUser);
        
        mockMvc.perform(delete("/agents/{id}/reviews/{rid}", agentProfile.getId(), review.getId())
                .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        assertThat(reviewRepository.findById(review.getId())).isEmpty();
    }

    @Test
    void createReview_updatesAgentAvgRating() throws Exception {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(5, "Absolutely wonderful experience with this agent.", null);
        mockMvc.perform(post("/agents/{id}/reviews", agentProfile.getId())
                .header("Authorization", reviewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
                
        mockMvc.perform(get("/agents/{id}/reviews/summary", agentProfile.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avgRating").value(5.0));
    }
}
