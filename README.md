# Green Summer [![Codacy Badge](https://api.codacy.com/project/badge/Grade/86d7fca3670149a2818e30c82581359e)](https://www.codacy.com/app/Verdoso/GreenSummer?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Verdoso/GreenSummer&amp;utm_campaign=Badge_Grade)
Green Summer is a set of different utilities that can be added on top of Spring to augment the basic functionality.
It started as a set of controllers and configuration that I felt tempted to copy/paste into each of my projects. So I decided to create a shared library instead. Some of the functionality might be developed in the future in Spring (Boot) itself, and then I'll be more than happy to deprecate it and move along.
In the mean time, this is a brief summary of the utilities that one can find in Green Summer

 - **ConfigInspectorController**: A controller to show the effective configuration that is in place currently and the sources it comes from.
 - **Log4JController**: A controller to list/modify the Log4j2 log levels configuration at runtime.
 - **LogbackController**: A controller to list/modify the Logback log levels configuration at runtime.
 - **HealthController**: A simple controller with an actionable toggle that can be used from balancers or health checks to control whether the application should be used or not.
 - **Slf4jMDCFilter**:  A servlet filter that adds a unique ID to all the log messages that belong to each request. This way you can track all the log messages for a given request, even if they intermingle with messages from other requests. (See [Spring Boot: Setting a unique ID per request](https://medium.com/@d.lopez.j/spring-boot-setting-a-unique-id-per-request-dd648efef2b) for more information)
 - **ProfilingAspect**: An aspect that can be easily used to profile method calls. With the appropriate AOP configuration, it can be used to profile also private methods and/or methods belonging to classes outside Spring.
 - **CustomXMLHttpMessageConverter**: A message converter that overrides Jaxb2RootElementHttpMessageConverter and uses a pool of Marshallers to convert your objects to XML, instead of the "a new marshaller per request" approach that the regular Jaxb2RootElementHttpMessageConverter uses. The performance difference in applications with heavy traffic is quite noticeable.
 - **LoggingClientHttpRequestInterceptor**: An interceptor to log the traffic back & forth when using a RestTemplate. INFO shows basic info, DEBUG includes headers and TRACE includes request and response bodies. Useful for debugging RestTemplate calls at runtime with the help of Log4JController.
 - **CacheConfiguration**: A configuration class for the Caffeine cache so you can initialised multiple caches using configuration properties easily. 
 - **MessageSourceConfiguration**: A configuration utility class that helps configuring a MessageSource for your labels, so you can easily set a default locale and if changes are detected are runtime or not (very useful during development). 
 - **SummerXSLTView**: An XSLTView that allows you to specify a result object and an XSLT. The result object is marshalled first to XML, using JAXB, and then the XSLT stylesheet is used to transform the XML to the final result. Useful to get different XML/HTML views without having to create extra Java classes.
 - **SummerJoltView**: A view that allows you to specify a result object and a Jolt specification. The result object is marshalled first to JSON, using Jackson, and then the Jolt specification is used to transform the JSON to the final JSON. Useful to get different JSON views without having to create extra Java classes.
 - **ObjectJoiner**: Just a simple convenient utility to create strings from a number of objects, specified as varargs or collection, optionally using a separator. 
 - **ActuatorCustomizer**: An EndpointHandlerMappingCustomizer that changes the order of the media types returned from the actuator so the media types with the version number included are at the end (so regular clients recognise the response a JSON). 
 - **ApplicationContextProvider**: A somehow hackish way to get a reference to the application context from almost any class (required by SummerJoltView). 
 - **SelfDiscoveryPropertySourceLocator**: A PropertySourceLocator that adds the property spring.cloud.consul.discovery.hostname with the value InetAddress.getLocalHost().getCanonicalHostName() at the very beginning so Consul can use that value.

The Spring integrated utilites can be enabled using the annotation @EnableSummer and specifying the appropriate parameters.

I'll be adding documentation as time permits.