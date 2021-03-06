package org.orienteer.core.component.visualizer;

import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.orienteer.core.component.property.DisplayMode;
import org.orienteer.core.component.property.OLocalizationEditPanel;
import org.orienteer.core.component.property.OLocalizationViewPanel;

/**
 * {@link IVisualizer} to display and modify documents localizations in Orienteer.
 */
public class LocalizationVisualizer extends AbstractSimpleVisualizer {

    public LocalizationVisualizer() {
        super("localization", false, OType.EMBEDDEDMAP);
    }

    @Override
    public <V> Component createComponent(String id, DisplayMode mode, IModel<ODocument> documentModel, IModel<OProperty> propertyModel, IModel<V> valueModel) {
        switch (mode)
        {
            case VIEW:
                return new OLocalizationViewPanel<V>(id, documentModel, propertyModel).setEscapeModelStrings(false);
            case EDIT:
                return new OLocalizationEditPanel<V>(id, documentModel, propertyModel).setType(String.class);
            default:
                return null;
        }
    }
}
