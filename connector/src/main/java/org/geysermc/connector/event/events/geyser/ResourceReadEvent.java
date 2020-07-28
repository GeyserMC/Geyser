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
import org.geysermc.connector.event.GeyserEvent;

import java.io.InputStream;


/**
 * Triggered when a resource needs to be read
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("JavaDoc")
public class ResourceReadEvent extends GeyserEvent {

    /**
     * The name of resource to load
     *
     * @param resourceName name of resource
     * @return name of resource
     */
    @NonNull
    private String resourceName;

    /**
     * InputStream to the loaded data
     *
     * Geyser will use the value here for the returned stream. This will initially contain the stream that
     * Geyser would normally load.
     *
     * @param inputStream the InputStream for the data, null if invalid
     * @return current InputStream for the resource, null if invalid
     */
    @NonNull
    private InputStream inputStream;
}
