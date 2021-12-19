/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.session;

import com.github.steveice10.mc.auth.exception.request.AuthPendingException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.geysermc.geyser.GeyserImpl;

import java.util.concurrent.*;

/**
 * Pending Microsoft authentication task cache.
 * It permits user to exit the server while they authorize Geyser to access their Microsoft account.
 */
public class PendingMicrosoftAuthentication {
    private final LoadingCache<String, AuthenticationTask> authentications;

    public PendingMicrosoftAuthentication(int timeoutSeconds) {
        this.authentications = CacheBuilder.newBuilder()
                .build(new CacheLoader<>() {
                    @Override
                    public AuthenticationTask load(@NonNull String userKey) {
                        return new AuthenticationTask(userKey, timeoutSeconds * 1000L);
                    }
                });
    }

    public AuthenticationTask getTask(@NonNull String userKey) {
        return authentications.getIfPresent(userKey);
    }

    @SneakyThrows(ExecutionException.class)
    public AuthenticationTask getOrCreateTask(@NonNull String userKey) {
        return authentications.get(userKey);
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public class AuthenticationTask {
        private static final Executor DELAYED_BY_ONE_SECOND = CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS);

        MsaAuthenticationService msaAuthenticationService = new MsaAuthenticationService(GeyserImpl.OAUTH_CLIENT_ID);
        String userKey;
        long timeoutMs;

        @NonFinal
        long remainingTimeMs;

        @NonFinal
        @Setter
        boolean online;

        @Getter
        CompletableFuture<MsaAuthenticationService.MsCodeResponse> code;
        @Getter
        CompletableFuture<MsaAuthenticationService> authentication;

        private AuthenticationTask(String userKey, long timeoutMs) {
            this.userKey = userKey;
            this.timeoutMs = timeoutMs;
            this.remainingTimeMs = timeoutMs;

            // Request the code
            this.code = CompletableFuture.supplyAsync(this::tryGetCode);
            this.authentication = new CompletableFuture<>();
            // Once the code is received, continuously try to request the access token, profile, etc
            this.code.thenRun(() -> performLoginAttempt(System.currentTimeMillis()));
            this.authentication.whenComplete((r, ex) -> {
                // avoid memory leak, in case player doesn't connect again
                CompletableFuture.delayedExecutor(timeoutMs, TimeUnit.MILLISECONDS).execute(this::cleanup);
            });
        }

        public void resetTimer() {
            this.remainingTimeMs = this.timeoutMs;
        }

        public void cleanup() {
            if (this == authentications.getIfPresent(userKey)) {
                authentications.invalidate(userKey);
            }
        }

        private MsaAuthenticationService.MsCodeResponse tryGetCode() throws CompletionException {
            try {
                return msaAuthenticationService.getAuthCode();
            } catch (RequestException e) {
                throw new CompletionException(e);
            }
        }

        private void performLoginAttempt(long lastAttempt) {
            CompletableFuture.runAsync(() -> {
                try {
                    msaAuthenticationService.login();
                } catch (AuthPendingException e) {
                    long currentAttempt = System.currentTimeMillis();
                    if (!online) {
                        // decrement timer only when player's offline
                        remainingTimeMs -= currentAttempt - lastAttempt;
                        if (remainingTimeMs <= 0L) {
                            // time's up
                            authentication.completeExceptionally(new TaskTimeoutException());
                            return;
                        }
                    }
                    // try again in 1 second
                    performLoginAttempt(currentAttempt);
                    return;
                } catch (Exception e) {
                    authentication.completeExceptionally(e);
                    return;
                }
                // login successful
                authentication.complete(msaAuthenticationService);
            }, DELAYED_BY_ONE_SECOND);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{userKey='" + userKey + "'}";
        }
    }

    /**
     * @see PendingMicrosoftAuthentication
     */
    public static class TaskTimeoutException extends Exception {
        TaskTimeoutException() {
            super("It took too long to authorize Geyser to access your Microsoft account. " +
                    "Please request new code and try again.");
        }
    }
}
