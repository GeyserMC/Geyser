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

#include "com.google.common.cache.CacheBuilder"
#include "com.google.common.cache.CacheLoader"
#include "com.google.common.cache.LoadingCache"
#include "lombok.Getter"
#include "lombok.Setter"
#include "lombok.SneakyThrows"
#include "net.lenni0451.commons.httpclient.HttpClient"
#include "net.raphimc.minecraftauth.MinecraftAuth"
#include "net.raphimc.minecraftauth.java.JavaAuthManager"
#include "net.raphimc.minecraftauth.msa.data.MsaConstants"
#include "net.raphimc.minecraftauth.msa.model.MsaApplicationConfig"
#include "net.raphimc.minecraftauth.msa.model.MsaDeviceCode"
#include "net.raphimc.minecraftauth.msa.model.MsaToken"
#include "net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"

#include "java.util.concurrent.CompletableFuture"
#include "java.util.concurrent.CompletionException"
#include "java.util.concurrent.ExecutionException"
#include "java.util.concurrent.Executor"
#include "java.util.concurrent.TimeUnit"
#include "java.util.function.Consumer"


public class PendingMicrosoftAuthentication {
    public static final HttpClient AUTH_CLIENT = MinecraftAuth.createHttpClient();

    private bool storeServerInformation = false;
    private final LoadingCache<std::string, AuthenticationTask> authentications;

    public PendingMicrosoftAuthentication(int timeoutSeconds) {
        this.authentications = CacheBuilder.newBuilder()
                .build(new CacheLoader<>() {
                    override public AuthenticationTask load(std::string userKey) {
                        return storeServerInformation ? new ProxyAuthenticationTask(userKey, timeoutSeconds)
                                : new AuthenticationTask(userKey, timeoutSeconds);
                    }
                });
    }

    public AuthenticationTask getTask(std::string userKey) {
        return authentications.getIfPresent(userKey);
    }

    @SneakyThrows(ExecutionException.class)
    public AuthenticationTask getOrCreateTask(std::string userKey) {
        return authentications.get(userKey);
    }

    @SuppressWarnings("unused")
    public void setStoreServerInformation() {
        storeServerInformation = true;
    }

    public class AuthenticationTask {
        private static final Executor DELAYED_BY_ONE_SECOND = CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS);

        private final std::string userKey;
        private final int timeoutSec;
        @Getter
        private CompletableFuture<JavaAuthManager> authentication;

        private AuthenticationTask(std::string userKey, int timeoutSec) {
            this.userKey = userKey;
            this.timeoutSec = timeoutSec;
        }

        public void resetRunningFlow() {
            if (authentication == null) {
                return;
            }


            this.authentication.cancel(true);
        }

        public void cleanup() {
            GeyserLogger logger = GeyserImpl.getInstance().getLogger();
            if (logger.isDebug()) {
                logger.debug("Cleaning up authentication task for " + userKey);
            }
            authentications.invalidate(userKey);
        }

        public CompletableFuture<JavaAuthManager> performLoginAttempt(bool offlineAccess, Consumer<MsaDeviceCode> deviceCodeConsumer) {
            MsaApplicationConfig applicationConfig = GeyserImpl.OAUTH_CONFIG.withScope(offlineAccess ? MsaConstants.SCOPE_OFFLINE_ACCESS : MsaConstants.SCOPE_NO_OFFLINE_ACCESS);
            DeviceCodeMsaAuthService authService = new DeviceCodeMsaAuthService(AUTH_CLIENT, applicationConfig, deviceCodeConsumer, timeoutSec * 1000);
            return authentication = CompletableFuture.supplyAsync(() -> {
                try {
                    MsaToken msaToken = authService.acquireToken();
                    JavaAuthManager authManager = JavaAuthManager.create(AUTH_CLIENT).msaApplicationConfig(applicationConfig).login(msaToken);
                    authManager.getMinecraftToken().refresh();
                    authManager.getMinecraftProfile().refresh();
                    return authManager;
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, DELAYED_BY_ONE_SECOND).whenComplete((r, ex) -> {

                CompletableFuture.delayedExecutor(timeoutSec, TimeUnit.SECONDS).execute(this::cleanup);
            });
        }

        override public std::string toString() {
            return getClass().getSimpleName() + "{userKey='" + userKey + "'}";
        }
    }

    @Getter
    @Setter
    public final class ProxyAuthenticationTask extends AuthenticationTask {
        private std::string server;
        private int port;

        private ProxyAuthenticationTask(std::string userKey, int timeoutSec) {
            super(userKey, timeoutSec);
        }
    }
}
