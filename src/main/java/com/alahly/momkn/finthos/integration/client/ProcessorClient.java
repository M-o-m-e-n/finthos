package com.alahly.momkn.finthos.integration.client;

import com.alahly.momkn.finthos.common.error.ProcessorDeclinedException;
import com.alahly.momkn.finthos.common.error.ProcessorTimeoutException;
import com.alahly.momkn.finthos.common.error.ProcessorUnavailableException;
import com.alahly.momkn.finthos.integration.config.ProcessorProperties;
import com.alahly.momkn.finthos.integration.domain.ProcessorAuthorization;
import com.alahly.momkn.finthos.integration.repository.ProcessorAuthorizationRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class ProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(ProcessorClient.class);

    private final RestClient rest;
    private final ProcessorProperties props;
    private final ProcessorAuthorizationRepository authRepo;
    private final RetryTemplate retryTemplate;

    public ProcessorClient(RestClient.Builder builder,
                           ProcessorProperties props,
                           ProcessorAuthorizationRepository authRepo) {
        this.props = props;
        this.authRepo = authRepo;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.timeoutMs());
        factory.setReadTimeout(props.timeoutMs());

        this.rest = builder
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();

        // Programmatic rather than @Retryable: authorizeWithCircuitBreaker calls this through
        // `this`, which would bypass the retry proxy and silently disable retries.
        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(Math.max(1, props.retryCount()))
                .fixedBackoff(Math.max(1, props.retryDelayMs()))
                .retryOn(ProcessorTimeoutException.class)
                .build();
    }

    public ProcessorResponse authorize(UUID transactionId, ProcessorRequest req) {
        return authorize(transactionId, req, 1);
    }

    private ProcessorResponse authorize(UUID transactionId, ProcessorRequest req, int attemptNumber) {
        ProcessorAuthorization auth = ProcessorAuthorization.create(
                transactionId, req.reference(), req.amount(), req.currency(),
                attemptNumber, props.timeoutMs());
        authRepo.save(auth);

        try {
            ProcessorResponse response = rest.post()
                    .uri("/authorize")
                    .body(req)
                    .retrieve()
                    .body(ProcessorResponse.class);

            if (response == null) {
                auth.markTimeout();
                markPersisted(auth);
                throw new ProcessorTimeoutException(req.reference());
            }

            if (response.isApproved()) {
                auth.markApproved(response.authCode());
                markPersisted(auth);
                return response;
            }

            if (response.isDeclined()) {
                auth.markDeclined();
                markPersisted(auth);
                throw new ProcessorDeclinedException(req.reference());
            }

            if (response.isTimeout()) {
                auth.markTimeout();
                markPersisted(auth);
                throw new ProcessorTimeoutException(req.reference());
            }

            auth.markDeclined();
            markPersisted(auth);
            throw new ProcessorDeclinedException(req.reference());

        } catch (ProcessorDeclinedException | ProcessorTimeoutException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("Processor call failed on attempt {} for {}: {}", attemptNumber, req.reference(), e.getMessage());
            auth.markTimeout();
            markPersisted(auth);
            throw new ProcessorTimeoutException(req.reference(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling processor for {}: {}", req.reference(), e.getMessage());
            auth.markTimeout();
            markPersisted(auth);
            throw new ProcessorTimeoutException(req.reference(), e);
        }
    }

    public ProcessorResponse authorizeWithRetry(UUID transactionId, ProcessorRequest req) {
        return retryTemplate.execute(ctx -> authorize(transactionId, req, ctx.getRetryCount() + 1));
    }

    @CircuitBreaker(name = "processor", fallbackMethod = "circuitBreakerFallback")
    public ProcessorResponse authorizeWithCircuitBreaker(UUID transactionId, ProcessorRequest req) {
        return authorizeWithRetry(transactionId, req);
    }

    public ProcessorResponse circuitBreakerFallback(UUID transactionId, ProcessorRequest req, Throwable t) {
        if (t instanceof ProcessorTimeoutException || t instanceof ProcessorDeclinedException) {
            throw (RuntimeException) t;
        }
        if (t instanceof CallNotPermittedException) {
            log.warn("Circuit breaker open for processor, failing fast for {}", req.reference());
        }
        throw new ProcessorUnavailableException(req.reference(), t);
    }

    private void markPersisted(ProcessorAuthorization auth) {
        auth.markPersisted();
        authRepo.save(auth);
    }
}
