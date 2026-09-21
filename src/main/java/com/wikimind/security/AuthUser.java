package com.wikimind.security;

/** The authenticated caller, taken from a verified JWT. */
public record AuthUser(String username, String orgId) {
}
