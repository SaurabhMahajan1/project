package com.centrica.bg.pulse.fifomessaging;

import com.centrica.bg.pulse.fifomessaging.constant.Constants;
import io.swagger.jaxrs.config.BeanConfig;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.IndividualDeadLetterStrategy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.broker.region.policy.SharedDeadLetterStrategy;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableJms
@EnableConfigurationProperties(Application.ActiveMQConnectionFactoryProperties.class)
public class Application extends SpringBootServletInitializer {

    private static final Logger LOG = Logger.getLogger(Application.class);
   // public static final String MSG_AWAITING_RESPONSE_QUEUE_NAME = "msg_awaiting_response";

    private static SpringApplication springApplication;

    private static ConfigurableApplicationContext applicationContext;

    @Autowired
    private ActiveMQConnectionFactoryProperties activeMQConnectionFactoryProperties;

    @Bean(name = "msgInProgressQueue")
    //@Bean
    public Queue queue() {
        ActiveMQQueue activeMQQueue = new ActiveMQQueue(activeMQConnectionFactoryProperties.getMsgPendingClientResponseQueue());
        LOG.debug(activeMQConnectionFactoryProperties);
        return activeMQQueue;
    }

    @Autowired
    @Bean
    public DefaultMessageListenerContainer jmsContainer(ConnectionFactory connectionFactory, Queue msgInProgressQueue){
        DefaultMessageListenerContainer defaultMessageListenerContainer = new DefaultMessageListenerContainer();
        defaultMessageListenerContainer.setConnectionFactory(connectionFactory);
        defaultMessageListenerContainer.setDestination(new ActiveMQQueue(activeMQConnectionFactoryProperties.getExpiredMessageQueue()));
        defaultMessageListenerContainer.setSessionTransacted(true);
        defaultMessageListenerContainer.setAutoStartup(false);
        return defaultMessageListenerContainer;
    }


    @Bean
    public ConnectionFactory pooledConnectionFactory(){
        PooledConnectionFactory pooledConnectionFactory =
        new PooledConnectionFactory(new org.apache.activemq.ActiveMQConnectionFactory(activeMQConnectionFactoryProperties.getBrokerURL()));

        return pooledConnectionFactory;
    }

    @Autowired
    @Bean
    public JmsTemplate myJmsTemplate(ConnectionFactory pooledConnectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(pooledConnectionFactory);
        jmsTemplate.setSessionTransacted(true);
        System.out.println(jmsTemplate.getDefaultDestinationName());
        return jmsTemplate;
    }

    @Bean
    public IndividualDeadLetterStrategy AmqDeadLetterStrategy() {

        IndividualDeadLetterStrategy individualDeadLetterStrategy =new IndividualDeadLetterStrategy();
        individualDeadLetterStrategy.setQueueSuffix(".DLQ");
       // individualDeadLetterStrategy.setQueuePrefix("");
        individualDeadLetterStrategy.setUseQueueForQueueMessages(true);

        return individualDeadLetterStrategy;
    }

    @Bean
    public SharedDeadLetterStrategy sharedDeadLetterStrategy(){
        SharedDeadLetterStrategy sharedDeadLetterStrategy = new SharedDeadLetterStrategy();

        sharedDeadLetterStrategy.setProcessNonPersistent(true);
        return sharedDeadLetterStrategy;

    }
    @Autowired
    @Bean
    public PolicyEntry getPolicyMap(IndividualDeadLetterStrategy individualDeadLetterStrategy){
        PolicyEntry policyEntry = new PolicyEntry();
        policyEntry.setQueue(activeMQConnectionFactoryProperties.getMsgPendingClientResponseQueue());
        policyEntry.setDeadLetterStrategy(individualDeadLetterStrategy);
        return policyEntry;
    }

    @Autowired
    @Bean
    public BrokerService brokerService(IndividualDeadLetterStrategy individualDeadLetterStrategy,PolicyEntry policyEntry) throws Exception {
        BrokerService brokerService = new BrokerService();

        //brokerService.addConnector("tcp://localhost:61616");
        List<PolicyEntry> policyEntryList = new ArrayList<>();
        policyEntryList.add(policyEntry);

        PolicyMap policyMap = new PolicyMap();
        policyMap.setPolicyEntries(policyEntryList);
        brokerService.setDataDirectory("${activemq.data}");
        LOG.debug(brokerService.getDataDirectoryFile());
        brokerService.setPersistent(true);
        brokerService.setSchedulerSupport(true);

        brokerService.setDestinationPolicy(policyMap);
        //brokerService.getDestinationPolicy().setPolicyEntries(policyEntryList);
       // brokerService.getDestinationPolicy().getDefaultEntry().setDeadLetterStrategy(sharedDeadLetterStrategy);
        brokerService.start();

      // brokerService.setDestinationPolicy(policyMap);
        LOG.debug("***** broker service init "+brokerService);
        return brokerService;
    }

    @Bean
    public JmsListenerContainerFactory<?> myFactory(ConnectionFactory connectionFactory,
                                                    DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();

        // This provides all boot's default to this factory, including the message converter
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jacksonJmsMessageConverter());

        return factory;
    }

    @Bean // Serialize message content to json using TextMessage
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }


   /* @Configuration
    static class JmsConfiguration {

        @Bean
        public DefaultJmsListenerContainerFactory myFactory(
                DefaultJmsListenerContainerFactoryConfigurer configurer) {
            DefaultJmsListenerContainerFactory factory =
                    new DefaultJmsListenerContainerFactory();

            configurer.configure(factory, connectionFactory);
           // factory.setMessageConverter(myMessageConverter());
            return factory;
        }

    }*/

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    public static void main(String... args) {
        applicationContext = SpringApplication.run(Application.class, args);
        LOG.debug(applicationContext.getEnvironment().getPropertySources());

    }

    public static void setSpringApplication(SpringApplication springApplication) {
        Application.springApplication = springApplication;
    }

    public static ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        LOG.info("Inside containerCustomizer");
        return container -> container.setContextPath(Constants.WEB_CONTEXT_ROOT);
    }

    @Bean
    public BeanConfig swaggerConfig() {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion(Constants.API_VERSION);
        beanConfig.setSchemes(new String[]{Constants.HTTP, Constants.HTTPS});
        beanConfig.setBasePath(Constants.BASE_PATH);
        beanConfig.setResourcePackage(Constants.RESOURCE_PACKAGE);
        beanConfig.setPrettyPrint(true);
        beanConfig.setScan(true);
        beanConfig.setTitle(Constants.TITLE);
        beanConfig.setDescription(Constants.DESCRIPTION);
        return beanConfig;
    }

    @ConfigurationProperties(prefix = "spring.activemq")
    public static class ActiveMQConnectionFactoryProperties {
        private String brokerURL;
        private String user;
        private String password;
        private String expiredMessageQueue;
        private String msgPendingClientResponseQueue;

        public String getBrokerURL() {
            return brokerURL;
        }

        public void setBrokerURL(String brokerURL) {
            this.brokerURL = brokerURL;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getExpiredMessageQueue() {
            return expiredMessageQueue;
        }

        public void setExpiredMessageQueue(String expiredMessageQueue) {
            this.expiredMessageQueue = expiredMessageQueue;
        }

        public String getMsgPendingClientResponseQueue() {
            return msgPendingClientResponseQueue;
        }

        public void setMsgPendingClientResponseQueue(String msgPendingClientResponseQueue) {
            this.msgPendingClientResponseQueue = msgPendingClientResponseQueue;
        }

        @Override
        public String toString() {
            return "ActiveMQConnectionFactoryProperties{" +
                    "brokerURL='" + brokerURL + '\'' +
                    ", user='" + user + '\'' +
                    ", password='" + password + '\'' +
                    ", expiredMessageQueue='" + expiredMessageQueue + '\'' +
                    ", msgPendingClientResponseQueue='" + msgPendingClientResponseQueue + '\'' +
                    '}';
        }
    }
}

