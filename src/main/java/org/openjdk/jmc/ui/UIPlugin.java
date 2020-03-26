package org.openjdk.jmc.ui;

import java.util.logging.Logger;

/**
 * FIXME This is only used for stubbing out a Logger instance used by JMC's JFCXMLValidator !!! This
 * is a major classloading hackaround !!!
 */
public class UIPlugin {

    public static UIPlugin getDefault() {
        return new UIPlugin();
    }

    public Logger getLogger() {
        return Logger.getGlobal();
    }
}
