package uk.co.javahelp.maven.plugin.fitnesse.mojo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.DuplicateMojoDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonatype.aether.RepositorySystemSession;

public class TearDownMojoTest {

	private TearDownMojo mojo;
	
    private ByteArrayOutputStream logStream;
    
    private BuildPluginManager pluginManager;
    
    private MavenSession mavenSession;
    
    private File workingDir;
    
	@Before
	public void setUp() throws Exception {
		pluginManager = mock(BuildPluginManager.class);
		mavenSession = mock(MavenSession.class);
		
		workingDir = new File(System.getProperty("java.io.tmpdir"), "unit_test_working");
		
		mojo = new TearDownMojo();
		mojo.workingDir = workingDir.getCanonicalPath();
		mojo.project = new MavenProject();
		mojo.project.setFile(new File(getClass().getResource("pom.xml").getPath()));
		mojo.pluginDescriptor = new PluginDescriptor();
		mojo.pluginDescriptor.getArtifactMap().put("org.apache.maven.plugins:maven-clean-plugin", setupArtifact("org.apache.maven.plugins", "maven-clean-plugin", "clean", "maven-plugin"));
		mojo.session = mavenSession;
		mojo.pluginManager = pluginManager;
        
		logStream = new ByteArrayOutputStream();
		mojo.setLog(new DefaultLog(new PrintStreamLogger(
			Logger.LEVEL_INFO, "test", new PrintStream(logStream))));
	}
	
	@SuppressWarnings("unchecked")
	private Artifact setupArtifact(String groupId, String artifactId, String goal, String type) throws DuplicateMojoDescriptorException, PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException {
		DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, "DUMMY", "compile", type, "", null);
	    MojoDescriptor mojoDescriptor = new MojoDescriptor();
		mojoDescriptor.setGoal(goal);
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
		pluginDescriptor.addMojo(mojoDescriptor);
		
		Plugin plugin = new Plugin();
		plugin.setGroupId(groupId);
		plugin.setArtifactId(artifactId);
		
        when(pluginManager.loadPlugin(eq(plugin), anyList(), any(RepositorySystemSession.class))).thenReturn(pluginDescriptor);
        return artifact;
	}
		
	@After
	public void tearDown() throws Exception {
		FileUtils.deleteQuietly(workingDir);
	}
	
	@Test
	public void testExecute() throws Exception {
		
        final Xpp3Dom cleanConfig = Xpp3DomBuilder.build(TearDownMojoTest.class.getResourceAsStream("teardown-clean-mojo-config.xml"), "UTF-8");
        
        doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertEquals(cleanConfig, 
				    ((MojoExecution) invocation.getArguments()[1]).getConfiguration());
				return null;
			}
        }).when(pluginManager).executeMojo(eq(mavenSession), any(MojoExecution.class));
        
		mojo.execute();
		
        verify(pluginManager, times(1)).executeMojo(eq(mavenSession), any(MojoExecution.class));
	}
}
