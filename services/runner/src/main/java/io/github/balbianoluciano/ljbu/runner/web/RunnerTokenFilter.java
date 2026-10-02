package io.github.balbianoluciano.ljbu.runner.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/** Only the api, which knows RUNNER_TOKEN, may ask for executions. */
public class RunnerTokenFilter extends OncePerRequestFilter {

  /** Far above the 64 KB of code a request may carry, JSON escaping included. */
  private static final long MAX_BODY_BYTES = 512 * 1024;

  private static final String SCHEME = "Bearer ";

  private final byte[] token;

  public RunnerTokenFilter(String token) {
    this.token = token.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    boolean authorized =
        header != null
            && header.startsWith(SCHEME)
            && MessageDigest.isEqual(
                header.substring(SCHEME.length()).getBytes(StandardCharsets.UTF_8), token);
    if (!authorized) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    if (request.getContentLengthLong() > MAX_BODY_BYTES) {
      response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      return;
    }
    chain.doFilter(request, response);
  }
}
