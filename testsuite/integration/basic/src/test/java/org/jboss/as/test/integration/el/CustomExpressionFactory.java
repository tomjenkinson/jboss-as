package org.jboss.as.test.integration.el;

import com.sun.el.ExpressionFactoryImpl;

import javax.el.ELResolver;
import java.util.Properties;

public class CustomExpressionFactory extends ExpressionFactoryImpl {
    public CustomExpressionFactory() {
    }

    public CustomExpressionFactory(Properties properties) {
        super(properties);
    }

    @Override
    public ELResolver getStreamELResolver() {
        return null;
    }
}
