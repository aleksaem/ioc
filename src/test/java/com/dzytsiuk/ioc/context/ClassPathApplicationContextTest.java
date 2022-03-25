package com.dzytsiuk.ioc.context;

import com.dzytsiuk.ioc.exception.MultipleBeansForClassException;
import com.dzytsiuk.ioc.io.XMLBeanDefinitionReader;
import com.dzytsiuk.ioc.service.MailService;
import com.dzytsiuk.ioc.service.PaymentService;
import com.dzytsiuk.ioc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassPathApplicationContextTest {
    private UserService userService;
    private MailService mailService;
    private PaymentService paymentService;
    private PaymentService paymentServiceWithMaxAmount;
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        mailService = new MailService();
        mailService.setProtocol("POP3");
        mailService.setPort(3000);
        userService = new UserService();
        userService.setMailService(mailService);
        paymentService = new PaymentService();
        paymentService.setMailService(mailService);
        paymentServiceWithMaxAmount = new PaymentService();
        paymentServiceWithMaxAmount.setMailService(mailService);
        paymentServiceWithMaxAmount.setMaxAmount(500);
        applicationContext = new ClassPathApplicationContext("src/test/resources/context.xml");
    }

    @Test
    void testApplicationContextInstantiation() {
        ClassPathApplicationContext applicationContextSetReader = new ClassPathApplicationContext();
        applicationContextSetReader.setBeanDefinitionReader(new XMLBeanDefinitionReader("src/test/resources/context.xml"));
        applicationContextSetReader.start();
        assertSame(applicationContext.getBean(UserService.class), applicationContextSetReader.getBean(UserService.class));
        assertSame(applicationContext.getBean("mailService", MailService.class), applicationContextSetReader.getBean("mailService", MailService.class));
        assertSame(applicationContext.getBean("paymentWithMaxService"), applicationContextSetReader.getBean("paymentWithMaxService"));
    }

    @Test
    void testGetBeanByClass() {
        assertTrue(userService == applicationContext.getBean(UserService.class));
        assertTrue(mailService == applicationContext.getBean(MailService.class));
    }

    @Test
    void testGetBeanByClassException() {
        assertThrows(MultipleBeansForClassException.class, () -> applicationContext.getBean(PaymentService.class));
    }

    @Test
    void testGetBeanByNameAndClass() {
        assertSame(userService, applicationContext.getBean("userService", UserService.class));
        assertSame(mailService, applicationContext.getBean("mailService", MailService.class));
        assertSame(paymentService, applicationContext.getBean("paymentService", PaymentService.class));
        assertSame(paymentServiceWithMaxAmount, applicationContext.getBean("paymentWithMaxService", PaymentService.class));
    }

    @Test
    void testGetBeanByName() {
        assertSame(userService, applicationContext.getBean("userService"));
        assertSame(mailService, applicationContext.getBean("mailService"));
        assertSame(paymentService, applicationContext.getBean("paymentService"));
        assertSame(paymentServiceWithMaxAmount, applicationContext.getBean("paymentWithMaxService"));
    }
}