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

package org.geysermc.connector.network.session.cache;

import com.nukkitx.protocol.bedrock.packet.ModalFormRequestPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.geysermc.common.window.FormWindow;
import org.geysermc.common.window.response.FormResponse;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.form.FormListener;

public class WindowCache {

    private final GeyserSession session;

    private int currentWindowId;

    @Getter
    private final Int2ObjectMap<FormWindow<?>> windows = new Int2ObjectOpenHashMap<>();

    private final Int2ObjectMap<FormListener<?>> formListeners = new Int2ObjectOpenHashMap<>();

    public WindowCache(GeyserSession session) {
        this.session = session;
    }

    public boolean handleFormResponse(int formId, String formResponse) {
        FormWindow<?> formWindow = windows.get(formId);

        if (formWindow == null)
            return false;

        FormListener<?> formListener = formListeners.remove(formId);

        if (formListener == null)
            return false;

        windows.remove(formId);
        formWindow.setResponse(formResponse);

        formListener.handleFormResponseCasted(formWindow.getResponse());
        return true;
    }

    public void addWindow(FormWindow<?> window) {
        windows.put(this.getAvailableWindowId(), window);
    }

    public void addWindow(FormWindow<?> window, int id) {
        windows.put(id, window);
    }

    public void showWindow(FormWindow<?> window) {
        showWindow(window, this.getAvailableWindowId());
    }

    public <T extends FormResponse> void showWindow(FormWindow<T> window, FormListener<T> formListener) {
        showWindow(window, this.getAvailableWindowId(), formListener);
    }

    public void showWindow(int id) {
        if (!windows.containsKey(id))
            return;

        ModalFormRequestPacket formRequestPacket = new ModalFormRequestPacket();
        formRequestPacket.setFormId(id);
        formRequestPacket.setFormData(windows.get(id).getJSONData());

        session.sendUpstreamPacket(formRequestPacket);
    }

    public void showWindow(FormWindow<?> window, int id) {
        this.showWindow(window, id, null);
    }

    /**
     * Sends a form window to the player
     *
     * @param window       the form window
     * @param id           the window's id
     * @param formListener the listener that should be called, as soon as there is a response,
     *                     or <code>null</code> if there is an other way the form gets handled
     * @param <T>          the type of the response
     */
    private <T extends FormResponse> void showWindow(FormWindow<T> window, int id, FormListener<T> formListener) {
        if (formListeners.containsKey(id))
            formListeners.remove(id);

        ModalFormRequestPacket formRequestPacket = new ModalFormRequestPacket();
        formRequestPacket.setFormId(id);
        formRequestPacket.setFormData(window.getJSONData());

        session.sendUpstreamPacket(formRequestPacket);

        if (formListener != null)
            formListeners.put(id, formListener);

        addWindow(window, id);
    }

    private int getAvailableWindowId() {
        do {
            this.currentWindowId++;
        } while (windows.containsKey(this.currentWindowId));

        return this.currentWindowId;
    }

}
