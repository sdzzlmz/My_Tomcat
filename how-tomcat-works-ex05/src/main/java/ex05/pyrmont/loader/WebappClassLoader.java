package ex05.pyrmont.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.security.Policy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.loader.Extension;
import org.apache.catalina.loader.Reloader;
import org.apache.naming.resources.ResourceAttributes;

public class WebappClassLoader 
extends URLClassLoader
implements Reloader, Lifecycle {
/******  Instance Variables***************************************/
	private static final String[] triggers = {
		        "javax.servlet.Servlet"                     // Servlet API
	};
/**
 * Set of package names which are not allowed to be loaded from a webapp
 * class loader without delegating first.
 */
	private static final String[] packageTriggers = {
		        "javax",                                     // Java extensions
		        "org.xml.sax",                               // SAX 1 & 2
		        "org.w3c.dom",                               // DOM 1 & 2
		        "org.apache.xerces",                         // Xerces 1 & 2
		        "org.apache.xalan"                           // Xalan
};
    /**
     * The set of optional packages (formerly standard extensions) that
     * are available in the repositories associated with this class loader.
     * Each object in this list is of type
     * <code>org.apache.catalina.loader.Extension</code>.
     */
    protected ArrayList available = new ArrayList();
    /**
     * The list of local repositories, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected String[] repositories = new String[0];
    /**
     * Repositories translated as path in the work directory (for Jasper
     * originally), but which is used to generate fake URLs should getURLs be
     * called.
     */
    protected File[] files = new File[0];
    /**
     * The list of JARs, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected JarFile[] jarFiles = new JarFile[0];
    /**
     * The list of JARs, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected File[] jarRealFiles = new File[0];
    /**
     * The path which will be monitored for added Jar files.
     */
    protected String jarPath = null;


    /**
     * The list of JARs, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected String[] jarNames = new String[0];
    /**
     * The list of JARs last modified dates, in the order they should be
     * searched for locally loaded classes or resources.
     */
    protected long[] lastModifiedDates = new long[0];

    /**
     * The list of resources which should be checked when checking for
     * modifications.
     */
    protected String[] paths = new String[0];
    /**
     * The set of optional packages (formerly standard extensions) that
     * are required in the repositories associated with this class loader.
     * Each object in this list is of type
     * <code>org.apache.catalina.loader.Extension</code>.
     */
    protected ArrayList required = new ArrayList();

    /**
     * Associated directory context giving access to the resources in this
     * webapp.
     */
	private ClassLoader parent = null;
	//The system classloader
	private ClassLoader system = null;
	//The instance of securityManager installed
	private SecurityManager securityManager = null;
    protected DirContext resources = null;
	protected HashMap ResourceEntries = new HashMap();
	protected HashMap notFoundResources = new HashMap();
	protected int debug = 0;
	protected boolean hasExternalRepositories = false;
/**********************Constructor *******************************/
	public WebappClassLoader() {

        super(new URL[0]);
        this.parent = getParent();
        system = getSystemClassLoader();
        securityManager = System.getSecurityManager();

        if (securityManager != null) {
            refreshPolicy();
        }

    }
/**********************protected methods************************************/
	protected void refreshPolicy() {
		try {
            // The policy file may have been modified to adjust
            // permissions, so we're reloading it when loading or
            // reloading a Context
            Policy policy = Policy.getPolicy();
            policy.refresh();
        } catch (AccessControlException e) {
            // Some policy files may restrict this, even for the core,
            // so this exception is ignored
        }
	}
/*****************URLClassLoader methods************************************/
	public DirContext getResources() { 
		return resources;
	}
/*****************Lifecycle methods*******************************************/
	public void addLifecycleListener(LifecycleListener listener) {
		// TODO Auto-generated method stub

	}

	public LifecycleListener[] findLifecycleListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeLifecycleListener(LifecycleListener listener) {
		// TODO Auto-generated method stub

	}

	public void start() throws LifecycleException {
		// TODO Auto-generated method stub

	}

	public void stop() throws LifecycleException {
		// TODO Auto-generated method stub

	}
/*************************Reloader methods**********************/
	public void addRepository(String repository) {

	        // Ignore any of the standard repositories, as they are set up using
	        // either addJar or addRepository
	        if (repository.startsWith("/WEB-INF/lib")
	            || repository.startsWith("/WEB-INF/classes"))
	            return;

	        // Add this repository to our underlying class loader
	        try {
	            URL url = new URL(repository);
	            super.addURL(url);
	            hasExternalRepositories = true;
	        } catch (MalformedURLException e) {
	            throw new IllegalArgumentException(e.toString());
	        }

	}

	public String[] findRepositories() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean modified() {
		// TODO Auto-generated method stub
		return false;
	}
    synchronized void addJar(String jar, JarFile jarFile, File file)
            throws IOException {

            if (jar == null)
                return;
            if (jarFile == null)
                return;
            if (file == null)
                return;

            if (debug >= 1)
                System.out.println("addJar(" + jar + ")");

            int i;

            if ((jarPath != null) && (jar.startsWith(jarPath))) {

                String jarName = jar.substring(jarPath.length());
                while (jarName.startsWith("/"))
                    jarName = jarName.substring(1);

                String[] result = new String[jarNames.length + 1];
                for (i = 0; i < jarNames.length; i++) {
                    result[i] = jarNames[i];
                }
                result[jarNames.length] = jarName;
                jarNames = result;

            }

            try {

                // Register the JAR for tracking

                long lastModified =
                    ((ResourceAttributes) resources.getAttributes(jar))
                    .getLastModified();

                String[] result = new String[paths.length + 1];
                for (i = 0; i < paths.length; i++) {
                    result[i] = paths[i];
                }
                result[paths.length] = jar;
                paths = result;

                long[] result3 = new long[lastModifiedDates.length + 1];
                for (i = 0; i < lastModifiedDates.length; i++) {
                    result3[i] = lastModifiedDates[i];
                }
                result3[lastModifiedDates.length] = lastModified;
                lastModifiedDates = result3;

            } catch (NamingException e) {
                // Ignore
            }

            // If the JAR currently contains invalid classes, don't actually use it
            // for classloading
            if (!validateJarFile(file))
                return;

            JarFile[] result2 = new JarFile[jarFiles.length + 1];
            for (i = 0; i < jarFiles.length; i++) {
                result2[i] = jarFiles[i];
            }
            result2[jarFiles.length] = jarFile;
            jarFiles = result2;

            // Add the file to the list
            File[] result4 = new File[jarRealFiles.length + 1];
            for (i = 0; i < jarRealFiles.length; i++) {
                result4[i] = jarRealFiles[i];
            }
            result4[jarRealFiles.length] = file;
            jarRealFiles = result4;

            // Load manifest
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Iterator extensions = Extension.getAvailable(manifest).iterator();
                while (extensions.hasNext()) {
                    available.add(extensions.next());
                }
                extensions = Extension.getRequired(manifest).iterator();
                while (extensions.hasNext()) {
                    required.add(extensions.next());
                }
            }

        }
    /**
     * Validate a classname. As per SRV.9.7.2, we must restict loading of
     * classes from J2SE (java.*) and classes of the servlet API
     * (javax.servlet.*). That should enhance robustness and prevent a number
     * of user error (where an older version of servlet.jar would be present
     * in /WEB-INF/lib).
     *
     * @param name class name
     * @return true if the name is valid
     */
    protected boolean validate(String name) {

        if (name == null)
            return false;
        if (name.startsWith("java."))
            return false;

        return true;

    }
    /**
     * Add a new repository to the set of places this ClassLoader can look for
     * classes to be loaded.
     *
     * @param repository Name of a source of classes to be loaded, such as a
     *  directory pathname, a JAR file pathname, or a ZIP file pathname
     *
     * @exception IllegalArgumentException if the specified repository is
     *  invalid or does not exist
     */

    /**
     * Check the specified JAR file, and return <code>true</code> if it does
     * not contain any of the trigger classes.
     *
     * @param jarFile The JAR file to be checked
     *
     * @exception IOException if an input/output error occurs
     */
    private boolean validateJarFile(File jarfile)
        throws IOException {

        if (triggers == null)
            return (true);
        JarFile jarFile = new JarFile(jarfile);
        for (int i = 0; i < triggers.length; i++) {
            Class clazz = null;
            try {
                if (parent != null) {
                    clazz = parent.loadClass(triggers[i]);
                } else {
                    clazz = Class.forName(triggers[i]);
                }
            } catch (Throwable t) {
                clazz = null;
            }
            if (clazz == null)
                continue;
            String name = triggers[i].replace('.', '/') + ".class";
            if (debug >= 2)
                System.out.println(" Checking for " + name);
            JarEntry jarEntry = jarFile.getJarEntry(name);
            if (jarEntry != null) {
                System.out.println("validateJarFile(" + jarfile +
                    ") - jar not loaded. See Servlet Spec 2.3, "
                    + "section 9.7.2. Offending class: " + name);
                jarFile.close();
                return (false);
            }
        }
        jarFile.close();
        return (true);
    }



}
