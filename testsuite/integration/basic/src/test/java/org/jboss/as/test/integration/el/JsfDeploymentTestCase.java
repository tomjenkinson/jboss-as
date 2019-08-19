package org.jboss.as.test.integration.el;

import com.sun.faces.config.FacesInitializer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.ServletContainerInitializer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * See https://issues.jboss.org/browse/JBEAP-17186
 *
 * Creates deployment that uses custom JSF expression factory, which returns null when getStreamELResolver() is called.
 * This leads to NPE being thrown during deployment.
 *
 * This test should rightly be a Mojarra unit test, but testing this requires newer version of EL API than what
 * Mojarra 2.3.5.SP uses. For upstream Mojarra versions this test is already part of Mojarra TS.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JsfDeploymentTestCase {

    @ArquillianResource
    protected URL url;

    @Deployment(name = "deployment")
    public static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, JsfDeploymentTestCase.class.getSimpleName() + ".war")
                .addClasses(SamplePhaseListener.class,
                        TestServlet.class,
                        CustomExpressionFactory.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<web-app xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee\n" +
                        "         http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\"\n" +
                        "         version=\"4.0\">\n" +
                        "    <context-param>\n" +
                        "        <param-name>com.sun.faces.expressionFactory</param-name>\n" +
                        "        <param-value>org.jboss.as.test.integration.el.CustomExpressionFactory</param-value>\n" +
                        "    </context-param>\n" +
                        "</web-app>\n"), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "faces-config.xml")
                .addAsServiceProvider(ServletContainerInitializer.class, FacesInitializer.class)
                .addAsManifestResource(new StringAsset(
                                "<jboss-deployment-structure xmlns=\"urn:jboss:deployment-structure:1.2\">\n" +
                                        "  <deployment>\n" +
                                        "   <dependencies>\n" +
                                        "     <module name=\"com.sun.jsf-impl\" />\n" +
                                        "   </dependencies>\n" +
                                        "  </deployment>\n" +
                                        "</jboss-deployment-structure>\n"),
                        "jboss-deployment-structure.xml");
        return war;
    }

    @Test
    public void test() throws IOException, URISyntaxException {
        // just test that deployment is accessible, deployment should however fail if JBEAP-17186 is not fixed, so this
        // could be in fact empty

        URI uri = url.toURI().resolve("test");

        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(new HttpGet(uri));
        String content = new BufferedReader(new InputStreamReader(response.getEntity().getContent())).readLine();
        Assert.assertEquals("test", content);
    }
}
