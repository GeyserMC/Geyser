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

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.geysermc.geyser.GeyserImpl;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class PendingMicrosoftAuthentication {
    private final LoadingCache<String, AuthenticationTask> authentications;

    public PendingMicrosoftAuthentication(int timeoutSeconds) {
        this.authentications = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(timeoutSeconds))
                .build(new CacheLoader<>() {
                    @Override
                    public AuthenticationTask load(@NonNull String userKey) {
                        return new AuthenticationTask(userKey, timeoutSeconds * 1000L);
                    }
                });
    }

    @SneakyThrows(ExecutionException.class)
    public AuthenticationTask queryOrCreatePendingTask(GeyserSession session) {
        return authentications.get(
                Objects.requireNonNull(session.getAuthData(), "authData").xuid()
        );
    }

    public static class AuthenticationTask {
        private final MsaAuthenticationService msaAuthenticationService =
                new MsaAuthenticationService(GeyserImpl.OAUTH_CLIENT_ID);

        private final String userKey;
        private final CompletableFuture<MsaAuthenticationService.MsCodeResponse> code;
        private final CompletableFuture<MsaAuthenticationService> auth;
        private final long timeoutMs;

        private AuthenticationTask(String userKey, long timeoutMs) {
            this.userKey = userKey;
            this.timeoutMs = timeoutMs;

            // Request the code
            this.code = CompletableFuture.supplyAsync(this::tryGetCode);

            // Once the code is received, continuously try to request the access token, profile, etc
            this.auth = code.thenApply((code) -> tryLoginContinuously());
        }

        public CompletableFuture<MsaAuthenticationService.MsCodeResponse> getCode() {
            return code;
        }

        public CompletableFuture<MsaAuthenticationService> getAuthentication() {
            return auth;
        }

        private MsaAuthenticationService.MsCodeResponse tryGetCode() throws CompletionException {
            try {
                return msaAuthenticationService.getAuthCode();
            } catch (RequestException e) {
                throw new CompletionException(e);
            }
        }

        private MsaAuthenticationService tryLoginContinuously() throws CompletionException {
            try {
                long startTime = System.currentTimeMillis();
                while (true) {
                    try {
                        msaAuthenticationService.login();
                    } catch (RequestException ignored) {
                        long deltaTime = System.currentTimeMillis() - startTime;
                        if(deltaTime > timeoutMs) {
                            throw new TimeoutException();
                        }
                        //noinspection BusyWait
                        Thread.sleep(1000);
                        continue;
                    }
                    return msaAuthenticationService;
                }
            } catch(Exception e) {
                throw new CompletionException(e);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "userKey='" + userKey + '\'' +
                    '}';
        }
    }
}
