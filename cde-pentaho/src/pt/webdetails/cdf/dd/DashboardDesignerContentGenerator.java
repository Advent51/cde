/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package pt.webdetails.cdf.dd;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPluginResourceLoader;
import org.pentaho.platform.api.engine.PentahoAccessControlException;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import pt.webdetails.cdf.dd.cdf.CdfStyles;
import pt.webdetails.cdf.dd.cdf.CdfTemplates;
import pt.webdetails.cdf.dd.datasources.CdaDataSourceReader;
import pt.webdetails.cdf.dd.editor.DashboardEditor;
import pt.webdetails.cdf.dd.editor.ExternalFileEditor;
import pt.webdetails.cdf.dd.model.core.writer.ThingWriteException;
import pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.dashboard.CdfRunJsDashboardWriteOptions;
import pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.dashboard.CdfRunJsDashboardWriteResult;
import pt.webdetails.cdf.dd.packager.Packager;
import pt.webdetails.cdf.dd.structure.DashboardWcdfDescriptor;
import pt.webdetails.cdf.dd.util.JsonUtils;
import pt.webdetails.cdf.dd.util.Utils;
import pt.webdetails.cdf.dd.utils.CommonParameterProvider;
import pt.webdetails.cpf.InterPluginCall;
import pt.webdetails.cpf.SimpleContentGenerator;
import pt.webdetails.cpf.VersionChecker;
import pt.webdetails.cpf.repository.IRepositoryAccess;
import pt.webdetails.cpf.annotations.AccessLevel;
import pt.webdetails.cpf.annotations.Audited;
import pt.webdetails.cpf.annotations.Exposed;
import pt.webdetails.cpf.olap.OlapUtils;
import pt.webdetails.cpf.repository.IRepositoryAccess.FileAccess;
import pt.webdetails.cpf.repository.PentahoRepositoryAccess;
import pt.webdetails.cpf.utils.MimeTypes;

public class DashboardDesignerContentGenerator extends SimpleContentGenerator {
	public static final String PLUGIN_NAME = "pentaho-cdf-dd";

	public static final String SYSTEM_PATH = "system";

	public static final String PLUGIN_PATH = SYSTEM_PATH + "/"
			+ DashboardDesignerContentGenerator.PLUGIN_NAME + "/";

	public static final String MOLAP_PLUGIN_PATH = SYSTEM_PATH + "/MOLA/";

	/**
	 * solution folder for custom components, styles and templates
	 */
	public static final String SOLUTION_DIR = "cde";

	public static final String SERVER_URL_VALUE = PentahoSystem
			.getApplicationContext().getBaseUrl() + "content/pentaho-cdf-dd/";

	private static final Log logger = LogFactory
			.getLog(DashboardDesignerContentGenerator.class);

	private static final long serialVersionUID = 1L;

	private static final String MIME_TYPE = "text/html";

	private static final String CSS_TYPE = "text/css";

	private static final String JAVASCRIPT_TYPE = "text/javascript";

	private static final String FILE_NAME_TAG = "@FILENAME@";

	private static final String SERVER_URL_TAG = "@SERVERURL@";

	private static final String DESIGNER_RESOURCE = "resources/cdf-dd.html";

	private static final String DESIGNER_STYLES_RESOURCE = "resources/styles.html";

	private static final String DESIGNER_SCRIPTS_RESOURCE = "resources/scripts.html";

	private static final String DESIGNER_HEADER_TAG = "@HEADER@";

	private static final String DESIGNER_CDF_TAG = "@CDF@";

	private static final String DESIGNER_STYLES_TAG = "@STYLES@";

	private static final String DESIGNER_SCRIPTS_TAG = "@SCRIPTS@";

	private static final String DATA_URL_TAG = "cdf-structure.js";

	private static final String DATA_URL_VALUE = PentahoSystem
			.getApplicationContext().getBaseUrl()
			+ "content/pentaho-cdf-dd/Syncronize";

	/**
	 * 1 week cache
	 */
	private static final int RESOURCE_CACHE_DURATION = 3600 * 24 * 8; // 1 week
																		// cache

	private static final int NO_CACHE_DURATION = 0;

	private static final String EXTERNAL_EDITOR_PAGE = "resources/ext-editor.html";

	private static final String COMPONENT_EDITOR_PAGE = "resources/cdf-dd-component-editor.html";

	private Packager packager;

	/**
	 * Parameters received by content generator
	 */
	protected static class MethodParams {
		public static final String DEBUG = "debug";

		public static final String ROOT = "root";

		public static final String SOLUTION = "solution";

		public static final String PATH = "path";

		public static final String FILE = "file";

		/**
		 * JSON structure
		 */

		public static final String DATA = "data";
	}

	/*
	 * This block initializes exposed methods
	 */
	private static Map<String, Method> exposedMethods = new HashMap<String, Method>();

	static {
		// to keep case-insensitive methods
		logger.info("loading exposed methods");
		exposedMethods = getExposedMethods(
				DashboardDesignerContentGenerator.class, true);
	}

	@Override
	protected Method getMethod(String methodName) throws NoSuchMethodException {
		Method method = exposedMethods.get(StringUtils.lowerCase(methodName));
		if (method == null) {
			throw new NoSuchMethodException();
		}
		return method;
	}

	public DashboardDesignerContentGenerator() {
		try {
			init();
		} catch (IOException ex) {
			logger.error("Failed to initialize!");
		}
	}

	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void syncronize(final OutputStream out) throws Exception {
		// 0 - Check security. Caveat: if no path is supplied, then we're in the
		// new parameter
		IParameterProvider requestParams = getRequestParameters();
		final String path = ((String) requestParams
				.getParameter(MethodParams.FILE)).replaceAll("cdfde", "wcdf");
		if (requestParams.hasParameter(MethodParams.PATH)
				&& !PentahoRepositoryAccess.getRepository(userSession)
						.hasAccess(path, FileAccess.EXECUTE)) {
			final HttpServletResponse response = getResponse();
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			logger.warn("Access denied for the syncronize method: " + path
					+ " by user " + userSession.getName());
			return;
		}

		final SyncronizeCdfStructure syncCdfStructure = new SyncronizeCdfStructure();
		syncCdfStructure.syncronize(out, new CommonParameterProvider(
				requestParams));
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	@Audited(action = "edit")
	public void newDashboard(final OutputStream out) throws Exception {
		this.edit(out);
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC, outputType = MimeType.JAVASCRIPT)
	public void getComponentDefinitions(OutputStream out) throws Exception {
		// Get and output the definitions
		try {
			String definition = MetaModelManager.getInstance()
					.getJsDefinition();
			out.write(definition.getBytes());
		} catch (Exception ex) {
			String msg = "Could not get component definitions: "
					+ ex.getMessage();
			logger.error(msg);
			throw ex;
		}
	}

	/**
	 * Re-initializes the designer back-end.
	 * 
	 * @author pdpi
	 */
	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public static void refresh() throws Exception {
		DashboardManager.getInstance().refreshAll();
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void getContent(OutputStream out) throws Exception {
		CdfRunJsDashboardWriteResult dashboardWrite = this.loadDashboard();
		writeOut(out, dashboardWrite.getContent());
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void getHeaders(final OutputStream out) throws Exception {
		CdfRunJsDashboardWriteResult dashboardWrite = this.loadDashboard();
		writeOut(out, dashboardWrite.getHeader());
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	@Audited(action = "render")
	public void render(final OutputStream out) throws IOException {
		String relativePath = getWcdfRelativePath(getRequestParameters());

		// Check security
		if (!PentahoRepositoryAccess.getRepository().hasAccess(relativePath,
				FileAccess.EXECUTE)) {
			writeOut(out, "Access Denied or File Not Found.");
			return;
		}

		// Response
		try {
			setResponseHeaders(MIME_TYPE, 0, null);
		} catch (Exception e) {
			logger.warn(e.toString());
		}

		try {
			Date dtStart = new Date();
			logger.info("[Timing] CDE Starting Dashboard Rendering");
			writeOut(out, this.loadDashboard().render(getCdfContext()));
			logger.info("[Timing] CDE Finished Dashboard Rendering: "
					+ Utils.ellapsedSeconds(dtStart) + "s");
		} catch (FileNotFoundException ex) { // could not open cdfe
			String msg = "File not found: " + ex.getLocalizedMessage();
			logger.error(msg);
			writeOut(out, msg);
		} catch (Exception ex) {
			String msg = "Could not load dashboard: " + ex.getMessage();
			logger.error(msg);
			writeOut(out, msg);
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC, outputType = MimeType.CSS)
	public void getCssResource(final OutputStream out) throws Exception {
		setResponseHeaders(CSS_TYPE, RESOURCE_CACHE_DURATION, null);
		getResource(out);
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC, outputType = MimeType.JAVASCRIPT)
	public void getJsResource(final OutputStream out) throws Exception {
		setResponseHeaders(JAVASCRIPT_TYPE, RESOURCE_CACHE_DURATION, null);
		final HttpServletResponse response = getResponse();
		// Set cache for 1 year, give or take.
		response.setHeader("Cache-Control", "max-age=" + 60 * 60 * 24 * 365);
		try {
			getResource(out);
		} catch (SecurityException e) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		} catch (FileNotFoundException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void getUntypedResource(final OutputStream out) throws IOException {
		final HttpServletResponse response = getResponse();
		response.setHeader("Content-Type", "text/plain");
		response.setHeader("content-disposition", "inline");

		getResource(out);
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void getImg(final OutputStream out) throws Exception {
		String pathString = getPathParameters().getStringParameter(
				MethodParams.PATH, "");
		String resource;
		if (pathString.split("/").length > 2) {
			resource = pathString.replaceAll("^/.*?/", "");
		} else {
			resource = getRequestParameters().getStringParameter(
					MethodParams.PATH, "");
		}

		resource = resource.startsWith("/") ? resource : "/" + resource;

		String[] path = resource.split("/");
		String fileName = path[path.length - 1];
		setResponseHeaders(MimeTypes.getMimeType(fileName),
				RESOURCE_CACHE_DURATION, null);
		final HttpServletResponse response = getResponse();
		// Set cache for 1 year, give or take.
		response.setHeader("Cache-Control", "max-age=" + 60 * 60 * 24 * 365);
		try {
			getResource(out);
		} catch (SecurityException e) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		} catch (FileNotFoundException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void res(final OutputStream out) throws Exception {
		String pathString = getPathParameters().getStringParameter(
				MethodParams.PATH, "");
		String resource;
		if (pathString.split("/").length > 2) {
			resource = pathString.replaceAll("^/.*?/", "");
		} else {
			resource = getRequestParameters().getStringParameter(
					MethodParams.PATH, "");
		}
		resource = resource.startsWith("/") ? resource : "/" + resource;

		String[] path = resource.split("/");
		String[] fileName = path[path.length - 1].split("\\.");

		String mimeType;
		try {
			final MimeTypes.FileType fileType = MimeTypes.FileType
					.valueOf(fileName[fileName.length - 1].toUpperCase());
			mimeType = MimeTypes.getMimeType(fileType);
		} catch (java.lang.IllegalArgumentException ex) {
			mimeType = "";
		} catch (EnumConstantNotPresentException ex) {
			mimeType = "";
		}

		setResponseHeaders(mimeType, RESOURCE_CACHE_DURATION, null);
		final HttpServletResponse response = getResponse();
		// Set cache for 1 year, give or take.
		response.setHeader("Cache-Control", "max-age=" + 60 * 60 * 24 * 365);
		response.setHeader("content-disposition", "inline; filename=\""
				+ path[path.length - 1] + "\"");
		try {
			ResourceManager.getInstance().getSolutionResource(out, resource);
			setCacheControl();
		} catch (SecurityException e) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		} catch (FileNotFoundException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void getResource(final OutputStream out) throws IOException {
		String pathString = getPathParameters().getStringParameter(
				MethodParams.PATH, null);
		String resource;
		if (pathString.split("/").length > 2) {
			resource = pathString.replaceAll("^/.*?/", "");
		} else {
			resource = getRequestParameters().getStringParameter("resource",
					null);
		}

		if (!Utils.pathStartsWith(resource, SOLUTION_DIR)
				&& !Utils.pathStartsWith(resource, PLUGIN_PATH)
				&& !Utils.pathStartsWith(resource, "system"))// added for other
																// plugins'
																// components
		{
			resource = Utils.joinPath(PLUGIN_PATH, resource);// default path
		}

		final HttpServletResponse response = getResponse();

		String[] roots = FsPluginResourceLocations.getResourcesAbsDirs();
		try {
			ResourceManager.getInstance().getSolutionResource(out, resource,
					roots);
		} catch (SecurityException e) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		} catch (FileNotFoundException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	@Audited(action = "edit")
	public void edit(final OutputStream out) throws IOException {
		// 0 - Check security. Caveat: if no path is supplied, then we're in the
		// new parameter
		IParameterProvider requestParams = getRequestParameters();
		IParameterProvider pathParams = getPathParameters();

		boolean debugMode = requestParams.hasParameter(MethodParams.DEBUG)
				&& requestParams.getParameter(MethodParams.DEBUG).toString()
						.equals("true");

		String wcdfPath = getWcdfRelativePath(requestParams);

		if (requestParams.hasParameter(CdeConstants.MethodParams.PATH)
				&& !PentahoRepositoryAccess.getRepository().hasAccess(wcdfPath,
						IRepositoryAccess.FileAccess.EDIT)) {
			writeOut(out, "Access Denied");
		}

		// TODO: remove packager from content generator when no need to get
		// solutionpath of plugin
		writeOut(out, DashboardEditor.getEditor(wcdfPath, debugMode,
				getScheme(pathParams), packager));
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void syncTemplates(final OutputStream out) throws Exception {
		final CdfTemplates cdfTemplates = new CdfTemplates();
		cdfTemplates.handleCall(out, getRequestParameters());
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void syncStyles(final OutputStream out) throws Exception {
		final CdfStyles cdfStyles = new CdfStyles();
		cdfStyles.handleCall(out, getRequestParameters());
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void listRenderers(final OutputStream out) throws Exception {
		writeOut(out, "{\"result\": [\""
				+ DashboardWcdfDescriptor.DashboardRendererType.MOBILE
				+ "\",\""
				+ DashboardWcdfDescriptor.DashboardRendererType.BLUEPRINT
				+ "\"]}");
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void olapUtils(final OutputStream out) {
		final OlapUtils olapUtils = new OlapUtils();

		try {
			final Object result = olapUtils.process(getRequestParameters());
			JsonUtils.buildJsonResult(out, result != null, result);
		} catch (Exception ex) {
			logger.fatal(ex);
			JsonUtils.buildJsonResult(out, false, "Exception found: "
					+ ex.getClass().getName() + " - " + ex.getMessage());
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void exploreFolder(final OutputStream out) throws IOException {
		IParameterProvider requestParams = getRequestParameters();
		final String folder = requestParams.getStringParameter("dir", null);
		final String fileExtensions = requestParams.getStringParameter(
				"fileExtensions", null);
		final String permission = requestParams.getStringParameter("access",
				null);
		final String outputType = requestParams.getStringParameter(
				"outputType", null);

		if (outputType != null && outputType.equals("json")) {
			writeOut(
					out,
					FileExplorer.getInstance().getJSON(folder, fileExtensions,
							permission));
		} else {
			writeOut(
					out,
					FileExplorer.getInstance().getJqueryFileTree(folder,
							fileExtensions, permission));
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void getCDEplugins(final OutputStream out) throws IOException {
		CdePlugins plugins = new CdePlugins();
		writeOut(out, plugins.getCdePlugins());
	}

	/**
	 * List CDA datasources for given dashboard.
	 */
	@Deprecated
	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void listCdaSources(final OutputStream out) throws IOException {
		String dashboard = getRequestParameters().getStringParameter(
				"dashboard", null);
		dashboard = DashboardWcdfDescriptor.toStructurePath(dashboard);

		List<CdaDataSourceReader.CdaDataSource> dataSourcesList = CdaDataSourceReader
				.getCdaDataSources(dashboard);
		CdaDataSourceReader.CdaDataSource[] dataSources = dataSourcesList
				.toArray(new CdaDataSourceReader.CdaDataSource[dataSourcesList
						.size()]);
		String result = "[" + StringUtils.join(dataSources, ",") + "]";
		writeOut(out, result);
	}

	// External Editor v
	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void getFile(final OutputStream out) throws IOException {
		String path = getRequestParameters().getStringParameter(
				MethodParams.PATH, "");

		String contents = ExternalFileEditor.getFileContents(path);

		setResponseHeaders("text/plain", NO_CACHE_DURATION, null);
		writeOut(out, contents);
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void createFolder(OutputStream out)
			throws PentahoAccessControlException, IOException {
		String path = getRequestParameters().getStringParameter(
				MethodParams.PATH, null);

		if (ExternalFileEditor.createFolder(path)) {
			writeOut(out, "Path " + path + " created ok");
		} else {
			writeOut(out, "error creating folder " + path);
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void deleteFile(OutputStream out)
			throws PentahoAccessControlException, IOException {
		String path = getRequestParameters().getStringParameter(
				MethodParams.PATH, null);

		IRepositoryAccess access = PentahoRepositoryAccess
				.getRepository(userSession);
		if (access.hasAccess(path, FileAccess.DELETE)
				&& access.removeFileIfExists(path))
			writeOut(out, "file  " + path + " removed ok");
		else
			writeOut(out, "Error removing " + path);
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void writeFile(OutputStream out)
			throws PentahoAccessControlException, IOException {
		IParameterProvider requestParams = getRequestParameters();
		String path = requestParams.getStringParameter(MethodParams.PATH, null);
		String contents = requestParams.getStringParameter(MethodParams.DATA,
				null);

		if (ExternalFileEditor.writeFile(path, contents)) {// saved ok
			writeOut(out, "file '" + path + "' saved ok");
		} else {// error
			writeOut(out, "error saving file " + path);
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void canEdit(OutputStream out) throws IOException {
		String path = getRequestParameters().getStringParameter(
				MethodParams.PATH, null);

		Boolean result = ExternalFileEditor.canEdit(path);
		writeOut(out, result.toString());
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void extEditor(final OutputStream out) throws IOException {
		String editorPath = Utils.joinPath(PLUGIN_PATH, EXTERNAL_EDITOR_PAGE);
		writeOut(out, ExternalFileEditor.getFileContents(editorPath));
	}

	// External Editor ^
	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void componentEditor(final OutputStream out) throws IOException {
		String editorPath = Utils.joinPath(PLUGIN_PATH, COMPONENT_EDITOR_PAGE);
		writeOut(out, ExternalFileEditor.getFileContents(editorPath));
	}

	private String getWcdfRelativePath(final IParameterProvider pathParams) {
		final String path = "/"
				+ pathParams.getStringParameter(MethodParams.SOLUTION, null)
				+ "/" + pathParams.getStringParameter(MethodParams.PATH, null)
				+ "/" + pathParams.getStringParameter(MethodParams.FILE, null);

		return path.replaceAll("//+", "/");
	}

	private String getStructureRelativePath(final IParameterProvider pathParams) {
		String wcdfPath = getWcdfRelativePath(pathParams);
		return DashboardWcdfDescriptor.toStructurePath(wcdfPath);
	}

	private void setCacheControl() {
		final IPluginResourceLoader resLoader = PentahoSystem.get(
				IPluginResourceLoader.class, null);
		final String maxAge = resLoader.getPluginSetting(this.getClass(),
				"max-age");
		final HttpServletResponse response = getResponse();
		if (maxAge != null && response != null) {
			response.setHeader("Cache-Control", "max-age=" + maxAge);
		}
	}

	private void init() throws IOException {
		this.packager = Packager.getInstance();
		Properties props = new Properties();
		String rootdir = PentahoSystem.getApplicationContext().getSolutionPath(
				PLUGIN_PATH);
		props.load(new FileInputStream(rootdir + "/includes.properties"));

		if (!packager.isPackageRegistered("scripts")) {
			String[] files = props.get("scripts").toString().split(",");
			packager.registerPackage("scripts", Packager.Filetype.JS, rootdir,
					rootdir + "/js/scripts.js", files);
		}
		if (!packager.isPackageRegistered("styles")) {
			String[] files = props.get("styles").toString().split(",");
			packager.registerPackage("styles", Packager.Filetype.CSS, rootdir,
					rootdir + "/css/styles.css", files);
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void checkVersion(OutputStream out) throws IOException,
			JSONException {
		VersionChecker versionChecker = new CdeVersionChecker(
				CdeSettings.getSettings());
		writeOut(out, versionChecker.checkVersion());
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void getVersion(OutputStream out) throws IOException, JSONException {
		VersionChecker versionChecker = new CdeVersionChecker(
				CdeSettings.getSettings());
		writeOut(out, versionChecker.getVersion());
	}

	private String getScheme(IParameterProvider pathParams) {
		try {
			ServletRequest req = (ServletRequest) (pathParams
					.getParameter("httprequest"));
			return req.getScheme();
		} catch (Exception e) {
			return "http";
		}
	}

	private CdfRunJsDashboardWriteResult loadDashboard()
			throws ThingWriteException {
		IParameterProvider pathParams = parameterProviders.get("path");
		IParameterProvider requestParams = parameterProviders.get("request");

		String scheme = (requestParams.hasParameter("inferScheme") && requestParams
				.getParameter("inferScheme").equals("false")) ? ""
				: getScheme(pathParams);

		String absRoot = requestParams.getStringParameter("root", "");

		boolean absolute = (!absRoot.equals(""))
				|| requestParams.hasParameter("absolute")
				&& requestParams.getParameter("absolute").equals("true");

		boolean bypassCacheRead = requestParams.hasParameter("bypassCache")
				&& requestParams.getParameter("bypassCache").equals("true");

		boolean debug = requestParams.hasParameter("debug")
				&& requestParams.getParameter("debug").equals("true");

		String wcdfFilePath = getWcdfRelativePath(requestParams);

		CdfRunJsDashboardWriteOptions options = new CdfRunJsDashboardWriteOptions(
				absolute, debug, absRoot, scheme);

		return DashboardManager.getInstance().getDashboardCdfRunJs(
				wcdfFilePath, options, bypassCacheRead);
	}

	private String getCdfContext() {
		InterPluginCall cdfContext = new InterPluginCall(InterPluginCall.CDF,
				"Context");
		cdfContext.setRequest(getRequest());
		cdfContext.setRequestParameters(getRequestParameters());
		return cdfContext.callInPluginClassLoader();
	}

	private String getCdfContext(IParameterProvider requestParameterProvider) {
		InterPluginCall cdfContext = new InterPluginCall(InterPluginCall.CDF,
				"Context");
		cdfContext.setRequestParameters(requestParameterProvider);
		return cdfContext.callInPluginClassLoader();
	}
}
