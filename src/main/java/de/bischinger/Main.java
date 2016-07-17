package de.bischinger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.wildfly.swarm.container.Container;
import org.wildfly.swarm.datasources.DatasourcesFraction;
import org.wildfly.swarm.jaxrs.JAXRSArchive;
import org.wildfly.swarm.jpa.JPAFraction;
import org.wildfly.swarm.management.ManagementFraction;
import org.wildfly.swarm.monitor.MonitorFraction;
import org.wildfly.swarm.swagger.SwaggerArchive;
import org.wildfly.swarm.undertow.WARArchive;

import java.net.URL;
import java.util.Properties;

import static org.wildfly.swarm.logging.LoggingFraction.createDefaultLoggingFraction;
import static org.wildfly.swarm.undertow.UndertowFraction.createDefaultHTTPSOnlyFraction;

/**
 * Created by bischofa on 14/07/16.
 */
public class Main {
    public static void main(String[] args) throws Exception {

        ClassLoader cl = Main.class.getClassLoader();
        URL stageConfig = cl.getResource("project-stages.yml");

        Container container = new Container(args).withStageConfig(stageConfig);

        //Swagger
        SwaggerArchive archive = ShrinkWrap.create(SwaggerArchive.class, "healthcheck-app.war");
        archive.setResourcePackages("de.bischinger");

        JAXRSArchive deployment = archive.as(JAXRSArchive.class)
                .addPackage(Main.class.getPackage())
                .addResource(HealthCheckResource.class)
                .addResource(RegularResource.class)

                //jpa
                .addClasses(Employee.class)
                .addAsWebInfResource(new ClassLoaderAsset("META-INF/persistence.xml",
                        Main.class.getClassLoader()), "classes/META-INF/persistence.xml")
                .addAsWebInfResource(new ClassLoaderAsset("META-INF/load.sql",
                        Main.class.getClassLoader()), "classes/META-INF/load.sql")

                //.staticContent("webroot")  still not working ???
                .addAllDependencies();

        container.fraction(new DatasourcesFraction()
                .jdbcDriver("h2", (d) -> {
                    d.driverClassName("org.h2.Driver");
                    d.xaDatasourceClass("org.h2.jdbcx.JdbcDataSource");
                    d.driverModuleName("com.h2database.h2");
                })
                .dataSource("MyDS", (ds) -> {
                    ds.driverName("h2");
                    ds.connectionUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
                    ds.userName("sa");
                    ds.password("sa");
                })
        );

        // Prevent JPA Fraction from installing it's default datasource fraction
        container.fraction(new JPAFraction()
                .inhibitDefaultDatasource()
                .defaultDatasource("jboss/datasources/MyDS")
        );

        container
                .fraction(createDefaultLoggingFraction())
                .fraction(new MonitorFraction().securityRealm("ManagementRealm"))
                .fraction(new ManagementFraction()
                        .securityRealm("ManagementRealm", (realm) -> {
                            realm.inMemoryAuthentication((authn) -> {
                                authn.add(new Properties() {{
                                    put("admin", "test");
                                }}, true);
                            });
                            realm.inMemoryAuthorization();
                        }))
                .fraction(createDefaultHTTPSOnlyFraction("keystore.jks", "password", "selfsigned")) //ssl
                .start()
                .deploy(deployment);
    }
}
