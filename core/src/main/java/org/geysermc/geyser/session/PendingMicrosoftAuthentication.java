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
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;

import java.io.Serial;
import java.util.concurrent.*;

/**
 * Pending Microsoft authentication task cache.
 * It permits user to exit the server while they authorize Geyser to access their Microsoft account.
 */
public class PendingMicrosoftAuthentication {
    /**
     * For GeyserConnect usage.
     */
    private boolean storeServerInformation = false;
    private final LoadingCache<String, AuthenticationTask> authentications;

    public PendingMicrosoftAuthentication(int timeoutSeconds) {
        this.authentications = CacheBuilder.newBuilder()
                .build(new CacheLoader<>() {
                    @Override
                    public AuthenticationTask load(@NonNull String userKey) {
                        return storeServerInformation ? new ProxyAuthenticationTask(userKey, timeoutSeconds * 1000L)
                                : new AuthenticationTask(userKey, timeoutSeconds * 1000L);
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

    @SuppressWarnings("unused") // GeyserConnect
    public void setStoreServerInformation() {
        storeServerInformation = true;
    }

    public class AuthenticationTask {
        private static final Executor DELAYED_BY_ONE_SECOND = CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS);

        @Getter
        private final MsaAuthenticationService msaAuthenticationService = new MsaAuthenticationService(GeyserImpl.OAUTH_CLIENT_ID);
        private final String userKey;
        private final long timeoutMs;

        private long remainingTimeMs;

        @Setter
        private boolean online = true;

        @Getter
        private final CompletableFuture<MsaAuthenticationService> authentication;

        @Getter
        private volatile Throwable loginException;

        private AuthenticationTask(String userKey, long timeoutMs) {
            this.userKey = userKey;
            this.timeoutMs = timeoutMs;
            this.remainingTimeMs = timeoutMs;

            this.authentication = new CompletableFuture<>();
            this.authentication.whenComplete((r, ex) -> {
                this.loginException = ex;
                // avoid memory leak, in case player doesn't connect again
                CompletableFuture.delayedExecutor(timeoutMs, TimeUnit.MILLISECONDS).execute(this::cleanup);
            });
        }

        public void resetTimer() {
            this.remainingTimeMs = this.timeoutMs;
        }

        public void cleanup() {
            GeyserLogger logger = GeyserImpl.getInstance().getLogger();
            if (logger.isDebug()) {
                logger.debug("Cleaning up authentication task for " + userKey);
            }
            authentications.invalidate(userKey);
        }

        public CompletableFuture<MsaAuthenticationService.MsCodeResponse> getCode(boolean offlineAccess) {
            // Request the code
            CompletableFuture<MsaAuthenticationService.MsCodeResponse> code = CompletableFuture.supplyAsync(() -> tryGetCode(offlineAccess));
            // Once the code is received, continuously try to request the access token, profile, etc
            code.thenRun(() -> performLoginAttempt(System.currentTimeMillis()));
            return code;
        }

        /**
         * @param offlineAccess whether we want a refresh token for later use.
         */
        private MsaAuthenticationService.MsCodeResponse tryGetCode(boolean offlineAccess) throws CompletionException {
            try {
                return msaAuthenticationService.getAuthCode(offlineAccess);
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
                            cleanup();
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

    @Getter
    @Setter
    public final class ProxyAuthenticationTask extends AuthenticationTask {
        private String server;
        private int port;

        private ProxyAuthenticationTask(String userKey, long timeoutMs) {
            super(userKey, timeoutMs);
        }
    }

    /**
     * @see PendingMicrosoftAuthentication
     */
    public static class TaskTimeoutException extends Exception {

        @Serial
        private static final long serialVersionUID = 1L;

        TaskTimeoutException() {
            super("It took too long to authorize Geyser to access your Microsoft account. " +
                    "Please request new code and try again.");
        }
    }
}
