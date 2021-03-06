package org.orienteer.core;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.core.request.mapper.HomePageMapper;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.datetime.StyleDateConverter;
import org.apache.wicket.guice.GuiceInjectorHolder;
import org.apache.wicket.markup.html.IPackageResourceGuard;
import org.apache.wicket.markup.html.SecurePackageResourceGuard;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.settings.IRequestCycleSettings;
import org.apache.wicket.util.convert.IConverter;
import org.orienteer.core.component.visualizer.UIVisualizersRegistry;
import org.orienteer.core.hook.CalculablePropertiesHook;
import org.orienteer.core.hook.ReferencesConsistencyHook;
import org.orienteer.core.module.IOrienteerModule;
import org.orienteer.core.module.ModuledDataInstallator;
import org.orienteer.core.module.OWidgetsModule;
import org.orienteer.core.module.OrienteerLocalizationModule;
import org.orienteer.core.module.PerspectivesModule;
import org.orienteer.core.module.UpdateDefaultSchemaModule;
import org.orienteer.core.service.IOClassIntrospector;
import org.orienteer.core.web.BasePage;
import org.orienteer.core.web.HomePage;
import org.orienteer.core.web.LoginPage;
import org.orienteer.core.widget.AbstractWidget;
import org.orienteer.core.widget.IWidgetTypesRegistry;
import org.orienteer.core.widget.Widget;

import ru.ydn.wicket.wicketorientdb.EmbeddOrientDbApplicationListener;
import ru.ydn.wicket.wicketorientdb.IOrientDbSettings;
import ru.ydn.wicket.wicketorientdb.OrientDbWebApplication;
import ru.ydn.wicket.wicketorientdb.OrientDbWebSession;
import ru.ydn.wicket.wicketorientdb.rest.OrientDBHttpAPIResource;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import de.agilecoders.wicket.webjars.WicketWebjars;
import de.agilecoders.wicket.webjars.settings.IWebjarsSettings;
import de.agilecoders.wicket.webjars.settings.WebjarsSettings;

/**
 * Main {@link WebApplication} for Orienteer bases applications
 */
public class OrienteerWebApplication extends OrientDbWebApplication
{
	public static final DateConverter DATE_CONVERTER = new StyleDateConverter("L-", true);
	public static final DateConverter DATE_TIME_CONVERTER = new StyleDateConverter("LL", true);
	
	private Map<String, IOrienteerModule> registeredModules = new LinkedHashMap<String, IOrienteerModule>();
	
	@Inject
	private IWebjarsSettings webjarSettings;
	
	@Inject
	@Named("orienteer.production")
	private boolean production;
	
	@Inject
	@Named("orientdb.embedded")
	private boolean embedded;
	
	@Inject(optional=true)
	@Named("wicket.render.strategy")
	private IRequestCycleSettings.RenderStrategy renderStrategy;

	@Inject
	@Named("orienteer.image.logo")
	private String imageLogoPath;

	@Inject
	public OrienteerWebApplication()
	{
		setConfigurationType(production?RuntimeConfigurationType.DEPLOYMENT:RuntimeConfigurationType.DEVELOPMENT);
	}
	
	@Inject
	@Override
	public void setOrientDbSettings(IOrientDbSettings orientDbSettings) {
		super.setOrientDbSettings(orientDbSettings);
	}
	
	public static OrienteerWebApplication get()
    {
        return (OrienteerWebApplication) WebApplication.get();
    }
	
	public static OrienteerWebApplication lookupApplication()
	{
		return lookupApplication(OrienteerWebApplication.class);
	}

	@Override
	public Class<? extends WebPage> getHomePage()
	{
		return HomePage.class;
	}
	
	@Override
	protected Class<? extends OrienteerWebSession> getWebSessionClass()
	{
		return OrienteerWebSession.class;
	}

	/**
	 * @see org.apache.wicket.Application#init()
	 */
	@Override
	public void init()
	{
		super.init();
		if(embedded)
		{
			getApplicationListeners().add(new EmbeddOrientDbApplicationListener(OrienteerWebApplication.class.getResource("db.config.xml"))
			{

				@Override
				public void onAfterServerStartupAndActivation(OrientDbWebApplication app)
						throws Exception {
					IOrientDbSettings settings = app.getOrientDbSettings();
					ODatabaseDocumentTx db = new ODatabaseDocumentTx(settings.getDBUrl());
					if(!db.exists()) db = db.create();
					if(db.isClosed()) db.open(settings.getDBInstallatorUserName(), settings.getDBInstallatorUserPassword());
					db.getMetadata().load();
					db.close();
				}
				
			});
		}
		WicketWebjars.install(this, webjarSettings);
		mountPages("org.orienteer.core.web");
		getResourceBundles().addCssBundle(BasePage.class, "orienteer.css", BasePage.SB_ADMIN_CSS, BasePage.ORIENTEER_CSS);
		mountResource("logo.png", new SharedResourceReference(imageLogoPath));
		getMarkupSettings().setStripWicketTags(true);
		getResourceSettings().setThrowExceptionOnMissingResource(false);
		getApplicationListeners().add(new ModuledDataInstallator());
		registerModule(OrienteerLocalizationModule.class);
		registerModule(UpdateDefaultSchemaModule.class);
		registerModule(PerspectivesModule.class);
		registerModule(OWidgetsModule.class);
		getOrientDbSettings().getORecordHooks().add(CalculablePropertiesHook.class);
		getOrientDbSettings().getORecordHooks().add(ReferencesConsistencyHook.class);
		mountOrientDbRestApi();
		registerWidgets("org.orienteer.core.component.widget");
		if(renderStrategy!=null) getRequestCycleSettings().setRenderStrategy(renderStrategy);
	}

	@Override
	protected Class<? extends WebPage> getSignInPageClass() {
		return LoginPage.class;
	}
	
	public Injector getInjector()
	{
		return getMetaData(GuiceInjectorHolder.INJECTOR_KEY).getInjector();
	}
	
	public <T> T getServiceInstance(Class<T> serviceType)
	{
		return getInjector().getInstance(serviceType);
	}
	
	public ODatabaseDocument getDatabase()
	{
		return OrientDbWebSession.get().getDatabase();
	}

	public Map<String, IOrienteerModule> getRegisteredModules() {
		return registeredModules;
	}
	
	public <M extends IOrienteerModule> M registerModule(Class<M> moduleClass)
	{
		M module = getServiceInstance(moduleClass);
		registeredModules.put(module.getName(), module);
		return module;
	}
	
	public IOrienteerModule getModuleByName(String name)
	{
		return registeredModules.get(name);
	}
	
	public UIVisualizersRegistry getUIVisualizersRegistry()
	{
		return getServiceInstance(UIVisualizersRegistry.class);
	}
	
	public IOClassIntrospector getOClassIntrospector()
	{
		return getServiceInstance(IOClassIntrospector.class);
	}
	
	public void mountPages(String packageName) {
		mountPages(packageName, OrienteerWebApplication.class.getClassLoader());
	}
	
	public void mountPages(String packageName, ClassLoader classLoader) {
		ClassPath classPath;
		try {
			classPath = ClassPath.from(classLoader);
		} catch (IOException e) {
			throw new WicketRuntimeException("Can't scan classpath", e);
		}
		
		for(ClassInfo classInfo : classPath.getTopLevelClassesRecursive(packageName)) {
			Class<?> clazz = classInfo.load();
			MountPath mountPath = clazz.getAnnotation(MountPath.class);
			if(mountPath!=null) {
				if(!IRequestablePage.class.isAssignableFrom(clazz)) 
					throw new WicketRuntimeException("@"+MountPath.class.getSimpleName()+" should be only on pages");
				Class<? extends IRequestablePage> pageClass = (Class<? extends IRequestablePage>) clazz;
				String mainPath = mountPath.value();
				String[] alt = mountPath.alt();
				for(int i=alt.length-1;i>=-1;i--)
				{
					String path = i<0?mainPath:alt[i];
					if ("/".equals(path)) {
						mount(new HomePageMapper(pageClass));
					}
					mount(new MountedMapper(path, pageClass));
				}
			}
		}
	}
	
	public void registerWidgets(String packageName) {
		IWidgetTypesRegistry registry = getServiceInstance(IWidgetTypesRegistry.class);
		registry.register(packageName);
	}
	
	@Override
	public void restartResponseAtSignInPage() {
		//This is required because home page is dynamic and depends on assigned perspective.
		if(RequestCycle.get().getRequest().getQueryParameters().getParameterValue(HomePage.FROM_HOME_PARAM).toBoolean(false)) {
			throw new RestartResponseException(getSignInPageClass());
		} else super.restartResponseAtSignInPage();
	}
}
