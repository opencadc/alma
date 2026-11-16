/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2026.                            (c) 2026.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 ************************************************************************
 */

package org.opencadc.tap.ws;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.NotAuthenticatedException;
import ca.nrc.cadc.reg.Capabilities;
import ca.nrc.cadc.reg.Capability;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.rest.SyncOutput;
import ca.nrc.cadc.vosi.CapGetAction;
import ca.nrc.cadc.vosi.CapInitAction;
import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.opencadc.alma.AlmaProperties;
import org.opencadc.tap.impl.ALMAUploadManager;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Serves {@code /capabilities} with TAP {@code uploadLimit} aligned to {@code almaTAPUploadByteLimit}.
 */
public class TapCapGetAction extends CapGetAction {
    private static final Logger log = Logger.getLogger(TapCapGetAction.class);

    @Override
    public void doAction() throws Exception {
        if (capInitAuthRequired(componentID)) {
            AuthMethod am = AuthenticationUtil.getAuthMethod(AuthenticationUtil.getCurrentSubject());
            if (am == null || am.equals(AuthMethod.ANON)) {
                throw new NotAuthenticatedException("permission denied");
            }
        }

        Capabilities caps = capInitTemplate(componentID);
        applyUploadByteLimit(caps);

        log.debug("transformAccessURL=" + doTransform);

        if (doTransform) {
            invokeTransform(caps);
        }

        invokeDoOutput(caps, syncOutput);
        logInfo.setSuccess(true);
    }

    static void applyUploadByteLimit(Capabilities caps) {
        if (caps == null) {
            return;
        }
        final Capability tap = caps.findCapability(Standards.TAP_10);
        if (tap == null) {
            log.warn("TAP capability not found in capabilities template; skipping uploadLimit patch");
            return;
        }
        final long byteLimit =
                new AlmaProperties().getTAPUploadByteLimit(ALMAUploadManager.DEFAULT_UPLOAD_BYTE_LIMIT);
        for (Element extension : extensionMetadata(tap)) {
            if ("uploadLimit".equals(extension.getName())) {
                setUploadLimitBytes(extension, byteLimit);
                log.debug("uploadLimit set to " + byteLimit + " bytes from AlmaProperties");
                return;
            }
        }
        log.warn("uploadLimit element not found in TAP capability; skipping uploadLimit patch");
    }

    private static void setUploadLimitBytes(final Element uploadLimit, final long bytes) {
        final String value = Long.toString(bytes);
        for (Element child : uploadLimit.getChildren()) {
            final String name = child.getName();
            if ("default".equals(name) || "hard".equals(name)) {
                child.setText(value);
                child.setAttribute("unit", "byte");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Element> extensionMetadata(final Capability capability) {
        try {
            Method accessor = Capability.class.getDeclaredMethod("getExtensionMetadata");
            accessor.setAccessible(true);
            return (List<Element>) accessor.invoke(capability);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("failed to read capability extension metadata", ex);
        }
    }

    private static boolean capInitAuthRequired(final String id) {
        try {
            Method m = CapInitAction.class.getDeclaredMethod("getAuthRequired", String.class);
            m.setAccessible(true);
            return (Boolean) m.invoke(null, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("failed to read authRequired from CapInitAction", ex);
        }
    }

    private static Capabilities capInitTemplate(final String id) {
        try {
            Method m = CapInitAction.class.getDeclaredMethod("getTemplate", String.class);
            m.setAccessible(true);
            return (Capabilities) m.invoke(null, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("failed to read capabilities template from CapInitAction", ex);
        }
    }

    private void invokeTransform(final Capabilities caps) throws Exception {
        Method m = CapGetAction.class.getDeclaredMethod("transform", Capabilities.class);
        m.setAccessible(true);
        m.invoke(this, caps);
    }

    private void invokeDoOutput(final Capabilities caps, final SyncOutput out) throws Exception {
        Method m = CapGetAction.class.getDeclaredMethod("doOutput", Capabilities.class, SyncOutput.class);
        m.setAccessible(true);
        m.invoke(this, caps, out);
    }
}