package org.orienteer.core.component.visualizer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;
import org.orienteer.core.component.property.DisplayMode;
import org.orienteer.core.util.ODocumentChoiceRenderer;

import ru.ydn.wicket.wicketorientdb.model.DynamicPropertyValueModel;
import ru.ydn.wicket.wicketorientdb.model.OQueryModel;
import ru.ydn.wicket.wicketorientdb.utils.OChoiceRenderer;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {@link IVisualizer} to show links as listboxes
 */
public class ListboxVisualizer extends AbstractSimpleVisualizer
{
	public ListboxVisualizer()
	{
		super("listbox", false, OType.LINK, OType.LINKLIST, OType.LINKSET, OType.LINKBAG);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> Component createComponent(String id, DisplayMode mode,
			IModel<ODocument> documentModel, IModel<OProperty> propertyModel, IModel<V> valueModel) {
		if(DisplayMode.EDIT.equals(mode))
		{
			
			OProperty property = propertyModel.getObject();
			OClass oClass = property.getLinkedClass();
			OQueryModel<ODocument> choicesModel = new OQueryModel<ODocument>("select from "+oClass.getName()+" LIMIT 100");
			if(property.getType().isMultiValue())
			{
				return new ListMultipleChoice<ODocument>(id, (IModel<Collection<ODocument>>) valueModel, choicesModel, new ODocumentChoiceRenderer());
			}
			else
			{
				return new DropDownChoice<ODocument>(id, (IModel<ODocument>)valueModel, 
					choicesModel, new ODocumentChoiceRenderer())
					.setNullValid(!property.isNotNull());
			}
		}
		else
		{
			return null;
		}
	}

}
