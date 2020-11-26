/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.common.form;

import org.geysermc.common.form.response.FormResponse;

import java.util.function.Consumer;

/**
 * Base class of all Forms. While it can be used it doesn't contain every data you could get when
 * using the specific class of the form type.
 *
 * @param <T> class provided by the specific form type. It understands the response data and makes
 *            the data easily accessible
 */
public interface Form<T extends FormResponse> {
    /**
     * Returns the form type of this specific instance. The valid form types can be found {@link
     * FormType in the FormType class}
     */
    FormType getType();

    /**
     * Returns the data that will be sent by Geyser to the Bedrock client
     */
    String getJsonData();

    /**
     * Returns the handler that will be invoked once the form got a response from the Bedrock
     * client
     */
    Consumer<String> getResponseHandler();

    /**
     * Sets the handler that will be invoked once the form got a response from the Bedrock client.
     * This handler contains the raw data sent by the Bedrock client. See {@link
     * #parseResponse(String)} if you want to turn the given data into something that's easier to
     * handle.
     *
     * @param responseHandler the response handler
     */
    void setResponseHandler(Consumer<String> responseHandler);

    /**
     * Parses the method into something provided by the form implementation, which will make the
     * data given by the Bedrock client easier to handle.
     *
     * @param response the raw data given by the Bedrock client
     * @return the data in an easy-to-handle class
     */
    T parseResponse(String response);

    /**
     * Checks if the given data by the Bedrock client is saying that the client closed the form.
     *
     * @param response the raw data given by the Bedrock client
     * @return true if the raw data implies that the Bedrock client closed the form
     */
    boolean isClosed(String response);
}
