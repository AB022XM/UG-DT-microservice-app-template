package ug.co.absa.microservice.app.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ug.co.absa.microservice.app.IntegrationTest;
import ug.co.absa.microservice.app.config.Constants;
import ug.co.absa.microservice.app.config.TestSecurityConfiguration;
import ug.co.absa.microservice.app.domain.User;
import ug.co.absa.microservice.app.repository.EntityManager;
import ug.co.absa.microservice.app.repository.UserRepository;
import ug.co.absa.microservice.app.repository.search.UserSearchRepository;
import ug.co.absa.microservice.app.security.AuthoritiesConstants;
import ug.co.absa.microservice.app.service.dto.UserDTO;

/**
 * Integration tests for the {@link UserResource} REST controller.
 */
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
@IntegrationTest
class PublicUserResourceIT {

    private static final String DEFAULT_LOGIN = "johndoe";

    @Autowired
    private UserRepository userRepository;

    /**
     * This repository is mocked in the ug.co.absa.microservice.app.repository.search test package.
     *
     * @see ug.co.absa.microservice.app.repository.search.UserSearchRepositoryMockConfiguration
     */
    @Autowired
    private UserSearchRepository mockUserSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private WebTestClient webTestClient;

    private User user;

    @BeforeEach
    public void setup() {
        cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).clear();
        cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE).clear();
    }

    @BeforeEach
    public void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    public void initTest() {
        user = UserResourceIT.initTestUser(userRepository, em);
    }

    @Test
    void getAllPublicUsers() {
        // Initialize the database
        userRepository.create(user).block();

        // Get all the users
        UserDTO foundUser = webTestClient
            .get()
            .uri("/api/users?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .returnResult(UserDTO.class)
            .getResponseBody()
            .blockFirst();

        assertThat(foundUser.getLogin()).isEqualTo(DEFAULT_LOGIN);
    }

    @Test
    void getAllAuthorities() {
        webTestClient
            .get()
            .uri("/api/authorities")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .expectBody()
            .jsonPath("$")
            .isArray()
            .jsonPath("$[?(@=='" + AuthoritiesConstants.ADMIN + "')]")
            .hasJsonPath()
            .jsonPath("$[?(@=='" + AuthoritiesConstants.USER + "')]")
            .hasJsonPath();
    }
}
