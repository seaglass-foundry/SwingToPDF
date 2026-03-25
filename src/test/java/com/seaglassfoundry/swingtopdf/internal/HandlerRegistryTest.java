package com.seaglassfoundry.swingtopdf.internal;

import org.junit.jupiter.api.Test;


import javax.swing.*;
import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HandlerRegistry} superclass-hierarchy lookup.
 */
class HandlerRegistryTest {

    /** Stub handler that records its identity. */
    private static ComponentHandler handler(String name) {
        return new ComponentHandler() {
            @Override
            public void render(Component comp, int absX, int absY, HandlerContext ctx) {}

            @Override
            public String toString() { return name; }
        };
    }

    @Test
    void exactMatch_returnsRegisteredHandler() {
        HandlerRegistry reg = new HandlerRegistry();
        ComponentHandler h = handler("JButton");
        reg.register(JButton.class, h);

        assertThat(reg.lookup(JButton.class)).isSameAs(h);
    }

    @Test
    void subclass_findsParentHandler() {
        HandlerRegistry reg = new HandlerRegistry();
        ComponentHandler h = handler("AbstractButton");
        reg.register(AbstractButton.class, h);

        // JButton extends AbstractButton — should find the parent handler
        assertThat(reg.lookup(JButton.class)).isSameAs(h);
    }

    @Test
    void moreSpecificRegistration_winsOverParent() {
        HandlerRegistry reg = new HandlerRegistry();
        ComponentHandler general = handler("AbstractButton");
        ComponentHandler specific = handler("JButton");
        reg.register(AbstractButton.class, general);
        reg.register(JButton.class, specific);

        assertThat(reg.lookup(JButton.class)).isSameAs(specific);
        assertThat(reg.lookup(JToggleButton.class)).isSameAs(general);
    }

    @Test
    void unregisteredType_returnsNull() {
        HandlerRegistry reg = new HandlerRegistry();
        reg.register(JButton.class, handler("JButton"));

        assertThat(reg.lookup(JLabel.class)).isNull();
    }

    @Test
    void deepHierarchy_walksUpToRegistered() {
        HandlerRegistry reg = new HandlerRegistry();
        ComponentHandler h = handler("JComponent");
        reg.register(JComponent.class, h);

        // JCheckBox -> JToggleButton -> AbstractButton -> JComponent
        assertThat(reg.lookup(JCheckBox.class)).isSameAs(h);
    }

    @Test
    void objectClass_neverMatched() {
        // lookup walks up the hierarchy but stops before Object.class
        HandlerRegistry reg = new HandlerRegistry();
        assertThat(reg.lookup(JPanel.class)).isNull();
    }

    @Test
    void emptyRegistry_returnsNull() {
        HandlerRegistry reg = new HandlerRegistry();
        assertThat(reg.lookup(JButton.class)).isNull();
    }
}
