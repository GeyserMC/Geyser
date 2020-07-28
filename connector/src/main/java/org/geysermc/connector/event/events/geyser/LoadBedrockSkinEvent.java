/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.event.events.geyser;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.event.Cancellable;
import org.geysermc.connector.event.GeyserEvent;
import org.geysermc.connector.network.session.auth.BedrockClientData;

/**
 * Triggered when a Bedrock skin needs to be loaded
 *
 * If cancelled then the regular skin loading will not occur.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("JavaDoc")
public class LoadBedrockSkinEvent extends GeyserEvent implements Cancellable {
    private boolean cancelled;

    /**
     * The player for which the skin is to be loaded for
     *
     * @param playerEntity The player entity
     * @return The current player entity
     */
    @NonNull
    private PlayerEntity playerEntity;

    /**
     * The client data of the player
     *
     * @param clientData Player client data
     * @return The current client data
     */
    @NonNull
    private BedrockClientData clientData;
}
